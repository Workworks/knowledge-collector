package com.example.knowledgecollector.web.page;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AiChatPageController {
    @GetMapping("/ai-chat")
    public String aiChat() {
        return "ai-chat";
    }
}
