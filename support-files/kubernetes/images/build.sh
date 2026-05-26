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
# --parallel 显式覆盖值；留空时按 detect_cpu_count() 自适应为 CPU 核数；N=1 等价串行
PARALLEL_OVERRIDE=
# 准备阶段收集的 docker 构建任务列表，每项格式： service|image_tag|context_dir
BUILD_TASKS=()
# 构建阶段记录的成功 / 失败任务，每项格式： service|log_path
SUCCEEDED_TASKS=()
FAILED_TASKS=()

cd $(dirname $0)
WORKING_DIR=$(pwd)
ROOT_DIR=${WORKING_DIR%/*/*/*}
BACKEND_DIR=$ROOT_DIR/src/backend
FRONTEND_DIR=$ROOT_DIR/src/frontend
SUPPORT_FILES_DIR=$ROOT_DIR/support-files
VERSION_LOGS_DIR=$ROOT_DIR/versionLogs
# 所有服务的 build context 统一放到该目录下的独立子目录中，
# 这样 skip / 非 skip 两种模式共用一套目录布局，并避免多个 backend 模块共用同一个临时
# 目录被互相覆盖；非 skip 模式下也不在脚本结束时清理，便于排查 docker build 日志。
BUILD_CONTEXT_DIR=$WORKING_DIR/build-context
# 并行 worker 的日志放在该目录下，刻意置于 build context 物理外层（兄弟目录、dot 前缀
# 避免与服务名冲突），原因有二：
#  1) 部分 Dockerfile 使用 COPY ./ 的粗粒度写法（backend/migration/startup-controller），
#     如果日志写在 context 内，docker build 会把日志也打进镜像；
#  2) docker build 的 context 打包阶段会读所有文件做 tar，与 worker 同时写入会触发
#     "file changed while reading" 警告甚至损坏 context 完整性。
# 该目录会被 build-context/ 这一条 .gitignore 规则一并忽略，不需要额外的 ignore 条目。
BUILD_LOG_DIR=$BUILD_CONTEXT_DIR/.logs

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
                                    under '$BUILD_CONTEXT_DIR/<service>/' so that an external CI image-build plugin can take over.
                                    Each service gets its own subdirectory so that multiple services can coexist. ]
            [ --parallel N          [Optional] Max number of concurrent 'docker build' workers (positive integer).
                                    Default: auto-detected CPU count (nproc / getconf _NPROCESSORS_ONLN / sysctl hw.ncpu, fallback 1).
                                    N=1 falls back to serial behaviour. Ignored when --skip-docker-build=true. ]
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

# 仅匹配正整数（不含 0、负数、非数字）；用于 --parallel 取值校验
is_positive_int () {
    [[ "$1" =~ ^[1-9][0-9]*$ ]]
}

# 探测可用 CPU 数，依次尝试 nproc / getconf / sysctl；都不可用或异常时回退为 1。
# 这样 Linux / macOS / Windows Git Bash 都能拿到合理的默认并发度。
detect_cpu_count () {
    local n=
    if command -v nproc >/dev/null 2>&1; then
        n=$(nproc 2>/dev/null || true)
    fi
    if [[ -z "$n" ]] && command -v getconf >/dev/null 2>&1; then
        n=$(getconf _NPROCESSORS_ONLN 2>/dev/null || true)
    fi
    if [[ -z "$n" ]] && command -v sysctl >/dev/null 2>&1; then
        n=$(sysctl -n hw.ncpu 2>/dev/null || true)
    fi
    if [[ -z "$n" || ! "$n" =~ ^[1-9][0-9]*$ ]]; then
        n=1
    fi
    echo "$n"
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
        --parallel )
            shift
            if ! is_positive_int "${1:-}"; then
                error "Invalid value for --parallel: '${1:-}'. Expected a positive integer (>=1)."
            fi
            PARALLEL_OVERRIDE=$1
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
    # 在并行 docker build 之前完成单次 docker login，所有 worker 复用登录态
    docker login --username $USERNAME --password $PASSWORD $REGISTRY
    log "docker login successfully"
fi

# 统一创建 build context 根目录；skip 与非 skip 模式都使用同一套布局
mkdir -p "$BUILD_CONTEXT_DIR"

if [[ "$SKIP_DOCKER_BUILD" == "true" ]]; then
    log "===================== Skip docker build mode enabled ====================="
    log "Build contexts will be prepared at: $BUILD_CONTEXT_DIR"
    log "Each service will have its own subdirectory so that multiple services can coexist."
    log "=========================================================================="
fi

# 为指定服务准备一个独立的 build context 子目录，返回该目录的绝对路径（stdout）。
# 每次都会先清空再重建，确保 context 不残留旧文件。
prepare_build_context_dir () {
    local service=$1
    local context_dir="$BUILD_CONTEXT_DIR/$service"
    rm -rf "$context_dir"
    mkdir -p "$context_dir"
    echo "$context_dir"
}

# 注册一个 docker 构建任务（准备阶段调用）：
#   service     - 服务名（同 docker tag 中的镜像名部分）
#   image_tag   - 完整镜像标签，例如 docker.io/job-manage:latest
#   context_dir - 该服务的 build context 绝对路径（含 Dockerfile）
register_build_task () {
    local service=$1
    local image_tag=$2
    local context_dir=$3
    BUILD_TASKS+=("$service|$image_tag|$context_dir")
    log "[prepare] Build context ready: service=$service image=$image_tag dir=$context_dir"
}

# 单个 docker build 任务的执行体；通过 '&' 在子 shell 中并发运行。
# 所有 stdout/stderr（含 set -x 调试输出）都重定向到 $BUILD_LOG_DIR/<service>.log；
# 该日志位于 build context 之外，避免被 Dockerfile 的 COPY ./ 打进镜像、也避免与
# docker build 的 context tar 阶段争抢同一文件。
# 失败时以非 0 退出码退出子 shell，由父进程通过 wait 取得退出码并归入 FAILED_TASKS。
run_docker_build_worker () {
    local task=$1
    local service image_tag context_dir
    IFS='|' read -r service image_tag context_dir <<< "$task"
    local log_file="$BUILD_LOG_DIR/$service.log"

    {
        echo "===== [$service] docker build started at $(date '+%F %T') ====="
        echo "===== cmd: docker build -f $context_dir/Dockerfile -t $image_tag $context_dir --network=host ====="
        if ! docker build -f "$context_dir/Dockerfile" -t "$image_tag" "$context_dir" --network=host; then
            echo "===== [$service] docker build FAILED ====="
            exit 1
        fi
        if [[ $PUSH -eq 1 ]]; then
            echo "===== [$service] docker push started ====="
            if ! docker push "$image_tag"; then
                echo "===== [$service] docker push FAILED ====="
                exit 1
            fi
        fi
        echo "===== [$service] completed at $(date '+%F %T') ====="
    } > "$log_file" 2>&1
}

# 以指定并发度运行 BUILD_TASKS 中的所有 docker 构建任务。
# 实现策略：纯 bash 的 pid 池 + 轮询，不依赖 'wait -n'，兼容 bash 3.2 (macOS 默认) / Git Bash / Linux。
# fail-late：任一任务失败时只追加到 FAILED_TASKS，不打断其他任务；全部结束后由调用方依据数组判定整体退出码。
run_docker_builds_parallel () {
    local max=$1
    local total=${#BUILD_TASKS[@]}
    if [[ $total -eq 0 ]]; then
        log "No docker build tasks to run."
        return 0
    fi
    log "===== Docker build phase: tasks=$total, parallelism=$max ====="

    # 并行 pid 池及其元数据（多个数组按下标对齐）
    local -a pool_pid=()
    local -a pool_svc=()
    local -a pool_idx=()
    local -a pool_log=()
    local -a pool_t0=()

    local next_task=0
    local finished=0

    while [[ $finished -lt $total ]]; do
        # 1) 填充 pid 池至 max
        while [[ ${#pool_pid[@]} -lt $max && $next_task -lt $total ]]; do
            local task="${BUILD_TASKS[$next_task]}"
            local svc="${task%%|*}"
            local rest="${task#*|}"
            local img="${rest%%|*}"
            local ctx="${rest#*|}"
            next_task=$((next_task + 1))
            log "[$next_task/$total] start: $svc -> $img"
            run_docker_build_worker "$task" &
            local pid=$!
            pool_pid+=("$pid")
            pool_svc+=("$svc")
            pool_idx+=("$next_task")
            pool_log+=("$BUILD_LOG_DIR/$svc.log")
            pool_t0+=("$SECONDS")
        done

        # 2) 扫描 pid 池，回收已完成的 worker
        local -a new_pid=() new_svc=() new_idx=() new_log=() new_t0=()
        local any_finished=0
        local i
        for ((i = 0; i < ${#pool_pid[@]}; i++)); do
            local pid="${pool_pid[$i]}"
            if kill -0 "$pid" 2>/dev/null; then
                new_pid+=("$pid")
                new_svc+=("${pool_svc[$i]}")
                new_idx+=("${pool_idx[$i]}")
                new_log+=("${pool_log[$i]}")
                new_t0+=("${pool_t0[$i]}")
            else
                local rc=0
                wait "$pid" || rc=$?
                local elapsed=$((SECONDS - pool_t0[i]))
                if [[ $rc -eq 0 ]]; then
                    log "[${pool_idx[$i]}/$total] done (${elapsed}s): ${pool_svc[$i]}"
                    SUCCEEDED_TASKS+=("${pool_svc[$i]}|${pool_log[$i]}")
                else
                    log "[${pool_idx[$i]}/$total] FAIL (rc=$rc, ${elapsed}s): ${pool_svc[$i]} -- log: ${pool_log[$i]}"
                    FAILED_TASKS+=("${pool_svc[$i]}|${pool_log[$i]}")
                fi
                finished=$((finished + 1))
                any_finished=1
            fi
        done

        # 3) 重置 pid 池（注意空数组 + set -u 在老 bash 上的展开兼容性，用长度判断）
        if [[ ${#new_pid[@]} -gt 0 ]]; then
            pool_pid=("${new_pid[@]}")
            pool_svc=("${new_svc[@]}")
            pool_idx=("${new_idx[@]}")
            pool_log=("${new_log[@]}")
            pool_t0=("${new_t0[@]}")
        else
            pool_pid=()
            pool_svc=()
            pool_idx=()
            pool_log=()
            pool_t0=()
        fi

        # 4) 池满且本轮无任务结束 → 小睡一下让出 CPU，避免忙等
        if [[ $any_finished -eq 0 && ${#pool_pid[@]} -ge $max && $finished -lt $total ]]; then
            sleep 0.3
        fi
    done
}

# Build frontend image
build_frontend_module () {
# Build frontend image
    log "Preparing frontend build context..."
    cd $FRONTEND_DIR || exit 1
    export JOB_VERSION=$VERSION
    echo "JOB_VERSION=${JOB_VERSION}"
    npm i --legacy-peer-deps
    npm run build
    cd $WORKING_DIR || exit 1

    # 把 Dockerfile 与 dist 一起放到独立 build context 子目录中，使该目录自包含，
    # 既可由本脚本的并行 worker 直接 docker build，也可由 CI 插件作为构建上下文复用。
    local context_dir
    context_dir=$(prepare_build_context_dir "job-frontend")
    cp -rf $FRONTEND_DIR/dist "$context_dir/"
    cp frontend/frontend.Dockerfile "$context_dir/Dockerfile"
    register_build_task "job-frontend" "$REGISTRY/job-frontend:$VERSION" "$context_dir"
}

# Build backend image
build_backend_modules () {
    MODULES=$1
    log "Preparing backdend {MODULES} build contexts, version: ${VERSION}..."
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
        # 每个 backend 模块都使用独立 build context 子目录，避免相互覆盖；
        # 把 backend.Dockerfile 拷为 Dockerfile，使目录自包含。
        local context_dir
        context_dir=$(prepare_build_context_dir "$MODULE")
        cp $BACKEND_DIR/release/$MODULE-$VERSION.jar "$context_dir/$MODULE.jar"
        cp backend/startup.sh backend/tini "$context_dir/"
        cp backend/backend.Dockerfile "$context_dir/Dockerfile"
        register_build_task "$MODULE" "$REGISTRY/$MODULE:$VERSION" "$context_dir"
    done
}

# Build migration image
build_migration_image(){
    log "Preparing migration build context, version: ${VERSION}..."
    $BACKEND_DIR/gradlew -p $BACKEND_DIR/upgrader clean :upgrader:build -DmavenRepoUrl=$MAVEN_REPO_URL -DbkjobVersion=$VERSION

    local context_dir
    context_dir=$(prepare_build_context_dir "job-migration")
    cp $BACKEND_DIR/release/upgrader-$VERSION.jar "$context_dir/upgrader.jar"
    cp migration/startup.sh "$context_dir/"
    cp migration/runUpgrader.sh "$context_dir/"
    cp $SUPPORT_FILES_DIR/templates/#etc#job#upgrader#upgrader.properties "$context_dir/upgrader.properties.tpl"
    cp -r $SUPPORT_FILES_DIR/bkiam "$context_dir/"
    cp -r $SUPPORT_FILES_DIR/sql "$context_dir/"
    cp migration/migration.Dockerfile "$context_dir/Dockerfile"
    register_build_task "job-migration" "$REGISTRY/job-migration:$VERSION" "$context_dir"
}

# Build sync-bk-api-gateway image
build_sync_bk_api_gateway_image(){
    log "Preparing sync_bk_api_gateway build context, version: ${VERSION}..."

    local context_dir
    context_dir=$(prepare_build_context_dir "job-sync-bk-api-gateway")
    cp -r $ROOT_DIR/support-files/bk-api-gateway/v3/* "$context_dir/"
    cp migration/bkApiGateway.Dockerfile "$context_dir/Dockerfile"
    register_build_task "job-sync-bk-api-gateway" "$REGISTRY/job-sync-bk-api-gateway:$VERSION" "$context_dir"
}

# Build startup-controller image
build_startup_controller_image(){
    log "Preparing startup-controller build context, version: ${VERSION}..."
    TOOL_NAME="k8s-startup-controller"
    $BACKEND_DIR/gradlew -p $BACKEND_DIR/job-tools clean :job-tools:$TOOL_NAME:build -DmavenRepoUrl=$MAVEN_REPO_URL -DbkjobVersion=$VERSION

    local context_dir
    context_dir=$(prepare_build_context_dir "job-tools-$TOOL_NAME")
    cp $BACKEND_DIR/release/$TOOL_NAME-$VERSION-all.jar "$context_dir/$TOOL_NAME.jar"
    cp startup-controller/startup.sh "$context_dir/"
    cp startup-controller/startupController.Dockerfile "$context_dir/Dockerfile"
    register_build_task "job-tools-$TOOL_NAME" "$REGISTRY/job-tools-$TOOL_NAME:$VERSION" "$context_dir"
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

# === Prep phase: 顺序为各服务准备 build context ===
# 准备阶段保持串行：底层 npm / gradle 等编译工具本身已是多线程或共享缓存，
# 在外层再并行收益有限且容易触发资源竞争；并行只用于真正独立的 docker build。
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
	    elif [[ "$MODULE" == "job-tools-k8s-startup-controller" ]]; then
		    build_startup_controller_image
		elif [[ ${BACKENDS[@]} =~ "${MODULE}" ]]; then
            BUILD_BACKEND_MODULES[${#BUILD_BACKEND_MODULES[*]}]=${MODULE}
		fi
	done
    if [[ ${#BUILD_BACKEND_MODULES[*]} > 0 ]] ; then
        build_backend_modules "${BUILD_BACKEND_MODULES[*]}"
    fi
fi

# === Docker build phase ===
if [[ "$SKIP_DOCKER_BUILD" == "true" ]]; then
    # skip 模式只输出 build context 清单，不执行 docker build / push；--parallel 在此模式下无意义
    if [[ -n "$PARALLEL_OVERRIDE" ]]; then
        log "Note: --parallel=$PARALLEL_OVERRIDE is ignored when --skip-docker-build=true (no docker build to run)."
    fi
    echo ""
    log "============================================================"
    log "Skip docker build mode summary"
    log "Total ${#BUILD_TASKS[@]} build context(s) prepared under: $BUILD_CONTEXT_DIR"
    log "------------------------------------------------------------"
    printf "  %-36s %s\n" "Service" "Build Context Path"
    log "------------------------------------------------------------"
    if [[ ${#BUILD_TASKS[@]} -gt 0 ]]; then
        for entry in "${BUILD_TASKS[@]}"; do
            svc="${entry%%|*}"
            rest="${entry#*|}"
            ctx="${rest#*|}"
            printf "  %-36s %s\n" "$svc" "$ctx"
        done
    fi
    log "------------------------------------------------------------"
    log "Each above directory is self-contained: a 'Dockerfile' plus all required artifacts."
    log "Hand each directory to your CI image-build plugin as the build context to produce the image."
    log "============================================================"
    echo "BUILD SUCCESSFUL!"
    exit 0
fi

# 非 skip 模式：在启动并行池前确保日志目录存在（worker 内的 > $log_file 不会主动建目录）
mkdir -p "$BUILD_LOG_DIR"

# 非 skip 模式：确定有效并发度并并行执行 docker build
if [[ -n "$PARALLEL_OVERRIDE" ]]; then
    PARALLEL=$PARALLEL_OVERRIDE
    log "Effective docker build parallelism: $PARALLEL (override via --parallel)"
else
    PARALLEL=$(detect_cpu_count)
    log "Effective docker build parallelism: $PARALLEL (auto-detected CPU count)"
fi
log "Per-service docker build logs: $BUILD_LOG_DIR/<service>.log"

run_docker_builds_parallel "$PARALLEL"

# === Build summary ===
echo ""
log "============================================================"
log "Docker build summary: ${#SUCCEEDED_TASKS[@]} succeeded, ${#FAILED_TASKS[@]} failed"
log "------------------------------------------------------------"
if [[ ${#SUCCEEDED_TASKS[@]} -gt 0 ]]; then
    log "Succeeded:"
    for entry in "${SUCCEEDED_TASKS[@]}"; do
        svc="${entry%%|*}"
        log_path="${entry##*|}"
        printf "  %-36s %s\n" "$svc" "$log_path"
    done
fi
if [[ ${#FAILED_TASKS[@]} -gt 0 ]]; then
    log "Failed:"
    for entry in "${FAILED_TASKS[@]}"; do
        svc="${entry%%|*}"
        log_path="${entry##*|}"
        printf "  %-36s %s\n" "$svc" "$log_path"
    done
    log "============================================================"
    log "BUILD FAILED with ${#FAILED_TASKS[@]} task(s) failing. See per-service log files under $BUILD_LOG_DIR/ for details."
    exit 1
fi
log "============================================================"

echo "BUILD SUCCESSFUL!"
