CREATE TABLE article_ai_analysis (
    article_id BIGINT PRIMARY KEY,
    provider VARCHAR(64) NOT NULL,
    model VARCHAR(128),
    status VARCHAR(32) NOT NULL,
    result_json CLOB,
    error_message VARCHAR(2000),
    analyzed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_ai_analysis_article FOREIGN KEY(article_id) REFERENCES article(id) ON DELETE CASCADE
);

CREATE INDEX idx_ai_analysis_status ON article_ai_analysis(status, analyzed_at);
