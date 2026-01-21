SET NAMES utf8mb4;
USE job_analysis;

DROP PROCEDURE IF EXISTS job_schema_update;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'ai_chat_history'
                    AND COLUMN_NAME = 'app_id') THEN
        ALTER TABLE `job_analysis`.`ai_chat_history` ADD COLUMN `app_id` bigint(20) DEFAULT NULL AFTER `username`;
    END IF;

    IF EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'ai_chat_history'
                AND COLUMN_NAME = 'ai_input') THEN
        ALTER TABLE `job_analysis`.`ai_chat_history` MODIFY COLUMN `ai_input`  MEDIUMTEXT NOT NULL COMMENT '提交给AI的输入内容';
    END IF;

    IF EXISTS(SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = db
                AND TABLE_NAME = 'ai_chat_history'
                AND COLUMN_NAME = 'ai_answer') THEN
        ALTER TABLE `job_analysis`.`ai_chat_history` MODIFY COLUMN `ai_answer`  MEDIUMTEXT NULL DEFAULT NULL COMMENT 'AI回答的内容';
    END IF;

COMMIT;
END <JOB_UBF>
DELIMITER ;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
