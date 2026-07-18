package com.example.knowledgecollector;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles({"test","local"})
class Stage26To27CapabilityIntegrationTest {
    private static final WireMockServer SERVICES=new WireMockServer(options().dynamicPort());
    static { SERVICES.start();
        SERVICES.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/api/tags")).willReturn(okJson("{\"models\":[{\"name\":\"manual-test:latest\"}]}")));
        SERVICES.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlPathEqualTo("/search")).willReturn(okJson("""
            {"results":[{"title":"示例研究数据 API","url":"https://fixture.local/public-api",
            "content":"面向研究人员的公开数据接口，持续更新且无需登录。"}]}
            """)));
    }
    @DynamicPropertySource static void props(DynamicPropertyRegistry r){
        r.add("spring.datasource.url",()->"jdbc:h2:mem:stage26-27-test;DB_CLOSE_DELAY=-1");
        r.add("knowledge-collector.ai.ollama.base-url",SERVICES::baseUrl);
        r.add("knowledge-collector.ai.ollama.model",()->"manual-test:latest");
        r.add("knowledge-collector.search.searxng.enabled",()->true);
        r.add("knowledge-collector.search.searxng.base-url",SERVICES::baseUrl);
    }
    @Autowired TestRestTemplate http; @LocalServerPort int port;
    @AfterAll static void stop(){SERVICES.stop();}

    @Test void configuresTestsDiscoversValidatesImportsAndLogs(){
        JsonNode services=get("/api/v1/capabilities").path("data");
        assertThat(services.size()).isEqualTo(2);
        JsonNode ollama=find(services,"ollama");
        assertThat(ollama.path("endpoint").asText()).isEqualTo(SERVICES.baseUrl());
        JsonNode tested=post("/api/v1/capabilities/ollama/test",null).path("data");
        assertThat(tested.path("available").asBoolean()).isTrue();
        assertThat(tested.path("models").get(0).asText()).isEqualTo("manual-test:latest");

        JsonNode candidates=post("/api/v1/sources/search-discovery",Map.of(
                "topic","开放数据","language","zh-CN","count",3,"sourceType","JSON_API","qualityLevel","权威"))
                .path("data");
        assertThat(candidates.size()).isEqualTo(1);
        long id=candidates.get(0).path("id").asLong();
        assertThat(candidates.get(0).path("recommendationReason").asText()).contains("SearXNG");
        JsonNode verified=post("/api/v1/sources/discovery-candidates/"+id+"/validate",null).path("data");
        assertThat(verified.path("validationStatus").asText()).isEqualTo("VERIFIED");
        JsonNode imported=post("/api/v1/sources/discovery-candidates/import",Map.of("ids",List.of(id))).path("data").get(0);
        assertThat(imported.path("validationStatus").asText()).isEqualTo("IMPORTED");
        assertThat(imported.path("importedSourceId").asLong()).isPositive();
        assertThat(get("/api/v1/capabilities/calls").path("data").size()).isGreaterThanOrEqualTo(3);
        assertThat(http.getForObject(url("/capabilities"),String.class)).contains("第三方能力","调用记录与失败重试");
        assertThat(http.getForObject(url("/sources"),String.class)).contains("SearXNG","批量验证","批量导入");
    }
    private JsonNode find(JsonNode a,String id){for(JsonNode n:a)if(id.equals(n.path("providerId").asText()))return n;throw new AssertionError(id);}
    private JsonNode get(String p){return http.getForObject(url(p),JsonNode.class);}
    private JsonNode post(String p,Object body){HttpHeaders h=new HttpHeaders();h.setContentType(MediaType.APPLICATION_JSON);ResponseEntity<JsonNode> r=http.exchange(url(p),HttpMethod.POST,new HttpEntity<>(body,h),JsonNode.class);assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();return r.getBody();}
    private String url(String p){return "http://127.0.0.1:"+port+p;}
}
