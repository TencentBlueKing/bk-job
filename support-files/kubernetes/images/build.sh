#!/usr/bin/env bash
# 用途：构建并推送docker镜像

# 安全模式
set -euo pipefail 

# 通用脚本框架变量
PROGRAM=$(basename "$0")
EXITCODE=0

ALL=1
FRONTEND=0
BACKEND=0
VERSION=latest
PUSH=0
REGISTRY=docker.io
USERNAME=
PASSWORD=
BACKENDS=(job-gateway job-manage job-execute job-crontab job-logsvr job-analysis job-backup)

cd $(dirname $0)
WORKING_DIR=$(pwd)
ROOT_DIR=${WORKING_DIR%/*/*/*}
BACKEND_DIR=$ROOT_DIR/src/backend
FRONTEND_DIR=$ROOT_DIR/src/frontend

usage () {
    cat <<EOF
用法: 
    $PROGRAM [OPTIONS]... 

            [ --frontend            [可选] 打包frontend镜像 ]
            [ --backend             [可选] 打包backend镜像 ]
            [ -v, --version         [可选] 镜像版本tag, 默认latest ]
            [ -p, --push            [可选] 推送镜像到docker远程仓库，默认不推送 ]
            [ -r, --registry        [可选] docker仓库地址, 默认docker.io ]
            [ --username            [可选] docker仓库用户名 ]
            [ --password            [可选] docker仓库密码 ]
            [ -h, --help            [可选] 查看脚本帮助 ]
EOF
}

usage_and_exit () {
    usage
    exit "$1"
}

log () {
    echo "$@"
}

error () {
    echo "$@" 1>&2
    usage_and_exit 1
}

warning () {
    echo "$@" 1>&2
    EXITCODE=$((EXITCODE + 1))
}

# 解析命令行参数，长短混合模式
(( $# == 0 )) && usage_and_exit 1
while (( $# > 0 )); do 
    case "$1" in
        --frontend )
            ALL=0
            FRONTEND=1
            ;;
        --backend )
            ALL=0
            BACKEND=1
            ;;
        -v | --version )
            shift
            VERSION=$1
            ;;
        -p | --push )
            PUSH=1
            ;;
        -r | --registry )
            shift
            REGISTRY=$1
            ;;
        --username )
            shift
            USERNAME=$1
            ;;
        --password )
            shift
            PASSWORD=$1
            ;;
        --help | -h | '-?' )
            usage_and_exit 0
            ;;
        -*)
            error "Invalid Param: $1"
            ;;
        *) 
            break
            ;;
    esac
    shift
done

if [[ $PUSH -eq 1 && -n "$USERNAME" ]] ; then
    docker login --username $USERNAME --password $PASSWORD $REGISTRY
    log "docker login successfully"
fi

# 创建临时目录
mkdir -p $WORKING_DIR/tmp
tmp_dir=$WORKING_DIR/tmp
# 执行退出时自动清理tmp目录
trap 'rm -rf $tmp_dir' EXIT TERM

# 编译frontend
if [[ $ALL -eq 1 || $FRONTEND -eq 1 ]] ; then
    # 打包frontend镜像
    log "Building frontend image..."
    cd $FRONTEND_DIR || exit 1
    npm i
    npm run build
    rm -rf tmp/*
    cp -rf dist/ tmp/

    docker build -f frontend/frontend.Dockerfile -t $REGISTRY/job-frontend:$VERSION tmp --network=host
    if [[ $PUSH -eq 1 ]] ; then
        docker push $REGISTRY/job-frontend:$VERSION
    fi
fi

# 构建backend镜像
if [[ $ALL -eq 1 || $BACKEND -eq 1 ]] ; then
    for SERVICE in ${BACKENDS[@]};
    do
        log "Building ${SERVICE} image..."
        $BACKEND_DIR/gradlew -p $BACKEND_DIR :$SERVICE:boot-$SERVICE:build -PassemblyMode=k8s
        rm -rf tmp/*
        cp backend/startup.sh tmp/
        cp $BACKEND_DIR/release/$SERVICE-*.jar tmp/
        docker build -f backend/backend.Dockerfile -t $REGISTRY/$SERVICE:$VERSION tmp --network=host
        if [[ $PUSH -eq 1 ]] ; then
            docker push $REGISTRY/$SERVICE:$VERSION
        fi
    done
fi

echo "BUILD SUCCESSFUL!"
