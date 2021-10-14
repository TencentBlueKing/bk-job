FROM blueking/jdk:0.0.1

LABEL maintainer="Tencent BlueKing Job"

ENV BK_JOB_HOME=/data/job \
    BK_JOB_LOGS_DIR=/data/job/logs

COPY ./ /data/job/
RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone && \
    chmod +x /data/job/startup.sh

ENV LANG zh_CN.UTF-8
ENV LANGUAGE zh_CN.UTF-8
ENV LC_ALL zh_CN.UTF-8

WORKDIR /data/job
CMD /data/job/startup.sh
