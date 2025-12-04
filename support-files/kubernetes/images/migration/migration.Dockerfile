FROM bkjob/tool-set:3.12.3

LABEL maintainer="Tencent BlueKing Job"
LABEL dockerfile.version="3.12.3"

ENV BK_JOB_HOME=/data/job/exec

COPY ./ /data/job/exec/

RUN ln -snf /usr/share/zoneinfo/UTC /etc/localtime && \
    echo 'UTC' > /etc/timezone && \
    chmod +x /data/job/exec/runUpgrader.sh && \
    chmod +x /data/job/exec/startup.sh

ENV LANG en_US.utf8
ENV LANGUAGE en_US.utf8
ENV LC_ALL en_US.utf8

WORKDIR /data/job/exec
CMD /data/job/exec/startup.sh
