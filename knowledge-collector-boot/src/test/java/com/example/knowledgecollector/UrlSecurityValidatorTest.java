package com.example.knowledgecollector;

import com.example.knowledgecollector.provider.security.DefaultUrlSecurityValidator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UrlSecurityValidatorTest {
    private final DefaultUrlSecurityValidator validator = new DefaultUrlSecurityValidator(false);

    @Test
    void blocksNonHttpAndLoopbackAddresses() {
        assertThat(validator.validate("file:///etc/passwd").valid()).isFalse();
        assertThat(validator.validate("http://127.0.0.1/private").valid()).isFalse();
    }

    @Test
    void acceptsPublicHttpAddress() {
        assertThat(validator.validate("https://example.com/articles").valid()).isTrue();
    }
}
