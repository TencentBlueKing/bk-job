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
