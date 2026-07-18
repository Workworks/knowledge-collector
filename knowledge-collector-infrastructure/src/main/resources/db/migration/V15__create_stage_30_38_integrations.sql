CREATE SEQUENCE advanced_execution_seq START WITH 1 INCREMENT BY 1;
CREATE TABLE advanced_execution (
    id BIGINT PRIMARY KEY,
    stage_no VARCHAR(8) NOT NULL,
    operation VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    request_json CLOB,
    result_json CLOB,
    error_message CLOB,
    retry_of_id BIGINT,
    duration_millis BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_advanced_retry FOREIGN KEY(retry_of_id) REFERENCES advanced_execution(id)
);
CREATE INDEX idx_advanced_stage_created ON advanced_execution(stage_no, created_at);

INSERT INTO crawl_source(source_code,source_name,source_type,home_url,feed_url,language,charset,user_agent,
 timeout_seconds,max_retries,request_interval_millis,obey_robots,fetch_full_content,summary_only,save_snapshot,enabled,notes,created_at,updated_at)
SELECT 'SYSTEM_IMPORT','文档与学术资料导入','MANUAL_URL','http://127.0.0.1/imports','http://127.0.0.1/imports',
 'zh-CN','UTF-8','KnowledgeCollector-Import',30,1,0,FALSE,FALSE,FALSE,FALSE,FALSE,
 'Stage 30/33 系统内置资料来源',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP
WHERE NOT EXISTS(SELECT 1 FROM crawl_source WHERE source_code='SYSTEM_IMPORT');

INSERT INTO third_party_service(provider_id,service_name,service_type,implementation_name,endpoint,model,
 authentication_type,enabled,default_provider,fallback_provider,user_configured,health_status,created_at,updated_at)
VALUES('tika-document','Apache Tika / OCR','DOCUMENT_EXTRACTION','TikaDocumentProvider','http://127.0.0.1:9998','tika-ocr','NONE',TRUE,TRUE,FALSE,FALSE,'UNKNOWN',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
INSERT INTO third_party_service(provider_id,service_name,service_type,implementation_name,endpoint,model,
 authentication_type,enabled,default_provider,fallback_provider,user_configured,health_status,created_at,updated_at)
VALUES('qdrant-vector','Qdrant 向量检索','VECTOR_SEARCH','QdrantSearchProvider','http://127.0.0.1:6333','LOCAL_HASH_64','NONE',TRUE,TRUE,FALSE,FALSE,'UNKNOWN',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
INSERT INTO third_party_service(provider_id,service_name,service_type,implementation_name,endpoint,model,
 authentication_type,enabled,default_provider,fallback_provider,user_configured,health_status,created_at,updated_at)
VALUES('crossref-academic','Crossref / PubMed','ACADEMIC_SEARCH','AcademicDiscoveryProvider','https://api.crossref.org','Crossref REST','NONE',TRUE,TRUE,FALSE,FALSE,'UNKNOWN',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
INSERT INTO third_party_service(provider_id,service_name,service_type,implementation_name,endpoint,model,
 authentication_type,enabled,default_provider,fallback_provider,user_configured,health_status,created_at,updated_at)
VALUES('cloud-llm','云端大模型','CONTENT_INTELLIGENCE','OpenAiCompatibleProvider','https://api.openai.com/v1/chat/completions','user-configured','BEARER',FALSE,FALSE,TRUE,FALSE,'UNKNOWN',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
INSERT INTO third_party_service(provider_id,service_name,service_type,implementation_name,endpoint,model,
 authentication_type,enabled,default_provider,fallback_provider,user_configured,health_status,created_at,updated_at)
VALUES('langfuse-observability','Langfuse','AI_OBSERVABILITY','LangfuseTraceProvider','http://127.0.0.1:3001',NULL,'BASIC',FALSE,TRUE,FALSE,FALSE,'UNKNOWN',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
INSERT INTO third_party_service(provider_id,service_name,service_type,implementation_name,endpoint,model,
 authentication_type,enabled,default_provider,fallback_provider,user_configured,health_status,created_at,updated_at)
VALUES('prometheus-grafana','Prometheus / Grafana','MONITORING','PrometheusMonitoringProvider','http://127.0.0.1:9090','Grafana','NONE',TRUE,TRUE,FALSE,FALSE,'UNKNOWN',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
INSERT INTO third_party_service(provider_id,service_name,service_type,implementation_name,endpoint,model,
 authentication_type,enabled,default_provider,fallback_provider,user_configured,health_status,created_at,updated_at)
VALUES('ntfy-notification','ntfy 通知','NOTIFICATION','NtfyNotificationProvider','http://127.0.0.1:8085',NULL,'NONE',TRUE,TRUE,FALSE,FALSE,'UNKNOWN',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
INSERT INTO third_party_service(provider_id,service_name,service_type,implementation_name,endpoint,model,
 authentication_type,enabled,default_provider,fallback_provider,user_configured,health_status,created_at,updated_at)
VALUES('n8n-workflow','n8n 外部工作流','WORKFLOW','N8nWorkflowProvider','http://127.0.0.1:5678',NULL,'WEBHOOK',TRUE,TRUE,FALSE,FALSE,'UNKNOWN',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP);
