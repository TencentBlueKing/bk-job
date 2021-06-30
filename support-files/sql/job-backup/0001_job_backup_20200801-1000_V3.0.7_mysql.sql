SET NAMES utf8mb4;

CREATE DATABASE job_backup DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_general_ci;
USE job_backup;

-- ----------------------------
-- Table structure for export_job
-- ----------------------------
CREATE TABLE `export_job`
(
    `id`                 varchar(36)         NOT NULL,
    `app_id`             bigint(20) UNSIGNED NOT NULL,
    `creator`            varchar(255)        NOT NULL,
    `create_time`        bigint(20) UNSIGNED NOT NULL DEFAULT 0,
    `update_time`        bigint(20) UNSIGNED NOT NULL DEFAULT 0,
    `status`             tinyint(2) UNSIGNED NULL     DEFAULT 0,
    `password`           varchar(255)        NULL     DEFAULT NULL,
    `package_name`       varchar(255)        NOT NULL,
    `secret_handler`     tinyint(2) UNSIGNED NOT NULL,
    `expire_time`        bigint(20) UNSIGNED NOT NULL,
    `template_plan_info` longtext            NOT NULL,
    `file_name`          varchar(255)        NULL     DEFAULT NULL,
    PRIMARY KEY (`id`, `app_id`) USING BTREE,
    INDEX `idx_creator` (`creator`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci;

-- ----------------------------
-- Table structure for export_log
-- ----------------------------
CREATE TABLE `export_log`
(
    `id`          bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `app_id`      bigint(20) UNSIGNED NOT NULL,
    `job_id`      varchar(36)         NOT NULL,
    `type`        tinyint(2) UNSIGNED NOT NULL,
    `timestamp`   bigint(20) UNSIGNED NOT NULL,
    `content`     longtext            NOT NULL,
    `template_id` bigint(20) UNSIGNED NULL DEFAULT NULL,
    `plan_id`     bigint(20) UNSIGNED NULL DEFAULT NULL,
    `link_text`   varchar(255)        NULL DEFAULT NULL,
    `link_url`    varchar(255)        NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci;

-- ----------------------------
-- Table structure for import_job
-- ----------------------------
CREATE TABLE `import_job`
(
    `id`                   varchar(36)         NOT NULL,
    `app_id`               bigint(20) UNSIGNED NOT NULL,
    `creator`              varchar(255)        NOT NULL,
    `create_time`          bigint(20) UNSIGNED NOT NULL,
    `update_time`          bigint(20) UNSIGNED NOT NULL,
    `status`               tinyint(2) UNSIGNED NOT NULL DEFAULT 0,
    `export_id`            varchar(36)         NOT NULL,
    `file_name`            varchar(255)        NOT NULL,
    `template_plan_info`   longtext            NOT NULL,
    `duplicate_suffix`     varchar(50)         NOT NULL,
    `duplicate_id_handler` tinyint(2) UNSIGNED NOT NULL,
    `id_name_info`         longtext            NULL,
    PRIMARY KEY (`id`, `app_id`) USING BTREE,
    INDEX `idx_creator` (`creator`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci;

-- ----------------------------
-- Table structure for import_log
-- ----------------------------
CREATE TABLE `import_log`
(
    `id`          bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
    `app_id`      bigint(20) UNSIGNED NOT NULL,
    `job_id`      varchar(36)         NOT NULL,
    `type`        tinyint(2) UNSIGNED NOT NULL,
    `timestamp`   bigint(20) UNSIGNED NOT NULL,
    `content`     longtext            NOT NULL,
    `template_id` bigint(20) UNSIGNED NULL DEFAULT NULL,
    `plan_id`     bigint(20) UNSIGNED NULL DEFAULT NULL,
    `link_text`   varchar(255)        NULL DEFAULT NULL,
    `link_url`    varchar(255)        NULL DEFAULT NULL,
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_general_ci;
