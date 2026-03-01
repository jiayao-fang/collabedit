package com.example.collabedit.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import io.micrometer.common.lang.NonNull;
@Configuration
public class FileUploadConfig implements WebMvcConfigurer {
    // 配置静态资源访问路径
    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // 本地文件上传路径
        String uploadPath = System.getProperty("user.dir") + "/uploads/";   
        // 映射访问路径为/upload/** 到本地文件路径
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);
    }
}