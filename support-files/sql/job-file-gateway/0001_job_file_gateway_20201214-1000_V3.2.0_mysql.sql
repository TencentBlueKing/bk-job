SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS job_file_gateway DEFAULT CHARACTER SET utf8mb4;
USE job_file_gateway;

-- ----------------------------
-- Table structure for file_source
-- ----------------------------
CREATE TABLE IF NOT EXISTS `file_source`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `app_id` bigint(20) NOT NULL COMMENT '业务Id',
  `code` varchar(255) CHARACTER SET utf8mb4 NOT NULL COMMENT '文件源标识',
  `alias` varchar(255) CHARACTER SET utf8mb4 NULL DEFAULT NULL COMMENT '文件源别名',
  `status` tinyint(255) NOT NULL DEFAULT 0 COMMENT '状态',
  `type` varchar(255) NOT NULL COMMENT '类型Code',
  `region_code` varchar(255) CHARACTER SET utf8mb4 NULL DEFAULT NULL COMMENT '地域code',
  `region_name` varchar(255) CHARACTER SET utf8mb4 NULL DEFAULT NULL COMMENT '地域名称',
  `endpoint_domain` varchar(255) CHARACTER SET utf8mb4 NULL DEFAULT NULL COMMENT 'EndPoint域名',
  `custom_info` text CHARACTER SET utf8mb4 NULL COMMENT '各不同文件源的差异性自定义信息Json串',
  `public` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否为公共存储',
  `share_to_all_app` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否共享至全业务',
  `credential_id` varchar(255) CHARACTER SET utf8mb4 NULL DEFAULT NULL COMMENT '凭据Id',
  `file_prefix` varchar(255) CHARACTER SET utf8mb4 NULL DEFAULT NULL COMMENT '文件前缀名',
  `worker_select_scope` varchar(255) CHARACTER SET utf8mb4 NOT NULL DEFAULT 'ALL' COMMENT '接入点选择范畴：APP/PUBLIC/ALL',
  `worker_select_mode` varchar(255) CHARACTER SET utf8mb4 NOT NULL DEFAULT 'AUTO' COMMENT '接入点选择模式：AUTO/MANUAL',
  `worker_id` bigint(20) NULL DEFAULT NULL COMMENT '接入的workerId',
  `enable` bit(1) NOT NULL DEFAULT b'1' COMMENT '是否启用',
  `creator` varchar(255) CHARACTER SET utf8mb4 NULL DEFAULT NULL COMMENT '创建人',
  `create_time` bigint(20) NULL DEFAULT NULL COMMENT '创建时间',
  `last_modify_user` varchar(255) CHARACTER SET utf8mb4 NULL DEFAULT NULL COMMENT '最后更新人',
  `last_modify_time` bigint(20) NULL DEFAULT NULL COMMENT '最后更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_app_id`(`app_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4;

-- ----------------------------
-- Table structure for file_source_batch_task
-- ----------------------------
CREATE TABLE IF NOT EXISTS `file_source_batch_task`  (
  `id` varchar(34) CHARACTER SET utf8mb4 NOT NULL,
  `app_id` bigint(20) NOT NULL COMMENT '业务Id',
  `step_instance_id` bigint(20) NULL DEFAULT NULL COMMENT '步骤实例Id',
  `execute_count` int(11) NOT NULL DEFAULT 0 COMMENT '执行次数',
  `status` tinyint(4) NOT NULL COMMENT '任务状态',
  `file_cleared` bit(1) NOT NULL DEFAULT b'0' COMMENT 'FileWorker上的文件是否被清理',
  `creator` varchar(255) CHARACTER SET utf8mb4 NULL DEFAULT NULL COMMENT '创建人',
  `create_time` bigint(20) NULL DEFAULT NULL COMMENT '创建时间',
  `last_modify_time` bigint(20) NULL DEFAULT NULL COMMENT '最后更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_app_id`(`app_id`) USING BTREE,
  INDEX `idx_create_time`(`create_time`) USING BTREE,
  INDEX `idx_step_instance_id_execute_count`(`step_instance_id`, `execute_count`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4;

-- ----------------------------
-- Table structure for file_source_share
-- ----------------------------
CREATE TABLE IF NOT EXISTS `file_source_share`  (
  `file_source_id` int(11) NOT NULL COMMENT '文件源id',
  `app_id` bigint(20) NOT NULL COMMENT '可以使用文件源的业务id',
  PRIMARY KEY (`file_source_id`, `app_id`) USING BTREE,
  INDEX `idx_app_id`(`app_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4;

-- ----------------------------
-- Table structure for file_source_task
-- ----------------------------
CREATE TABLE IF NOT EXISTS `file_source_task`  (
  `id` varchar(34) CHARACTER SET utf8mb4 NOT NULL,
  `batch_task_id` varchar(34) CHARACTER SET utf8mb4 NULL DEFAULT NULL COMMENT '所属的批量任务Id',
  `app_id` bigint(20) NOT NULL COMMENT '业务Id',
  `step_instance_id` bigint(20) NULL DEFAULT NULL COMMENT '步骤实例Id',
  `execute_count` int(11) NOT NULL DEFAULT 0 COMMENT '执行次数',
  `file_source_id` int(11) NOT NULL COMMENT '文件源Id',
  `file_worker_id` bigint(20) NOT NULL COMMENT 'FileWorkerId',
  `status` tinyint(4) NOT NULL COMMENT '任务状态',
  `file_cleared` bit(1) NOT NULL DEFAULT b'0' COMMENT 'FileWorker上的文件是否被清理',
  `creator` varchar(255) CHARACTER SET utf8mb4 NULL DEFAULT NULL COMMENT '创建人',
  `create_time` bigint(20) NULL DEFAULT NULL COMMENT '创建时间',
  `last_modify_time` bigint(20) NULL DEFAULT NULL COMMENT '最后更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_app_id`(`app_id`) USING BTREE,
  INDEX `idx_file_worker_id`(`file_worker_id`) USING BTREE,
  INDEX `idx_create_time`(`create_time`) USING BTREE,
  INDEX `idx_step_instance_id`(`step_instance_id`) USING BTREE,
  INDEX `idx_batch_task_id`(`batch_task_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4;

-- ----------------------------
-- Table structure for file_source_type
-- ----------------------------
CREATE TABLE IF NOT EXISTS `file_source_type`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `storage_type` varchar(255) CHARACTER SET utf8mb4 NOT NULL COMMENT '存储类型：文件系统（FILE_SYSTEM）/对象存储（OSS）',
  `code` varchar(255) CHARACTER SET utf8mb4 NOT NULL COMMENT '文件源类型Code',
  `name` varchar(255) CHARACTER SET utf8mb4 NOT NULL COMMENT '文件源类型名称',
  `icon` text CHARACTER SET utf8mb4 NULL COMMENT 'base64编码的图片信息',
  `last_modify_user` varchar(255) CHARACTER SET utf8mb4 NULL DEFAULT NULL COMMENT '最后更新人',
  `last_modify_time` bigint(20) NULL DEFAULT NULL COMMENT '最后更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4;

-- ----------------------------
-- 初始数据
-- ----------------------------

REPLACE INTO `job_file_gateway`.`file_source_type`(`id`, `storage_type`, `code`, `name`, `icon`, `last_modify_user`, `last_modify_time`) VALUES (1, 'OSS', 'TENCENT_CLOUD_COS', '腾讯云COS', NULL, 'admin', 1);

-- ----------------------------
-- Table structure for file_task
-- ----------------------------
CREATE TABLE IF NOT EXISTS `file_task`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `file_source_task_id` varchar(34) CHARACTER SET utf8mb4 NOT NULL COMMENT '文件源任务Id',
  `file_path` text CHARACTER SET utf8mb4 NULL COMMENT '文件路径',
  `download_path` text CHARACTER SET utf8mb4 NULL COMMENT '文件下载路径',
  `file_size` bigint(20) NULL DEFAULT NULL COMMENT '文件大小，单位：字节',
  `status` tinyint(4) NULL DEFAULT NULL COMMENT '文件下载状态',
  `progress` int(11) NULL DEFAULT NULL COMMENT '文件下载进度',
  `error_msg` text CHARACTER SET utf8mb4 NULL COMMENT '错误信息',
  `create_time` bigint(20) NULL DEFAULT NULL COMMENT '记录创建时间',
  `last_modify_time` bigint(20) NULL DEFAULT NULL COMMENT '最近更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_file_source_task_id`(`file_source_task_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4;

-- ----------------------------
-- Table structure for file_worker
-- ----------------------------
CREATE TABLE IF NOT EXISTS `file_worker`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `app_id` bigint(20) NOT NULL DEFAULT -1 COMMENT '业务ID',
  `name` varchar(255) CHARACTER SET utf8mb4 NOT NULL DEFAULT 'ANON' COMMENT '名称',
  `description` text CHARACTER SET utf8mb4 NULL COMMENT '描述',
  `token` varchar(34) CHARACTER SET utf8mb4 NULL DEFAULT NULL COMMENT '预设的凭证',
  `access_host` varchar(255) CHARACTER SET utf8mb4 NOT NULL COMMENT 'gateway主动访问worker使用的host',
  `access_port` int(11) NOT NULL COMMENT 'gateway主动访问worker使用的port',
  `cloud_area_id` bigint(20) NULL DEFAULT NULL COMMENT '云区域Id',
  `inner_ip` varchar(255) CHARACTER SET utf8mb4 NULL DEFAULT NULL COMMENT '所在机器的内网IP',
  `cpu_overload` float(255, 2) NULL DEFAULT NULL COMMENT 'CPU负载',
  `mem_rate` float(255, 2) NULL DEFAULT NULL COMMENT '内存占用率',
  `mem_free_space` float(255, 2) NULL DEFAULT NULL COMMENT '内存剩余空间',
  `disk_rate` float(255, 2) NULL DEFAULT NULL COMMENT '磁盘占用率',
  `disk_free_space` float(255, 2) NULL DEFAULT NULL COMMENT '磁盘剩余空间',
  `version` varchar(255) CHARACTER SET utf8mb4 NULL DEFAULT NULL COMMENT 'worker版本',
  `online_status` tinyint(255) NOT NULL COMMENT '是否在线，1为在线，0为离线',
  `last_heart_beat` bigint(20) NOT NULL COMMENT '上一次心跳时间',
  `creator` varchar(255) CHARACTER SET utf8mb4 NULL DEFAULT NULL COMMENT '创建者',
  `create_time` bigint(20) NOT NULL COMMENT '创建时间，即第一次心跳时间',
  `last_modify_user` varchar(255) CHARACTER SET utf8mb4 NULL DEFAULT NULL COMMENT '最近修改人',
  `last_modify_time` bigint(20) NULL DEFAULT NULL COMMENT '最近修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_app_id`(`app_id`) USING BTREE,
  INDEX `idx_token`(`token`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4;

-- ----------------------------
-- Table structure for file_worker_ability
-- ----------------------------
CREATE TABLE IF NOT EXISTS `file_worker_ability`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `worker_id` bigint(20) NOT NULL,
  `tag` varchar(255) CHARACTER SET utf8mb4 NOT NULL COMMENT '能力标签',
  `description` varchar(255) CHARACTER SET utf8mb4 NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `unique_worker_tag`(`worker_id`, `tag`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4;


