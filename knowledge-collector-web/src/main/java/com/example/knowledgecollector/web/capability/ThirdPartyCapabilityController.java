package com.example.knowledgecollector.web.capability;

import com.example.knowledgecollector.application.capability.ThirdPartyCapabilityService;
import com.example.knowledgecollector.web.api.ApiResponse;
import com.example.knowledgecollector.web.api.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/capabilities")
public class ThirdPartyCapabilityController {
    private final ThirdPartyCapabilityService service;
    public ThirdPartyCapabilityController(ThirdPartyCapabilityService service) { this.service=service; }
    @GetMapping public ApiResponse<?> list(HttpServletRequest r) { return ApiResponse.success(service.list(),cid(r)); }
    @PutMapping("/{providerId}") public ApiResponse<?> save(@PathVariable String providerId,
            @Valid @RequestBody ThirdPartyServiceRequest body,HttpServletRequest r) {
        return ApiResponse.success(service.save(providerId,body.command()),cid(r));
    }
    @PostMapping("/{providerId}/test") public ApiResponse<?> test(@PathVariable String providerId,HttpServletRequest r) {
        return ApiResponse.success(service.test(providerId,null),cid(r));
    }
    @GetMapping("/calls") public ApiResponse<?> calls(@RequestParam(defaultValue="100") int limit,HttpServletRequest r) {
        return ApiResponse.success(service.calls(limit),cid(r));
    }
    @PostMapping("/calls/{id}/retry") public ApiResponse<?> retry(@PathVariable long id,HttpServletRequest r) {
        return ApiResponse.success(service.retry(id),cid(r));
    }
    private String cid(HttpServletRequest r){Object v=r.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);return v==null?"unknown":v.toString();}
}
