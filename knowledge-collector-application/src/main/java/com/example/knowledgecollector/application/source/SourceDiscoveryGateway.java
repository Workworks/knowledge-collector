package com.example.knowledgecollector.application.source;

import com.example.knowledgecollector.capability.source.SourceDiscoveryProvider;
import java.util.List;

public interface SourceDiscoveryGateway {
    List<DiscoveryCandidateView> replace(String providerId, String topic,
            List<SourceDiscoveryProvider.Candidate> candidates);
    List<DiscoveryCandidateView> list();
    DiscoveryCandidateView get(long id);
    DiscoveryCandidateView validation(long id, boolean valid, String message);
    DiscoveryCandidateView imported(long id, long sourceId);
    DiscoveryCandidateView ignored(long id);
}
