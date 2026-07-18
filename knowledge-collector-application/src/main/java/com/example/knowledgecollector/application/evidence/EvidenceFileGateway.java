package com.example.knowledgecollector.application.evidence;
import java.util.List;
public interface EvidenceFileGateway {
 EvidenceFileView add(String ownerType,long ownerId,String fileKind,String fileName,String objectKey,String contentType,long length,String checksum,String providerId);
 List<EvidenceFileView> list(String ownerType,Long ownerId);
 EvidenceFileView get(long id);
}
