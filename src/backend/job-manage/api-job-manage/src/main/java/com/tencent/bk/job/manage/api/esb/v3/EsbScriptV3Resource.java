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
import com.tencent.bk.job.manage.model.esb.v3.request.EsbCheckScriptV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbCreateScriptV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbCreateScriptVersionV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbDeleteScriptV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbDeleteScriptVersionV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetScriptListV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetScriptVersionDetailV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetScriptVersionListV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbManageScriptVersionV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbUpdateScriptBasicV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbUpdateScriptVersionV3Req;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbCheckScriptV3DTO;
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

import java.util.List;

/**
 * 脚本相关API-V3
 */
@RequestMapping("/esb/api/v3")
@RestController
@EsbAPI
public interface EsbScriptV3Resource {

    @GetMapping("/get_script_list")
    EsbResp<EsbPageDataV3<EsbScriptV3DTO>> getScriptList(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestParam(value = "bk_biz_id", required = false) Long bizId,
        @RequestParam(value = "bk_scope_type", required = false) String scopeType,
        @RequestParam(value = "bk_scope_id", required = false) String scopeId,
        @RequestParam(value = "name", required = false) String name,
        @RequestParam(value = "script_language", required = false) Integer scriptLanguage,
        @RequestParam(value = "start", required = false) Integer start,
        @RequestParam(value = "length", required = false) Integer length);

    @GetMapping("/get_script_version_list")
    EsbResp<EsbPageDataV3<EsbScriptVersionDetailV3DTO>> getScriptVersionList(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestParam(value = "bk_biz_id", required = false) Long bizId,
        @RequestParam(value = "bk_scope_type", required = false) String scopeType,
        @RequestParam(value = "bk_scope_id", required = false) String scopeId,
        @RequestParam(value = "script_id") String scriptId,
        @RequestParam(value = "return_script_content", required = false, defaultValue = "false")
            boolean returnScriptContent,
        @RequestParam(value = "start", required = false) Integer start,
        @RequestParam(value = "length", required = false) Integer length);

    @GetMapping("/get_script_version_detail")
    EsbResp<EsbScriptVersionDetailV3DTO> getScriptVersionDetail(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestParam(value = "bk_biz_id", required = false) Long bizId,
        @RequestParam(value = "bk_scope_type", required = false) String scopeType,
        @RequestParam(value = "bk_scope_id", required = false) String scopeId,
        @RequestParam(value = "id", required = false) Long scriptVersionId,
        @RequestParam(value = "script_id", required = false) String scriptId,
        @RequestParam(value = "version", required = false) String version);

    @PostMapping("/get_script_list")
    EsbResp<EsbPageDataV3<EsbScriptV3DTO>> getScriptListUsingPost(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestBody
        @Validated
            EsbGetScriptListV3Req request
    );

    @PostMapping("/get_script_version_list")
    EsbResp<EsbPageDataV3<EsbScriptVersionDetailV3DTO>> getScriptVersionListUsingPost(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestBody
        @Validated
            EsbGetScriptVersionListV3Req request
    );

    @PostMapping("/get_script_version_detail")
    EsbResp<EsbScriptVersionDetailV3DTO> getScriptVersionDetailUsingPost(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestBody
        @Validated
            EsbGetScriptVersionDetailV3Req request
    );

    @PostMapping("/create_script")
    EsbResp<EsbScriptVersionDetailV3DTO> createScript(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestBody
        @Validated(Create.class)
            EsbCreateScriptV3Req request
    );

    @PostMapping("/create_script_version")
    EsbResp<EsbScriptVersionDetailV3DTO> createScriptVersion(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestBody
        @Validated(Create.class)
            EsbCreateScriptVersionV3Req request
    );

    @PostMapping("/delete_script")
    EsbResp deleteScript(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestBody
        @Validated(Delete.class)
            EsbDeleteScriptV3Req request
    );

    @PostMapping("/delete_script_version")
    EsbResp deleteScriptVersion(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestBody
        @Validated(Delete.class)
            EsbDeleteScriptVersionV3Req request
    );

    @PostMapping("/disable_script_version")
    EsbResp<EsbScriptVersionDetailV3DTO> disableScriptVersion(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestBody
        @Validated(Update.class)
            EsbManageScriptVersionV3Req request
    );

    @PostMapping("/publish_script_version")
    EsbResp<EsbScriptVersionDetailV3DTO> publishScriptVersion(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestBody
        @Validated(Update.class)
            EsbManageScriptVersionV3Req request
    );

    @PostMapping("/update_script_basic")
    EsbResp<EsbScriptV3DTO> updateScriptBasic(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestBody
        @Validated(Update.class)
            EsbUpdateScriptBasicV3Req request
    );

    @PostMapping("/update_script_version")
    EsbResp<EsbScriptVersionDetailV3DTO> updateScriptVersion(
        @RequestHeader(value = JobCommonHeaders.USERNAME) String username,
        @RequestHeader(value = JobCommonHeaders.APP_CODE) String appCode,
        @RequestBody
        @Validated(Update.class)
            EsbUpdateScriptVersionV3Req request
    );

    @PostMapping("/check_script")
    EsbResp<List<EsbCheckScriptV3DTO>> checkScript(
        @RequestBody
        @Validated
            EsbCheckScriptV3Req request
    );
}
