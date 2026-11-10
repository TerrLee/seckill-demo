package com.example.seckilldemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.seckilldemo.entity.OrderDetailDTO;
import com.example.seckilldemo.entity.SeckillOrder;

import java.util.List;

public interface SeckillOrderMapper extends BaseMapper<SeckillOrder> {
    List<OrderDetailDTO>selectOrderDetails();
}
