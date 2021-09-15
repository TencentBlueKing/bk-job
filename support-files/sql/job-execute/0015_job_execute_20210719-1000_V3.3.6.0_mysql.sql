USE job_execute;

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS job_schema_update;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;
        
    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'task_instance'
                    AND INDEX_NAME = 'idx_operator_app_id_create_time') THEN
        ALTER TABLE task_instance ADD INDEX idx_operator_app_id_create_time(`operator`,`app_id`,`create_time`);
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'task_instance'
                    AND INDEX_NAME = 'idx_app_id_status_create_time') THEN
        ALTER TABLE task_instance ADD INDEX idx_app_id_status_create_time(`app_id`,`status`,`create_time`);
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'task_instance'
                    AND INDEX_NAME = 'idx_app_id_task_id_create_time') THEN
        ALTER TABLE task_instance ADD INDEX idx_app_id_task_id_create_time(`app_id`,`task_id`,`create_time`);
    END IF;

    IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'task_instance'
                    AND INDEX_NAME = 'idx_app_id_cron_task_id_create_time') THEN
        ALTER TABLE task_instance ADD INDEX idx_app_id_cron_task_id_create_time(`app_id`,`cron_task_id`,`create_time`);
    END IF;
	
	IF NOT EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'task_instance'
                    AND INDEX_NAME = 'idx_create_time') THEN
        ALTER TABLE task_instance ADD INDEX idx_create_time(`create_time`);
    END IF;
	
	
	
    IF EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'task_instance'
                    AND INDEX_NAME = 'idx_create_time_app_name') THEN
        ALTER TABLE task_instance DROP INDEX idx_create_time_app_name;
    END IF;
	
    IF EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'task_instance'
                    AND INDEX_NAME = 'idx_create_time_app_operator') THEN
        ALTER TABLE task_instance DROP INDEX idx_create_time_app_operator;
    END IF;

    IF EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'task_instance'
                    AND INDEX_NAME = 'idx_create_time_app_task') THEN
        ALTER TABLE task_instance DROP INDEX idx_create_time_app_task;
    END IF;

    IF EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'task_instance'
                    AND INDEX_NAME = 'idx_create_time_app_status') THEN
        ALTER TABLE task_instance DROP INDEX idx_create_time_app_status;
    END IF;

    IF EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'task_instance'
                    AND INDEX_NAME = 'idx_create_time_app_type') THEN
        ALTER TABLE task_instance DROP INDEX idx_create_time_app_type;
    END IF;	

    IF EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'task_instance'
                    AND INDEX_NAME = 'idx_create_time_app_startup') THEN
        ALTER TABLE task_instance DROP INDEX idx_create_time_app_startup;
    END IF;	
	
    IF EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'task_instance'
                    AND INDEX_NAME = 'idx_appId_totalTime_createTime') THEN
        ALTER TABLE task_instance DROP INDEX idx_appId_totalTime_createTime;
    END IF;	
	
	IF EXISTS(SELECT 1
                  FROM information_schema.statistics
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'task_instance'
                    AND INDEX_NAME = 'idx_app_cron_id') THEN
        ALTER TABLE task_instance DROP INDEX idx_app_cron_id;
    END IF;	
	
    COMMIT;
END <JOB_UBF>
DELIMITER ;
COMMIT;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
