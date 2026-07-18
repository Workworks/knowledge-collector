package com.example.knowledgecollector.application.knowledge;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public interface KnowledgeWorkspaceGateway {
    Map<String, Object> createCard(Map<String, Object> values);
    List<Map<String, Object>> listCards(Long articleId, String type, Boolean confirmed);
    Map<String, Object> confirmCard(long id);
    Map<String, Object> relateCards(long fromId, long toId, String type, String note);

    Map<String, Object> createClaim(Map<String, Object> values);
    List<Map<String, Object>> listClaims();
    Map<String, Object> addEvidence(long claimId, Map<String, Object> values);

    Map<String, Object> createEntity(Map<String, Object> values);
    List<Map<String, Object>> listEntities(String type, String keyword);
    Map<String, Object> addEntityReference(long entityId, Map<String, Object> values);
    Map<String, Object> mergeEntity(long sourceId, long targetId);

    Map<String, Object> createEvent(Map<String, Object> values);
    List<Map<String, Object>> listEvents();
    Map<String, Object> attachEventArticle(long eventId, long articleId, String sourceRole, boolean confirmed);

    Map<String, Object> createTopicPage(Map<String, Object> values);
    List<Map<String, Object>> listTopicPages();

    Map<String, Object> createProject(Map<String, Object> values);
    List<Map<String, Object>> listProjects();
    Map<String, Object> addProjectItem(long projectId, Map<String, Object> values);

    Map<String, Object> createSynthesis(Map<String, Object> values);
    List<Map<String, Object>> listSyntheses();

    Map<String, Object> createDraft(Map<String, Object> values);
    List<Map<String, Object>> listDrafts();

    Map<String, Object> createGap(Map<String, Object> values);
    List<Map<String, Object>> listGaps(String status);

    Map<String, Object> scheduleReview(long cardId, String reviewType, OffsetDateTime dueAt);
    List<Map<String, Object>> dueReviews(OffsetDateTime now);
    Map<String, Object> completeReview(long reviewId, OffsetDateTime nextDueAt);

    Map<String, Long> statistics();
}
