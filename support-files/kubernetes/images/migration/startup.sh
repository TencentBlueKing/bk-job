#! /bin/sh

mkdir -p $BK_JOB_LOGS_DIR
chmod 777 $BK_JOB_LOGS_DIR

pwd
ls -ahl
echo "===========SQL========="
find .
echo "===========ENV========="
env
echo "===========EXEC========"
mysql --version

ALL_SQL=($(echo ./sql/*/*.sql))
echo "ALL_SQL=$ALL_SQL"

function checkMysql(){
  c=$(mysql -h $BK_JOB_MYSQL_HOST -P $BK_JOB_MYSQL_PORT -uroot -p$BK_JOB_MYSQL_ROOT_PASSWORD -e "select 1"|grep 1|wc -l)
  echo "c=$c"
  if [[ "$c" == "2" ]];then
    return 0
  fi
  return 1
}

isMysqlOK=1
until [ "${isMysqlOK}" -eq "0" ]; do
  echo "wait for mysql to be available"
  sleep 1
  checkMysql
  isMysqlOK=$?
done

for sql in "${ALL_SQL[@]}"; do
  mysql -h $BK_JOB_MYSQL_HOST -P $BK_JOB_MYSQL_PORT -uroot -p$BK_JOB_MYSQL_ROOT_PASSWORD < $sql
done

echo "begin to migrate iam model"
echo "BK_JOB_GATEWAY_URL=${BK_JOB_GATEWAY_URL}"
ALL_IAM_MODEL=($(echo ./bkiam/*.json))
for iam_model in "${ALL_IAM_MODEL[@]}"; do
  sed -i "s,https://job-gateway.service.consul:10503,${BK_JOB_GATEWAY_URL}," $iam_model
  python ./bkiam/do_migrate.py -t $BK_IAM_URL -a $BK_JOB_APP_CODE -s $BK_JOB_APP_SECRET -f  $iam_model
done
