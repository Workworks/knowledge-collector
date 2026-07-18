package com.example.knowledgecollector.application.capability;

public record ThirdPartyServiceCommand(String endpoint, String model, String authenticationType,
        String credential, boolean enabled, boolean defaultProvider, boolean fallbackProvider) {
}
