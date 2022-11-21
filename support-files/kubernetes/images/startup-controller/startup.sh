#! /bin/sh

pwd
ls -ahl
echo "===========ENV========="
env
echo "======================="
exec java \
     -Dfile.encoding=UTF-8 \
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
