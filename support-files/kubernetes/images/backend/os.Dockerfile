FROM tencentos/tencentos4-minimal

LABEL maintainer="Tencent BlueKing Job"
LABEL dockerfile.version="0.0.3"

ENV LANG="en_US.UTF-8"

# 操作系统相关
RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime &&\
    echo 'alias ls="ls --color=auto"' >> ~/.bashrc &&\
    echo 'alias ll="ls -l"' >> ~/.bashrc &&\
    echo 'alias tailf="tail -f"' >> ~/.bashrc

# 安装软件
RUN dnf install -y \
        vim \
        less \
        wget \
        lrzsz \
        net-tools \
        bind-utils && \
    dnf clean all