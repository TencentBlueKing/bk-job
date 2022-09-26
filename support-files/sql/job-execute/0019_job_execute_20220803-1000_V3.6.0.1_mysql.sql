USE job_execute;

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS job_schema_update;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_schema_update()
BEGIN

  DECLARE db VARCHAR(100);
  SET AUTOCOMMIT = 0;
  SELECT DATABASE() INTO db;

  ALTER TABLE gse_task MODIFY COLUMN `status` tinyint(4) NOT NULL DEFAULT '-1';
  ALTER TABLE gse_script_agent_task MODIFY COLUMN `status` int(11) NOT NULL DEFAULT '-1';
  ALTER TABLE gse_file_agent_task MODIFY COLUMN `status` int(11) NOT NULL DEFAULT '-1';
  ALTER TABLE step_instance_rolling_task MODIFY COLUMN `status` tinyint(4) NOT NULL DEFAULT '-1';

  COMMIT;
END <JOB_UBF>
DELIMITER ;
COMMIT;

CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
