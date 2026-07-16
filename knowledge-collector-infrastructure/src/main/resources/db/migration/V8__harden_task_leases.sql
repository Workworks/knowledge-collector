ALTER TABLE crawl_task ADD COLUMN heartbeat_at TIMESTAMP WITH TIME ZONE;

UPDATE crawl_task
SET status = 'FAILED',
    active_source_id = NULL,
    error_code = 'TASK-INTERRUPTED',
    error_message = '应用升级或重启时回收未正常结束的任务',
    finished_at = CURRENT_TIMESTAMP,
    duration_millis = DATEDIFF('MILLISECOND', COALESCE(started_at, created_at), CURRENT_TIMESTAMP)
WHERE status IN ('CREATED', 'RUNNING');

CREATE INDEX idx_task_active_heartbeat ON crawl_task(active_source_id, heartbeat_at);
