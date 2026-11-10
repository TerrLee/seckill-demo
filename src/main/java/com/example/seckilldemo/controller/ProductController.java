package com.example.seckilldemo.controller;


import com.example.seckilldemo.common.Result;
import com.example.seckilldemo.service.ProductService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/product")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/preheat/{productId}")
    public Result<String> preheatStock(@PathVariable Long productId){
        productService.preheatStock(productId);
        return Result.success("预热成功");
    }

    //2.秒杀 测试接口（暂时通过请求参数传userid，模拟不同的用户抢购）
    @PostMapping("/seckill")
    public Result<String> seckill(@RequestBody Map<String,Long> params){
        Long productId = params.get("productId");
        Long userId = params.get("userId");

        productService.doSeckill(productId,userId);
                return  Result.success("秒杀成功");
    }
}

