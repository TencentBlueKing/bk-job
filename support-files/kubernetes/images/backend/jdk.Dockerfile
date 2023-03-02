FROM bkjob/os:0.0.1

LABEL maintainer="Tencent BlueKing Job"

## 安装JDK
RUN mkdir -p /data && \
    cd /data/ &&\
    curl -OL https://github.com/Tencent/TencentKona-8/releases/download/8.0.13-GA/TencentKona8.0.13.b1_jdk_linux-x86_64_8u362.tar.gz &&\
    tar -xzf TencentKona8.0.13.b1_jdk_linux-x86_64_8u362.tar.gz &&\
    rm -f TencentKona8.0.13.b1_jdk_linux-x86_64_8u362.tar.gz
ENV JAVA_HOME=/data/TencentKona-8.0.13-362
ENV PATH=${JAVA_HOME}/bin:$PATH
ENV CLASSPATH=.:${JAVA_HOME}/lib