USE `job_manage`;

DROP PROCEDURE IF EXISTS job_data_update;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_data_update()
BEGIN

    DECLARE maxSize VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT `value` FROM `global_setting` WHERE `key`='FILE_UPLOAD_MAX_SIZE' INTO maxSize;
    IF EXISTS(SELECT 1
                  FROM `global_setting`
                  WHERE `key` = 'FILE_UPLOAD_MAX_SIZE') THEN
		INSERT IGNORE INTO `global_setting`(`key`, `value`, `decription`) VALUES ('FILE_UPLOAD_SETTING', CONCAT('{"maxSize":"',maxSize,'"}'), 'setting of upload file');

    END IF;

    COMMIT;
END <JOB_UBF>
DELIMITER ;
CALL job_data_update();

DROP PROCEDURE IF EXISTS job_data_update;
