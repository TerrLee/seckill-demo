package com.example.seckilldemo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.seckilldemo.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface UserService extends IService<User> {

     Boolean register(User user);

     String login(String username,String password);

     String updateAvatar(MultipartFile file ,String username);

     String sendCode(String email);

     void restPassword(String email,String code,String nePassword);

     User getUserInfo(String username);

     void logout(String token);
}
