# chart values 更新日志
## 0.3.0-rc.37
1.增加文件网关系统file-worker调度标签相关配置
```shell script
## job-file-gateway文件网关服务配置
fileGatewayConfig:
  # 用于确定调度范围的worker标签
  workerTags:
    # 能够调度的worker标签白名单，逗号分隔
    white: "k8s"
    # 不能调度的worker标签黑名单，逗号分隔
    black: ""
## job-file-worker文件源接入点配置
fileWorkerConfig:
  # 标签，逗号分隔
  tags: "k8s"
```

## 0.3.0-rc.31
1.增加Trace及数据上报至APM相关配置
```shell script
## Trace配置
job:
  trace:
    report:
      # 是否上报Trace数据至监控平台APM应用，默认不上报
      enabled: false
      # 监控平台中目标APM应用的PUSH URL
      pushUrl: ""
      # 监控平台中目标APM应用的SecureKey
      secureKey: ""
      # Trace数据上报比率，取值范围为0~1，根据作业平台与监控平台负载适当调节该比率
      ratio: 0.1
```
2.fileWorker对应的Service端口默认值设置为与pod端口一致，避免混淆
```shell script
## job-file-worker文件源接入点配置
fileWorkerConfig:
  service:
    port: 19810
```

## 0.2.2-rc.1
1.增加权限中心系统ID配置：
```shell script
## 权限中心配置
iam:
  # 作业平台注册到IAM的系统ID
  systemId: bk_job
```

## 0.1.53
1.增加CMDB供应商配置：
```shell script
## 对接蓝鲸CMDB参数配置
cmdb:
  # 供应商，默认为0
  supplierAccount: 0
```
2.增加各模块是否启用开关配置：
```shell script
## job-gateway网关配置
gatewayConfig:
  # 模块是否启用，默认启用
  enabled: true
## job-manage作业管理配置
manageConfig:
  # 模块是否启用，默认启用
  enabled: true
## job-execute执行引擎配置
executeConfig:
  # 模块是否启用，默认启用
  enabled: true
## job-logsvr日志服务配置
logsvrConfig:
  # 模块是否启用，默认启用
  enabled: true
## job-backup备份服务配置
backupConfig:
  # 模块是否启用，默认启用
  enabled: true
## job-analysis统计分析服务配置
analysisConfig:
  # 模块是否启用，默认启用
  enabled: true
## job-file-gateway文件网关服务配置
fileGatewayConfig:
  # 模块是否启用，默认启用
  enabled: true
## job-file-worker文件源接入点配置
fileWorkerConfig:
  # 模块是否启用，默认启用
  enabled: true
## job-frontend前端配置
frontendConfig:
  # 模块是否启用，默认启用
  enabled: true
## migration迁移任务配置
migration:
  # 模块是否启用，默认启用
  enabled: true
```
3.更新微服务默认镜像版本。


## 0.1.52
1.更新微服务默认镜像版本；
2.更新k8s-wait-for默认镜像版本；  
**3.增加登录配置**  
```shell script
## 登录配置
login:
  ## 自定义登录配置
  custom:
    # 是否对接自定义登录地址，默认不开启（使用蓝鲸集成的统一登录）
    enabled: false
    # 页面登录地址
    loginUrl: "http://login.example.com/login/"
    # 获取用户信息的接口地址
    apiUrl: "http://login.example.com/api/"
    # 完成页面登录后前端通过Cookie提交的凭据的Key，也是后台向apiUrl获取用户信息提交的凭据的Key
    tokenName: "bk_token"
```
