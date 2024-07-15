## 作业平台 V3 API 简介

**作业平台（Job）是一套基于蓝鲸智云管控平台 Agent 管道之上的基础操作平台，具备大并发处理能力；除了支持脚本执行、文件拉取/分发、定时任务等一系列可实现的基础运维场景以外，还运用流程化的理念很好的将零碎的单个任务组装成一个作业流程；而每个任务都可做为一个原子节点，提供给其它系统和平台调度，实现调度自动化。**


| 资源名称 | 资源描述 |
| -------- | ------- |
| [batch_get_job_instance_ip_log](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/batch_get_job_instance_ip_log.md) | 根据主机列表批量查询作业执行日志 |
| [callback_protocol](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/callback_protocol.md) | 此组件用于展示回调协议文档 |
| [check_script](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/check_script.md) | 高危脚本检测 |
| [create_account](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/create_account.md) | 创建账号（当前仅支持系统账号） |
| [create_credential](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/create_credential.md) | 新建凭证 |
| [create_dangerous_rule](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/create_dangerous_rule.md) | 新建高危语句检测规则 |
| [create_file_source](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/create_file_source.md) | 新建文件源 |
| [create_public_script](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/create_public_script.md) | 新建公共脚本 |
| [create_public_script_version](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/create_public_script_version.md) | 新建公共脚本版本 |
| [create_script](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/create_script.md) | 新建脚本 |
| [create_script_version](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/create_script_version.md) | 新建脚本版本 |
| [delete_account](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/delete_account.md) | 删除账号 |
| [delete_cron](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/delete_cron.md) | 删除定时任务 |
| [delete_dangerous_rule](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/delete_dangerous_rule.md) | 删除高危语句检测规则 |
| [delete_public_script](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/delete_public_script.md) | 删除公共脚本，同时会删除该脚本下的所有的脚本版本 |
| [delete_public_script_version](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/delete_public_script_version.md) | 删除公共脚本版本 |
| [delete_script](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/delete_script.md) | 删除脚本，同时会删除该脚本下的所有的脚本版本 |
| [delete_script_version](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/delete_script_version.md) | 删除脚本版本 |
| [disable_dangerous_rule](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/disable_dangerous_rule.md) | 停用高危语句检测规则 |
| [disable_public_script_version](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/disable_public_script_version.md) | 禁用公共脚本版本 |
| [disable_script_version](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/disable_script_version.md) | 禁用脚本版本 |
| [enable_dangerous_rule](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/enable_dangerous_rule.md) | 启用高危语句检测规则 |
| [execute_job_plan](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/execute_job_plan.md) | 启动作业执行方案 |
| [fast_execute_script](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/fast_execute_script.md) | 快速执行脚本 |
| [fast_execute_sql](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/fast_execute_sql.md) | 快速执行SQL脚本 |
| [fast_transfer_file](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/fast_transfer_file.md) | 快速分发文件 |
| [generate_local_file_upload_url](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/generate_local_file_upload_url.md) | 生成本地文件上传URL |
| [get_account_list](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/get_account_list.md) | 查询业务下用户有权限的执行账号列表 |
| [get_cron_detail](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/get_cron_detail.md) | 查询定时作业详情 |
| [get_cron_list](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/get_cron_list.md) | 查询业务下定时作业信息 |
| [get_dangerous_rule_list](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/get_dangerous_rule_list.md) | 查看高危语句检测规则列表 |
| [get_job_instance_global_var_value](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/get_job_instance_global_var_value.md) | 获取作业实例全局变量的值 |
| [get_job_instance_ip_log](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/get_job_instance_ip_log.md) | 根据主机查询作业执行日志 |
| [get_job_instance_list](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/get_job_instance_list.md) | 查询作业实例列表（执行历史) |
| [get_job_instance_status](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/get_job_instance_status.md) | 根据作业实例 ID 查询作业执行状态 |
| [get_job_plan_detail](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/get_job_plan_detail.md) | 根据作业执行方案 ID 查询作业执行方案详情 |
| [get_job_plan_list](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/get_job_plan_list.md) | 查询执行方案列表 |
| [get_job_template_list](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/get_job_template_list.md) | 查询作业模版列表 |
| [get_latest_service_version](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/get_latest_service_version.md) | 查询作业平台最新的服务版本号 |
| [get_public_script_list](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/get_public_script_list.md) | 查询公共脚本列表 |
| [get_public_script_version_detail](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/get_public_script_version_detail.md) | 查询公共脚本版本详情 |
| [get_public_script_version_list](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/get_public_script_version_list.md) | 查询公共脚本版本列表 |
| [get_script_list](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/get_script_list.md) | 查询业务脚本列表 |
| [get_script_version_detail](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/get_script_version_detail.md) | 查询业务脚本版本详情 |
| [get_script_version_list](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/get_script_version_list.md) | 查询业务脚本版本列表 |
| [operate_job_instance](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/operate_job_instance.md) | 用于对执行的作业实例进行操作，例如终止作业 |
| [operate_step_instance](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/operate_step_instance.md) | 用于对执行的实例的步骤进行操作，例如重试，忽略错误等 |
| [publish_public_script_version](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/publish_public_script_version.md) | 上线公共脚本版本 |
| [publish_script_version](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/publish_script_version.md) | 上线脚本版本 |
| [push_config_file](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/push_config_file.md) | 分发配置文件，此接口用于分发配置文件等小的纯文本文件 |
| [save_cron](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/save_cron.md) | 新建或保存定时任务 |
| [update_credential](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/update_credential.md) | 更新凭证 |
| [update_cron_status](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/update_cron_status.md) | 更新定时作业状态，如启动或暂停 |
| [update_dangerous_rule](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/update_dangerous_rule.md) | 修改高危语句检测规则 |
| [update_file_source](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/update_file_source.md) | 更新文件源 |
| [update_public_script_basic](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/update_public_script_basic.md) | 更新公共脚本基础信息 |
| [update_public_script_version](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/update_public_script_version.md) | 修改公共脚本版本 |
| [update_script_basic](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/update_script_basic.md) | 更新脚本基础信息 |
| [update_script_version](https://github.com/TencentBlueKing/bk-job/blob/3.9.x/docs/apidoc/esb/jobv3-confapis/apidocs/zh_hans/update_script_version.md) | 修改脚本版本信息 |

