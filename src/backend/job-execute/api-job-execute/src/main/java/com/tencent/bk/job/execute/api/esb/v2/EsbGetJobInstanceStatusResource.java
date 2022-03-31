/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.job.execute.api.esb.v2;

import com.tencent.bk.job.common.annotation.EsbAPI;
import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.execute.model.esb.v2.EsbJobInstanceStatusDTO;
import com.tencent.bk.job.execute.model.esb.v2.request.EsbGetJobInstanceStatusRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 根据作业实例 ID 查询作业执行状态API-V2
 */
@RequestMapping("/esb/api/v2")
@RestController
@EsbAPI
public interface EsbGetJobInstanceStatusResource {

    @PostMapping("/get_job_instance_status")
    EsbResp<EsbJobInstanceStatusDTO> getJobInstanceStatusUsingPost(
        @RequestBody
        @Validated
            EsbGetJobInstanceStatusRequest request
    );

    @GetMapping("/get_job_instance_status")
    EsbResp<EsbJobInstanceStatusDTO> getJobInstanceStatus(
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestParam(value = "bk_biz_id", required = false) Long bizId,
        @RequestParam(value = "bk_scope_type", required = false) String scopeType,
        @RequestParam(value = "bk_scope_id", required = false) String scopeId,
        @RequestParam(value = "job_instance_id") Long taskInstanceId);


}
