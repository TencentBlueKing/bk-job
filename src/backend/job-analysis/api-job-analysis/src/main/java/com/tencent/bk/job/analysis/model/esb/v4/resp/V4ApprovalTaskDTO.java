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

package com.tencent.bk.job.analysis.model.esb.v4.resp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 流转接口（查询 / 推进 / 作废）的返回体
 */
@Data
public class V4ApprovalTaskDTO {

    @JsonProperty("approval_task_id")
    private String approvalTaskId;

    /**
     * 任务状态。EXPIRED 是按 expire_at 惰性判断出的呈现值，DB 中不存在该状态
     */
    @JsonProperty("status")
    private String status;

    @JsonProperty("operation_type")
    private String operationType;

    @JsonProperty("approval_channel")
    private String approvalChannel;

    /**
     * 已绑定的审批单据ID，尚未绑定时为 null
     */
    @JsonProperty("approval_ticket_id")
    private String approvalTicketId;

    @JsonProperty("creator")
    private String creator;

    /**
     * 发起时刻，Unix 时间戳，单位毫秒
     */
    @JsonProperty("create_time")
    private Long createTime;

    /**
     * 过期时刻，Unix 时间戳，单位毫秒
     */
    @JsonProperty("expire_at")
    private Long expireAt;

    /**
     * 审批人，由作业平台回查审批渠道得到，未审批时为 null
     */
    @JsonProperty("approver")
    private String approver;

    /**
     * 审批时刻，Unix 时间戳，单位毫秒；未审批时为 null
     */
    @JsonProperty("approved_at")
    private Long approvedAt;

    /**
     * 放行后的操作结果，结构与对应操作的直接执行接口返回的 data 一致；未执行时为 null
     */
    @JsonProperty("execute_result")
    private Object executeResult;

    /**
     * 执行结果未知标记：已下发执行请求但没拿到下游响应时为 true。
     * <p>
     * 此时<b>系统不会自动重试</b>，需人工到执行历史确认是否已产生作业
     */
    @JsonProperty("result_unknown")
    private Boolean resultUnknown;

    /**
     * 面向用户的可读说明
     */
    @JsonProperty("message")
    private String message;
}
