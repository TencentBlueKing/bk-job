# 后端微服务部署  

[English](backend.en.md) | 简体中文

bk-job后端共有10个微服务和一个文件源接入点（job-file-worker），编译产物如下表所示：

|包名称 | 描述 |
|:---- |:----|
job-analysis-{version}.jar | 统计分析微服务Springboot.jar 
job-backup-{version}.jar   | 备份管理微服务Springboot.jar  
job-config-{version}.jar   | 配置中心微服务Springboot.jar  
job-crontab-{version}.jar  | 定时任务微服务Springboot.jar  
job-execute-{version}.jar  | 作业执行微服务Springboot.jar  
job-file-gateway-{version}.jar | 文件网关微服务Springboot.jar  
job-file-worker-{version}.jar | 文件源接入点Springboot.jar  
job-gateway-{version}.jar | 后台网关微服务Springboot.jar  
job-logsvr-{version}.jar | 日志管理微服务Springboot.jar  
job-manage-{version}.jar | 作业管理微服务Springboot.jar  

**部署流程**  
1.周边系统依赖确认
确保bk-job依赖的蓝鲸基础环境已搭建好，主要包括PaaS、用户管理、ESB、CMDB、GSE、IAM，确保其开放的ESB接口及后台接口能够正常工作。

2.机器基础环境确认
确保bk-job依赖的组件（MySQL、Redis等）在前述步骤中已搭建完成。

3.服务配置与启动
（1）创建如下图所示的目录结构，将编译所得产物及scripts、support-files目录下文件放置于对应目录。
```
|- /data/bkee/job        # 程序主目录
  |- support-files       # 资源文件
  |- backend             # 存放后台微服务及文件源接入点程序
    |- job-manage        # job-manage微服务相关jar包与运维脚本，共有11个目录，不再一一列举
      |- job-manage.jar  # job-manage微服务的SpringBoot.jar
      |- bin  
        |- job-manage.sh # job-manage微服务的运维脚本
    |- job-xxx
```
（2）确保JAVA_HOME环境变量已正确配置后，按顺序启动各微服务进程：
微服务启动方法示例：
```shell script
cd /data/bkee/job/backend/job-config/bin
./job-config.sh start
```  

启动顺序：  
job-config(配置中心被其他微服务依赖，需要最先完成启动)  
job-manage  
job-backup  
job-logsvr  
job-file-gateway  
job-execute  
job-crontab  
job-gateway  
job-file-worker  
job-analysis  
以上顺序为根据调用关系推荐的逐微服务最佳启动顺序，若需快速启动所有服务，也可在job-config启动完成后并行启动其他所有微服务及job-file-worker，启动完成后检查日志输出与进程状态，对未能成功启动的进程进行重启。  
微服务启动后，将自动创建日志存放目录：  
${BK_HOME}/logs/${PROJECT_NAME}/${SERVICE_NAME}/
该示例中，BK_HOME为/data/bkee，PROJECT_NAME为job，SERVICE_NAME为微服务名称，job-file-worker不属于微服务，但其启动方式与其他微服务相同。  
