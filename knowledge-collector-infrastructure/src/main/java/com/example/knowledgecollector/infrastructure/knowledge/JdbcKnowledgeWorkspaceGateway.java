package com.example.knowledgecollector.infrastructure.knowledge;

import com.example.knowledgecollector.application.exception.ResourceNotFoundException;
import com.example.knowledgecollector.application.knowledge.KnowledgeWorkspaceGateway;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Repository
public class JdbcKnowledgeWorkspaceGateway implements KnowledgeWorkspaceGateway {
    private final JdbcClient jdbc;

    public JdbcKnowledgeWorkspaceGateway(JdbcClient jdbc) { this.jdbc = jdbc; }

    @Override @Transactional
    public Map<String, Object> createCard(Map<String, Object> v) {
        OffsetDateTime now = OffsetDateTime.now();
        jdbc.sql("""
                insert into knowledge_card(article_id,topic_id,title,content,card_type,source_quote,
                source_location,author,source_name,published_at,tags,user_note,confidence,verification_status,
                ai_suggested,confirmed,created_at,updated_at)
                values(:articleId,:topicId,:title,:content,:cardType,:sourceQuote,:sourceLocation,:author,
                :sourceName,:publishedAt,:tags,:userNote,:confidence,:verificationStatus,:aiSuggested,:confirmed,:now,:now)
                """).param("articleId", number(v, "articleId")).param("topicId", number(v, "topicId"))
                .param("title", text(v, "title")).param("content", text(v, "content"))
                .param("cardType", text(v, "cardType")).param("sourceQuote", text(v, "sourceQuote"))
                .param("sourceLocation", text(v, "sourceLocation")).param("author", text(v, "author"))
                .param("sourceName", text(v, "sourceName")).param("publishedAt", dateTime(v, "publishedAt"))
                .param("tags", text(v, "tags")).param("userNote", text(v, "userNote"))
                .param("confidence", integer(v, "confidence", 50))
                .param("verificationStatus", defaultText(v, "verificationStatus", "UNVERIFIED"))
                .param("aiSuggested", bool(v, "aiSuggested", false))
                .param("confirmed", bool(v, "confirmed", !bool(v, "aiSuggested", false)))
                .param("now", now).update();
        return find("knowledge_card", lastId("knowledge_card"));
    }

    @Override
    public List<Map<String, Object>> listCards(Long articleId, String type, Boolean confirmed) {
        StringBuilder sql = new StringBuilder("select * from knowledge_card where 1=1");
        Map<String, Object> params = new LinkedHashMap<>();
        if (articleId != null) { sql.append(" and article_id=:articleId"); params.put("articleId", articleId); }
        if (type != null) { sql.append(" and card_type=:type"); params.put("type", type); }
        if (confirmed != null) { sql.append(" and confirmed=:confirmed"); params.put("confirmed", confirmed); }
        sql.append(" order by updated_at desc,id desc");
        return jdbc.sql(sql.toString()).params(params).query(this::row).list();
    }

    @Override @Transactional
    public Map<String, Object> confirmCard(long id) {
        changed(jdbc.sql("update knowledge_card set confirmed=true,updated_at=:now where id=:id")
                .param("now", OffsetDateTime.now()).param("id", id).update(), "知识卡片", id);
        return find("knowledge_card", id);
    }

    @Override @Transactional
    public Map<String, Object> relateCards(long fromId, long toId, String type, String note) {
        jdbc.sql("""
                insert into knowledge_relation(from_card_id,to_card_id,relation_type,note,created_at)
                values(:fromId,:toId,:type,:note,:now)
                """).param("fromId", fromId).param("toId", toId).param("type", type)
                .param("note", note).param("now", OffsetDateTime.now()).update();
        return find("knowledge_relation", lastId("knowledge_relation"));
    }

    @Override @Transactional
    public Map<String, Object> createClaim(Map<String, Object> v) {
        OffsetDateTime now = OffsetDateTime.now();
        jdbc.sql("""
                insert into knowledge_claim(statement,status,assumptions,scope_note,limitations,created_at,updated_at)
                values(:statement,:status,:assumptions,:scope,:limitations,:now,:now)
                """).param("statement", text(v, "statement")).param("status", defaultText(v, "status", "PENDING_VERIFICATION"))
                .param("assumptions", text(v, "assumptions")).param("scope", text(v, "scopeNote"))
                .param("limitations", text(v, "limitations")).param("now", now).update();
        return find("knowledge_claim", lastId("knowledge_claim"));
    }
    @Override public List<Map<String, Object>> listClaims() {
        return jdbc.sql("select * from knowledge_claim order by updated_at desc,id desc").query(this::row).list();
    }
    @Override @Transactional
    public Map<String, Object> addEvidence(long claimId, Map<String, Object> v) {
        find("knowledge_claim", claimId);
        jdbc.sql("""
                insert into claim_evidence(claim_id,article_id,card_id,evidence_type,excerpt,citation,strength,created_at)
                values(:claimId,:articleId,:cardId,:type,:excerpt,:citation,:strength,:now)
                """).param("claimId", claimId).param("articleId", number(v, "articleId"))
                .param("cardId", number(v, "cardId")).param("type", text(v, "evidenceType"))
                .param("excerpt", text(v, "excerpt")).param("citation", text(v, "citation"))
                .param("strength", integer(v, "strength", 50)).param("now", OffsetDateTime.now()).update();
        return find("claim_evidence", lastId("claim_evidence"));
    }

    @Override @Transactional
    public Map<String, Object> createEntity(Map<String, Object> v) {
        OffsetDateTime now = OffsetDateTime.now();
        jdbc.sql("""
                insert into knowledge_entity(entity_type,canonical_name,description,aliases,created_at,updated_at)
                values(:type,:name,:description,:aliases,:now,:now)
                """).param("type", text(v, "entityType")).param("name", text(v, "canonicalName"))
                .param("description", text(v, "description")).param("aliases", text(v, "aliases"))
                .param("now", now).update();
        return find("knowledge_entity", lastId("knowledge_entity"));
    }
    @Override public List<Map<String, Object>> listEntities(String type, String keyword) {
        StringBuilder sql = new StringBuilder("select * from knowledge_entity where merged_into_id is null");
        Map<String, Object> params = new LinkedHashMap<>();
        if (type != null) { sql.append(" and entity_type=:type"); params.put("type", type); }
        if (keyword != null && !keyword.isBlank()) { sql.append(" and (lower(canonical_name) like :keyword or lower(aliases) like :keyword)"); params.put("keyword", "%" + keyword.toLowerCase(Locale.ROOT) + "%"); }
        sql.append(" order by updated_at desc,id desc");
        return jdbc.sql(sql.toString()).params(params).query(this::row).list();
    }
    @Override @Transactional
    public Map<String, Object> addEntityReference(long entityId, Map<String, Object> v) {
        find("knowledge_entity", entityId);
        jdbc.sql("""
                insert into entity_reference(entity_id,article_id,card_id,claim_id,relation_type,created_at)
                values(:entityId,:articleId,:cardId,:claimId,:type,:now)
                """).param("entityId", entityId).param("articleId", number(v, "articleId"))
                .param("cardId", number(v, "cardId")).param("claimId", number(v, "claimId"))
                .param("type", defaultText(v, "relationType", "MENTIONED_IN"))
                .param("now", OffsetDateTime.now()).update();
        return find("entity_reference", lastId("entity_reference"));
    }
    @Override @Transactional
    public Map<String, Object> mergeEntity(long sourceId, long targetId) {
        find("knowledge_entity", targetId);
        changed(jdbc.sql("update knowledge_entity set merged_into_id=:target,updated_at=:now where id=:source and merged_into_id is null")
                .param("target", targetId).param("source", sourceId).param("now", OffsetDateTime.now()).update(), "实体", sourceId);
        jdbc.sql("update entity_reference set entity_id=:target where entity_id=:source")
                .param("target", targetId).param("source", sourceId).update();
        return find("knowledge_entity", sourceId);
    }

    @Override @Transactional
    public Map<String, Object> createEvent(Map<String, Object> v) {
        OffsetDateTime now = OffsetDateTime.now();
        jdbc.sql("""
                insert into knowledge_event(title,summary,event_date,cluster_key,status,created_at,updated_at)
                values(:title,:summary,:eventDate,:clusterKey,:status,:now,:now)
                """).param("title", text(v, "title")).param("summary", text(v, "summary"))
                .param("eventDate", localDate(v, "eventDate")).param("clusterKey", text(v, "clusterKey"))
                .param("status", defaultText(v, "status", "TRACKING")).param("now", now).update();
        return find("knowledge_event", lastId("knowledge_event"));
    }
    @Override public List<Map<String, Object>> listEvents() {
        return jdbc.sql("""
                select e.*,count(ea.article_id) article_count from knowledge_event e
                left join event_article ea on ea.event_id=e.id group by e.id order by e.updated_at desc,e.id desc
                """).query(this::row).list();
    }
    @Override @Transactional
    public Map<String, Object> attachEventArticle(long eventId, long articleId, String sourceRole, boolean confirmed) {
        find("knowledge_event", eventId);
        jdbc.sql("merge into event_article(event_id,article_id,source_role,confirmed) key(event_id,article_id) values(:eventId,:articleId,:role,:confirmed)")
                .param("eventId", eventId).param("articleId", articleId).param("role", sourceRole)
                .param("confirmed", confirmed).update();
        return Map.of("eventId", eventId, "articleId", articleId, "sourceRole", sourceRole, "confirmed", confirmed);
    }

    @Override @Transactional
    public Map<String, Object> createTopicPage(Map<String, Object> v) {
        OffsetDateTime now = OffsetDateTime.now();
        jdbc.sql("""
                insert into knowledge_topic_page(topic_id,name,introduction,knowledge_summary,reading_order,
                unresolved_questions,user_summary,created_at,updated_at)
                values(:topicId,:name,:intro,:summary,:readingOrder,:questions,:userSummary,:now,:now)
                """).param("topicId", number(v, "topicId")).param("name", text(v, "name"))
                .param("intro", text(v, "introduction")).param("summary", text(v, "knowledgeSummary"))
                .param("readingOrder", text(v, "readingOrder")).param("questions", text(v, "unresolvedQuestions"))
                .param("userSummary", text(v, "userSummary")).param("now", now).update();
        return find("knowledge_topic_page", lastId("knowledge_topic_page"));
    }
    @Override public List<Map<String, Object>> listTopicPages() {
        return jdbc.sql("select * from knowledge_topic_page order by updated_at desc,id desc").query(this::row).list();
    }

    @Override @Transactional
    public Map<String, Object> createProject(Map<String, Object> v) {
        OffsetDateTime now = OffsetDateTime.now();
        jdbc.sql("""
                insert into research_project(title,objective,status,core_questions,hypotheses,conclusions,research_log,created_at,updated_at)
                values(:title,:objective,:status,:questions,:hypotheses,:conclusions,:log,:now,:now)
                """).param("title", text(v, "title")).param("objective", text(v, "objective"))
                .param("status", defaultText(v, "status", "ACTIVE")).param("questions", text(v, "coreQuestions"))
                .param("hypotheses", text(v, "hypotheses")).param("conclusions", text(v, "conclusions"))
                .param("log", text(v, "researchLog")).param("now", now).update();
        return find("research_project", lastId("research_project"));
    }
    @Override public List<Map<String, Object>> listProjects() {
        return jdbc.sql("select * from research_project order by updated_at desc,id desc").query(this::row).list();
    }
    @Override @Transactional
    public Map<String, Object> addProjectItem(long projectId, Map<String, Object> v) {
        find("research_project", projectId);
        jdbc.sql("""
                insert into research_item(project_id,article_id,card_id,claim_id,usage_type,note,created_at)
                values(:projectId,:articleId,:cardId,:claimId,:usageType,:note,:now)
                """).param("projectId", projectId).param("articleId", number(v, "articleId"))
                .param("cardId", number(v, "cardId")).param("claimId", number(v, "claimId"))
                .param("usageType", defaultText(v, "usageType", "BACKGROUND"))
                .param("note", text(v, "note")).param("now", OffsetDateTime.now()).update();
        return find("research_item", lastId("research_item"));
    }

    @Override @Transactional
    public Map<String, Object> createSynthesis(Map<String, Object> v) {
        OffsetDateTime now = OffsetDateTime.now();
        jdbc.sql("""
                insert into knowledge_synthesis(project_id,topic_page_id,title,synthesis_type,content,source_references,status,created_at,updated_at)
                values(:projectId,:topicPageId,:title,:type,:content,:refs,:status,:now,:now)
                """).param("projectId", number(v, "projectId")).param("topicPageId", number(v, "topicPageId"))
                .param("title", text(v, "title")).param("type", defaultText(v, "synthesisType", "RESEARCH_BRIEF"))
                .param("content", text(v, "content")).param("refs", text(v, "sourceReferences"))
                .param("status", defaultText(v, "status", "DRAFT")).param("now", now).update();
        return find("knowledge_synthesis", lastId("knowledge_synthesis"));
    }
    @Override public List<Map<String, Object>> listSyntheses() {
        return jdbc.sql("select * from knowledge_synthesis order by updated_at desc,id desc").query(this::row).list();
    }

    @Override @Transactional
    public Map<String, Object> createDraft(Map<String, Object> v) {
        OffsetDateTime now = OffsetDateTime.now();
        jdbc.sql("""
                insert into writing_draft(project_id,title,outline,content,source_references,status,created_at,updated_at)
                values(:projectId,:title,:outline,:content,:refs,:status,:now,:now)
                """).param("projectId", number(v, "projectId")).param("title", text(v, "title"))
                .param("outline", text(v, "outline")).param("content", text(v, "content"))
                .param("refs", text(v, "sourceReferences")).param("status", defaultText(v, "status", "DRAFT"))
                .param("now", now).update();
        return find("writing_draft", lastId("writing_draft"));
    }
    @Override public List<Map<String, Object>> listDrafts() {
        return jdbc.sql("select * from writing_draft order by updated_at desc,id desc").query(this::row).list();
    }

    @Override @Transactional
    public Map<String, Object> createGap(Map<String, Object> v) {
        OffsetDateTime now = OffsetDateTime.now();
        jdbc.sql("""
                insert into knowledge_gap(project_id,topic_page_id,claim_id,gap_type,description,status,due_at,created_at,updated_at)
                values(:projectId,:topicPageId,:claimId,:type,:description,:status,:dueAt,:now,:now)
                """).param("projectId", number(v, "projectId")).param("topicPageId", number(v, "topicPageId"))
                .param("claimId", number(v, "claimId")).param("type", defaultText(v, "gapType", "MISSING_EVIDENCE"))
                .param("description", text(v, "description")).param("status", defaultText(v, "status", "OPEN"))
                .param("dueAt", dateTime(v, "dueAt")).param("now", now).update();
        return find("knowledge_gap", lastId("knowledge_gap"));
    }
    @Override public List<Map<String, Object>> listGaps(String status) {
        if (status == null || status.isBlank()) return jdbc.sql("select * from knowledge_gap order by updated_at desc,id desc").query(this::row).list();
        return jdbc.sql("select * from knowledge_gap where status=:status order by updated_at desc,id desc")
                .param("status", status.toUpperCase(Locale.ROOT)).query(this::row).list();
    }

    @Override @Transactional
    public Map<String, Object> scheduleReview(long cardId, String reviewType, OffsetDateTime dueAt) {
        find("knowledge_card", cardId); OffsetDateTime now = OffsetDateTime.now();
        jdbc.sql("""
                insert into knowledge_review(card_id,review_type,due_at,active,created_at,updated_at)
                values(:cardId,:type,:dueAt,true,:now,:now)
                """).param("cardId", cardId).param("type", reviewType).param("dueAt", dueAt).param("now", now).update();
        return find("knowledge_review", lastId("knowledge_review"));
    }
    @Override public List<Map<String, Object>> dueReviews(OffsetDateTime now) {
        return jdbc.sql("""
                select r.*,c.title card_title,c.content card_content,c.card_type from knowledge_review r
                join knowledge_card c on c.id=r.card_id where r.active=true and r.due_at<=:now order by r.due_at,r.id
                """).param("now", now).query(this::row).list();
    }
    @Override @Transactional
    public Map<String, Object> completeReview(long reviewId, OffsetDateTime nextDueAt) {
        OffsetDateTime now = OffsetDateTime.now();
        changed(jdbc.sql("""
                update knowledge_review set last_reviewed_at=:now,review_count=review_count+1,
                due_at=:nextDue,updated_at=:now where id=:id
                """).param("now", now).param("nextDue", nextDueAt).param("id", reviewId).update(), "复习任务", reviewId);
        return find("knowledge_review", reviewId);
    }

    @Override public Map<String, Long> statistics() {
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("cards", count("knowledge_card")); result.put("claims", count("knowledge_claim"));
        result.put("entities", count("knowledge_entity where merged_into_id is null")); result.put("events", count("knowledge_event"));
        result.put("topicPages", count("knowledge_topic_page")); result.put("projects", count("research_project"));
        result.put("syntheses", count("knowledge_synthesis")); result.put("drafts", count("writing_draft"));
        result.put("openGaps", count("knowledge_gap where status='OPEN'"));
        result.put("dueReviews", jdbc.sql("select count(*) from knowledge_review where active=true and due_at<=:now")
                .param("now", OffsetDateTime.now()).query(Long.class).single());
        return result;
    }

    private Map<String, Object> find(String table, long id) {
        return jdbc.sql("select * from " + table + " where id=:id").param("id", id).query(this::row).optional()
                .orElseThrow(() -> new ResourceNotFoundException("记录不存在：" + table + "#" + id));
    }
    private long lastId(String table) { return jdbc.sql("select max(id) from " + table).query(Long.class).single(); }
    private long count(String from) { return jdbc.sql("select count(*) from " + from).query(Long.class).single(); }
    private void changed(int count, String name, long id) { if (count == 0) throw new ResourceNotFoundException(name + "不存在：" + id); }

    private Map<String, Object> row(ResultSet rs, int ignored) throws SQLException {
        Map<String, Object> values = new LinkedHashMap<>();
        var meta = rs.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            Object value = rs.getObject(i);
            if (value instanceof Clob clob) value = clob.getSubString(1, (int) clob.length());
            values.put(camel(meta.getColumnLabel(i)), value);
        }
        return values;
    }
    private String camel(String name) {
        StringBuilder value = new StringBuilder(); boolean upper = false;
        for (char c : name.toLowerCase(Locale.ROOT).toCharArray()) {
            if (c == '_') upper = true; else { value.append(upper ? Character.toUpperCase(c) : c); upper = false; }
        }
        return value.toString();
    }
    private String text(Map<String, Object> v, String key) { Object x = v.get(key); return x == null || x.toString().isBlank() ? null : x.toString().trim(); }
    private String defaultText(Map<String, Object> v, String key, String fallback) { String x = text(v, key); return x == null ? fallback : x; }
    private Long number(Map<String, Object> v, String key) { Object x = v.get(key); if (x instanceof Number n) return n.longValue(); if (x == null || x.toString().isBlank()) return null; return Long.valueOf(x.toString()); }
    private int integer(Map<String, Object> v, String key, int fallback) { Object x = v.get(key); return x instanceof Number n ? n.intValue() : x == null ? fallback : Integer.parseInt(x.toString()); }
    private boolean bool(Map<String, Object> v, String key, boolean fallback) { Object x = v.get(key); return x == null ? fallback : Boolean.parseBoolean(x.toString()); }
    private OffsetDateTime dateTime(Map<String, Object> v, String key) { String x = text(v, key); return x == null ? null : OffsetDateTime.parse(x); }
    private LocalDate localDate(Map<String, Object> v, String key) { String x = text(v, key); return x == null ? null : LocalDate.parse(x); }
}
