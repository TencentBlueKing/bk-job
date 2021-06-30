ALTER TABLE `job_manage`.`task_template_step_file_list`
MODIFY COLUMN `file_location` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL AFTER `file_type`;
ALTER TABLE `job_manage`.`task_plan_step_file_list`
MODIFY COLUMN `file_location` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL AFTER `file_type`;