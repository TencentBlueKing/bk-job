FROM bkjob/os:3.12.2

ARG KONA_JDK17_SHA256=ffa817d8234e3658f9120356ee097fc1397f8b581d206e5a1b725f1ef57318c0
ARG ARTHAS_SHA256=231f1d4f8ceb1c0ff8ece67b117b107ddaad7bec1765c15ffe1b77d7fdf0f19b

LABEL maintainer="Tencent BlueKing Job"
LABEL dockerfile.version="3.12.7"

RUN mkdir -p /bk_job_data && \
    cd /bk_job_data/ &&\
    curl -OL https://github.com/Tencent/TencentKona-17/releases/download/TencentKona-17.0.21.1/TencentKona-17.0.21.1.b1-jdk_linux-x86_64.tar.gz &&\
    echo "${KONA_JDK17_SHA256}  TencentKona-17.0.21.1.b1-jdk_linux-x86_64.tar.gz" | sha256sum -c - &&\
    tar -xzf TencentKona-17.0.21.1.b1-jdk_linux-x86_64.tar.gz &&\
    rm -f TencentKona-17.0.21.1.b1-jdk_linux-x86_64.tar.gz
ENV JAVA_HOME=/bk_job_data/TencentKona-17.0.21.1.b1
ENV PATH=${JAVA_HOME}/bin:$PATH
ENV CLASSPATH=.:${JAVA_HOME}/lib

## 安装在线诊断工具arthas
RUN mkdir -p /bk_job_data/tools && \
    cd /bk_job_data/tools && \
    curl -OL https://github.com/alibaba/arthas/releases/download/arthas-all-4.0.5/arthas-bin.zip && \
    echo "${ARTHAS_SHA256}  arthas-bin.zip" | sha256sum -c - && \
    dnf install -y unzip && \
    unzip arthas-bin.zip arthas-boot.jar && \
    rm -rf arthas-bin* && \
    echo 'alias arthas="java -jar /bk_job_data/tools/arthas-boot.jar --repo-mirror aliyun --use-http"' >> ~/.bashrc
