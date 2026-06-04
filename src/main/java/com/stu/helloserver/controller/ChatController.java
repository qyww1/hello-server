package com.stu.helloserver.controller;

import com.stu.helloserver.common.Result;
import com.stu.helloserver.dto.ChatRequestDTO;
import com.stu.helloserver.service.ChatService;
import com.stu.helloserver.vo.ChatResponseVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public Result<ChatResponseVO> chat(@RequestBody ChatRequestDTO requestDTO) {
        ChatResponseVO responseVO = chatService.chat(requestDTO);
        return Result.success(responseVO);
    }
}