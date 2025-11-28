# k8s-startup-controller 模块文档

## 概述

k8s-startup-controller是蓝鲸作业平台（BK-JOB）中的一个关键组件，负责在Kubernetes环境中更新应用版本时实现各服务之间的有序更新。

## 应用场景
K8S应用中包含ServiceA与ServiceB两个服务，其中ServiceB调用ServiceA，在从1.0版本升级到2.0版本时，ServiceA新增了一个接口，且ServiceB会调用该新接口，在各服务均存在多个副本的环境下，如果两个服务对应的Pod同时更新，则在更新过程中存在2.0版本的ServiceB调用到1.0版本的ServiceA的情况，结果会导致调用失败，因为旧版本的ServiceA的Pod还没有新增的接口。
k8s-startup-controller正是为解决该问题而生，通过灵活控制Init-Container中的等待时间，来实现各服务Pod的有序更新。

## 功能特性
- **支持多组Service间的依赖指定**
通过依赖表达式解析出多组服务之间的依赖，无依赖关系的服务可以并行更新，在确保依赖关系满足的同时最大限度提高更新效率。

- **支持为服务指定外部依赖（当前支持：HTTP）**
只有检测到外部依赖满足时，才认为当前服务已经Ready，更新过程才继续往后进行。

## 工作原理
k8s-startup-controller在K8S Pod的Init-Container中运行，在服务主容器启动前在Init-Container中的Java进程通过K8S接口不断轮询依赖服务对应Pod状态，检查依赖是否满足，直到满足后才启动当前服务主容器。 
使用了k8s-startup-controller的Pod启动流程如下：
1. **Pod启动**：Kubernetes创建Pod；
2. **Init-Container执行**：k8s-startup-controller开始检查依赖服务；
3. **依赖检查**：根据配置的依赖关系，检查依赖服务Pod是否就绪；
4. **标签匹配**：验证依赖服务Pod是否拥有期望的标签；
5. **外部依赖检查**：根据配置检查当前服务的外部依赖是否就绪；
6. **启动主容器**：所有依赖就绪后，Init-Container执行完成，启动主服务容器。

## 使用方式
在Deployment/StatefulSet中配置Init-Container，指定k8s-startup-controller的镜像与环境变量。

#### 镜像信息
- **镜像地址**：`hub.bktencent.com/blueking/job-tools-k8s-startup-controller`
- **镜像版本**：`3.11.12-beta.6`（与作业平台版本一致）

### 环境变量配置
```yaml
# Init-Container环境变量配置示例
env:
  # Kubernetes命名空间
  - name: KUBERNETES_NAMESPACE
    value: {{ .Release.Namespace }}
  
  # 当前服务名称
  - name: BK_JOB_CURRENT_SERVICE_NAME
    value: {{ .currentServiceName }}

  # 服务间的依赖关系定义，多个依赖关系用逗号分隔
  # 例如：(A:B,C),(B:D)表示服务A必须在服务B与服务C启动完成后才启动，服务B必须在服务D启动完成后才启动
  # 说明：A/B/C/D均为Service名称，无头服务需要额外增加-headless后缀
  - name: BK_JOB_STARTUP_DEPENDENCIES_STR
    value: (job-execute:job-manage,job-logsvr),(bk-job-frontend:job-analysis,job-backup,job-crontab,job-execute,job-file-gateway,bk-job-gateway,job-manage)
  
  # 日志级别：默认INFO，可选DEBUG/WARN/ERROR
  - name: BK_JOB_LOG_LEVEL
    value: "INFO"
  
  # 依赖服务的Pod启动完成后需要拥有的label，所有依赖服务都必须拥有的label，多个label间用英文逗号分隔
  # 格式：label1=value1,label2=value2,...
  - name: BK_JOB_EXPECT_POD_LABELS_COMMON
    value: "bk.job.image/tag=2.0.0"
  
  # 为每个依赖服务单独定义的必须拥有的label，多个服务间用英文括号及英文逗号分隔
  # 格式：(job-manage:label1=value1,label2=value2),(job-execute:label3=value3),(...)
  - name: BK_JOB_EXPECT_POD_LABELS_SERVICE
    value: ""
  
  # 是否开启外部依赖检查
  - name: BK_JOB_EXTERNAL_DEPENDENCY_CHECK_ENABLED
    value: "false"

  # 检查地址（该地址提供的服务需要接收并处理Http Get请求的Query参数：namespace=${namespace}&serviceName=${serviceName}），
  # 返回值格式：
  # 已准备就绪：{"ready": true}
  # 未准备就绪：{"ready": false}
  - name: BK_JOB_EXTERNAL_DEPENDENCY_CHECK_URL
    value: "http://example.com/checkServiceDependency"
```

### 实际部署示例
#### job-execute服务的完整配置

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: job-execute
  labels:
    app: job-execute
spec:
  replicas: 2
  selector:
    matchLabels:
      app: job-execute
  template:
    metadata:
      labels:
        app: job-execute
        bk.job.image/tag: 2.0.0
    spec:
      # Init-Container配置
      initContainers:
      - name: wait-for-depend-services
        image: hub.bktencent.com/blueking/job-tools-k8s-startup-controller:2.0.0
        imagePullPolicy: IfNotPresent
        env:
        - name: KUBERNETES_NAMESPACE
          valueFrom:
            fieldRef:
              fieldPath: metadata.namespace
        - name: BK_JOB_CURRENT_SERVICE_NAME
          value: "job-execute"
        - name: BK_JOB_STARTUP_DEPENDENCIES_STR
          value: "(job-execute:job-manage,job-logsvr),(bk-job-frontend:job-analysis,job-backup,job-crontab,job-execute,job-file-gateway,bk-job-gateway,job-manage)"
        - name: BK_JOB_LOG_LEVEL
          value: "INFO"
        - name: BK_JOB_EXPECT_POD_LABELS_COMMON
          value: "bk.job.image/tag=2.0.0"
        - name: BK_JOB_EXPECT_POD_LABELS_SERVICE
          value: ""
        - name: BK_JOB_EXTERNAL_DEPENDENCY_CHECK_ENABLED
          value: "false"
        resources:
          limits:
            cpu: 500m
            memory: 1Gi
          requests:
            cpu: 125m
            memory: 256Mi
      
      # 主容器配置
      containers:
      - name: job-execute
        # ... 其他主容器配置
```
