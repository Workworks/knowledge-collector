package com.example.knowledgecollector.provider.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

@Component
public class TlsContextFactory {
    private static final Logger log = LoggerFactory.getLogger(TlsContextFactory.class);
    private final SSLContext sslContext;

    public TlsContextFactory(
            @Value("${knowledge-collector.network.trust-system-store:true}") boolean trustSystemStore,
            @Value("${knowledge-collector.network.additional-ca-file:}") String additionalCaFile) {
        this.sslContext = build(trustSystemStore, additionalCaFile);
    }

    public SSLContext sslContext() {
        return sslContext;
    }

    private SSLContext build(boolean trustSystemStore, String additionalCaFile) {
        try {
            List<X509TrustManager> managers = new ArrayList<>();
            managers.add(loadDefault());
            if (trustSystemStore && System.getProperty("os.name", "").toLowerCase().contains("windows")) {
                try {
                    managers.add(loadKeyStore("Windows-ROOT", null));
                    log.info("TLS trust includes Windows system root certificates");
                } catch (Exception exception) {
                    log.warn("Unable to load Windows system root certificates; using JDK trust store", exception);
                }
            }
            if (additionalCaFile != null && !additionalCaFile.isBlank()) {
                managers.add(loadPem(Path.of(additionalCaFile).toAbsolutePath().normalize()));
                log.info("TLS trust includes additional CA file {}", additionalCaFile);
            }
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[]{new CompositeTrustManager(managers)}, null);
            return context;
        } catch (Exception exception) {
            throw new IllegalStateException("TLS 信任库初始化失败", exception);
        }
    }

    private X509TrustManager loadDefault() throws Exception {
        return loadKeyStore(TrustManagerFactory.getDefaultAlgorithm(), null);
    }

    private X509TrustManager loadKeyStore(String typeOrAlgorithm, KeyStore supplied) throws Exception {
        KeyStore keyStore = supplied;
        String algorithm = TrustManagerFactory.getDefaultAlgorithm();
        if ("Windows-ROOT".equals(typeOrAlgorithm)) {
            keyStore = KeyStore.getInstance("Windows-ROOT");
            keyStore.load(null, null);
        } else {
            algorithm = typeOrAlgorithm;
        }
        TrustManagerFactory factory = TrustManagerFactory.getInstance(algorithm);
        factory.init(keyStore);
        for (TrustManager manager : factory.getTrustManagers()) {
            if (manager instanceof X509TrustManager x509) {
                return x509;
            }
        }
        throw new IllegalStateException("未找到 X509 TrustManager");
    }

    private X509TrustManager loadPem(Path path) throws Exception {
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("附加 CA 文件不存在：" + path);
        }
        CertificateFactory certificates = CertificateFactory.getInstance("X.509");
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null);
        try (var input = Files.newInputStream(path)) {
            int index = 0;
            for (var certificate : certificates.generateCertificates(input)) {
                keyStore.setCertificateEntry("additional-ca-" + index++, certificate);
            }
            if (index == 0) {
                throw new IllegalArgumentException("附加 CA 文件未包含 X.509 证书：" + path);
            }
        }
        return loadKeyStore(TrustManagerFactory.getDefaultAlgorithm(), keyStore);
    }

    private static final class CompositeTrustManager extends X509ExtendedTrustManager {
        private final List<X509TrustManager> delegates;

        private CompositeTrustManager(List<X509TrustManager> delegates) {
            this.delegates = List.copyOf(delegates);
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            verify(manager -> manager.checkClientTrusted(chain, authType));
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            verify(manager -> manager.checkServerTrusted(chain, authType));
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket)
                throws CertificateException {
            verify(manager -> {
                if (manager instanceof X509ExtendedTrustManager extended) {
                    extended.checkClientTrusted(chain, authType, socket);
                } else {
                    manager.checkClientTrusted(chain, authType);
                }
            });
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket)
                throws CertificateException {
            verify(manager -> {
                if (manager instanceof X509ExtendedTrustManager extended) {
                    extended.checkServerTrusted(chain, authType, socket);
                } else {
                    manager.checkServerTrusted(chain, authType);
                }
            });
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType,
                                       javax.net.ssl.SSLEngine engine) throws CertificateException {
            verify(manager -> {
                if (manager instanceof X509ExtendedTrustManager extended) {
                    extended.checkClientTrusted(chain, authType, engine);
                } else {
                    manager.checkClientTrusted(chain, authType);
                }
            });
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType,
                                       javax.net.ssl.SSLEngine engine) throws CertificateException {
            verify(manager -> {
                if (manager instanceof X509ExtendedTrustManager extended) {
                    extended.checkServerTrusted(chain, authType, engine);
                } else {
                    manager.checkServerTrusted(chain, authType);
                }
            });
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return delegates.stream().flatMap(manager -> List.of(manager.getAcceptedIssuers()).stream())
                    .toArray(X509Certificate[]::new);
        }

        private void verify(TrustCheck check) throws CertificateException {
            CertificateException last = null;
            for (X509TrustManager delegate : delegates) {
                try {
                    check.verify(delegate);
                    return;
                } catch (CertificateException exception) {
                    last = exception;
                }
            }
            throw last == null ? new CertificateException("证书链不受信任") : last;
        }

        @FunctionalInterface
        private interface TrustCheck {
            void verify(X509TrustManager manager) throws CertificateException;
        }
    }
}
