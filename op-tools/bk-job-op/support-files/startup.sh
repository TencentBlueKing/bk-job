#! /bin/sh

echo "JOB_OP_APP_NAME=$JOB_OP_APP_NAME"
echo "JOB_OP_JAR=$JOB_OP_JAR"
echo "JOB_OP_PROFILE=$JOB_OP_PROFILE"
echo "JOB_OP_JVM_OPTION=$JOB_OP_JVM_OPTION"

# 根据cgroup信息获取容器ID
CONTAINER_ID=$(cat /proc/self/cgroup|grep "pids"|sed 's/\//\n/g'|tail -1|cut -c 1-12)
if [[ "$CONTAINER_ID" == "" ]];then
    CONTAINER_ID=$(cat /proc/self/cgroup|grep "memory"|sed 's/\//\n/g'|tail -1|cut -c 1-12)
fi
echo "CONTAINER_ID=$CONTAINER_ID"

# 设置默认应用名称
if [[ "$JOB_OP_APP_NAME" == "" ]];then
    JOB_OP_APP_NAME="bk-job-op"
fi

# 拼接日志相关路径
JOB_OP_LOG_BASE_DIR="/data/logs"
JOB_OP_LOG_DIR="$JOB_OP_LOG_BASE_DIR/$JOB_OP_APP_NAME"

echo "JOB_OP_LOG_BASE_DIR=$JOB_OP_LOG_BASE_DIR"
mkdir -p "$JOB_OP_LOG_BASE_DIR"
chmod 777 "$JOB_OP_LOG_BASE_DIR"

echo "JOB_OP_LOG_DIR=$JOB_OP_LOG_DIR"
mkdir -p "$JOB_OP_LOG_DIR"
chmod 777 "$JOB_OP_LOG_DIR"

# 创建JVM相关文件存储空间
JVM_FILE_DIR="/data/jvm"
if [[ ! -d "$JVM_FILE_DIR" ]];then
    echo "mkdir $JVM_FILE_DIR"
    mkdir -p "$JVM_FILE_DIR"
    chmod 777 "$JVM_FILE_DIR"
fi

# 设置默认的 JVM 参数
if [[ "$JOB_OP_JVM_OPTION" == "" ]];then
    JOB_OP_JVM_OPTION="-Xms512m -Xmx1024m"
fi

# 设置默认的 Profile
if [[ "$JOB_OP_PROFILE" == "" ]];then
    JOB_OP_PROFILE="prod"
fi

# 获取主机名作为实例标识
HOSTNAME=$(hostname)

exec java -server \
     -Dfile.encoding=UTF-8 \
     -Djob.log.dir=$JOB_OP_LOG_BASE_DIR \
     -Xlog:gc*,gc+age=trace:file=${JOB_OP_LOG_DIR}/gc.log:time,uptime,level,tags:filecount=12,filesize=100m \
     -XX:+HeapDumpOnOutOfMemoryError \
     -XX:HeapDumpPath=${JVM_FILE_DIR}/${HOSTNAME}_${CONTAINER_ID}_heap.hprof \
     -XX:ErrorFile=${JVM_FILE_DIR}/${HOSTNAME}_${CONTAINER_ID}_jvm_error.log \
     -Dspring.profiles.active=$JOB_OP_PROFILE \
     $JOB_OP_JVM_OPTION \
     -jar /data/job/exec/$JOB_OP_JAR \
     "$@"
