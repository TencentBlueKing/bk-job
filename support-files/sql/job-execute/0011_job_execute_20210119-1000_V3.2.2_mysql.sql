USE job_execute;
SET NAMES utf8mb4;

alter table task_instance add index idx_appId_totalTime_createTime(app_id,total_time,create_time);
