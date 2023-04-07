USE job_execute;

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS job_schema_update;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_schema_update()
BEGIN
  SET AUTOCOMMIT = 0;

  ALTER TABLE gse_script_agent_task MODIFY `agent_id` varchar(64); 
  ALTER TABLE gse_file_agent_task MODIFY `agent_id` varchar(64);

  COMMIT;
END <JOB_UBF>
DELIMITER ;
COMMIT;

CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
