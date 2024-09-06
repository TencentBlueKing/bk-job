#!/bin/bash -x
# Description: build and push docker image

# Safe mode
set -euo pipefail

PROGRAM=$(basename "$0")
EXITCODE=0

BUILD_ALL=1
BUILD_FRONTEND=0
BUILD_BACKEND=0
BUILD_MIGRATION=0
BUILD_STARTUP_CONTROLLER=0
BUILD_SYNC_BK_API_GATEWAY=0
BUILD_MODULES=()
BUILD_BACKEND_MODULES=()
VERSION=latest
PUSH=0
REGISTRY=docker.io
USERNAME=
PASSWORD=
BACKENDS=(job-gateway job-manage job-execute job-crontab job-logsvr job-analysis job-backup job-file-gateway job-file-worker job-assemble)
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
            [ --startup-controller  [Optional] Build startup-controller image ]
            [ --sync-bk-api-gateway [Optional] Build sync-bk-api-gateway image ]
			[ -m, --modules         [Optional] Build specified module images, modules are separated by commas. values:job-frontend,job-migration,job-gateway,job-manage,job-execute,job-crontab,job-logsvr,job-analysis,job-backup,job-file-gateway,job-file-worker,job-assemble. Example: job-manage,job-execute ]
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
        --sync_bk_api_gateway )
            BUILD_ALL=0
            BUILD_SYNC_BK_API_GATEWAY=1
            ;;
        --startup-controller )
            BUILD_ALL=0
            BUILD_STARTUP_CONTROLLER=1
            ;;
        -m | --modules )
		    shift
            BUILD_ALL=0
			BUILD_FRONTEND=0
            BUILD_BACKEND=0
			BUILD_MIGRATION=0
			BUILD_SYNC_BK_API_GATEWAY=0
			BUILD_STARTUP_CONTROLLER=0
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
tmp_dir=$WORKING_DIR/tmp
mkdir -p $tmp_dir/frontend
mkdir -p $tmp_dir/backend
mkdir -p $tmp_dir/startup_controller
mkdir -p $tmp_dir/migration
mkdir -p $tmp_dir/sync_bk_api_gateway

# Automatically clean up the tmp directory when executing exit
trap 'rm -rf $tmp_dir' EXIT TERM

# Build frontend image
build_frontend_module () {
# Build frontend image
    log "Building frontend image..."
    cd $FRONTEND_DIR || exit 1
    export JOB_VERSION=$VERSION
    echo "JOB_VERSION=${JOB_VERSION}"
    npm i --legacy-peer-deps
    npm run build
    cd $WORKING_DIR || exit 1

    rm -rf $tmp_dir/frontend/*

    echo "=======$FRONTEND_DIR======="
    ls $FRONTEND_DIR
    echo "=======$FRONTEND_DIR  end======="
    echo "=======$tmp_dir======="
    ls $tmp_dir
    echo "=======$tmp_dir  end======="
    cp -rf $FRONTEND_DIR/dist $tmp_dir/frontend/
    log "Building version logs"
    cd $VERSION_LOGS_DIR || exit 1
    python genBundledVersionLog.py
    cd $WORKING_DIR || exit 1
    cp $VERSION_LOGS_DIR/bundledVersionLog*.json tmp/frontend/dist/static

    docker build -f frontend/frontend.Dockerfile -t $REGISTRY/job-frontend:$VERSION tmp/frontend --network=host
    if [[ $PUSH -eq 1 ]] ; then
        docker push $REGISTRY/job-frontend:$VERSION
    fi
}

# Build backend image
build_backend_modules () {
    MODULES=$1
    log "Building backdend {MODULES} image, version: ${VERSION}..."
    tasks=""
    for MODULE in ${MODULES[@]}; do
        if [[ "${MODULE}" == "job-assemble" ]] || [[ "${MODULE}" == "job-gateway" ]]; then
            tasks+=":${MODULE}:build "
        else
            tasks+=":${MODULE}:boot-${MODULE}:build "
        fi
    done
    log "Building backdend modules, gradle tasks: ${tasks}"
    $BACKEND_DIR/gradlew -p $BACKEND_DIR clean ${tasks} -DassemblyMode=k8s -DmysqlURL=$MYSQL_URL -DmysqlUser=$MYSQL_USERNAME -DmysqlPasswd=$MYSQL_PASSWORD -DmavenRepoUrl=$MAVEN_REPO_URL -DbkjobVersion=$VERSION --parallel
    for MODULE in ${MODULES[@]}; do
        rm -rf tmp/backend/*
        cp $BACKEND_DIR/release/$MODULE-$VERSION.jar tmp/backend/$MODULE.jar
        cp backend/startup.sh backend/tini tmp/backend/
        docker build -f backend/backend.Dockerfile -t $REGISTRY/$MODULE:$VERSION tmp/backend --network=host
        if [[ $PUSH -eq 1 ]] ; then
            docker push $REGISTRY/$MODULE:$VERSION
        fi
    done
}

# Build migration image
build_migration_image(){
    log "Building migration image, version: ${VERSION}..."
    $BACKEND_DIR/gradlew -p $BACKEND_DIR/upgrader clean :upgrader:build -DmavenRepoUrl=$MAVEN_REPO_URL -DbkjobVersion=$VERSION
    rm -rf tmp/migration/*
    cp $BACKEND_DIR/release/upgrader-$VERSION.jar tmp/migration/upgrader.jar
    cp migration/startup.sh tmp/migration
    cp migration/runUpgrader.sh tmp/migration
    cp $SUPPORT_FILES_DIR/templates/#etc#job#upgrader#upgrader.properties tmp/migration/upgrader.properties.tpl
    cp -r $SUPPORT_FILES_DIR/bkiam tmp/migration/
    cp -r $SUPPORT_FILES_DIR/sql tmp/migration/
    docker build -f migration/migration.Dockerfile -t $REGISTRY/job-migration:$VERSION tmp/migration --network=host
    if [[ $PUSH -eq 1 ]] ; then
        docker push $REGISTRY/job-migration:$VERSION
    fi
}

# Build sync-bk-api-gateway image
build_sync_bk_api_gateway_image(){
    log "Building sync_bk_api_gateway image, version: ${VERSION}..."
    rm -rf tmp/sync_bk_api_gateway/*
    cp -r $ROOT_DIR/support-files/bk-api-gateway/v3/* tmp/sync_bk_api_gateway/
    docker build -f migration/bkApiGateway.Dockerfile -t $REGISTRY/job-sync-bk-api-gateway:$VERSION tmp/sync_bk_api_gateway --network=host
    if [[ $PUSH -eq 1 ]] ; then
        docker push $REGISTRY/job-sync-bk-api-gateway:$VERSION
    fi
}

# Build startup-controller image
build_startup_controller_image(){
    log "Building startup-controller image, version: ${VERSION}..."
    TOOL_NAME="k8s-startup-controller"
    $BACKEND_DIR/gradlew -p $BACKEND_DIR/job-tools clean :job-tools:$TOOL_NAME:build -DmavenRepoUrl=$MAVEN_REPO_URL -DbkjobVersion=$VERSION
    rm -rf tmp/startup_controller/*
    cp $BACKEND_DIR/release/$TOOL_NAME-$VERSION.jar tmp/startup_controller/$TOOL_NAME.jar
    cp startup-controller/startup.sh tmp/startup_controller/
    docker build -f startup-controller/startupController.Dockerfile -t $REGISTRY/job-tools-$TOOL_NAME:$VERSION tmp/startup_controller --network=host
    if [[ $PUSH -eq 1 ]] ; then
        docker push $REGISTRY/job-tools-$TOOL_NAME:$VERSION
    fi
}

# Building
if [[ $BUILD_ALL -eq 1 || $BUILD_FRONTEND -eq 1 ]] ; then
    build_frontend_module
fi
if [[ $BUILD_ALL -eq 1 || $BUILD_MIGRATION -eq 1 ]] ; then
    build_migration_image
fi
if [[ $BUILD_ALL -eq 1 || $BUILD_SYNC_BK_API_GATEWAY -eq 1 ]] ; then
    build_sync_bk_api_gateway_image
fi
if [[ $BUILD_ALL -eq 1 || $BUILD_STARTUP_CONTROLLER -eq 1 ]] ; then
    build_startup_controller_image
fi
if [[ $BUILD_ALL -eq 1 || $BUILD_BACKEND -eq 1 ]] ; then
    build_backend_modules "${BACKENDS[*]}"
fi
if [[ ${#BUILD_MODULES[@]} -ne 0 ]]; then
    log "Build ${BUILD_MODULES[@]}"
    for MODULE in ${BUILD_MODULES[@]};
	do
	    log "$MODULE"
	    if [[ "$MODULE" == "job-frontend" ]]; then
		    build_frontend_module
	    elif [[ "$MODULE" == "job-migration" ]]; then
		    build_migration_image
		  elif [[ "$MODULE" == "job-sync-bk-api-gateway" ]]; then
		    build_sync_bk_api_gateway_image
	    elif [[ "$MODULE" == "startup-controller" ]]; then
		    build_startup_controller_image
		elif [[ ${BACKENDS[@]} =~ "${MODULE}" ]]; then
            BUILD_BACKEND_MODULES[${#BUILD_BACKEND_MODULES[*]}]=${MODULE}
		fi
	done
    if [[ ${#BUILD_BACKEND_MODULES[*]} > 0 ]] ; then
        build_backend_modules "${BUILD_BACKEND_MODULES[*]}"
    fi
fi

echo "BUILD SUCCESSFUL!"
