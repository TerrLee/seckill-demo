package com.example.seckilldemo.utils;


import com.example.seckilldemo.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Component
public class FileUploadUtils {
    //定义个常量作为上传目录
    public static final String UPLOAD_DIR = "/Users/chenshuai/Documents/Code/seckill-demo/uploads/";
    //核心方法，必须是static静态的！
    public static String upload(MultipartFile file){
        if(file.isEmpty()){
            throw new BusinessException("上传的文件不能为空");

        }
        //获取用户原本的文件名
        String originalFilename = file.getOriginalFilename();
        //截取文件后缀（从最后的一个.开始截取，拿到.png或。jpg）。
        //为了防止黑客上传.jsp,.sh等恶意脚本
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));

        //名单校验
        if(!".jpg".equalsIgnoreCase(extension)&&!".png".equalsIgnoreCase(extension)){
            throw new BusinessException("只许上传.jpg或.png格式的图片");
        }
        //[防重复名称核心】使用uuid （全局唯一标识符）生成一个绝对不会重复的随机字符
        //replace（“-”，“”）是为了把uuid自带的横杠去掉，看起来更加清爽
        //最终生成类似的：a1b2c34d5e.png
        String newFileName = UUID.randomUUID().toString().replace("-","")+extension;

        //凭借完整的物理路径
        File dest = new File(UPLOAD_DIR+newFileName);

        //防止崩溃
        //如果第一次运行.uploads文件夹根本不存在，transferTo会直接报错
        //getParentFile().mkdirs()会自动创建一整串目录
        if(!dest.getParentFile().exists()){
            dest.getParentFile().mkdirs();
        }

        try{
            //IO核心真正把内存里面的文件流，写入到物理硬盘里面的指定位置！
            file.transferTo(dest);
        }catch(IOException e){
            //如果磁盘满了，没权限，或者路径写错了，这里捕获并抛出业务异常。
            throw new BusinessException("文件保存失败"+e.getMessage());
        }
        //[返回核心】：绝对不能是硬盘的路径
        //而是返回前端（或存入数据库）的相对访问路径
        //
        return "/uploads/"+ newFileName;
    }
}
