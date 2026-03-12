---
name: task-duration-analysis
description: 蓝鲸作业平台任务耗时分布分析专家。当用户需要：(1) 分析任务各阶段耗时分布，(2) 定位任务执行中的性能瓶颈，(3) 了解任务执行链路和各阶段涉及的中间件，(4) 生成任务耗时分布统计报告时使用。基于六阶段模型，通过 request_id 查询完整链路日志进行分析。
---

# 作业平台任务耗时分布分析

## 概述

当用户询问任务耗时分布时，按照以下六个阶段进行分析。分析时只需通过 `request_id` 查询整个链路的完整日志，根据日志内容和时间戳自行判断各阶段的耗时情况。

**重要**：不要使用特定的日志关键字构造查询语句，只需查询完整链路日志即可总结出耗时分布。

## 耗时分析六阶段模型

### 第一阶段：任务启动阶段（准备阶段）

**阶段描述**：从接收 API 请求到发送 JobEvent 启动作业

**涉及中间件/服务**：

| 中间件/服务 | 用途 |
|------------|------|
| **CMDB API** | 解析主机变量、动态分组、拓扑节点，获取主机信息 |
| **IAM API** | 权限鉴权 |
| **MySQL** | 保存任务实例、步骤实例 |
| **RabbitMQ** | 发送 JobEvent 到 task-out-0 队列 |

### 第二阶段：Job Event 处理阶段

**阶段描述**：JobListener 消费 JobEvent，触发步骤执行

**涉及中间件/服务**：

| 中间件/服务 | 用途 |
|------------|------|
| **RabbitMQ** | 消费 task-in-0 队列的 JobEvent |
| **MySQL** | 查询任务实例、更新任务状态 |
| **RabbitMQ** | 发送 StepEvent 到 step-out-0 队列 |

### 第三阶段：Step Event 处理阶段

**阶段描述**：StepListener 消费 StepEvent，准备 GSE 任务

**涉及中间件/服务**：

| 中间件/服务 | 用途 |
|------------|------|
| **RabbitMQ** | 消费 step-in-0 队列的 StepEvent |
| **MySQL** | 创建执行对象任务、创建 GseTask 记录 |
| **Artifactory/文件源** | 文件分发时准备源文件（仅文件分发步骤） |
| **RabbitMQ** | 发送 GseTaskEvent 到 gseTask-out-0 队列 |

### 第四阶段：GSE 任务下发阶段

**阶段描述**：GseTaskListener 消费 GseTaskEvent，调用 GSE API 下发任务

**涉及中间件/服务**：

| 中间件/服务 | 用途 |
|------------|------|
| **RabbitMQ** | 消费 gseTask-in-0 队列的 GseTaskEvent |
| **Redis** | 获取分布式锁 |
| **MySQL** | 加载任务和步骤实例 |
| **GSE API** | 下发脚本执行/文件分发任务 |
| **MySQL** | 更新 GSE 任务状态 |

### 第五阶段：脚本执行 + 结果轮询阶段 ⏱️ 【主要耗时】

**阶段描述**：脚本在目标机器上执行 + Job 系统周期性轮询 GSE 获取执行结果

**涉及中间件/服务**：

| 中间件/服务 | 用途 |
|------------|------|
| **GSE（目标机器）** | 实际执行脚本/文件分发 |
| **GSE API** | 周期性查询任务执行结果 |
| **MySQL** | 批量更新执行对象任务状态 |
| **MongoDB（job-logsvr）** | 保存脚本执行日志 |

**备注**：脚本实际执行时间可通过 GSE 返回的 `startTime` 和 `endTime` 时间戳计算得出。

### 第六阶段：任务完成阶段

**阶段描述**：任务执行完成，更新状态、发送通知、执行回调

**涉及中间件/服务**：

| 中间件/服务 | 用途 |
|------------|------|
| **RabbitMQ** | 发送/消费 StepEvent.refreshStep 和 JobEvent.refreshJob |
| **MySQL** | 更新任务/步骤最终状态和结束时间 |
| **Redis** | 清理目标主机密码缓存 |
| **RabbitMQ** | 发送通知消息到 notifyMsg-out-0 队列 |
| **job-manage 服务** | 调用 ServiceNotificationResource 接口处理通知 |
| **PaaS 消息中心 API** | 最终发送通知（微信、邮件、短信等渠道） |
| **RabbitMQ** | 发送回调消息到 callback-out-0 队列（如配置了回调） |
| **外部 HTTP** | 执行用户配置的回调 URL（支持 JSON 和 FormData 格式） |

**备注**：
- 消息通知流程：`finishJob()` → MQ → `NotifyMsgListener` → `job-manage` → **PaaS 消息中心 API**
- 回调流程：`finishJob()` → MQ → `CallbackListener` → **外部 HTTP 调用**（用户配置的回调地址）

## 分析工作流

1. 通过 `request_id` 查询完整链路日志（使用 `searchLogsByCondition` 工具）
2. 根据日志时间戳，识别各阶段的起止时间
3. 计算各阶段耗时和占比
4. 读取 [assets/duration_report_template.md](assets/duration_report_template.md) 获取输出模板
5. 按模板格式输出耗时分布统计报告
6. 分析主要耗时点并给出建议

## 环境配置

### 虚拟环境设置

此 Skill 包含必需的环境配置文件：

- **requirements.txt**: Python 依赖列表，至少包含 `requests>=2.28.0`
- **setup_venv.sh**: 自动化虚拟环境设置脚本

**首次使用前，必须运行：**

```bash
./setup_venv.sh
```

这将创建 venv 虚拟环境并安装所有依赖。

**后续使用时：**

```bash
# 激活虚拟环境
source venv/bin/activate

# 运行脚本
python scripts/example.py

# 退出虚拟环境
deactivate
```

**添加新依赖：**

1. 编辑 `requirements.txt`，添加新包（例如 `pandas>=1.5.0`）
2. 重新运行 `./setup_venv.sh` 安装新依赖

## 资源

此 Skill 包含示例资源目录，展示了如何组织不同类型的捆绑资源:

### scripts/
可执行代码（Python/Bash 等），可以直接运行以执行特定操作。

**其他 Skills 的示例:**
- PDF skill: `fill_fillable_fields.py`, `extract_form_field_info.py` - PDF 操作工具
- DOCX skill: `document.py`, `utilities.py` - 文档处理的 Python 模块

**适用于:** Python 脚本、shell 脚本或任何执行自动化、数据处理或特定操作的可执行代码。

**注意:**
- 脚本可以在不加载到上下文的情况下执行，但 Claude 仍然可以读取它们以进行修补或环境调整
- **始终在虚拟环境中运行 Python 脚本**：`source venv/bin/activate && python scripts/your_script.py`

### references/
旨在加载到上下文中的文档和参考资料，用于指导 Claude 的流程和思考。

**其他 Skills 的示例:**
- Product management: `communication.md`, `context_building.md` - 详细的工作流指南
- BigQuery: API 参考文档和查询示例
- Finance: Schema 文档、公司政策

**适用于:** 深入的文档、API 参考、数据库 Schema、综合指南，或 Claude 在工作时应参考的任何详细信息。

### assets/
不打算加载到上下文中的文件，而是在 Claude 生成的输出中使用。

**其他 Skills 的示例:**
- Brand styling: PowerPoint 模板文件（.pptx）、logo 文件
- Frontend builder: HTML/React 样板项目目录
- Typography: 字体文件（.ttf、.woff2）

**适用于:** 模板、样板代码、文档模板、图像、图标、字体，或任何要在最终输出中复制或使用的文件。

---

**可以删除任何不需要的目录。** 并非每个 Skill 都需要全部三种类型的资源。
