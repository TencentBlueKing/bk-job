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
import com.tencent.bk.job.common.validation.Create;
import com.tencent.bk.job.common.validation.Delete;
import com.tencent.bk.job.common.validation.Update;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbCreatePublicScriptV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbCreatePublicScriptVersionV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbDeletePublicScriptV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbDeletePublicScriptVersionV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetPublicScriptListV3Request;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetPublicScriptVersionDetailV3Request;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetPublicScriptVersionListV3Request;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbManagePublicScriptVersionV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbUpdatePublicScriptBasicV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbUpdatePublicScriptVersionV3Req;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbScriptV3DTO;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbScriptVersionDetailV3DTO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公共脚本相关API-V3
 */
@RequestMapping("/esb/api/v3")
@RestController
@EsbAPI
public interface EsbPublicScriptV3Resource {

    @GetMapping("/get_public_script_list")
    EsbResp<EsbPageDataV3<EsbScriptV3DTO>> getPublicScriptList(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestParam(value = "name", required = false) String name,
        @RequestParam(value = "script_language", required = false) Integer scriptLanguage,
        @RequestParam(value = "start", required = false) Integer start,
        @RequestParam(value = "length", required = false) Integer length);

    @GetMapping("/get_public_script_version_list")
    EsbResp<EsbPageDataV3<EsbScriptVersionDetailV3DTO>> getPublicScriptVersionList(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestParam(value = "script_id") String scriptId,
        @RequestParam(value = "return_script_content", required = false, defaultValue = "false")
            boolean returnScriptContent,
        @RequestParam(value = "start", required = false) Integer start,
        @RequestParam(value = "length", required = false) Integer length);

    @GetMapping("/get_public_script_version_detail")
    EsbResp<EsbScriptVersionDetailV3DTO> getPublicScriptVersionDetail(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestParam(value = "id", required = false) Long scriptVersionId,
        @RequestParam(value = "script_id", required = false) String scriptId,
        @RequestParam(value = "version", required = false) String version);

    @PostMapping("/get_public_script_list")
    EsbResp<EsbPageDataV3<EsbScriptV3DTO>> getPublicScriptListUsingPost(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestBody
        @Validated
            EsbGetPublicScriptListV3Request request
    );

    @PostMapping("/get_public_script_version_list")
    EsbResp<EsbPageDataV3<EsbScriptVersionDetailV3DTO>> getPublicScriptVersionListUsingPost(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestBody
        @Validated
            EsbGetPublicScriptVersionListV3Request request
    );

    @PostMapping("/get_public_script_version_detail")
    EsbResp<EsbScriptVersionDetailV3DTO> getPublicScriptVersionDetailUsingPost(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestBody
        @Validated
            EsbGetPublicScriptVersionDetailV3Request request
    );

    @PostMapping("/create_public_script")
    EsbResp<EsbScriptVersionDetailV3DTO> createPublicScript(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestBody
        @Validated(Create.class)
            EsbCreatePublicScriptV3Req request
    );

    @PostMapping("/create_public_script_version")
    EsbResp<EsbScriptVersionDetailV3DTO> createPublicScriptVersion(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestBody
        @Validated(Create.class)
            EsbCreatePublicScriptVersionV3Req request
    );

    @PostMapping("/delete_public_script")
    EsbResp deletePublicScript(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestBody
        @Validated(Delete.class)
            EsbDeletePublicScriptV3Req request
    );

    @PostMapping("/delete_public_script_version")
    EsbResp deletePublicScriptVersion(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestBody
        @Validated(Delete.class)
            EsbDeletePublicScriptVersionV3Req request
    );

    @PostMapping("/disable_public_script_version")
    EsbResp<EsbScriptVersionDetailV3DTO> disablePublicScriptVersion(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestBody
        @Validated(Update.class)
            EsbManagePublicScriptVersionV3Req request
    );

    @PostMapping("/publish_public_script_version")
    EsbResp<EsbScriptVersionDetailV3DTO> publishPublicScriptVersion(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestBody
        @Validated(Update.class)
            EsbManagePublicScriptVersionV3Req request
    );

    @PostMapping("/update_public_script_basic")
    EsbResp<EsbScriptV3DTO> updatePublicScriptBasic(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestBody
        @Validated(Update.class)
            EsbUpdatePublicScriptBasicV3Req request
    );

    @PostMapping("/update_public_script_version")
    EsbResp<EsbScriptVersionDetailV3DTO> updatePublicScriptVersion(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestBody
        @Validated(Update.class)
            EsbUpdatePublicScriptVersionV3Req request
    );
}
