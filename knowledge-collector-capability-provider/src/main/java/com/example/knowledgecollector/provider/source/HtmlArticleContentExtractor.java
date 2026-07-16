package com.example.knowledgecollector.provider.source;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class HtmlArticleContentExtractor {
    private static final Pattern HEADER_CHARSET =
            Pattern.compile("charset\\s*=\\s*[\"']?([^;\\s\"']+)", Pattern.CASE_INSENSITIVE);
    private static final List<String> PREFERRED_SELECTORS = List.of(
            "[itemprop=articleBody]", "article .article-content", "article .post-content",
            "article .entry-content", "article .content", ".article-content", ".post-content",
            ".entry-content", ".article-body", ".post-body", ".story-body", "main article", "article"
    );
    private static final String NOISE_SELECTORS = String.join(",",
            "script", "style", "noscript", "iframe", "form", "nav", "footer", "aside",
            ".advertisement", ".ads", ".ad", ".share", ".social", ".related", ".recommend",
            ".comments", ".comment", ".breadcrumb", ".pagination", ".newsletter");
    private static final Safelist ARTICLE_SAFELIST = Safelist.relaxed()
            .removeTags("img")
            .addProtocols("a", "href", "http", "https");

    public ExtractedContent extract(byte[] body, String contentType, String configuredCharset, String baseUri) {
        Document document = Jsoup.parse(new String(body,
                Charset.forName(charset(contentType, configuredCharset))), baseUri);
        Element content = preferred(document);
        if (content == null) {
            content = scored(document);
        }
        if (content == null) {
            return ExtractedContent.empty();
        }
        Element copy = content.clone();
        copy.select(NOISE_SELECTORS).remove();
        return sanitize(copy.html(), baseUri);
    }

    public ExtractedContent sanitize(String html, String baseUri) {
        if (html == null || html.isBlank()) {
            return ExtractedContent.empty();
        }
        String cleanHtml = Jsoup.clean(html, baseUri, ARTICLE_SAFELIST);
        String cleanText = Jsoup.parseBodyFragment(cleanHtml, baseUri).text().trim();
        if (cleanText.isBlank()) {
            return ExtractedContent.empty();
        }
        return new ExtractedContent(cleanHtml, cleanText);
    }

    public String plainText(String html) {
        return html == null ? null : Jsoup.parse(html).text().trim();
    }

    private Element preferred(Document document) {
        for (String selector : PREFERRED_SELECTORS) {
            Element candidate = document.selectFirst(selector);
            if (candidate != null && candidate.text().trim().length() >= 80) {
                return candidate;
            }
        }
        return null;
    }

    private Element scored(Document document) {
        return document.select("main,article,section,div").stream()
                .filter(element -> element.text().trim().length() >= 120)
                .max(Comparator.comparingDouble(this::score))
                .filter(element -> score(element) >= 160)
                .orElse(null);
    }

    private double score(Element element) {
        String text = element.text().trim();
        int textLength = text.length();
        int linkLength = element.select("a").text().length();
        double linkDensity = textLength == 0 ? 1 : (double) linkLength / textLength;
        return textLength
                + element.select("p").size() * 80.0
                + element.select("h1,h2,h3").size() * 30.0
                - linkDensity * textLength * 1.5
                - element.childrenSize() * 2.0;
    }

    private String charset(String contentType, String configuredCharset) {
        Matcher matcher = HEADER_CHARSET.matcher(contentType == null ? "" : contentType);
        String value = matcher.find() ? matcher.group(1) : configuredCharset;
        if (value == null || value.isBlank()) {
            return StandardCharsets.UTF_8.name();
        }
        try {
            return Charset.forName(value.trim().toUpperCase(Locale.ROOT)).name();
        } catch (Exception ignored) {
            return StandardCharsets.UTF_8.name();
        }
    }

    public record ExtractedContent(String html, String text) {
        static ExtractedContent empty() {
            return new ExtractedContent(null, null);
        }

        public boolean present() {
            return text != null && !text.isBlank();
        }
    }
}
