# BK-JOB

此Chart用于在Kubernetes集群中通过helm部署bk-job

## 环境要求
- Kubernetes 1.12+
- Helm 3+
- PV provisioner

## 安装Chart
使用以下命令安装名称为`bk-job`的release, 其中`<bk-job helm repo url>`代表helm仓库地址:

```shell
$ helm repo add bkee <bk-job helm repo url>
$ helm install bk-job bkee/bk-job
```

上述命令将使用默认配置在Kubernetes集群中部署bk-job, 并输出访问指引。

## 卸载Chart
使用以下命令卸载`bk-job`:

```shell
$ helm uninstall bk-job
```

上述命令将移除所有和bk-job相关的Kubernetes组件，并删除release。
