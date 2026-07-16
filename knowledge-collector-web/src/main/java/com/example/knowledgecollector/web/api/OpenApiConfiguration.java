package com.example.knowledgecollector.web.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    OpenAPI knowledgeCollectorOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Knowledge Collector API")
                .version("1.0.0")
                .description("本地资料采集与阅读管理系统 REST API。错误响应使用统一 ApiResponse 结构。"));
    }
}
