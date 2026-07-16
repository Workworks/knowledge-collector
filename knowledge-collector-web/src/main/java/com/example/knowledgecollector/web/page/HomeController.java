package com.example.knowledgecollector.web.page;

import com.example.knowledgecollector.application.system.SystemStatusQuery;
import com.example.knowledgecollector.application.operations.OperationsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final SystemStatusQuery statusQuery;
    private final OperationsService operations;

    public HomeController(SystemStatusQuery statusQuery, OperationsService operations) {
        this.statusQuery = statusQuery;
        this.operations = operations;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("status", statusQuery.getStatus());
        model.addAttribute("dashboard", operations.dashboard());
        return "index";
    }

    @GetMapping("/test-console")
    public String testConsole() {
        return "test-console";
    }

    @GetMapping("/operations")
    public String operations() {
        return "operations";
    }
}
