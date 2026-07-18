package com.example.knowledgecollector.provider.storage;

import com.example.knowledgecollector.capability.management.ManagedCapabilityProvider;
import com.example.knowledgecollector.capability.storage.ObjectStorageProvider;
import io.minio.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MinioObjectStorageProvider implements ObjectStorageProvider, ManagedCapabilityProvider {
    private final Map<String, byte[]> memory = new ConcurrentHashMap<>();
    private volatile RuntimeConfiguration configuration;
    public MinioObjectStorageProvider(
            @Value("${knowledge-collector.storage.minio.enabled:false}") boolean enabled,
            @Value("${knowledge-collector.storage.minio.endpoint:http://127.0.0.1:9000}") String endpoint,
            @Value("${knowledge-collector.storage.minio.bucket:knowledge-collector}") String bucket,
            @Value("${knowledge-collector.storage.minio.access-key:}") String access,
            @Value("${knowledge-collector.storage.minio.secret-key:}") String secret) {
        configuration = new RuntimeConfiguration(enabled, endpoint, bucket, "ACCESS_SECRET",
                access.isBlank() ? null : access + ":" + secret);
    }
    @Override public String id(){return "minio";}
    @Override public String serviceType(){return "STORAGE";}
    @Override public String displayName(){return "MinIO 原始证据存储";}
    @Override public String implementationName(){return getClass().getName();}
    @Override public List<String> businessUsages(){return List.of("原始网页与截图", "补充材料上传", "文件下载与版本管理");}
    @Override public RuntimeConfiguration currentConfiguration(){return configuration;}
    @Override public synchronized void configure(RuntimeConfiguration value){ configuration=value; }
    @Override public ConnectionResult testConnection(){
        if(!configuration.enabled()) return new ConnectionResult(false,"MinIO 已停用",List.of());
        if(memory()) return new ConnectionResult(true,"MinIO 内存验收存储可用",List.of(bucket()));
        try { boolean exists=client().bucketExists(BucketExistsArgs.builder().bucket(bucket()).build());
            return new ConnectionResult(exists,exists?"MinIO 连接成功，存储桶可用":"MinIO 存储桶不存在："+bucket(),List.of(bucket()));
        } catch(Exception e){return new ConnectionResult(false,"无法连接 MinIO："+safe(e),List.of());}
    }
    @Override public StoredObject save(StorageRequest request){
        ensure();
        try {
            byte[] bytes=request.content().readAllBytes();
            if(memory()) memory.put(request.objectKey(),bytes);
            else client().putObject(PutObjectArgs.builder().bucket(bucket()).object(request.objectKey())
                    .stream(new ByteArrayInputStream(bytes),bytes.length,-1).contentType(request.contentType()).userMetadata(request.metadata()).build());
            return new StoredObject(request.objectKey(),bytes.length,sha(bytes));
        }catch(Exception e){throw new IllegalStateException("MINIO-SAVE-FAILED: "+safe(e),e);}
    }
    @Override public InputStream read(String key){
        ensure(); try { if(memory()){byte[] value=memory.get(key);if(value==null)throw new IllegalStateException("对象不存在");return new ByteArrayInputStream(value);}
            return client().getObject(GetObjectArgs.builder().bucket(bucket()).object(key).build());
        }catch(Exception e){throw new IllegalStateException("MINIO-READ-FAILED: "+safe(e),e);}
    }
    @Override public void delete(String key){ensure();try{if(memory())memory.remove(key);else client().removeObject(RemoveObjectArgs.builder().bucket(bucket()).object(key).build());}catch(Exception e){throw new IllegalStateException("MINIO-DELETE-FAILED: "+safe(e),e);}}
    private boolean memory(){return configuration.endpoint().startsWith("memory://");}
    private String bucket(){return configuration.model()==null||configuration.model().isBlank()?"knowledge-collector":configuration.model();}
    private MinioClient client(){String[] pair=configuration.credential()==null?new String[0]:configuration.credential().split(":",2);if(pair.length!=2)throw new IllegalStateException("请按 accessKey:secretKey 配置 MinIO 凭据");return MinioClient.builder().endpoint(URI.create(configuration.endpoint()).toString()).credentials(pair[0],pair[1]).build();}
    private void ensure(){if(!configuration.enabled())throw new IllegalStateException("MINIO-DISABLED: MinIO 已停用");}
    private String sha(byte[] bytes)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
    private String safe(Exception e){return e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
}
