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

package com.tencent.bk.job.analysis.approval.channel.impl.model;

import lombok.Data;

/**
 * IMate 审批单详情，回查审批结论时只取判定与审计需要的字段。
 * <p>
 * <b>刻意不映射 approvalContent</b>：那是作业平台自己渲染后交给 IMate 的正文，含脚本明文，
 * 回查链路不需要它，不接进来就不会被日志或异常栈带出去。
 */
@Data
public class ImateApprovalDetail {

    /**
     * IMate 建单时固化的作业平台审批任务 ID，用作单据与任务的绑定证明
     */
    private String taskId;

    /**
     * 审批状态：PENDING / APPROVED / REJECTED / EXPIRED / CANCELED
     */
    private String status;

    /**
     * 审批人，仅 APPROVED / REJECTED 时有值
     */
    private String approver;

    private String approveComment;

    /**
     * 审批完成时间，ISO-8601 的本地时间（形如 2026-08-17T15:30:20），无时区信息
     */
    private String approveTime;
}
