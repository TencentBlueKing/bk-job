# AIDEV 资源同步镜像，基础镜像由 AIDEV 平台提供，其中已内置 bkai-init
# 另需基础镜像中带有 python3，用于同步前渲染资源文件中的占位符（bin/render_placeholders.py）
FROM hub.bktencent.com/blueking/bk-aidev-init:0.1.5

# 资源目录放在 /{系统标识} 下，清单路径与 values 中 bkai.packagePath 的默认值一致
WORKDIR /bk-job

# 基础镜像默认以 10001:10001 运行，且 render_placeholders.py 会就地改写资源文件，
# 故 COPY 时把属主交给运行用户，否则渲染会因无写权限失败
COPY --chown=10001:10001 bin /bk-job/bin
COPY --chown=10001:10001 bkai.yaml /bk-job/bkai.yaml
COPY --chown=10001:10001 agents /bk-job/agents
COPY --chown=10001:10001 skills /bk-job/skills
COPY --chown=10001:10001 knowledgebases /bk-job/knowledgebases

RUN chmod +x /bk-job/bin/sync-bkaidev.sh
