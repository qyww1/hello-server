package com.stu.helloserver.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatRecord {
    
    /**
     * 会话编号
     */
    private String sessionId;
    
    /**
     * 用户问题
     */
    private String userMessage;
    
    /**
     * 大模型回答
     */
    private String assistantMessage;
    
    /**
     * 记录时间
     */
    private LocalDateTime createTime;
}