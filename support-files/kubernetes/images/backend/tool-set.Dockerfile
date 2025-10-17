FROM bkjob/os:0.0.2

LABEL maintainer="Tencent BlueKing Job"
LABEL dockerfile.version="3.12.1"

## 安装MySQL
RUN curl -o mysql-8.4.6-linux-glibc2.17-x86_64-minimal.tar.xz https://cdn.mysql.com//Downloads/MySQL-8.4/mysql-8.4.6-linux-glibc2.17-x86_64-minimal.tar.xz \
    && tar -xvf mysql-8.4.6-linux-glibc2.17-x86_64-minimal.tar.xz \
    && mv mysql-8.4.6-linux-glibc2.17-x86_64-minimal /usr/local/mysql \
    && ln -s /usr/local/mysql/bin/mysql /usr/bin/mysql

## 安装JDK
RUN mkdir -p /data && \
    cd /data/ &&\
    curl -OL https://github.com/Tencent/TencentKona-8/releases/download/8.0.21-GA/TencentKona8.0.21.b1_jdk_linux-x86_64_8u442.tar.gz &&\
    tar -xzf TencentKona8.0.21.b1_jdk_linux-x86_64_8u442.tar.gz &&\
    rm -f TencentKona8.0.21.b1_jdk_linux-x86_64_8u442.tar.gz
ENV JAVA_HOME=/data/TencentKona-8.0.21-442
ENV PATH=${JAVA_HOME}/bin:$PATH
ENV CLASSPATH=.:${JAVA_HOME}/lib

# 软件与镜像源准备
# 备份原有仓库配置
RUN mv /etc/yum.repos.d/CentOS-Base.repo /etc/yum.repos.d/CentOS-Base.repo.backup
# 下载腾讯云镜像源
RUN curl -o /etc/yum.repos.d/CentOS-Base.repo https://mirrors.tencent.com/repo/centos7_base.repo
# 安装epel并更新epel源为腾讯云源
RUN yum install -y epel-release \
    && sed -i 's|^#baseurl=|baseurl=|' /etc/yum.repos.d/epel.repo \
    && sed -i 's|^metalink=|#metalink=|' /etc/yum.repos.d/epel.repo \
    && sed -i 's|^baseurl=.*epel.*|baseurl=http://mirrors.tencentyun.com/epel/$releasever/$basearch/|' /etc/yum.repos.d/epel.repo
# 清理缓存
RUN yum clean all

## 安装Python
RUN yum install -y python36
RUN mkdir -p /root/.pip
RUN echo -e "[global]\nindex-url = https://mirrors.tencent.com/pypi/simple\n[install]\ntrusted-host=mirrors.tencent.com" > /root/.pip/pip.conf
RUN pip3 install requests==2.27.1
