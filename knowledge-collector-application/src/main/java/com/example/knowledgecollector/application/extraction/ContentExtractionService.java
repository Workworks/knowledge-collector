package com.example.knowledgecollector.application.extraction;

import com.example.knowledgecollector.application.article.ArticleService;
import com.example.knowledgecollector.application.capability.ThirdPartyCapabilityService;
import com.example.knowledgecollector.application.evidence.EvidenceFileService;
import com.example.knowledgecollector.application.exception.BusinessRuleException;
import com.example.knowledgecollector.capability.extraction.ContentExtractionProvider;
import com.example.knowledgecollector.capability.security.UrlSecurityValidator;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ContentExtractionService {
    private final ContentExtractionGateway gateway;
    private final ArticleService articles;
    private final ThirdPartyCapabilityService capabilities;
    private final EvidenceFileService evidence;
    private final UrlSecurityValidator urlSecurity;
    private final List<ContentExtractionProvider> providers;
    public ContentExtractionService(ContentExtractionGateway gateway, ArticleService articles,
            ThirdPartyCapabilityService capabilities, EvidenceFileService evidence, UrlSecurityValidator urlSecurity,
            List<ContentExtractionProvider> providers) {
        this.gateway=gateway;this.articles=articles;this.capabilities=capabilities;this.evidence=evidence;this.urlSecurity=urlSecurity;this.providers=providers;
    }
    public ContentExtractionView execute(ContentExtractionCommand command) {
        String method=normalize(command.method());
        String providerId=switch(method){case "DIRECT"->"direct";case "FIRECRAWL"->"firecrawl";case "PLAYWRIGHT"->"playwright";default->throw new IllegalStateException();};
        String url=command.url();
        if((url==null||url.isBlank())&&command.articleId()!=null) url=articles.get(command.articleId()).originalUrl();
        if(url==null||!url.matches("https?://.+")) throw new BusinessRuleException("EXTRACTION-URL-INVALID","请输入 HTTP/HTTPS 网页地址");
        var validation=urlSecurity.validate(url);
        if(!validation.valid()) throw new BusinessRuleException(validation.errorCode(),validation.message());
        url=validation.normalizedUri().toString();
        ContentExtractionProvider provider=providers.stream().filter(p->p.id().equalsIgnoreCase(providerId)).findFirst()
                .orElseThrow(()->new BusinessRuleException("EXTRACTION-PROVIDER-NOT-FOUND","未加载抓取 Provider："+providerId));
        long id=gateway.start(new ContentExtractionCommand(command.articleId(),url,method,command.retryOfId()),providerId);
        long started=System.currentTimeMillis();
        try {
            String finalUrl=url;
            var result="direct".equals(providerId)
                    ? provider.extract(new ContentExtractionProvider.ExtractionRequest(finalUrl,30,Map.of()))
                    : capabilities.invoke(providerId,"EXTRACT_CONTENT","网页提取",finalUrl,
                        ()->provider.extract(new ContentExtractionProvider.ExtractionRequest(finalUrl,45,Map.of())),
                        value->"正文 "+length(value.contentText())+" 字");
            if(result.contentText()==null||result.contentText().isBlank()) throw new IllegalStateException("ARTICLE-CONTENT-NOT-FOUND: 未提取到可用正文");
            long duration=System.currentTimeMillis()-started;
            gateway.succeed(id,result,duration);
            if(command.articleId()!=null) gateway.updateArticle(command.articleId(),result);
            long ownerId=command.articleId()==null?id:command.articleId();
            String ownerType=command.articleId()==null?"EXTRACTION":"ARTICLE";
            try {
                if(result.rawHtml()!=null) evidence.store(ownerType,ownerId,"RAW_HTML","webpage-"+id+".html","text/html;charset=UTF-8",result.rawHtml().getBytes(StandardCharsets.UTF_8));
                if(result.screenshot()!=null) evidence.store(ownerType,ownerId,"SCREENSHOT","screenshot-"+id+".png","image/png",result.screenshot());
            } catch(Exception ignored) { /* MinIO is optional for extraction; job keeps embedded evidence. */ }
        } catch(Exception e) { gateway.fail(id,safe(e),System.currentTimeMillis()-started); }
        return gateway.get(id);
    }
    public ContentExtractionView retry(long id){var old=gateway.get(id);return execute(new ContentExtractionCommand(old.articleId(),old.requestedUrl(),old.method(),old.id()));}
    public ContentExtractionView get(long id){return gateway.get(id);}
    public List<ContentExtractionView> list(Long articleId,int limit){return gateway.list(articleId,Math.max(1,Math.min(limit,100)));}
    private String normalize(String value){String result=value==null?"DIRECT":value.trim().toUpperCase(Locale.ROOT);if(!List.of("DIRECT","FIRECRAWL","PLAYWRIGHT").contains(result))throw new BusinessRuleException("EXTRACTION-METHOD-INVALID","抓取方式仅支持 DIRECT、FIRECRAWL、PLAYWRIGHT");return result;}
    private int length(String value){return value==null?0:value.length();}
    private String safe(Exception e){return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
}
