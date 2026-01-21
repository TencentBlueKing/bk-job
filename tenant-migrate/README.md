# BK-JOB 多租户迁移工具

## 概述

本工具用于将老版本（3.11.x）的 BK-JOB 业务数据迁移到多租户版本（3.12.x）的 BK-JOB。

**迁移方案**：
- 新环境的 DB 是空的，老环境的数据 ID 完全不变地迁移
- **全局资源**：新环境搭好后一次性迁移
- **业务资源**：按业务单独迁移

---

## 前置条件

- ✅ **BKCC（蓝鲸配置平台）已完成迁移**，目标环境的 `job_manage.application` 表中已有业务信息
- ✅ 源环境（3.11.x）数据库可访问（只读权限即可）
- ✅ 目标环境（3.12.x）数据库已创建且为空
- ✅ **目标环境数据库已设置自增偏移量**（防止 ID 冲突）
- ✅ 临时数据库（可以是独立的 MySQL 实例）
- ✅ MySQL 客户端和 mysqldump 工具已安装

> **⚠️ 重要：为什么需要先完成 BKCC 迁移？**
> 
> 迁移工具需要根据 `bk_scope_id` 查询目标环境的 `app_id`（因为源环境和目标环境的 `app_id` 可能不同）。这个映射关系存储在目标环境的 `job_manage.application` 表中，该表的数据来源于 BKCC 的同步。如果目标环境没有业务信息，迁移将会失败。

> **⚠️ 重要：为什么需要设置自增偏移量？**
> 
> 迁移方案基于 **ID 不变** 的原则，直接将老环境的数据 ID 迁移到新环境。但新环境在部分业务迁移后开始使用，会产生新记录。为避免新记录的 ID 与待迁移数据的 ID 冲突，需要在迁移前设置新环境表的自增偏移量。
>
> ```bash
> bash tool_set_auto_increment_offset.sh
> ```
>
> **[警告] 此脚本只能执行一次，严禁在业务使用后重复执行！**

> **关于临时数据库**：可以是独立的 MySQL 实例，不需要与源数据库在同一实例。工具会自动将源数据库中需要迁移的表 dump 到临时数据库进行转换，避免影响生产数据库。

---

## 迁移操作指南

### 方式一：一键迁移（推荐）

最简单的使用方式，适合快速完成迁移。

#### 1. 配置数据库连接

```bash
cd tenant-migrate
vim 0_config_common.sh
```

只需配置数据库连接信息，**不需要修改 `defaultTenantId`**，脚本会自动设置。

#### 2. 迁移全局资源到 tencent 租户（只需执行一次）

```bash
# 一键迁移全局资源到 tencent 租户
bash migrate_global_to_tencent.sh
```

该脚本会自动执行：
- `tool_dump_source_db.sh`（dump 源数据）
- `tool_prepare_db.sh`（准备临时数据库）
- 设置 `defaultTenantId=tencent`
- `migrate_global.sh`（迁移全局资源）

#### 3. 批量迁移业务

```bash
# 批量迁移多个业务（完整流程）
bash batch_migrate_biz.sh 1,2,3,4,5 tencent

# 跳过 dump 和 prepare 阶段（适用于已准备好临时数据库的情况）
bash batch_migrate_biz.sh 1,2,3,4,5 tencent --skip-prepare
```

**参数说明**：
- 第一个参数：用逗号分隔的 `bk_scope_id` 列表
- 第二个参数：目标租户ID（如 `tencent`）
- `--skip-prepare` 或 `-s`：可选，跳过 dump 和 prepare 阶段

**特性**：
- ✅ 幂等性：可重复执行，自动清理目标环境中已有数据
- ✅ 自动化：dump 和 prepare 只执行一次，然后批量迁移
- ✅ 报告：输出成功/失败统计和耗时

> **⏱️ 耗时说明**
> 
> - **dump 阶段**（`tool_dump_source_db.sh`）：耗时取决于源数据库数据量，可能需要几分钟到几十分钟
> - **prepare 阶段**（`tool_prepare_db.sh`）：将 dump 文件导入临时数据库，通常比 dump 快
> - **迁移阶段**：每个业务的迁移时间取决于该业务的数据量
> 
> 建议在业务低峰期执行 dump 操作，以减少对生产数据库的影响。

---

### 方式二：单业务迁移

适合逐个迁移业务，或对失败的业务进行重试。

```bash
# 首次迁移
bash migrate.sh 2

# 重新迁移（需要先清理）
bash tool_clear_all_migration_data.sh 2
bash migrate.sh 2
```

**注意**：`migrate.sh` 只做迁移，不会自动清理。如果目标环境已有该业务数据，需要先执行清理脚本。

---

### 方式三：分步迁移（Gen + Apply）

最细粒度的控制方式，适合调试或需要检查中间数据的场景。

#### 全局资源

```bash
# 生成迁移数据
bash 1_gen_global_data.sh

# 检查生成的 SQL 文件
ls -la sql/global/

# 应用到目标环境
bash 2_apply_global_data.sh
```

#### 业务资源

```bash
# 生成迁移数据
bash 1_gen_migration_data.sh 2

# 检查生成的 SQL 文件
ls -la sql/2/

# 应用到目标环境
bash 2_apply_migration_data.sh 2

# 清理临时数据
bash tool_clear_tmp_data.sh 2
```

---

## 回滚操作

```bash
# 回滚全局资源
bash tool_clear_global_migration_data.sh

# 回滚单个业务
bash tool_clear_all_migration_data.sh <scopeId>
```

---

## 脚本清单

| 脚本                                    | 说明                     |
|---------------------------------------|------------------------|
| **配置文件**                              |                        |
| `0_config_common.sh`                  | 公共配置（数据库连接、租户ID）       |
| `0_config_business.sh`                | 业务迁移配置（表清单）            |
| **一键迁移**                              |                        |
| `migrate_global_to_tencent.sh`        | 一键迁移全局资源到 tencent 租户   |
| `batch_migrate_biz.sh`                | 批量迁移多个业务（清理+迁移，幂等）     |
| `migrate.sh`                          | 迁移单个业务（仅迁移）            |
| `migrate_global.sh`                   | 迁移全局资源                 |
| **分步迁移**                              |                        |
| `1_gen_global_data.sh`                | 生成全局资源迁移数据             |
| `2_apply_global_data.sh`              | 应用全局资源迁移数据             |
| `1_gen_migration_data.sh`             | 生成业务资源迁移数据             |
| `2_apply_migration_data.sh`           | 应用业务资源迁移数据             |
| **工具脚本**                              |                        |
| `tool_dump_source_db.sh`              | 从源数据库 dump 表数据         |
| `tool_prepare_db.sh`                  | 准备临时数据库                |
| `tool_clear_tmp_data.sh`              | 清理临时数据                 |
| `tool_clear_global_migration_data.sh` | 回滚全局资源                 |
| `tool_clear_all_migration_data.sh`    | 回滚业务资源                 |
| `tool_clear_running_lock.sh`          | 清理运行锁文件                |
| `tool_set_auto_increment_offset.sh`   | 设置目标数据库自增偏移量（防止 ID 冲突） |
| `tool_check_auto_increment.sh`        | 检查目标数据库当前自增值           |
| `tool_switch_cron_job.sh`             | 切换定时任务（关闭老环境、开启新环境）    |

---

## 定时任务切换

迁移后的定时任务在新环境默认是**关闭**状态。当确认迁移无误后，可以使用 `tool_switch_cron_job.sh` 批量切换定时任务状态：

```bash
bash tool_switch_cron_job.sh <tenantId> <scopeId> \
  --oldUrl 'http://bkapigw-old.example.com' \
  --oldAppCode 'bk_job' \
  --oldAppSecret 'xxx' \
  --oldEnvOperator 'xxx' \
  --newUrl 'http://bkapigw-new.example.com' \
  --newAppCode 'bk_job' \
  --newAppSecret 'xxx' \
  --newEnvOperator 'xxx'
```

**参数说明**：
| 参数 | 说明 |
|------|------|
| `tenantId` | 新环境的租户ID |
| `scopeId` | 业务的 bk_scope_id |
| `--oldUrl` | 老环境 bk-apigateway 地址 |
| `--oldAppCode` | 老环境 bk_app_code |
| `--oldAppSecret` | 老环境 bk_app_secret |
| `--oldEnvOperator` | 老环境操作人（bk_username） |
| `--newUrl` | 新环境 bk-apigateway 地址 |
| `--newAppCode` | 新环境 bk_app_code |
| `--newAppSecret` | 新环境 bk_app_secret |
| `--newEnvOperator` | 新环境操作人（bk_username） |

**工作流程**：
1. 从老环境数据库读取该业务下 `is_enable=1` 的定时任务 ID 列表
2. 调用老环境 API 关闭这些定时任务（`/api/v3/system/update_cron_status`）
3. 调用新环境 API 开启这些定时任务（`/api/v3/system/update_cron_status`，带 `X-Bk-Tenant-Id` 头）

---

## 迁移资源清单

### 全局资源（一次性迁移）

| 数据库        | 表                        | 迁移条件            | 特殊处理           |
|------------|--------------------------|-----------------|----------------|
| job_manage | `dangerous_rule`         | 全部              | 补充 `tenant_id` |
| job_manage | `notify_black_user_info` | 全部              | 补充 `tenant_id` |
| job_manage | `script`                 | `is_public = 1` | 补充 `tenant_id` |

### 业务资源（按业务迁移）

#### job_manage 库

| 表                              | 迁移条件                                | 关联关系                           | 特殊处理           |
|--------------------------------|-------------------------------------|--------------------------------|----------------|
| `account`                      | `app_id = {业务ID}`                   | -                              | -              |
| `script`                       | `app_id = {业务ID}` 且 `is_public = 0` | -                              | 补充 `tenant_id` |
| `script_version`               | `script_id IN (...)`                | 关联 `script`                    | -              |
| `task_template`                | `app_id = {业务ID}`                   | -                              | -              |
| `task_template_step`           | `template_id IN (...)`              | 关联 `task_template`             | -              |
| `task_template_step_approval`  | `step_id IN (...)`                  | 关联 `task_template_step`        | -              |
| `task_template_step_file`      | `step_id IN (...)`                  | 关联 `task_template_step`        | -              |
| `task_template_step_file_list` | `step_id IN (...)`                  | 关联 `task_template_step`        | -              |
| `task_template_step_script`    | `step_id IN (...)`                  | 关联 `task_template_step`        | -              |
| `task_template_variable`       | `template_id IN (...)`              | 关联 `task_template`             | -              |
| `task_plan`                    | `app_id = {业务ID}`                   | -                              | -              |
| `task_plan_step`               | `plan_id IN (...)`                  | 关联 `task_plan`                 | -              |
| `task_plan_step_approval`      | `step_id IN (...)`                  | 关联 `task_plan_step`            | -              |
| `task_plan_step_file`          | `step_id IN (...)`                  | 关联 `task_plan_step`            | -              |
| `task_plan_step_file_list`     | `step_id IN (...)`                  | 关联 `task_plan_step`            | -              |
| `task_plan_step_script`        | `step_id IN (...)`                  | 关联 `task_plan_step`            | -              |
| `task_plan_variable`           | `plan_id IN (...)`                  | 关联 `task_plan`                 | -              |
| `credential`                   | `app_id = {业务ID}`                   | -                              | -              |
| `notify_trigger_policy`        | `app_id = {业务ID}`                   | -                              | -              |
| `notify_policy_role_target`    | `policy_id IN (...)`                | 关联 `notify_trigger_policy`     | -              |
| `notify_role_target_channel`   | `role_target_id IN (...)`           | 关联 `notify_policy_role_target` | -              |
| `task_favorite_plan`           | `app_id = {业务ID}`                   | -                              | -              |
| `task_favorite_template`       | `app_id = {业务ID}`                   | -                              | -              |
| `tag`                          | `app_id = {业务ID}`                   | -                              | -              |
| `resource_tag`                 | `tag_id IN (...)`                   | 关联 `tag`                       | -              |

#### job_crontab 库

| 表          | 迁移条件              | 特殊处理                |
|------------|-------------------|---------------------|
| `cron_job` | `app_id = {业务ID}` | `is_enable` 设置为 `0` |

#### job_file_gateway 库

| 表                   | 迁移条件                      | 关联关系             | 特殊处理              |
|---------------------|---------------------------|------------------|-------------------|
| `file_source`       | `app_id = {业务ID}`         | -                | 补充 `tenant_id`    |
| `file_source_share` | `file_source_id IN (...)` | 关联 `file_source` | 处理跨业务 `app_id` 映射 |

---

## 自增偏移量配置

为防止新环境产生的数据 ID 与迁移数据冲突，需要在迁移前设置目标数据库表的自增偏移量。

> **[警告] 此脚本只能执行一次！** 在业务使用后重复执行会导致主键冲突、业务故障。如果不确定是否已执行过，请先检查目标数据库表的 `AUTO_INCREMENT` 值。

```bash
# 设置偏移量
bash tool_set_auto_increment_offset.sh

# （可选）检查当前偏移量（可用于验证设置结果）
bash tool_check_auto_increment.sh
```

### job_execute 数据库

| 表                             | 偏移量                  |
|-------------------------------|----------------------|
| `task_instance`               | 30000000000 (300亿)   |
| `step_instance`               | 30000000000 (300亿)   |
| `step_instance_variable`      | 20000000 (2000万)     |
| `step_instance_rolling_task`  | 100000               |
| `task_instance_variable`      | 1000000000 (10亿)     |
| `gse_task`                    | 50000000000 (500亿)   |
| `gse_script_agent_task`       | 50000000000 (500亿)   |
| `gse_script_execute_obj_task` | 200000000000 (2000亿) |
| `gse_file_agent_task`         | 50000000000 (500亿)   |
| `gse_file_execute_obj_task`   | 200000000000 (2000亿) |
| `operation_log`               | 1000000000 (10亿)     |
| `file_source_task_log`        | 3000000              |
| `rolling_config`              | 40000                |

> 不需要设置的表：`step_instance_script`、`step_instance_file`、`step_instance_confirm`（跟随 step_instance）、`task_instance_host`（复合主键）

### job_manage 数据库

| 表                              | 偏移量     |
|--------------------------------|---------|
| `dangerous_rule`               | 100     |
| `notify_black_user_info`       | 100     |
| `account`                      | 31000   |
| `script_version`               | 4100000 |
| `task_template`                | 400000  |
| `task_template_step`           | 400000  |
| `task_template_step_approval`  | 400000  |
| `task_template_step_file`      | 400000  |
| `task_template_step_file_list` | 400000  |
| `task_template_step_script`    | 400000  |
| `task_template_variable`       | 4100000 |
| `task_plan`                    | 5100000 |
| `task_plan_step`               | 500000  |
| `task_plan_step_approval`      | 400000  |
| `task_plan_step_file`          | 400000  |
| `task_plan_step_file_list`     | 400000  |
| `task_plan_step_script`        | 500000  |
| `task_plan_variable`           | 400000  |
| `notify_trigger_policy`        | 3000    |
| `notify_policy_role_target`    | 3000    |
| `notify_role_target_channel`   | 3000    |
| `task_favorite_plan`           | 2000    |
| `task_favorite_template`       | 2000    |
| `tag`                          | 2000    |
| `resource_tag`                 | 5000    |

> 不需要设置的表：`credential`、`script`（使用 UUID 主键）

### job_crontab 数据库

| 表          | 偏移量     |
|------------|---------|
| `cron_job` | 4100000 |

### job_file_gateway 数据库

| 表             | 偏移量  |
|---------------|------|
| `file_source` | 2000 |

> 不需要设置的表：`file_source_share`（跟随 file_source.id）

---

## 技术细节

### app_id 处理

源环境和目标环境的 `bk_scope_id` 相同，但 `app_id` 可能不同（因为两套环境各自从 CMDB 同步，自增值不同）。

迁移工具会：
1. 根据 `bk_scope_id` 查询目标环境的 `app_id`
2. 用目标环境的 `app_id` 替换临时数据中的 `app_id`
3. 对于 `file_source_share` 表，处理跨业务的 `app_id` 映射（若目标环境不存在该业务则删除记录）

### tenant_id 字段处理

以下表在 V3.12.0 中新增了 `tenant_id` 字段，迁移时自动补充：

| 表                        | 默认值                  |
|--------------------------|----------------------|
| `dangerous_rule`         | `${defaultTenantId}` |
| `notify_black_user_info` | `${defaultTenantId}` |
| `script`                 | `${defaultTenantId}` |
| `file_source`            | `${defaultTenantId}` |

### 并发控制

- 使用锁文件防止同一业务并发迁移
- 每次只能迁移一个业务

### 幂等性

- `batch_migrate_biz.sh`：每个业务迁移前自动清理，可重复执行
- `migrate.sh`：不自动清理，重复执行需先手动清理

---

## 注意事项

1. **设置自增偏移量（只能执行一次）**：迁移前必须先运行 `tool_set_auto_increment_offset.sh`，严禁在业务使用后重复执行，否则会导致主键冲突
2. **定时任务安全**：所有迁移的定时任务自动禁用（`is_enable=0`），验证后手动启用
3. **ID 保持不变**：新环境 DB 为空，数据 ID 完全保持不变
4. **文件源共享级联**：`file_source_share` 采用级联迁移，跨业务共享记录会自动处理 `app_id` 映射

---

## 常见问题

**Q: 迁移失败如何重试？**

```bash
bash tool_clear_all_migration_data.sh <scopeId>
bash migrate.sh <scopeId>
```

**Q: 定时任务为什么被禁用？**

为安全起见，迁移后的定时任务默认禁用。请在验证无误后手动启用。

**Q: 可以只迁移部分业务吗？**

可以。每个业务独立迁移，按需执行。

**Q: 如何确认迁移成功？**

对比源环境和目标环境的数据量：
```sql
-- 源环境
SELECT COUNT(*) FROM job_manage.task_template WHERE app_id = 100;

-- 目标环境
SELECT COUNT(*) FROM job_manage.task_template WHERE app_id = {目标环境app_id};
```

---

## 技术支持

如有问题，请联系 BK-JOB 团队。
