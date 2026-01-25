package com.example.blog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取当前项目运行目录
        String basePath = System.getProperty("user.dir") + File.separator + "files" + File.separator;

        // 映射规则：访问 /files/** -> 去本地 files 目录找
        // "file:" 前缀告诉 Spring Boot 这是一个文件系统路径
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + basePath);
    }
}