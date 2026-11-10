# 知识库资源目录

本目录下每一个子目录对应 AIDEV 平台上的一个知识库，目录内的 Markdown 与图片会按原有层级同步到平台。

知识库 code 在子目录的 `knowledgebase.yaml` 中声明，未声明时以目录名兜底。**code 只允许小写英文、
数字与下划线，字母开头、最长 64 位，不支持中划线**——code 直接用作 Milvus 集合名，而 Milvus 不接受中划线，
因此只能写 `bk_job_user_guide`，不能写 `bk-job-user-guide`。

目录结构示例：

```plain
knowledgebases/
└── bk_job_user_guide/          # 目录名与 knowledgebase.yaml 中的 code 保持一致
    ├── knowledgebase.yaml      # 知识库定义：code、名称、可见性、标签
    ├── README.md
    └── guides/
        └── execute.md
```

## 同步行为

- `knowledgebase.yaml` 位于知识库根目录，**不会被当作文档同步**；
- 目录内**只接受 Markdown 与图片**，其它文件类型与符号链接会被拒绝；
- 带文档同步是**镜像式**的：以本地目录为准，**线上多余的文档会被删除**，因此不要用不完整的目录去更新已有知识库；
- 平台侧限制：ZIP 最大 500 MiB、文件总大小最大 2 GiB、最多 10000 个文件；
- 导入任务完成不等于向量处理与检索效果验证完成，上线前需要单独确认检索结果。

## 维护约定

- 知识库内容由各模块单独维护，本目录只承载已确定要同步到平台的 Markdown 与图片；
- 新增知识库后，需要在 `../bkai.yaml` 的 `resources` 中补上 `knowledgebases/<code>`，且要排在引用它的 Agent 之前，否则不会被同步或同步顺序出错；
- 若需要被 Agent 引用，还需在 `../agents/bk_job_ai.yaml` 的 `spec.knowledgebases.items` 中登记该 code；
- 不要在知识库文件中写入 Token、Secret 等敏感凭证。

> 当前已纳入 `bk_job_user_guide`（作业平台使用手册）。
