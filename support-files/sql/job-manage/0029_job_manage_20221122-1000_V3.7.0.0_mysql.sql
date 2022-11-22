USE job_manage;

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS job_schema_update;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_schema_update()
BEGIN

  DECLARE db VARCHAR(100);
  SET AUTOCOMMIT = 0;
  SELECT DATABASE() INTO db;
  
  -- Update `host` schema
  ALTER TABLE host MODIFY COLUMN ip_v6 VARCHAR(2000) DEFAULT NULL COMMENT '主机IPv6地址，可能存在多个';

  -- Update `white_ip_ip` schema
  ALTER TABLE white_ip_ip MODIFY COLUMN ip_v6 VARCHAR(2000) DEFAULT NULL COMMENT '主机IPv6地址，可能存在多个';

COMMIT;
END <JOB_UBF>
DELIMITER ;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;
