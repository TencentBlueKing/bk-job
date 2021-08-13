FROM blueking/jdk:0.0.1

LABEL maintainer="Tencent BlueKing Job"

ENV BK_JOB_HOME=/data/job \
    BK_JOB_LOGS_DIR=/data/job/logs

COPY ./ /data/job/

RUN yum -y install mysql

RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone && \
    chmod +x /data/job/startup.sh
WORKDIR /data/job
CMD /data/job/startup.sh
