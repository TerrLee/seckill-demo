package com.example.seckilldemo.controller;

import com.example.seckilldemo.common.Result;
import com.example.seckilldemo.entity.User;
import com.example.seckilldemo.service.UserService;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;


@RestController
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<String> register(@RequestBody User user){
        userService.register(user);
        return Result.success("注册成功");
    }
    @PostMapping("login")
    public Result<String> login (@RequestBody User user){
        //1调用Service的登陆方式，拿到token
        String token = userService.login(user.getUsername(),user.getPassword());
        //2.把Token塞进result里返回前端
        return Result.success(token);
    }
    @PostMapping("/avatar")
    public Result<String> updateAvatar(@RequestParam("file") MultipartFile file){

        String username = SecurityContextHolder.getContext().getAuthentication().getName();

        String avatarUrl=userService.updateAvatar(file , username);

        return Result.success(avatarUrl);
    }

    @PostMapping("/send-code")
    public  Result<String>  sendCode(@RequestBody Map<String,String> request){
        //
        String email = request.get("email");
        if(email == null || email.trim().isEmpty()){
            return Result.error("邮箱不能为空");
        }
        //2.调用Service
        String msg=userService.sendCode(email);
        //3。返回成功
        return Result.success(msg);
    }

    @PostMapping("/reset-password")
    public Result<String> restPassword(@RequestBody Map<String,String> request) {
        String email = request.get("email");
        String code = request.get("code");
        String newPassword = request.get("newPassword");

        //简单校验
        if(email == null||code ==null|| newPassword==null){
            return Result.error("参数不完整");
        }
        //调用service
        userService.restPassword(email,code,newPassword);
        return Result.success("密码重置成功2，请重新登录");
    }

    @GetMapping("/info")
    public Result<User> getUserInfo(){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.getUserInfo(username);
        return Result.success(user);
    }

    @PostMapping("/logout")
    public Result<String>logout(@RequestHeader("Authorization")String token){
        userService.logout(token);
        return Result.success("退出登录成功");

    }
}
