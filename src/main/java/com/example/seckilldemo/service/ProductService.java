package com.example.seckilldemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.seckilldemo.entity.Product;

public interface ProductService extends IService<Product> {

    void preheatStock(Long productId);

    void doSeckill(Long producId,Long userId );
}
