SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS job_leaf DEFAULT CHARACTER SET utf8mb4;
USE job_leaf;


CREATE TABLE IF NOT EXISTS `t_leaf_alloc` (
  `biz_tag` varchar(128)  NOT NULL DEFAULT '',
  `max_id` bigint(20) NOT NULL DEFAULT '1',
  `step` int(11) NOT NULL,
  `description` varchar(256)  DEFAULT NULL,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`biz_tag`)
) ENGINE=InnoDB;


insert ignore into t_leaf_alloc(biz_tag, max_id, step, description) values('job_execute.task_instance', 1, 2000, 'job_execute.task_instance');
insert ignore into t_leaf_alloc(biz_tag, max_id, step, description) values('job_execute.step_instance', 1, 2000, 'job_execute.step_instance');
insert ignore into t_leaf_alloc(biz_tag, max_id, step, description) values('job_execute.gse_task', 1, 2000, 'job_execute.gse_task');
insert ignore into t_leaf_alloc(biz_tag, max_id, step, description) values('job_execute.operation_log', 1, 2000, 'job_execute.operation_log');
insert ignore into t_leaf_alloc(biz_tag, max_id, step, description) values('job_execute.file_source_task_log', 1, 2000, 'job_execute.file_source_task_log');
insert ignore into t_leaf_alloc(biz_tag, max_id, step, description) values('job_execute.gse_file_execute_obj_task', 1, 2000, 'job_execute.gse_file_execute_obj_task');
insert ignore into t_leaf_alloc(biz_tag, max_id, step, description) values('job_execute.gse_script_execute_obj_task', 1, 2000, 'job_execute.gse_script_execute_obj_task');
insert ignore into t_leaf_alloc(biz_tag, max_id, step, description) values('job_execute.rolling_config', 1, 2000, 'job_execute.rolling_config');
insert ignore into t_leaf_alloc(biz_tag, max_id, step, description) values('job_execute.step_instance_rolling_task', 1, 2000, 'job_execute.step_instance_rolling_task');
insert ignore into t_leaf_alloc(biz_tag, max_id, step, description) values('job_execute.step_instance_variable', 1, 2000, 'job_execute.step_instance_variable');
insert ignore into t_leaf_alloc(biz_tag, max_id, step, description) values('job_execute.task_instance_variable', 1, 2000, 'job_execute.task_instance_variable');