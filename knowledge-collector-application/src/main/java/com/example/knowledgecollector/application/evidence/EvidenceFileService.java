package com.example.knowledgecollector.application.evidence;

import com.example.knowledgecollector.application.capability.ThirdPartyCapabilityService;
import com.example.knowledgecollector.application.exception.BusinessRuleException;
import com.example.knowledgecollector.capability.storage.ObjectStorageProvider;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class EvidenceFileService {
    private final EvidenceFileGateway gateway;
    private final ThirdPartyCapabilityService capabilities;
    private final List<ObjectStorageProvider> providers;
    public EvidenceFileService(EvidenceFileGateway gateway, ThirdPartyCapabilityService capabilities,
                               List<ObjectStorageProvider> providers) {
        this.gateway=gateway;this.capabilities=capabilities;this.providers=providers;
    }
    public EvidenceFileView store(String ownerType,long ownerId,String kind,String fileName,String contentType,byte[] bytes) {
        if(bytes==null||bytes.length==0) throw new BusinessRuleException("EVIDENCE-FILE-EMPTY","文件内容不能为空");
        String normalizedOwner=normalize(ownerType,List.of("ARTICLE","CARD","CLAIM","DRAFT","EXTRACTION","GENERAL"),"EVIDENCE-OWNER-TYPE-INVALID");
        String normalizedKind=normalize(kind,List.of("RAW_HTML","SCREENSHOT","ATTACHMENT","SUPPLEMENT"),"EVIDENCE-FILE-KIND-INVALID");
        String providerId=capabilities.defaultProvider("STORAGE");
        ObjectStorageProvider provider=providers.stream().filter(p->p.id().equalsIgnoreCase(providerId)).findFirst()
                .orElseThrow(()->new BusinessRuleException("STORAGE-PROVIDER-NOT-FOUND","未加载对象存储 Provider："+providerId));
        String safeName=(fileName==null?"evidence.bin":fileName).replaceAll("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]","_");
        String key=normalizedOwner.toLowerCase()+"/"+ownerId+"/"+OffsetDateTime.now().toLocalDate()+"/"+UUID.randomUUID()+"-"+safeName;
        var stored=capabilities.invoke(providerId,"SAVE_OBJECT","文件与快照",normalizedOwner+":"+ownerId+" "+safeName,
                ()->provider.save(new ObjectStorageProvider.StorageRequest(key,contentType,new ByteArrayInputStream(bytes),bytes.length,
                        Map.of("ownerType",normalizedOwner,"ownerId",String.valueOf(ownerId),"kind",normalizedKind))),
                result->result.objectKey()+" ("+result.contentLength()+" bytes)");
        return gateway.add(normalizedOwner,ownerId,normalizedKind,safeName,stored.objectKey(),contentType,stored.contentLength(),stored.checksum(),providerId);
    }
    public List<EvidenceFileView> list(String ownerType,Long ownerId){return gateway.list(ownerType,ownerId);}
    public EvidenceDownload download(long id){
        EvidenceFileView file=gateway.get(id);
        ObjectStorageProvider provider=providers.stream().filter(p->p.id().equalsIgnoreCase(file.providerId())).findFirst()
                .orElseThrow(()->new BusinessRuleException("STORAGE-PROVIDER-NOT-FOUND","未加载对象存储 Provider"));
        byte[] bytes=capabilities.invoke(file.providerId(),"READ_OBJECT","文件与快照",file.objectKey(),()->{
            try(var input=provider.read(file.objectKey())){return input.readAllBytes();}catch(Exception e){throw new IllegalStateException(e);}},
                value->value.length+" bytes");
        return new EvidenceDownload(file,bytes);
    }
    public record EvidenceDownload(EvidenceFileView file,byte[] bytes){}
    private String normalize(String value,List<String> allowed,String code){String normalized=value==null?"":value.trim().toUpperCase(java.util.Locale.ROOT);if(!allowed.contains(normalized))throw new BusinessRuleException(code,"不支持的证据归属或文件类型："+value);return normalized;}
}
