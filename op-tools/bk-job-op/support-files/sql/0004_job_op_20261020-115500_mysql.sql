USE job_op;

-- 防御性回填早期试运行或手工导入数据中的空更新时间。
-- 0003 的实际表结构包含 row_update_time，转换为毫秒时间戳后与应用层 update_time 的单位一致。
SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'issue_status'
          AND COLUMN_NAME = 'update_time'
    )
    AND EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'issue_status'
          AND COLUMN_NAME = 'row_update_time'
    ),
    'UPDATE `issue_status`
     SET `update_time` = CAST(UNIX_TIMESTAMP(`row_update_time`) * 1000 AS UNSIGNED)
     WHERE `update_time` IS NULL',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 回填后恢复标准结构的非空约束。
SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'issue_status'
          AND COLUMN_NAME = 'update_time'
    ),
    'ALTER TABLE `issue_status`
     MODIFY COLUMN `update_time` BIGINT NOT NULL COMMENT ''状态最后更新时间（毫秒时间戳，应用层写入）''',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 无关键字的高频列表查询可按更新时间倒序扫描该复合索引。
SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'issue_status'
    )
    AND NOT EXISTS(
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'issue_status'
          AND INDEX_NAME = 'idx_update_time_id'
    ),
    'ALTER TABLE `issue_status`
     ADD INDEX `idx_update_time_id` (`update_time`, `id`)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- issue_no 已不再读写，先安全删除旧索引，再删除列。
SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'issue_status'
          AND INDEX_NAME = 'idx_issue_no'
    ),
    'ALTER TABLE `issue_status` DROP INDEX `idx_issue_no`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    EXISTS(
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'issue_status'
          AND COLUMN_NAME = 'issue_no'
    ),
    'ALTER TABLE `issue_status` DROP COLUMN `issue_no`',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
