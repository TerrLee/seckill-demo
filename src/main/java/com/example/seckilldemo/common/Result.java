package com.example.seckilldemo.common;

import lombok.Data;


//解释@Data是lombok的注解，
// 自动生成getter/setter和toString 方法，省的代码太长
@Data
public class Result<T>{//<T>是泛形，意思是这个类可以装任任何数据类型，就像“一个什么都能装的快递箱

    private Integer code;//状态码
    private String message;//提示信息
    private T data;//数据

    //写一个无参构造，有时候json框架序列化需要用到
    public Result(){}
    //写个全参构造，方便我们直接new一个result对象
    public Result(Integer code , String message, T data){
        this.code=code;
        this.message=message;
        this.data=data;
    }
    //static静态方法（工厂方法），这样就不用手动new了，直接Result.success()就能用
    public static <T> Result <T> success(T data){
        return new Result<>(200,"操作成功",data);
    }
    public static <T> Result <T> success(){
        return new Result<>(200,"操作成功",null);
    }
    public static <T> Result <T> error(String message){
        return new Result<>(500,message,null);
    }
    public static <T> Result <T> unauthorized(String message ){
        return new Result<>(401,message,null);
    }

}


