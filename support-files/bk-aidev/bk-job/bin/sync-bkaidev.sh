#!/bin/bash
# 把 support-files/bk-aidev/bk-job 下的 AIDEV 资源（Agent / Skill / 知识库）同步到 AIDEV 平台。
# 由 job-sync-bk-aidev 镜像在部署阶段执行，镜像中已内置 bkai-init。
set -e

log() {
  echo "$(date +"%Y-%m-%d %H:%M:%S") [$1] $2"
}
log_info() { log "INFO" "$1"; }
log_warn() { log "WARN" "$1"; }
log_error() { log "ERROR" "$1"; }
title() { echo "====== $1 ======"; }

# 本脚本所在目录，用于定位同目录下的 Python 辅助脚本
script_dir=$(cd "$(dirname "$0")" && pwd)
# 待同步的 Agent Package 清单，路径与镜像中的资源目录一致
package_path="${BK_AIDEV_PACKAGE_PATH:-/bk-job/bkai.yaml}"
# 目标空间完整 ID，由 Chart 按是否多租户给出默认值；资源文件不再声明 space，全部以该参数为准
space="${BK_AIDEV_SPACE:-}"
# 目标租户，由 Chart 注入；未注入时多租户取 system、非多租户取 default，显式传入避免依赖 bkai-init 的默认值
if [ "${BKPAAS_MULTI_TENANT_MODE}" = "true" ]; then
  tenant_id="${BK_AIDEV_TENANT_ID:-system}"
else
  tenant_id="${BK_AIDEV_TENANT_ID:-default}"
fi
# 是否发布智能体：不发布时同步结果只是草稿，页面上的小鲸仍是旧版本
publish="${BK_AIDEV_PUBLISH:-true}"
# 是否只发布配置：false 时走平台常规发布流程，与 bkai-init 的默认值一致
publish_config_only="${BK_AIDEV_PUBLISH_CONFIG_ONLY:-false}"
# 需要跳过的资源，格式为 kind/code（kind 小写，取值 agent/collection/skill/knowledgebase），
# 多个以空格分隔；用于资源已被用户在平台上手工改动、不希望被再次覆盖的场景。
exclude_resources="${BK_AIDEV_EXCLUDE_RESOURCES:-}"
# 额外的智能体管理员，须为 bk_username，多个以空格分隔；应用于所有选中的主子智能体，只追加不删除
admins="${BK_AIDEV_ADMINS:-}"
# 同步重试参数：Agent 引用的 MCP Server 由网关同步任务异步注册，
# 两个任务之间没有强制先后关系，这里通过有限重试等待 MCP 就绪。
max_retry="${BK_AIDEV_SYNC_MAX_RETRY:-3}"
retry_interval="${BK_AIDEV_SYNC_RETRY_INTERVAL:-30}"
# 同步最终失败时是否忽略错误：默认 false，失败时脚本非 0 退出使 K8S Job 与 Helm 部署失败；
# 设为 true 时忽略同步错误并以 0 退出，不阻塞 Helm 部署。
ignore_sync_error="${BK_AIDEV_IGNORE_SYNC_ERROR:-false}"

on_exit() {
  exit_code=$?
  if [ -n "${sync_log_file:-}" ] && [ -f "${sync_log_file}" ]; then
    rm -f "${sync_log_file}"
  fi
  if [ "${exit_code}" -ne 0 ] && [ "${ignore_sync_error}" = "true" ]; then
    log_warn "Sync bk-aidev failed with exit code ${exit_code}, ignored because BK_AIDEV_IGNORE_SYNC_ERROR=true"
    exit 0
  fi
}
trap on_exit EXIT

title "checking params"
if [ ! -f "${package_path}" ]; then
  log_error "Agent package file ${package_path} does not exist"
  exit 1
fi
# 资源根目录，渲染与同步都以该目录为基准
resource_dir=$(dirname "${package_path}")
# 目标空间缺失时直接失败，避免回落到 bkai-init 的默认空间、把资源同步到非预期的空间
if [ -z "${space}" ]; then
  log_error "BK_AIDEV_SPACE (target space id) is required"
  exit 1
fi
# bkai-init 从环境变量读取网关地址模板与应用身份，这里复用作业平台自身的 appCode/appSecret
if [ -z "${BK_API_URL_TMPL}" ]; then
  log_error "BK_API_URL_TMPL is required by bkai-init"
  exit 1
fi
# bkai-init 要把模板里的 {api_name} 分别替换成 bk-aidev 与 bk-user，
# 误填成某一个网关的完整地址时，要到调用接口时才会报错，这里提前拦下
case "${BK_API_URL_TMPL}" in
  *"{api_name}"*) ;;
  *)
    log_error "BK_API_URL_TMPL must contain the {api_name} placeholder, got ${BK_API_URL_TMPL}"
    exit 1
    ;;
esac
if [ -z "${BKAI_APP_CODE}" ] || [ -z "${BKAI_APP_SECRET}" ]; then
  log_error "BKAI_APP_CODE / BKAI_APP_SECRET is required by bkai-init"
  exit 1
fi
log_info "package=${package_path} multi_tenant_mode=${BKPAAS_MULTI_TENANT_MODE:-false} tenant=${tenant_id} space=${space} app_code=${BKAI_APP_CODE}"
log_info "api_url_tmpl=${BK_API_URL_TMPL}"
# 初始化用户由 BKAI_ACCESS_TOKEN / BKAI_USERNAME 指定，BKAI_USERNAME 须为 bk_username，bkai-init 不做转换；
# 两者都不传时 bkai-init 默认用 bk_admin。这里只打印用户名，不打印令牌
log_info "init_user=${BKAI_USERNAME:-<default bk_admin>} access_token=${BKAI_ACCESS_TOKEN:+<set>}"
log_info "admins=[${admins}]"
log_info "publish=${publish} publish_config_only=${publish_config_only} exclude_resources=[${exclude_resources}] max_retry=${max_retry} retry_interval=${retry_interval}s ignore_sync_error=${ignore_sync_error}"

# 组装 --exclude-resource 参数，未配置时不传该参数。
# 这里刻意用字符串拼接而非 bash 数组：资源标识形如 kind/code，不含空格，
# 用字符串拼接可以让脚本在只有 sh 的精简基础镜像中同样可用。
exclude_args=""
for resource in ${exclude_resources}; do
  exclude_args="${exclude_args} --exclude-resource ${resource}"
done

# 发布参数：不开启发布时同步结果停留在草稿，页面上的智能体不会更新。
# bkai-init 的 --publish_config_only 只接受 0/1，这里把 values 传来的布尔值转成数字。
publish_args=""
if [ "${publish}" = "true" ]; then
  if [ "${publish_config_only}" = "true" ]; then
    publish_args="--publish --publish_config_only=1"
  else
    publish_args="--publish --publish_config_only=0"
  fi
fi

# 管理员参数：bkai-init 禁止在 YAML 中配置 spec.admins，只能通过 --admins 传入，形如 --admins user_a user_b；
# 未配置时不传，此时不处理管理员。追加失败不会中断同步，原因保留在 bkai-init 的最终报告中。
admins_args=""
if [ -n "${admins}" ]; then
  admins_args="--admins ${admins}"
fi

# 占位符渲染交给 Python 脚本处理：字面量替换不涉及 sed 的转义规则（& \ 与分隔符），
# 文件编码与渲染范围也更可控；占位符名单维护在 render_placeholders.py 中。
title "preparing python interpreter"
# 解释器可通过 PYTHON_BIN 指定；未指定时依次探测 python3、python，
# 部分基础镜像只提供 python 而没有 python3。
python_bin="${PYTHON_BIN:-}"
if [ -z "${python_bin}" ]; then
  for candidate in python3 python; do
    if command -v "${candidate}" >/dev/null 2>&1; then
      python_bin="${candidate}"
      break
    fi
  done
fi
if [ -z "${python_bin}" ]; then
  log_error "python3 is required to render placeholders in resource files, but no python interpreter found"
  exit 1
fi
# 渲染脚本按 Python 3 编写，用 python2 执行会直接语法报错，这里提前给出明确提示
if ! "${python_bin}" -c 'import sys; sys.exit(0 if sys.version_info[0] >= 3 else 1)' >/dev/null 2>&1; then
  log_error "Python 3 is required, but ${python_bin} is not python3"
  exit 1
fi
log_info "Using python interpreter: ${python_bin}"

title "rendering placeholders"
"${python_bin}" "${script_dir}/render_placeholders.py" --base-dir "${resource_dir}"

title "validating aidev resources"
bkai-init validate -f "${package_path}" --space "${space}"

# 预览同步动作与配置差异，把本次将要覆盖的内容留在部署日志里，便于同步异常后追溯。
# 参数与 sync 保持一致，预览结果才与实际同步对应；plan 是只读操作，失败不阻断后续同步。
title "planning aidev resources"
# publish_args / exclude_args / admins_args 需要按空格拆分成多个参数，故刻意不加引号
if ! bkai-init plan -f "${package_path}" --tenant-id "${tenant_id}" --space "${space}" \
    ${publish_args} ${exclude_args} ${admins_args}; then
  log_warn "Plan returned non-zero, continue to sync"
fi

title "syncing aidev resources"
sync_log_file=$(mktemp /tmp/bkai-sync.XXXXXX)
attempt=1
while true; do
  log_info "Syncing aidev resources, attempt ${attempt}/${max_retry}"
  # 必须带 --confirm，否则 sync 只做只读预览，不会写入任何资源
  # publish_args / exclude_args / admins_args 需要按空格拆分成多个参数，故刻意不加引号
  # 同时检查退出码与输出中的失败标记，确保同步报错时一定触发重试并在最终失败时以非 0 退出码终止容器
  sync_ok=true
  set +e
  bkai-init sync -f "${package_path}" --tenant-id "${tenant_id}" --space "${space}" \
      --confirm ${publish_args} ${exclude_args} ${admins_args} 2>&1 | tee "${sync_log_file}"
  sync_rc=${PIPESTATUS[0]}
  set -e
  if [ "${sync_rc}" -ne 0 ]; then
    log_warn "bkai-init sync exited with code ${sync_rc}"
    sync_ok=false
  elif grep -Eq "(初始化已停止|✗|失败 [1-9][0-9]* 项)" "${sync_log_file}"; then
    log_warn "bkai-init sync output indicates failure despite exit code 0"
    sync_ok=false
  fi
  if [ "${sync_ok}" = "true" ]; then
    log_info "Aidev resources synced successfully"
    break
  fi
  if [ "${attempt}" -ge "${max_retry}" ]; then
    log_error "Aidev resources sync failed after ${max_retry} attempts"
    exit 1
  fi
  # 同步中途失败时，此前已写入的资源不会自动回滚，重试是按 code 覆盖式重入
  log_warn "Sync failed, the referenced mcp server or skill may not be ready, retry in ${retry_interval}s"
  attempt=$((attempt + 1))
  sleep "${retry_interval}"
done

title "done"
