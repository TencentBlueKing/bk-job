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

package com.tencent.bk.job.analysis.api.esb.v4;

import com.tencent.bk.job.analysis.model.esb.v4.req.V4CancelApprovalTaskRequest;
import com.tencent.bk.job.analysis.model.esb.v4.req.V4RefreshApprovalTaskRequest;
import com.tencent.bk.job.analysis.model.esb.v4.resp.V4ApprovalTaskDTO;
import com.tencent.bk.job.common.annotation.EsbV4API;
import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotBlank;

/**
 * 审批任务的流转接口：查询 / 推进 / 作废。
 * <p>
 * 三个接口对 6 种操作类型通用，因此实现类上的 {@code @AuditEntry} <b>不得写死 actionId</b>：
 * actionId 沿用各操作原有的权限点、由任务的 operation_type 在运行时决定。
 */
@RequestMapping("/esb/api/v4")
@EsbV4API
@RestController
@Validated
public interface OpenApiApprovalTaskV4Resource {

    /**
     * 查询审批任务状态，供调用方轮询与向用户反馈进度
     */
    @GetMapping("/get_approval_task")
    EsbV4Response<V4ApprovalTaskDTO> getApprovalTask(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestParam("approval_task_id")
        @NotBlank(message = "{validation.constraints.ApprovalTask_approvalTaskIdEmpty.message}")
        String approvalTaskId
    );

    /**
     * 推进审批任务：由作业平台自己回查审批结论，通过全部校验后才执行。
     * <p>
     * <b>请求体只有两个 ID，不接受任何审批结论字段</b>（无 status / approved / approver /
     * approved_at / comment）。
     */
    @PostMapping("/refresh_approval_task")
    EsbV4Response<V4ApprovalTaskDTO> refreshApprovalTask(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestBody
        @Validated
        V4RefreshApprovalTaskRequest request
    );

    /**
     * 主动作废，只把本地任务置为 CANCELED 终态，不反向通知审批渠道
     */
    @PostMapping("/cancel_approval_task")
    EsbV4Response<V4ApprovalTaskDTO> cancelApprovalTask(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestBody
        @Validated
        V4CancelApprovalTaskRequest request
    );
}
