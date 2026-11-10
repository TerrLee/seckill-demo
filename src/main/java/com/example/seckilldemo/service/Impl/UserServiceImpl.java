package com.example.seckilldemo.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.seckilldemo.common.Result;
import com.example.seckilldemo.entity.User;
import com.example.seckilldemo.exception.BusinessException;
import com.example.seckilldemo.mapper.UserMapper;
import com.example.seckilldemo.service.UserService;
import com.example.seckilldemo.utils.FileUploadUtils;
import com.example.seckilldemo.utils.JwtUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;
import java.util.concurrent.TimeUnit;



@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User>implements UserService {
    private final JwtUtils jwtUtils;
    private final StringRedisTemplate stringRedisTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public UserServiceImpl(JwtUtils jwtUtils, StringRedisTemplate stringRedisTemplate) {

        this.jwtUtils = jwtUtils;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public Boolean register(User user) {
        //查询构建器条件盒子
        LambdaQueryWrapper<User>wrapper=new LambdaQueryWrapper<>();
        //用wrapper的eq方法，条件 username =前端传的
        wrapper.eq(User::getUsername,user.getUsername());
        //去数据库查询
        User existUser = this.getOne(wrapper);
        //做判断
        if(existUser!=null){
            throw new BusinessException("用户名已存在");
        }
        //加密
        String bcryptPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(bcryptPassword);
        //6.保存数据到数据库并返回结果
        return this.save(user);
    }


    @Override
    public String login(String username, String password){
        LambdaQueryWrapper <User>wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername,username);
        //
        User exisUser = this.getOne(wrapper);
        if(exisUser == null){
            throw new BusinessException("用户不存在");
        }

        if(!passwordEncoder.matches(password,exisUser.getPassword())){
            throw new BusinessException("密码错误");
        }

        //4.生成token
        String token = jwtUtils.generateToken(exisUser.getUsername(),exisUser.getRole());
        //存入redis（设置30分钟过期）
        stringRedisTemplate.opsForValue().set("login:token:"+token,exisUser.getUsername(),30, TimeUnit.MINUTES);

        return token;
    }

    @Override
    public String updateAvatar(MultipartFile file, String username) {
        String avatarUrl = FileUploadUtils.upload(file);
        LambdaQueryWrapper<User>wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername,username);
        User user = this.getOne(wrapper);

        if(user==null){
            throw new BusinessException("未查询到该用户信息");
        }
        user.setAvatar(avatarUrl);
        this.updateById(user);

        //将路径返回给前端
        return avatarUrl;
    }

    @Override
    public String sendCode(String email) {
        //1查库
        LambdaQueryWrapper<User>wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail,email);
        if(this.count(wrapper)==0){
            throw new BusinessException("该邮箱未注册");
        }
        //2.防刷机制(查redis里有没有还没过期的key）
        String redisKey = "email:code:"+email;
        if(Boolean.TRUE.equals(stringRedisTemplate.hasKey(redisKey))){
            throw new BusinessException("验证码已经发送，请一分钟后再试！");
        }
        //3.生成六位随机数字
        String code = String.valueOf((int)((Math.random()*9+1)*100000));

        //4.存入redis（有效期5分钟）
        stringRedisTemplate.opsForValue().set(redisKey,code,5,TimeUnit.MINUTES);

        //5发邮件
        //（这里去调用发邮件的方法，把email和code传进去）

        //测试时为了不浪费邮箱的次数，
        System.out.println("======== 发送验证码 ========");
        System.out.println("发给：" + email + " 的验证码是：" + code);
        System.out.println("============================");

        return  "验证码已发送，请查收.";
    }

    @Override
    public void restPassword(String email, String code, String newPassword) {
        String redisKey="email:code:"+email;


        String realCode=stringRedisTemplate.opsForValue().get(redisKey);
        if(realCode==null){
            throw new BusinessException("验证码已过期或不存在");
        }
        if (!realCode.equals(code)){
            throw new BusinessException("验证码错误");
        }
        LambdaQueryWrapper<User>wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail,email);
        User user = this.getOne(wrapper);

        if(user==null){
            throw new BusinessException("未找到该邮箱绑定的用户");
        }
        //5.把新密码加密，然后更新到数据库
        String bcetptPassword = passwordEncoder.encode(newPassword);
        user.setPassword(bcetptPassword);
        this.updateById(user);

        //6.极其重要：密码改完了，验证码立即作废！从redis中删除
        stringRedisTemplate.delete(redisKey);
    }

    @Override
    public User getUserInfo(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername,username);
        User user=this.getOne(wrapper);
                if(user!=null){
                    user.setPassword(null);
                }
                return user;
    }

    @Override
    public void logout(String token) {
        //1.去掉“Bearer “前缀
        if(token!=null&&token.startsWith("Bearer ")){
            token = token.substring(7);
        }
        if(token!= null){
            stringRedisTemplate.delete("login:token:"+token);
        }
    }


}
