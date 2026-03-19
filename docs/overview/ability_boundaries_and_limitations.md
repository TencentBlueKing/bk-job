## 作业平台的能力边界与限制

#### 1. 不支持分发路径含有正则表达式特殊字符的文件（目前不支持）  
详情参考：https://github.com/TencentBlueKing/bk-job/issues/3449

#### 2. 在系统参数与环境变量使用上，与交互式Shell登录执行存在差异，无法保证执行结果完全一致
由于作业平台底层依赖的GSE Agent是机器上的一个**常驻进程**，其使用的系统参数（ulimit参数等）与环境变量是**Agent进程启动那一刻**读取到的数据，由于**操作系统限制**，Agent在重启前无法感知并热加载后续修改的系统参数与环境变量，因此会导致某些依赖系统参数或环境变量的任务执行时与交互式Shell登录执行存在差异。  
交互式Shell登录执行：使用用户当前能够加载到的系统参数与环境变量；  
GSE Agent执行：使用Agent启动时的用户当时能够加载到的环境变量；  
**使用上的建议：**    
如果你的脚本依赖系统参数或环境变量，请在脚本中显式指定，**不要先登录机器修改后再使用作业平台执行**。  
如果一定要先登录机器修改后再使用作业平台执行，请在修改后先通过节点管理或命令**重启GSE Agent使其生效**，重启Agent的命令由GSE维护，请参考GSE Agent操作相关运维文档，此处仅给出参考示例：  
```shell
// Linux机器
cd /usr/local/gse2/agent/bin && ./gsectl restart

// Windows机器
cd /cygdrive/c/gse2/agent/bin && ./gsectl.bat restart
或者
cd C:/gse2/agent/bin && ./gsectl.bat restart
# 不同环境下，GSE的安装路径可能不同，如果上述路径对不上，可先通过`ps -ef|grep gse`找出GSE Agent的安装位置，再使用对应路径下的控制程序进行操作。
```


