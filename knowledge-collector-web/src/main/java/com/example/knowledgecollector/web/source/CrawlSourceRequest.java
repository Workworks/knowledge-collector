package com.example.knowledgecollector.web.source;

import com.example.knowledgecollector.application.source.CrawlSourceCommand;
import com.example.knowledgecollector.domain.source.SourceType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record CrawlSourceRequest(
        @NotBlank(message = "采集源编码不能为空")
        @Size(max = 64, message = "采集源编码不能超过 64 个字符")
        @Pattern(regexp = "[A-Za-z0-9_-]+", message = "采集源编码格式不正确")
        String code,
        @NotBlank(message = "采集源名称不能为空")
        @Size(max = 128, message = "采集源名称不能超过 128 个字符")
        String name,
        @NotNull(message = "来源类型不能为空")
        SourceType type,
        @Size(max = 2048, message = "首页地址不能超过 2048 个字符")
        String homeUrl,
        @Size(max = 2048, message = "订阅地址不能超过 2048 个字符")
        String feedUrl,
        @NotBlank(message = "默认语言不能为空") @Size(max = 16) String language,
        @NotBlank(message = "字符集不能为空") @Size(max = 32) String charset,
        @NotBlank(message = "User-Agent 不能为空") @Size(max = 256) String userAgent,
        @Min(value = 1, message = "超时时间至少 1 秒")
        @Max(value = 120, message = "超时时间不能超过 120 秒")
        int timeoutSeconds,
        @Min(value = 0, message = "重试次数不能小于 0")
        @Max(value = 10, message = "重试次数不能超过 10")
        int maxRetries,
        @Min(value = 0, message = "请求间隔不能小于 0")
        @Max(value = 3600000, message = "请求间隔不能超过 3600000 毫秒")
        long requestIntervalMillis,
        boolean obeyRobots,
        boolean fetchFullContent,
        boolean summaryOnly,
        boolean saveSnapshot,
        boolean enabled,
        @Size(max = 2000, message = "备注不能超过 2000 个字符")
        String notes,
        Set<Long> topicIds
) {
    public CrawlSourceCommand toCommand() {
        return new CrawlSourceCommand(code, name, type, homeUrl, feedUrl, language, charset,
                userAgent, timeoutSeconds, maxRetries, requestIntervalMillis, obeyRobots,
                fetchFullContent, summaryOnly, saveSnapshot, enabled, notes, topicIds);
    }
}
