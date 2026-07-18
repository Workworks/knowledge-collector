package com.example.knowledgecollector.application.extraction;
import com.example.knowledgecollector.capability.extraction.ContentExtractionProvider;
import java.util.List;
public interface ContentExtractionGateway {
 long start(ContentExtractionCommand command, String providerId);
 void succeed(long id, ContentExtractionProvider.ExtractionResult result, long durationMillis);
 void fail(long id, String error, long durationMillis);
 void updateArticle(long articleId, ContentExtractionProvider.ExtractionResult result);
 ContentExtractionView get(long id);
 List<ContentExtractionView> list(Long articleId, int limit);
}
