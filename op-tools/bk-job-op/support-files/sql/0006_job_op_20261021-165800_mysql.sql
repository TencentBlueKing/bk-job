USE job_op;

SET @index_exists = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pull_request'
      AND INDEX_NAME = 'idx_update_time_pr_id'
);

SET @add_index_sql = IF(
    @index_exists = 0,
    'ALTER TABLE `pull_request` ADD KEY `idx_update_time_pr_id` (`update_time`, `pr_id`)',
    'SELECT 1'
);

PREPARE add_index_statement FROM @add_index_sql;
EXECUTE add_index_statement;
DEALLOCATE PREPARE add_index_statement;
