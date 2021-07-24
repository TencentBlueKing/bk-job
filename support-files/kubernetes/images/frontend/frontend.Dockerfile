FROM nginx:1.18.0

LABEL maintainer="Tencent BlueKing Job"

WORKDIR /data/job/job-frontend
COPY dist/index.html /data/job/job-frontend/
COPY dist/static/ /data/job/job-frontend/static
COPY dist/js/ /data/job/job-frontend/js

RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone && \
    mkdir -p /data/job/logs/job-frontend/nginx
