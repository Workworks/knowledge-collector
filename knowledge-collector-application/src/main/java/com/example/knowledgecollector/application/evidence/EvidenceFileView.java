package com.example.knowledgecollector.application.evidence;
import java.time.OffsetDateTime;
public record EvidenceFileView(Long id,String ownerType,long ownerId,String fileKind,String fileName,String objectKey,
 String contentType,long contentLength,String checksum,int versionNo,String providerId,OffsetDateTime createdAt) {}
