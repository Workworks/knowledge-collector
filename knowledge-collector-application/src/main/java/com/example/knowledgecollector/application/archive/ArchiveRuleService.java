package com.example.knowledgecollector.application.archive;

import com.example.knowledgecollector.application.exception.BusinessRuleException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArchiveRuleService {
    private final ArchiveRuleGateway gateway;

    public ArchiveRuleService(ArchiveRuleGateway gateway) {
        this.gateway = gateway;
    }

    public List<ArchiveRuleView> findAll() {
        return gateway.findAll();
    }

    public ArchiveRuleView save(Long id, String name, String keyword, Long topicId,
                                Long sourceId, Integer minQuality, int sortOrder, boolean enabled) {
        if (name == null || name.isBlank()) {
            throw new BusinessRuleException("ARCHIVE-RULE-NAME-EMPTY", "整理规则名称不能为空");
        }
        if (minQuality != null && (minQuality < 0 || minQuality > 100)) {
            throw new BusinessRuleException("ARCHIVE-RULE-QUALITY-INVALID", "最低质量必须在 0 到 100 之间");
        }
        return gateway.save(id, name.trim(), trim(keyword), topicId, sourceId, minQuality, sortOrder, enabled);
    }

    public void delete(long id) {
        gateway.delete(id);
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
