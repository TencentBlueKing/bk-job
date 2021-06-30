USE `job_manage`;
UPDATE `job_manage`.`task_plan` SET is_deleted = 1 WHERE template_id IN (SELECT id FROM task_template WHERE is_deleted = 1);