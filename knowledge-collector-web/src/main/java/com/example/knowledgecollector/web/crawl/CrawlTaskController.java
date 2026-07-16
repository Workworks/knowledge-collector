package com.example.knowledgecollector.web.crawl;

import com.example.knowledgecollector.application.crawl.CrawlTaskService;
import com.example.knowledgecollector.web.api.ApiResponse;
import com.example.knowledgecollector.web.api.CorrelationIdFilter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated @RestController @RequestMapping("/api/v1/tasks")
@Tag(name="采集任务",description="RSS/Atom 手动任务与任务项")
public class CrawlTaskController {
    private final CrawlTaskService service;
    public CrawlTaskController(CrawlTaskService service){this.service=service;}
    @GetMapping @Operation(summary="分页查询任务")
    public ApiResponse<?> list(@RequestParam(defaultValue="0")@Min(0)int page,
            @RequestParam(defaultValue="20")@Min(1)@Max(100)int size,HttpServletRequest r){
        return ApiResponse.success(service.findPage(page,size),cid(r));}
    @GetMapping("/{id}") public ApiResponse<?> get(@PathVariable long id,HttpServletRequest r){
        return ApiResponse.success(service.get(id),cid(r));}
    @GetMapping("/{id}/items") public ApiResponse<?> items(@PathVariable long id,HttpServletRequest r){
        service.get(id);return ApiResponse.success(service.items(id),cid(r));}
    @PostMapping("/{id}/retry") @Operation(summary="重试失败任务")
    public ApiResponse<?> retry(@PathVariable long id,HttpServletRequest r){
        return ApiResponse.success(service.retry(id),cid(r));}
    @PostMapping("/{id}/cancel") @Operation(summary="取消尚未运行的任务")
    public ApiResponse<?> cancel(@PathVariable long id,HttpServletRequest r){
        return ApiResponse.success(service.cancel(id),cid(r));}
    private String cid(HttpServletRequest r){return(String)r.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);}
}
