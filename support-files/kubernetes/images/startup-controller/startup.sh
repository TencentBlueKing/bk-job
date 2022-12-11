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
     -XX:+UseGCLogFileRotation \
     -XX:NumberOfGCLogFiles=12 \
     -XX:GCLogFileSize=1G \
     -XX:+PrintTenuringDistribution \
     -XX:+PrintGCDetails \
     -XX:+PrintGCDateStamps \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=heap.hprof \
     -jar /data/job/exec/k8s-startup-controller.jar \
     "$@"
