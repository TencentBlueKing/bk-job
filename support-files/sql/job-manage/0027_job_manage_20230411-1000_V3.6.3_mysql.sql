USE job_manage;

SET NAMES utf8mb4;

BEGIN;

DELETE FROM `job_manage`.`global_setting` WHERE `key`='DEFAULT_NAME_RULES';
DELETE FROM `job_manage`.`global_setting` WHERE `key`='DEFAULT_NAME_RULES_EN';

COMMIT;
