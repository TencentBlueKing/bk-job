# bk-job About Compiling

English | [简体中文](source_compile.md)

### Compiling Frontend Code

## System Requirement

nodejs 6.9.0 or higher   
npm 6.0.0 or higher

## About Installation

- 1. Package the vue project
Go to the src/frontend directory
```
# Install the dependent package
npm i
```

```
# Local Development
npm run dev
```

```
# Setup build
npm run build
```

## Backend Microservice Compiling (Java)

## System Requirement

- MySQL 5.7
- JDK 1.8
- Gradle 6.3

#### Initialize Database

When compiling PO, JOOQ is used for DB table and PO mapping, which is dependent on the database table. Therefore, database needs to be initialized first. Initialization script can be found in bk-job/support-files/sql. Please log into the database and execute in sequence. 

#### gradle Pre-compiling Config
gradle.properties Change the following configuration items:

  ```
  MAVEN_REPO_URL=Change it into your private Maven library, if there's any. If not, use the public source.
  MAVEN_REPO_SNAPSHOT_URL=Change it into your snapshoot Maven library, if there's any. If not, use the public source.
  MAVEN_REPO_DEPLOY_URL= If you need to deploy your Jar package to your private Maven library, set it as your address.
  MAVEN_REPO_USERNAME=Needs to be filled out when deployed.
  MAVEN_REPO_PASSWORD=Needs to be filled out when deployed.
  DB_HOST=Your database. When compiling, JOOQ needs to connect to the database and access the database table to create PO compiling.
  DB_USERNAME=Username of the database
  DB_PASSWORD=Password of the database
  ```

#### Compiling

```shell
cd bk-job/src/backend & gradle clean build
```

All created products are placed in backend/release. The detailed content is listed below: 

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
