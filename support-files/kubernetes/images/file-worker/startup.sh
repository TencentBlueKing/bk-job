#! /bin/sh

echo "BK_JOB_APP_NAME=$BK_JOB_APP_NAME"
echo "KUBERNETES_NAMESPACE=$KUBERNETES_NAMESPACE"
echo "BK_JOB_POD_NAME=$BK_JOB_POD_NAME"
echo "BK_JOB_STORAGE_BASE_DIR=$BK_JOB_STORAGE_BASE_DIR"

# 拼接日志相关路径
BK_JOB_LOG_BASE_DIR="$BK_JOB_STORAGE_BASE_DIR/$KUBERNETES_NAMESPACE/$BK_JOB_POD_NAME/logs"
BK_JOB_LOG_DIR="$BK_JOB_LOG_BASE_DIR/$BK_JOB_APP_NAME"

echo "BK_JOB_LOG_BASE_DIR=$BK_JOB_LOG_BASE_DIR"
mkdir -p "$BK_JOB_LOG_BASE_DIR"
chmod 777 "$BK_JOB_LOG_BASE_DIR"

echo "BK_JOB_LOG_DIR=$BK_JOB_LOG_DIR"
mkdir -p "$BK_JOB_LOG_DIR"
chmod 777 "$BK_JOB_LOG_DIR"

echo "BK_JOB_FILE_WORKER_WORKSPACE_DIR=$BK_JOB_FILE_WORKER_WORKSPACE_DIR"
mkdir -p "$BK_JOB_FILE_WORKER_WORKSPACE_DIR"
chmod 777 "$BK_JOB_FILE_WORKER_WORKSPACE_DIR"

echo "BK_JOB_NODE_IP=$BK_JOB_NODE_IP"

ls $BK_JOB_LOG_BASE_DIR

java -server \
     -Dfile.encoding=UTF-8 \
     -Djob.log.dir=$BK_JOB_LOG_BASE_DIR \
     -Xloggc:$BK_JOB_LOG_DIR/gc.log \
     -XX:+UseGCLogFileRotation \
     -XX:NumberOfGCLogFiles=12 \
     -XX:GCLogFileSize=1G \
     -XX:+PrintTenuringDistribution \
     -XX:+PrintGCDetails \
     -XX:+PrintGCDateStamps \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=heap.hprof \
     -XX:ErrorFile=$BK_JOB_LOG_DIR/error_sys.log \
     -Dspring.profiles.active=$BK_JOB_PROFILE \
     $BK_JOB_JVM_OPTION \
     -jar /data/job/exec/$BK_JOB_JAR
