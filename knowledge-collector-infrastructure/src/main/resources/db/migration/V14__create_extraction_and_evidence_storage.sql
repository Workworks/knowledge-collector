CREATE SEQUENCE content_extraction_job_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE content_extraction_job (
    id BIGINT DEFAULT NEXT VALUE FOR content_extraction_job_seq PRIMARY KEY,
    article_id BIGINT,
    requested_url VARCHAR(2048) NOT NULL,
    final_url VARCHAR(2048),
    method VARCHAR(32) NOT NULL,
    provider_id VARCHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL,
    page_title VARCHAR(1000),
    author VARCHAR(500),
    published_at TIMESTAMP WITH TIME ZONE,
    content_length INTEGER NOT NULL DEFAULT 0,
    content_html CLOB,
    content_text CLOB,
    raw_html CLOB,
    screenshot BLOB,
    duration_millis BIGINT NOT NULL DEFAULT 0,
    error_message CLOB,
    retry_of_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_extraction_article FOREIGN KEY(article_id) REFERENCES article(id) ON DELETE SET NULL,
    CONSTRAINT fk_extraction_retry FOREIGN KEY(retry_of_id) REFERENCES content_extraction_job(id)
);
CREATE INDEX idx_extraction_article ON content_extraction_job(article_id,created_at);

CREATE SEQUENCE evidence_file_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE evidence_file (
    id BIGINT DEFAULT NEXT VALUE FOR evidence_file_seq PRIMARY KEY,
    owner_type VARCHAR(32) NOT NULL,
    owner_id BIGINT NOT NULL,
    file_kind VARCHAR(32) NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    object_key VARCHAR(1000) NOT NULL UNIQUE,
    content_type VARCHAR(200) NOT NULL,
    content_length BIGINT NOT NULL,
    checksum VARCHAR(128),
    version_no INTEGER NOT NULL,
    provider_id VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_evidence_owner ON evidence_file(owner_type,owner_id,file_kind,version_no);

INSERT INTO third_party_service(provider_id,service_name,service_type,implementation_name,endpoint,model,
 authentication_type,enabled,default_provider,fallback_provider,health_status,created_at,updated_at)
VALUES('firecrawl','Firecrawl 正文提取','EXTRACTION','com.example.knowledgecollector.provider.extraction.FirecrawlContentExtractionProvider',
 'http://127.0.0.1:3002',NULL,'BEARER',FALSE,TRUE,FALSE,'UNKNOWN',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
INSERT INTO third_party_service(provider_id,service_name,service_type,implementation_name,endpoint,model,
 authentication_type,enabled,default_provider,fallback_provider,health_status,created_at,updated_at)
VALUES('playwright','Playwright 浏览器抓取','EXTRACTION','com.example.knowledgecollector.provider.extraction.PlaywrightContentExtractionProvider',
 'http://127.0.0.1:3003',NULL,'NONE',FALSE,FALSE,TRUE,'UNKNOWN',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
INSERT INTO third_party_service(provider_id,service_name,service_type,implementation_name,endpoint,model,
 authentication_type,enabled,default_provider,fallback_provider,health_status,created_at,updated_at)
VALUES('minio','MinIO 原始证据存储','STORAGE','com.example.knowledgecollector.provider.storage.MinioObjectStorageProvider',
 'http://127.0.0.1:9000','knowledge-collector','ACCESS_SECRET',FALSE,TRUE,FALSE,'UNKNOWN',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
