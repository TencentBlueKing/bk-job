FROM bkjob/jdk17:3.12.4

ARG MYSQL_SHA256=1e9f341e36d4fcdbb3e4d7d3dae6d6b2a42ebe8333e8f276c25e25564ce565c1

LABEL maintainer="Tencent BlueKing Job"
LABEL dockerfile.version="3.12.6"

## 安装MySQL与兼容库
RUN curl -o mysql-8.4.9-linux-glibc2.17-x86_64-minimal.tar.xz https://cdn.mysql.com/Downloads/MySQL-8.4/mysql-8.4.9-linux-glibc2.17-x86_64-minimal.tar.xz \
    && echo "${MYSQL_SHA256}  mysql-8.4.9-linux-glibc2.17-x86_64-minimal.tar.xz" | sha256sum -c - \
    && tar -xvf mysql-8.4.9-linux-glibc2.17-x86_64-minimal.tar.xz \
    && mv mysql-8.4.9-linux-glibc2.17-x86_64-minimal /usr/local/mysql \
    && ln -s /usr/local/mysql/bin/mysql /usr/bin/mysql \
    && dnf install -y ncurses-compat-libs \
    && dnf clean all

## 安装pip与requests库
RUN dnf install -y python-pip
RUN mkdir -p /root/.pip
RUN echo -e "[global]\nindex-url = https://mirrors.tencent.com/pypi/simple\n[install]\ntrusted-host=mirrors.tencent.com" > /root/.pip/pip.conf
RUN pip3 install requests==2.27.1
