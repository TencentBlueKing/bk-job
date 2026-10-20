#!/bin/bash

# bk-job-op Docker镜像构建脚本
# 用法: ./build.sh [选项]
# 选项:
#   -t, --tag TAG        指定镜像标签 (默认: latest)
#   -r, --registry REG   指定镜像仓库地址
#   -p, --push           构建后推送到镜像仓库
#   -h, --help           显示帮助信息

set -e

# 默认配置
MODULE="bk-job-op"
VERSION=""
REGISTRY=""
PUSH_IMAGE=false
USERNAME="${DOCKER_REGISTRY_USERNAME:-}"
PASSWORD="${DOCKER_REGISTRY_PASSWORD:-}"
MAVEN_REPO="${MAVEN_REPO_URL:-}"
GRADLE_PLUGIN_REPO="${GRADLE_PLUGIN_REPO_URL:-}"
GRADLE_DISTRIBUTION_URL_VALUE="${GRADLE_DISTRIBUTION_URL:-}"
NODE_DIST_BASE_URL_VALUE="${NODE_DIST_BASE_URL:-}"
NPM_REGISTRY="${NPM_REGISTRY_URL:-}"
BASE_IMAGE="${DOCKER_BASE_IMAGE:-}"
SKIP_FRONTEND=false
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
# 后端Gradle工程位于 src/backend，与 src/frontend 并列
BACKEND_DIR="${PROJECT_ROOT}/src/backend"
BUILD_TEMP_DIR="${SCRIPT_DIR}/docker-build"
WRAPPER_PROPERTIES="${BACKEND_DIR}/gradle/wrapper/gradle-wrapper.properties"
WRAPPER_BACKUP=""
WRAPPER_REPLACEMENT=""

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 日志函数
log_info() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

sanitize_url() {
    local value="$1"
    if [[ "$value" =~ ^([^:]+://)[^/]*@(.+)$ ]]; then
        printf '%s***@%s' "${BASH_REMATCH[1]}" "${BASH_REMATCH[2]}"
    else
        printf '%s' "$value"
    fi
}

resolve_image_registry() {
    local image="$1"
    local first_component="${image%%/*}"
    if [[ "$image" != */* ]] || [[ "$first_component" != *.* && "$first_component" != *:* && "$first_component" != "localhost" ]]; then
        printf 'docker.io'
    else
        printf '%s' "$first_component"
    fi
}

configure_gradle_wrapper_credentials() {
    local username="${GRADLE_DISTRIBUTION_USERNAME:-}"
    local password="${GRADLE_DISTRIBUTION_PASSWORD:-}"
    local username_option
    local password_option

    if [ -n "$username" ] || [ -n "$password" ]; then
        if [ -z "$username" ] || [ -z "$password" ]; then
            log_error "GRADLE_DISTRIBUTION_USERNAME 与 GRADLE_DISTRIBUTION_PASSWORD 必须同时配置"
            return 1
        fi
        printf -v username_option '%q' "-Dgradle.wrapperUser=${username}"
        printf -v password_option '%q' "-Dgradle.wrapperPassword=${password}"
        export GRADLE_OPTS="${GRADLE_OPTS:+${GRADLE_OPTS} }${username_option} ${password_option}"
    fi
}

cleanup() {
    local exit_code=$?
    if [ -n "$WRAPPER_BACKUP" ] && [ -f "$WRAPPER_BACKUP" ]; then
        cp "$WRAPPER_BACKUP" "$WRAPPER_PROPERTIES"
        rm -f "$WRAPPER_BACKUP"
        WRAPPER_BACKUP=""
    fi
    if [ -n "$WRAPPER_REPLACEMENT" ] && [ -f "$WRAPPER_REPLACEMENT" ]; then
        rm -f "$WRAPPER_REPLACEMENT"
        WRAPPER_REPLACEMENT=""
    fi
    rm -rf "${BUILD_TEMP_DIR}"
    trap - EXIT
    exit "$exit_code"
}

replace_gradle_distribution_url() {
    WRAPPER_BACKUP=$(mktemp "${WRAPPER_PROPERTIES}.backup.XXXXXX")
    WRAPPER_REPLACEMENT=$(mktemp "${WRAPPER_PROPERTIES}.replacement.XXXXXX")
    cp "$WRAPPER_PROPERTIES" "$WRAPPER_BACKUP"

    while IFS= read -r line || [ -n "$line" ]; do
        line="${line%$'\r'}"
        if [[ "$line" == distributionUrl=* ]]; then
            printf 'distributionUrl=%s\n' "$GRADLE_DISTRIBUTION_URL_VALUE"
        else
            printf '%s\n' "$line"
        fi
    done < "$WRAPPER_BACKUP" > "$WRAPPER_REPLACEMENT"
    mv "$WRAPPER_REPLACEMENT" "$WRAPPER_PROPERTIES"
    WRAPPER_REPLACEMENT=""
}

trap cleanup EXIT

if [[ "${BASH_SOURCE[0]}" != "$0" ]]; then
    return 0
fi

# 显示帮助信息
show_help() {
    cat << EOF
用法: ./build.sh [选项]

选项:
  -t, --tag TAG        指定镜像版本标签 (必需，例如: 1.0.0)
  -r, --registry REG   指定镜像仓库地址 (必需，例如: registry.example.com/bk-job)
  -u, --username USER  镜像仓库用户名 (推送时需要)
  -w, --password PASS  镜像仓库密码 (兼容参数，推荐改用 DOCKER_REGISTRY_PASSWORD)
  -m, --maven-repo URL 指定Maven仓库地址
      --gradle-plugin-repo URL
                       指定Gradle插件仓库地址
      --gradle-distribution-url URL
                       指定Gradle发行包完整URL
      --node-dist-url URL
                       指定Node发行包根地址
      --npm-registry URL
                       指定npm registry地址
      --base-image IMAGE
                       指定Docker基础镜像
  -p, --push           构建后推送到镜像仓库
      --skip-frontend  跳过OP Web前端构建 (镜像中不包含Web页面，仅调试后端时使用)
  -h, --help           显示帮助信息

同名语义环境变量 (命令行优先):
  MAVEN_REPO_URL、GRADLE_PLUGIN_REPO_URL、GRADLE_DISTRIBUTION_URL
  NODE_DIST_BASE_URL、NPM_REGISTRY_URL、DOCKER_BASE_IMAGE

可选凭据环境变量 (不会打印):
  MAVEN_REPO_USERNAME / MAVEN_REPO_PASSWORD
  GRADLE_PLUGIN_REPO_USERNAME / GRADLE_PLUGIN_REPO_PASSWORD
  GRADLE_DISTRIBUTION_USERNAME / GRADLE_DISTRIBUTION_PASSWORD
  NPM_REPO_USERNAME / NPM_REPO_PASSWORD
  DOCKER_BASE_IMAGE_REGISTRY / DOCKER_BASE_IMAGE_USERNAME / DOCKER_BASE_IMAGE_PASSWORD
  DOCKER_REGISTRY_USERNAME / DOCKER_REGISTRY_PASSWORD

示例:
  ./build.sh -r registry.example.com/bk-job -t 1.0.0
  ./build.sh -r registry.example.com/bk-job -t 1.0.0 -u admin -p
  ./build.sh -r registry.example.com/bk-job -t 1.0.0 -m https://repo.example.com/maven/

EOF
}

while [[ $# -gt 0 ]]; do
    case $1 in
        -t|--tag)
            VERSION="$2"
            shift 2
            ;;
        -r|--registry)
            REGISTRY="$2"
            shift 2
            ;;
        -u|--username)
            USERNAME="$2"
            shift 2
            ;;
        -w|--password)
            PASSWORD="$2"
            shift 2
            ;;
        -m|--maven-repo)
            MAVEN_REPO="$2"
            shift 2
            ;;
        --gradle-plugin-repo)
            GRADLE_PLUGIN_REPO="$2"
            shift 2
            ;;
        --gradle-distribution-url)
            GRADLE_DISTRIBUTION_URL_VALUE="$2"
            shift 2
            ;;
        --node-dist-url)
            NODE_DIST_BASE_URL_VALUE="$2"
            shift 2
            ;;
        --npm-registry)
            NPM_REGISTRY="$2"
            shift 2
            ;;
        --base-image)
            BASE_IMAGE="$2"
            shift 2
            ;;
        -p|--push)
            PUSH_IMAGE=true
            shift
            ;;
        --skip-frontend)
            SKIP_FRONTEND=true
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            log_error "未知选项: $1"
            show_help
            exit 1
            ;;
    esac
done

# 检查必需参数
if [ -z "$VERSION" ]; then
    log_error "必须指定镜像版本标签 (-t 参数)"
    show_help
    exit 1
fi

if [ -z "$REGISTRY" ]; then
    log_error "必须指定镜像仓库地址 (-r 参数)"
    show_help
    exit 1
fi

# 构建完整的镜像名称，如blueking/bk-job-op:1.0.0
FULL_IMAGE_NAME="${REGISTRY}/${MODULE}:${VERSION}"

# 显示构建信息
log_info "=========================================="
log_info "开始构建 bk-job-op Docker镜像"
log_info "=========================================="
log_info "项目目录: ${PROJECT_ROOT}"
log_info "后端目录: ${BACKEND_DIR}"
log_info "镜像名称: ${FULL_IMAGE_NAME}"
log_info "构建时间: $(date '+%Y-%m-%d %H:%M:%S')"
log_info "=========================================="

# 切换到后端工程目录
cd "${BACKEND_DIR}"

# 清理旧的临时构建目录
if [ -d "${BUILD_TEMP_DIR}" ]; then
    log_info "清理旧的构建目录..."
    rm -rf "${BUILD_TEMP_DIR}"
fi

# 创建临时构建目录
log_info "创建临时构建目录: ${BUILD_TEMP_DIR}"
mkdir -p "${BUILD_TEMP_DIR}"

# 使用 Gradle 编译项目
log_info "开始编译项目..."
if [ -f "${BACKEND_DIR}/gradlew" ]; then
    chmod +x "${BACKEND_DIR}/gradlew"
    
    # 构建Gradle命令参数
    GRADLE_ARGS=(clean build -x test)
    
    # 如果指定了Maven仓库，添加仓库配置
    if [ -n "$MAVEN_REPO" ]; then
        log_info "使用自定义Maven仓库: $(sanitize_url "$MAVEN_REPO")"
        GRADLE_ARGS+=("-PmavenRepoUrl=${MAVEN_REPO}")
    fi

    if [ -n "$GRADLE_PLUGIN_REPO" ]; then
        log_info "使用自定义Gradle插件仓库: $(sanitize_url "$GRADLE_PLUGIN_REPO")"
        GRADLE_ARGS+=("-PgradlePluginRepoUrl=${GRADLE_PLUGIN_REPO}")
    fi

    if [ -n "$GRADLE_DISTRIBUTION_URL_VALUE" ]; then
        if [[ "$GRADLE_DISTRIBUTION_URL_VALUE" =~ ^[^:]+://[^/]*@ ]]; then
            log_error "Gradle发行包URL不得内嵌凭据，请使用GRADLE_DISTRIBUTION_USERNAME/PASSWORD"
            exit 1
        fi
        log_info "使用自定义Gradle发行包: $(sanitize_url "$GRADLE_DISTRIBUTION_URL_VALUE")"
        replace_gradle_distribution_url
    fi

    if [ -n "$NODE_DIST_BASE_URL_VALUE" ]; then
        log_info "使用自定义Node发行包源: $(sanitize_url "$NODE_DIST_BASE_URL_VALUE")"
        GRADLE_ARGS+=("-PnodeDistBaseUrl=${NODE_DIST_BASE_URL_VALUE}")
    fi

    if [ -n "$NPM_REGISTRY" ]; then
        log_info "使用自定义npm registry: $(sanitize_url "$NPM_REGISTRY")"
        GRADLE_ARGS+=("-PnpmRegistryUrl=${NPM_REGISTRY}")
    fi

    if [ -n "$BASE_IMAGE" ]; then
        log_info "使用自定义Docker基础镜像: $(sanitize_url "$BASE_IMAGE")"
    fi

    # 前端构建开关
    if [ "$SKIP_FRONTEND" = true ]; then
        log_warn "已跳过前端构建，产出的镜像不包含OP Web页面"
        GRADLE_ARGS+=("-PskipFrontend")
    else
        log_info "将同时构建前端（首次构建会下载Node与npm依赖，耗时较长）"
    fi

    configure_gradle_wrapper_credentials
    "${BACKEND_DIR}/gradlew" "${GRADLE_ARGS[@]}"
else
    log_error "找不到 gradlew 文件"
    exit 1
fi

# 查找编译产物
JAR_FILE=$(find "${BACKEND_DIR}/build/libs" -name "*.jar" -type f | grep -v "plain" | head -n 1)
if [ -z "$JAR_FILE" ]; then
    log_error "找不到编译产物 JAR 文件"
    exit 1
fi

log_info "找到 JAR 文件: ${JAR_FILE}"

# 复制必要文件到临时构建目录
log_info "复制文件到临时构建目录..."
cp "${JAR_FILE}" "${BUILD_TEMP_DIR}/${MODULE}.jar"
cp "${SCRIPT_DIR}/Dockerfile" "${BUILD_TEMP_DIR}/"
cp "${SCRIPT_DIR}/startup.sh" "${BUILD_TEMP_DIR}/"
cp "${SCRIPT_DIR}/tini" "${BUILD_TEMP_DIR}/"

# 临时构建目录
cd "${BUILD_TEMP_DIR}"

# 构建Docker镜像
log_info "开始构建Docker镜像..."
if [ -n "${DOCKER_BASE_IMAGE_USERNAME:-}" ] || [ -n "${DOCKER_BASE_IMAGE_PASSWORD:-}" ]; then
    if [ -z "${DOCKER_BASE_IMAGE_USERNAME:-}" ] || [ -z "${DOCKER_BASE_IMAGE_PASSWORD:-}" ]; then
        log_error "DOCKER_BASE_IMAGE_USERNAME 与 DOCKER_BASE_IMAGE_PASSWORD 必须同时配置"
        exit 1
    fi
    BASE_IMAGE_REGISTRY="${DOCKER_BASE_IMAGE_REGISTRY:-$(resolve_image_registry "${BASE_IMAGE:-bkjob/jdk17:3.12.3}")}"
    export DOCKER_CONFIG="${BUILD_TEMP_DIR}/.docker"
    mkdir -p "$DOCKER_CONFIG"
    log_info "登录到Docker基础镜像仓库 $(sanitize_url "$BASE_IMAGE_REGISTRY")..."
    if ! printf '%s\n' "$DOCKER_BASE_IMAGE_PASSWORD" |
        docker login --username "$DOCKER_BASE_IMAGE_USERNAME" --password-stdin "$BASE_IMAGE_REGISTRY"; then
        log_error "Docker基础镜像仓库登录失败"
        exit 1
    fi
fi

DOCKER_BUILD_ARGS=(--build-arg "MODULE=${MODULE}")
if [ -n "$BASE_IMAGE" ]; then
    DOCKER_BUILD_ARGS+=(--build-arg "BASE_IMAGE=${BASE_IMAGE}")
fi
if docker build "${DOCKER_BUILD_ARGS[@]}" -t "${FULL_IMAGE_NAME}" .; then
    log_info "Docker镜像构建成功: ${FULL_IMAGE_NAME}"
else
    log_error "Docker镜像构建失败"
    exit 1
fi

# 显示镜像信息
log_info "镜像详情:"
docker images "${FULL_IMAGE_NAME}" --format "table {{.Repository}}\t{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}"

# 清理临时构建目录
log_info "清理临时构建目录..."
cd "${PROJECT_ROOT}"
rm -rf "${BUILD_TEMP_DIR}"

# 推送镜像到仓库
if [ "$PUSH_IMAGE" = true ]; then
    if [ -n "$USERNAME" ] && [ -n "$PASSWORD" ]; then
        log_info "登录到镜像仓库 ${REGISTRY}..."
        if printf '%s\n' "$PASSWORD" | docker login --username "$USERNAME" --password-stdin "$REGISTRY"; then
            log_info "Docker登录成功"
        else
            log_error "Docker登录失败"
            exit 1
        fi
    else
        log_warn "未提供用户名和密码，跳过登录步骤"
    fi
    
    log_info "开始推送镜像到仓库..."
    if docker push "${FULL_IMAGE_NAME}"; then
        log_info "镜像推送成功: ${FULL_IMAGE_NAME}"
    else
        log_error "镜像推送失败"
        exit 1
    fi
fi

log_info "=========================================="
log_info "构建完成！"
log_info "=========================================="
