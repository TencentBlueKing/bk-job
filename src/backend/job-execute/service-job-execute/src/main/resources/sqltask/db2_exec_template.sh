#!/bin/bash
anynowtime="date +'%Y-%m-%d %H:%M:%S'"
NOW="echo [\`${anynowtime}\`][PID:$$]"
function job_start
{
    echo "`eval ${NOW}` job_start"
}
function job_success
{
    MSG="$*"
    echo "`eval ${NOW}` job_success:[${MSG}]"
    exit 0
}
function job_fail
{
    MSG="$*"
    echo "`eval ${NOW}` job_fail:[${MSG}]"
    exit 1
}

function error_check
{
    SQL_E=$1
    LOG_FILE=$2
    ERROR_DESC=`egrep "${SQL_E}" ${LOG_FILE} | sed -r "s/(${SQL_E}).*/\1/g" | awk 'END{print}'`

    if [ "${ERROR_DESC}" != "" ]; then
        job_fail ${ERROR_DESC}
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

# start
db2 -tvf ${sqlFile} > ${LOG} 2>&1

RET_CODE=$?

cat ${LOG}
# check
error_check "SQLSTATE=[0-9]+[A-Z]+" ${LOG}
error_check "^SQL[0-9]+[A-Z]+ " ${LOG}

# 其他告警,比如错误的将mysql服务器当成Oracle来执行，会找不到sqlplus
grep -qiF "command not found" ${LOG} && job_fail "DB error or unknown error,plz check"

if [ ${RET_CODE} -ne 0 ]; then
    job_fail
fi

job_success "success"