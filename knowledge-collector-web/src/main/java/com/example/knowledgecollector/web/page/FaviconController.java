package com.example.knowledgecollector.web.page;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Controller
public class FaviconController {
    private static final byte[] ICON = """
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64">
              <rect width="64" height="64" rx="14" fill="#2563eb"/>
              <path d="M18 16h25a5 5 0 0 1 5 5v31H23a5 5 0 0 1-5-5V16Z" fill="#fff"/>
              <path d="M23 16v31a5 5 0 0 0 5 5" fill="none" stroke="#bfdbfe" stroke-width="4"/>
              <path d="M29 25h12M29 32h12M29 39h8" stroke="#2563eb" stroke-width="4" stroke-linecap="round"/>
            </svg>
            """.getBytes(StandardCharsets.UTF_8);

    @GetMapping("/favicon.ico")
    @ResponseBody
    public ResponseEntity<byte[]> favicon() {
        return ResponseEntity.ok()
                .contentType(MediaType.valueOf("image/svg+xml"))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .body(ICON);
    }
}
