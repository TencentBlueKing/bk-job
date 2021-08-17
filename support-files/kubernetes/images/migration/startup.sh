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

function isMysqlOk(){
  c=$(mysql -h $BK_JOB_MYSQL_HOST -P $BK_JOB_MYSQL_PORT -uroot -p$BK_JOB_MYSQL_ROOT_PASSWORD -e "select 1"|grep 1|wc -l)
  if [[ "$c" == "2" ]];then
    return 0
  fi
  return 1
}

until [ $(isMysqlOk) -eq 0 ]; do
  echo "wait for mysql to be available"
  sleep 1
done

for sql in "${ALL_SQL[@]}"; do
  mysql -h $BK_JOB_MYSQL_HOST -P $BK_JOB_MYSQL_PORT -uroot -p$BK_JOB_MYSQL_ROOT_PASSWORD < $sql
done
