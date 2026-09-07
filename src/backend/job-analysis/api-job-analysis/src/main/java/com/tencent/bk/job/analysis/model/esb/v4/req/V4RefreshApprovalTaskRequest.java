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

package com.tencent.bk.job.analysis.model.esb.v4.req;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;

/**
 * 推进审批任务的请求体。<b>字段清单即安全契约</b>。
 * <p>
 * 只有两个 ID。<b>任何审批结论字段（status / approved / approver / approved_at / comment）
 * 一律不得出现在本类中</b>：审批结论只能由作业平台自己回查渠道得出，一旦接口层允许调用方声明"已通过"，
 * 服务层那条校验链就形同虚设，整套机制退化为"调用方自己说批了就算批了"。
 * <p>
 * 多传字段不需要额外防护：Spring Boot 默认 {@code FAIL_ON_UNKNOWN_PROPERTIES=false}，
 * 多传的结论字段本就被静默丢弃，与 400 拒绝在安全性上等价（都不会被绑定）。
 * <p>
 * <b>本类刻意不继承 {@code EsbAppScopeReq}</b>：审批任务的 app_id 只从 DB 读，不接受入参覆盖；
 * 归属校验用 DB 中的 tenant_id / creator / app_code 与请求上下文比对。
 */
@Getter
@Setter
public class V4RefreshApprovalTaskRequest {

    @JsonProperty("approval_task_id")
    @NotBlank(message = "{validation.constraints.ApprovalTask_approvalTaskIdEmpty.message}")
    private String approvalTaskId;

    /**
     * 审批渠道单据ID。只用于回查，不代表任何审批结论
     */
    @JsonProperty("approval_ticket_id")
    @NotBlank(message = "{validation.constraints.ApprovalTask_approvalTicketIdEmpty.message}")
    private String approvalTicketId;
}
