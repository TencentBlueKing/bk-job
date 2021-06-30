# bk-job 编译说明
## Frontend前端代码编译

## 系统要求

nodejs版本 6.9.0及以上   
npm版本 6.0.0及以上

## 安装说明

- 1、打包vue工程
进入到src/frontend目录下
```
# 先安装依赖包
npm i
```

```
# 本地开发
npm run dev
```

```
# 部署构建
npm run build
```

## backend后端微服务编译(java)

### 系统要求

- MySQL 5.7
- JDK 1.8
- Gradle 6.3

#### 数据库初始化

编译PO时用了JOOQ做DB表与PO生成映射，需要依赖于数据库表，所以数据库需要先初始化，初始化脚本在工程bk-job/support-files/sql目录下，登录到数据库中按顺序执行即可。 

#### gradle编译前配置
gradle.properties 配置修改如下配置项:

  ```
  MAVEN_REPO_URL=修改为你的maven私库，如果有的话,没有可用公共的源
  MAVEN_REPO_SNAPSHOT_URL=修改为你的快照Maven私库，如果有的话,没有可用公共的源
  MAVEN_REPO_DEPLOY_URL= 这个是如果你需要将jar包deploy到你的maven私有库，则设置为你的地址
  MAVEN_REPO_USERNAME=需要deploy时才需要填写
  MAVEN_REPO_PASSWORD=需要deploy时才需要填写
  DB_HOST=你的数据库，编译时JOOQ需要连接数据库读取数据库表来进行生成PO编译
  DB_USERNAME=数据库用户名
  DB_PASSWORD=数据库密码
  ```

#### 编译

```shell
cd bk-job/src/backend & gradle clean build
```

构建出来的产物都放在backend/release目录下，主要包含以下产物: 

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
job-ticket-{version}.jar | 凭据管理微服务Springboot.jar  
