USE job_execute;
SET NAMES utf8mb4;

alter table step_instance add index idx_app_id_create_time(app_id,create_time);
