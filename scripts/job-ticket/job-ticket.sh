#!/bin/bash
PROJECT_NAME=job
SERVICE_NAME=job-ticket

SELF_DIR=$(readlink -f "$(dirname $0)")
SERVICE_HOME=$(cd $SELF_DIR/.. && pwd )
if [[ -z "${BK_HOME}" ]]; then
    BK_HOME=$(cd $SELF_DIR/../../../../ && pwd)
fi
JOB_HOME=${BK_HOME}/${PROJECT_NAME}
if [[ -z "${JOB_CONF_HOME}" ]]; then
    JOB_CONF_HOME=${BK_HOME}/etc/${PROJECT_NAME}
fi
if [[ -z "${LOGS_HOME}" ]]; then
    LOGS_HOME=${BK_HOME}/logs/${PROJECT_NAME}/${SERVICE_NAME}/
fi

SHELL_FILE_NAME=${0##*/}
JAR_FILE=${SERVICE_HOME}/${SERVICE_NAME}.jar


if [[ ! -n "${JAVA_HOME}" ]]||[[ ! -d "${JAVA_HOME}" ]]; then
    JAVA_HOME="${BK_HOME}/service/java"
    if [[ ! -d "${JAVA_HOME}" ]]; then
        JAVA_HOME="${BK_HOME}/common/java"
    fi
    export JAVA_HOME
    export PATH=${JAVA_HOME}/bin:$PATH
fi

# Java process pid
PID=0
# Spring profile param
SPRING_PROFILE="$2"

# Get current java process pid
function getPID(){
    javaps=`${JAVA_HOME}/bin/jps -l | grep "${JAR_FILE}"`
    if [[ -n "$javaps" ]]; then
        PID=`echo ${javaps} | awk '{print $1}'`
    else
        PID=0
    fi
}



function startup() {
    JAVA_OPTS="$JAVA_OPTS -server -Dfile.encoding=UTF-8"
	JAVA_OPTS="$JAVA_OPTS -Dspring.config.additional-location=file://${JOB_CONF_HOME}/application-file-gateway.yml"
	
    if [ -n "${SPRING_PROFILE}" ];then
        JAVA_OPTS="$JAVA_OPTS -Dspring.profiles.active=${SPRING_PROFILE}"
	else
		JAVA_OPTS="$JAVA_OPTS -Dspring.profiles.active=prod"
    fi

    getPID
    if [[ ${PID} -ne 0 ]]; then
        echo "${PROJECT_NAME}:${SERVICE_NAME} already started(PID=$PID)"
    else
        echo -n "Starting ${PROJECT_NAME}:${SERVICE_NAME}"
        if [[ ! -d "${LOGS_HOME}" ]]; then
            mkdir -p "${LOGS_HOME}"
        fi

        NOHUPLOG=/dev/null
        #NOHUPLOG=${PROJECT_NAME}_${SERVICE_NAME}_console.log
        nohup ${JAVA_HOME}/bin/java ${JAVA_OPTS} -jar ${JAR_FILE} > ${NOHUPLOG} 2>&1 &

        # Process listening port status, 0:not listen, 1:listen
        HTTP_PORT_STATUS=0

        for i in $(seq 60)
        do
            sleep 0.5
            echo -e ".\c"
            getPID
            if [[ ${PID} -ne 0 ]]; then
                checkPortStatus "${PID}"
                HTTP_PORT_STATUS=$?
                if [[ ${HTTP_PORT_STATUS} -gt 0 ]]; then
                    break;
                fi
            fi
        done

        if [[ ${HTTP_PORT_STATUS} -eq 0 ]]; then
            echo "[Failed]-- http port ${HTTP_PORT} start fail!"
            exit 1
        fi

        echo "(PID=$PID)...[Success]"

    fi
}

function checkPortStatus() {
    # Process listening port status, 0:not listen, 1:listen
    HTTP_PORT_STATUS=0
    # $1 is pid
    PID_TMP="$1"
    PID_PNAME="${PID_TMP}/java"

    LISTEN_PID_COUNT=`netstat -atnp|grep "${PID_PNAME}"|wc -l`
    if [[ ${LISTEN_PID_COUNT} -ne 0 ]]; then
        HTTP_PORT_STATUS=1
    else
        HTTP_PORT_STATUS=0
    fi
    return $HTTP_PORT_STATUS
}

function shutdown(){
    getPID
    if [[ ${PID} -ne 0 ]]; then
        echo -n "Stopping ${PROJECT_NAME}:${SERVICE_NAME}(PID=${PID})..."
        kill ${PID}
        if [[ $? -ne 0 ]]; then
            echo "[Failed]"
            exit 1
        fi
        for i in $(seq 20)
        do
            sleep 0.5
            getPID
            if [[ ${PID} -eq 0 ]]; then

                checkPortStatus "${PID}"
                HTTP_PORT_STATUS=$?

                if [[ ${HTTP_PORT_STATUS} -eq 0 ]]; then
                    break
                fi
            fi
            echo -e ".\c"
        done
        getPID
        if [[ ${PID} -eq 0 ]]; then
            echo "[Success]"
        else
            kill -9 ${PID}
            if [[ $? -ne 0 ]]; then
                echo "[Failed]"
                exit 1
            fi
            echo "some task is running in background,force stop it.[Success]"
        fi
    else
        echo "${PROJECT_NAME}:${SERVICE_NAME} is not running"
    fi
}

function getServerStatus(){
    getPID
    if [[ ${PID} -ne 0 ]]; then
        checkPortStatus "${PID}"
        HTTP_PORT_STATUS=$?
        if [[ ${HTTP_PORT_STATUS} -eq 0 ]]; then
            echo "${PROJECT_NAME}:${SERVICE_NAME} port ${HTTP_PORT} is not listening(PID=${PID})";
            exit 99;
        fi
        echo "${PROJECT_NAME}:${SERVICE_NAME} is running(PID=${PID})"
    else
        echo "${PROJECT_NAME}:${SERVICE_NAME} is not running"
    fi
}

function restart(){
    shutdown
    sleep 1
    startup
}

case "$1" in 
restart)
    restart;;
start)
    startup;;
stop)
    shutdown;;
status)
    getServerStatus;;
*)
echo $"Usage: ${SHELL_FILE_NAME} {start|stop|status|restart}"
esac
