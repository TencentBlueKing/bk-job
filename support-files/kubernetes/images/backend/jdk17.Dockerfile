FROM bkjob/os:0.0.3

LABEL maintainer="Tencent BlueKing Job"
LABEL dockerfile.version="0.0.1"

RUN mkdir -p /data && \
    cd /data/ &&\
    curl -OL https://github.com/Tencent/TencentKona-17/releases/download/TencentKona-17.0.8/TencentKona-17.0.8.b1-jdk_linux-x86_64.tar.gz &&\
    tar -xzf TencentKona-17.0.8.b1-jdk_linux-x86_64.tar.gz &&\
    rm -f TencentKona-17.0.8.b1-jdk_linux-x86_64.tar.gz
ENV JAVA_HOME=/data/TencentKona-17.0.8.b1
ENV PATH=${JAVA_HOME}/bin:$PATH
ENV CLASSPATH=.:${JAVA_HOME}/lib
