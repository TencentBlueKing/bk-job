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

### 2.生成SM2加解密所需的秘钥对

#### 代码位置
sm2_keypair

#### 功能简介

生成SM2加解密所需的秘钥对，脚本json格式输出。用于作业平台后端values.yaml配置，对应配置项`job.encrypt.sm2PublicKey`、`job.encrypt.sm2PrivateKey`。

> python环境: python3.6

#### 执行

```shell
# 安装依赖
pip install bk-crypto-python-sdk
# 生成秘钥对
python generate_sm2_keypair.py
python generate_sm2_keypair.py --pretty
```

### 3.生成服务间调用 RSA 密钥（Helm values）

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

### 4.生成job-gateway开启HTTPS所需的证书（Helm values）

#### 代码位置
gateway-tls-cert

#### 功能简介

生成一套自签名证书材料并输出单行 JSON 到标准输出，用于填写 `values.yaml` 中的 `gatewayConfig.server.ssl.*` 共 4 个配置项。

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
