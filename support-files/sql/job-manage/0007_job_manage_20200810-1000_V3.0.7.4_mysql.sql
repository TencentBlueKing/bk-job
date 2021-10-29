SET NAMES utf8mb4;
USE job_manage;

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
                    AND TABLE_NAME = 'notify_template'
                    AND INDEX_NAME = 'uniq_code_channel_isDefault') THEN
        ALTER TABLE notify_template ADD UNIQUE INDEX uniq_code_channel_isDefault(code,channel,is_default);
    END IF;

    COMMIT;
END <JOB_UBF>
DELIMITER ;
CALL job_schema_update();

DROP PROCEDURE IF EXISTS job_schema_update;


REPLACE INTO `job_manage`.`notify_template`(`code`, `name`, `channel`, `title`, `content`, `title_en`, `content_en`, `creator`, `last_modify_user`, `create_time`, `last_modify_time`, `is_default`) VALUES ('beforeCronJobExecute', '定时任务执行前通知模板', 'common', '定时任务执行前提醒：您的业务[{{ APP_NAME }}]有一个定时任务[{{ cron_name }}]将在{{ notify_time }}分钟后执行，请知悉。【蓝鲸作业平台】', '定时任务执行前提醒：您的业务[{{ APP_NAME }}]有一个定时任务[{{ cron_name }}]将在{{ notify_time }}分钟后执行，请知悉。【蓝鲸作业平台】', 'Cron Pre-Launch Notification: There\'s a task of [{{ APP_NAME }}] will launch automatically after {{ notify_time }} mins, more details: {{BASE_HOST}}{{cron_uri}}【BlueKing JOB】', 'Cron Pre-Launch Notification: There\'s a task of [{{ APP_NAME }}] will launch automatically after {{ notify_time }} mins, more details: {{BASE_HOST}}{{cron_uri}}【BlueKing JOB】', 'admin', 'admin', 0, 0, b'1');
REPLACE INTO `job_manage`.`notify_template`(`code`, `name`, `channel`, `title`, `content`, `title_en`, `content_en`, `creator`, `last_modify_user`, `create_time`, `last_modify_time`, `is_default`) VALUES ('beforeCronJobEnd', '定时任务结束前通知模板', 'common', '定时任务结束前提醒：您的业务[{{ APP_NAME }}]有一个定时任务[{{ cron_name }}]将在{{ notify_time }}分钟后结束并关闭，请知悉。【蓝鲸作业平台】', '定时任务结束前提醒：您的业务[{{ APP_NAME }}]有一个定时任务[{{ cron_name }}]将在{{ notify_time }}分钟后结束并关闭，请知悉。【蓝鲸作业平台】', 'Cron End-time Notification: There\'s a task of [{{ APP_NAME }}] will Turn-Off automatically after {{ notify_time }} mins, more details: {{BASE_HOST}}{{cron_uri}}【BlueKing JOB】', 'Cron End-time Notification: There\'s a task of [{{ APP_NAME }}] will Turn-Off automatically after {{ notify_time }} mins, more details: {{BASE_HOST}}{{cron_uri}}【BlueKing JOB】', 'admin', 'admin', 0, 0, b'1');
REPLACE INTO `job_manage`.`notify_template`(`code`, `name`, `channel`, `title`, `content`, `title_en`, `content_en`, `creator`, `last_modify_user`, `create_time`, `last_modify_time`, `is_default`) VALUES ('confirmation', '人工确认', 'common', '【蓝鲸作业平台】待确认任务', '您的业务[{{task.app.name}}]当前有一个待确认的作业任务:{{task.name}}({{task.detail.url}})需要处理，请尽快操作。【蓝鲸作业平台】', '【BlueKing JOB】There\'s one message needs you to confirm', 'There\'s a task of [{{task.app.name}}] is on \"Confirmation\" status now, more details: {{task.name}}({{task.detail.url}})【BlueKing JOB】', 'admin', 'admin', 0, 0, b'1');
REPLACE INTO `job_manage`.`notify_template`(`code`, `name`, `channel`, `title`, `content`, `title_en`, `content_en`, `creator`, `last_modify_user`, `create_time`, `last_modify_time`, `is_default`) VALUES ('executeSuccess', '执行成功', 'common', '【蓝鲸作业平台】任务执行成功通知', '您的业务[{{task.app.name}}]当前有一个任务:{{task.name}}({{task.detail.url}})已执行成功，请尽快查看。【蓝鲸作业平台】', '【BlueKing JOB】Task has running successfully', 'There\'s a task of [{{task.app.name}}] is running successfully, more details: {{task.name}}({{task.detail.url}})【BlueKing JOB】', 'admin', 'admin', 0, 0, b'1');
REPLACE INTO `job_manage`.`notify_template`(`code`, `name`, `channel`, `title`, `content`, `title_en`, `content_en`, `creator`, `last_modify_user`, `create_time`, `last_modify_time`, `is_default`) VALUES ( 'executeFailure', '执行失败', 'common', '【蓝鲸作业平台】任务执行失败通知', '您的业务[{{task.app.name}}]当前有一个任务:{{task.name}}({{task.detail.url}})执行失败，请尽快查看。【蓝鲸作业平台】', '【BlueKing JOB】Task failed during execution', 'There\'s a task of [{{task.app.name}}] is run \"Failed\", please be noted! more details: {{task.name}}({{task.detail.url}})【BlueKing JOB】', 'admin', 'admin', 0, 0, b'1');
