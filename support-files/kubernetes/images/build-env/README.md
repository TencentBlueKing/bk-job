# bk-job 构建工具镜像

该镜像用于统一 bk-job 前后端编译环境，默认基于 `bkjob/tool-set:3.12.5` 构建。镜像提供构建所需工具和容器内 MySQL 启动能力。

## 内置组件

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK17 | TencentKona 17.0.16 | 默认 JDK，来自 `bkjob/tool-set:3.12.5` |
| JDK8 | TencentKona 8.0.26 / jdk8u492 | 通过 `use-jdk8` 切换 |
| Git | 2.41.3 |  |
| NodeJS | 24.16.0 | 通过 nvm 安装，版本固定 |
| nvm | 0.40.3 | 用于切换 NodeJS 版本 |
| MySQL | 8.4.6 | 来自 `tool-set`，通过 `start-mysql` 启动 |
| OpenSSL 1.1 兼容库 | 系统仓库版本 | 用于运行 flapdoodle embedded MongoDB 4.4.x |

## 构建镜像

目录文件说明：

| 文件 | 说明 |
| --- | --- |
| `build-tool.Dockerfile` | 构建工具镜像定义 |
| `jdk-env.sh` | JDK 环境脚本，复制到 `/etc/profile.d/jdk-env.sh` |
| `start-mysql.sh` | MySQL 启动脚本，复制到镜像内 `/usr/local/bin/start-mysql` |

构建镜像：

```bash
cd support-files/kubernetes/images/build-env
docker build \
  -t hub.bktencent.com/blueking/job-build-tool:1.0.0 \
  -f build-tool.Dockerfile \
  .
```

需要调整基础镜像或组件版本时，通过 build-arg 覆盖：

```bash
cd support-files/kubernetes/images/build-env
docker build \
  --build-arg BASE_IMAGE=bkjob/tool-set:3.12.5 \
  --build-arg GIT_VERSION=2.41.3 \
  --build-arg KONA_JDK8_TAG=8.0.26-GA \
  --build-arg KONA_JDK8_PACKAGE=TencentKona8.0.26.b1_jdk_linux-x86_64_8u492.tar.gz \
  --build-arg NODE_VERSION=24.16.0 \
  --build-arg NVM_VERSION=0.40.3 \
  -t hub.bktencent.com/blueking/job-build-tool:1.0.0 \
  -f build-tool.Dockerfile \
  .
```

## MySQL

容器内 MySQL 默认参数：

| 参数 | 默认值 | 覆盖方式 |
| --- | --- | --- |
| 地址 | `127.0.0.1` | 固定监听容器内本机 |
| 端口 | `3306` | `MYSQL_PORT` |
| 用户 | `root` | `MYSQL_USER` |
| 密码 | `root` | `MYSQL_PASSWORD` |
| 数据目录 | `/data/mysql` | `MYSQL_DATADIR` |
| 运行目录 | `/tmp/mysql` | `MYSQL_RUN_DIR` |

这些变量在容器环境中已有默认值，如需要修改可通过 `docker run -e` 覆盖。

启动并验证 MySQL：

```bash
start-mysql
/usr/local/mysql/bin/mysqladmin -h127.0.0.1 -P${MYSQL_PORT} -u${MYSQL_USER} -p${MYSQL_PASSWORD} ping
```

使用时可以通过环境变量覆盖账号、密码等变量：

```bash
docker run --rm -it \
  -e MYSQL_PORT=3307 \
  -e MYSQL_USER=bkjob \
  -e MYSQL_PASSWORD=bkjob \
  hub.bktencent.com/blueking/job-build-tool:1.0.0 \
  bash -l
```

## 本地使用

在宿主机先拉取 bk-job 源码，然后把源码目录挂载到容器中：

```bash
git clone https://github.com/TencentBlueKing/bk-job.git
cd bk-job
docker run --rm -it \
  -v "$PWD:/workspace" \
  -w /workspace \
  hub.bktencent.com/blueking/job-build-tool:1.0.0 \
  bash -l
```

### 后端构建

进入容器后，先启动干净的 MySQL，再导入 `support-files/sql` 下的初始化 SQL，最后按项目现有方式构建后端：

```bash
start-mysql

cd /workspace/support-files/sql
export MYSQL_PWD="${MYSQL_PASSWORD}"
for i in */*.sql; do
  echo "${i}"
  /usr/local/mysql/bin/mysql -h127.0.0.1 -P${MYSQL_PORT} -u${MYSQL_USER} < "${i}"
done
unset MYSQL_PWD

cd /workspace/src/backend
./gradlew clean build \
  -DmysqlURL=127.0.0.1:${MYSQL_PORT} \
  -DmysqlUser=${MYSQL_USER} \
  -DmysqlPasswd=${MYSQL_PASSWORD} \
  -DmavenRepoUrl=<maven_repo_url>
```

### 前端构建

进入容器后，按项目现有方式构建前端：

```bash
cd /workspace/src/frontend
npm i
npm run build
```

## GitHub Actions 使用

在 GitHub Actions 中，可以把该镜像作为 job container。`actions/checkout` 会把源码检出到工作目录，后续命令直接基于检出的源码执行。

### 后端构建

```yaml
jobs:
  build-backend:
    runs-on: ubuntu-22.04
    container:
      image: hub.bktencent.com/blueking/job-build-tool:1.0.0
    steps:
      - uses: actions/checkout@v4
      - name: Init MySQL
        working-directory: support-files/sql
        run: |
          start-mysql
          export MYSQL_PWD="${MYSQL_PASSWORD}"
          for i in */*.sql; do
            echo "${i}"
            /usr/local/mysql/bin/mysql -h127.0.0.1 -P${MYSQL_PORT} -u${MYSQL_USER} < "${i}"
          done
          unset MYSQL_PWD
      - name: Build backend
        working-directory: src/backend
        run: |
          ./gradlew clean build \
            -DmysqlURL=127.0.0.1:${MYSQL_PORT} \
            -DmysqlUser=${MYSQL_USER} \
            -DmysqlPasswd=${MYSQL_PASSWORD} \
            -DmavenRepoUrl="https://mirrors.cloud.tencent.com/nexus/repository/maven-public/" \
            --info --stacktrace
```

### 前端构建

```yaml
jobs:
  build-frontend:
    runs-on: ubuntu-latest
    container:
      image: hub.bktencent.com/blueking/job-build-tool:1.0.0
    steps:
      - uses: actions/checkout@v2
      - name: Build frontend
        working-directory: src/frontend
        run: |
          npm i --legacy-peer-deps && npm run build
```

## JDK 切换

默认使用 JDK17：

```bash
java -version
```

切换到 JDK8：

```bash
use-jdk8
java -version
```

切回 JDK17：

```bash
use-jdk17
java -version
```

## 组件验证

```bash
docker run --rm hub.bktencent.com/blueking/job-build-tool:1.0.0 bash -lc \
  'java -version && use-jdk8 && java -version && use-jdk17 && node -v && npm -v && nvm --version && git --version && ldconfig -p | grep libcrypto.so.1.1 && start-mysql && /usr/local/mysql/bin/mysqladmin -h127.0.0.1 -P${MYSQL_PORT} -u${MYSQL_USER} -p${MYSQL_PASSWORD} ping'
```
