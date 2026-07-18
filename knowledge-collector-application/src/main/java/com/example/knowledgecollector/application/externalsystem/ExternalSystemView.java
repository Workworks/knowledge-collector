package com.example.knowledgecollector.application.externalsystem;
import java.time.OffsetDateTime;
public record ExternalSystemView(long id,String code,String name,String type,String description,String icon,String accessUrl,String healthUrl,String openMode,boolean enabled,String allowedRole,int sortOrder,String status,OffsetDateTime lastCheckedAt,OffsetDateTime lastSuccessAt,String lastError){}
