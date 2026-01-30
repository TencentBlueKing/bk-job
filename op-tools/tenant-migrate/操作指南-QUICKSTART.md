# 操作指南 - 快速开始

本文档帮助你快速完成从 BK-JOB 3.11.x 到**启用了多租户**的 3.12.x 版本的迁移。

> **⚠️ 重要：何时需要使用此迁移工具？**
> 
> - **需要迁移**：从 3.11.x 迁移到启用了多租户的 3.12.x 环境（即 values 中配置了 `tenant.enabled: true`）
> - **无需迁移**：如果目标 3.12.x 环境未启用多租户（`tenant.enabled: false`），可以直接从 3.11.x 原地升级，无需使用本迁移工具

> 详细的设计思想、表清单和技术细节请参阅 [README.md](README.md)

## 前置条件

- **bk-cmdb（蓝鲸配置平台）已完成迁移**，目标环境job_manage.application表已从bk-cmdb同步到业务信息
- 源环境（3.11.x）数据库可访问
- 目标环境（3.12.x）数据库已创建且为空
- **目标环境数据库已设置自增偏移量**（`bash tool_set_auto_increment_offset.sh`）
- 临时数据库（可以是独立的 MySQL 实例）
- MySQL 客户端和 mysqldump 工具已安装

## 一键迁移

### 1. 配置数据库连接

```bash
cd tenant-migrate
# 修改数据库配置
vim 0_config_common.sh
```

### 2. 设置自增偏移量

```bash
bash tool_set_auto_increment_offset.sh
```

> 此脚本支持重复执行：设置前会比较当前偏移量与目标偏移量，若当前 >= 目标则跳过该表并打印警告日志。

### 3. 迁移全局资源到tencent租户（仅执行一次）

```bash
# 一键迁移全局资源到 tencent 租户
bash migrate_global_to_tencent.sh
```

### 4. 批量迁移业务

```bash
# 批量迁移多个业务到 tencent 租户
bash batch_migrate_biz.sh 1,2,3,4,5 tencent

# 如果已经执行过 dump 和 prepare，可以跳过准备阶段
bash batch_migrate_biz.sh 6,7,8 tencent --skip-prepare
```

### 5. 验证

登录目标环境，确认数据完整、关联正确，手动启用需要的定时任务。

## 回滚

```bash
# 回滚全局数据
bash tool_clear_global_migration_data.sh

# 回滚业务数据
bash tool_clear_all_migration_data.sh <scopeId>
```

## 常见问题

**Q: 迁移失败后如何重试？**

```bash
bash tool_clear_all_migration_data.sh <scopeId>
bash migrate.sh <scopeId>
```

**Q: 定时任务为什么被禁用？**

为安全起见，迁移后的定时任务默认禁用，请验证后手动启用。

**Q: 可以只迁移部分业务吗？**

可以，每个业务独立迁移。
