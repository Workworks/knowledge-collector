package com.example.knowledgecollector.application.capability;

import java.util.List;
import java.util.Optional;
import com.example.knowledgecollector.capability.management.ManagedCapabilityProvider;

public interface ThirdPartyCapabilityGateway {
    List<ThirdPartyServiceView> listServices();
    Optional<ThirdPartyServiceView> findService(String providerId);
    Optional<ManagedCapabilityProvider.RuntimeConfiguration> runtimeConfiguration(String providerId);
    ThirdPartyServiceView save(String providerId, ThirdPartyServiceCommand command);
    void updateHealth(String providerId, boolean available, String message);
    long beginCall(String providerId, String operation, String scene, String requestSummary, Long retryOfId);
    void finishCall(long id, boolean success, String resultSummary, String errorMessage, long durationMillis);
    List<ThirdPartyCallLogView> listCalls(int limit);
    ThirdPartyCallLogView getCall(long id);
}
