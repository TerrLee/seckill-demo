package com.example.seckilldemo.exception;

import com.example.seckilldemo.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public Result<String> handleBusinessException(BusinessException e){
        log.warn("业务异常拦截：{}",e.getMessage());
        //e.getMessage() 就是你在Service层 throw 时传的那个“库存不足”
        return Result.error(e.getMessage());
    }
    //这个兜底也一样，用Exception 接收所有没预料的错误
    @ExceptionHandler(Exception.class)
    public Result<String>handleException(Exception e){
        log.error("系统异常",e);
        return Result.error("系统开小差了，请稍后重试");
    }
}
