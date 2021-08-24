#!/usr/bin/env bash
# Description: build and push docker image

# Safe mode
set -euo pipefail 

PROGRAM=$(basename "$0")
EXITCODE=0

BUILD_ALL=1
BUILD_FRONTEND=0
BUILD_BACKEND=0
BUILD_MIGRATION=0
BUILD_MODULES=()
VERSION=latest
PUSH=0
REGISTRY=docker.io
USERNAME=
PASSWORD=
BACKENDS=(job-gateway job-manage job-execute job-crontab job-logsvr job-analysis job-backup job-file-gateway job-file-worker)
MYSQL_URL=
MYSQL_USERNAME=
MYSQL_PASSWORD=
MAVEN_REPO_URL=

cd $(dirname $0)
WORKING_DIR=$(pwd)
ROOT_DIR=${WORKING_DIR%/*/*/*}
BACKEND_DIR=$ROOT_DIR/src/backend
FRONTEND_DIR=$ROOT_DIR/src/frontend
SUPPORT_FILES_DIR=$ROOT_DIR/support-files
VERSION_LOGS_DIR=$ROOT_DIR/versionLogs

usage () {
    cat <<EOF
Usage:
    $PROGRAM [OPTIONS]... 

            [ --frontend            [Optional] Build frontend image ]
            [ --backend             [Optional] Build backend image ]
            [ --migration           [Optional] Build migration image ]
			      [ --modules             [Optional] Build specified module images, modules are separated by commas.
 values:job-gateway,job-manage,job-execute,job-crontab,job-logsvr,job-analysis,job-backup,job-frontend. Example: job-manage,job-execute ]
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

warning () {
    echo "$@" 1>&2
    EXITCODE=$((EXITCODE + 1))
}

# Parse command line
(( $# == 0 )) && usage_and_exit 1
while (( $# > 0 )); do 
    case "$1" in
        --frontend )
            BUILD_ALL=0
            BUILD_FRONTEND=1
            ;;
        --backend )
            BUILD_ALL=0
            BUILD_BACKEND=1
            ;;
        --migration )
            BUILD_ALL=0
            BUILD_MIGRATION=1
            ;;
        --modules )
		        shift
            BUILD_ALL=0
			      BUILD_FRONTEND=0
            BUILD_BACKEND=0
			modules_str=$1
			BUILD_MODULES=(${modules_str//,/ })
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
build_frontend_module () {
# Build frontend image
    log "Building frontend image..."
    cd $FRONTEND_DIR || exit 1
    npm i
    npm run build
    cd $WORKING_DIR || exit 1

    rm -rf tmp/*
    cp -rf $FRONTEND_DIR/dist tmp/
    log "Building version logs"
    cd $VERSION_LOGS_DIR || exit 1
    python genBundledVersionLog.py
    cd $ROOT_DIR || exit 1
    cp versionLogs/bundledVersionLog*.json tmp/dist/static

    docker build -f frontend/frontend.Dockerfile -t $REGISTRY/job-frontend:$VERSION tmp --network=host
    if [[ $PUSH -eq 1 ]] ; then
        docker push $REGISTRY/job-frontend:$VERSION
    fi
}
if [[ $BUILD_ALL -eq 1 || $BUILD_FRONTEND -eq 1 ]] ; then
    build_frontend_module
fi

# Build backend image
build_backend_module () {
    SERVICE=$1
    log "Building ${SERVICE} image, version: ${VERSION}..."
    if [[ ${SERVICE} == "job-gateway" ]] ; then
      $BACKEND_DIR/gradlew -p $BACKEND_DIR clean :$SERVICE:build -DassemblyMode=k8s -DmysqlURL=$MYSQL_URL -DmysqlUser=$MYSQL_USERNAME -DmysqlPasswd=$MYSQL_PASSWORD -DmavenRepoUrl=$MAVEN_REPO_URL -DbkjobVersion=$VERSION
    else
      $BACKEND_DIR/gradlew -p $BACKEND_DIR clean :$SERVICE:boot-$SERVICE:build -DassemblyMode=k8s -DmysqlURL=$MYSQL_URL -DmysqlUser=$MYSQL_USERNAME -DmysqlPasswd=$MYSQL_PASSWORD -DmavenRepoUrl=$MAVEN_REPO_URL -DbkjobVersion=$VERSION
    fi
    rm -rf tmp/*
    cp backend/startup.sh tmp/
    cp $BACKEND_DIR/release/$SERVICE-$VERSION.jar tmp/$SERVICE.jar
    docker build -f backend/backend.Dockerfile -t $REGISTRY/$SERVICE:$VERSION tmp --network=host
    if [[ $PUSH -eq 1 ]] ; then
        docker push $REGISTRY/$SERVICE:$VERSION
    fi
}

# Build migration image
build_migration_image(){
    log "Building migration image, version: ${VERSION}..."
    rm -rf tmp/*
    cp migration/startup.sh tmp/
    cp -r $SUPPORT_FILES_DIR/sql tmp/
    docker build -f migration/migration.Dockerfile -t $REGISTRY/job-migration:$VERSION tmp --network=host
    if [[ $PUSH -eq 1 ]] ; then
        docker push $REGISTRY/job-migration:$VERSION
    fi
}
if [[ $BUILD_ALL -eq 1 || $BUILD_MIGRATION -eq 1 ]] ; then
    build_migration_image
fi

if [[ $BUILD_ALL -eq 1 || $BUILD_BACKEND -eq 1 ]] ; then
    for SERVICE in ${BACKENDS[@]};
    do
        build_backend_module $SERVICE
    done
fi


if [[ ${#BUILD_MODULES[@]} -ne 0 ]]; then
    log "Build ${BUILD_MODULES[@]}"
    for SERVICE in ${BUILD_MODULES[@]};
	do
	    log "$SERVICE"
	    if [[ "$SERVICE" == "job-frontend" ]]; then
		    build_frontend_module
		else
		    build_backend_module $SERVICE
		fi
	done
fi

echo "BUILD SUCCESSFUL!"
