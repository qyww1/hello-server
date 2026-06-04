package com.stu.helloserver.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stu.helloserver.dto.ChatRequestDTO;
import com.stu.helloserver.service.ChatService;
import com.stu.helloserver.vo.ChatResponseVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatServiceImpl implements ChatService {

    @Value("${dashscope.api-key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final StringRedisTemplate stringRedisTemplate;

    public ChatServiceImpl(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public ChatResponseVO chat(ChatRequestDTO requestDTO) {
        String sessionId = requestDTO.getSessionId();
        String message = requestDTO.getMessage();

        if (apiKey == null || apiKey.isEmpty()) {
            return new ChatResponseVO(message, "请设置环境变量 AI_DASHSCOPE_API_KEY");
        }

        if (sessionId == null || sessionId.isEmpty()) {
            return new ChatResponseVO(message, "sessionId不能为空");
        }

        String redisKey = "chat:session:" + sessionId;
        String historyText = "";

        // 1. 读取历史消息（带异常处理）
        try {
            List<String> records = stringRedisTemplate.opsForList().range(redisKey, 0, -1);
            if (records != null && !records.isEmpty()) {
                historyText = String.join("\n", records);
            }
        } catch (Exception e) {
            System.out.println("Redis读取失败，跳过会话历史: " + e.getMessage());
        }

        // 2. 拼接上下文
        String finalPrompt = """
                以下是历史对话:
                %s
                
                当前用户问题:
                %s
                """.formatted(historyText, message);

        // 3. 调用模型
        String answer = callDashScopeAPI(finalPrompt);

        // 4. 保存本轮记录（带异常处理）
        try {
            String recordText = "用户: " + message + "\n助手: " + answer;
            stringRedisTemplate.opsForList().rightPush(redisKey, recordText);

            // 5. 只保留最近3轮
            Long size = stringRedisTemplate.opsForList().size(redisKey);
            if (size != null && size > 3) {
                stringRedisTemplate.opsForList().trim(redisKey, size - 3, size - 1);
            }
        } catch (Exception e) {
            System.out.println("Redis写入失败，跳过会话保存: " + e.getMessage());
        }

        return new ChatResponseVO(message, answer);
    }

    private String callDashScopeAPI(String prompt) {
        String url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "qwen-max");

        Map<String, Object> input = new HashMap<>();
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
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