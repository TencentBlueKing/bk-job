FROM tencentos/tencentos4-minimal

LABEL maintainer="Tencent BlueKing Job"

RUN dnf -y install nginx && \
    dnf clean all && \
    rm -rf /var/cache/dnf

WORKDIR /data/job/job-frontend
COPY dist/index.html /data/job/job-frontend/
COPY dist/static/ /data/job/job-frontend/static
COPY dist/js/ /data/job/job-frontend/js

RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone && \
    mkdir -p /data/job/logs/job-frontend/nginx