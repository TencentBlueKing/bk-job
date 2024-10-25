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
    `status`                      tinyint(4)                            NULL    DEFAULT NULL    COMMENT 'AI对话状态：1表示正在回答，2表示已完成',
    `ai_answer`                   TEXT                                  NULL    DEFAULT NULL    COMMENT 'AI回答的内容',
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
-- 创建AI分析任务报错上下文信息表
-- ------------------------
CREATE TABLE IF NOT EXISTS `ai_analyze_error_context`  (
    `ai_chat_history_id`          bigint(20)      NOT NULL,
    `task_instance_id`            bigint(20)      NOT NULL                COMMENT '任务ID',
    `step_instance_id`            bigint(20)      NOT NULL                COMMENT '步骤ID',
    `execute_count`               int(11)         NOT NULL                COMMENT '执行次数',
    `batch`                       smallint(6)     NOT NULL DEFAULT '0'    COMMENT '滚动批次',
    `execute_object_type`         tinyint(4)          NULL DEFAULT NULL   COMMENT '执行对象类型：1-主机，2-容器',
    `execute_object_resource_id`  bigint(20)          NULL DEFAULT NULL   COMMENT '执行对象资源ID',
    `mode`                        tinyint(4)          NULL DEFAULT NULL   COMMENT '文件任务上传下载标识：0-上传，1-下载',
    `row_create_time`             datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `row_update_time`             datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`ai_chat_history_id`) USING BTREE
) ENGINE = InnoDB
CHARACTER SET = utf8mb4;

-- ------------------------
-- 插入初始化数据
-- ------------------------
REPLACE INTO job_analysis.ai_prompt_template
(id, code, locale, name, raw_prompt, template, description, creator, last_modify_user, create_time, last_modify_time, row_create_time, row_update_time)
VALUES(1, 'CHECK_SCRIPT', 'zh_CN', '检查脚本是否存在语法问题', '请帮忙检查一下脚本是否有语法或逻辑问题。', '已知脚本内容如下：\n```{BK_JOB_AI_TEMPLATE_VAR{script_type}}\n{BK_JOB_AI_TEMPLATE_VAR{script_content}}\n```\n请帮忙检查一下脚本是否有语法或逻辑问题！注意，当脚本内容中出现以下这些内容时，针对这部分无需检查和给建议，因为这一段是系统内置的初始化脚本逻辑，但不影响在给用户输出的建议的脚本内容中带上；最后，你还要问一下我：“需不需要我基于修改建议重新发一版脚本内容给你？”\n```\n{BK_JOB_AI_TEMPLATE_VAR{script_template}}\n```\n', '系统内置的AI命令提示模板：检查脚本是否存在语法问题', 'admin', 'admin', NULL, NULL, '1970-01-01 00:00:00', '1970-01-01 00:00:00');
REPLACE INTO job_analysis.ai_prompt_template
(id, code, locale, name, raw_prompt, template, description, creator, last_modify_user, create_time, last_modify_time, row_create_time, row_update_time)
VALUES(2, 'ANALYZE_SCRIPT_EXECUTE_TASK_ERROR', 'zh_CN', '分析脚本执行任务报错信息', '帮我分析一下这个任务[{BK_JOB_AI_TEMPLATE_VAR{step_instance_name}}]报错的主要原因，并提供一下处理建议', '请首先理解以下作业平台领域知识：\n作业平台是一个支持脚本批量执行与文件批量传输的Web平台，底层依赖BK-GSE系统完成脚本执行与文件分发。\n作业平台的依赖系统介绍：\nBK-GSE：由GSE Agent与GSE Server构成，其中GSE Agent分布在被管控的对象机器上（也就是用户的执行对象），GSE Server属于平台自己的服务节点，由平台自己部署和维护；GSE Server对所有的GSE Agent进行管理与控制，可以向Agent下发脚本任务、获取脚本执行日志与结果信息等，还可以调度多个Agent利用组建的BT网络在多个Agent之间传输文件，当出现BK-GSE相关的问题时，通过【BK助手】这个企业微信服务号来响应用户的咨询。\n\n以下介绍作业平台脚本执行功能的原理：\n【功能一：脚本批量执行】\n用户在Web页面填写任务名称、脚本来源、脚本内容、脚本参数、超时时长、执行账号、目标服务器等信息，作业平台将这些信息组装为原子任务信息后，调用BK-GSE提供的接口将任务信息提交给BK-GSE系统，并得到生成的任务ID，随后作业平台每间隔一定时间调用BK-GSE的任务状态查询接口查询任务状态、拉取任务产生的日志并存储到自身系统的DB中，直到任务完成或者超时。用户可以在任务执行结果页面查看任务执行结果信息、报错信息与日志。\n\n你是作业平台的AI助手，需要结合报错信息分析用户的提问并给出回答，回答中可以使用作业平台领域知识中的概念，如果需要BK-GSE进一步排查，请在回答的末尾添加该语句：如有其他问题，可以点击联系[BK助手]({BK_JOB_AI_TEMPLATE_VAR{bk_helper_link}})进行人工咨询。\n\n当前任务执行的脚本内容为：\n```{BK_JOB_AI_TEMPLATE_VAR{script_type}}\n{BK_JOB_AI_TEMPLATE_VAR{script_content}}\n```\n脚本参数为：\n{BK_JOB_AI_TEMPLATE_VAR{script_params}}\n\n报错信息如下：\n{BK_JOB_AI_TEMPLATE_VAR{error_content}}\n\n请回答用户的提问：\n请你结合任务执行的脚本内容，对执行结果的报错信息进行分析；要说明报错的主要含义，如果和脚本内容有关系，则明确指出是哪一段落的问题，并给出修改建议！', '系统内置的AI命令提示模板：分析脚本执行任务报错信息', 'admin', 'admin', NULL, NULL, '1970-01-01 00:00:00', '1970-01-01 00:00:00');
REPLACE INTO job_analysis.ai_prompt_template
(id, code, locale, name, raw_prompt, template, description, creator, last_modify_user, create_time, last_modify_time, row_create_time, row_update_time)
VALUES(3, 'ANALYZE_FILE_TRANSFER_TASK_ERROR', 'zh_CN', '分析文件分发任务报错信息', '帮我分析一下这个任务[{BK_JOB_AI_TEMPLATE_VAR{step_instance_name}}]报错的主要原因，并提供一下处理建议', '请首先理解以下作业平台领域知识：\n作业平台是一个支持脚本批量执行与文件批量传输的Web平台，底层依赖BK-GSE系统完成脚本执行与文件分发。\n作业平台的依赖系统介绍：\nBK-GSE：由GSE Agent与GSE Server构成，其中GSE Agent分布在被管控的对象机器上（也就是用户的执行对象），GSE Server属于平台自己的服务节点，由平台自己部署和维护；GSE Server对所有的GSE Agent进行管理与控制，可以向Agent下发脚本任务、获取脚本执行日志与结果信息等，还可以调度多个Agent利用组建的BT网络在多个Agent之间传输文件，当出现BK-GSE相关的问题时，通过【BK助手】这个企业微信服务号来响应用户的咨询。\n\n以下介绍作业平台文件分发功能的原理：\n【功能：文件批量传输】\n用户在Web页面填写任务名称、超时时长、上传限速、下载限速、源文件、目标路径、传输模式、执行账号、目标服务器等信息，作业平台将这些信息组装为原子任务信息后，调用BK-GSE提供的接口将任务信息提交给BK-GSE系统，并得到生成的任务ID，随后作业平台每间隔一定时间调用BK-GSE的任务状态查询接口查询任务状态、拉取任务状态与传输进度日志并存储到自身系统的DB中，直到任务完成或者超时。用户可以在任务执行结果页面查看任务执行结果信息、报错信息与进度日志。如果用户的文件分发任务报错的原因是目标目录不存在的话，除了正常的处理建议以外，还可以提示用户可以把任务的传输模式改为“强制模式”，“强制模式”的用途是如果目标服务器上不存在用户指定的路径，平台将自动创建。\n\n你是作业平台的AI助手，需要结合报错信息分析用户的提问并给出回答，回答中可以使用作业平台领域知识中的概念，如果需要BK-GSE进一步排查，请在回答的末尾添加该语句：如有其他问题，可以点击联系[BK助手]({BK_JOB_AI_TEMPLATE_VAR{bk_helper_link}})进行人工咨询。\n\n当前已经结合任务上下文信息进行了预分析，得到以下结果：\n（1）当前任务失败的主要原因为：\n{BK_JOB_AI_TEMPLATE_VAR{file_task_error_source}}\n\n（2）源文件上传失败的机器与报错信息（JSON格式）为：\n{BK_JOB_AI_TEMPLATE_VAR{upload_file_error_data}}\n\n（3）目标机器下载失败的机器与报错信息（JSON格式）为：\n{BK_JOB_AI_TEMPLATE_VAR{download_file_error_data}}\n\n\n请回答用户的提问：\n请你结合预分析结果数据，对任务报错内容进一步分析，并给出总结性的原因与处理建议。', '系统内置的AI命令提示模板：分析文件分发任务报错信息', 'admin', 'admin', NULL, NULL, '1970-01-01 00:00:00', '1970-01-01 00:00:00');
REPLACE INTO job_analysis.ai_prompt_template
(id, code, locale, name, raw_prompt, template, description, creator, last_modify_user, create_time, last_modify_time, row_create_time, row_update_time)
VALUES(4, 'CHECK_SCRIPT', 'en', 'Check the syntax problems of the script', 'Please check if there are any syntax or logic issues in the script', 'Given the following script content:\n```{BK_JOB_AI_TEMPLATE_VAR{script_type}}\n{BK_JOB_AI_TEMPLATE_VAR{script_content}}\n```\nPlease check if there are any syntax or logic issues in the script. Note that when the following content appears in the script, there is no need to check or provide suggestions for this part because it is the built-in initialization script logic of the system. However, it does not affect including this part in the script content that is output to the user as a suggestion. In the end of the conversation, I also need you to ask me: "Do you need me to resend a new version of the script content based on the modification suggestions?"\n```\n{BK_JOB_AI_TEMPLATE_VAR{script_template}}\n```\n', 'System AI template: Check the syntax problems of the script', 'admin', 'admin', NULL, NULL, '1970-01-01 00:00:00', '1970-01-01 00:00:00');
REPLACE INTO job_analysis.ai_prompt_template
(id, code, locale, name, raw_prompt, template, description, creator, last_modify_user, create_time, last_modify_time, row_create_time, row_update_time)
VALUES(5, 'ANALYZE_SCRIPT_EXECUTE_TASK_ERROR', 'en', 'Analyze error information for script execution tasks', 'Help me analyze the main cause of this task[{BK_JOB_AI_TEMPLATE_VAR{step_instance_name}}] error and provide some processing suggestions.', 'First, please understand the following knowledge related to the JOB platform:\nJOB is a web platform that supports batch script execution and file batch transfer, and relies on the BK-GSE system at the underlying infrastructure.\nIntroduction to the dependent system of the JOB platform:\nBK-GSE: composed of GSE Agent and GSE Server, where GSE Agent is distributed on the controlled object machine (i.e., the user''s execution object), and GSE Server belongs to the platform''s own service node, which is deployed and maintained by the platform itself. GSE Server manages and controls all GSE Agents, can issue script tasks to Agents, obtain script execution logs and result information, etc., and can also schedule multiple Agents to use the BT network to transfer files between multiple Agents. When there are BK-GSE related issues, the WeCom service account [BK-Assistant]({BK_JOB_AI_TEMPLATE_VAR{bk_helper_link}}) is used to respond to user inquiries.\n\nThe following introduces the principle of the script execution of the JOB platform:\nBatch Script Execution: Users fill in task name, script source, script content, script parameters, timeout duration, execution account, target server and other information on the web page. JOB assembles this information and calls the interface provided by BK-GSE to submit the task to the BK-GSE system, and obtains the generated task ID. Afterwards, the JOB calls the API of BK-GSE at intervals to query the task status, pull the generated logs and store them in its own system DB, until the task is completed or timed out. Users can view task execution result, error information and logs on the task execution result page.\nAs an AI assistant of the JOB platform, you need to be familiar with the functions of the JOB platform and based on the actual error information, provide answers to user''s question. If further troubleshooting by BK-GSE is needed, please add the following statement at the end of the answer: If you have any questions for GSE, click [BK-Assistant]({BK_JOB_AI_TEMPLATE_VAR{bk_helper_link}}) for manual consultation.\n\nThe current task execution script content is:\n```{BK_JOB_AI_TEMPLATE_VAR{script_type}}\n{BK_JOB_AI_TEMPLATE_VAR{script_content}}\n```\nThe script parameters are:\n{BK_JOB_AI_TEMPLATE_VAR{script_params}}\n\nThe error information is as follows:\n{BK_JOB_AI_TEMPLATE_VAR{error_content}}\n\nPlease answer the user''s question:\nPlease analyze the error information of the execution result based on the task execution script content, and explain the main meaning of the error. If it is related to the script content, please specify which paragraph has the problem and provide modification suggestions!', 'System AI template: Analyze error information for script execution tasks', 'admin', 'admin', NULL, NULL, '1970-01-01 00:00:00', '1970-01-01 00:00:00');
REPLACE INTO job_analysis.ai_prompt_template
(id, code, locale, name, raw_prompt, template, description, creator, last_modify_user, create_time, last_modify_time, row_create_time, row_update_time)
VALUES(6, 'ANALYZE_FILE_TRANSFER_TASK_ERROR', 'en', 'Analyze error information for file distribution tasks', 'Help me analyze the main cause of this task[{BK_JOB_AI_TEMPLATE_VAR{step_instance_name}}] error and provide some processing suggestions.', 'First, please understand the following knowledge related to the JOB platform:\nJOB is a web platform that supports batch script execution and file batch transfer, and relies on the BK-GSE system at the underlying infrastructure.\nIntroduction to the dependent system of the JOB platform:\nBK-GSE: composed of GSE Agent and GSE Server, where GSE Agent is distributed on the controlled object machine (i.e., the user''s execution object), and GSE Server belongs to the platform''s own service node, which is deployed and maintained by the platform itself. GSE Server manages and controls all GSE Agents, can issue script tasks to Agents, obtain script execution logs and result information, etc., and can also schedule multiple Agents to use the BT network to transfer files between multiple Agents. When there are BK-GSE related issues, the WeCom service account [BK-Assistant]({BK_JOB_AI_TEMPLATE_VAR{bk_helper_link}}) is used to respond to user inquiries.\n\nThe following introduces is the principle of the file transfer of JOB platform:\nBatch File Transfer:  Users fill in various parameters on the web interface, such as task name, timeout duration, upload speed limit, download speed limit, source file, target path, transfer mode, execution account, target server, etc. The JOB platform assembles this information and submits it to the BK-GSE system via its API. The BK-GSE system generates a task ID and starts executing the task. The JOB platform periodically queries the task status and pulls the logs and progress information from the BK-GSE system, storing them in its own system DB. Users can view the task execution result, error information, and logs on the task execution result page.If the reason for a user`s file distribution task error is that the target directory does not exist, in addition to the normal processing suggestions, you can also suggest the user to change the task transfer mode to "Force". The purpose of "Force" mode is that if the path specified by the user does not exist on the target server, the platform will automatically create it.\n\nAs an AI assistant of the JOB platform, you need to be familiar with the functions of the JOB platform and based on the actual error information, provide answers to user''s question. If further troubleshooting by BK-GSE is needed, please add the following statement at the end of the answer: If you have any questions for GSE, click [BK-Assistant]({BK_JOB_AI_TEMPLATE_VAR{bk_helper_link}}) for manual consultation.\n\nBased on the pre-analysis of the task context information, the following results have been obtained:\n(1) The main reason for the failure of the current task is:\n{BK_JOB_AI_TEMPLATE_VAR{file_task_error_source}}\n\n(2) The machine that failed to upload the source file and the error information (in JSON format) are:\n{BK_JOB_AI_TEMPLATE_VAR{upload_file_error_data}}\n\n(3) The machine that failed to download the target file and the error information (in JSON format) are:\n{BK_JOB_AI_TEMPLATE_VAR{download_file_error_data}}\n\nPlease answer the user''s question:\nBased on the pre-analysis results, please further analyze the error content of the task and provide a summary of the main reasons and processing suggestions.', 'System AI template: Analyze error information for file distribution tasks', 'admin', 'admin', NULL, NULL, '1970-01-01 00:00:00', '1970-01-01 00:00:00');
