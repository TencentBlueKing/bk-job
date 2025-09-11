FROM bkjob/tool-set:0.0.2

LABEL maintainer="Tencent BlueKing Job"
LABEL dockerfile.version="0.0.2"

ENV BK_JOB_HOME=/data/job/exec

COPY ./ /data/job/exec/

RUN dnf -y install mysql && \
    dnf clean all && \
    rm -rf /var/cache/dnf
    
RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone && \
    chmod +x /data/job/exec/runUpgrader.sh && \
    chmod +x /data/job/exec/startup.sh

ENV LANG en_US.utf8 \
    LANGUAGE en_US.utf8 \
    LC_ALL en_US.utf8

WORKDIR /data/job/exec
CMD /data/job/exec/startup.sh
