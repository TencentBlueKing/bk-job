FROM bkjob/os:3.11.11

LABEL maintainer="Tencent BlueKing Job"
LABEL dockerfile.version="3.11.11"

RUN mkdir -p /data && \
    cd /data/ &&\
    curl -OL https://github.com/Tencent/TencentKona-17/releases/download/TencentKona-17.0.16/TencentKona-17.0.16.b1-jdk_linux-x86_64.tar.gz &&\
    tar -xzf TencentKona-17.0.16.b1-jdk_linux-x86_64.tar.gz &&\
    rm -f TencentKona-17.0.16.b1-jdk_linux-x86_64.tar.gz
ENV JAVA_HOME=/data/TencentKona-17.0.16.b1
ENV PATH=${JAVA_HOME}/bin:$PATH
ENV CLASSPATH=.:${JAVA_HOME}/lib

## 安装在线诊断工具arthas
RUN mkdir -p /data/tools && \
    cd /data/tools && \
    curl -OL https://github.com/alibaba/arthas/releases/download/arthas-all-4.0.5/arthas-bin.zip && \
    dnf install -y unzip && \
    unzip arthas-bin.zip arthas-boot.jar && \
    rm -rf arthas-bin* && \
    echo 'alias arthas="java -jar /data/tools/arthas-boot.jar"' >> ~/.bashrc
