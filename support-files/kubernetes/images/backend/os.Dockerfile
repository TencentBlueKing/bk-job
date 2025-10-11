FROM tencentos/tencentos4-minimal:4.4-v20250805

LABEL maintainer="Tencent BlueKing Job"
LABEL dockerfile.version="3.11.11"

ENV LANG="en_US.UTF-8"

# 操作系统相关
RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime &&\
    echo 'alias ls="ls --color=auto"' >> ~/.bashrc &&\
    echo 'alias ll="ls -l"' >> ~/.bashrc &&\
    echo 'alias tailf="tail -f"' >> ~/.bashrc

# 安装软件
RUN dnf install -y \
        procps \
        xz \
        vim \
        less \
        wget \
        lrzsz \
        iputils \
        telnet \
        net-tools \
        bind-utils && \
    dnf clean all
