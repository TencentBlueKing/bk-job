USE `job_manage`;

ALTER TABLE `job_manage`.`task_plan_step_script` MODIFY COLUMN `script_param` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL AFTER `language`;

ALTER TABLE `job_manage`.`task_template_step_script` MODIFY COLUMN `script_param` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL AFTER `language`;
