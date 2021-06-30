#!/bin/sh
anynowtime="date +'%Y-%m-%d %H:%M:%S'"
NOW="echo [\`$anynowtime\`][PID:$$]"
function job_start
{
    echo "`eval $NOW` job_start"
}
function job_success
{
    MSG="$*"
    echo "`eval $NOW` job_success:[$MSG]"
    exit 0
}
function job_fail
{
    MSG="$*"
    echo "`eval $NOW` job_fail:[$MSG]"
    exit 1
}

# 执行脚本
function exec_SQL
{
    touch ${LOG}
	sqlplus /nolog <<ENDOFSQL
    set time on;
    set timing on;
    set linesize 300;
	conn ${DB_USER}/${DB_PASS}
	spool ${LOG}
	@${sqlFile}
	spool off
	quit
ENDOFSQL
}


job_start

sqlFile=$1
# PORT在Oracle中似乎不用了,但先放着吧
PORT=$2
DB_USER=$3
DB_PASS=$4
timeSec=`date +'%s'`

#当前目录
SCRIPT_NAME=${0##*/}
#日志文件全路径
LOG=/tmp/result_${SCRIPT_NAME}_${timeSec}.log
ERROR_LOG=${LOG}.err

# 调用执行Oracle SQL, 将未进行mysql的错误信息追加写入LOG
exec_SQL 2>${ERROR_LOG}

if [ -s ${ERROR_LOG} ]; then
    cat ${ERROR_LOG}
    cat ${ERROR_LOG} >> ${LOG}
fi


# 通用错误检查输出
ERROR_DESC=`egrep -i "((ORA)|(SP2)|(PLS))-.*:.*" ${LOG} | sed -r 's/.*:(.*)/\1/g' | awk 'END{print}'`

if [ "$ERROR_DESC" != "" ]; then
    job_fail ${ERROR_DESC}
fi


# 其他告警,比如错误的将mysql服务器当成Oracle来执行，会找不到sqlplus
grep -qiF "command not found" ${LOG} && job_fail "command not found"

# 其他告警,比如错误的将mysql服务器当成Oracle来执行，会找不到sqlplus
egrep -qi "ERROR [0-9]+.* Can't connect to" ${LOG} && job_fail "DB error or unknown error,plz check"


job_success "success"

