# Backend Microservice Setup  

English | [简体中文](backend.md)

There are 10 microservices and 1 file source access point (job-file-worker) on bk-job's backend. The compiled products are listed below:

|Package Name | Description |
|:---- |:----|
job-analysis-{version}.jar | Statistical Analysis Microservice Springboot.jar 
job-backup-{version}.jar   | Backup Management Microservice Springboot.jar  
job-config-{version}.jar   | Configuration Center Microservice Springboot.jar  
job-crontab-{version}.jar  | Scheduled Task Microservice Springboot.jar  
job-execute-{version}.jar  | Job Execution Microservice Springboot.jar  
job-file-gateway-{version}.jar | File Gateway Microservice Springboot.jar  
job-file-worker-{version}.jar | File Source Access Point Springboot.jar  
job-gateway-{version}.jar | Backend Gateway Microservice Springboot.jar  
job-logsvr-{version}.jar | Log Management Microservice Springboot.jar  
job-manage-{version}.jar | Job Management Microservice Springboot.jar  
job-ticket-{version}.jar | Ticket Management Microservice Springboot.jar  

**Process**  
1. Check System Dependencies
Make sure that the basic Blueking environment has been properly configured, including PaaS, user management, ESB, CMDB, GSE, and IAM. Make sure the opened ESB interface and backend interface is working properly.

2. Check Basic Environment
Make sure that the bk-job's component dependencies (MySQL, Redis, etc.) have been set up in the previous step.

3. Configure and Launch Service
(1) Create a directory structure as shown below. Place the compiled products and the files under Scripts and Support-files directories in the corresponding locations.
```
|- /data/bkee/job        # Home Directory
  |- support-files       # Resource Files
  |- backend             # The program that stores backend microservices and file source access points
    |- job-manage        # job-manage Microservice Jar package and ops script. There are 11 directories in total, which have already been introduced above.
      |- job-manage.jar  # job-manage Microservice's SpringBoot.jar
      |- bin  
        |- job-manage.sh # job-manage Microservice's ops script
    |- job-xxx
```
(2) After ensuring JAVA_HOME's environment variables have been properly configured, start all microservice processes in sequence:
Example of starting microservice:
```shell script
cd /data/bkee/job/backend/job-config/bin
./job-config.sh start
```  

Sequence:  
job-config (The configuration center is essential for other microservices. Therefore, it needs to be started first.)  
job-manage  
job-backup  
job-logsvr  
job-ticket  
job-file-gateway  
job-execute  
job-crontab  
job-gateway  
job-file-worker  
job-analysis  
The sequence mentioned above is the optimal one recommended as per invocation relations. If you need to start all services immediately, after launching job-config, you can start other microservices and job-file-worker at the same time. Once it's completed, check the log output and process state and restart the failed processes.  
When the microservice is started, log directory will be automatically created:  
${BK_HOME}/logs/${PROJECT_NAME}/${SERVICE_NAME}/
In this example, BK_HOME is /data/bkee, PROJECT_NAME is job, SERVICE_NAME is the name of the microservice. Job-file-worker doesn't count as a microservice, but the way of launching it is the same with other microservices.  
