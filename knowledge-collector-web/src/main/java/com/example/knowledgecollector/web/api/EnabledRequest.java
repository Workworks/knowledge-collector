package com.example.knowledgecollector.web.api;

import jakarta.validation.constraints.NotNull;

public record EnabledRequest(@NotNull(message = "启用状态不能为空") Boolean enabled) {
}
