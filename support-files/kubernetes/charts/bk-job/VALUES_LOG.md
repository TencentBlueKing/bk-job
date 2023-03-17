# chart values 更新日志

## 0.2.7
1.增加对外暴露API的Ingress配置
```shell script
## 对外暴露API的Ingress配置
apiConfig:
  ingress:
    # 对外暴露API的Ingress是否启用，默认启用
    enabled: true
    annotations:
      kubernetes.io/ingress.class: nginx
      nginx.ingress.kubernetes.io/use-regex: "true"
      # 请求包大小限制
      nginx.ingress.kubernetes.io/proxy-body-size: "10240m"
```

## 0.2.2-rc.7
1.增加文档中心与问题反馈URL配置项
```shell script
# 文档中心 url
bkDocsCenterUrl: "https://bk.tencent.com/docs"
# 问题反馈 url
bkFeedBackUrl: "https://bk.tencent.com/s-mart/community"
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
