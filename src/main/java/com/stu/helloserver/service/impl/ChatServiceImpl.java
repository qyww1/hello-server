package com.stu.helloserver.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stu.helloserver.service.ChatService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatServiceImpl implements ChatService {

    @Value("${dashscope.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String chat(String message) {
        if (apiKey == null || apiKey.isEmpty()) {
            return "请设置环境变量 AI_DASHSCOPE_API_KEY";
        }

        String url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "qwen-max");
        
        Map<String, Object> input = new HashMap<>();
        List<Map<String, String>> messages = new java.util.ArrayList<>();
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", message);
        messages.add(userMessage);
        input.put("messages", messages);
        body.put("input", input);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("result_format", "message");
        body.put("parameters", parameters);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                String responseBody = response.getBody();
                System.out.println("DashScope API响应: " + responseBody);
                JsonNode root = objectMapper.readTree(responseBody);
                
                JsonNode output = root.path("output");
                if (output.has("choices")) {
                    JsonNode choices = output.path("choices");
                    if (choices.isArray() && choices.size() > 0) {
                        JsonNode firstChoice = choices.get(0);
                        JsonNode msg = firstChoice.path("message");
                        if (msg.has("content")) {
                            return msg.path("content").asText();
                        }
                    }
                }
                
                if (output.has("text")) {
                    return output.path("text").asText();
                }
                
                return "响应格式未识别: " + responseBody;
            }
            return "请求失败: " + response.getStatusCode() + ", 响应体: " + response.getBody();
        } catch (Exception e) {
            return "调用失败: " + e.getMessage();
        }
    }
}