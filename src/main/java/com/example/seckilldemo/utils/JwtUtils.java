package com.example.seckilldemo.utils;
import io.jsonwebtoken.Jwts;

import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

import java.util.Date;



@Component

public class JwtUtils {
    @Value("${jwt.secret}")
    private String secret;
    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey key;
    @PostConstruct
     public void init() {
        //2,把字符串的secrect 转换成真正的密匙
       this.key= Keys.hmacShaKeyFor(secret.getBytes());
    }
    //----------------
    //生成token
    //----------------
    public String generateToken(String username,String role){
        //核心API：Jwts。builder()
        return Jwts.builder()
                .setSubject(username)
                .claim("role",role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();


    }

    public boolean validateToken(String token){
        try{
            //解析token，只要不抛异常，就说明没被篡改而且没过期
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        }catch(Exception e){
            return false;
        }
    }

    public String getUsernameFromToken(String token){
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getSubject();
        //核心api：jwt.parseBuilder()...getBody().getSubject()

    }
    public String getRoleFromToken(String token){
        //核心api：jwts。parserBuilder()...getBody().get("role",String.class)
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().get("role",String.class);
    }
}
