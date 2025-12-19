FROM bkjob/jdk17:3.11.11

LABEL maintainer="Tencent BlueKing Job"
LABEL dockerfile.version="1.0.0"

ARG MODULE=bk-job-op

ENV JOB_OP_HOME=/data/job/exec
ENV JOB_OP_APP_NAME=${MODULE}
ENV JOB_OP_JAR=${MODULE}.jar

COPY ./ /data/job/exec/

RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone && \
    chmod +x /data/job/exec/startup.sh

# 设置快捷命令
RUN echo "alias log='cd /data/logs;cd \$(ls|grep job-)'" >> ~/.bashrc && \
    echo "alias bin='cd /data/job/exec'" >> ~/.bashrc

ENV LANG en_US.utf8
ENV LANGUAGE en_US.utf8
ENV LC_ALL en_US.utf8

WORKDIR /data/job/exec

ENTRYPOINT ["/data/job/exec/startup.sh"]
