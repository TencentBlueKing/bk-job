FROM bkjob/os:0.0.2

LABEL maintainer="Tencent BlueKing Job"
LABEL dockerfile.version="0.0.1"

## 安装JDK
RUN mkdir -p /data && \
    cd /data/ &&\
    curl -OL https://github.com/Tencent/TencentKona-8/releases/download/8.0.3-GA/TencentKona8.0.3.b2_jdk_linux-x86_64_8u262.tar.gz &&\
    tar -xzf TencentKona8.0.3.b2_jdk_linux-x86_64_8u262.tar.gz &&\
    rm -f TencentKona8.0.3.b2_jdk_linux-x86_64_8u262.tar.gz
ENV JAVA_HOME=/data/TencentKona-8.0.3-262
ENV PATH=${JAVA_HOME}/bin:$PATH
ENV CLASSPATH=.:${JAVA_HOME}/lib
## 安装Python
RUN yum install -y epel-release
RUN yum install -y python-pip
RUN mkdir -p /root/.pip
RUN echo -e "[global]\nindex-url = https://pypi.tuna.tsinghua.edu.cn/simple\n[install]\ntrusted-host=pypi.tuna.tsinghua.edu.cn" > /root/.pip/pip.conf
RUN pip install requests==2.6.0
