package com.example.seckilldemo.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.seckilldemo.entity.Product;

import com.example.seckilldemo.entity.SeckillOrder;
import com.example.seckilldemo.exception.BusinessException;
import com.example.seckilldemo.mapper.ProductMapper;
import com.example.seckilldemo.service.ProductService;
import com.example.seckilldemo.service.SeckillOrderService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper,Product> implements ProductService {
    //注入redis模版
    private final StringRedisTemplate redisTemplate;
    //注入订单服务
    private final SeckillOrderService secKillOrderService;
    public ProductServiceImpl(StringRedisTemplate redisTemplate, SeckillOrderService secKillOrderService) {
        this.redisTemplate = redisTemplate;
        this.secKillOrderService = secKillOrderService;
    }

//--------------------加载核心Lua脚本----------------------
    //使用静态代码块，保证项目启动时之加载一次，提高性能
    private static  final DefaultRedisScript<Long>SECKILL_SCRIPT ;
    static {
        SECKILL_SCRIPT= new DefaultRedisScript<>();
        //指定脚本路径，对应resource 下的sckill.lua
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        //指定返回值类型
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    //库存预热
    @Override
    public void preheatStock(Long productId) {
        //查数据库商品
        Product product = this.getById(productId);
        if(product == null){
            throw new BusinessException("商品不存在");
        }

        //2.存进redis
        //key 命名规范：seckill：stock：商品id
        //Value：库存数量（String类型）
        String redisKey = "seckill:stock:"+productId;
        redisTemplate.opsForValue().set(redisKey,String.valueOf(product.getStock()));

        System.out.println("=========预热成功========");
        System.out.println("商品："+product.getName()+"的库存"+product.getStock()+"已经存入redis");
    }

    //执行秒杀
    @Override
    public void doSeckill(Long productId, Long userId) {
        String stockKey ="seckill:stock:"+productId;

        //执行lua脚本
        //参数一：脚本对象
        //参数二：Key的集合（Lua里面的Keys[1])
        //参数三：集合参数（Lua里的ARGV[1])

        Long result = redisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.singletonList(stockKey),
                userId.toString()
        );
        //解析lua脚本的返回值
        if(result == null || result ==0){
            throw new BusinessException("商品已经售磬");
        }
        if (result == -1){
            throw new BusinessException("秒杀尚未开始或已经结束！");
        }
        if(result==-2){
            throw new BusinessException("你已经抢过了，请勿重复下单！");
        }

        //走到这里说明redis 扣件成功，可以进行后续业务
        System.out.println("用户："+userId+"抢购商品："+productId+"成功！正在准备生成订单");

        //生成订单并保存到数据库
        SeckillOrder order = new SeckillOrder();
        order.setUserId(userId);
        order.setProductId(productId);
        order.setCreateTime(LocalDateTime.now());
        try {
            //1.生成订单落库
            secKillOrderService.save(order);
            //🌟 2. 核心：扣减 MySQL 库存（必须用乐观锁！）
            //底层执行的sql是：update product Set stock = stock -1 where id =? and stock>0
            boolean updateSuccess = this.update()
                    .setSql("stock = stock -1") //不能直接set stock=99 ，高并发下会覆盖别人的扣减
                    .eq("id",productId)
                    .gt("stock",0)
                    .update();
            if(!updateSuccess){
                //如果数据库扣减失败（比如数据库库存已经被别人扣光了），必须抛异常
                throw new RuntimeException("MySQL库存不足");
            }
            System.out.println("订单落库成功！订单id:"+order.getId());
        }catch (Exception e){
            //打印一下报错日志
            System.out.println("订单落库失败，触发回滚机制："+e.getMessage());
            //2.把库存加回去！
            redisTemplate.opsForValue().increment(stockKey);
            //3.把名单划掉：把用户从防重踢出去，允许他重新抢！
            String userSetkey = "seckill:order:"+stockKey;
            redisTemplate.opsForSet().remove(userSetkey,userId.toString());
            //如果数据库写入失败（比如触发了唯一的索引报错），要把redis的库存加回去（补偿机制）
            //但单体暂时先简单抛异常，后续进阶在学补偿
            System.out.println("订单落库失败"+e.getMessage());
            throw new BusinessException("下单失败，请稍后尝试");
        }
    }
}
