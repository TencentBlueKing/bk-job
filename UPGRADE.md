## 升级说明
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
