FROM nginx:1.18.0

LABEL maintainer="Tencent BlueKing Job"

WORKDIR /data/job/job-frontend
COPY dist/index.html /data/job/job-frontend/
COPY dist/static/ /data/job/job-frontend/static

RUN ln -snf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone && \
    mkdir -p /data/job/logs/frontend/nginx
