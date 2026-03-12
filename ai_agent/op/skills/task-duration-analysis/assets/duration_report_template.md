### 任务信息

- **request_id**: `{request_id}`
- **任务实例ID**: {taskInstanceId}
- **步骤实例ID**: {stepInstanceId}
- **执行结果**: {成功/失败/超时}

### 耗时分布详情

#### 1️⃣ 任务启动阶段 ({耗时}ms)
- **{开始时间}** - 接收API请求
- **{结束时间}** - 任务开始执行
- 涉及：CMDB API、IAM、MySQL、RabbitMQ

#### 2️⃣ Job Event 处理阶段 ({耗时}ms)
- **{开始时间}** - JobListener 处理
- **{结束时间}** - 步骤开始执行
- 涉及：RabbitMQ、MySQL

#### 3️⃣ Step Event 处理阶段 ({耗时}ms)
- **{开始时间}** - StepListener 处理
- **{结束时间}** - GSE任务创建
- 涉及：RabbitMQ、MySQL、（Artifactory）

#### 4️⃣ GSE 任务下发阶段 ({耗时}ms)
- **{开始时间}** - GseTaskListener 处理
- **{结束时间}** - GSE任务启动成功
- **GSE任务ID**: `{gseTaskId}`
- 涉及：Redis、MySQL、GSE API

#### 5️⃣ 脚本执行 + 结果轮询阶段 ({耗时}秒) ⏱️
- **开始时间**: {startTime}（GSE返回）
- **结束时间**: {endTime}（GSE返回）
- **轮询次数**: {N}次
- 涉及：GSE、MySQL、MongoDB

#### 6️⃣ 任务完成阶段 ({耗时}ms)
- **{开始时间}** - 确认执行结果
- **{结束时间}** - 刷新任务状态完成
- 涉及：RabbitMQ、MySQL、Redis
- **消息通知**：（异步，不阻塞主流程）
  - 通知队列：notifyMsg-out-0 → NotifyMsgListener
  - 调用链：job-manage → **PaaS 消息中心 API**
- **回调**：（异步，如有配置）
  - 回调队列：callback-out-0 → CallbackListener
  - 外部调用：**用户配置的回调 URL**

### 总耗时统计

| 阶段 | 耗时 | 占比 |
|------|------|------|
| 任务启动 | {X}ms | {Y}% |
| Job Event 处理 | {X}ms | {Y}% |
| Step Event 处理 | {X}ms | {Y}% |
| GSE 任务下发 | {X}ms | {Y}% |
| **脚本执行 + 轮询** | **{X}秒** | **{Y}%** |
| 任务完成 | {X}ms | {Y}% |
| **总计** | **{X}** | 100% |

### 关键发现

1. {主要耗时点分析}
2. {异常或瓶颈分析}
3. {建议或结论}
