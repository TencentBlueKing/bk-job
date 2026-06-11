#!/usr/bin/env bash
set -euo pipefail

export PATH="/usr/local/mysql/bin:$PATH"

# 这些参数均可在 docker run 时通过 -e 覆盖。
MYSQL_DATADIR=${MYSQL_DATADIR:-/data/mysql}
MYSQL_RUN_DIR=${MYSQL_RUN_DIR:-/tmp/mysql}
MYSQL_SOCKET=${MYSQL_SOCKET:-${MYSQL_RUN_DIR}/mysql.sock}
MYSQL_PORT=${MYSQL_PORT:-3306}
MYSQL_USER=${MYSQL_USER:-root}
MYSQL_PASSWORD=${MYSQL_PASSWORD:-root}
MYSQL_LOG=${MYSQL_LOG:-${MYSQL_RUN_DIR}/mysqld.log}

case "${MYSQL_DATADIR}" in
    /data/*|/tmp/*) ;;
    *)
        echo "MYSQL_DATADIR must be under /data or /tmp because start-mysql recreates it: ${MYSQL_DATADIR}" >&2
        exit 1
        ;;
esac

case "${MYSQL_RUN_DIR}" in
    /tmp/*) ;;
    *)
        echo "MYSQL_RUN_DIR must be under /tmp because start-mysql recreates it: ${MYSQL_RUN_DIR}" >&2
        exit 1
        ;;
esac

mkdir -p "${MYSQL_RUN_DIR}"

mysql_ping() {
    mysqladmin --socket="${MYSQL_SOCKET}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" ping >/dev/null 2>&1 \
        || mysqladmin --socket="${MYSQL_SOCKET}" -uroot ping >/dev/null 2>&1
}

mysql_shutdown() {
    mysqladmin --socket="${MYSQL_SOCKET}" -uroot shutdown >/dev/null 2>&1 \
        || mysqladmin --socket="${MYSQL_SOCKET}" -u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}" shutdown >/dev/null 2>&1 \
        || true
}

# 构建场景需要纯净的MySQL，每次执行都停止旧实例并重建数据目录
if mysql_ping; then
    mysql_shutdown
    for i in {1..30}; do
        if ! mysql_ping; then
            break
        fi
        sleep 1
    done
fi

rm -rf "${MYSQL_DATADIR}" "${MYSQL_RUN_DIR}"
mkdir -p "${MYSQL_DATADIR}" "${MYSQL_RUN_DIR}"

mysqld --initialize-insecure --user=root --datadir="${MYSQL_DATADIR}" --log-error="${MYSQL_RUN_DIR}/initialize.log"

mysqld \
    --user=root \
    --datadir="${MYSQL_DATADIR}" \
    --socket="${MYSQL_SOCKET}" \
    --port="${MYSQL_PORT}" \
    --bind-address=127.0.0.1 \
    --log-error="${MYSQL_LOG}" \
    --daemonize

# 等待 mysqld 完成启动
for i in {1..60}; do
    if mysql_ping; then
        break
    fi
    sleep 1
done

if mysql --socket="${MYSQL_SOCKET}" -uroot -e "SELECT 1" >/dev/null 2>&1; then
    MYSQL_ROOT_AUTH=(-uroot)
else
    MYSQL_ROOT_AUTH=(-u"${MYSQL_USER}" -p"${MYSQL_PASSWORD}")
fi

# 设置 root 密码，并确保 MYSQL_USER 可以通过 localhost 访问。
mysql --socket="${MYSQL_SOCKET}" "${MYSQL_ROOT_AUTH[@]}" <<SQL
ALTER USER 'root'@'localhost' IDENTIFIED BY '${MYSQL_PASSWORD}';
CREATE USER IF NOT EXISTS '${MYSQL_USER}'@'localhost' IDENTIFIED BY '${MYSQL_PASSWORD}';
GRANT ALL PRIVILEGES ON *.* TO '${MYSQL_USER}'@'localhost' WITH GRANT OPTION;
FLUSH PRIVILEGES;
SQL

echo "MySQL started: 127.0.0.1:${MYSQL_PORT}, user=${MYSQL_USER}"
