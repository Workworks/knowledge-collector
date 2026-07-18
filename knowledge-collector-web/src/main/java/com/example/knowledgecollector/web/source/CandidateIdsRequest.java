package com.example.knowledgecollector.web.source;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
public record CandidateIdsRequest(@NotEmpty List<Long> ids) {}
