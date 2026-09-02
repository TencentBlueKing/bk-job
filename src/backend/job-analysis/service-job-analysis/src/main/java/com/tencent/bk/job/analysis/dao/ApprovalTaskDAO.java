/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.analysis.dao;

import com.tencent.bk.job.analysis.model.dto.ApprovalTaskDTO;

/**
 * 审批任务 DAO。
 * <p>
 * <b>写操作最小化是本接口的契约</b>：这里刻意<b>不提供</b> operation_params / creator / app_code /
 * operation_type 的 update 方法 —— 让"参数快照不可篡改"成为 DAO 契约的一部分，
 * 而不是靠 Service 层自觉。新增写方法时必须重新评估这条契约。
 */
public interface ApprovalTaskDAO {

    /**
     * 插入审批任务。operation_params / resolved_summary 只在此写入
     */
    long insertApprovalTask(ApprovalTaskDTO task);

    ApprovalTaskDTO getByApprovalTaskId(String approvalTaskId);

    /**
     * 首次回查确认绑定后固化 ticketId；仅当当前值为 NULL 时更新
     *
     * @return 受影响行数，0 表示已被其他请求绑定
     */
    int bindTicketIdIfAbsent(String approvalTaskId, String approvalTicketId);

    /**
     * 记录渠道拉取审批内容的时间，仅观测
     */
    int updateTicketFetchedAt(String approvalTaskId, long fetchedAt);

    /**
     * CAS 消费：仅当 status=PENDING 且 expire_at > now 时置为 EXECUTING，
     * 同时写入 approver / approved_at / consumed_at
     *
     * @return 1 表示消费成功，0 表示已被并发消费或已过期
     */
    int casConsumeToExecuting(String approvalTaskId, String approver, long approvedAt, long consumedAt, long now);

    /**
     * 下发下游执行请求前打点，仅当 status=EXECUTING 时更新
     */
    int markDispatched(String approvalTaskId, long dispatchedAt);

    /**
     * 放行执行完成：EXECUTING → EXECUTED，回填 execute_result
     */
    int markExecuted(String approvalTaskId, String executeResultJson);

    /**
     * 下游明确业务失败：EXECUTING → FAILED，回填错误信息。
     * <p>
     * 前置条件严格限定 status=EXECUTING —— 状态机中不存在 PENDING → FAILED，
     * <b>CAS 之前的任何校验失败都不得调用本方法</b>。
     */
    int markFailed(String approvalTaskId, String errorJson);

    /**
     * 回查得到拒绝：PENDING → REJECTED。这是 CAS 之前唯一允许的状态变更
     */
    int markRejected(String approvalTaskId, String approver, Long approvedAt, String comment);

    /**
     * 主动作废：PENDING → CANCELED
     */
    int markCanceled(String approvalTaskId);

    /**
     * 清理任务用：按 create_time 分页删除，且跳过 status=EXECUTING 的任务（保留人工排障线索）
     */
    int deleteByCreateTimeBefore(long maxCreateTime, int limit);
}
