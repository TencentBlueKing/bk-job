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

import com.tencent.bk.job.analysis.model.esb.v4.req.V4FastExecuteScriptWithApprovalRequest;
import com.tencent.bk.job.analysis.model.esb.v4.resp.V4ApprovalTaskCreatedDTO;
import com.tencent.bk.job.common.annotation.EsbV4API;
import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 发起「快速执行脚本」审批。
 * <p>
 * 6 个发起接口按操作类型各自成一个资源，<b>不做统一入口</b>：APIGW 的权限、限流、授权都是资源粒度，
 * 统一入口做不到"只许某应用发起脚本执行审批、不许发起文件分发审批"。
 */
@RequestMapping("/esb/api/v4")
@EsbV4API
@RestController
@Validated
public interface OpenApiFastExecuteScriptWithApprovalV4Resource {

    /**
     * 发起审批。<b>返回体里没有任何执行结果</b>：此时只做了 dryRun 预检，作业并未执行
     */
    @PostMapping("/fast_execute_script_with_approval")
    EsbV4Response<V4ApprovalTaskCreatedDTO> fastExecuteScriptWithApproval(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestBody
        @Validated
        V4FastExecuteScriptWithApprovalRequest request
    );
}
