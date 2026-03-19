package com.stu.helloserver;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    // 定义 GET 请求接口，路径为 /hello
    @GetMapping("/hello")
    public String hello() {
        return "Hello, Spring Boot RESTful API!";
    }
}