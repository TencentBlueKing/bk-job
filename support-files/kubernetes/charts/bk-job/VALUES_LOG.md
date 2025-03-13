# chart values 更新日志
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
