package com.example.knowledgecollector.web.page;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPageController {

    @GetMapping("/topics")
    public String topics() {
        return "topics";
    }

    @GetMapping("/sources")
    public String sources() {
        return "sources";
    }
}
