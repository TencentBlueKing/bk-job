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

用于生成SM2加解密所需的秘钥对，生成的原始秘钥可以直接用于后端SM2Util工具，生成的PEM秘钥可以直接用于前端vue。

> python环境: python3.6

#### 执行

```shell
# 安装依赖
pip install bk-crypto-python-sdk
# 生成秘钥对
python generate_sm2_keypair.py
```
