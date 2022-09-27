#! /bin/sh

echo "KUBERNETES_NAMESPACE=$KUBERNETES_NAMESPACE"
echo "BK_JOB_HOME=$BK_JOB_HOME"
echo "BK_JOB_APP_NAME=$BK_JOB_APP_NAME"
echo "BK_JOB_STORAGE_BASE_DIR=$BK_JOB_STORAGE_BASE_DIR"
echo "BK_JOB_STORAGE_OUTER_DIR=$BK_JOB_STORAGE_OUTER_DIR"
echo "BK_JOB_STORAGE_LOCAL_DIR=$BK_JOB_STORAGE_LOCAL_DIR"
echo "BK_JOB_POD_NAME=$BK_JOB_POD_NAME"
echo "BK_JOB_NODE_IP=$BK_JOB_NODE_IP"
echo "OTEL_TRACE_ENABLED=$OTEL_TRACE_ENABLED"
echo "OTEL_TRACE_REPORT_ENABLED=$OTEL_TRACE_REPORT_ENABLED"
echo "OTEL_TRACE_REPORT_ENDPOINT_URL=$OTEL_TRACE_REPORT_ENDPOINT_URL"
echo "OTEL_TRACE_REPORT_BK_DATA_TOKEN=$OTEL_TRACE_REPORT_BK_DATA_TOKEN"

# 创建本地临时文件目录
mkdir -p "$BK_JOB_STORAGE_LOCAL_DIR"
chmod 666 "$BK_JOB_STORAGE_LOCAL_DIR"

# 拼接日志相关路径
BK_JOB_LOG_BASE_DIR="/data/logs"
BK_JOB_LOG_DIR="$BK_JOB_LOG_BASE_DIR/$BK_JOB_APP_NAME"

echo "BK_JOB_LOG_BASE_DIR=$BK_JOB_LOG_BASE_DIR"
mkdir -p "$BK_JOB_LOG_BASE_DIR"
chmod 666 "$BK_JOB_LOG_BASE_DIR"

echo "BK_JOB_LOG_DIR=$BK_JOB_LOG_DIR"
mkdir -p "$BK_JOB_LOG_DIR"
chmod 666 "$BK_JOB_LOG_DIR"

# 创建软链接供容器内日志采集使用
ln -s $BK_JOB_LOG_BASE_DIR /data/logs
ls $BK_JOB_LOG_BASE_DIR

# 创建file-worker工作空间
if [[ "$BK_JOB_FILE_WORKER_WORKSPACE_DIR" != "" ]];then
    echo "BK_JOB_FILE_WORKER_WORKSPACE_DIR=$BK_JOB_FILE_WORKER_WORKSPACE_DIR"
    mkdir -p "$BK_JOB_FILE_WORKER_WORKSPACE_DIR"
    chmod 666 "$BK_JOB_FILE_WORKER_WORKSPACE_DIR"
fi

exec java -server \
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
     -jar /data/job/exec/$BK_JOB_JAR \
     "$@"
