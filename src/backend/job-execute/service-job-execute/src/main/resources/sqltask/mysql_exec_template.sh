#!/bin/bash
anynowtime="date +'%Y-%m-%d %H:%M:%S'"
NOW="echo [\`$anynowtime\`][PID:$$]"
function job_start
{
    echo "`eval ${NOW}` job_start"
}
function job_success
{
    MSG="$*"
    echo "`eval ${NOW}` job_success:[$MSG]"
    exit 0
}
function job_fail
{
    MSG="$*"
    echo "`eval ${NOW}` job_fail:[$MSG]"
    exit 1
}

function error_check
{
    error_count=`grep -i "$1" ${LOG} -c`
    if [ ${error_count} -gt 0 ];
        then
            job_fail "$2"
    fi
}

job_start

SCRIPT_NAME=${0##*/}
sqlFile=$1
PORT=$2
USER=$3
PASS=$4
timeSec=`date +'%s'`
LOG=/tmp/result_${SCRIPT_NAME}_${timeSec}.log

if [ ${PORT} -gt 0 ]; then
    HOST_AND_PORT=`netstat -ntl | grep ":${PORT} " | awk '{print $4}'`
    HOST=${HOST_AND_PORT%:${PORT}}
    if [ ${HOST} = '0.0.0.0' -o ${HOST} = '::' ];then
        HOST='127.0.0.1'
    fi
    CMD="-h${HOST} -P$PORT"
fi

if [ "$USER" != "EMPTY" ]; then
    CMD="$CMD -u$USER"
fi

if [ "$PASS" != "EMPTY" ]; then
    CMD="$CMD -p$PASS"
fi

mysql ${CMD} -vvv < ${sqlFile} > ${LOG} 2>&1

RET_CODE=$?

cat ${LOG}

# 通用错误检查输出
ERROR_DESC=`egrep -i "ERROR.* at line [0-9]+:(.*)" ${LOG} | sed -r 's/.* at line [0-9]+:(.*)/\1/g' | awk 'END{print}'`

if [ "$ERROR_DESC" != "" ]; then
    job_fail ${ERROR_DESC}
fi
# 系统级错误输出 例如：ERROR 1045 (28000): Access denied for user 'root'@'localhost' (using password: YES)
ERROR_DESC=`egrep -i "ERROR [0-9]+.*?:(.*)" ${LOG} | awk -F':' '{print $2}'`

if [ "$ERROR_DESC" != "" ]; then
    job_fail ${ERROR_DESC}
fi
# 其他告警,比如错误的将mysql服务器当成Oracle来执行，会找不到sqlplus
grep -qiF "command not found" ${LOG} && job_fail "DB error or unknown error,plz check"

# 其他告警,比如错误的将mysql服务器当成Oracle来执行，会找不到sqlplus
egrep -qi "ERROR [0-9]+.* Can't connect to" ${LOG} && job_fail "DB error or unknown error,plz check"

if [ ${RET_CODE} -ne 0 ]; then
    job_fail
fi
job_success "success"
