package com.example.knowledgecollector.web.security;
import org.springframework.stereotype.Controller;import org.springframework.web.bind.annotation.GetMapping;
@Controller public class SecurityPageController {@GetMapping("/login")public String login(){return "login";}@GetMapping("/access-denied")public String denied(){return "access-denied";}@GetMapping("/users")public String users(){return "users";}@GetMapping("/profile")public String profile(){return "profile";}@GetMapping("/third-party-systems")public String systems(){return "third-party-systems";}}
