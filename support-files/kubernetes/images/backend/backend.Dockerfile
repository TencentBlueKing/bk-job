FROM bkjob/jdk:0.0.3

LABEL maintainer="Tencent BlueKing Job"
LABEL dockerfile.version="0.0.1"

ENV BK_JOB_HOME=/data/job/exec

COPY ./ /data/job/exec/
RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone && \
    chmod +x /data/job/exec/startup.sh && \
    chmod +x /data/job/exec/tini


ENV LANG en_US.utf8
ENV LANGUAGE en_US.utf8
ENV LC_ALL en_US.utf8

WORKDIR /data/job/exec

ENTRYPOINT ["./tini", "--", "/data/job/exec/startup.sh"]
