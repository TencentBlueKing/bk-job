USE `job_manage`;

UPDATE `task_template_step_file`
SET destination_file_location = CONCAT(destination_file_location, '/[FILESRCIP]/')
WHERE duplicate_handler = 2;

UPDATE `task_template_step_file`
SET destination_file_location = CONCAT(destination_file_location, '/[YYYY-MM-DD]/[FILESRCIP]/')
WHERE duplicate_handler = 3;

UPDATE `task_template_step_file`
SET duplicate_handler = 1
WHERE duplicate_handler = 2 OR duplicate_handler = 3;

UPDATE `task_plan_step_file`
SET destination_file_location = CONCAT(destination_file_location, '/[FILESRCIP]/')
WHERE duplicate_handler = 2;

UPDATE `task_plan_step_file`
SET destination_file_location = CONCAT(destination_file_location, '/[YYYY-MM-DD]/[FILESRCIP]/')
WHERE duplicate_handler = 3;

UPDATE `task_plan_step_file`
SET duplicate_handler = 1
WHERE duplicate_handler = 2 OR duplicate_handler = 3;
