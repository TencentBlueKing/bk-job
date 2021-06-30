USE `job_manage`;

ALTER TABLE `job_manage`.`task_template_step_file_list` ADD COLUMN file_source_id INT(11);
ALTER TABLE `job_manage`.`task_plan_step_file_list` ADD COLUMN file_source_id INT(11);
