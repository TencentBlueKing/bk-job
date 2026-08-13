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

import com.tencent.bk.job.analysis.model.esb.v4.resp.V4ApprovalContentDTO;
import com.tencent.bk.job.common.annotation.EsbV4API;
import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotBlank;

/**
 * 审批渠道拉取审批内容。
 * <p>
 * <b>服务端路径不含 {@code system} 段</b>：网关谓词 {@code /api/job/v4/job-analysis/{api_name}} 的
 * {@code api_name} 是单段变量、不跨 {@code /}。网关侧本资源登记为
 * {@code /api/v4/system/get_approval_content}，且不登记同名的用户态资源。
 */
@RequestMapping("/esb/api/v4")
@EsbV4API
@RestController
@Validated
public interface OpenApiApprovalContentV4Resource {

    /**
     * 取审批内容：调用方必须是该任务指派渠道的应用，且 username 必须是任务发起人本人。
     * 内容里含脚本明文，任一不符都按"任务不存在"返回
     */
    @GetMapping("/get_approval_content")
    EsbV4Response<V4ApprovalContentDTO> getApprovalContent(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestParam("approval_task_id")
        @NotBlank(message = "{validation.constraints.ApprovalTask_approvalTaskIdEmpty.message}")
        String approvalTaskId
    );
}
