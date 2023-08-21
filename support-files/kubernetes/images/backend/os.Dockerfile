FROM centos:7

LABEL maintainer="Tencent BlueKing Job"

ENV LANG="en_US.UTF-8"

# 操作系统相关
RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime &&\
    echo 'alias ls="ls --color=auto"' >> ~/.bashrc &&\
    echo 'alias ll="ls -l"' >> ~/.bashrc &&\
    echo 'alias tailf="tail -f"' >> ~/.bashrc

# 安装软件
RUN yum install -y vim && \
    yum install -y less && \
    yum install -y wget && \
    yum install -y lrzsz && \
    yum install -y net-tools.x86_64 && \
    yum install -y bind-utils
