ARG BASE_IMAGE=bkjob/tool-set:3.12.6

# git编译阶段,该阶段会安装gcc/make等编译依赖，不打入最终镜像
FROM ${BASE_IMAGE} AS git-builder

ARG GIT_VERSION=2.41.3
ARG GIT_SHA256=baa39125deee194b440ac7d0138f6c34f0d87ceb1cb9da5b2becf704f45b0819

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
    echo "${GIT_SHA256}  git.tar.xz" | sha256sum -c - && \
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
ARG KONA_JDK8_SHA256=9df50dd8e888ac62721ddd18400194f190bd91b8f3a1e1d782f710fc0eab2478
ARG NODE_VERSION=24.16.0
ARG NVM_VERSION=0.40.3
ARG NVM_SHA256=5f4d6aaa04a177dc93c985e31dbc411ab6b8c6e1e21d8015dbc1372625fcd1d0

LABEL maintainer="Tencent BlueKing Job" \
      dockerfile.version="1.0.1"

ENV JAVA17_HOME=/bk_job_data/TencentKona-17.0.16.b1
ENV KONA_JDK17_HOME=/bk_job_data/TencentKona-17.0.16.b1
ENV JAVA8_HOME=/bk_job_data/TencentKona8
ENV KONA_JDK8_HOME=/bk_job_data/TencentKona8
ENV NVM_DIR=/usr/local/nvm
ENV NODE_HOME=/usr/local/nvm/versions/node/v${NODE_VERSION}
ENV PATH=${NODE_HOME}/bin:${PATH}
ENV BASH_ENV=/etc/profile.d/jdk-env.sh
ENV MYSQL_PORT=3306
ENV MYSQL_USER=root
ENV MYSQL_PASSWORD=root
ENV MYSQL_DATADIR=/bk_job_data/mysql
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
RUN mkdir -p /bk_job_data && \
    cd /tmp && \
    curl -fSL "https://github.com/Tencent/TencentKona-8/releases/download/${KONA_JDK8_TAG}/${KONA_JDK8_PACKAGE}" -o kona8.tar.gz && \
    echo "${KONA_JDK8_SHA256}  kona8.tar.gz" | sha256sum -c - && \
    KONA_DIR="$(tar -tzf kona8.tar.gz | head -1 | cut -d/ -f1)" && \
    tar -xzf kona8.tar.gz -C /bk_job_data && \
    rm -rf "${JAVA8_HOME}" && \
    mv "/bk_job_data/${KONA_DIR}" "${JAVA8_HOME}" && \
    rm -f /tmp/kona8.tar.gz

# 通过nvm安装固定版本nodejs
RUN mkdir -p "${NVM_DIR}" && \
    cd /tmp && \
    curl -fSL "https://github.com/nvm-sh/nvm/archive/refs/tags/v${NVM_VERSION}.tar.gz" -o nvm.tar.gz && \
    echo "${NVM_SHA256}  nvm.tar.gz" | sha256sum -c - && \
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
