package com.example.knowledgecollector;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties="spring.datasource.url=jdbc:h2:mem:stage14;DB_CLOSE_DELAY=-1")
@ActiveProfiles({"test","local"})
class Stage14ManagementIntegrationTest {
    @Autowired TestRestTemplate rest; @LocalServerPort int port;

    @Test
    void refreshesSourcesFiltersRecentTasksAndOrganizesArchive() {
        String suffix=UUID.randomUUID().toString().substring(0,8);
        JsonNode source=request(HttpMethod.POST,"/api/v1/sources",Map.ofEntries(
                Map.entry("code","S14_"+suffix),Map.entry("name","Stage14 采集员"),Map.entry("type","JSON_API"),
                Map.entry("homeUrl","https://fixture.local"),Map.entry("feedUrl","https://fixture.local/"+suffix),
                Map.entry("language","zh-CN"),Map.entry("charset","UTF-8"),Map.entry("userAgent","Stage14/1.0"),
                Map.entry("timeoutSeconds",5),Map.entry("maxRetries",0),Map.entry("requestIntervalMillis",0),
                Map.entry("obeyRobots",true),Map.entry("fetchFullContent",true),Map.entry("summaryOnly",false),
                Map.entry("saveSnapshot",false),Map.entry("enabled",true),Map.entry("notes","stage14"),Map.entry("topicIds",new long[]{})));
        long sourceId=source.path("data").path("id").asLong();
        JsonNode health=request(HttpMethod.POST,"/api/v1/sources/"+sourceId+"/health",null);
        assertThat(health.path("data").path("healthStatus").asText()).isEqualTo("VERIFIED");
        assertThat(health.path("data").path("lastHealthCheckedAt").asText()).isNotBlank();
        JsonNode task=request(HttpMethod.POST,"/api/v1/sources/"+sourceId+"/crawl",null);
        long articleId=get("/api/v1/articles?sourceId="+sourceId+"&archived=false").path("data").path("content").get(0).path("id").asLong();
        JsonNode filtered=get("/api/v1/tasks?sourceId="+sourceId+"&status=SUCCESS&from="+LocalDate.now().minusDays(6)+"&to="+LocalDate.now());
        assertThat(filtered.path("data").path("totalElements").asInt()).isEqualTo(1);
        assertThat(task.path("data").path("id").asLong()).isPositive();
        request(HttpMethod.PATCH,"/api/v1/articles/"+articleId+"/reading/state",Map.of("readingStatus","ARCHIVED","archived",true));
        assertThat(get("/api/v1/articles?archived=true").path("data").path("totalElements").asInt()).isEqualTo(1);
        JsonNode rule=request(HttpMethod.POST,"/api/v1/archive-rules",Map.of("name","优质归档","minQuality",60,"sortOrder",1,"enabled",true));
        assertThat(rule.path("data").path("name").asText()).isEqualTo("优质归档");
        assertThat(rest.getForObject(url("/articles/archive"),String.class)).contains("归档资料库","整理规则");
        assertThat(rest.getForObject(url("/tasks"),String.class)).contains("开始日期","最近 7 天");
    }

    private JsonNode get(String path){return rest.getForObject(url(path),JsonNode.class);}
    private JsonNode request(HttpMethod method,String path,Object body){HttpHeaders h=new HttpHeaders();h.setContentType(MediaType.APPLICATION_JSON);ResponseEntity<JsonNode> r=rest.exchange(url(path),method,new HttpEntity<>(body,h),JsonNode.class);assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();return r.getBody();}
    private String url(String path){return "http://127.0.0.1:"+port+path;}
}
