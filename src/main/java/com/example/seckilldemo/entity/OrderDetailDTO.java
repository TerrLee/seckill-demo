package com.example.seckilldemo.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderDetailDTO extends SeckillOrder{
    private String username;

    private String productName;

    private BigDecimal price;
}
