#! /bin/sh

pwd
ls -ahl
echo "===========ENV========="
env
echo "======================="
logLevel="INFO"
if [[ "$BK_JOB_LOG_LEVEL" != "" ]];then
    logLevel="$BK_JOB_LOG_LEVEL"
fi
echo "logLevel=$logLevel"
exec java \
     -Dfile.encoding=UTF-8 \
     -Dlog.level=${logLevel} \
     -Xlog:gc*,gc+age=trace:file=/data/logs/controller/gc.log:time,uptime,level,tags:filecount=12,filesize=1g \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=heap.hprof \
     -jar /data/job/exec/k8s-startup-controller.jar \
     "$@"
