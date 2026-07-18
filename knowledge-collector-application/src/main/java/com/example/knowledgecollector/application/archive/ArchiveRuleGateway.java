package com.example.knowledgecollector.application.archive;

import java.util.List;

public interface ArchiveRuleGateway {
    List<ArchiveRuleView> findAll();
    ArchiveRuleView save(Long id, String name, String keyword, Long topicId,
                         Long sourceId, Integer minQuality, int sortOrder, boolean enabled);
    void delete(long id);
}
