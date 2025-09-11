FROM blueking/jdk:0.0.5

LABEL maintainer="Tencent BlueKing Job"

ENV BK_JOB_HOME=/data/job/exec

COPY ./ /data/job/exec/

RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone && \
    chmod +x /data/job/exec/startup.sh

ENV LANG en_US.utf8 && \
    LANGUAGE en_US.utf8 && \
    LC_ALL en_US.utf8

WORKDIR /data/job/exec
CMD /data/job/exec/startup.sh
