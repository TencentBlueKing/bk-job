# BK-JOB

此Chart用于在Kubernetes集群中通过helm部署bk-job

## 软件构成
BK-JOB由11个微服务/独立程序构成

## 环境要求
- Kubernetes 1.12+
- Helm 3+
- PV provisioner

## 宿主机要求
注意：若完全使用默认参数，需要保证宿主机/data存在、可读写、且目录下有足够大的空间，chart将自动创建bkjob目录用于存放日志与临时文件，该目录在chart卸载后若无需保留则需要进行清理：
参考命令：
rm -r /data/bkjob

日志与临时文件存储说明：
作业平台使用Persistent Volume存储程序产生的日志、导入导出操作产生的临时文件、第三方源文件分发产生的临时文件，使用PVC进行声明，若K8s集群不提供合适的共享存储作为PV资源，则默认采用宿主机路径进行HostPath挂载，需要保证配置的路径在宿主机上存在且可读写；
默认路径为：/data/bkjob，其中bkjob层将自动创建，可通过values文件中的`persistence.localStorage.path`进行配置。

## 安装Chart
使用以下命令在命名空间bk-job中安装名称为`bk-job`的release, 其中`<bk-job helm repo url>`代表helm仓库地址:

```shell
$ helm repo add bkee <bk-job helm repo url>
$ helm install bk-job -n bk-job bkee/bk-job
```

上述命令将使用默认配置在Kubernetes集群中部署bk-job, 并输出访问指引。

## 卸载Chart
使用以下命令卸载`bk-job`:

```shell
$ helm uninstall bk-job -n bk-job
$ kubectl delete pvc -n bk-job --all
```

上述命令将移除所有和bk-job相关的Kubernetes组件，并删除release。

## Chart依赖
bitnami/common
bitnami/nginx-ingress-controller
bitnami/mariadb
bitnami/redis
bitnami/mongodb
bitnami/rabbitmq

## 配置说明
各项配置集中在仓库的一个values.yaml文件之中，下面展示了可配置的参数列表以及默认值
### Charts 全局配置
|参数|描述|默认值 |
|---|---|---|
| `global.imageRegistry`    | 全局镜像源                | `nil`   |
| `global.imagePullSecrets` | 全局镜像拉取密钥           | `[]`    |
| `global.storageClass`     | 全局动态卷分配使用的存储类   | `nil`   |

### 依赖的其他蓝鲸系统配置
|参数|描述|默认值 |
|---|---|---|
| `app.code`    | 调用ESB接口使用的app_code    | `bk_job`       |
| `app.secret` | 调用ESB接口使用的app_secret | ``  |
| `bkComponentApiUrl`  | ESB接口根地址   | `http://bkapi.paas.example.com`        |
| `bkLoginUrl`    | 统一登录根地址   | `http://paas.example.com/login/`        |
| `bkCmdbUrl`       | CMDB首页地址  | `http://cmdb.example.com`      |
| `bkIamApiUrl`   | 权限中心后台API根地址  | `http://bkiam-api.example.com`      |
| `bkRepoUrl`     | 制品库根地址  | `http://bkrepo.example.com`      |
| `bkNodemanUrl`  | 节点管理首页地址  | `http://nodeman.example.com`      |

### 依赖的GSE配置
|参数|描述|默认值 |
|---|---|---|
| `gse.cacheApiServer.host`     | GSE API接口根地址  | `gse-api.example.com`      |
| `gse.cacheApiServer.port`     | GSE API接口端口  | `59313`      |
| `gse.ssl.keystore.base64Content`   | GSE 接口SSL证书keystore文件单行base64编码值  | `见values文件`      |
| `gse.ssl.keystore.password`   | GSE 接口SSL证书keystore密码  | `2y#8VI2B4Sm9Dk^J`      |
| `gse.ssl.truststore.base64Content`   | GSE 接口SSL证书truststore文件单行base64编码值  | `见values文件`      |
| `gse.ssl.truststore.password`   | GSE 接口SSL证书truststore密码  | `2y#8VI2B4Sm9Dk^J`      |
| `gse.taskserver.host`   | GSE TASK接口根地址  | `gse-task.example.com`      |
| `gse.taskserver.port`   | GSE TASK接口端口  | `48673`      |
| `gse.server.zookeeper.connect.string`   | GSE Zookeeper根地址  | `gse-zk.example.com`      |

### 蓝鲸制品库相关配置
|参数|描述|默认值 |
|---|---|---|
| `artifactory.admin.username`  | 制品库管理员账号   | `admin`       |
| `artifactory.admin.password`  | 制品库管理员密码   | `blueking`       |
| `artifactory.job.username`  | 作业平台账号   | `bkjob`       |
| `artifactory.job.password`  | 作业平台密码   | `bkjob`       |
| `artifactory.job.project`  | 作业平台使用的项目   | `bkjob`       |

### 本地文件上传配置
|参数|描述|默认值 |
|---|---|---|
| `localFile.storageBackend` | 本地文件存储后端（local:本地，artifactory:蓝鲸制品库）| `artifactory`  |
| `localFile.artifactory.download.concurrency`  | 制品库单机下载并发数   | `10`       |
| `localFile.artifactory.repo`  | 作业平台存储本地上传文件使用的仓库   | `localupload`       |

### 作业平台公共配置
|参数|描述|默认值 |
|---|---|---|
| `job.security.privateKeyBase64`    | 服务间调用privateKey的Base64编码    | ``       |
| `job.security.publicKeyBase64`    | 服务间调用publicKey的Base64编码    | ``       |
| `job.security.actuator.user.name`    | actuator管理账号    | `actuator_name`       |
| `job.security.actuator.user.password`    | actuator管理密码    | `actuator_password`       |
| `job.encrypt.password`    | 加密DB密码/凭证的对称密钥    | `encrypt_password`       |
| `job.migration.iamModel.enabled`    | 是否开启权限模型migration    | `true`       |
| `job.migration.mysqlSchema.enabled`    | 是否开启Mysql数据库结构migration    | `true`       |
| `job.web.domain` | 主站域名（前端与Web API使用）    | `job.example.com`       |
| `job.web.apiDomain` | 暴露给其他上层系统（ESB、ApiGW）的API地址    | `api.job.example.com`       |
| `job.ingress.https.enabled` | 是否启用HTTPS    | `false`       |
| `job.ingress.https.certBase64` | 开启HTTPS时使用的证书base64编码    | ``       |
| `job.ingress.https.keyBase64` | 开启HTTPS时使用的证书私钥base64编码    | ``       |

### 持久化存储配置
|参数|描述|默认值 |
|---|---|---|
| `persistence.enabled`       | 是否开启持久化存储              | `true`           |
| `persistence.accessMode`    | 持久化存储模式                 | `ReadWriteOnce`  |
| `persistence.size`          | 持久化存储空间大小，默认200Gi    | `200Gi`          |

### 蓝鲸日志采集配置
|参数|描述|默认值 |
|---|---|---|
| `bkLogConfig.enabled`                   | 是否开启蓝鲸日志采集                     | `false`  |
| `bkLogConfig.file.serviceLog.dataId`    | 微服务日志采集到的dataId                 | ``       |
| `bkLogConfig.file.accessLog.dataId`     | job-gateway网关access日志采集到的dataId  | ``       |
| `bkLogConfig.std.dataId`                | 容器标准输出日志采集到的dataId             | ``       |

### 微服务网关Job-Gateway配置
|参数|描述|默认值 |
|---|---|---|
| `gatewayConfig.loginExemption.enabled`    | 是否开启登录豁免    | `false`       |
| `gatewayConfig.loginExemption.defaultUser` | 登录豁免下的默认用户    | `admin`       |
| `gatewayConfig.replicaCount` | 服务实例数量    | `1`       |
| `gatewayConfig.autoscaling.enabled` | 是否开启自动扩缩容    | `false`       |
| `gatewayConfig.autoscaling.minReplicas` | 自动扩缩容最小副本数    | `1`       |
| `gatewayConfig.autoscaling.maxReplicas` | 自动扩缩容最大副本数    | `5`       |
| `gatewayConfig.autoscaling.targetCPU` | 自动扩缩容目标CPU百分比    | `50`       |
| `gatewayConfig.autoscaling.targetMemory` | 自动扩缩容目标内存百分比    | `50`       |
| `gatewayConfig.ingress.annotations.nginx.ingress.kubernetes.io/proxy-body-size` | 最大请求体限制    | `10240m`       |
| `gatewayConfig.server.ssl.keystore.base64Content` | job-gateway开启https时使用的p12证书keystore单行base64编码内容    | `见values文件`       |
| `gatewayConfig.server.ssl.keystore.password` | job-gateway开启https时使用的keystore的密码    | `mLnuob1**4D74c@F`       |
| `gatewayConfig.server.ssl.truststore.base64Content` | job-gateway开启https时使用的p12证书truststore单行base64编码内容    | `见values文件`       |
| `gatewayConfig.server.ssl.truststore.password` | job-gateway开启https时使用的truststore的密码    | `mLnuob1**4D74c@F`       |
| `gatewayConfig.jvmOptions` | 运行时JVM参数    | `-Dreactor.netty.http.server.accessLogEnabled=true -Xms256m -Xmx256m -XX:NewRatio=1 -XX:SurvivorRatio=8 -XX:+UseG1GC`       |

### Job-Manage配置
|参数|描述|默认值 |
|---|---|---|
| `manageConfig.replicaCount` | 服务实例数量    | `1`       |
| `manageConfig.autoscaling.enabled` | 是否开启自动扩缩容    | `false`       |
| `manageConfig.autoscaling.minReplicas` | 自动扩缩容最小副本数    | `1`       |
| `manageConfig.autoscaling.maxReplicas` | 自动扩缩容最大副本数    | `5`       |
| `manageConfig.autoscaling.targetCPU` | 自动扩缩容目标CPU百分比    | `50`       |
| `manageConfig.autoscaling.targetMemory` | 自动扩缩容目标内存百分比    | `50`       |
| `manageConfig.jvmOptions` | 运行时JVM参数    | `-Xms256m -Xmx256m -XX:NewRatio=1 -XX:SurvivorRatio=8`       |

### Job-Execute配置
|参数|描述|默认值 |
|---|---|---|
| `executeConfig.replicaCount` | 服务实例数量    | `1`       |
| `executeConfig.autoscaling.enabled` | 是否开启自动扩缩容    | `false`       |
| `executeConfig.autoscaling.minReplicas` | 自动扩缩容最小副本数    | `1`       |
| `executeConfig.autoscaling.maxReplicas` | 自动扩缩容最大副本数    | `5`       |
| `executeConfig.autoscaling.targetCPU` | 自动扩缩容目标CPU百分比    | `50`       |
| `executeConfig.autoscaling.targetMemory` | 自动扩缩容目标内存百分比    | `50`       |
| `executeConfig.jvmOptions` | 运行时JVM参数    | `-Xms512m -Xmx512m -XX:NewRatio=1 -XX:SurvivorRatio=8`       |

### Job-Crontab配置
|参数|描述|默认值 |
|---|---|---|
| `crontabConfig.enabled` | 是否部署定时任务微服务    | `true`       |
| `crontabConfig.replicaCount` | 服务实例数量    | `1`       |
| `crontabConfig.autoscaling.enabled` | 是否开启自动扩缩容    | `false`       |
| `crontabConfig.autoscaling.minReplicas` | 自动扩缩容最小副本数    | `1`       |
| `crontabConfig.autoscaling.maxReplicas` | 自动扩缩容最大副本数    | `5`       |
| `crontabConfig.autoscaling.targetCPU` | 自动扩缩容目标CPU百分比    | `50`       |
| `crontabConfig.autoscaling.targetMemory` | 自动扩缩容目标内存百分比    | `50`       |
| `crontabConfig.jvmOptions` | 运行时JVM参数    | `-Xms256m -Xmx256m -XX:NewRatio=1 -XX:SurvivorRatio=8`       |

### Job-Logsvr配置
|参数|描述|默认值 |
|---|---|---|
| `logsvrConfig.replicaCount` | 服务实例数量    | `1`       |
| `logsvrConfig.autoscaling.enabled` | 是否开启自动扩缩容    | `false`       |
| `logsvrConfig.autoscaling.minReplicas` | 自动扩缩容最小副本数    | `1`       |
| `logsvrConfig.autoscaling.maxReplicas` | 自动扩缩容最大副本数    | `5`       |
| `logsvrConfig.autoscaling.targetCPU` | 自动扩缩容目标CPU百分比    | `50`       |
| `logsvrConfig.autoscaling.targetMemory` | 自动扩缩容目标内存百分比    | `50`       |
| `logsvrConfig.jvmOptions` | 运行时JVM参数    | `-Xms256m -Xmx256m -XX:NewRatio=1 -XX:SurvivorRatio=8`       |

### Job-Backup配置
|参数|描述|默认值 |
|---|---|---|
| `backupConfig.replicaCount` | 服务实例数量    | `1`       |
| `backupConfig.autoscaling.enabled` | 是否开启自动扩缩容    | `false`       |
| `backupConfig.autoscaling.minReplicas` | 自动扩缩容最小副本数    | `1`       |
| `backupConfig.autoscaling.maxReplicas` | 自动扩缩容最大副本数    | `5`       |
| `backupConfig.autoscaling.targetCPU` | 自动扩缩容目标CPU百分比    | `50`       |
| `backupConfig.autoscaling.targetMemory` | 自动扩缩容目标内存百分比    | `50`       |
| `backupConfig.jvmOptions` | 运行时JVM参数    | `-Xms256m -Xmx256m -XX:NewRatio=1 -XX:SurvivorRatio=8`       |

### Job-Analysis配置
|参数|描述|默认值 |
|---|---|---|
| `analysisConfig.replicaCount` | 服务实例数量    | `1`       |
| `analysisConfig.autoscaling.enabled` | 是否开启自动扩缩容    | `false`       |
| `analysisConfig.autoscaling.minReplicas` | 自动扩缩容最小副本数    | `1`       |
| `analysisConfig.autoscaling.maxReplicas` | 自动扩缩容最大副本数    | `5`       |
| `analysisConfig.autoscaling.targetCPU` | 自动扩缩容目标CPU百分比    | `50`       |
| `analysisConfig.autoscaling.targetMemory` | 自动扩缩容目标内存百分比    | `50`       |
| `analysisConfig.jvmOptions` | 运行时JVM参数    | `-Xms256m -Xmx256m -XX:NewRatio=1 -XX:SurvivorRatio=8`       |

### Job-File-Gateway配置
|参数|描述|默认值 |
|---|---|---|
| `fileGatewayConfig.replicaCount` | 服务实例数量    | `1`       |
| `fileGatewayConfig.autoscaling.enabled` | 是否开启自动扩缩容    | `false`       |
| `fileGatewayConfig.autoscaling.minReplicas` | 自动扩缩容最小副本数    | `1`       |
| `fileGatewayConfig.autoscaling.maxReplicas` | 自动扩缩容最大副本数    | `5`       |
| `fileGatewayConfig.autoscaling.targetCPU` | 自动扩缩容目标CPU百分比    | `50`       |
| `fileGatewayConfig.autoscaling.targetMemory` | 自动扩缩容目标内存百分比    | `50`       |
| `fileGatewayConfig.jvmOptions` | 运行时JVM参数    | `-Xms256m -Xmx256m -XX:NewRatio=1 -XX:SurvivorRatio=8`       |

### Job-File-Worker配置
|参数|描述|默认值 |
|---|---|---|
| `fileWorkerConfig.instanceName` | 实例名称    | `job-file-worker-public-1`       |
| `fileWorkerConfig.token` | 实例凭据    | `testToken`       |
| `fileWorkerConfig.downloadFile.maxSizeGB` | 最大占用空间（GB），超过则进行清理    | `100`       |
| `fileWorkerConfig.downloadFile.expireDays` | 临时文件过期清理时间（天）    | `7`       |
| `fileWorkerConfig.jvmOptions` | 运行时JVM参数    | `-Xms256m -Xmx256m -XX:NewRatio=1 -XX:SurvivorRatio=8`       |

### Job-Frontend配置
|参数|描述|默认值 |
|---|---|---|
| `frontendConfig.ingress.annotations.nginx.ingress.kubernetes.io/proxy-body-size` | 前端资源最大请求体大小    | `2048m`       |

### Job-Migration配置
暂无

## 常见问题
**1. 首次启动时间过长，且READY状态为`0/1`？**

答: BK-JOB的启动包含以下流程：等待MySQL/MariaDB准备完成->执行SQL Migration->执行权限模型Migration->启动job-manage、job-execute等依赖数据库的微服务->执行数据初始化->处理访问请求，Migration过程可能耗时较长，可通过kubectl logs -f查看initContainer实时日志观测进度，一般情况下只要未报出Error，程序就在正常运行，耐心等待即可。

**2. 如何查看日志？**

答: 有两种方式可以查看日志: 1. kubectl logs pod 查看实时日志  2.日志保存在/data/logs/job目录下，可以进入容器内查看
