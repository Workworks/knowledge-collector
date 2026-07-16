package com.example.knowledgecollector.web.page;

import com.example.knowledgecollector.application.system.SystemStatusQuery;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final SystemStatusQuery statusQuery;

    public HomeController(SystemStatusQuery statusQuery) {
        this.statusQuery = statusQuery;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("status", statusQuery.getStatus());
        return "index";
    }

    @GetMapping("/test-console")
    public String testConsole() {
        return "test-console";
    }
}
