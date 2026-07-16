package com.example.knowledgecollector;

import com.example.knowledgecollector.provider.web.TlsContextFactory;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TlsContextFactoryTest {
    @Test
    void loadsAdditionalPemCertificateWithoutDisablingValidation() throws Exception {
        Path certificate = Path.of(getClass().getResource("/fixtures/tls/test-ca.pem").toURI());
        TlsContextFactory factory = new TlsContextFactory(false, certificate.toString());
        assertThat(factory.sslContext()).isNotNull();
        assertThat(factory.sslContext().getProtocol()).isEqualTo("TLS");
    }

    @Test
    void failsFastWhenConfiguredCertificateFileDoesNotExist() {
        assertThatThrownBy(() -> new TlsContextFactory(false, "missing-test-ca.pem"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TLS 信任库初始化失败");
    }
}
