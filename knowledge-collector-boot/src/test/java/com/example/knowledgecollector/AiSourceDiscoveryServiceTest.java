package com.example.knowledgecollector;

import com.example.knowledgecollector.application.crawl.CrawlTaskService;
import com.example.knowledgecollector.application.source.AiSourceDiscoveryService;
import com.example.knowledgecollector.application.source.CrawlSourceRepository;
import com.example.knowledgecollector.application.source.CrawlSourceService;
import com.example.knowledgecollector.application.topic.TopicRepository;
import com.example.knowledgecollector.application.rule.SourceRuleService;
import com.example.knowledgecollector.capability.intelligence.ContentIntelligenceProvider;
import com.example.knowledgecollector.capability.intelligence.ConversationalIntelligenceProvider;
import com.example.knowledgecollector.domain.source.CrawlSource;
import com.example.knowledgecollector.domain.source.SourceType;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiSourceDiscoveryServiceTest {
    @Test
    void importsOnlyUniqueAndReachableAiSuggestionsAsVerified() {
        CrawlSourceService sources = mock(CrawlSourceService.class);
        CrawlTaskService tasks = mock(CrawlTaskService.class);
        TopicRepository topics = mock(TopicRepository.class);
        SourceRuleService rules = mock(SourceRuleService.class);
        ConversationalIntelligenceProvider ai = new FakeAi();
        when(topics.findAllEnabled()).thenReturn(List.of());
        when(sources.existsByFeedUrl(anyString())).thenReturn(false);
        when(tasks.testCandidate(any())).thenReturn(3).thenThrow(new IllegalStateException("无效来源"));
        OffsetDateTime now = OffsetDateTime.now();
        CrawlSource created = new CrawlSource(7L,"AI_TEST","AI 官方",SourceType.RSS,
                "https://example.org","https://example.org/feed.xml","zh-CN","UTF-8","test",15,2,1000,
                true,true,false,false,true,null,null,0,"UNKNOWN",null,null,null,Set.of(),0,now,now);
        CrawlSource verified = new CrawlSource(7L,"AI_TEST","AI 官方",SourceType.RSS,
                "https://example.org","https://example.org/feed.xml","zh-CN","UTF-8","test",15,2,1000,
                true,true,false,false,true,null,null,0,"VERIFIED",now,"访问正常",null,Set.of(),1,now,now);
        when(sources.create(any())).thenReturn(created);
        when(sources.updateHealth(eq(7L),eq(true),anyString())).thenReturn(verified);

        var result = new AiSourceDiscoveryService(sources,tasks,topics,rules,List.of(ai),"ollama")
                .discover("AI","中文",10,"权威");

        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.sources()).extracting(CrawlSource::healthStatus).containsExactly("VERIFIED");
        assertThat(result.rejected()).hasSize(1);
        verify(sources,times(1)).create(any());
    }

    private static class FakeAi implements ConversationalIntelligenceProvider {
        public String id(){return "ollama";}
        public ContentIntelligenceProvider.ProviderStatus status(){return new ContentIntelligenceProvider.ProviderStatus("ollama",true,true,"fake","local","ok");}
        public ChatResult chat(ChatRequest request){return new ChatResult("""
                RSS|AI 官方|https://example.org|https://example.org/feed.xml
                RSS|重复来源|https://example.org|https://example.org/feed.xml
                ATOM|不可访问|https://bad.example|https://bad.example/atom.xml
                ""","ollama","fake",0,0,1);}
    }
}
