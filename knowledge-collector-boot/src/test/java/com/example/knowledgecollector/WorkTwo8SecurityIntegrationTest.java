package com.example.knowledgecollector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WorkTwo8SecurityIntegrationTest {
    private static final String ADMIN_PASSWORD = "AdminSecure123!";
    private static WireMockServer healthServer;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("knowledge-collector.security.enabled", () -> true);
        registry.add("knowledge-collector.security.initial-admin-username", () -> "admin");
        registry.add("knowledge-collector.security.initial-admin-password", () -> ADMIN_PASSWORD);
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:worktwo8-security;DB_CLOSE_DELAY=-1");
        registry.add("knowledge-collector.storage.local.base-path", () -> System.getProperty("java.io.tmpdir") + "/knowledge-collector-worktwo8");
    }

    @BeforeAll static void startHealthServer() {
        healthServer = new WireMockServer(0);
        healthServer.start();
        healthServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.get(urlEqualTo("/health"))
                .willReturn(okJson("{\"status\":\"ok\"}")));
    }

    @AfterAll static void stopHealthServer() { healthServer.stop(); }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;

    @Test
    void authenticationRbacUsersAndExternalSystemsWorkEndToEnd() throws Exception {
        mvc.perform(get("/articles")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mvc.perform(get("/api/v1/articles")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("AUTH-401"));

        MockHttpSession admin = login("admin", ADMIN_PASSWORD);
        mvc.perform(get("/users").session(admin)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("用户管理")));

        String createBody = """
                {"username":"researcher","displayName":"研究员示例","password":"Researcher123!","role":"USER","enabled":true}
                """;
        String created = mvc.perform(post("/api/v1/users").session(admin).contentType(MediaType.APPLICATION_JSON).content(createBody))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.username").value("researcher"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("passwordHash"))))
                .andReturn().getResponse().getContentAsString();
        long userId = json.readTree(created).path("data").path("id").asLong();

        String hash = jdbc.queryForObject("select password_hash from app_user where id=?", String.class, userId);
        assertThat(hash).startsWith("$2").doesNotContain("Researcher123!");

        MockHttpSession user = login("researcher", "Researcher123!");
        mvc.perform(get("/articles").session(user)).andExpect(status().isOk());
        mvc.perform(get("/users").session(user)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/access-denied"));
        mvc.perform(get("/api/v1/users").session(user)).andExpect(status().isForbidden()).andExpect(jsonPath("$.error.code").value("AUTH-403"));
        mvc.perform(get("/").session(user)).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("用户管理"))));

        mvc.perform(put("/api/v1/users/{id}", userId).session(admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\":\"研究员示例\",\"role\":\"USER\",\"enabled\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.enabled").value(false));
        mvc.perform(post("/login").with(csrf()).param("username", "researcher").param("password", "Researcher123!"))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?error"));

        JsonNode systems = json.readTree(mvc.perform(get("/api/v1/external-systems").session(admin)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).path("data");
        assertThat(systems.size()).isGreaterThanOrEqualTo(10);
        long grafanaId = 0;
        for (JsonNode item : systems) if ("GRAFANA".equals(item.path("code").asText())) grafanaId = item.path("id").asLong();
        assertThat(grafanaId).isPositive();

        String systemBody = """
                {"code":"GRAFANA","name":"Grafana","type":"OBSERVABILITY","description":"测试仪表盘","icon":"📊",
                 "accessUrl":"http://127.0.0.1:3000","healthUrl":"%s/health","openMode":"NEW_TAB","enabled":true,"allowedRole":"ADMIN","sortOrder":20}
                """.formatted(healthServer.baseUrl());
        mvc.perform(put("/api/v1/external-systems/{id}", grafanaId).session(admin).contentType(MediaType.APPLICATION_JSON).content(systemBody)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/external-systems/{id}/test", grafanaId).session(admin)).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("HEALTHY"));
        mvc.perform(get("/api/v1/external-systems/{id}/open", grafanaId).session(admin)).andExpect(status().isOk()).andExpect(jsonPath("$.data.openMode").value("NEW_TAB"));
        mvc.perform(get("/api/v1/external-systems").session(user)).andExpect(status().isForbidden());

        Integer audits = jdbc.queryForObject("select count(*) from audit_log where action_type in ('USER_CREATE','USER_UPDATE','EXTERNAL_SYSTEM_UPDATE','EXTERNAL_SYSTEM_TEST')", Integer.class);
        assertThat(audits).isGreaterThanOrEqualTo(4);

        mvc.perform(post("/logout").with(csrf()).session(admin)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?logout"));
        mvc.perform(get("/users").session(admin)).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }

    private MockHttpSession login(String username, String password) throws Exception {
        HttpSession session = mvc.perform(post("/login").with(csrf()).param("username", username).param("password", password))
                .andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/"))
                .andReturn().getRequest().getSession(false);
        assertThat(session).isNotNull();
        return (MockHttpSession) session;
    }
}
