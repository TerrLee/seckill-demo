package com.example.seckilldemo.service.Impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.seckilldemo.entity.SeckillOrder;
import com.example.seckilldemo.mapper.SeckillOrderMapper;
import com.example.seckilldemo.service.SeckillOrderService;
import org.springframework.stereotype.Service;

@Service
public class SecKillOrderServiceImpl extends ServiceImpl<SeckillOrderMapper, SeckillOrder>implements SeckillOrderService {
}
