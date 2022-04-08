USE `job_manage`;

INSERT IGNORE INTO `job_manage`.`global_setting`(`key`, `value`, `decription`) VALUES ('FILE_UPLOAD_SETTING', '', '');


DROP PROCEDURE IF EXISTS job_data_update;

DELIMITER <JOB_UBF>

CREATE PROCEDURE job_data_update()
BEGIN

    DECLARE maxSize VARCHAR(100);
    SET AUTOCOMMIT = 0;
    SELECT `value` FROM `global_setting` WHERE `key`='FILE_UPLOAD_MAX_SIZE' INTO maxSize;
    IF EXISTS(SELECT 1
                  FROM `global_setting`
                  WHERE `key` = 'FILE_UPLOAD_SETTING'
                    AND (`value` IS NULL OR `value`='') AND (`decription` IS NULL OR `decription`='')) THEN
        UPDATE `global_setting` SET `value`=CONCAT('{"maxSize":"',maxSize,'"}'),`decription`='setting of upload file' WHERE `key` ='FILE_UPLOAD_SETTING';
		DELETE FROM `global_setting` WHERE `key` = 'FILE_UPLOAD_MAX_SIZE';
    END IF;

    COMMIT;
END <JOB_UBF>
DELIMITER ;
CALL job_data_update();

DROP PROCEDURE IF EXISTS job_data_update;