# 该文件由shell启动时 source，用于初始化默认jdk，并提供jdk切换函数
export JAVA17_HOME=/bk_job_data/TencentKona-17.0.16.b1
export KONA_JDK17_HOME=/bk_job_data/TencentKona-17.0.16.b1
export JAVA8_HOME=/bk_job_data/TencentKona8
export KONA_JDK8_HOME=/bk_job_data/TencentKona8
export NVM_DIR=/usr/local/nvm
export MYSQL_PORT=${MYSQL_PORT:-3306}
export MYSQL_USER=${MYSQL_USER:-root}
export MYSQL_PASSWORD=${MYSQL_PASSWORD:-root}
export MYSQL_DATADIR=${MYSQL_DATADIR:-/bk_job_data/mysql}
export MYSQL_RUN_DIR=${MYSQL_RUN_DIR:-/tmp/mysql}

[ -s "$NVM_DIR/nvm.sh" ] && . "$NVM_DIR/nvm.sh"

# nodejs版本由Dockerfile安装，运行时从nvm目录自动识别，避免脚本内重复写版本号
if [ -z "${NODE_HOME:-}" ] && [ -d "$NVM_DIR/versions/node" ]; then
    NODE_HOME="$(find "$NVM_DIR/versions/node" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -1)"
fi
export NODE_HOME

use-jdk17() {
    export JAVA_HOME="$JAVA17_HOME"
    export CLASSPATH=".:$JAVA_HOME/lib"
    export PATH="$JAVA_HOME/bin:$NODE_HOME/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
}

use-jdk8() {
    export JAVA_HOME="$JAVA8_HOME"
    export CLASSPATH=".:$JAVA_HOME/lib"
    export PATH="$JAVA_HOME/bin:$NODE_HOME/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
}

# 默认使用jdk17，需要jdk8时手动执行use-jdk8
use-jdk17
