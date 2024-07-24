SET NAMES utf8mb4;
USE job_analysis;

-- ------------------------
-- 创建AI提示符模板数据表
-- ------------------------
CREATE TABLE IF NOT EXISTS `ai_prompt_template`  (
    `id`                          int(10)                           NOT NULL    AUTO_INCREMENT,
    `code`                        varchar(255)                      NOT NULL                    COMMENT '模板代码，用于唯一标识模板',
    `locale`                      varchar(16)                       NOT NULL    DEFAULT 'zh_CN' COMMENT '语言',
    `name`                        varchar(255)                      NOT NULL                    COMMENT '模板名称',
    `raw_prompt`                  varchar(255)                          NULL    DEFAULT NULL    COMMENT '不含上下文的简单提示符',
    `template`                    TEXT                              NOT NULL                    COMMENT '模板内容',
    `description`                 TEXT                                                          COMMENT '对模板的描述',
    `creator`                     varchar(128)                      NOT NULL                    COMMENT '创建者',
    `last_modify_user`            varchar(128)                          NULL    DEFAULT NULL    COMMENT '更新者',
    `create_time`                 bigint(20) UNSIGNED                   NULL    DEFAULT NULL    COMMENT '创建时间',
    `last_modify_time`            bigint(20) UNSIGNED                   NULL    DEFAULT NULL    COMMENT '更新时间',
    `row_create_time`             datetime                          NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`             datetime                          NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `idx_code_locale` (`code`,`locale`) USING BTREE
) ENGINE = InnoDB
CHARACTER SET = utf8mb4;

-- ------------------------
-- 创建AI对话记录表
-- ------------------------
CREATE TABLE IF NOT EXISTS `ai_chat_history`  (
    `id`                          bigint(20)                        NOT NULL    AUTO_INCREMENT,
    `username`                    varchar(64)                       NOT NULL                    COMMENT '用户名',
    `user_input`                  TEXT                              NOT NULL                    COMMENT '用户输入内容',
    `prompt_template_id`          int(10)                               NULL    DEFAULT NULL    COMMENT '使用的提示符模板ID',
    `ai_input`                    TEXT                              NOT NULL                    COMMENT '提交给AI的输入内容',
    `ai_answer`                   TEXT                              NOT NULL                    COMMENT 'AI回答的内容',
    `error_code`                  varchar(128)                          NULL    DEFAULT NULL    COMMENT 'AI回答失败时的错误码',
    `error_message`               varchar(512)                          NULL    DEFAULT NULL    COMMENT 'AI回答失败时的错误信息',
    `start_time`                  bigint(20) UNSIGNED                   NULL    DEFAULT NULL    COMMENT '开始时间',
    `answer_time`                 bigint(20) UNSIGNED                   NULL    DEFAULT NULL    COMMENT 'AI回答完成时间',
    `total_time`                  bigint(20) UNSIGNED                   NULL    DEFAULT NULL    COMMENT '总耗时',
    `is_deleted`                  TINYINT(1) UNSIGNED               NOT NULL    DEFAULT '0'     COMMENT '是否已删除：0表示未删除，1表示已删除',
    `row_create_time`             datetime                          NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`             datetime                          NOT NULL    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_username_startTime` (`username`,`start_time`) USING BTREE,
    INDEX `idx_start_time` (`start_time`) USING BTREE
) ENGINE = InnoDB
CHARACTER SET = utf8mb4;

-- ------------------------
-- 插入初始化数据
-- ------------------------
REPLACE INTO job_analysis.ai_prompt_template
(id, code, locale, name, raw_prompt, template, description, creator, last_modify_user, create_time, last_modify_time, row_create_time, row_update_time)
VALUES(1, 'CHECK_SCRIPT', 'zh_CN', '检查脚本是否有语法问题', '帮我检查一下我写的脚本有没有语法问题。', '已知脚本内容如下：\\n``````{script_type}\\n{script_content}\\n``````\\n帮我检查一下我写的脚本有没有语法问题。\\n\\n回答问题时请遵守以下要求：\\n1. 指出问题时请给出对应的脚本行号，请注意首行shebang与空白的行也需要计算，没有问题的行不需要体现在回答中。', '检查脚本是否有语法问题', 'admin', 'admin', NULL, NULL, '1970-01-01 00:00:00', '1970-01-01 00:00:00');
REPLACE INTO job_analysis.ai_prompt_template
(id, code, locale, name, raw_prompt, template, description, creator, last_modify_user, create_time, last_modify_time, row_create_time, row_update_time)
VALUES(2, 'ANALYZE_SCRIPT_EXECUTE_TASK_ERROR', 'zh_CN', '分析脚本执行任务报错信息', '任务内容报错解析', '请首先理解以下作业平台领域知识：\\n作业平台是一个支持脚本批量执行与文件批量传输的Web平台，底层依赖BK-GSE系统完成脚本执行与文件分发。\\n作业平台的依赖系统介绍：\\nBK-GSE：由GSE Agent与GSE Server构成，其中GSE Agent分布在多台用户（属于业务团队）机器上，每台机器上一个，GSE Server部署在蓝鲸团队自己的机器上，GSE Server对所有的GSE Agent进行管理与控制，可以向Agent下发脚本任务、获取脚本执行日志与结果信息等，还可以调度多个Agent利用组建的BT网络在多个Agent之间传输文件，BK-GSE通过【BK助手】这个企业微信服务号来响应用户咨询。\\n\\n以下介绍作业平台脚本执行功能的原理：\\n【功能一：脚本批量执行】\\n用户在Web页面填写任务名称、脚本来源、脚本内容、脚本参数、超时时长、执行账号、目标服务器等信息，作业平台将这些信息组装为原子任务信息后，调用BK-GSE提供的接口将任务信息提交给BK-GSE系统，并得到生成的任务ID，随后作业平台每间隔一定时间调用BK-GSE的任务状态查询接口查询任务状态、拉取任务产生的日志并存储到自身系统中的MongoDB中，直到任务完成或者超时。用户可以在任务执行结果页面查看任务执行结果信息、报错信息与日志。\\n\\n你是作业平台的AI助手，需要结合报错信息分析用户的提问并给出回答，回答中可以使用作业平台领域知识中的概念，如果需要BK-GSE进一步排查，请在回答的末尾添加该语句：如有其他问题，可以点击联系[BK助手]({bk_helper_link})进行人工咨询。\\n\\n当前任务执行的脚本内容为：\\n``````{script_type}\\n{script_content}\\n``````\\n脚本参数为：\\n{script_params}\\n\\n报错信息如下：\\n{error_content}\\n\\n请回答用户的提问：\\n任务报错内容解析', '系统内置的AI命令提示模板：分析脚本执行任务报错信息', 'admin', 'admin', NULL, NULL, '1970-01-01 00:00:00', '1970-01-01 00:00:00');
