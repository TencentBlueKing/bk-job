# 升级说明

[English](UPGRADE.en.md) | 简体中文  

## 一、升级工具介绍与用法
### 1.介绍  
从3.3.4版本开始，包内开始提供一个升级工具（upgrader-${version}.jar，与其他二进制包在同一目录），用于在大版本之间升级时（例如：3.2.x->3.3.x, 3.3.x->3.4.x等）对不兼容的数据进行迁移，每次进行大版本升级时，**必须查阅此文档**，查看是否有需要执行的升级任务，若有则需要按照说明执行，否则可能出现数据异常、权限丢失等问题。

说明：同一大版本的多个rc/release版本之间升级（例如：3.7.3->3.7.4-rc.8）无需执行。

### 2.用法  
#### （1）二进制环境  

使用命令`java -Dfile.encoding=utf8 -Djob.log.dir=path/to/log/dir -Dconfig.file=/path/to/config/file -jar upgrader-[x.x.x.x].jar [fromVersion] [toVersion] [executeTime]` 启动工具，再根据命令行提示输入与具体版本升级任务相关的特定参数，运行升级工具。  

**/path/to/log/dir** 用于指定工具日志保存路径，通常为${BK_HOME}/logs/job  
**/path/to/config/file** 用于指定工具读取的配置文件，该配置文件由部署脚本自动渲染生成，路径为${BK_HOME}/etc/job/upgrader/upgrader.properties  
**fromVersion** 为升级前的作业平台版本，如3.2.7.3   
**toVersion** 为升级后的作业平台版本，如3.3.4.0  
**executeTime** 为升级任务执行的时间点，取值为BEFORE_UPDATE_JOB、AFTER_UPDATE_JOB，在更新作业平台进程前执行本工具填写BEFORE_UPDATE_JOB，更新进程后执行则填写AFTER_UPDATE_JOB  

示例：  
Job进程更新前：    
```shell script
/opt/java/bin/java -Dfile.encoding=utf8 -Djob.log.dir=/data/bkee/logs/job -Dconfig.file=/data/bkee/etc/job/upgrader/upgrader.properties -jar upgrader-3.3.4.0.jar 3.2.7.3 3.3.4.0 BEFORE_UPDATE_JOB  
```
Job进程更新后：  
```shell script
/opt/java/bin/java -Dfile.encoding=utf8 -Djob.log.dir=/data/bkee/logs/job -Dconfig.file=/data/bkee/etc/job/upgrader/upgrader.properties -jar upgrader-3.3.4.0.jar 3.2.7.3 3.3.4.0 AFTER_UPDATE_JOB
```

#### （2）容器化环境
1. 从对应版本的二进制包内获取upgrader二进制jar包与配置模板文件；
2. 选定一台机器（可选中控机），准备Java运行环境（JDK 1.8），作为运行迁移工具的机器；
3. 查看配置模板文件，从部署Job的K8s集群中将需要访问的服务（job-manage/job-crontab等）端口使用port-forward等方式映射到有固定IP的机器（能操作K8s集群的机器，可以为中控机）端口（可自行分配）上；
4. 根据配置模板文件与环境变量对应的实际数据填写配置文件（其中job服务的机器与端口填写上一步骤中映射好的固定机器IP与端口）；
5. 准备日志目录，按照二进制环境的命令运行迁移工具；
6. 确认迁移成功后，清理迁移过程中临时使用的机器端口映射，日志文件与Java环境可按需保留。


## 二、各大版本间升级需要执行的升级任务

### 3.0.x/3.1.x/3.2.x -> 3.3.x
需要在更新Job二进制Jar文件重启进程（所有微服务进程）前后用不同参数分别执行一次升级工具，工具总共执行两次，用于迁移权限数据和账号加密数据。  

### 3.3.x -> 3.4.x  
需要在更新Job后执行一次升级工具，用于迁移作业模板/脚本引用的标签数据。  

### 3.4.x -> 3.5.x  
1.在更新Job后执行一次升级工具，用于迁移业务集数据；

2.除了执行以上升级工具外，还需要额外执行以下步骤：  

（1）获取安装包内根目录下support-files/bk-cmdb/changeBizSetId.js脚本文件；  
（2）获取上一步中upgrader执行完成后在upgrader同一路径下生成的biz_set_list.json文件，使用其中的数据替换changeBizSetId.js脚本文件中占位符${biz_set_list}；  
（3）将替换后的脚本文件移动到一台具有MongoDB命令行且有权限访问CMDB的MongoDB数据库的机器上，执行以下命令，完成CMDB中的业务集ID更改：  
mongo cmdb  -u $BK_CMDB_MONGODB_USERNAME -p $BK_CMDB_MONGODB_PASSWORD --host $BK_CMDB_MONGODB_HOST --port $BK_CMDB_MONGODB_PORT   changeBizSetId.js  
注：  
$BK_CMDB_MONGODB_USERNAME等变量可从中控机获取，方法如下：  
cd /data/install  
source load_env.sh  
echo $BK_CMDB_MONGODB_<tab>补全。  
该操作将修改CMDB的MongoDB数据库中的相关数据，存在一定风险，在获取参数后务必先确认连接的是CMDB的MongoDB实例。  
（4）完成CMDB中的业务集ID更改后，人工确认需要迁移的业务集均已在CMDB存在且ID与原Job中ID一致（若业务集未迁移成功则不执行后续步骤防止产生脏数据），确认成功后继续到执行upgrader的机器再次执行upgrader对迁移后的业务集进行授权（权限有效期为一年，过期后需要重新申请）：  
示例：  
```shell
/opt/java/bin/java -Dfile.encoding=utf8 -Djob.log.dir=/data/bkee/logs/job -Dconfig.file=/data/bkee/etc/job/upgrader/upgrader.properties -Dtarget.tasks=BizSetAuthMigrationTask -jar upgrader-3.5.0.0.jar 3.4.5.0 3.5.0.0 MAKE_UP
```  
（5）完成业务集授权后，再次执行upgrader进行业务集迁移状态更新（用于触发业务集同步）：
```shell
/opt/java/bin/java -Dfile.encoding=utf8 -Djob.log.dir=/data/bkee/logs/job -Dconfig.file=/data/bkee/etc/job/upgrader/upgrader.properties -Dtarget.tasks=BizSetMigrationStatusUpdateTask -jar upgrader-3.5.0.0.jar 3.4.5.0 3.5.0.0 MAKE_UP true
```  
注：命令中涉及到的版本号请使用真实值，示例代码中仅为参考。  

### 3.5.x -> 3.6.x  
无需执行升级工具。

### 3.6.x -> 3.7.x  
需要在更新Job后执行一次升级工具，用于为存量的作业模板/执行方案/定时任务/IP白名单中的主机数据添加hostId，以及迁移对所有业务生效的IP白名单数据。  

### 3.7.x -> 3.8.x
无需执行升级工具。  

### 3.8.x -> 3.9.x
需要在更新Job后执行一次升级工具，用于导出已配置的平台信息数据（title/footer/助手链接等），从3.9.3版本开始，作业平台改为使用基于BK-REPO的全局配置统一方案，升级任务运行后会输出平台信息全局配置base.js文件，以便于迁移之前用户已通过页面【平台管理-全局设置-平台信息】配置的数据：将该任务生成的base.js文件导入BK-REPO中（若已存在则覆盖），路径取值与chart values的bkSharedResUrl、bkSharedBaseJsPath配置项保持一致，默认为：${当前环境部署的BK-REPO根地址}/generic/blueking/bk-config/bk_job/base.js。 
