package com.example.knowledgecollector;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test","local"})
class Stage28To29ExtractionEvidenceIntegrationTest {
    private static final WireMockServer REMOTE=new WireMockServer(options().dynamicPort());
    static {REMOTE.start();
        REMOTE.stubFor(post(urlEqualTo("/v1/scrape")).willReturn(okJson("""
          {"data":{"html":"<html><head><title>Firecrawl 验收文章</title><meta name='author' content='测试作者'></head><body><article><h1>Firecrawl 验收文章</h1><p>这是可验证的正文内容，用于确认 Firecrawl 抓取结果能够写回文章资料库。</p></article></body></html>","markdown":"这是可验证的正文内容","metadata":{"title":"Firecrawl 验收文章","author":"测试作者","sourceURL":"https://example.org/firecrawl"}}}
          """)));
        REMOTE.stubFor(post(urlEqualTo("/render")).willReturn(okJson("""
          {"finalUrl":"https://example.org/dynamic","title":"Playwright 动态文章","author":"浏览器作者","html":"<html><body><article><h1>Playwright 动态文章</h1><p>浏览器渲染后的正文可正常提取并生成截图证据。</p></article></body></html>","screenshotBase64":"iVBORw0KGgo="}
          """)));
        REMOTE.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/health")).willReturn(okJson("{\"status\":\"UP\"}")));
    }
    @DynamicPropertySource static void props(DynamicPropertyRegistry r){
        r.add("spring.datasource.url",()->"jdbc:h2:mem:stage28-29-test;DB_CLOSE_DELAY=-1");
        r.add("knowledge-collector.network.allow-loopback",()->true);
        r.add("knowledge-collector.extraction.firecrawl.enabled",()->true);r.add("knowledge-collector.extraction.firecrawl.base-url",REMOTE::baseUrl);
        r.add("knowledge-collector.extraction.playwright.enabled",()->true);r.add("knowledge-collector.extraction.playwright.base-url",REMOTE::baseUrl);
        r.add("knowledge-collector.storage.minio.enabled",()->true);r.add("knowledge-collector.storage.minio.endpoint",()->"memory://stage29");r.add("knowledge-collector.storage.minio.bucket",()->"test-evidence");
    }
    @Autowired TestRestTemplate http;@Autowired JdbcClient jdbc;@LocalServerPort int port;long articleId;
    @BeforeEach void seed(){if(jdbc.sql("select count(*) from crawl_source where source_code='stage28'").query(Long.class).single()==0){OffsetDateTime now=OffsetDateTime.now();jdbc.sql("insert into crawl_source(source_code,source_name,source_type,home_url,feed_url,user_agent,created_at,updated_at) values('stage28','Stage 28 示例源','MANUAL_URL','https://example.org','https://example.org/old','test',:now,:now)").param("now",now).update();long source=jdbc.sql("select id from crawl_source where source_code='stage28'").query(Long.class).single();jdbc.sql("insert into article(source_id,title,original_url,normalized_url,url_hash,language,first_collected_at,last_collected_at,created_at,updated_at) values(:source,'旧标题','https://example.org/old','https://example.org/old',:hash,'zh-CN',:now,:now,:now,:now)").param("source",source).param("hash","a".repeat(64)).param("now",now).update();}articleId=jdbc.sql("select id from article where url_hash=:hash").param("hash","a".repeat(64)).query(Long.class).single();}
    @AfterAll static void stop(){REMOTE.stop();}

    @Test void extractsRetriesWritesBackAndStoresVersionedEvidence(){
        JsonNode fire=postJson("/api/v1/extractions",Map.of("articleId",articleId,"url","https://example.org/firecrawl","method","FIRECRAWL")).path("data");
        assertThat(fire.path("status").asText()).isEqualTo("SUCCESS");assertThat(fire.path("contentLength").asInt()).isPositive();
        assertThat(get("/api/v1/articles/"+articleId).path("data").path("title").asText()).isEqualTo("Firecrawl 验收文章");
        JsonNode browser=postJson("/api/v1/extractions",Map.of("articleId",articleId,"url","https://example.org/dynamic","method","PLAYWRIGHT")).path("data");
        assertThat(browser.path("screenshot").asText()).isNotBlank();
        JsonNode retried=postJson("/api/v1/extractions/"+browser.path("id").asLong()+"/retry",null).path("data");
        assertThat(retried.path("retryOfId").asLong()).isEqualTo(browser.path("id").asLong());

        upload("note-v1.txt","第一版补充证据");upload("note-v2.txt","第二版补充证据");
        JsonNode files=get("/api/v1/evidence-files?ownerType=ARTICLE&ownerId="+articleId).path("data");
        assertThat(files.size()).isGreaterThanOrEqualTo(4);
        assertThat(files.toString()).contains("RAW_HTML","SCREENSHOT","\"versionNo\":2");
        ResponseEntity<byte[]> download=http.getForEntity(url("/api/v1/evidence-files/"+files.get(0).path("id").asLong()+"/download"),byte[].class);
        assertThat(download.getStatusCode().is2xxSuccessful()).isTrue();assertThat(download.getBody()).isNotEmpty();
        assertThat(http.getForObject(url("/extractions?articleId="+articleId),String.class)).contains("网页提取工作台","Firecrawl","Playwright");
        assertThat(http.getForObject(url("/evidence-files?ownerType=ARTICLE&ownerId="+articleId),String.class)).contains("文件与快照","上传到 MinIO");
    }
    private void upload(String name,String text){var body=new LinkedMultiValueMap<String,Object>();body.add("ownerType","ARTICLE");body.add("ownerId",String.valueOf(articleId));body.add("fileKind","SUPPLEMENT");body.add("file",new ByteArrayResource(text.getBytes(StandardCharsets.UTF_8)){@Override public String getFilename(){return name;}});HttpHeaders h=new HttpHeaders();h.setContentType(MediaType.MULTIPART_FORM_DATA);var response=http.exchange(url("/api/v1/evidence-files"),HttpMethod.POST,new HttpEntity<>(body,h),JsonNode.class);assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();}
    private JsonNode get(String path){return http.getForObject(url(path),JsonNode.class);}
    private JsonNode postJson(String path,Object body){HttpHeaders h=new HttpHeaders();h.setContentType(MediaType.APPLICATION_JSON);var r=http.exchange(url(path),HttpMethod.POST,new HttpEntity<>(body,h),JsonNode.class);assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();return r.getBody();}
    private String url(String path){return "http://127.0.0.1:"+port+path;}
}
