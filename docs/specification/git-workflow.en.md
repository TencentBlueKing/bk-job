# BK-JOB Git Development Workflow

English | [简体中文](git-workflow.md)

## Overview

This document defines the Git development workflow for the BK-JOB project, aiming to standardize team collaboration, ensure code quality and project stability.

## Fork and Repository Management

BK-JOB adopts the **Fork + Pull Request** collaboration model. Developers need to fork the main repository to their personal GitHub account first. All feature branch development is done in the personal repository, and changes are merged into the main repository's `master` branch via PR.

### Initialize Personal Repository

```shell
# 1. Fork the bk-job main repository to your personal account
# Visit https://github.com/TencentBlueKing/bk-job and click the Fork button in the upper right corner

# 2. Clone your personal repository locally
git clone https://github.com/<your-github-username>/bk-job.git
cd bk-job

# 3. Add the main repository (upstream) as a remote
git remote add upstream https://github.com/TencentBlueKing/bk-job.git

# 4. Verify remote repository configuration
git remote -v
# origin    https://github.com/<your-github-username>/bk-job.git (fetch)
# origin    https://github.com/<your-github-username>/bk-job.git (push)
# upstream  https://github.com/TencentBlueKing/bk-job.git (fetch)
# upstream  https://github.com/TencentBlueKing/bk-job.git (push)
```

### Sync Latest Code from Main Repository

Before creating a feature branch, make sure to sync the latest code from the main repository:

```shell
git fetch upstream
git checkout master
git merge upstream/master
git push origin master
```

## Issue Tracker

The project uses GitHub Issues as the unified platform for requirement management and bug tracking:

👉 **https://github.com/TencentBlueKing/bk-job/issues**

All feature requests, bug reports and improvement suggestions should be tracked via Issues. Before starting development, please check whether a related Issue already exists to avoid duplicate work.

## Branch Management

### Main Branches

| Branch          | Description                                                                                 | Stability        |
|-----------------|---------------------------------------------------------------------------------------------|------------------|
| **master**      | Main branch (stable), contains release-ready code                                           | ⭐⭐⭐ Highest      |
| **dev**         | Integration branch, used for multi-feature integration testing and environment verification | ⭐⭐ Medium        |
| **feature/xxx** | Feature development branch, created from master, merged back to master when complete        | ⭐ In Development |

### Branch Naming Convention

| Branch Type     | Naming Format                  | Example                  |
|-----------------|--------------------------------|--------------------------|
| Feature branch  | `feature/<short-description>`  | `feature/add-user-auth`  |
| Fix branch      | `fix/<short-description>`      | `fix/login-timeout`      |
| Refactor branch | `refactor/<short-description>` | `refactor/task-executor` |

> **Note**: Branch names should use lowercase English letters, with words separated by hyphens `-`. Names should be concise and descriptive.

## Development Workflow

### Workflow Overview

```mermaid
flowchart TD
    A["🔵 upstream/master<br/>(Main repo stable branch)"]
    A0["🔀 Fork to personal repo<br/>and sync latest master"]
    B["Create feature branch in personal repo<br/><code>git checkout -b feature/xxx</code>"]
    C["🟢 feature/xxx<br/>Develop and commit on personal repo's feature branch"]
    D{"Development complete<br/>Ready to merge into dev for integration testing"}
    E["Switch to dev integration branch<br/><code>git checkout dev</code><br/><code>git pull upstream dev</code>"]
    F["Merge feature branch into dev<br/><code>git merge feature/xxx</code>"]
    G{"Any conflicts?"}
    H["⚠️ Resolve conflicts on dev integration branch<br/><code>Manually edit conflict files</code><br/><code>git add .</code><br/><code>git commit</code>"]
    I["Push to main repo dev integration branch<br/><code>git push upstream dev</code>"]
    J["🟡 Dev integration environment verification"]
    K{"Verification passed?"}
    L["Go back to personal repo's feature branch to fix<br/>Re-merge into dev after fixing"]
    N["Sync latest master on feature branch<br/><code>git fetch upstream</code><br/><code>git rebase upstream/master</code>"]
    N1{"Any conflicts?"}
    N2["⚠️ Resolve conflicts on feature branch<br/><code>Manually edit conflict files</code><br/><code>git add .</code><br/><code>git rebase --continue</code>"]
    M["✅ Personal repo feature branch → Main repo master<br/>(Via PR + Code Review)<br/><b>⚠️ Note: personal repo feature → main repo master<br/>NOT dev → master<br/>To avoid unverified commits entering master</b>"]

    A --> A0
    A0 -->|"git checkout -b"| B
    B --> C
    C --> D
    D --> E
    E --> F
    F --> G
    G -->|"No conflict"| I
    G -->|"Has conflicts"| H
    H --> I
    I --> J
    J --> K
    K -->|"❌ Failed"| L
    L --> D
    K -->|"✅ Passed"| N
    N --> N1
    N1 -->|"No conflict"| M
    N1 -->|"Has conflicts"| N2
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

### Detailed Steps

#### 1. Create Feature Branch

Sync the latest code from the main repository first, then create a feature branch in your **personal repository**:

```shell
# Sync latest code from upstream
git fetch upstream
git checkout master
git merge upstream/master

# Create and switch to feature branch (in your personal repository)
git checkout -b feature/xxx
```

#### 2. Develop and Commit on Feature Branch

Develop and commit on the feature branch. Please follow the [commit convention](./commit-spec.en.md) for commit messages.

```shell
git add .
git commit -m 'feat: add xxx feature #123'
```

#### 3. Merge into dev Branch for Integration Testing

After development is complete, merge the feature branch into the main repository's `dev` integration branch for integration testing and environment verification:

```shell
# Switch to dev branch and pull latest dev code from upstream
git checkout dev
git pull upstream dev

# Merge feature branch into dev
git merge feature/xxx
```

If conflicts occur, **resolve them on the dev branch**:

```shell
# Manually edit conflict files and resolve conflicts
git add .
git commit -m 'merge: resolve conflicts merging feature/xxx into dev'

# Push to main repo's dev integration branch
git push upstream dev
```

#### 4. Dev Environment Verification

Perform functional verification and integration testing in the dev environment:

- ✅ **Verification passed**: Proceed to the next step, submit PR to merge into main repo's master
- ❌ **Verification failed**: Go back to the feature branch in your personal repository to fix issues, then re-merge into the dev integration branch for verification

#### 5. Sync Latest Master and Resolve Conflicts

After verification passes, before submitting a PR, sync the latest master code from the main repository on your feature branch. During your development, other feature branches may have been merged into master, and submitting a PR directly may cause conflicts:

```shell
# Switch to feature branch
git checkout feature/xxx

# Fetch latest code from upstream and rebase
git fetch upstream
git rebase upstream/master
```

If conflicts occur, **resolve them on the feature branch**:

```shell
# Manually edit conflict files and resolve conflicts
git add .
git rebase --continue
# If multiple commits have conflicts, repeat the above steps until rebase is complete
```

> 💡 **Tip**: Use `rebase` instead of `merge` to sync master. This keeps the feature branch's commit history linear and clean, making Code Review easier.

#### 6. Merge into Master (via PR/MR)

After resolving conflicts, push the feature branch from your personal repository to remote, then create a Pull Request on GitHub from **your personal repository's feature branch** to **the main repository (TencentBlueKing/bk-job) `master`**:

```shell
# Push feature branch to your personal remote repository (force push needed after rebase)
git push origin feature/xxx --force-with-lease
```

Then create a PR on GitHub:
- **Source**: `<your-username>/bk-job` : `feature/xxx`
- **Target**: `TencentBlueKing/bk-job` : `master`

> ⚠️ **Important**: The PR should be from your personal repository's `feature/xxx` → main repository's `master`, **NOT** `dev` → `master`.
> The `dev` integration branch is only for integration testing and verification, and should not be merged directly into master. Since the dev branch may contain code from multiple feature branches, some of which may not have passed verification, merging dev directly into master would bring **unverified commits into the main branch**. Therefore, PRs must be submitted individually from verified feature branches.

Before submitting a PR, please ensure:

- Use `git rebase` to clean up commits (refer to [Commit Convention](./commit-spec.en.md))
- Code passes Code Review (refer to [Review Process](./review.en.md))
- Unit tests pass
- Related documentation is updated

## Commit Convention

For detailed commit convention, please refer to: [BK-JOB Commit Convention](./commit-spec.en.md)

### Quick Reference

Commit format:

```
type:message issue
```

| Mark           | Description                                 |
|----------------|---------------------------------------------|
| feat / feature | New feature development                     |
| fix            | Bug fix                                     |
| docs           | Documentation changes                       |
| style          | Code formatting (no business logic changes) |
| refactor       | Code refactoring                            |
| perf           | Performance optimization                    |
| test           | Add/modify test cases                       |
| chore          | Build scripts, tasks and related code       |
| merge          | Branch merge and sync                       |

Examples:

```shell
git commit -m 'feat: add job template import feature #456'
git commit -m 'fix: fix file distribution timeout issue #789'
```

## Important Notes

1. **Never push code directly to the master branch**. All changes must go through PR/MR + Code Review.
2. **The dev integration branch is only for integration testing and verification**. Do not merge from dev to master (dev may contain unverified commits; merging directly into master would compromise the stability of the main branch).
3. Before submitting a PR/MR, use `git rebase -i` to squash and organize commits for a clean commit history.
4. Merge conflicts should be resolved on the dev integration branch. Do not pollute the feature branch.
5. Feature branches should have a short lifecycle. Clean up merged branches promptly.
6. Link Issues: Each commit and PR/MR should reference the corresponding [GitHub Issue](https://github.com/TencentBlueKing/bk-job/issues).

## Related Documents

- [Commit Convention](./commit-spec.en.md)
- [Code Review Process](./review.en.md)
- [Contributing Guide](../../CONTRIBUTING.en.md)
