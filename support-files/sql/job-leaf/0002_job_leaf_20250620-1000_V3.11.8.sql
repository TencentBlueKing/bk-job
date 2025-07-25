SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS job_leaf DEFAULT CHARACTER SET utf8mb4;
USE job_leaf;

insert ignore into t_leaf_alloc(biz_tag, max_id, step, description) values('job_execute.step_instance_file_batch', 1, 2000, 'job_execute.step_instance_file_batch');
