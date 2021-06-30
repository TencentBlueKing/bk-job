use job_manage;

SET NAMES utf8mb4;

-- ----------------------------
-- analysis相关功能已迁移至job-analysis模块，job-manage中清理掉相关表
-- ----------------------------
DROP TABLE IF EXISTS analysis_task;
DROP TABLE IF EXISTS analysis_task_instance;
DROP TABLE IF EXISTS analysis_task_static_instance;
