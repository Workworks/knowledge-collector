package com.example.knowledgecollector.application.security;
import java.time.OffsetDateTime;
public record UserView(long id,String username,String displayName,String role,boolean enabled,OffsetDateTime lastLoginAt,OffsetDateTime passwordChangedAt,OffsetDateTime createdAt,OffsetDateTime updatedAt){}
