## 升级说明

[English](UPGRADE.en.md) | 简体中文  

### 版本3.3.4.x
该版本增加一个升级工具（upgrader-3.3.4.x.jar，与其他二进制包在同一目录），需要在更新Job二进制Jar文件重启进程（所有微服务进程）前后用不同参数分别执行一次，工具总共执行两次，用于迁移权限数据和账号加密数据。  

**适用范围**  
适用于从3.x.x.x向3.3.4.x及以上版本升级，3.3.4.x版本内升级（如3.3.4.0-->3.3.4.3）无需执行。  

**工具用法**   
使用命令`java -Dfile.encoding=utf8 -Djob.log.dir=path/to/log/dir -Dconfig.file=/path/to/config/file -jar upgrader-[x.x.x.x].jar [fromVersion] [toVersion] [executeTime]` 启动工具，再根据命令行提示输入与具体版本升级任务相关的特定参数，运行升级工具。  

/path/to/log/dir用于指定工具日志保存路径，通常为${BK_HOME}/logs/job  
/path/to/config/file用于指定工具读取的配置文件，该配置文件由部署脚本自动渲染生成，路径为${BK_HOME}/etc/job/upgrader/upgrader.properties  
fromVersion为当前作业平台版本，如3.2.7.3   
toVersion为目标作业平台版本，如3.3.4.0  
executeTime为升级任务执行的时间点，取值为BEFORE_UPDATE_JOB、AFTER_UPDATE_JOB，在更新作业平台进程前执行本工具填写BEFORE_UPDATE_JOB，更新进程后执行则填写AFTER_UPDATE_JOB  

示例：  
Job进程更新前：    
```shell script
/opt/java/bin/java -Dfile.encoding=utf8 -Djob.log.dir=/data/bkee/logs/job -Dconfig.file=/data/bkee/etc/job/upgrader/upgrader.properties -jar upgrader-3.3.4.0.jar 3.2.7.3 3.3.4.0 BEFORE_UPDATE_JOB  
```
Job进程更新后：  
```shell script
/opt/java/bin/java -Dfile.encoding=utf8 -Djob.log.dir=/data/bkee/logs/job -Dconfig.file=/data/bkee/etc/job/upgrader/upgrader.properties -jar upgrader-3.3.4.0.jar 3.2.7.3 3.3.4.0 AFTER_UPDATE_JOB
```

### 版本3.5.x.x
该版本对接了CMDB业务集，需要对Job原有业务集数据进行迁移，除了执行以上升级工具外，还需要额外执行以下步骤：  
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
（4）完成CMDB中的业务集ID更改后，人工确认需要迁移的业务集均已在CMDB存在且ID与原Job中ID一致（若业务集未迁移成功则不执行后续步骤防止产生脏数据），确认成功后继续到执行upgrader的机器再次执行upgrader进行业务集迁移状态更新（用于触发业务集同步）：
```shell
/opt/java/bin/java -Dfile.encoding=utf8 -Djob.log.dir=/data/bkee/logs/job -Dconfig.file=/data/bkee/etc/job/upgrader/upgrader.properties -Dtarget.tasks=BizSetMigrationStatusUpdateTask -jar upgrader-3.5.0.0.jar 3.4.5.0 3.5.0.0 MAKE_UP true
```  
（5）完成迁移状态更新后，再次执行upgrader对迁移后的业务集进行授权（权限有效期为一年，过期后需要重新申请）：  
示例：  
```shell
/opt/java/bin/java -Dfile.encoding=utf8 -Djob.log.dir=/data/bkee/logs/job -Dconfig.file=/data/bkee/etc/job/upgrader/upgrader.properties -Dtarget.tasks=BizSetAuthMigrationTask -jar upgrader-3.5.0.0.jar 3.4.5.0 3.5.0.0 MAKE_UP
```  
注：命令中涉及到的版本号请使用真实值，示例代码中仅为参考。  


