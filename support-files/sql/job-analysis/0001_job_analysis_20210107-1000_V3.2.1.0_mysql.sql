SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS job_analysis DEFAULT CHARACTER SET utf8mb4;
USE job_analysis;

CREATE TABLE IF NOT EXISTS `analysis_task`  (
                                                `id`                          bigint(20)                                                    NOT NULL AUTO_INCREMENT,
                                                `code`                        varchar(255) CHARACTER SET utf8mb4 NOT NULL COMMENT '任务代码，用于匹配处理器',
                                                `app_ids`                     text CHARACTER SET utf8mb4         NOT NULL COMMENT '生效的appId，null为全部生效',
                                                `result_description_template` TEXT COMMENT '任务结果总体描述模板',
												`result_description_template_en` TEXT NULL COMMENT '英文版分析结果总体描述模板内容',
                                                `result_item_template`        text CHARACTER SET utf8mb4         NOT NULL COMMENT '每条任务结果描述模板',
												`result_item_template_en` 	  TEXT NULL COMMENT '英文版分析结果子项模板内容',
                                                `priority`                    int(10)                                                       NOT NULL COMMENT '优先级',
                                                `active`                      bit(1)                                                        NOT NULL COMMENT '是否启用',
                                                `period_seconds`              bigint(20)                                                    NOT NULL COMMENT '触发周期',
                                                `creator`                     varchar(128) CHARACTER SET utf8mb4         NOT NULL COMMENT '创建者',
                                                `last_modify_user`            varchar(128) CHARACTER SET utf8mb4         NULL DEFAULT NULL COMMENT '更新者',
                                                `create_time`                 bigint(20) UNSIGNED                                           NULL DEFAULT NULL COMMENT '创建时间',
                                                `last_modify_time`            bigint(20) UNSIGNED                                           NULL DEFAULT NULL COMMENT '更新时间',
                                                `description`                 TEXT COMMENT '对任务的描述',
                                                PRIMARY KEY (`id`) USING BTREE,
                                                UNIQUE INDEX `idx_code` (`code`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4;


CREATE TABLE IF NOT EXISTS `analysis_task_instance`
(
    `id`               bigint(20)                                                  NOT NULL AUTO_INCREMENT,
    `app_id`           bigint(20)                                                  NOT NULL COMMENT '业务id',
    `task_id`          bigint(255)                                                 NOT NULL COMMENT '任务id',
    `status`           int(10)                                                     NOT NULL COMMENT '任务状态',
    `result_data`      MEDIUMTEXT CHARACTER SET utf8mb4 NOT NULL COMMENT '任务结果数据，各任务自定义格式',
    `priority`         int(10)                                                     NOT NULL COMMENT '优先级',
    `active`           bit(1)                                                      NOT NULL COMMENT '是否启用',
    `creator`          varchar(128) CHARACTER SET utf8mb4       NOT NULL COMMENT '创建者',
    `last_modify_user` varchar(128) CHARACTER SET utf8mb4       NULL DEFAULT NULL COMMENT '更新者',
    `create_time`      bigint(20) UNSIGNED                                         NULL DEFAULT NULL COMMENT '创建时间',
    `last_modify_time` bigint(20) UNSIGNED                                         NULL DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_appId_taskId` (`app_id`, `task_id`) USING BTREE
) ENGINE = InnoDB
  CHARACTER SET = utf8mb4;
  
-- ------------------------
-- 增加静态分析结果数据表
-- ------------------------
CREATE TABLE IF NOT EXISTS `analysis_task_static_instance`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `app_id` bigint(20) NOT NULL COMMENT '业务id',
  `task_id` bigint(255) NOT NULL COMMENT '任务id',
  `status` int(10) NOT NULL COMMENT '任务状态',
  `result_data` mediumtext CHARACTER SET utf8mb4 NOT NULL COMMENT '任务结果数据，各任务自定义格式',
  `result_data_en` mediumtext CHARACTER SET utf8mb4 NULL COMMENT '任务结果数据（英文）',
  `priority` int(10) NOT NULL COMMENT '优先级',
  `active` bit(1) NOT NULL COMMENT '是否启用',
  `creator` varchar(128) CHARACTER SET utf8mb4  NOT NULL COMMENT '创建者',
  `last_modify_user` varchar(128) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '更新者',
  `create_time` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '创建时间',
  `last_modify_time` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_appId_taskId`(`app_id`, `task_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 ;


REPLACE INTO `analysis_task_static_instance`(`id`, `app_id`, `task_id`, `status`, `result_data`, `result_data_en`, `priority`, `active`, `creator`, `last_modify_user`, `create_time`, `last_modify_time`) VALUES (1, -1, -1, 2, '使用作业平台 Shell 脚本的内置函数 `job_success` `job_fail`，可以轻松实现简单的执行结果归类分组效果；更多使用技巧，详见文档 <a>https://bk.tencent.com/docs/</a>', 'Using built-in functions `job_success` `job_fail`, execution results can be grouped easily. Find out more tips with docs <a>https://bk.tencent.com/docs/</a>.', 100, b'1', 'admin', 'admin', 1583492717314, 1583492717318);
REPLACE INTO `analysis_task_static_instance`(`id`, `app_id`, `task_id`, `status`, `result_data`, `result_data_en`, `priority`, `active`, `creator`, `last_modify_user`, `create_time`, `last_modify_time`) VALUES (2, -1, -1, 2, '脚本传参涉及到敏感信息（如密码）该如何规避因明文传输而导致信息泄露的风险？密码 变量可以帮你解决这个问题！使用案例详见[文档](https://bk.tencent.com/docs/)', 'Params involves sensitive information (such as passwords). How to avoid the risk of information leakage? The `password` variable can help you solve this problem! For more information and examples see docs (https://bk.tencent.com/docs/).', 100, b'1', 'admin', 'admin', 1583492717314, 1583492717318);
REPLACE INTO `analysis_task_static_instance`(`id`, `app_id`, `task_id`, `status`, `result_data`, `result_data_en`, `priority`, `active`, `creator`, `last_modify_user`, `create_time`, `last_modify_time`) VALUES (3, -1, -1, 2, '为你的作业模板设置一个标签可以方便你更好的进行分类管理，在「作业管理」页面左侧可以快速通过分类标签找到你的作业模板。', 'Job tags can help you manage template classification better, you can easily toggle different tags in left side on Jobs page.', 100, b'1', 'admin', 'admin', 1583492717314, 1583492717318);
REPLACE INTO `analysis_task_static_instance`(`id`, `app_id`, `task_id`, `status`, `result_data`, `result_data_en`, `priority`, `active`, `creator`, `last_modify_user`, `create_time`, `last_modify_time`) VALUES (4, -1, -1, 2, '收藏的作业模板可以显示在首页中，这样你就能更快的找到你需要执行的作业了。\r\n常用的脚本语言语法或方法使用技巧的推荐', 'Favorite Jobs can be displayed on the homepage, so you can find the job you need faster when you came to Job.', 100, b'1', 'admin', 'admin', 1583492717314, 1583492717318);
REPLACE INTO `analysis_task_static_instance`(`id`, `app_id`, `task_id`, `status`, `result_data`, `result_data_en`, `priority`, `active`, `creator`, `last_modify_user`, `create_time`, `last_modify_time`) VALUES (5, -1, -1, 2, '你知道么，Shell 也支持数组变量喔~ 写法示例： `var[0]=\'test1\'; var[1]=\'test2\';` 通过 `echo ${var[*]}` 可以打印变量的所有索引值，`echo ${#var[*]}` 可以打印变量一共有多少个索引。', 'You know what, Array variable is supported in Bash. How? `var[0]=\'test1\'; var[1]=\'test2\';` and using `echo ${var[*]}` to print all index values of Array variable, using `echo ${#var[*]}` to show how many index the Array variable has.', 100, b'1', 'admin', 'admin', 1583492717314, 1583492717318);


REPLACE INTO `analysis_task`(`id`, `code`, `app_ids`, `result_description_template`, `result_description_template_en`, `result_item_template`, `result_item_template_en`, `priority`, `active`, `period_seconds`, `creator`, `last_modify_user`, `create_time`, `last_modify_time`, `description`) VALUES (-1, 'DefaultTipsProvider', '', NULL, NULL, '', NULL, 1, b'1', 3600, 'admin', 'admin', 1, 1, NULL);
REPLACE INTO `analysis_task`(`id`, `code`, `app_ids`, `result_description_template`, `result_description_template_en`, `result_item_template`, `result_item_template_en`, `priority`, `active`, `period_seconds`, `creator`, `last_modify_user`, `create_time`, `last_modify_time`, `description`) VALUES (2, 'TimerTaskFailWatcher', '', '系统发现多个运行中的定时任务在近期出现了执行失败的情况，请关注！', 'Attention! System has detected that many Crons have running failed recently, please click Details to check if there is problems in it.', '定时任务【${taskName}】在近期出现了执行失败的问题，请留意。', 'Caution: Cron[${taskName}] has executed failed recently.', 1, b'1', 1200, 'admin', 'admin', 1, 1, NULL);
REPLACE INTO `analysis_task`(`id`, `code`, `app_ids`, `result_description_template`, `result_description_template_en`, `result_item_template`, `result_item_template_en`, `priority`, `active`, `period_seconds`, `creator`, `last_modify_user`, `create_time`, `last_modify_time`, `description`) VALUES (3, 'TimerTaskFailRateWatcher', '', '系统发现多个运行中的定时任务周期执行成功率低于 60%，请关注！', 'Attention! System has detected that many Crons success rate is lower than 60%， it seems there are some problems, please pay attention.', '定时任务【${taskName}】周期执行成功率低于 60%，请留意。', 'Caution: Cron[${taskName}] success-rate is lower than 60%.', 1, b'1', 1200, 'admin', 'admin', 1, 1, NULL);
REPLACE INTO `analysis_task`(`id`, `code`, `app_ids`, `result_description_template`, `result_description_template_en`, `result_item_template`, `result_item_template_en`, `priority`, `active`, `period_seconds`, `creator`, `last_modify_user`, `create_time`, `last_modify_time`, `description`) VALUES (4, 'ForbiddenScriptFinder', '', '系统发现有多个作业模板/执行方案使用了【已禁用】状态的脚本版本，被禁用脚本版本将无法正常执行，请及时处理。', 'Caution! System has detected that many Jobs using the script version in \"Banned\" status, it will not able to execute， please handle it ASAP.', '${typeName}：${instanceName}的步骤【$stepName】使用了 已禁用 状态的脚本版本，该步骤将无法正常执行，请关注！', 'Caution: Job[${instanceName}]\'s step[$stepName] uses script version in \"Banned\" state, which will not be executed properly.', 1, b'1', 2400, 'admin', 'admin', 1, 1, NULL);
REPLACE INTO `analysis_task`(`id`, `code`, `app_ids`, `result_description_template`, `result_description_template_en`, `result_item_template`, `result_item_template_en`, `priority`, `active`, `period_seconds`, `creator`, `last_modify_user`, `create_time`, `last_modify_time`, `description`) VALUES (5, 'TaskPlanTargetChecker', '', '系统发现多个作业模板/执行方案的步骤中存在执行目标【Agent状态异常】的情况，请关注。', 'Caution! System has detected that many Jobs using Abnormal status Host, it will cause the job fail to execute, please handle it ASAP.', '作业：${planName}的步骤【$stepName】的执行目标存在异常：【$description】，请关注。', 'Caution: Job plan[${planName}]\'s step[$stepName] contains abnormal hosts.', 1, b'1', 7200, 'admin', 'admin', 1, 1, NULL);

CREATE TABLE IF NOT EXISTS `statistics`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `app_id` bigint(20) NOT NULL COMMENT '业务id',
  `resource` text CHARACTER SET utf8mb4 NOT NULL COMMENT '统计的资源',
  `dimension` text CHARACTER SET utf8mb4 NOT NULL COMMENT '统计维度',
  `dimension_value` text CHARACTER SET utf8mb4 NOT NULL COMMENT '统计维度取值',
  `date` text CHARACTER SET utf8mb4 NOT NULL COMMENT '统计时间',
  `value` text CHARACTER SET utf8mb4 NOT NULL COMMENT '统计值',
  `create_time` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '创建时间',
  `last_modify_time` bigint(20) UNSIGNED NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_appId_res_dim_dimValue`(`app_id`, `resource`(32), `dimension`(32), `dimension_value`(32)) USING BTREE,
  INDEX `idx_date`(`date`(22)) USING BTREE,
  INDEX `idx_lastModifyTime`(`last_modify_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 ;

