package com.example.knowledgecollector.web.evidence;

import com.example.knowledgecollector.application.evidence.EvidenceFileService;
import com.example.knowledgecollector.web.api.ApiResponse;
import com.example.knowledgecollector.web.api.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/evidence-files")
public class EvidenceFileController {
    private final EvidenceFileService service; public EvidenceFileController(EvidenceFileService service){this.service=service;}
    @GetMapping public ApiResponse<?> list(@RequestParam(required=false)String ownerType,@RequestParam(required=false)Long ownerId,HttpServletRequest request){return ApiResponse.success(service.list(ownerType,ownerId),cid(request));}
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public ApiResponse<?> upload(@RequestParam String ownerType,@RequestParam long ownerId,@RequestParam(defaultValue="SUPPLEMENT")String fileKind,@RequestPart("file") MultipartFile file,HttpServletRequest request)throws Exception{return ApiResponse.success(service.store(ownerType,ownerId,fileKind,file.getOriginalFilename(),file.getContentType()==null?MediaType.APPLICATION_OCTET_STREAM_VALUE:file.getContentType(),file.getBytes()),cid(request));}
    @GetMapping("/{id}/download") public ResponseEntity<byte[]> download(@PathVariable long id){var item=service.download(id);return ResponseEntity.ok().contentType(MediaType.parseMediaType(item.file().contentType())).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(item.file().fileName(),java.nio.charset.StandardCharsets.UTF_8).build().toString()).body(item.bytes());}
    private String cid(HttpServletRequest request){return (String)request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME);}
}
