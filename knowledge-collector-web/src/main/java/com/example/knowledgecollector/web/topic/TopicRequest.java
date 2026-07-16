package com.example.knowledgecollector.web.topic;

import com.example.knowledgecollector.application.topic.TopicCommand;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TopicRequest(
        @NotBlank(message = "主题编码不能为空")
        @Size(max = 64, message = "主题编码不能超过 64 个字符")
        @Pattern(regexp = "[A-Za-z0-9_-]+", message = "主题编码只允许字母、数字、下划线和连字符")
        String code,
        @NotBlank(message = "主题名称不能为空")
        @Size(max = 128, message = "主题名称不能超过 128 个字符")
        String name,
        @Size(max = 1000, message = "描述不能超过 1000 个字符")
        String description,
        String keywords,
        String excludedKeywords,
        @NotBlank(message = "主题颜色不能为空")
        @Pattern(regexp = "#[0-9A-Fa-f]{6}", message = "主题颜色必须是 #RRGGBB")
        String color,
        @Size(max = 64, message = "图标不能超过 64 个字符")
        String icon,
        @NotBlank(message = "默认语言不能为空")
        @Size(max = 16, message = "语言代码不能超过 16 个字符")
        String language,
        boolean enabled,
        @Min(value = -9999, message = "排序值不能小于 -9999")
        @Max(value = 9999, message = "排序值不能大于 9999")
        int sortOrder
) {
    public TopicCommand toCommand() {
        return new TopicCommand(code, name, description, keywords, excludedKeywords,
                color, icon, language, enabled, sortOrder);
    }
}
