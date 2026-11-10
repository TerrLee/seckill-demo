package com.example.seckilldemo.exception;

public class BusinessException extends RuntimeException{
    //构造器：接受一个String 错误类型信息·
    public BusinessException(String message){
        //关键！把message 直接交给RuntimeException去处理
        super(message);
    }
}
