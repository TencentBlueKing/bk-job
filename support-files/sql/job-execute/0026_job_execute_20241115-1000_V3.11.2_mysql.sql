USE job_execute;

SET NAMES utf8mb4;

-- 更新 schema
DROP PROCEDURE IF EXISTS job_schema_update;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_schema_update()
BEGIN

  DECLARE db VARCHAR(100);
  SET AUTOCOMMIT = 0;
  SELECT DATABASE() INTO db;

  -- task_instance_host
  IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'task_instance_host'
                    AND COLUMN_NAME = 'app_id') THEN
    ALTER TABLE task_instance_host ADD COLUMN `app_id` bigint(20) NOT NULL DEFAULT '0';
  END IF;
  IF NOT EXISTS(SELECT 1
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'task_instance_host'
                  AND INDEX_NAME = 'idx_app_id_task_instance_id') THEN
      ALTER TABLE task_instance_host ADD INDEX `idx_app_id_task_instance_id` (`app_id`,`task_instance_id`);
  END IF;

  -- step_instance_script
  IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'step_instance_script'
                    AND COLUMN_NAME = 'task_instance_id') THEN
    ALTER TABLE step_instance_script ADD COLUMN `task_instance_id` bigint(20) NOT NULL DEFAULT '0';
  END IF;
  IF NOT EXISTS(SELECT 1
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'step_instance_script'
                  AND INDEX_NAME = 'idx_task_instance_id') THEN
      ALTER TABLE step_instance_script ADD INDEX `idx_task_instance_id` (`task_instance_id`);
  END IF;

  -- step_instance_file
  IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'step_instance_file'
                    AND COLUMN_NAME = 'task_instance_id') THEN
    ALTER TABLE step_instance_file ADD COLUMN `task_instance_id` bigint(20) NOT NULL DEFAULT '0';
  END IF;
  IF NOT EXISTS(SELECT 1
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'step_instance_file'
                  AND INDEX_NAME = 'idx_task_instance_id') THEN
      ALTER TABLE step_instance_file ADD INDEX `idx_task_instance_id` (`task_instance_id`);
  END IF;

  -- step_instance_confirm
  IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'step_instance_confirm'
                    AND COLUMN_NAME = 'task_instance_id') THEN
    ALTER TABLE step_instance_confirm ADD COLUMN `task_instance_id` bigint(20) NOT NULL DEFAULT '0';
  END IF;
  IF NOT EXISTS(SELECT 1
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'step_instance_confirm'
                  AND INDEX_NAME = 'idx_task_instance_id') THEN
      ALTER TABLE step_instance_confirm ADD INDEX `idx_task_instance_id` (`task_instance_id`);
  END IF;

  -- gse_task
  IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'gse_task'
                    AND COLUMN_NAME = 'task_instance_id') THEN
    ALTER TABLE gse_task ADD COLUMN `task_instance_id` bigint(20) NOT NULL DEFAULT '0';
  END IF;
  IF NOT EXISTS(SELECT 1
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'gse_task'
                  AND INDEX_NAME = 'idx_task_instance_id') THEN
      ALTER TABLE gse_task ADD INDEX `idx_task_instance_id` (`task_instance_id`);
  END IF;

  -- gse_script_agent_task
  IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'gse_script_agent_task'
                    AND COLUMN_NAME = 'task_instance_id') THEN
    ALTER TABLE gse_script_agent_task ADD COLUMN `task_instance_id` bigint(20) NOT NULL DEFAULT '0';
  END IF;
  IF NOT EXISTS(SELECT 1
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'gse_script_agent_task'
                  AND INDEX_NAME = 'idx_task_instance_id') THEN
      ALTER TABLE gse_script_agent_task ADD INDEX `idx_task_instance_id` (`task_instance_id`);
  END IF;

  -- gse_file_agent_task
  IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'gse_file_agent_task'
                    AND COLUMN_NAME = 'task_instance_id') THEN
    ALTER TABLE gse_file_agent_task ADD COLUMN `task_instance_id` bigint(20) NOT NULL DEFAULT '0';
  END IF;
  IF NOT EXISTS(SELECT 1
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'gse_file_agent_task'
                  AND INDEX_NAME = 'idx_task_instance_id') THEN
      ALTER TABLE gse_file_agent_task ADD INDEX `idx_task_instance_id` (`task_instance_id`);
  END IF;

  -- file_source_task_log
  IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'file_source_task_log'
                    AND COLUMN_NAME = 'task_instance_id') THEN
    ALTER TABLE file_source_task_log ADD COLUMN `task_instance_id` bigint(20) NOT NULL DEFAULT '0';
  END IF;
  IF NOT EXISTS(SELECT 1
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'file_source_task_log'
                  AND INDEX_NAME = 'idx_task_instance_id') THEN
      ALTER TABLE file_source_task_log ADD INDEX `idx_task_instance_id` (`task_instance_id`);
  END IF;
  
  -- step_instance_rolling_task
  IF NOT EXISTS(SELECT 1
                  FROM information_schema.COLUMNS
                  WHERE TABLE_SCHEMA = db
                    AND TABLE_NAME = 'step_instance_rolling_task'
                    AND COLUMN_NAME = 'task_instance_id') THEN
    ALTER TABLE step_instance_rolling_task ADD COLUMN `task_instance_id` bigint(20) NOT NULL DEFAULT '0';
  END IF;
  IF NOT EXISTS(SELECT 1
                FROM information_schema.statistics
                WHERE TABLE_SCHEMA = db
                  AND TABLE_NAME = 'step_instance_rolling_task'
                  AND INDEX_NAME = 'idx_task_instance_id') THEN
      ALTER TABLE step_instance_rolling_task ADD INDEX `idx_task_instance_id` (`task_instance_id`);
  END IF;

  COMMIT;
END <JOB_UBF>
DELIMITER ;
COMMIT;

CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;


-- 更新 task_instance_id 列
DROP PROCEDURE IF EXISTS job_add_task_instance_id;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_add_task_instance_id(IN fromStepInstanceId BIGINT, IN endStepInstanceId BIGINT)
label:BEGIN

  DECLARE minId BIGINT;
  DECLARE maxId BIGINT;
  DECLARE fromId BIGINT;
  DECLARE endId BIGINT;

  SET AUTOCOMMIT = 0;
  -- 如果 step_instance 表为空，无需变更
  IF NOT EXISTS (SELECT 1 FROM step_instance LIMIT 1) THEN
    LEAVE label;
  END IF;

  -- 如果 gse_task 表中不存在task_instance_id = 0，说明已经执行过该变更
  IF NOT EXISTS (SELECT 1 FROM gse_task WHERE task_instance_id = 0 LIMIT 1) THEN
    LEAVE label;
  END IF;


  SELECT MIN(id), MAX(id) INTO minId, maxId FROM step_instance;

  IF fromStepInstanceId > 0 THEN
    SET minId = fromStepInstanceId;
  END IF;

  IF endStepInstanceId > 0 THEN
    SET maxId = endStepInstanceId;
  END IF;

  SET fromId = minId - 1;

  WHILE fromId <= maxId DO
    SELECT MIN(t.id),MAX(t.id) INTO fromId,endId FROM (SELECT id FROM step_instance WHERE id > fromId AND id <= maxId ORDER BY id asc LIMIT 1000) t;

    UPDATE file_source_task_log t1
    INNER JOIN (
      SELECT id, task_instance_id 
      FROM step_instance 
      WHERE id BETWEEN fromId AND endId)
    AS tmp ON t1.step_instance_id = tmp.id 
    SET t1.task_instance_id = tmp.task_instance_id;

    UPDATE gse_file_agent_task t1
    INNER JOIN (
      SELECT id, task_instance_id 
      FROM step_instance 
      WHERE id BETWEEN fromId AND endId)
    AS tmp ON t1.step_instance_id = tmp.id 
    SET t1.task_instance_id = tmp.task_instance_id;

    UPDATE gse_script_agent_task t1
    INNER JOIN (
      SELECT id, task_instance_id 
      FROM step_instance 
      WHERE id BETWEEN fromId AND endId)
    AS tmp ON t1.step_instance_id = tmp.id 
    SET t1.task_instance_id = tmp.task_instance_id;

    UPDATE gse_task t1
    INNER JOIN (
      SELECT id, task_instance_id 
      FROM step_instance 
      WHERE id BETWEEN fromId AND endId)
    AS tmp ON t1.step_instance_id = tmp.id 
    SET t1.task_instance_id = tmp.task_instance_id;

    UPDATE step_instance_confirm t1
    INNER JOIN (
      SELECT id, task_instance_id 
      FROM step_instance 
      WHERE id BETWEEN fromId AND endId)
    AS tmp ON t1.step_instance_id = tmp.id 
    SET t1.task_instance_id = tmp.task_instance_id;

    UPDATE step_instance_script t1
    INNER JOIN (
      SELECT id, task_instance_id 
      FROM step_instance 
      WHERE id BETWEEN fromId AND endId)
    AS tmp ON t1.step_instance_id = tmp.id 
    SET t1.task_instance_id = tmp.task_instance_id;

    UPDATE step_instance_file t1
    INNER JOIN (
      SELECT id, task_instance_id 
      FROM step_instance 
      WHERE id BETWEEN fromId AND endId)
    AS tmp ON t1.step_instance_id = tmp.id 
    SET t1.task_instance_id = tmp.task_instance_id;

    UPDATE step_instance_rolling_task t1
    INNER JOIN (
      SELECT id, task_instance_id 
      FROM step_instance 
      WHERE id BETWEEN fromId AND endId)
    AS tmp ON t1.step_instance_id = tmp.id 
    SET t1.task_instance_id = tmp.task_instance_id;

    COMMIT;

    SET fromId = endId;
  END WHILE;
  
END <JOB_UBF>
DELIMITER ;
COMMIT;

CALL job_add_task_instance_id(-1,-1);
DROP PROCEDURE IF EXISTS job_add_task_instance_id;


-- 更新 task_instance_host 表字段数据
DROP PROCEDURE IF EXISTS job_update_task_instance_host_data;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_update_task_instance_host_data(IN fromTaskInstanceId BIGINT, IN endTaskInstanceId BIGINT)
label:BEGIN

  DECLARE minId BIGINT;
  DECLARE maxId BIGINT;
  DECLARE fromId BIGINT;
  DECLARE endId BIGINT;

  IF NOT EXISTS (SELECT 1 FROM task_instance LIMIT 1) THEN
    LEAVE label;
  END IF;

  IF NOT EXISTS (SELECT 1 FROM task_instance_host WHERE app_id = 0 LIMIT 1) THEN
    LEAVE label;
  END IF;

  SET AUTOCOMMIT = 0;

  SELECT MIN(id), MAX(id) INTO minId, maxId FROM task_instance;
  IF fromTaskInstanceId > 0 THEN
    SET minId = fromTaskInstanceId;
  END IF;

  IF endTaskInstanceId > 0 THEN
    SET maxId = endTaskInstanceId;
  END IF;

  SET fromId = minId - 1;

  WHILE fromId <= maxId DO
    SELECT MIN(t.id),MAX(t.id) INTO fromId,endId FROM (SELECT id FROM task_instance WHERE id > fromId AND id <= maxId ORDER BY id asc LIMIT 1000) t;

    UPDATE task_instance_host t1
    INNER JOIN (
      SELECT id,app_id 
      FROM task_instance 
      WHERE id BETWEEN fromId AND endId)
    AS tmp ON t1.task_instance_id = tmp.id 
    SET t1.app_id = tmp.app_id
    WHERE t1.app_id = 0;

    COMMIT;

    SET fromId = endId;
  END WHILE;
  
END <JOB_UBF>
DELIMITER ;
COMMIT;

CALL job_update_task_instance_host_data(-1,-1);
DROP PROCEDURE IF EXISTS job_update_task_instance_host_data;

