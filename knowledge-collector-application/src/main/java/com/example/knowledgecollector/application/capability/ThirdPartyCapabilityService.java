package com.example.knowledgecollector.application.capability;

import com.example.knowledgecollector.application.exception.BusinessRuleException;
import com.example.knowledgecollector.capability.management.ManagedCapabilityProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
public class ThirdPartyCapabilityService {
    private final ThirdPartyCapabilityGateway gateway;
    private final List<ManagedCapabilityProvider> providers;

    public ThirdPartyCapabilityService(ThirdPartyCapabilityGateway gateway,
                                       List<ManagedCapabilityProvider> providers) {
        this.gateway = gateway;
        this.providers = providers;
    }

    public List<ThirdPartyServiceView> list() {
        gateway.listServices().stream().filter(ThirdPartyServiceView::userConfigured).forEach(this::apply);
        return gateway.listServices().stream().map(this::effective).map(this::withUsages).toList();
    }

    @Transactional
    public ThirdPartyServiceView save(String providerId, ThirdPartyServiceCommand command) {
        ManagedCapabilityProvider provider = provider(providerId);
        if (command.endpoint() == null || command.endpoint().isBlank())
            throw new BusinessRuleException("CAPABILITY-ENDPOINT-EMPTY", "服务地址不能为空");
        ThirdPartyServiceView saved = gateway.save(providerId, command);
        apply(saved, provider);
        return withUsages(saved);
    }

    public ManagedCapabilityProvider.ConnectionResult test(String providerId, Long retryOfId) {
        ThirdPartyServiceView config = effective(gateway.findService(providerId)
                .orElseThrow(() -> new BusinessRuleException("CAPABILITY-NOT-FOUND", "第三方服务不存在：" + providerId)));
        ManagedCapabilityProvider provider = provider(providerId);
        apply(config, provider);
        long started = System.currentTimeMillis();
        long logId = gateway.beginCall(providerId, "TEST_CONNECTION", "第三方能力管理",
                "测试 " + config.endpoint(), retryOfId);
        try {
            var result = provider.testConnection();
            gateway.updateHealth(providerId, result.available(), result.message());
            gateway.finishCall(logId, result.available(), result.message(),
                    result.available() ? null : result.message(), System.currentTimeMillis() - started);
            return result;
        } catch (Exception exception) {
            String message = safe(exception);
            gateway.updateHealth(providerId, false, message);
            gateway.finishCall(logId, false, null, message, System.currentTimeMillis() - started);
            throw new BusinessRuleException("CAPABILITY-CONNECTION-FAILED", message);
        }
    }

    public List<ThirdPartyCallLogView> calls(int limit) { return gateway.listCalls(Math.max(1, Math.min(limit, 200))); }

    public ManagedCapabilityProvider.ConnectionResult retry(long logId) {
        ThirdPartyCallLogView log = gateway.getCall(logId);
        return test(log.providerId(), log.id());
    }

    public String defaultProvider(String serviceType) {
        ThirdPartyServiceView selected = gateway.listServices().stream().map(this::effective)
                .filter(item -> item.enabled() && item.serviceType().equalsIgnoreCase(serviceType) && item.defaultProvider())
                .findFirst().orElseThrow(() -> new BusinessRuleException("CAPABILITY-DEFAULT-NOT-FOUND",
                        "未配置已启用的默认 " + serviceType + " Provider"));
        if (selected.userConfigured()) apply(selected);
        return selected.providerId();
    }

    public <T> T invoke(String providerId, String operation, String scene, String requestSummary,
                        Supplier<T> invocation, Function<T, String> summary) {
        ThirdPartyServiceView config = effective(gateway.findService(providerId)
                .orElseThrow(() -> new BusinessRuleException("CAPABILITY-NOT-FOUND", "第三方服务不存在：" + providerId)));
        if (!config.enabled()) throw new BusinessRuleException("CAPABILITY-DISABLED", config.serviceName() + " 已停用");
        if (config.userConfigured()) apply(config);
        long started = System.currentTimeMillis();
        long id = gateway.beginCall(providerId, operation, scene, requestSummary, null);
        try {
            T result = invocation.get();
            gateway.finishCall(id, true, summary.apply(result), null, System.currentTimeMillis() - started);
            return result;
        } catch (Exception exception) {
            gateway.finishCall(id, false, null, safe(exception), System.currentTimeMillis() - started);
            throw exception;
        }
    }

    private ThirdPartyServiceView withUsages(ThirdPartyServiceView value) {
        return new ThirdPartyServiceView(value.id(), value.providerId(), value.serviceName(), value.serviceType(),
                value.implementationName(), value.endpoint(), value.model(), value.authenticationType(),
                value.credentialConfigured(), value.enabled(), value.defaultProvider(), value.fallbackProvider(), value.userConfigured(),
                value.healthStatus(), value.lastCheckedAt(), value.lastSuccessAt(), value.lastError(),
                value.todayCalls(), value.averageDurationMillis(), provider(value.providerId()).businessUsages());
    }
    private ThirdPartyServiceView effective(ThirdPartyServiceView value) {
        if (value.userConfigured()) return value;
        var current = provider(value.providerId()).currentConfiguration();
        return new ThirdPartyServiceView(value.id(), value.providerId(), value.serviceName(), value.serviceType(),
                value.implementationName(), current.endpoint(), current.model(), current.authenticationType(),
                value.credentialConfigured(), current.enabled(), value.defaultProvider(), value.fallbackProvider(), false,
                value.healthStatus(), value.lastCheckedAt(), value.lastSuccessAt(), value.lastError(),
                value.todayCalls(), value.averageDurationMillis(), value.businessUsages());
    }
    private void apply(ThirdPartyServiceView config) { apply(config, provider(config.providerId())); }
    private void apply(ThirdPartyServiceView config, ManagedCapabilityProvider provider) {
        var runtime = config.userConfigured()
                ? gateway.runtimeConfiguration(config.providerId()).orElseThrow()
                : provider.currentConfiguration();
        provider.configure(runtime);
    }
    private ManagedCapabilityProvider provider(String id) {
        return providers.stream().filter(item -> item.id().equalsIgnoreCase(id)).findFirst()
                .orElseThrow(() -> new BusinessRuleException("CAPABILITY-PROVIDER-NOT-FOUND", "未加载 Provider：" + id));
    }
    private String safe(Exception e) { return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(); }
}
