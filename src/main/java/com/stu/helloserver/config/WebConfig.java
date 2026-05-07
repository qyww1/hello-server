package com.stu.helloserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    // 旧鉴权拦截器已迁移至Spring Security
    // 此文件保留用于其他MVC配置
}
