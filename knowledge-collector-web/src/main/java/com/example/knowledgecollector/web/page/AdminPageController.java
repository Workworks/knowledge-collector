package com.example.knowledgecollector.web.page;

import com.example.knowledgecollector.application.article.ArticleService;
import com.example.knowledgecollector.application.assessment.ArticleAssessmentService;
import com.example.knowledgecollector.application.reading.ArticleReadingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class AdminPageController {
    private final ArticleService articles;
    private final ArticleAssessmentService assessments;
    private final ArticleReadingService reading;

    public AdminPageController(ArticleService articles, ArticleAssessmentService assessments,
                               ArticleReadingService reading) {
        this.articles = articles;
        this.assessments = assessments;
        this.reading = reading;
    }

    @GetMapping("/topics")
    public String topics() {
        return "topics";
    }

    @GetMapping("/capabilities")
    public String capabilities() { return "capabilities"; }

    @GetMapping("/sources")
    public String sources() {
        return "sources";
    }

    @GetMapping("/sources/{id}/rules")
    public String sourceRules(@PathVariable long id, Model model) {
        model.addAttribute("sourceId", id);
        return "source-rules";
    }

    @GetMapping("/sources/{id}/test")
    public String sourceTest(@PathVariable long id, Model model) {
        model.addAttribute("sourceId", id);
        return "source-rules";
    }

    @GetMapping("/tasks")
    public String tasks() {
        return "tasks";
    }

    @GetMapping("/tasks/{id}")
    public String taskDetail(@PathVariable long id, Model model) {
        model.addAttribute("taskId", id);
        return "task-detail";
    }

    @GetMapping("/articles")
    public String articles(Model model) {
        model.addAttribute("reviewMode", false);
        model.addAttribute("archiveMode", false);
        return "articles";
    }

    @GetMapping("/articles/review")
    public String reviewArticles(Model model) {
        model.addAttribute("reviewMode", true);
        model.addAttribute("archiveMode", false);
        return "articles";
    }

    @GetMapping("/articles/archive")
    public String archivedArticles() {
        return "archive";
    }

    @GetMapping("/knowledge")
    public String knowledgeWorkspace() {
        return "knowledge-workspace";
    }

    @GetMapping("/articles/{id}")
    public String articleDetail(@PathVariable long id, Model model) {
        model.addAttribute("article", articles.get(id));
        model.addAttribute("assessment", assessments.get(id));
        model.addAttribute("reading", reading.get(id));
        return "article-detail";
    }
}
