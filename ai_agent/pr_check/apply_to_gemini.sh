#!/usr/bin/env bash
set -euo pipefail

# 将 ai_agent/pr_check 下的 Agent 资源增量导入项目根目录下的 .gemini 目录
# 仅覆盖 pr_check 同名的 agent / skill，并将 rules 追加到 GEMINI.md，不影响已有内容
# 用法：
#   bash ai_agent/pr_check/apply_to_gemini.sh [目标项目根目录]

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="${1:-$(cd "${SCRIPT_DIR}/../.." && pwd)}"
GEMINI_DIR="${PROJECT_ROOT}/.gemini"
GEMINI_MD="${GEMINI_DIR}/GEMINI.md"

mkdir -p "${GEMINI_DIR}/agents" "${GEMINI_DIR}/skills"

for agent_dir in "${SCRIPT_DIR}/agents"/*/; do
    [ -d "${agent_dir}" ] || continue
    agent_name="$(basename "${agent_dir}")"
    rm -rf "${GEMINI_DIR}/agents/${agent_name}"
    cp -r "${agent_dir%/}" "${GEMINI_DIR}/agents/${agent_name}"
done

for skill_dir in "${SCRIPT_DIR}/skills"/*/; do
    [ -d "${skill_dir}" ] || continue
    skill_name="$(basename "${skill_dir}")"
    rm -rf "${GEMINI_DIR}/skills/${skill_name}"
    cp -r "${skill_dir%/}" "${GEMINI_DIR}/skills/${skill_name}"
done

if [ ! -f "${GEMINI_MD}" ]; then
    touch "${GEMINI_MD}"
fi
cat "${SCRIPT_DIR}/rules/"*.md >> "${GEMINI_MD}"

echo "Applied pr_check agent resources to ${GEMINI_DIR}"
