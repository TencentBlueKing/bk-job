USE `job_manage`;

update `global_setting` set `key`='FILE_UPLOAD_SETTING',`value`=CONCAT('{"maxSize":"',`value`,'"}'),`decription`='setting of upload file' 
where `key` ='FILE_UPLOAD_MAX_SIZE';
