#! /bin/sh
PROGRAM=$(basename "$0")
usage () {
    cat <<EOF
Usage:
    $PROGRAM [FROM_VERSION] [TO_VERSION]
    FROM_VERSION：升级前的系统版本号，如3.5.1-rc.4
    TO_VERSION：升级后的系统版本号，如3.6.2-rc.4
    注意：请先通过配置模板文件upgrader.properties.tpl渲染生成配置文件upgrader.properties后再执行该脚本
EOF
}

usage_and_exit () {
    usage
    exit "$1"
}

# Parse command line
fromVersion=""
toVersion=""
if [ $# -eq 2 ]; then
    fromVersion=$1
    toVersion=$2
    echo "fromVersion=$fromVersion"
    echo "toVersion=$toVersion"
else
    usage_and_exit 1
fi

if [ ! -f "upgrader.properties" ];then
    usage_and_exit 1
fi

if [[ ! -d /data/job/logs ]];then
    mkdir -p /data/job/logs
fi
java -Dfile.encoding=utf8 -Djob.log.dir=/data/job/logs -Dconfig.file=upgrader.properties -jar upgrader.jar $fromVersion $toVersion AFTER_UPDATE_JOB
