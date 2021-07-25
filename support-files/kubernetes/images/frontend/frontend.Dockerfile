FROM nginx:1.18.0

LABEL maintainer="Tencent BlueKing Job"

WORKDIR /data/job/job-frontend
COPY dist/index.html /data/job/job-frontend/
COPY dist/static/ /data/job/job-frontend/static
