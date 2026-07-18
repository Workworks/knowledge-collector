package com.example.knowledgecollector.web.capability;

import com.example.knowledgecollector.application.capability.ThirdPartyServiceCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ThirdPartyServiceRequest(@NotBlank @Size(max=2048) String endpoint,
        @Size(max=200) String model, @Size(max=32) String authenticationType,
        @Size(max=4000) String credential, boolean enabled, boolean defaultProvider,
        boolean fallbackProvider) {
    ThirdPartyServiceCommand command() { return new ThirdPartyServiceCommand(endpoint, model,
            authenticationType, credential, enabled, defaultProvider, fallbackProvider); }
}
