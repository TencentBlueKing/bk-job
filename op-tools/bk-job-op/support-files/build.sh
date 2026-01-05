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
USERNAME=""
PASSWORD=""
MAVEN_REPO=""
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BUILD_TEMP_DIR="${SCRIPT_DIR}/docker-build"

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

# 显示帮助信息
show_help() {
    cat << EOF
用法: ./build.sh [选项]

选项:
  -t, --tag TAG        指定镜像版本标签 (必需，例如: 1.0.0)
  -r, --registry REG   指定镜像仓库地址 (必需，例如: mirrors.com/bk-job)
  -u, --username USER  镜像仓库用户名 (推送时需要)
  -w, --password PASS  镜像仓库密码 (推送时需要)
  -m, --maven-repo URL 指定Maven仓库地址 (例如: https://maven.example.com/repository/public/)
  -p, --push           构建后推送到镜像仓库
  -h, --help           显示帮助信息

示例:
  ./build.sh -r mirrors.com/bk-job -t 1.0.0
  ./build.sh -r mirrors.com/bk-job -t 1.0.0 -u admin -w password123 -p
  ./build.sh -r mirrors.com/bk-job -t 1.0.0 -m https://maven.example.com/repository/public/

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
        -p|--push)
            PUSH_IMAGE=true
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
log_info "镜像名称: ${FULL_IMAGE_NAME}"
log_info "构建时间: $(date '+%Y-%m-%d %H:%M:%S')"
log_info "=========================================="

# 切换到项目根目录
cd "${PROJECT_ROOT}"

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
if [ -f "${PROJECT_ROOT}/gradlew" ]; then
    chmod +x "${PROJECT_ROOT}/gradlew"
    
    # 构建Gradle命令参数
    GRADLE_ARGS="clean build -x test"
    
    # 如果指定了Maven仓库，添加仓库配置
    if [ -n "$MAVEN_REPO" ]; then
        log_info "使用自定义Maven仓库: ${MAVEN_REPO}"
        GRADLE_ARGS="${GRADLE_ARGS} -PmavenRepoUrl=${MAVEN_REPO}"
    fi
    
    "${PROJECT_ROOT}/gradlew" ${GRADLE_ARGS}
else
    log_error "找不到 gradlew 文件"
    exit 1
fi

# 查找编译产物
JAR_FILE=$(find "${PROJECT_ROOT}/build/libs" -name "*.jar" -type f | grep -v "plain" | head -n 1)
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
if docker build --build-arg MODULE="${MODULE}" -t "${FULL_IMAGE_NAME}" .; then
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
        if echo "$PASSWORD" | docker login --username "$USERNAME" --password-stdin "$REGISTRY"; then
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
