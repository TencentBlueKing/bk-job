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
# 是否跳过 docker build 命令的执行：true 时仅准备各服务的镜像构建上下文，
# 不实际执行 docker build / docker push，便于在不支持 docker 命令的 CI 公共构建机上
# 由后续的 CI 镜像构建插件接管。默认 false 保持原有行为。
SKIP_DOCKER_BUILD=false
# 跳过模式下用于记录每个服务对应的 build context 路径，脚本结束时统一打印清单
PREPARED_CONTEXTS=()

cd $(dirname $0)
WORKING_DIR=$(pwd)
ROOT_DIR=${WORKING_DIR%/*/*/*}
BACKEND_DIR=$ROOT_DIR/src/backend
FRONTEND_DIR=$ROOT_DIR/src/frontend
SUPPORT_FILES_DIR=$ROOT_DIR/support-files
VERSION_LOGS_DIR=$ROOT_DIR/versionLogs
# 跳过 docker build 模式下，各服务的镜像构建上下文都会落到该目录下的独立子目录中，
# 避免后续 CI 镜像构建插件拿不到完整原材料（多个服务共用 tmp/backend 会互相覆盖）。
BUILD_CONTEXT_DIR=$WORKING_DIR/build-context

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
            [ --skip-docker-build   [Optional] Skip the 'docker build' / 'docker push' steps. Accepts 'true' or 'false', default 'false'.
                                    When set to 'true', the script only prepares the per-service build contexts (Dockerfile + artifacts)
                                    under '$WORKING_DIR/build-context/<service>/' so that an external CI image-build plugin can take over.
                                    Each service gets its own subdirectory so that multiple services can coexist. ]
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
        --skip-docker-build )
            # 取值规则：
            #  1) 后跟 true / false 时，按取值设置；
            #  2) 未传值（参数为空、或下一个参数也是一个 flag）时，按 false 处理（保持默认行为）；
            #  3) 非法值（不是 true/false）：明确报错并以非 0 退出，避免静默生效错误的语义。
            if [[ $# -ge 2 && "$2" != -* ]]; then
                case "$2" in
                    true|false)
                        shift
                        SKIP_DOCKER_BUILD=$1
                        ;;
                    *)
                        error "Invalid value for --skip-docker-build: '$2'. Expected 'true' or 'false'."
                        ;;
                esac
            else
                SKIP_DOCKER_BUILD=false
            fi
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

if [[ "$SKIP_DOCKER_BUILD" == "true" && $PUSH -eq 1 ]] ; then
    # 跳过模式下不会执行 docker build，自然也无法 push；明确给出提示避免用户误以为生效
    log "Warning: --push has no effect when --skip-docker-build=true; docker push will be skipped."
fi

if [[ "$SKIP_DOCKER_BUILD" == "false" && $PUSH -eq 1 && -n "$USERNAME" ]] ; then
    docker login --username $USERNAME --password $PASSWORD $REGISTRY
    log "docker login successfully"
fi

# Create tmp dir
tmp_dir=$WORKING_DIR/tmp
if [[ "$SKIP_DOCKER_BUILD" == "true" ]]; then
    # 跳过 docker build 时不创建临时目录，而是为每个服务准备独立的 build context 子目录；
    # 也不注册 tmp 目录的清理 trap，否则会把刚准备好的原材料一并清掉。
    mkdir -p "$BUILD_CONTEXT_DIR"
    log "===================== Skip docker build mode enabled ====================="
    log "Build contexts will be prepared at: $BUILD_CONTEXT_DIR"
    log "Each service will have its own subdirectory so that multiple services can coexist."
    log "=========================================================================="
else
    mkdir -p $tmp_dir/frontend
    mkdir -p $tmp_dir/backend
    mkdir -p $tmp_dir/startup_controller
    mkdir -p $tmp_dir/migration
    mkdir -p $tmp_dir/sync_bk_api_gateway

    # Automatically clean up the tmp directory when executing exit
    trap 'rm -rf $tmp_dir' EXIT TERM
fi

# 跳过 docker build 模式下使用：为指定服务准备一个独立的 build context 子目录，
# 返回该目录的绝对路径（通过 stdout）。会先清空已有内容，确保每次都是干净的。
prepare_build_context_dir () {
    local service=$1
    local context_dir="$BUILD_CONTEXT_DIR/$service"
    rm -rf "$context_dir"
    mkdir -p "$context_dir"
    echo "$context_dir"
}

# 跳过 docker build 模式下使用：记录某个服务的 build context 路径，
# 脚本结束时统一打印清单，便于 CI 插件按服务定位目录。
record_prepared_context () {
    local service=$1
    local context_dir=$2
    PREPARED_CONTEXTS+=("$service|$context_dir")
    log "[skip-docker-build] Build context ready for service '$service' at: $context_dir"
}

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

    if [[ "$SKIP_DOCKER_BUILD" == "true" ]]; then
        # 跳过 docker build：把 Dockerfile 与 dist 一起放到独立 build context 子目录中，
        # 使该目录自包含，CI 插件可直接拿来当 docker build 的上下文使用。
        local context_dir
        context_dir=$(prepare_build_context_dir "job-frontend")
        cp -rf $FRONTEND_DIR/dist "$context_dir/"
        cp frontend/frontend.Dockerfile "$context_dir/Dockerfile"
        record_prepared_context "job-frontend" "$context_dir"
        return 0
    fi

    rm -rf $tmp_dir/frontend/*

    echo "=======$FRONTEND_DIR======="
    ls $FRONTEND_DIR
    echo "=======$FRONTEND_DIR  end======="
    echo "=======$tmp_dir======="
    ls $tmp_dir
    echo "=======$tmp_dir  end======="
    cp -rf $FRONTEND_DIR/dist $tmp_dir/frontend/

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
        if [[ "${MODULE}" == "job-manage" || "${MODULE}" == "job-assemble" ]]; then
            log "Building version logs for module: ${MODULE}"
            if ! generate_version_logs "${MODULE}"; then
                log "Version logs generation failed, terminate build."
                exit 1
            fi
        fi
        if [[ "${MODULE}" == "job-assemble" ]] || [[ "${MODULE}" == "job-gateway" ]]; then
            tasks+=":${MODULE}:build "
        else
            tasks+=":${MODULE}:boot-${MODULE}:build "
        fi
    done
    log "Building backdend modules, gradle tasks: ${tasks}"
    $BACKEND_DIR/gradlew -p $BACKEND_DIR clean ${tasks} -DassemblyMode=k8s -DmysqlURL=$MYSQL_URL -DmysqlUser=$MYSQL_USERNAME -DmysqlPasswd=$MYSQL_PASSWORD -DmavenRepoUrl=$MAVEN_REPO_URL -DbkjobVersion=$VERSION --parallel
    for MODULE in ${MODULES[@]}; do
        if [[ "$SKIP_DOCKER_BUILD" == "true" ]]; then
            # 跳过 docker build：原本所有 backend 模块共用 tmp/backend 目录、构建完一个清一个；
            # 这里改为每个模块一个独立的子目录，避免后构建的把前一个的原材料覆盖掉。
            local context_dir
            context_dir=$(prepare_build_context_dir "$MODULE")
            cp $BACKEND_DIR/release/$MODULE-$VERSION.jar "$context_dir/$MODULE.jar"
            cp backend/startup.sh backend/tini "$context_dir/"
            cp backend/backend.Dockerfile "$context_dir/Dockerfile"
            record_prepared_context "$MODULE" "$context_dir"
            continue
        fi
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

    if [[ "$SKIP_DOCKER_BUILD" == "true" ]]; then
        local context_dir
        context_dir=$(prepare_build_context_dir "job-migration")
        cp $BACKEND_DIR/release/upgrader-$VERSION.jar "$context_dir/upgrader.jar"
        cp migration/startup.sh "$context_dir/"
        cp migration/runUpgrader.sh "$context_dir/"
        cp $SUPPORT_FILES_DIR/templates/#etc#job#upgrader#upgrader.properties "$context_dir/upgrader.properties.tpl"
        cp -r $SUPPORT_FILES_DIR/bkiam "$context_dir/"
        cp -r $SUPPORT_FILES_DIR/sql "$context_dir/"
        cp migration/migration.Dockerfile "$context_dir/Dockerfile"
        record_prepared_context "job-migration" "$context_dir"
        return 0
    fi

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

    if [[ "$SKIP_DOCKER_BUILD" == "true" ]]; then
        local context_dir
        context_dir=$(prepare_build_context_dir "job-sync-bk-api-gateway")
        cp -r $ROOT_DIR/support-files/bk-api-gateway/v3/* "$context_dir/"
        cp migration/bkApiGateway.Dockerfile "$context_dir/Dockerfile"
        record_prepared_context "job-sync-bk-api-gateway" "$context_dir"
        return 0
    fi

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

    if [[ "$SKIP_DOCKER_BUILD" == "true" ]]; then
        local context_dir
        context_dir=$(prepare_build_context_dir "job-tools-$TOOL_NAME")
        cp $BACKEND_DIR/release/$TOOL_NAME-$VERSION-all.jar "$context_dir/$TOOL_NAME.jar"
        cp startup-controller/startup.sh "$context_dir/"
        cp startup-controller/startupController.Dockerfile "$context_dir/Dockerfile"
        record_prepared_context "job-tools-$TOOL_NAME" "$context_dir"
        return 0
    fi

    rm -rf tmp/startup_controller/*
    cp $BACKEND_DIR/release/$TOOL_NAME-$VERSION-all.jar tmp/startup_controller/$TOOL_NAME.jar
    cp startup-controller/startup.sh tmp/startup_controller/
    docker build -f startup-controller/startupController.Dockerfile -t $REGISTRY/job-tools-$TOOL_NAME:$VERSION tmp/startup_controller --network=host
    if [[ $PUSH -eq 1 ]] ; then
        docker push $REGISTRY/job-tools-$TOOL_NAME:$VERSION
    fi
}

# Generate version log files
generate_version_logs() {
    local module="${1:-}"
    log "Generating version logs..."
    if [[ ! -d "$VERSION_LOGS_DIR" ]]; then
        log "Version log dir not found: $VERSION_LOGS_DIR"
        return 1
    fi

    local worker_dir=$(pwd)
    cd "$VERSION_LOGS_DIR"
    if ! python genBundledVersionLog.py; then
        log "genBundledVersionLog.py failed"
        cd "$worker_dir"
        return 1
    fi

    local target_dir=""
    if [[ "$module" == "job-manage" ]]; then
        target_dir="$BACKEND_DIR/job-manage/boot-job-manage/src/main/resources/versionLog"
    elif [[ "$module" == "job-assemble" ]]; then
        target_dir="$BACKEND_DIR/job-assemble/src/main/resources/versionLog"
    else
        log "Unsupported module for version logs: ${module}"
        cd "$worker_dir"
        return 1
    fi
    mkdir -p "$target_dir"
    log "Copy version log files to ${target_dir}"
    cp bundledVersionLog*.json "$target_dir"
    log "Version logs copied:"
    ls -l "$target_dir"/*.json
    cd "$worker_dir"
    return 0
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

# 跳过 docker build 模式结束时，统一打印各服务的 build context 清单，
# 便于 CI 镜像构建插件按服务定位目录、传给后续 docker build 命令使用。
if [[ "$SKIP_DOCKER_BUILD" == "true" ]]; then
    echo ""
    log "============================================================"
    log "Skip docker build mode summary"
    log "Total ${#PREPARED_CONTEXTS[@]} build context(s) prepared under: $BUILD_CONTEXT_DIR"
    log "------------------------------------------------------------"
    printf "  %-36s %s\n" "Service" "Build Context Path"
    log "------------------------------------------------------------"
    for entry in "${PREPARED_CONTEXTS[@]}"; do
        service="${entry%%|*}"
        path="${entry##*|}"
        printf "  %-36s %s\n" "$service" "$path"
    done
    log "------------------------------------------------------------"
    log "Each above directory is self-contained: a 'Dockerfile' plus all required artifacts."
    log "Hand each directory to your CI image-build plugin as the build context to produce the image."
    log "============================================================"
fi

echo "BUILD SUCCESSFUL!"
