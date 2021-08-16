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
for sql in "${ALL_SQL[@]}"; do
  echo "mysql -h $BK_JOB_MYSQL_HOST -P $BK_JOB_MYSQL_PORT -u root -p $BK_JOB_MYSQL_ROOT_PASSWORD < $sql"
  mysql -h $BK_JOB_MYSQL_HOST -P $BK_JOB_MYSQL_PORT -u root -p $BK_JOB_MYSQL_ROOT_PASSWORD < $sql
done
