# 蓝鲸作业平台部署与运维文档

[English](operation_en.md) | 简体中文

## 一、部署
蓝鲸作业平台（后续简称作业平台）是蓝鲸原子平台之一，其底层依赖包括：
### 1.强依赖
以下的依赖系统是作业平台的强依赖系统，若缺失或异常将导致作业平台核心功能不可用：    
**PaaS平台**：作业平台调用其接口对接统一登录服务；  
**用户管理**：作业平台调用其接口获取用户信息；  
**权限中心**：作业平台调用其接口获取权限策略对用户操作进行鉴权；  
**APIGateway**：作业平台通过APIGateway调用底层平台接口并为其他上层平台提供接口；  
**配置平台**：作业平台调用其接口获取业务、主机等资源数据；  
**管控平台**：作业平台调用其接口下发脚本执行、文件分发任务并获取任务执行结果与日志。
### 2.弱依赖
以下的依赖系统是作业平台的弱依赖系统，若缺失或异常将导致作业平台部分功能不可用：  
**蓝鲸制品库**：作业平台调用其接口存储本地文件、导入导出、执行日志导出产生的临时文件数据；  
**审计中心**：作业平台输出审计数据日志文件供其采集，用于审计用户操作；  
**日志平台**：作业平台输出的后台日志文件会被其采集器采集，便于在日志平台上进行统一检索；  
**监控平台**：作业平台通过Prometheus协议暴露出指标数据，会被其采集器采集，便于在监控平台上配置指标面板与告警，此外，作业平台通过OTEL协议调用其接口向其上报调用链APM数据，便于在监控平台上进行调用链观测与分析；  
**消息通知中心**：作业平台调用其接口获取通知与公告信息并在页面进行展示。  

由于依赖众多，仅部署作业平台本身是无法正常工作的，需要首先部署其依赖的底层平台，因此请参考[蓝鲸文档中心](https://bk.tencent.com/docs/)部署维护版块的基础套餐相关内容进行部署。

## 二、运维
### 1.资源占用管理
在成功部署作业平台后，随着系统运行时长增加，使用数据随之增加，会导致作业平台占用的资源增加，出于成本与性能考虑，需要对资源占用进行管理，必要时按需清理部分过期数据。  
#### （1）MySQL数据
作业平台的不同功能模块使用不同的数据库存储数据，其中需要重点关注的是任务执行引擎使用的job_execute库，该库存储了任务执行过程中产生的流水数据，数据量随着任务量的增长而增长，通常在平台运行一段时间后，job_execute库会成为占用存储空间最多的库，需要考虑数据的归档与清理。  
作业平台自身支持了任务流水数据的自动归档与清理机制，可通过helm chart values中的backupConfig.archive下的属性进行配置，默认的归档策略为：不启用归档，永久保留所有数据。如果有清理过期数据的需求，可以将策略配置为不备份数据，直接删除超出过期时间（默认为30天）的数据。如果有备份过期数据的需求，可以将策略配置为先备份数据再删除。  

#### （2）MongoDB数据
作业平台使用MongoDB存储任务执行过程中产生的业务脚本执行日志及文件分发任务执行日志数据，默认存储在joblog库中（可通过helm chart values中的相关属性配置），日志数据量随着任务量的增长而增长，通常在平台运行一段时间后，joblog库会成为占用存储空间最多的库，需要考虑数据的归档与清理。  
作业平台使用任务步骤类型（脚本/文件）及日期来组织存储日志数据，其中脚本任务的日志存储在job_log_script_{date}集合中，文件任务的日志存储在job_log_file_{date}集合中，其中{date}为任务创建时的日期，如：job_log_script_2024_07_19、job_log_file_2024_07_20。  
作业平台当前暂未提供自动归档与清理机制，需要用户自行手动执行命令或编写脚本导出/删除需要处理的集合。  

#### （3）蓝鲸制品库数据
作业平台使用蓝鲸制品库存储用户上传的本地文件、导入导出、执行日志导出过程产生的临时文件，默认使用的蓝鲸制品库项目为bkjob（可通过helm chart values中的artifactory相关属性进行配置），使用的仓库为localupload(本地文件)、backup(导入导出临时文件)、filedata(执行日志导出临时文件)，其中localupload仓库中的文件如果被作业/执行方案的本地文件分发步骤引用，则会永久保存，未被引用的文件将在超出过期时间后被自动清理，默认过期时间为7天（可通过helm chart values中的localFile相关属性进行配置），backup与filedata仓库中的文件为临时文件，若系统未自动清理可按需清理。

#### （4）其他数据
**后台日志：** 系统运行过程中产生的后台日志默认打印在容器内的文件中，超出限定容量或过期时间后会被自动清理，通常无需处理，容量与过期时间可通过helm chart values中的log下的属性配置。    
**日志平台数据：** 如果配置了日志采集（helm chart values中的bkLogConfig下的属性），日志平台将通过采集器将后台日志采集到日志平台并存放于其后台存储组件中用于检索，这部分数据需要在日志平台配置清理策略。  
**监控指标数据：** 如果配置了监控数据采集（helm chart values中的serviceMonitor下的属性），监控平台将通过采集器采集后台服务指标数据用于在监控平台通过仪表盘进行查看或配置告警，这部分数据需要在监控平台配置清理策略。  
**APM数据：** 如果配置了APM数据采集（helm chart values中的job.trace下的属性），作业平台将使用OTEL协议向监控平台主动上报后台服务调用链数据用于在监控平台进行调用链的可视化分析，这部分数据需要在监控平台配置清理策略。  

### 2.系统异常排查与恢复
（1）服务状态查看  
作业平台成功部署后，系统管理员可以在Web页面的【平台管理-服务状态】子页面查看系统各服务模块及各个实例的状态，一般情况下各实例状态应当全部为“正常”，若非正常则需要对相应的实例进一步排查。  

（2）Pod状态查看  
在部署作业平台的命名空间中，可以使用kubectl get pod命令结合“bk-job”关键字grep出作业平台的所有Pod，正常情况下作业平台所有Pod状态都应为Running/Completed，若不正常则需要进一步排查对应Pod内的服务日志或者尝试重启Pod。  

（3）服务日志分析  
在容器化部署下，可进入各服务Pod中的容器内查看服务日志，作业平台各服务的日志存储位置为：/data/logs/{服务名}/，例如执行引擎的日志位置为/data/logs/job-execute/，目录下的日志按照类别分为多个文件并且会按时间进行滚动，以下分别介绍各类日志文件的用途：  
error.log: 错误日志汇总，其内容在其余常规日志文件中也会有一份，需要重点关注；  
{服务名}.log: 服务主日志，如execute.log，为服务的核心业务逻辑日志，需要重点关注；  
openapi.log：作业平台对外提供的API（ESB、APIGW等）调用日志；  
schedule.log：执行引擎任务调度日志；  
monitor_task.log：超大任务（涉及的主机数量大）监控日志；  
sync_app_host.log：业务（集）、主机等CMDB资源同步任务与事件监控日志；  
access.log：网关接口访问日志；  
esb_access.log：网关ESB接口访问日志；  
cmdb.log：配置平台（CMDB）相关接口调用日志；  
gse.log：管控平台（GSE）相关接口调用日志；  
iam.log：权限中心（IAM）相关接口调用日志；  
paas.log：PaaS平台相关接口（登录、用户等）调用日志；  
notice.log：消息通知中心相关接口（通知、公告等）调用日志；  
audit_event.log：审计日志，如果配置了对接审计中心，会被审计中心通过采集器进行采集；   
gc.log*：JVM垃圾回收日志，出现JVM级别的性能问题时才需关注。  

**日志分析方法：**  
作业平台的绝大多数服务日志中都包含traceId与spanId，其在一行日志中的位置位于日志级别后，以下通过示例进行说明：  
```shell
[2024-07-19 19:00:00.001]  INFO [job-execute,84a2e275a062728b9b54aab16dea4289,4f786ec86329458a] 6 --- [execute-scheduler-3] e.e.r.h.ResultHandleTaskKeepaliveManager : Refresh task keepalive info start...
```
在上述日志中，traceId为84a2e275a062728b9b54aab16dea4289，spanId为4f786ec86329458a，通过traceId可以过滤出一次请求产生的所有日志（支持跨服务，但网关日志除外），通过spanId可以过滤出一个单独的处理阶段的日志，为了快速排查问题，可以先通过业务特征信息（例如taskInstanceId、stepInstanceId等）找出对应的核心流程的traceId，再用traceId过滤出核心流程所有日志进行分析，建议配置日志采集、清洗并结合日志平台高效检索日志。    
在作业平台的接口（Web、Service、ESB接口等）响应中traceId会作为单独的字段（Web与Service接口为requestId，ESB接口为job_request_id）返回，因此可从接口响应中拿到traceId用于检索日志。
