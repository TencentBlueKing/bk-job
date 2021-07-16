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

package com.tencent.bk.job.execute.api.esb.v3;

import com.tencent.bk.job.common.annotation.EsbAPI;
import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.job.v3.EsbPageDataV3;
import com.tencent.bk.job.execute.model.esb.v3.EsbTaskInstanceV3DTO;
import com.tencent.bk.job.execute.model.esb.v3.request.EsbGetJobInstanceListV3Request;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 查询作业执行历史 -V3
 */
@RequestMapping("/esb/api/v3")
@RestController
@EsbAPI
public interface EsbGetJobInstanceListV3Resource {

    @PostMapping("/get_job_instance_list")
    EsbResp<EsbPageDataV3<EsbTaskInstanceV3DTO>> getJobInstanceListUsingPost(
        @RequestBody EsbGetJobInstanceListV3Request request);

    @GetMapping("/get_job_instance_list")
    EsbResp<EsbPageDataV3<EsbTaskInstanceV3DTO>> getJobInstanceList(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestParam(value = "bk_biz_id") Long appId,
        @RequestParam(value = "create_time_start") Long createTimeStart,
        @RequestParam(value = "create_time_end") Long createTimeEnd,
        @RequestParam(value = "job_instance_id", required = false) Long taskInstanceId,
        @RequestParam(value = "operator", required = false) String operator,
        @RequestParam(value = "name", required = false) String taskName,
        @RequestParam(value = "launch_mode", required = false) Integer startupMode,
        @RequestParam(value = "type", required = false) Integer taskType,
        @RequestParam(value = "status", required = false) Integer taskStatus,
        @RequestParam(value = "ip", required = false) String ip,
        @RequestParam(value = "start", required = false) Integer start,
        @RequestParam(value = "length", required = false) Integer length);


}
