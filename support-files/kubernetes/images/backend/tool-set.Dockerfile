FROM bkjob/os:0.0.2

LABEL maintainer="Tencent BlueKing Job"
LABEL dockerfile.version="3.12.0"

## 安装JDK
RUN mkdir -p /data && \
    cd /data/ &&\
    curl -OL https://github.com/Tencent/TencentKona-8/releases/download/8.0.21-GA/TencentKona8.0.21.b1_jdk_linux-x86_64_8u442.tar.gz &&\
    tar -xzf TencentKona8.0.21.b1_jdk_linux-x86_64_8u442.tar.gz &&\
    rm -f TencentKona8.0.21.b1_jdk_linux-x86_64_8u442.tar.gz
ENV JAVA_HOME=/data/TencentKona-8.0.21-442
ENV PATH=${JAVA_HOME}/bin:$PATH
ENV CLASSPATH=.:${JAVA_HOME}/lib
## 安装Python
RUN yum install -y epel-release
RUN yum install -y python36
RUN mkdir -p /root/.pip
RUN echo -e "[global]\nindex-url = https://mirrors.tencent.com/pypi/simple\n[install]\ntrusted-host=mirrors.tencent.com" > /root/.pip/pip.conf
RUN pip3 install requests==2.27.1
