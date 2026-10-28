package com.smartattend.controller;

import com.smartattend.dto.ChatMessageRequest;
import com.smartattend.dto.ChatMessageResponse;
import com.smartattend.security.UserDetailsImpl;
import com.smartattend.service.GeminiChatbotService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final GeminiChatbotService chatbotService;

    public ChatbotController(GeminiChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/message")
    public ResponseEntity<ChatMessageResponse> handleMessage(
            @Valid @RequestBody ChatMessageRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        ChatMessageResponse response = chatbotService.processUserMessage(request.getMessage(), userDetails);
        return ResponseEntity.ok(response);
    }
}
