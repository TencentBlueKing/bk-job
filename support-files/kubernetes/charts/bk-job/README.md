# BK-JOB

此Chart用于在Kubernetes集群中通过helm部署bk-job

## 软件构成
BK-JOB由11个微服务/独立程序构成

## 环境要求
- Kubernetes 1.12+
- Helm 3+
- PV provisioner

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
| `global.imageRegistry`    | Global Docker image registry                    | `nil`                                                   |
| `global.imagePullSecrets` | Global Docker registry secret names as an array | `[]` (does not add image pull secrets to deployed pods) |
| `global.storageClass`     | Global storage class for dynamic provisioning   | `nil`                                                   |
| `nameOverride`     | Global storage class for dynamic provisioning   | `nil`                                                   |
### 镜像拉取策略

### 依赖的其他周边系统配置
|参数|描述|默认值 |
|---|---|---|
| `app.code`    | 调用ESB接口使用的app_code    | `bk_job`       |
| `app.secret` | 调用ESB接口使用的app_secret | ``  |
| `esb.service.url`  | ESB接口根地址   | `http://paas.example.com`        |
| `gse.cacheApiServer.host`     | GSE API接口根地址  | `gse-api.example.com`      |
| `gse.cacheApiServer.port`     | GSE API接口端口  | `59313`      |
| `gse.ssl.keystore.password`   | GSE 接口SSL证书keystore密码  | ``      |
| `gse.ssl.truststore.password`   | GSE 接口SSL证书truststore密码  | ``      |
| `gse.taskserver.host`   | GSE TASK接口根地址  | `gse-task.example.com`      |
| `gse.taskserver.port`   | GSE TASK接口端口  | `48673`      |
| `gse.server.zookeeper.connect.string`   | GSE Zookeeper根地址  | `gse-zk.example.com`      |
| `iam.baseUrl`   | 权限中心后台根地址  | `http://bkiam.example.com`      |
| `paas.login.url`   | PaaS登录地址  | `http://paas.example.com/login`      |
| `cmdb.server.url`   | CMDB首页地址  | `http://cmdb.example.com`      |
| `nodeman.server.url`   | 节点管理首页地址  | `http://nodeman.example.com`      |

### 本地文件上传配置
|参数|描述|默认值 |
|---|---|---|
| `localFile.storageBackend`    | 本地文件存储后端（local:本地，artifactory:蓝鲸制品库）    | `artifactory`       |
| `localFile.artifactory.baseUrl`    | 制品库根地址    | `http://bkrepo.example.com`       |
| `localFile.artifactory.download.concurrency`  | 制品库单机下载并发数   | `10`       |
| `localFile.artifactory.admin.username`  | 制品库管理员账号   | `admin`       |
| `localFile.artifactory.admin.password`  | 制品库管理员密码   | `blueking`       |
| `localFile.artifactory.job.username`  | 作业平台账号   | `bkjob`       |
| `localFile.artifactory.job.password`  | 作业平台密码   | `bkjob`       |
| `localFile.artifactory.job.project`  | 作业平台使用的项目   | `bkjob`       |
| `localFile.artifactory.repo.localUpload`  | 作业平台存储本地上传文件使用的仓库   | `localupload`       |

### 作业平台公共配置
|参数|描述|默认值 |
|---|---|---|
| `job.security.privateKeyBase64`    | 服务间调用privateKey的Base64编码    | ``       |
| `job.security.publicKeyBase64`    | 服务间调用publicKey的Base64编码    | ``       |
| `job.security.actuator.user.name`    | actuator管理账号    | `actuator_name`       |
| `job.security.actuator.user.password`    | actuator管理密码    | `actuator_password`       |
| `job.encrypt.password`    | 加密DB密码/凭证的对称密钥    | `encrypt_password`       |
| `job.storage.rootPath`    | 本地文件下载暂存路径    | `/data/job/storage/local`       |

### 微服务网关Job-Gateway配置
|参数|描述|默认值 |
|---|---|---|
| `gatewayConfig.loginExemption.enable`    | 是否开启登录豁免    | `false`       |
| `gatewayConfig.loginExemption.defaultUser` | 登录豁免下的默认用户    | `admin`       |
| `gatewayConfig.replicaCount` | 服务实例数量    | `1`       |
| `gatewayConfig.autoscaling.enabled` | 是否开启自动扩缩容    | `false`       |
| `gatewayConfig.autoscaling.minReplicas` | 自动扩缩容最小副本数    | `1`       |
| `gatewayConfig.autoscaling.maxReplicas` | 自动扩缩容最大副本数    | `5`       |
| `gatewayConfig.autoscaling.targetCPU` | 自动扩缩容目标CPU百分比    | `50`       |
| `gatewayConfig.autoscaling.targetMemory` | 自动扩缩容目标内存百分比    | `50`       |
| `gatewayConfig.ingress.hostname` | 暴露给前端的API地址    | `api.job.example.com`       |
| `gatewayConfig.ingress.annotations.nginx.ingress.kubernetes.io/proxy-body-size` | 最大请求体限制    | `10240m`       |

### Job-Manage配置
|参数|描述|默认值 |
|---|---|---|
| `manageConfig.replicaCount` | 服务实例数量    | `1`       |
| `manageConfig.autoscaling.enabled` | 是否开启自动扩缩容    | `false`       |
| `manageConfig.autoscaling.minReplicas` | 自动扩缩容最小副本数    | `1`       |
| `manageConfig.autoscaling.maxReplicas` | 自动扩缩容最大副本数    | `5`       |
| `manageConfig.autoscaling.targetCPU` | 自动扩缩容目标CPU百分比    | `50`       |
| `manageConfig.autoscaling.targetMemory` | 自动扩缩容目标内存百分比    | `50`       |

### Job-Execute配置
|参数|描述|默认值 |
|---|---|---|
| `executeConfig.replicaCount` | 服务实例数量    | `1`       |
| `executeConfig.autoscaling.enabled` | 是否开启自动扩缩容    | `false`       |
| `executeConfig.autoscaling.minReplicas` | 自动扩缩容最小副本数    | `1`       |
| `executeConfig.autoscaling.maxReplicas` | 自动扩缩容最大副本数    | `5`       |
| `executeConfig.autoscaling.targetCPU` | 自动扩缩容目标CPU百分比    | `50`       |
| `executeConfig.autoscaling.targetMemory` | 自动扩缩容目标内存百分比    | `50`       |

### Job-Crontab配置
|参数|描述|默认值 |
|---|---|---|
| `crontabConfig.replicaCount` | 服务实例数量    | `1`       |
| `crontabConfig.autoscaling.enabled` | 是否开启自动扩缩容    | `false`       |
| `crontabConfig.autoscaling.minReplicas` | 自动扩缩容最小副本数    | `1`       |
| `crontabConfig.autoscaling.maxReplicas` | 自动扩缩容最大副本数    | `5`       |
| `crontabConfig.autoscaling.targetCPU` | 自动扩缩容目标CPU百分比    | `50`       |
| `crontabConfig.autoscaling.targetMemory` | 自动扩缩容目标内存百分比    | `50`       |

### Job-Logsvr配置
|参数|描述|默认值 |
|---|---|---|
| `logsvrConfig.replicaCount` | 服务实例数量    | `1`       |
| `logsvrConfig.autoscaling.enabled` | 是否开启自动扩缩容    | `false`       |
| `logsvrConfig.autoscaling.minReplicas` | 自动扩缩容最小副本数    | `1`       |
| `logsvrConfig.autoscaling.maxReplicas` | 自动扩缩容最大副本数    | `5`       |
| `logsvrConfig.autoscaling.targetCPU` | 自动扩缩容目标CPU百分比    | `50`       |
| `logsvrConfig.autoscaling.targetMemory` | 自动扩缩容目标内存百分比    | `50`       |

### Job-Backup配置
|参数|描述|默认值 |
|---|---|---|
| `backupConfig.replicaCount` | 服务实例数量    | `1`       |
| `backupConfig.autoscaling.enabled` | 是否开启自动扩缩容    | `false`       |
| `backupConfig.autoscaling.minReplicas` | 自动扩缩容最小副本数    | `1`       |
| `backupConfig.autoscaling.maxReplicas` | 自动扩缩容最大副本数    | `5`       |
| `backupConfig.autoscaling.targetCPU` | 自动扩缩容目标CPU百分比    | `50`       |
| `backupConfig.autoscaling.targetMemory` | 自动扩缩容目标内存百分比    | `50`       |

### Job-Analysis配置
|参数|描述|默认值 |
|---|---|---|
| `analysisConfig.replicaCount` | 服务实例数量    | `1`       |
| `analysisConfig.autoscaling.enabled` | 是否开启自动扩缩容    | `false`       |
| `analysisConfig.autoscaling.minReplicas` | 自动扩缩容最小副本数    | `1`       |
| `analysisConfig.autoscaling.maxReplicas` | 自动扩缩容最大副本数    | `5`       |
| `analysisConfig.autoscaling.targetCPU` | 自动扩缩容目标CPU百分比    | `50`       |
| `analysisConfig.autoscaling.targetMemory` | 自动扩缩容目标内存百分比    | `50`       |

### Job-File-Gateway配置
|参数|描述|默认值 |
|---|---|---|
| `fileGatewayConfig.replicaCount` | 服务实例数量    | `1`       |
| `fileGatewayConfig.autoscaling.enabled` | 是否开启自动扩缩容    | `false`       |
| `fileGatewayConfig.autoscaling.minReplicas` | 自动扩缩容最小副本数    | `1`       |
| `fileGatewayConfig.autoscaling.maxReplicas` | 自动扩缩容最大副本数    | `5`       |
| `fileGatewayConfig.autoscaling.targetCPU` | 自动扩缩容目标CPU百分比    | `50`       |
| `fileGatewayConfig.autoscaling.targetMemory` | 自动扩缩容目标内存百分比    | `50`       |

### Job-File-Worker配置
|参数|描述|默认值 |
|---|---|---|
| `fileWorkerConfig.instanceName` | 实例名称    | `job-file-worker-public-1`       |
| `fileWorkerConfig.token` | 实例凭据    | `testToken`       |
| `fileWorkerConfig.downloadFile.dir` | 文件下载根路径    | `/tmp/job`       |
| `fileWorkerConfig.downloadFile.expireDays` | 临时文件过期清理时间（天）    | `7`       |

### Job-Frontend配置
|参数|描述|默认值 |
|---|---|---|
| `frontendConfig.web.domain` | 前端主站域名    | `job.example.com`       |
| `frontendConfig.backend.apiGateway.domain` | 对接的后台网关域名 | `api.job.example.com`       |
| `frontendConfig.ingress.annotations.nginx.ingress.kubernetes.io/proxy-body-size` | 前端资源最大请求体大小    | `2048m`       |

### Job-Migration配置
暂无

## 常见问题
**1. 首次启动时间过长，且READY状态为`0/1`？**

答: BK-JOB的启动包含以下流程：等待MySQL/MariaDB准备完成->执行SQL Migration->执行权限模型Migration->启动job-manage、job-execute等依赖数据库的微服务->执行数据初始化->处理访问请求，Migration过程可能耗时较长，可通过kubectl logs -f查看initContainer实时日志观测进度，一般情况下只要未报出Error，程序就在正常运行，耐心等待即可。

**2. 如何查看日志？**

答: 有两种方式可以查看日志: 1. kubectl logs pod 查看实时日志  2.日志保存在/data/logs/job目录下，可以进入容器内查看
