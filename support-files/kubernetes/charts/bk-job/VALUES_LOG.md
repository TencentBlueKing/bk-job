# chart values 更新日志

## 0.9.1
1. 新增外部系统（GSE、CMDB、IAM、BK-Login、BK-User）重试配置，采用指数退避策略
```yaml
## 外部系统（GSE、CMDB、IAM、BK-Login、BK-User）重试配置
## 采用指数退避策略：重试间隔按指数增长（如 500ms → 1s → 2s → 4s → 8s）
externalSystemRetry:
  # 全局配置
  global:
    # 是否启用外部系统重试（默认开启）
    enabled: true
    # 初始重试间隔（毫秒），默认500ms
    initialIntervalMs: 500
    # 最大重试次数，默认5次
    maxAttempts: 5
    # 最大重试间隔（毫秒），默认30000ms（30秒）
    maxIntervalMs: 30000
    # 间隔增长倍数，默认2.0
    multiplier: 2.0
    # 是否启用重试指标采集（默认开启）
    metricsEnabled: true
    # 熔断器配置
    circuitBreaker:
      # 是否启用熔断器（默认关闭）
      enabled: false
      # 失败率阈值（百分比），默认80.0%
      failureRateThreshold: 80.0
      # 慢调用率阈值（百分比），默认90.0%
      slowCallRateThreshold: 90.0
      # 慢调用时长阈值（毫秒），默认30000ms（30秒）
      slowCallDurationThresholdMs: 30000
      # 滑动窗口大小，默认100次调用
      slidingWindowSize: 100
      # 最小调用次数（达到此次数后才开始计算失败率），默认10次
      minimumNumberOfCalls: 10
      # 熔断器开启状态下的等待时间（毫秒），默认30000ms（30秒）
      waitDurationInOpenStateMs: 30000
      # HALF_OPEN 状态允许的调用次数，默认10次
      permittedCallsInHalfOpenState: 10
      # 熔断器 OPEN 时是否快速失败：true：快速失败（抛出异常），false：继续调用但不重试
      fastFail: false
  # 各外部系统单独配置（可选，不配置则使用全局配置）
  cmdb:
    circuitBreaker:
      # 白名单（API名称列表，这些API不参与熔断，一般情况下无需修改）
      whiteApiList:
        # CMDB 事件监听接口，正常耗时约 20 秒
        - "getBizEvents"
        - "getBizSetEvents"
        - "getBizSetRelationEvents"
        - "getHostEvents"
        - "getHostRelationEvents"
  # iam:
  #   enabled: true
  #   circuitBreaker:
  #     enabled: true
  # gse:
  #   enabled: true
  #   circuitBreaker:
  #     enabled: true
  # bkLogin:
  #   enabled: true
  #   circuitBreaker:
  #     enabled: true
  # bkUser:
  #   enabled: true
  #   circuitBreaker:
  #     enabled: true
```

2. 移除旧的 GSE V2 重试配置，统一使用新的外部系统重试配置
```yaml
# 被移除的配置项
gseV2:
  # 重试策略
  retry:
    # 是否开启重试
    enabled: false
    # 含重试的最大执行次数
    maxAttempts: 3
    # 重试间隔（单位：秒）
    intervalSeconds: 5
```

3. 新增前端提给后端账号密码的加密算法配置
```yaml
job:
  encrypt:
    # SM2加密算法原始公钥(可以通过op-tools/sm2_keypair/generate_sm2_keypair.py工具生成)
    sm2PublicKey: ""
    # SM2加密算法原始私钥(可以通过op-tools/sm2_keypair/generate_sm2_keypair.py工具生成)
    sm2PrivateKey: ""
```

## 0.9.0
1. 新增 bk-login/bk-user蓝鲸网关配置
```yaml
# 蓝鲸登录 API Gateway url
bkLoginApiGatewayUrl: "http://bkapi.example.com/api/bk-login/prod"
# 蓝鲸用户管理 API Gateway url
bkUserApiGatewayUrl: "http://bkapi.example.com/api/bk-user/prod"
# 蓝鲸用户管理前端服务 API Gateway url
bkUserWebApiGatewayUrl: "http://bkapi.example.com/api/bk-user-web/prod"
# 蓝鲸权限中心 API Gateway url
bkIamApiGatewayUrl: "http://bkapi.example.com/api/bk-iam/prod"
# 消息通知 API Gateway url
bkCmsiApiGatewayUrl: "http://bkapi.example.com/api/cmsi/prod"
```

2. 新增租户配置
```yaml
# 多租户配置
tenant:
  # 是否启用多租户
  enabled: false
```

3. 去除GSE 1.0相关配置项，不再支持使用1.0的Thrift协议调用GSE接口（但是在GSE 2.0服务端兼容1.0 Agent的前提下，作业平台支持用2.0的HTTP协议调用GSE 2.0接口管控1.0的Agent）
```yaml
# 被去除的配置项
gse:
  # 是否初始化 GSE1.0 Client。如果需要对接GSE1.0(job.features.gseV2.enabled=false), 必须设置gse.enabled=true
  enabled: true
  # 已存在的Tls证书Secret名称
  existingTlsSecret: ""
  # Agent状态查询接口相关配置
  cacheApiServer:
    # 接口地址host
    host: "gse-api.example.com"
    # 端口
    port: 59313
  # ssl证书相关配置
  ssl:
    # 是否开启SSL，默认开启
    enabled: true
    keystore:
      ## 证书keystore文件单行base64编码值，此处默认值为社区版公开的默认证书keystore内容，正式部署请自行修改
      base64Content: "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"
      ## keystore的密码，此处默认值为社区版公开的默认证书keystore的密码，正式部署请自行修改
      password: "2y#8VI2B4Sm9Dk^J"
    truststore:
      ## 证书truststore文件单行base64编码值，此处默认值为社区版公开的默认证书truststore内容，正式部署请自行修改
      base64Content: "/u3+7QAAAAIAAAABAAAAAgACY2EAAAF8F68PKQAFWC41MDkAAAM4MIIDNDCCAhwCCQC4iHdzcyDjoTANBgkqhkiG9w0BAQUFADBbMQswCQYDVQQGEwJDTjELMAkGA1UECAwCR0QxCzAJBgNVBAcMAlNaMQwwCgYDVQQKDANpZWcxEDAOBgNVBAsMB2JsdWtpbmcxEjAQBgNVBAMMCUdTRVJPT1RDQTAgFw0xOTAyMjUxMDMyMDRaGA8yMTE5MDIwMTEwMzIwNFowWzELMAkGA1UEBhMCQ04xCzAJBgNVBAgMAkdEMQswCQYDVQQHDAJTWjEMMAoGA1UECgwDaWVnMRAwDgYDVQQLDAdibHVraW5nMRIwEAYDVQQDDAlHU0VST09UQ0EwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDWRsDA1FRFoVPJkLA0XBk3ZHFg5CGjVDeZJGw2L+Y5SJEt/KTB4sLfwIELO1mldos6VEDuGQGAk3g4oA8qqY5jrZXRRRd/bUanyb1GaXi4oXMnZaTy+uGZcYHTpI0RPuzIMD5e/P5+ecsBlnFnAxCjBY2Ivfg04QuwxYMST2IvCy4qpfeuc+TGvucZVdsVrIvKVqHyCGAmeRvHcGCbh/i0SUi30BXljOy+LVE5PqdWmoyFAelCbMyKclAmv+Yhm38NE8bgahV9Zb8zc4sxiU3gGqyrXUzTFII6/hdHPyhNYKniUWxaI7BWkfFyJ75aJPcawqhBoLCA4XVFKpUNBGQHAgMBAAEwDQYJKoZIhvcNAQEFBQADggEBAMnrbZFPGtfHBio/bl7V1as8NNeJWAPRj5dPr2cPBFyPHyAccixlLlh0wXr1EvyBpmENQ3P3IuPq+j37AcJS4YnS/1ncZhDZfOVIOxGvjaMIOTADQj4SIB2FMv2541Z0Y51WYwDO8x36YKBVJZKQ+xVhFI4ZknkZS7gTGyuYDy8FT020Yi4ufAhga8Zw3qun3RzE9b0Ua8XGfPIVuqB7pk5Bn++ou/2t6eb2qkEKBo/lRqHxWQk1og2L+7tibhK3D48WATZNKd81Sm3RYOMpmjrgVwIG/2FGInWoa+VdbVvxdKFSzRBLHMMETockEQTwmKFT9neXwxNM5oeT+W+Q2QrbpmZGDsc6xjUy7MDM2PvfOjI3OA=="
      ## truststore的密码，此处默认值为社区版公开的默认证书truststore的密码，正式部署请自行修改
      password: "2y#8VI2B4Sm9Dk^J"
  # 任务下发接口相关配置
  taskserver:
    # 接口地址host
    host: "gse-task.example.com"
    # 端口
    port: 48673
  # 服务发现配置
  server:
    discovery:
      # 服务发现模式：取值为zookeeper，不使用zookeeper则无需配置
      type: zookeeper
    zookeeper:
      connect:
        # zookeeper连接字符串，由host:port构成
        string: "gse-zk.example.com:2181"
```

4. 去除权限中心后台接口配置（调用权限中心的请求改为全部走APIGW）
```yaml
# 蓝鲸 IAM 后台 url
bkIamApiUrl: "http://bkiam-api.example.com"
```

5. 支持按子域名/子路径模式部署
```yaml
bkWebSiteAccess:
  # 可选值：subdomain（子域名）、subpath（子路径）
  mode: "subdomain"
  # 子域名模式生效的配置
  # subdomain:
    # 请补充可能配置的特性项目，暂无
  # 子路径模式生效的配置
  subpath:
    # 根路径前缀（如 "/app" 则访问路径为 https://www.example.com/app/xxx）
    rootPrefix: "/job"
```

## 0.8.13
1. job-gateway访问日志优化
```yaml
gatewayConfig:
  customAccessLog:
    # 是否开启自定义访问日志，默认开启
    enabled: true
```

## 0.8.12
1. 升级过程中的SQL更新支持增量更新
```yaml
job:
  migration:
    mysqlSchema:
      # 是否开启增量更新模式，默认开启
      incrementalEnabled: true
```

## 0.8.11
1. 支持针对任务历史查询配置复杂查询限制
```yaml
## job-execute执行引擎配置
executeConfig:
  # 任务历史查询配置
  taskHistoryQuery:
    # 复杂查询限制，用于阻止大业务下大时间范围的复杂查询导致慢查询影响整个系统
    complexQueryLimit:
      # 是否开启，默认开启
      enabled: true
      # 允许查询扫描的最大数据量，默认2000万
      maxQueryDataNum: 20000000
      # 样本天数，用于估算平均每天任务量，默认7天
      sampleDays: 7
```

## 0.8.9
1. 支持多集群部署（共享MySQL、MongoDB、Redis、周边系统等基础组件）
```yaml
# 部署集群配置，单集群部署时无需修改，多集群部署且共享DB时需要分别指定不同的集群名称，用于避免资源冲突
cluster:
  ## 集群名称
  name: default
```

## 0.8.6
1. 文件分发时采用外部的Gse Agent代替Job机器作为源机器分发
```yaml
externalGseAgent:
  ## 默认不使用集群外的GSE Agent分发文件
  enabled: false
    ## 与集群外GSE Agent共享文件采用的storageClass名
  storageClass: nfs-client
  ## 期望的大小
  storageSize: 200Gi
  ## 集群外部已安装 GSE Agent 的机器
  hosts:
    - bkCloudId: 0
      ip: ""
```

## 0.8.3
1. 作业执行日志归档配置
```yaml
backupConfig:
  archive:
    executeLog:
      # 是否开启执行日志归档
      enabled: true
      # 是否试运行，试运行下仅打印日志不会真删除mongodb数据
      dryRun: true
      # 归档模式，仅支持删除
      mode: deleteOnly
      # 归档任务运行的cron表达式，默认每天凌晨04:00
      cron : 0 0 4 * * *
      # 执行日志保留天数(要结合执行历史的保留时间合理配置执行日志保留天数)
      keepDays: 360
      # 归档任务并发数量
      concurrent: 6
```

## 0.8.2
1. AI小鲸支持配置使用的大模型
```yaml
analysisConfig:
  # AI相关配置
  ai:
    # 使用的大模型类型，默认腾讯混元，支持的取值：hunyuan、hunyuan-turbo、gpt-4、gpt-4o、gpt-4o-mini、gpt-3.5-turbo等，
    # 平台会将任务相关的脚本、参数、日志等数据发送给大模型，使用大模型时请注意数据安全风险
    model: hunyuan
```

2. 调整pod删除时等待优雅关闭的最大时间，从 40s -> 60s
```yaml
# pod删除时等待优雅关闭的最大时间，单位为秒（超出后强制删除）
podTerminationGracePeriodSeconds: 60
```

## 0.8.0
1. 增加按主机拓扑路径鉴权相关配置
```yaml
executeConfig:
  # 权限中心相关配置
  iam:
    # 按主机拓扑路径鉴权相关配置
    hostTopoPath:
      # 主机拓扑路径填充服务是否开启，如果需要使用按主机拓扑路径鉴权功能则必须开启，默认开启
      enabled : true
      # 缓存配置
      cache:
        # 是否开启，默认开启
        enabled: true
        # 过期时间（s）
        expireSeconds: 10
```
## 0.7.4
1. 增加作业执行结果轮询规则的配置

```yaml
executeConfig:  
  result:
    pollingStrategy:
      # 脚本执行   
      script:
        # 轮询间隔表，间隔不应太短，任务量大会给执行引擎、GSE增加压力，应根据实际情况渐进式调整
        # Key：轮询次数起点-轮询次数终点，Value：使用的轮询间隔时间，单位为毫秒
        intervalMap:
          # 第1-10次轮询（轮询用时<=10s），每1000ms一次
          1-10: 1000
          # 第11-35次轮询（轮询用时：10s-1min），每2000ms轮询一次
          11-35: 2000
          # 第36-88次轮询（轮询用时：1min-5min），每5000ms轮询一次
          36-88: 5000
        # 超出轮询间隔表中配置的轮询次数后使用的统一间隔（单位：毫秒）    
        finalInterval: 10000
      # 文件分发  
      file:
        intervalMap:
          # 第1-2次轮询（轮询用时<=2s），每1000ms一次
          1-2: 1000
          # 第3-11次轮询（轮询用时：2s-20s），每2000ms轮询一次
          3-11: 2000
          # 第12-67次轮询（轮询用时：20s-5min），每5000ms轮询一次
          12-67: 5000
        # 超出轮询间隔表中配置的轮询次数后使用的统一间隔（单位：毫秒）    
        finalInterval: 10000
```

## 0.7.3
1. 增加连接外部MariaDB、Redis、RabbitMQ、MongoDB支持TLS相关配置
```yaml
externalMariaDB:
  ## TLS相关配置
  tls:
    ## 是否开启tls认证
    enabled: false
    ## 存储trustStore与keyStore的secret名称
    existingSecret: ""
    ## 单向tls认证配置
    ## 密钥库类型：支持PKCS12、JKS，默认JKS
    trustStoreType: "JKS"
    ## 信任库文件名称（与Secret中的Key一致）
    trustStoreFilename: "truststore.jks"
    ## 信任库密码
    trustStorePassword: ""
    ## 双向tls认证配置
    ## 密钥库类型：支持PKCS12、JKS，默认PKCS12
    keyStoreType: "PKCS12"
    ## 密钥库文件名称（与Secret中的Key一致）
    keyStoreFilename: ""
    ## 密钥库密码
    keyStorePassword: ""

## job-backup备份服务配置
backupConfig:
  ## 数据归档配置
  archive:
    # 归档使用的MariaDB实例，若开启归档且开启 DB 数据备份，必须配置该项内容
    mariadb:
      ## TLS相关配置
      tls :
        ## 是否开启tls认证
        enabled : false
        ## 存储trustStore与keyStore的secret名称
        existingSecret : ""
        ## 单向tls认证配置
        ## 密钥库类型：支持PKCS12、JKS，默认JKS
        trustStoreType : "JKS"
        ## 信任库文件名称（与Secret中的Key一致）
        trustStoreFilename : "truststore.jks"
        ## 信任库密码
        trustStorePassword : ""
        ## 双向tls认证配置
        ## 密钥库类型：支持PKCS12、JKS，默认PKCS12
        keyStoreType : "PKCS12"
        ## 密钥库文件名称（与Secret中的Key一致）
        keyStoreFilename : ""
        ## 密钥库密码
        keyStorePassword : ""
        ## 是否校验主机名
        verifyHostname : false

## 应用MySQL表结构Migration时使用的TLS配置
job:
  migration:
    mysqlSchema:
      ## TLS相关配置
      tls:
        ## 是否开启tls认证
        enabled: false
        ## 存储证书与私钥文件内容的secret名称
        existingSecret: ""
        ## 单向tls认证配置
        ## 客户端需要信任的服务端CA证书文件（PEM格式）名称（与Secret中的Key一致）
        certCAFilename: "ca.pem"
        ## 双向tls认证配置
        ## 服务端需要信任的客户端证书文件（PEM格式）名称（与Secret中的Key一致）
        certFilename: ""
        ## 客户端私钥文件名称（与Secret中的Key一致）
        certKeyFilename: ""
        ## 是否校验主机名
        verifyHostname: false

externalRedis:
  ## TLS相关配置
  tls:
    ## 是否开启tls认证
    enabled: false
    ## 存储trustStore与keyStore的secret名称
    existingSecret: ""
    ## 单向tls认证配置
    ## 密钥库类型：支持PKCS12、JKS，默认JKS，注意：连接Redis使用的trustStoreType与keyStoreType必须相同，若不同则以keyStoreType为准
    trustStoreType: "JKS"
    ## 信任库文件名称（与Secret中的Key一致）
    trustStoreFilename: "truststore.jks"
    ## 信任库密码
    trustStorePassword: ""
    ## 双向tls认证配置
    ## 密钥库类型：支持PKCS12、JKS，默认JKS
    keyStoreType: "JKS"
    ## 密钥库文件名称（与Secret中的Key一致）
    keyStoreFilename: ""
    ## 密钥库密码
    keyStorePassword: ""
    ## 是否校验主机名
    verifyHostname: false
    
externalRabbitMQ:
  ## TLS相关配置
  tls:
    ## 是否开启tls认证
    enabled: false
    ## 存储trustStore与keyStore的secret名称
    existingSecret: ""
    ## 单向tls认证配置
    ## 密钥库类型：支持PKCS12、JKS，默认JKS
    trustStoreType: "JKS"
    ## 信任库文件名称（与Secret中的Key一致）
    trustStoreFilename: "truststore.jks"
    ## 信任库密码
    trustStorePassword: ""
    ## 双向tls认证配置
    ## 密钥库类型：支持PKCS12、JKS，默认PKCS12
    keyStoreType: "PKCS12"
    ## 密钥库文件名称（与Secret中的Key一致）
    keyStoreFilename: ""
    ## 密钥库密码
    keyStorePassword: ""
    ## 是否校验主机名
    verifyHostname: false
    
externalMongoDB:
  ## TLS相关配置
  tls:
    ## 是否开启tls认证
    enabled: false
    ## 存储trustStore与keyStore的secret名称
    existingSecret: ""
    ## 单向tls认证配置
    ## 密钥库类型：支持PKCS12、JKS，默认JKS
    trustStoreType: "JKS"
    ## 信任库文件名称（与Secret中的Key一致）
    trustStoreFilename: "truststore.jks"
    ## 信任库密码
    trustStorePassword: ""
    ## 双向tls认证配置
    ## 密钥库类型：支持PKCS12、JKS，默认PKCS12
    keyStoreType: "PKCS12"
    ## 密钥库文件名称（与Secret中的Key一致）
    keyStoreFilename: ""
    ## 密钥库密码
    keyStorePassword: ""
    ## 是否校验主机名
    verifyHostname: false
```

2. 修改备份服务热库的保留时间配置项
```yaml
backupConfig:
  archive:
    execute:
      # 热库中的数据保留时间（天）
      keepDays: 30
```

## 0.7.1
1. 增加AI相关配置

```yaml
# 蓝鲸 AIDev API Gateway url
bkAIDevApiGatewayUrl: "http://bkapi.example.com/api/aidev"
# 调用蓝鲸AIDev API使用的appCode，如果AIDev API与当前Job在同一个环境则无需配置，直接使用appCode的值
bkAIDevAppCode: ""
# 调用蓝鲸AIDev API使用的appSecret，如果AIDev API与当前Job在同一个环境则无需配置，直接使用appSecret的值
bkAIDevAppSecret: ""
analysisConfig:
  # AI相关配置
  ai:
    # 是否开启AI功能，默认不开启
    enabled: false
    # AI分析错误日志功能相关配置
    analyzeErrorLog:
      # 支持分析的错误日志最大长度，单位支持B、KB、MB、GB、TB、PB，默认5MB
      logMaxLength: "5MB"
    # AI对话记录相关配置
    chatHistory:
      # 最大保留天数，默认31天
      maxKeepDays: 31
      # 单个用户最大保留的对话记录数量，默认1000条
      maxHistoryPerUser: 1000
```

## 0.7.0
1. 增加接入蓝鲸网关配置

```yaml
bkApiGatewayConfig:
  # 是否自动把API注册到蓝鲸网关
  sync: false
  # 是否自动发布资源，true：生成版本且发布资源，false：只生成版本不发布资源
  autoPublish: true
  # 是否开启apigw jwt认证
  enabled: true
  jwtPublicKey:
    # jwtPublicKey获取策略，获取失败重试：retry, 获取失败终止启动：abort
    failPolicy: "retry"
  # 接入的网关名称，蓝鲸官方网关都是bk-开头
  gatewayName: "bk-job"
  # 蓝鲸网关url
  url: "http://bkapi.example.com/api/bk-apigateway"
  # 接入环境
  stage: "prod"
  # 接入的api资源目录, 默认是/data/apidocs/
  resourceDir: "/data/apidocs/"
  # 网关维护人员，仅维护人员有管理网关的权限, 多个维护人员逗号分隔，如:"user1,user2"
  maintainers: "admin"
```

## 0.6.5
1. 增加正在执行中的作业总量的配额限制

```yaml
job:
  # 资源配额限制
  resourceQuotaLimit:
    resources:
      # 配额限制资源-正在执行中的作业
      runningJob:
        # 是否启用配额限制
        enabled: true
        # 正在执行中的作业总量限制
        capacity: "10000"
        # 基于资源管理空间(业务/业务集)的配额限制
        resourceScopeQuotaLimit:
          # 全局限制，每个资源管理空间默认的配额限制
          global: "20%"
          # 自定义配额限制，会覆盖 global
          custom: "biz:2=1000,biz_set:9991001=50%"
        # 基于蓝鲸应用的配额限制
        appQuotaLimit:
          global: "20%"
          custom: "bk-nodeman=1000,bk-soap=50%"
```

## 0.6.4
1. 增加 全局配置（title/footer/name/logo/产品商标）相关的前端资源文件基础地址与base.js路径

```yaml
# 全局配置（title/footer/name/logo/产品商标）相关的前端资源文件基础地址，例如：http://bkrepo.example.com/generic/blueking/bk-config，留空则采用前端默认值
bkSharedResUrl: ""
# 全局配置（title/footer/name/logo/产品商标）相关的前端资源文件base.js路径
bkSharedBaseJsPath: "/bk_job/base.js"
```

## 0.6.2
1. 增加 GSE 脚本任务执行结果查询 API 请求参数配置

```yaml
executeConfig:
  scriptTask:
    query:
      # 脚本任务执行，从 GSE 查询结果的 API 单次返回的脚本执行输出内容最大长度。该参数需要合理设置，避免因为输出日志太多导致拉取 GSE 执行结果 API 超时。该参数支持使用不同的单位(KB/MB/GB)
      contentSizeLimit: 512MB
```

## 0.6.0
1. 增加额外定义的前端页面访问URL

```yaml
# 额外定义的前端页面访问URL
job:
  web:
    extraWebUrls: "http://my.job.example.com"
```

2. 增加 cmdb API 蓝鲸网关配置

```yaml
# cmdb API Gateway url
bkCmdbApiGatewayUrl: "http://bkapi.example.com/api/cmdb"
```

## 0.5.10
1. 增加MySQL migration的自定义数据库账号和密码

```yaml
job:
  migration:
    mysqlSchema:
      # mysql数据库migrate时使用的管理员用户名，不填默认是root
      adminUsername: ""
      # mysql数据库migrate时使用的管理员密码，不填使用mariadb/externalMariaDB的root密码
      adminPassword: ""
```

## 0.5.9

1. 新增备份服务中的数据归档相关配置

```yaml
backupConfig:
  ## 数据归档配置
  archive:
    # 归档使用的MariaDB实例，若开启归档且开启 DB 数据备份，必须配置该项内容
    mariadb:
      host: ""
      port: ""
      username: "job"
      password: "job"
      connection:
        properties: ?useUnicode=true&characterEncoding=UTF-8&zeroDateTimeBehavior=convertToNull
    # job-execute模块的归档配置
    execute:
      # 是否启用 DB 归档
      enabled: true
      # 被归档 DB 的配置
      mariadb:
        connection:
          properties: ?useUnicode=true&characterEncoding=UTF-8&zeroDateTimeBehavior=convertToNull
      # 归档模式。deleteOnly: 仅删除； backupThenDelete: 先备份数据再删除。默认 deleteOnly
      mode: backupThenDelete
      # 归档任务运行的cron表达式，默认每天凌晨04:00
      cron: 0 0 4 * * *
      # 热库中的数据保留时间（天）
      keeyDays: 30
      # 归档数据读取时每次读取的数据量（单个表），服务内存受限时可适当降低该值
      readIdStepSize: 1000
      # 每次从 db 表中读取的行数
      readRowLimit: 10000
      # 归档数据写入归档库时每次写入的数据量（单个表），服务内存受限时可适当降低该值
      batchInsertRowSize: 1000
      # 每次执行删除的最大行数。为了减少 MySQL 事务时长，DB 性能受限时可适当降低该值
      deleteRowLimit: 1000
      # 表作用域的归档配置，会覆盖全局配置
      tableConfigs:
        # 表名
        task_instance:
          # 归档数据读取时每次读取的数据量（单个表），服务内存受限时可适当降低该值
          readIdStepSize: 2000
          # 每次从 db 表中读取的行数
          readRowLimit: 20000
          # 归档数据写入归档库时每次写入的数据量（单个表），服务内存受限时可适当降低该值
          batchInsertRowSize: 2000
          # 每次执行删除的最大行数。为了减少 MySQL 事务时长，DB 性能受限时可适当降低该值
          deleteRowLimit: 2000
```

## 0.5.8
1. 增加JVM诊断文件留存配置

```yaml
# JVM诊断文件留存配置
jvmDiagnosticFile:
  # 根据最后修改时间清理
  clearByLastModifyTime:
    # 是否开启自动清理任务，默认开启
    enabled: true
    # JVM诊断文件保留的小时数，默认168小时（7天）
    keepHours: 168
```

## 0.5.7
1. 增加日志留存配置

```yaml
# 日志留存配置
log:
  # 服务后台日志保留的小时数，默认48小时（2天）
  keepHours: 48
  # 根据磁盘占用量自动清理后台日志相关配置
  clearByVolumeUsage:
    # 是否开启自动清理任务，默认开启
    enabled: true
    # 服务后台日志可使用的最大磁盘空间（超出后将清理最旧的日志文件，但每类日志文件至少保留一个），单位支持B、KB、MB、GB、TB、PB，默认40GB
    maxVolume: 40GB
```

## 0.5.6
1. 增加消息通知中心配置

```yaml
# 蓝鲸消息通知中心 API Gateway url
bkNoticeApiGatewayUrl: "http://bkapi.example.com/api/bk-notice"
# 消息通知中心配置
bkNotice:
  # 是否对接消息通知中心
  enabled: true
```

## 0.5.5
1. 增加权限中心web地址配置

```yaml
# 蓝鲸 IAM url
bkIamUrl: "http://bkiam.example.com"
```

## 0.5.4
1. 增加定时任务服务独立数据库配置（若不配置该项则使用与其他服务共用的公共数据库），默认无需配置

```yaml
## job-crontab定时任务配置
crontabConfig:
  # 定时任务服务独立数据库配置，若不配置该项则使用与其他服务共用的公共数据库
  database:
    host: ""
    port: 3306
    connection:
      properties: ?useUnicode=true&characterEncoding=UTF-8&zeroDateTimeBehavior=convertToNull
    username: "job"
    password: "job"
```

## 0.5.3
1. 增加操作审计相关配置

```yaml
## 操作审计配置
audit:
  # 是否开启操作审计
  enabled: true
  # 作业平台系统ID
  systemId: bk_job
```
2. 增加pod删除时等待优雅关闭的最大时间配置

```yaml
# pod删除时等待优雅关闭的最大时间，单位为秒（超出后强制删除）
podTerminationGracePeriodSeconds: 40
```

## 0.5.2
1.增加依赖宿主机GSE Agent的分发相关配置

```yaml
## 依赖宿主机GSE Agent的文件分发相关配置
fileDistribute:
  # 分发文件所在根目录：宿主机路径（以HostPath方式挂载到容器内）
  hostPath: /data/bkjob
```

2.去除实际上并未用到的`persistence.enabled`配置

3.临时文件存储根路径`persistence.localStorage.path`默认值修改为`/data/job_temp_file`

4.新增job-file-gateway文件网关任务重调度相关配置
```yaml
## job-file-gateway文件网关服务配置
fileGatewayConfig:
  # 任务重调度相关配置
  reDispatch:
    # 超时任务
    timeoutTask:
      # 是否开启重调度
      enabled: true
      # 超时时间（秒）
      timeoutSeconds: 10

```

## 0.5.1
1.增加轻量化部署配置

```yaml
deploy:
  ## 部署方式。支持标准(standard) 和 轻量化部署(lite)方式
  mode: standard
```
2. 修改备份服务中的数据归档相关配置

```yaml
backupConfig:
  ## 数据归档配置
  archive:
    # 归档使用的MariaDB实例，若开启归档且开启 DB 数据备份，必须配置该项内容
    mariadb:
      host: ""
      port: ""
      username: "job"
      password: "job"
      connection:
        properties: ?useUnicode=true&characterEncoding=UTF-8&zeroDateTimeBehavior=convertToNull
    # job-execute模块的归档配置
    execute:
      # 是否启用 DB 归档
      enabled: false
      # 归档模式。deleteOnly: 仅删除； backupThenDelete: 先备份数据再删除。默认 deleteOnly
      mode: deleteOnly
      # 归档任务运行的cron表达式，默认每天凌晨04:00
      cron: 0 0 4 * * *
      # 热库中的数据保留时间（天）
      keep_days: 30
      # 归档数据读取时每次读取的数据量（单个表），服务内存受限时可适当降低该值
      read_id_step_size: 1000
      # 归档数据写入归档库时每次写入的数据量（单个表），服务内存受限时可适当降低该值
      batch_insert_row_size: 1000
```

## 0.5.0
1.增加 加密类型 配置

```yaml
job:
  encrypt:
    # 可选值：CLASSIC（经典国际算法RSA、AES等），SHANGMI（国家商用密码算法SM2、SM4等）
    type: "CLASSIC"
```

## 0.4.6
1.增加备份服务中的数据归档相关配置
```yaml
## job-backup备份服务配置
backupConfig:
  ## 数据归档配置
  archive:
    # 归档使用的MariaDB实例，若开启归档，必须配置该项内容
    mariadb:
      host: ""
      port: ""
      username: "job"
      password: "job"
      connection:
        properties: ?useUnicode=true&characterEncoding=UTF-8&zeroDateTimeBehavior=convertToNull
    # job-execute模块的归档配置
    execute:
      # 是否开启数据归档，默认不开启
      enabled: false
      # 归档任务运行的cron表达式，默认每天凌晨04:00
      cron: 0 0 4 * * *
      data:
        # 热库中的数据保留时间（天）
        keep_days: 30
        # 归档数据读取时每次读取的数据量（单个表），服务内存受限时可适当降低该值
        read_id_step_size: 1000
        # 归档数据写入归档库时每次写入的数据量（单个表），服务内存受限时可适当降低该值
        batch_insert_row_size: 1000
      delete:
        # 是否删除热库中的过期老数据，默认不删除
        enabled: false
```

## 0.4.5
1.增加 bkDomain 配置

- 配置说明
```yaml
global:
  # 蓝鲸根域名。指蓝鲸产品公共 cookies 写入的目录，同时也是各个系统的公共域名部分
  bkDomain: "example.com"
```
## 0.4.0
1.增加特性开关相关配置

- 特性开关配置说明
```yaml
job:
  features:
    # 是否开启文件管理特性，容器化环境默认开启
    fileManage: 
      enabled: true
    # 是否兼容ESB API 的 bk_biz_id 参数
    bkBizIdCompatible:
      enabled: true
    # 是否对接GSE2.0。 如果需要对接GSE1.0，设置job.features.gseV2.enabled=false
    gseV2:
      enabled: true
      # 特性启用策略，当enabled=true的时候生效；如果不配置，那么仅判断enabled字段
      strategy:
        # 特性策略ID，支持ResourceScopeWhiteListToggleStrategy/ResourceScopeBlackListToggleStrategy
        id: ResourceScopeWhiteListToggleStrategy
        # 特性策略初始化参数，kv结构，具体传入参数根据不同的StrategyId变化
        params: 
          resourceScopeList: "biz:2,biz_set:9999001"
```

- 特性灰度策略说明
```yaml
# 灰度策略使用说明
# ResourceScopeWhiteListToggleStrategy
job:
  features:
    gseV2:
      enabled: true
      # 特性启用策略，当enabled=true的时候生效；如果不配置，那么仅判断enabled字段
      strategy:
        # 按照资源范围(业务/业务集)白名单灰度
        id: ResourceScopeWhiteListToggleStrategy
        params: 
          # 表示业务(ID=2)和业务集(ID=9999001)
          resourceScopeList: "biz:2,biz_set:9999001"
          
# ResourceScopeBlackListToggleStrategy
job:
  features:
    gseV2:
      enabled: true
      # 特性启用策略，当enabled=true的时候生效；如果不配置，那么仅判断enabled字段
      strategy:
        # 按照资源范围(业务/业务集)黑名单灰度
        id: ResourceScopeBlackListToggleStrategy
        params: 
          # 表示业务(ID=2)和业务集(ID=9999001)
          resourceScopeList: "biz:2,biz_set:9999001"
```


2. 新增GSE1.0 控制开关

```yaml
gse:
  # 是否初始化 GSE1.0 Client。如果需要对接GSE1.0(job.features.gseV2.enabled=false), 必须设置gse.enabled=true
  enabled: true
```

3. 新增GSE API Gateway URL

```yaml
# 蓝鲸 GSE API Gateway url。格式:{网关访问地址}/{网关环境}，网关访问地址、网关环境的取值见bk-gse网关API文档。
bkGseApiGatewayUrl: "https://bk-gse.apigw.com/prod"
```

4. 新增job-k8s-config-watcher 配置，用于监听 Job 配置文件的变化并发送事件给涉及的微服务

```yaml
## job-k8s-config-watcher 配置
k8sConfigWatcherConfig:
  image:
    registry: hub.bktencent.com
    repository: springcloud/spring-cloud-kubernetes-configuration-watcher
    tag: "3.0.0"
    pullPolicy: IfNotPresent
    pullSecrets: []
  hostAliases: []
  containerSecurityContext:
    enabled: false
    runAsUser: 1001
    runAsNonRoot: true
  podSecurityContext:
    enabled: false
    fsGroup: 1001
  podAffinityPreset: ""
  podAntiAffinityPreset: soft
  nodeAffinityPreset:
    type: ""
    key: ""
    values: []
  affinity: {}
  nodeSelector: {}
  tolerations: []
  resources:
    limits:
      cpu: 200m
      memory: 512Mi
    requests:
      cpu: 100m
      memory: 256Mi 
```

## 0.3.1-rc.7
1.manageConfig现有配置项下增加CMDB资源同步与事件监听相关配置子项
```yaml
## job-manage作业管理配置
manageConfig:
  # CMDB资源（业务、业务集、主机等）同步与事件监听相关配置
  sync:
    app:
      # 是否开启业务同步
      enabled: true
    host:
      # 是否开启主机同步
      enabled: true
    resource:
      watch:
        # 是否开启业务、主机等事件监听
        enabled: true
    hostEvent:
      # 开启CMDB事件监听时用于处理主机事件的线程数量，一般情况下无须修改
      # 当环境中主机事件平均产生速率较高（>10/s）或主机信息相比于CMDB数据经常性明显滞后（分钟级）时，可增大该数值
      handlerNum: 3
```

## 0.3.0-rc.46

1.增加服务启动顺序控制相关配置

```yaml
## 服务下Pod等待依赖的其他服务Pod完成启动的init任务配置
waitForDependServices:
  image:
    # 镜像拉取仓库根地址
    registry: "hub.bktencent.com"
    # 镜像拉取仓库组织与镜像名称
    repository: "blueking/job-tools-k8s-startup-controller"
    # 镜像标签
    tag: "{{APP_VERSION}}"
    # 镜像拉取策略
    pullPolicy: IfNotPresent
  # 是否开启服务启动顺序控制，不开启则所有服务并行启动，默认不开启
  enabled: false
  # 服务间的依赖关系定义，多个依赖关系用逗号分隔
  # 例如：(A:B,C),(B:D)表示服务A必须在服务B与服务C启动完成后才启动，服务B必须在服务D启动完成后才启动
  # 全量服务名称：job-analysis,job-backup,job-crontab,job-execute,job-file-gateway,job-file-worker-headless,
  #            bk-job-gateway,job-logsvr,job-manage,bk-job-frontend
  # 说明：bk-job-gateway与bk-job-frontend为对其他产品暴露的服务，因此有bk-前缀，job-file-worker-headless为无头服务，因此有-headless后缀
  dependencies: (job-execute:job-manage,job-logsvr),(bk-job-frontend:job-analysis,job-backup,job-crontab,job-execute,job-file-gateway,bk-job-gateway,job-manage)
  # 依赖服务的Pod启动完成后需要拥有的label（label与value中请勿包含英文逗号/括号）
  expectPodLabels:
    # 所有依赖服务都必须拥有的label，多个label间用英文逗号分隔
    # 格式：label1=value1,label2=value2,...
    common: "bk.job.image/tag={{APP_VERSION}}"
    # 为每个依赖服务单独定义的必须拥有的label，多个服务间用英文括号及英文逗号分隔
    # 格式：(job-manage:label1=value1,label2=value2),(job-execute:label3=value3),(...)
    service: ""
  # 日志级别：默认INFO，可选DEBUG/WARN/ERROR
  logLevel: "INFO"
  # 资源要求与限制
  resources:
    limits:
      cpu: 1024m
      memory: 1Gi
    requests:
      cpu: 125m
      memory: 256Mi
```

## 0.3.0-rc.37
1.增加文件网关系统file-worker调度标签相关配置

```yaml
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
```yaml
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
```yaml
## job-file-worker文件源接入点配置
fileWorkerConfig:
  service:
    port: 19810
```

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
```yaml
# 文档中心 url
bkDocsCenterUrl: "https://bk.tencent.com/docs"
# 问题反馈 url
bkFeedBackUrl: "https://bk.tencent.com/s-mart/community"
```

## 0.2.2-rc.1
1.增加权限中心系统ID配置：
```yaml
## 权限中心配置
iam:
  # 作业平台注册到IAM的系统ID
  systemId: bk_job
```

## 0.1.53
1.增加CMDB供应商配置：
```yaml
## 对接蓝鲸CMDB参数配置
cmdb:
  # 供应商，默认为0
  supplierAccount: 0
```
2.增加各模块是否启用开关配置：
```yaml
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
```yaml
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
