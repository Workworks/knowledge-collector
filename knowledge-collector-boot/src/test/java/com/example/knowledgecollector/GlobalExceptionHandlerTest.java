package com.example.knowledgecollector;

import com.example.knowledgecollector.application.exception.BusinessRuleException;
import com.example.knowledgecollector.web.api.CorrelationIdFilter;
import com.example.knowledgecollector.web.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void convertsBusinessExceptionToStableApiError() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(CorrelationIdFilter.ATTRIBUTE_NAME, "stage3-test-correlation");

        var response = new GlobalExceptionHandler().handleBusinessRule(
                new BusinessRuleException("BUS-001", "操作不符合业务规则"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().error().code()).isEqualTo("BUS-001");
        assertThat(response.getBody().correlationId()).isEqualTo("stage3-test-correlation");
    }
}
