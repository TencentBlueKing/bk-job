#! /bin/sh

mkdir -p $BK_JOB_LOGS_DIR
chmod 777 $BK_JOB_LOGS_DIR

java -server \
     -Dfile.encoding=UTF-8 \
     -Xloggc:$BK_JOB_LOGS_DIR/gc.log \
     -XX:+UseGCLogFileRotation \
     -XX:NumberOfGCLogFiles=12 \
     -XX:GCLogFileSize=1G \
     -XX:+PrintTenuringDistribution \
     -XX:+PrintGCDetails \
     -XX:+PrintGCDateStamps \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=heap.hprof \
     -XX:ErrorFile=$BK_JOB_LOGS_DIR/error_sys.log \
     -Dspring.profiles.active=$BK_JOB_PROFILE \
     $BK_JOB_JVM_OPTION \
     -jar /data/job/$BK_JOB_JAR
