package com.example.seckilldemo.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect//告诉哦spring：这是一个切面类
@Component//放进ioc容器
public class LogAspect {
    //1.定义切点：只拦截com.example.demo2.service包下的所有方法
    @Pointcut("execution(* com.example.seckilldemo.service.*.*(..))")
    public void servicePointcut(){}

    //2.定义通知（前置通知):在切点方法执行之前【执行】运行
    @Before("servicePointcut()")
    public void beforeAdvice(JoinPoint joinPoint){
        //获取被拦截的方法名
        String methodName = joinPoint.getSignature().getName();
        //获取传进来的参数
        Object[] args = joinPoint.getArgs();

        System.out.println("=====AOP拦截到了！方法名："+methodName+",参数：："+args.length+"个======");
    }
}
