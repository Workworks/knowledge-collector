package com.example.knowledgecollector.web.extraction;

import com.example.knowledgecollector.application.extraction.*;
import com.example.knowledgecollector.web.api.ApiResponse;
import com.example.knowledgecollector.web.api.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/extractions")
public class ContentExtractionController {
    private final ContentExtractionService service;
    public ContentExtractionController(ContentExtractionService service){this.service=service;}
    @PostMapping public ApiResponse<?> execute(@RequestBody ExtractionRequest request,HttpServletRequest servlet){return ApiResponse.success(service.execute(new ContentExtractionCommand(request.articleId(),request.url(),request.method(),null)),cid(servlet));}
    @GetMapping public ApiResponse<?> list(@RequestParam(required=false)Long articleId,@RequestParam(defaultValue="30")int limit,HttpServletRequest request){return ApiResponse.success(service.list(articleId,limit),cid(request));}
    @GetMapping("/{id}") public ApiResponse<?> get(@PathVariable long id,HttpServletRequest request){return ApiResponse.success(service.get(id),cid(request));}
    @PostMapping("/{id}/retry") public ApiResponse<?> retry(@PathVariable long id,HttpServletRequest request){return ApiResponse.success(service.retry(id),cid(request));}
    @GetMapping(value="/{id}/raw",produces=MediaType.TEXT_PLAIN_VALUE) public ResponseEntity<String> raw(@PathVariable long id){var item=service.get(id);return ResponseEntity.ok().contentType(new MediaType("text","plain",java.nio.charset.StandardCharsets.UTF_8)).header("X-Content-Type-Options","nosniff").body(item.rawHtml()==null?"":item.rawHtml());}
    @GetMapping(value="/{id}/screenshot",produces=MediaType.IMAGE_PNG_VALUE) public ResponseEntity<byte[]> screenshot(@PathVariable long id){byte[] value=service.get(id).screenshot();return value==null?ResponseEntity.notFound().build():ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(value);}
    private String cid(HttpServletRequest request){return (String)request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);}
    public record ExtractionRequest(Long articleId,String url,String method){}
}
