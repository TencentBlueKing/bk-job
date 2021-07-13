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

package com.tencent.bk.job.manage.api.esb.v3;

import com.tencent.bk.job.common.annotation.EsbAPI;
import com.tencent.bk.job.common.constant.JobCommonHeaders;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.job.v3.EsbPageDataV3;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetTemplateListV3Request;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbTemplateBasicInfoV3DTO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @since 15/10/2020 16:19
 */
@RequestMapping("/esb/api/v3")
@RestController
@EsbAPI
public interface EsbTemplateV3Resource {

    @GetMapping("/get_job_template_list")
    EsbResp<EsbPageDataV3<EsbTemplateBasicInfoV3DTO>> getTemplateList(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestParam(value = "bk_biz_id") Long appId,
        @RequestParam(value = "creator", required = false) String creator,
        @RequestParam(value = "name", required = false) String name,
        @RequestParam(value = "create_time_start", required = false) Long createTimeStart,
        @RequestParam(value = "create_time_end", required = false) Long createTimeEnd,
        @RequestParam(value = "last_modify_user", required = false) String lastModifyUser,
        @RequestParam(value = "last_modify_time_start", required = false) Long lastModifyTimeStart,
        @RequestParam(value = "last_modify_time_end", required = false) Long lastModifyTimeEnd,
        @RequestParam(value = "start", required = false) Integer start,
        @RequestParam(value = "length", required = false) Integer length);

    @PostMapping("/get_job_template_list")
    EsbResp<EsbPageDataV3<EsbTemplateBasicInfoV3DTO>> getTemplateListUsingPost(
        @RequestBody EsbGetTemplateListV3Request request);
}
