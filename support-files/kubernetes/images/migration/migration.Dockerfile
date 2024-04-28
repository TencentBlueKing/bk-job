FROM bkjob/jdk:0.0.3

LABEL maintainer="Tencent BlueKing Job"

ENV BK_JOB_HOME=/data/job/exec

COPY ./ /data/job/exec/

RUN yum -y install mysql
RUN yum install -y epel-release
RUN yum install -y python-pip
RUN mkdir -p /root/.pip
RUN echo -e "[global]\nindex-url = https://pypi.tuna.tsinghua.edu.cn/simple\n[install]\ntrusted-host=pypi.tuna.tsinghua.edu.cn" > /root/.pip/pip.conf
RUN pip install requests==2.6.0
RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone && \
    chmod +x /data/job/exec/runUpgrader.sh && \
    chmod +x /data/job/exec/startup.sh

ENV LANG en_US.utf8
ENV LANGUAGE en_US.utf8
ENV LC_ALL en_US.utf8

WORKDIR /data/job/exec
CMD /data/job/exec/startup.sh
