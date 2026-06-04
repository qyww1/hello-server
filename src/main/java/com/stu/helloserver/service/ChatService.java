package com.stu.helloserver.service;

import com.stu.helloserver.dto.ChatRequestDTO;
import com.stu.helloserver.vo.ChatResponseVO;

public interface ChatService {
    ChatResponseVO chat(ChatRequestDTO requestDTO);
}