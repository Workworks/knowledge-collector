package com.example.knowledgecollector.capability.storage;

import java.io.InputStream;
import java.util.Map;

public interface ObjectStorageProvider {
    String id();
    StoredObject save(StorageRequest request);
    InputStream read(String objectKey);
    void delete(String objectKey);

    record StorageRequest(String objectKey, String contentType, InputStream content,
                          long contentLength, Map<String, String> metadata) {
    }

    record StoredObject(String objectKey, long contentLength, String checksum) {
    }
}
