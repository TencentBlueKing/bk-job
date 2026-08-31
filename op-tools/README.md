## 作业平台（Job）诊断运维工具集


### 1.批量调用用户态接口

#### 代码位置  
batch-invoke

#### 功能简介  
遍历输入数据，通过多线程快速并发调用同一个接口多次  

> 运行环境要求：python3

> 输入：  
> 数据集合：execute_data_1.txt,execute_data_2.txt,...,execute_data_n.txt，每个文件每行一个数据  
> 执行模板：execute.sh，脚本内可引用由数据集合传入的变量：${1},${2},...,${n}
> 
  
> 执行：python3 run.py {并发数}  
 
> 输出：output.json，输出有序数组，每个对象含调用参数与结果


#### 适用场景   
- 强制终止一批异常作业
- 批量开启/关闭一批定时任务

### 2.单接口压测

#### 代码位置

api_stress_testing

#### 功能简介

给定一个BKAPIGW上的接口，并发调用以测试其qps，观察接口性能。

> python环境: python3.6

#### 执行

```shell
python request_api.py \
--app_code={appCode} \
--app_secret={appSecret} \
--username={用户名} \
--url={接口url} \
--process_cnt={进程数} \
--concurrent_cnt={单个进程内并发数}
```

#### 输出

report.md: 统计了 平均响应耗时、最大响应耗时、最小响应耗时、错误率、所有请求发送时间 的表格


### 3.作业平台OP系统

#### 代码位置
bk-job-op

#### 功能简介
该系统为基于SpringBoot搭建的一套作业平台OP系统，承担系统管控、日志分析、辅助运维等功能，不断向自动化、智能化的方向迭代。

> 运行环境要求：JDK 17

> 构件出可执行Jar包，指定外部配置文件，运行：  
> cd bk-job-op/
> ./gradlew clean build
> java -Dspring.config.additional-location=file:/xxx/application.yml -jar build/libs/bk-job-op-0.0.1-SNAPSHOT.jar

> 在浏览器访问OP系统接口：
> http://127.0.0.1:8080/checkServiceDependency?namespace=blueking&serviceName=bk-job-gateway

### 4.生成SM2加解密所需的秘钥对

#### 代码位置
sm2_keypair

#### 功能简介

用于生成SM2加解密所需的秘钥对，生成的原始秘钥可以直接用于后端SM2Util工具，生成的PEM秘钥可以直接用于前端vue。

> python环境: python3.6

#### 执行

```shell
# 安装依赖
pip install bk-crypto-python-sdk
# 生成秘钥对
python generate_sm2_keypair.py
```

### 5.生成服务间调用 RSA 密钥（Helm values）

#### 代码位置
service-rsa-keypair

#### 功能简介

生成 `job.security.privateKeyBase64` / `job.security.publicKeyBase64` 所需的 PEM，并按作业平台 Java 侧 `RSAUtils` 约定对整段 PEM 再做 Base64，输出单行 JSON 到标准输出（便于复制进 `values.yaml` 或管道给自动化脚本）。

> python 环境: python3.6+

#### 执行

```shell
pip install cryptography
cd service-rsa-keypair
python generate_service_rsa_keys.py
python generate_service_rsa_keys.py --bits 2048 --pretty
```

### 6.生成job-gateway开启HTTPS所需的证书（Helm values）

#### 代码位置
gateway-tls-cert

#### 功能简介

生成一套自签名证书材料并输出单行 JSON 到标准输出，用于填写 `values.yaml` 中的 `gatewayConfig.server.ssl.*` 这 4 个配置项。

输出的 4 个值分别为：

- `gatewayConfig.server.ssl.p12.base64Content`：PKCS12 格式 keystore 的单行 base64 编码内容，含服务端私钥、服务端证书与自签名 CA 证书
- `gatewayConfig.server.ssl.keystore.password`：上述 keystore 的口令，默认随机生成
- `gatewayConfig.server.ssl.truststore.base64Content`：JKS 格式 truststore 的单行 base64 编码内容，含上述自签名 CA 证书，条目别名为 `ca`
- `gatewayConfig.server.ssl.truststore.password`：上述 truststore 的口令，默认随机生成

服务端证书默认签发给主机名 `bk-job-gateway` 与 `localhost`，若客户端会校验主机名，需通过 `--san` 指定实际访问的域名或 IP。

> python 环境: python3.6+

#### 执行

```shell
pip install cryptography
cd gateway-tls-cert
# 生成证书，口令随机生成
python generate_gateway_tls_cert.py --pretty
# 指定实际访问的域名与 IP
python generate_gateway_tls_cert.py --san bk-job-gateway --san 127.0.0.1 --days 3650
```
