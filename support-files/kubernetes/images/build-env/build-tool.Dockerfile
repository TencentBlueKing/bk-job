ARG BASE_IMAGE=bkjob/tool-set:3.12.5

# git编译阶段,该阶段会安装gcc/make等编译依赖，不打入最终镜像
FROM ${BASE_IMAGE} AS git-builder

ARG GIT_VERSION=2.41.3

# 构建指定版本的git
RUN dnf install -y \
        curl-devel \
        diffutils \
        expat-devel \
        gcc \
        make \
        openssl-devel \
        perl-ExtUtils-MakeMaker \
        zlib-devel && \
    cd /tmp && \
    curl -fSL "https://mirrors.edge.kernel.org/pub/software/scm/git/git-${GIT_VERSION}.tar.xz" -o git.tar.xz && \
    mkdir git-src && \
    tar -xJf git.tar.xz -C git-src --strip-components=1 && \
    cd git-src && \
    make prefix="/opt/git" NO_GETTEXT=YesPlease NO_TCLTK=YesPlease all && \
    make prefix="/opt/git" NO_GETTEXT=YesPlease NO_TCLTK=YesPlease install && \
    rm -rf /tmp/git-src /tmp/git.tar.xz && \
    dnf clean all

# 最终镜像阶段。重新基于base_image，避免带入git编译阶段的编译依赖
FROM ${BASE_IMAGE}

ARG KONA_JDK8_TAG=8.0.26-GA
ARG KONA_JDK8_PACKAGE=TencentKona8.0.26.b1_jdk_linux-x86_64_8u492.tar.gz
ARG NODE_VERSION=24.16.0
ARG NVM_VERSION=0.40.3

LABEL maintainer="Tencent BlueKing Job" \
      dockerfile.version="1.0.0"

ENV JAVA17_HOME=/data/TencentKona-17.0.16.b1
ENV KONA_JDK17_HOME=/data/TencentKona-17.0.16.b1
ENV JAVA8_HOME=/data/TencentKona8
ENV KONA_JDK8_HOME=/data/TencentKona8
ENV NVM_DIR=/usr/local/nvm
ENV NODE_HOME=/usr/local/nvm/versions/node/v${NODE_VERSION}
ENV PATH=${NODE_HOME}/bin:${PATH}
ENV BASH_ENV=/etc/profile.d/jdk-env.sh
ENV MYSQL_PORT=3306
ENV MYSQL_USER=root
ENV MYSQL_PASSWORD=root
ENV MYSQL_DATADIR=/data/mysql
ENV MYSQL_RUN_DIR=/tmp/mysql

# 补齐基础镜像缺失的运行依赖：
# - nvm安装脚本需要 awk
# - mysqld需要 libaio/libnuma
# - embedded MongoDB 4.4.x需要 OpenSSL 1.1 兼容库
RUN dnf install -y \
        gawk \
        libaio \
        numactl-libs \
        compat-openssl11 && \
    dnf clean all

COPY --from=git-builder /opt/git /opt/git
RUN ln -sf /opt/git/bin/git /usr/local/bin/git

# 安装jdk8
RUN mkdir -p /data && \
    cd /tmp && \
    curl -fSL "https://github.com/Tencent/TencentKona-8/releases/download/${KONA_JDK8_TAG}/${KONA_JDK8_PACKAGE}" -o kona8.tar.gz && \
    KONA_DIR="$(tar -tzf kona8.tar.gz | head -1 | cut -d/ -f1)" && \
    tar -xzf kona8.tar.gz -C /data && \
    rm -rf "${JAVA8_HOME}" && \
    mv "/data/${KONA_DIR}" "${JAVA8_HOME}" && \
    rm -f /tmp/kona8.tar.gz

# 通过nvm安装固定版本nodejs
RUN mkdir -p "${NVM_DIR}" && \
    cd /tmp && \
    curl -fSL "https://github.com/nvm-sh/nvm/archive/refs/tags/v${NVM_VERSION}.tar.gz" -o nvm.tar.gz && \
    tar -xzf nvm.tar.gz -C "${NVM_DIR}" --strip-components=1 && \
    rm -f /tmp/nvm.tar.gz && \
    . "${NVM_DIR}/nvm.sh" && \
    nvm install "${NODE_VERSION}" && \
    nvm alias default "${NODE_VERSION}" && \
    nvm cache clear

COPY jdk-env.sh /etc/profile.d/jdk-env.sh
COPY start-mysql.sh /usr/local/bin/start-mysql
RUN chmod 644 /etc/profile.d/jdk-env.sh && \
    chmod +x /usr/local/bin/start-mysql
