FROM bkjob/jdk:3.10.0

LABEL maintainer="Tencent BlueKing Job"
LABEL dockerfile.version="0.0.2"

## 安装Python
RUN dnf install -y \
        mysql \
        python3-pip && \
    dnf clean all && \
    rm -rf /var/cache/dnf

RUN mkdir -p /root/.pip && \
    echo -e "[global]\nindex-url = https://pypi.tuna.tsinghua.edu.cn/simple\n[install]\ntrusted-host=pypi.tuna.tsinghua.edu.cn" > /root/.pip/pip.conf && \
    pip3 install requests
