FROM bkjob/jdk:0.0.6

LABEL maintainer="Tencent BlueKing Job"
LABEL dockerfile.version="0.0.2"

ENV BK_JOB_HOME=/data/job/exec

COPY ./ /data/job/exec/
RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone && \
    chmod +x /data/job/exec/startup.sh && \
    chmod +x /data/job/exec/tini

# 设置快捷命令
RUN echo "alias log='cd /data/logs;cd $(ls|grep job-)'" >> ~/.bashrc && \
    echo "alias bin='cd /data/job/exec'" >> ~/.bashrc

ENV LANG en_US.utf8
ENV LANGUAGE en_US.utf8
ENV LC_ALL en_US.utf8

WORKDIR /data/job/exec

ENTRYPOINT ["./tini", "--", "/data/job/exec/startup.sh"]
