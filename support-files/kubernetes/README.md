# Kubernetes部署
## 概述
bk-job k8s构建工具

## 目录结构
```              
├── charts                 # helm charts目录
│    └── bk-job            # 业务相关helm charts目录
├── images                 # 业务镜像构建目录
│    ├── backend           # 微服务 Dockerfile
│    ├── init              # 初始化job Dockerfile
│    └── build.sh          # 业务镜像构建脚本
├── build.env              # 构建环境变量
└── README.md 
```

## 环境准备
- k8s集群和helm环境
- mongodb服务器
- MySQL
- RabbitMQ
- Redis

## 使用方式

1. 添加helm仓库
    ```shell
    $ helm repo add bkee <bk-job helm repo url>
    "bkee" has been added to your repositories
    ```

2. 确认访问helm仓库正常
    ```shell
    $ helm search repo bkee/bk-job
    NAME            	CHART VERSION	APP VERSION	DESCRIPTION
    bkee/bk-job     	1.0.0        	1.0.0      	BlueKing Job
    ```

3. 部署bk-job

    `config.yaml` 配置请参考[./charts/bk-job/values.yaml](./charts/bk-job/values.yaml)

    ```shell
    $ helm install bk-job bkee/bk-job --namespace=default -f config.yaml
    NAME: bk-job
    ...
    ```

## 构建镜像和charts包指引

1. 构建docker镜像
    ```shell script
    ./images/build.sh
    ```

2. 部署业务镜像
    ```shell script
    cd charts/bk-job
    # 根据需要修改values.yaml
    # helm3
    helm install bk-job . --namespace=bk-job
    ```
