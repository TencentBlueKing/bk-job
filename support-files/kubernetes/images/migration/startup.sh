#! /bin/sh

pwd
ls -ahl
echo "===========SQL========="
find .
echo "===========ENV========="
env
echo "===========EXEC========"
MYSQL_CMD="mysql $BK_JOB_TLS_OPTIONS $BK_JOB_MYSQL_EXTRA_OPTIONS"
echo "MYSQL_CMD=$MYSQL_CMD"

echo "sleep ${BK_JOB_SLEEP_SECONDS_BEFORE_MIGRATION}s before migration"
sleep ${BK_JOB_SLEEP_SECONDS_BEFORE_MIGRATION}
echo "sleep end"

$MYSQL_CMD --version

# 记录退出码
exitCode=0

function checkMysql(){
  c=$($MYSQL_CMD -h $BK_JOB_MYSQL_HOST -P $BK_JOB_MYSQL_PORT -u$BK_JOB_MYSQL_ADMIN_USERNAME -p$BK_JOB_MYSQL_ADMIN_PASSWORD -e "select 1"|grep 1|wc -l)
  echo "c=$c"
  if [[ "$c" == "2" ]];then
    return 0
  fi
  return 1
}

function migrateMySQL(){
  ALL_SQL=($(echo ./sql/*/*.sql))
  echo "ALL_SQL=$ALL_SQL"
  isMysqlOK=1
  until [ "${isMysqlOK}" -eq "0" ]; do
    echo "wait for mysql to be available"
    sleep 1
    checkMysql
    isMysqlOK=$?
  done

  for sql in "${ALL_SQL[@]}"; do
    echo "migrate $sql"
    $MYSQL_CMD -h $BK_JOB_MYSQL_HOST -P $BK_JOB_MYSQL_PORT -u$BK_JOB_MYSQL_ADMIN_USERNAME -p$BK_JOB_MYSQL_ADMIN_PASSWORD < $sql
    echo "migrate $sql done"
  done
}

function migrateIamModel(){
  echo "begin to migrate iam model"
  echo "BK_JOB_API_URL=${BK_JOB_API_URL}"
  ALL_IAM_MODEL=($(echo ./bkiam/*.json))
  for iam_model in "${ALL_IAM_MODEL[@]}"; do
    sed -i "s,https://job-gateway.service.consul:10503,${BK_JOB_API_URL}," $iam_model
    python3.6 ./bkiam/do_migrate.py --bk_tenant_id system -t $BK_IAM_APIGATEWAY_URL -a $BK_JOB_APP_CODE -s $BK_JOB_APP_SECRET -f  $iam_model
    exitCode=$?
    if [ "$exitCode" -ne "0" ];then
        echo "migrateIamModel exitCode=$exitCode"
        break
    fi
  done
}

echo "BK_JOB_MIGRATION_MYSQL_SCHEMA_ENABLED=${BK_JOB_MIGRATION_MYSQL_SCHEMA_ENABLED}"
if [[ "${BK_JOB_MIGRATION_MYSQL_SCHEMA_ENABLED}" == "true" ]];then
  migrateMySQL
else
  echo "skip migrateMySQL"
fi

echo "BK_JOB_MIGRATION_IAM_MODEL_ENABLED=${BK_JOB_MIGRATION_IAM_MODEL_ENABLED}"
if [[ "${BK_JOB_MIGRATION_IAM_MODEL_ENABLED}" == "true" ]];then
  migrateIamModel
else
  echo "skip migrateIamModel"
fi

echo "sleep ${BK_JOB_SLEEP_SECONDS_AFTER_MIGRATION}s after migration"
sleep ${BK_JOB_SLEEP_SECONDS_AFTER_MIGRATION}
echo "sleep end"

echo "exitCode=$exitCode"
exit $exitCode
