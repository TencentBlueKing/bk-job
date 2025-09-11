FROM bkjob/jdk:3.10.0

LABEL maintainer="Tencent BlueKing Job"
LABEL dockerfile.version="3.10.0"

## 安装Python
RUN dnf install -y \
        mysql \
        python3-pip && \
    dnf clean all && \
    rm -rf /var/cache/dnf

RUN mkdir -p /root/.pip && \
    printf '[global]\nindex-url = https://mirrors.tencent.com/pypi/simple/' > /root/.pip/pip.conf && \
    pip3 install requests
