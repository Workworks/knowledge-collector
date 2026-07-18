package com.example.knowledgecollector.web.knowledge;

import com.example.knowledgecollector.application.knowledge.KnowledgeWorkspaceService;
import com.example.knowledgecollector.web.api.ApiResponse;
import com.example.knowledgecollector.web.api.CorrelationIdFilter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/knowledge")
@Tag(name = "知识研究工作台", description = "Stage 16-25 知识卡片、证据、实体、专题、研究、写作与复习")
public class KnowledgeWorkspaceController {
    private final KnowledgeWorkspaceService service;
    public KnowledgeWorkspaceController(KnowledgeWorkspaceService service) { this.service = service; }

    @GetMapping("/statistics") public ApiResponse<?> statistics(HttpServletRequest r) { return ok(service.statistics(), r); }

    @GetMapping("/cards") public ApiResponse<?> cards(@RequestParam(required=false) Long articleId,
            @RequestParam(required=false) String type, @RequestParam(required=false) Boolean confirmed, HttpServletRequest r) {
        return ok(service.cards(articleId, type, confirmed), r);
    }
    @PostMapping("/cards") public ApiResponse<?> createCard(@RequestBody Map<String,Object> body, HttpServletRequest r) { return ok(service.createCard(body), r); }
    @PostMapping("/cards/{id}/confirm") public ApiResponse<?> confirmCard(@PathVariable long id, HttpServletRequest r) { return ok(service.confirmCard(id), r); }
    @PostMapping("/cards/{fromId}/relations/{toId}") public ApiResponse<?> relate(@PathVariable long fromId, @PathVariable long toId,
            @RequestBody Map<String,Object> body, HttpServletRequest r) { return ok(service.relateCards(fromId, toId, body), r); }

    @GetMapping("/claims") public ApiResponse<?> claims(HttpServletRequest r) { return ok(service.claims(), r); }
    @PostMapping("/claims") public ApiResponse<?> createClaim(@RequestBody Map<String,Object> body, HttpServletRequest r) { return ok(service.createClaim(body), r); }
    @PostMapping("/claims/{id}/evidence") public ApiResponse<?> evidence(@PathVariable long id, @RequestBody Map<String,Object> body, HttpServletRequest r) { return ok(service.addEvidence(id, body), r); }

    @GetMapping("/entities") public ApiResponse<?> entities(@RequestParam(required=false) String type,
            @RequestParam(required=false) String keyword, HttpServletRequest r) { return ok(service.entities(type, keyword), r); }
    @PostMapping("/entities") public ApiResponse<?> createEntity(@RequestBody Map<String,Object> body, HttpServletRequest r) { return ok(service.createEntity(body), r); }
    @PostMapping("/entities/{id}/references") public ApiResponse<?> reference(@PathVariable long id, @RequestBody Map<String,Object> body, HttpServletRequest r) { return ok(service.addEntityReference(id, body), r); }
    @PostMapping("/entities/{sourceId}/merge/{targetId}") public ApiResponse<?> merge(@PathVariable long sourceId, @PathVariable long targetId, HttpServletRequest r) { return ok(service.mergeEntity(sourceId, targetId), r); }

    @GetMapping("/events") public ApiResponse<?> events(HttpServletRequest r) { return ok(service.events(), r); }
    @PostMapping("/events") public ApiResponse<?> createEvent(@RequestBody Map<String,Object> body, HttpServletRequest r) { return ok(service.createEvent(body), r); }
    @PostMapping("/events/{id}/articles") public ApiResponse<?> eventArticle(@PathVariable long id, @RequestBody Map<String,Object> body, HttpServletRequest r) { return ok(service.attachEventArticle(id, body), r); }

    @GetMapping("/topics") public ApiResponse<?> topics(HttpServletRequest r) { return ok(service.topicPages(), r); }
    @PostMapping("/topics") public ApiResponse<?> createTopic(@RequestBody Map<String,Object> body, HttpServletRequest r) { return ok(service.createTopicPage(body), r); }

    @GetMapping("/projects") public ApiResponse<?> projects(HttpServletRequest r) { return ok(service.projects(), r); }
    @PostMapping("/projects") public ApiResponse<?> createProject(@RequestBody Map<String,Object> body, HttpServletRequest r) { return ok(service.createProject(body), r); }
    @PostMapping("/projects/{id}/items") public ApiResponse<?> projectItem(@PathVariable long id, @RequestBody Map<String,Object> body, HttpServletRequest r) { return ok(service.addProjectItem(id, body), r); }

    @GetMapping("/syntheses") public ApiResponse<?> syntheses(HttpServletRequest r) { return ok(service.syntheses(), r); }
    @PostMapping("/syntheses") public ApiResponse<?> createSynthesis(@RequestBody Map<String,Object> body, HttpServletRequest r) { return ok(service.createSynthesis(body), r); }

    @GetMapping("/drafts") public ApiResponse<?> drafts(HttpServletRequest r) { return ok(service.drafts(), r); }
    @PostMapping("/drafts") public ApiResponse<?> createDraft(@RequestBody Map<String,Object> body, HttpServletRequest r) { return ok(service.createDraft(body), r); }

    @GetMapping("/gaps") public ApiResponse<?> gaps(@RequestParam(required=false) String status, HttpServletRequest r) { return ok(service.gaps(status), r); }
    @PostMapping("/gaps") public ApiResponse<?> createGap(@RequestBody Map<String,Object> body, HttpServletRequest r) { return ok(service.createGap(body), r); }

    @GetMapping("/reviews/due") public ApiResponse<?> dueReviews(HttpServletRequest r) { return ok(service.dueReviews(), r); }
    @PostMapping("/cards/{cardId}/reviews") public ApiResponse<?> review(@PathVariable long cardId, @RequestBody Map<String,Object> body, HttpServletRequest r) { return ok(service.scheduleReview(cardId, body), r); }
    @PostMapping("/reviews/{id}/complete") public ApiResponse<?> completeReview(@PathVariable long id, @RequestBody Map<String,Object> body, HttpServletRequest r) { return ok(service.completeReview(id, body), r); }

    private ApiResponse<?> ok(Object data, HttpServletRequest request) {
        return ApiResponse.success(data, (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE_NAME));
    }
}
