# BK-JOB Git 开发工作流规范

[English](git-workflow.en.md) | 简体中文

## 概述

本文档定义了 BK-JOB 项目的 Git 开发工作流规范，旨在规范团队协作流程，保证代码质量和项目稳定性。

## Fork 与仓库管理

 BK-JOB 采用 **Fork + Pull Request** 的协作模式。开发者需要先将主仓库 Fork 到个人 GitHub 账号下，所有的特性分支开发都在个人仓库中进行，最终通过 PR 从个人仓库的特性分支合入主仓库的 `master` 分支。

### 初始化个人仓库

```shell
# 1. 在 GitHub 上 Fork bk-job 主仓库到个人账号
# 访问 https://github.com/TencentBlueKing/bk-job，点击右上角 Fork 按钮

# 2. 克隆个人仓库到本地
git clone https://github.com/<你的GitHub用户名>/bk-job.git
cd bk-job

# 3. 添加主仓库（upstream）为远程仓库
git remote add upstream https://github.com/TencentBlueKing/bk-job.git

# 4. 验证远程仓库配置
git remote -v
# origin    https://github.com/<你的GitHub用户名>/bk-job.git (fetch)
# origin    https://github.com/<你的GitHub用户名>/bk-job.git (push)
# upstream  https://github.com/TencentBlueKing/bk-job.git (fetch)
# upstream  https://github.com/TencentBlueKing/bk-job.git (push)
```

### 同步主仓库最新代码

在创建特性分支前，务必先同步主仓库的最新代码：

```shell
git fetch upstream
git checkout master
git merge upstream/master
git push origin master
```

## 需求库

项目使用 GitHub Issues 作为统一的需求管理和 Bug 追踪平台：

👉 **https://github.com/TencentBlueKing/bk-job/issues**

所有功能需求、Bug 报告和改进建议都应通过 Issue 进行跟踪管理。在开始开发前，请先确认相关 Issue 是否已存在，避免重复工作。

## 分支管理

### 主要分支说明

| 分支              | 说明                                          | 稳定性    |
|-----------------|---------------------------------------------|--------|
| **3.x.x**       | Release 分支，各大版本的发布分支，存放可随时发布生产环境的代码         | ⭐⭐⭐ 最高 |
| **master**      | 主分支，存放当前最新版本已完成开发测试 Review 的代码，可随时发布 UAT 环境 | ⭐⭐ 次高  |
| **tenant-dev**  | 联调分支，用于多特性集成联调和环境验证                         | ⭐ 中等   |
| **feature/xxx** | 特性开发分支，从 master 拉取，开发完成后合入 master           | 开发中    |

### 分支命名建议

贡献者自己的分支命名不做强制要求，只需与 PR 内容的意图相符即可。以下是一些常见的命名方式供参考：

| 分支类型     | 命名格式              | 示例                       |
|----------|-------------------|--------------------------|
| 特性分支     | `feature/<简短描述>`  | `feature/add-user-auth`  |
| 修复分支     | `fix/<简短描述>`      | `fix/login-timeout`      |
| 重构分支     | `refactor/<简短描述>` | `refactor/task-executor` |
| Issue 关联 | `issue_<编号>`      | `issue_123`              |

> **提示**：建议分支名称使用小写英文字母，单词间用短横线 `-` 分隔，命名简洁且具有描述性，便于他人理解分支用途。

## 开发工作流

### 流程概览

```mermaid
flowchart TD
    A["🔵 upstream/master<br/>(主仓库稳定分支)"]
    A0["🔀 Fork 到个人仓库<br/>并同步最新 master"]
    B["在个人仓库创建特性分支<br/><code>git checkout -b feature/xxx</code>"]
    C["🟢 feature/xxx<br/>在个人仓库的特性分支上开发、提交"]
    D{"开发完成<br/>准备合入 tenant-dev 联调"}
    E["切换到 tenant-dev 联调分支<br/><code>git checkout tenant-dev</code><br/><code>git pull upstream tenant-dev</code>"]
    F["在 tenant-dev 上合并特性分支<br/><code>git merge feature/xxx</code>"]
    G{"是否有冲突？"}
    H["⚠️ 在 tenant-dev 联调分支上解决冲突<br/><code>手动编辑冲突文件</code><br/><code>git add .</code><br/><code>git commit</code>"]
    I["推送到主仓库 tenant-dev 联调分支<br/><code>git push upstream tenant-dev</code>"]
    J["🟡 tenant-dev 联调环境验证"]
    K{"验证通过？"}
    L["回到个人仓库特性分支继续修复<br/>修复后重新合入 tenant-dev 联调"]
    N["在特性分支上同步最新 master<br/><code>git fetch upstream</code><br/><code>git rebase 或 merge upstream/master</code>"]
    N1{"是否有冲突？"}
    N2["⚠️ 在特性分支上解决冲突<br/><code>手动编辑冲突文件</code><br/><code>git add . → rebase --continue 或 commit</code>"]
    M["✅ 个人仓库特性分支 → 主仓库 master<br/>(通过 PR + Code Review)<br/><b>⚠️ 注意：是个人仓库 feature → 主仓库 master<br/>不是 tenant-dev → master<br/>避免未验证的 commit 合入 master</b>"]

    A --> A0
    A0 -->|"git checkout -b"| B
    B --> C
    C --> D
    D --> E
    E --> F
    F --> G
    G -->|"无冲突"| I
    G -->|"有冲突"| H
    H --> I
    I --> J
    J --> K
    K -->|"❌ 不通过"| L
    L --> D
    K -->|"✅ 通过"| N
    N --> N1
    N1 -->|"无冲突"| M
    N1 -->|"有冲突"| N2
    N2 --> M
    M --> A

    style A fill:#4A90D9,stroke:#2C5F8A,color:#fff,stroke-width:2px
    style A0 fill:#9B59B6,stroke:#7D3C98,color:#fff,stroke-width:2px
    style C fill:#7BC67E,stroke:#4A8B4D,color:#fff,stroke-width:2px
    style J fill:#F5D76E,stroke:#C9A83E,color:#333,stroke-width:2px
    style M fill:#4A90D9,stroke:#2C5F8A,color:#fff,stroke-width:2px
    style H fill:#E88D67,stroke:#B5643E,color:#fff,stroke-width:2px
    style N fill:#8E44AD,stroke:#6C3483,color:#fff,stroke-width:2px
    style N2 fill:#E88D67,stroke:#B5643E,color:#fff,stroke-width:2px
```

### 详细步骤说明

#### 1. 创建特性分支

先同步主仓库最新代码，然后在**个人仓库**中创建特性分支进行开发：

```shell
# 同步主仓库（upstream）最新代码
git fetch upstream
git checkout master
git merge upstream/master

# 创建并切换到特性分支（在个人仓库中）
git checkout -b feature/xxx
```

#### 2. 在特性分支上开发与提交

在特性分支上进行日常开发和提交，提交信息请遵循 [commit 提交规范](./commit-spec.md)。

```shell
git add .
git commit -m 'feat: 新增xxx功能 #123'
```

#### 3. 合入 tenant-dev 分支进行联调验证

开发完成后，将特性分支合入主仓库的 `tenant-dev` 联调分支进行集成联调和环境验证：

```shell
# 切换到 tenant-dev 分支并拉取主仓库最新的 tenant-dev 代码
git checkout tenant-dev
git pull upstream tenant-dev

# 合并特性分支到 tenant-dev
git merge feature/xxx
```

如果出现冲突，**在 tenant-dev 分支上解决冲突**：

```shell
# 手动编辑冲突文件，解决冲突
git add .
git commit -m 'merge: 解决 feature/xxx 合入 tenant-dev 的冲突'

# 推送到主仓库的 tenant-dev 联调分支
git push upstream tenant-dev
```

#### 4. tenant-dev 环境验证

在 tenant-dev 联调环境中进行功能验证和集成联调：

- ✅ **验证通过**：进入下一步，提交 PR 合入主仓库 master
- ❌ **验证不通过**：回到个人仓库的特性分支修复问题，修复后重新合入 tenant-dev 联调分支验证

#### 5. 同步最新 master 并解决冲突

验证通过后，在提交 PR 之前，先在特性分支上同步主仓库最新的 master 代码。因为在你开发期间，可能已有其他特性分支合入了 master，直接提 PR 可能会产生冲突。

同步 master 有两种方式：`rebase`（推荐）和 `merge`，开发者可根据实际情况自行选择。

**方式一：使用 rebase（推荐，适合 commit 较少的简单需求）**

```shell
# 切换到特性分支
git checkout feature/xxx

# 拉取主仓库最新代码并 rebase
git fetch upstream
git rebase upstream/master
```

如果出现冲突，在特性分支上解决冲突：

```shell
# 手动编辑冲突文件，解决冲突
git add .
git rebase --continue
# 如有多个 commit 存在冲突，重复上述步骤直到 rebase 完成
```

**方式二：使用 merge（适合 commit 较多、需要多次同步 master 的大需求）**

```shell
# 切换到特性分支
git checkout feature/xxx

# 拉取主仓库最新代码并 merge
git fetch upstream
git merge upstream/master
```

如果出现冲突，在特性分支上解决冲突：

```shell
# 手动编辑冲突文件，解决冲突
git add .
git commit
```

> 💡 **如何选择**：
> - `rebase` 可以保持提交历史线性整洁，便于 Code Review，**推荐在 commit 较少的简单需求中使用**。
> - 对于 commit 较多的大需求，尤其是需要多次同步 master 且反复往 tenant-dev 合入验证的场景，`rebase` 的成本较高（每次 rebase 都可能需要逐个 commit 解决冲突），此时使用 `merge` 更加高效。
> - **开发者可根据实际情况自行抉择，不做强制要求。**

#### 6. 合入 master（通过 PR/MR）

解决完冲突后，将个人仓库的特性分支推送到远程，然后在 GitHub 上从 **个人仓库的特性分支** 向 **主仓库（TencentBlueKing/bk-job）的 `master`** 提交 Pull Request：

```shell
# 推送特性分支到个人远程仓库
# 如果使用了 rebase 同步 master，需要 force push
git push origin feature/xxx --force-with-lease

# 如果使用了 merge 同步 master，正常 push 即可
git push origin feature/xxx
```

然后在 GitHub 上创建 PR：
- **Source**：`<你的用户名>/bk-job` : `feature/xxx`
- **Target**：`TencentBlueKing/bk-job` : `master`

> ⚠️ **重要**：PR 是从个人仓库的 `feature/xxx` → 主仓库的 `master`，而**不是** `tenant-dev` → `master`。  
> `tenant-dev` 联调分支仅用于集成联调验证，不直接合入 master。由于 tenant-dev 联调分支可能包含多个特性分支的代码，其中部分可能尚未通过验证，直接从 tenant-dev 合入 master 会导致**未验证的 commit 被带入主分支**，因此必须从经过验证的特性分支单独提交 PR。

提交 PR 前请确保：

- 建议使用 `git rebase -i` 精简 commit（参考 [commit 提交规范](./commit-spec.md)），非强制要求
- 代码通过 Code Review（参考 [Review 流程](./review.md)）
- 单元测试通过
- 相关文档已更新

## Commit 提交规范

详细的 Commit 提交规范请参阅：[BK-JOB Commit 提交规范](./commit-spec.md)

### 快速参考

提交格式：

```
type:message issue
```

| 标记             | 说明             |
|----------------|----------------|
| feat / feature | 新功能开发          |
| fix            | Bug 修复         |
| docs           | 文档更改           |
| style          | 代码格式化（不影响业务逻辑） |
| refactor       | 代码重构           |
| perf           | 性能优化           |
| test           | 添加/修改测试用例      |
| chore          | 构建脚本、任务等相关代码   |
| merge          | 分支合并同步         |

示例：

```shell
git commit -m 'feat: 新增作业模板导入功能 #456'
git commit -m 'fix: 修复文件分发超时问题 #789'
```

## 注意事项

1. **严禁直接向 master 分支推送代码**，所有变更必须通过 PR/MR + Code Review 合入
2. **tenant-dev 联调分支仅用于集成联调验证**，不要从 tenant-dev 分支向 master 合并（tenant-dev 中可能包含未验证的 commit，直接合入 master 会破坏主分支稳定性）
3. 提交 PR/MR 前，建议使用 `git rebase -i` 合并整理 commit，保持提交历史清晰（非强制，commit 较多的大需求可酌情处理）
4. 合并冲突应在 tenant-dev 联调分支上解决，不要污染特性分支
5. 特性分支的生命周期应尽量短，完成后及时清理已合并的分支
6. 关联 Issue：每次提交和 PR/MR 都应关联对应的 [GitHub Issue](https://github.com/TencentBlueKing/bk-job/issues)

## 相关文档

- [Commit 提交规范](./commit-spec.md)
- [Code Review 流程](./review.md)
- [贡献指南](../../CONTRIBUTING.md)
