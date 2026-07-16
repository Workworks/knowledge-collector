package com.example.knowledgecollector.web.page;

import com.example.knowledgecollector.application.article.ArticleService;
import com.example.knowledgecollector.application.assessment.ArticleAssessmentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class AdminPageController {
    private final ArticleService articles;
    private final ArticleAssessmentService assessments;

    public AdminPageController(ArticleService articles, ArticleAssessmentService assessments) {
        this.articles = articles;
        this.assessments = assessments;
    }

    @GetMapping("/topics")
    public String topics() {
        return "topics";
    }

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
    public String taskDetail() {
        return "task-detail";
    }

    @GetMapping("/articles")
    public String articles(Model model) {
        model.addAttribute("articles", articles.findPage(null, null, 0, 100).content());
        return "articles";
    }

    @GetMapping("/articles/review")
    public String reviewArticles(Model model) {
        model.addAttribute("articles",
                articles.findPage(null, null, "PENDING_REVIEW", null, null, 0, 100).content());
        model.addAttribute("reviewMode", true);
        return "articles";
    }

    @GetMapping("/articles/{id}")
    public String articleDetail(@PathVariable long id, Model model) {
        model.addAttribute("article", articles.get(id));
        model.addAttribute("assessment", assessments.get(id));
        return "article-detail";
    }
}
