#!/usr/bin/env bash
# Description: build and push docker image

# Safe mode
set -euo pipefail 

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
MYSQL_URL=
MYSQL_USER=
MYSQL_PASSWORD=
MAVEN_REPO_URL=

cd $(dirname $0)
WORKING_DIR=$(pwd)
ROOT_DIR=${WORKING_DIR%/*/*/*}
BACKEND_DIR=$ROOT_DIR/src/backend
FRONTEND_DIR=$ROOT_DIR/src/frontend

usage () {
    cat <<EOF
Usage:
    $PROGRAM [OPTIONS]... 

            [ --frontend            [Optional] Package the frontend image ]
            [ --backend             [Optional] Package the backend image ]
            [ -v, --version         [Optional] Image tag, default latest ]
            [ -p, --push            [Optional] Push the image to the docker remote repository, not push by default ]
            [ -r, --registry        [Optional] docker repository, default docker.io ]
            [ --username            [Optional] docker repository username ]
            [ --password            [Optional] docker repository password ]
            [ --mysql_url           [Optional] MySQL url used for backend compilation, for example 127.0.0.1:3306 ]
            [ --mysql_username      [Optional] MySQL username used for backend compilation ]
            [ --mysql_password      [Optional] MySQL password used for backend compilation ]
            [ --maven_repo_url      [Optional] Maven repository url used for backend compilation, example: https://repo.maven.apache.org/maven2/ ]
            [ -h, --help            [Optional] Show help ]
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

# Parse command line
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
        --mysql_url )
            shift
            MYSQL_URL=$1
            ;;
        --mysql_username )
            shift
            MYSQL_USERNAME=$1
            ;;
        --mysql_password )
            shift
            MYSQL_PASSWORD=$1
            ;;
        --maven_repo_url )
            shift
            MAVEN_REPO_URL=$1
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

# Create tmp dir
mkdir -p $WORKING_DIR/tmp
tmp_dir=$WORKING_DIR/tmp
# Automatically clean up the tmp directory when executing exit
trap 'rm -rf $tmp_dir' EXIT TERM

# Build frontend image
if [[ $ALL -eq 1 || $FRONTEND -eq 1 ]] ; then
    # Build frontend image
    log "Building frontend image..."
    cd $FRONTEND_DIR || exit 1
    npm i
    npm run build
    cd $WORKING_DIR || exit 1

    rm -rf tmp/*
    cp -rf $FRONTEND_DIR/dist/ tmp/

    docker build -f frontend/frontend.Dockerfile -t $REGISTRY/job-frontend:$VERSION tmp --network=host
    if [[ $PUSH -eq 1 ]] ; then
        docker push $REGISTRY/job-frontend:$VERSION
    fi
fi

# Build backend image
if [[ $ALL -eq 1 || $BACKEND -eq 1 ]] ; then
    for SERVICE in ${BACKENDS[@]};
    do
        log "Building ${SERVICE} image..."
        log "$MYSQL_URL"
        log "$MYSQL_USERNAME"
        log "$MYSQL_PASSWORD"
        if [[ ${SERVICE} == "job-gateway" ]] ; then
          $BACKEND_DIR/gradlew -p $BACKEND_DIR :$SERVICE:build -DassemblyMode=k8s -DmysqlURL=$MYSQL_URL -DmysqlUser=$MYSQL_USER -DmysqlPasswd=$MYSQL_PASSWORD -DmavenRepoUrl=$MAVEN_REPO_URL
        else
          $BACKEND_DIR/gradlew -p $BACKEND_DIR :$SERVICE:boot-$SERVICE:build -DassemblyMode=k8s -DmysqlURL=$MYSQL_URL -DmysqlUser=$MYSQL_USERNAME -DmysqlPasswd=$MYSQL_PASSWORD -DmavenRepoUrl=$MAVEN_REPO_URL
        fi
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
