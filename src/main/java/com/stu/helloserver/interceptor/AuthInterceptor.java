package com.stu.helloserver.interceptor;

import com.stu.helloserver.common.ResultCode;
import com.stu.helloserver.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");

        if (token == null || token.isEmpty()) {
            response.setContentType("application/json;charset=UTF-8");
            Result<Object> errorResult = Result.error(ResultCode.TOKEN_INVALID);
            ObjectMapper objectMapper = new ObjectMapper();
            String errorJson = objectMapper.writeValueAsString(errorResult);
            response.getWriter().write(errorJson);
            return false;
        }

        return true;
    }
}