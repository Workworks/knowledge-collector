package com.example.knowledgecollector.domain.article;

import com.example.knowledgecollector.domain.topic.Topic;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ArticleAssessmentRules {
    private ArticleAssessmentRules() {
    }

    public static Assessment assess(Input input, List<Topic> topics, Set<Long> defaultTopicIds) {
        String title = normalize(input.title());
        String summary = normalize(input.summary());
        String content = normalize(input.contentText());
        String corpus = title + " " + summary + " " + content;
        Map<Long, TopicMatch> matches = new LinkedHashMap<>();
        for (Topic topic : topics) {
            String excluded = firstMatch(corpus, topic.excludedKeywords());
            if (excluded != null) {
                continue;
            }
            List<String> reasons = new ArrayList<>();
            int score = defaultTopicIds.contains(topic.id()) ? 25 : 0;
            if (score > 0) {
                reasons.add("采集源默认主题");
            }
            for (String keyword : topic.keywords()) {
                String normalizedKeyword = normalize(keyword);
                if (normalizedKeyword.isBlank()) {
                    continue;
                }
                if (title.contains(normalizedKeyword)) {
                    score += 35;
                    reasons.add("标题命中：" + keyword);
                } else if (summary.contains(normalizedKeyword)) {
                    score += 20;
                    reasons.add("摘要命中：" + keyword);
                } else if (content.contains(normalizedKeyword)) {
                    score += 10;
                    reasons.add("正文命中：" + keyword);
                }
            }
            if (score > 0) {
                matches.put(topic.id(), new TopicMatch(Math.min(100, score), String.join("；", reasons)));
            }
        }

        int quality = 0;
        List<String> warnings = new ArrayList<>();
        if (!title.isBlank() && !"(无标题)".equals(input.title())) quality += 15; else warnings.add("标题缺失");
        if (input.author() != null && !input.author().isBlank()) quality += 10; else warnings.add("作者缺失");
        if (input.publishTimePresent()) quality += 10; else warnings.add("发布时间为推断值");
        if (input.summary() != null && input.summary().length() >= 50) quality += 10; else warnings.add("摘要过短");
        if (input.contentText() != null && input.contentText().length() >= 200) quality += 25;
        else if (input.contentText() != null && !input.contentText().isBlank()) quality += 12;
        else warnings.add("未采集正文");
        if (!matches.isEmpty()) quality += 15; else warnings.add("未匹配主题");
        if (input.url() != null && input.url().startsWith("https://")) quality += 5;
        String normalizedUrl = normalize(input.url());
        boolean hasDoi = normalizedUrl.contains("doi.org/")
                || corpus.matches(".*10\\.\\d{4,9}/\\S+.*");
        if (hasDoi) quality += 10;
        quality = Math.min(100, quality);

        String sourceLevel;
        if (hasDoi) sourceLevel = "PRIMARY_SOURCE";
        else if (input.url() != null && input.url().contains("arxiv.org")) {
            sourceLevel = "PREPRINT";
            warnings.add("预印本，尚未确认同行评审");
        } else if (input.contentText() != null && !input.contentText().isBlank()) sourceLevel = "DIRECT_SOURCE";
        else sourceLevel = "AGGREGATED_METADATA";

        int evidence = (input.author() == null || input.author().isBlank() ? 0 : 1)
                + (input.publishTimePresent() ? 1 : 0) + (hasDoi ? 1 : 0)
                + (input.contentText() == null || input.contentText().isBlank() ? 0 : 1);
        String reviewStatus = quality >= 60 && !matches.isEmpty() ? "AUTO_ACCEPTED" : "PENDING_REVIEW";
        return new Assessment(quality, reviewStatus, sourceLevel, evidence, hasDoi,
                fingerprint(title + "\n" + (content.isBlank() ? summary : content)),
                List.copyOf(warnings), Map.copyOf(matches));
    }

    private static String firstMatch(String corpus, List<String> values) {
        for (String value : values) {
            if (!normalize(value).isBlank() && corpus.contains(normalize(value))) {
                return value;
            }
        }
        return null;
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private static String fingerprint(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("内容指纹计算失败", exception);
        }
    }

    public record Input(String title, String author, String summary, String contentText,
                        String url, boolean publishTimePresent) {
    }

    public record TopicMatch(int score, String reason) {
    }

    public record Assessment(int qualityScore, String reviewStatus, String sourceLevel,
                             int evidenceCount, boolean hasDoi, String fingerprint,
                             List<String> warnings, Map<Long, TopicMatch> topicMatches) {
    }
}
