#! /bin/sh

pwd
ls -ahl
echo "===========SQL========="
find .
echo "===========ENV========="
env
echo "===========EXEC========"
MYSQL_CMD_OPTIONS="mysql $BK_JOB_TLS_OPTIONS $BK_JOB_MYSQL_EXTRA_OPTIONS"
echo "MYSQL_CMD_OPTIONS=$MYSQL_CMD_OPTIONS"
echo "Whether to enable incremental migration of mysql: $BK_JOB_INCREMENTAL_MIGRATION_ENABLED"
echo "sleep ${BK_JOB_SLEEP_SECONDS_BEFORE_MIGRATION}s before migration"
sleep ${BK_JOB_SLEEP_SECONDS_BEFORE_MIGRATION}
echo "sleep end"
$MYSQL_CMD_OPTIONS --version

MYSQL_CMD="$MYSQL_CMD_OPTIONS -h$BK_JOB_MYSQL_HOST -P$BK_JOB_MYSQL_PORT -u$BK_JOB_MYSQL_ADMIN_USERNAME -p$BK_JOB_MYSQL_ADMIN_PASSWORD"

# 记录退出码
exitCode=0

function checkMysql(){
  c=$($MYSQL_CMD -e "select 1"|grep 1|wc -l)
  echo "c=$c"
  if [[ "$c" == "2" ]];then
    return 0
  fi
  return 1
}

# 在job_manage库中创建migration表，用于升级时记录成功执行的sql脚本
function initMigrationHistoryTable() {
  $MYSQL_CMD -e "
  SET NAMES utf8mb4;
  CREATE DATABASE IF NOT EXISTS job_manage DEFAULT CHARACTER SET utf8mb4;
  USE job_manage;
  CREATE TABLE IF NOT EXISTS db_migration_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    db_name VARCHAR(64) NOT NULL,
    script_name VARCHAR(128) NOT NULL,
    execute_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_migration_db_name_script_name (db_name, script_name)
  );"
}

# 从sql脚本的路径中解析到库名称和脚本名称
function parseNameFromPath() {
  local filePath="$1"
  local fileName
  # 命名规范：{序号}_{db名称}_{创建时间}-{脚本序号}_{版本号}_{db类型}.sql
  fileName=$(basename "$filePath")
  # 截取 - 左边部分，去掉开头的序号_，去掉最后一个下划线及之后的内容
  local left_part="${fileName%%-*}"
  local tmp="${left_part#*_}"
  local dbName="${tmp%_*}"
  echo "$dbName|$fileName"
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

  if [[ "$BK_JOB_INCREMENTAL_MIGRATION_ENABLED" == "true" ]]; then
    initMigrationHistoryTable
  fi

  for sql in "${ALL_SQL[@]}"; do
    dbName=""
    scriptName=""

    if [[ "$BK_JOB_INCREMENTAL_MIGRATION_ENABLED" == "true" ]]; then
      nameInfo=$(parseNameFromPath "$sql")
      dbName=$(echo "$nameInfo" | cut -d'|' -f1)
      scriptName=$(echo "$nameInfo" | cut -d'|' -f2)
      executed=$($MYSQL_CMD -N -e "SELECT 1 FROM job_manage.db_migration_history WHERE db_name='$dbName' AND script_name='$scriptName' LIMIT 1;")
      if [[ "$executed" == "1" ]]; then
        echo "Skip $sql, already executed."
        continue
      fi
    fi

    echo "migrate $sql"
    if $MYSQL_CMD < "$sql" 2>&1; then
      echo "Migrate $sql success"
      if [[ "$BK_JOB_INCREMENTAL_MIGRATION_ENABLED" == "true" ]]; then
        $MYSQL_CMD -e "INSERT INTO job_manage.db_migration_history(db_name, script_name) VALUES('$dbName','$scriptName');"
      fi
    else
      exitCode=$?
      echo "Migrate $sql failed, exitCode=$exitCode"
      exit $exitCode
    fi
  done
  echo "All migration scripts executed successfully."
}

function migrateIamModel(){
  echo "begin to migrate iam model"
  echo "BK_JOB_API_URL=${BK_JOB_API_URL}"
  echo "BK_JOB_TENANT_ENABLED=${BK_JOB_TENANT_ENABLED}"
  if [[ "${BK_JOB_TENANT_ENABLED}" == "true" ]]; then
      echo "Add iam model for tenant-enabled environment:"
      echo ./bkiam/tenant/*.json
      cp ./bkiam/tenant/*.json ./bkiam/
  fi
  ALL_IAM_MODEL=($(echo ./bkiam/*.json))
  for iam_model in "${ALL_IAM_MODEL[@]}"; do
    sed -i "s,https://job-gateway.service.consul:10503,${BK_JOB_API_URL}," $iam_model
    python3 ./bkiam/do_migrate.py --bk_tenant_id system -t $BK_IAM_APIGATEWAY_URL -a $BK_JOB_APP_CODE -s $BK_JOB_APP_SECRET -f  $iam_model
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
