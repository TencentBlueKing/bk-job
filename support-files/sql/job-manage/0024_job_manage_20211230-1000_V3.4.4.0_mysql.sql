USE job_manage;

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS job_schema_update;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_schema_update()
BEGIN

    DECLARE db VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT DATABASE() INTO db;
    
	-- bugfix: Using second instead of millsecond
    UPDATE task_plan SET last_modify_time = last_modify_time/1000 WHERE last_modify_time > 1000000000000;
	-- bugfix: script step script_timeout=0 is invalid, convert from 0 to 86400
    UPDATE task_template_step_script SET script_timeout=86400 WHERE script_timeout=0;
    UPDATE task_plan_step_script SET script_timeout=86400 WHERE script_timeout=0;
	-- bugfix: file step timeout=0 is invalid, convert from 0 to 86400
    UPDATE task_template_step_file SET timeout=86400 WHERE timeout=0;
	UPDATE task_plan_step_file SET script_timeout=86400 WHERE script_timeout=0;
    
	
    COMMIT;
END <JOB_UBF>
DELIMITER ;
COMMIT;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
