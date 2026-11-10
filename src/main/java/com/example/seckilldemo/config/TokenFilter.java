package com.example.seckilldemo.config;

import com.example.seckilldemo.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

@Component
public class TokenFilter extends  OncePerRequestFilter {
    @Autowired
    private JwtUtils jwtUtils   ;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Override
    protected  void doFilterInternal (HttpServletRequest request ,HttpServletResponse response,FilterChain filterChain)
            throws ServletException,IOException{
        String token =request.getHeader("Authorization");
        if(token!= null&& token.startsWith("Bearer ")){
            token=token.substring(7);//去掉bearer

        }

        //config token

        if(token!=null && jwtUtils.validateToken(token)){
            //3去查redis这个token是否存在

            String username = redisTemplate.opsForValue().get("login:token:"+token);
            
            //获取Redis中该Token的剩余过期时间（秒）
            Long expire = redisTemplate.getExpire("login:token:"+token, TimeUnit.SECONDS);
            //如果剩余时间少于600秒，借给他续3600秒
            if(expire!=null&&expire>0&&expire<600){
                Boolean success = redisTemplate.expire("login:token:" + token, 30, TimeUnit.MINUTES);
                // 立刻再查一次，看看续期后的结果
                Long newExpire=redisTemplate.getExpire("login:token:"+token,TimeUnit.SECONDS);

            }
            if(username!=null){
                //从token里面把角色拿出来
                String role = jwtUtils.getRoleFromToken(token);
                //把角色变成权限列表（spring security 要求角色名必须以role_开头）
                System.out.println("解析出的角色是: " + role);
                //singlettonList会创建一个不可变的（immutable）且包含一个元素的列表
                List<SimpleGrantedAuthority> authorities =Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_"+role.toLowerCase())
                );
                //4存在就把用户名和权限一起塞进securitycontexholder
                UsernamePasswordAuthenticationToken authentication=new UsernamePasswordAuthenticationToken(username,null,authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        }
        filterChain.doFilter(request,response);
    }
}
