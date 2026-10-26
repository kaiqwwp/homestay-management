package com.homestay.homestay.controller.api;

import com.homestay.homestay.dto.ai.ChatMessageDto;
import com.homestay.homestay.service.AiChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiChatApiController {

    private final AiChatService aiChatService;

    public AiChatApiController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chat(@RequestBody ChatMessageDto request) {
        String reply = aiChatService.reply(request.getMessage());
        return ResponseEntity.ok(Map.of("reply", reply));
    }
}
