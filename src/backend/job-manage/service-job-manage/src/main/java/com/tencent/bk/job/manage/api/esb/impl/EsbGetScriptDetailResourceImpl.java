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

package com.tencent.bk.job.manage.api.esb.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.manage.api.esb.EsbGetScriptDetailResource;
import com.tencent.bk.job.manage.auth.ScriptAuthService;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.esb.EsbScriptDTO;
import com.tencent.bk.job.manage.model.esb.request.EsbGetScriptDetailRequest;
import com.tencent.bk.job.manage.service.ScriptService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@RestController
@Slf4j
public class EsbGetScriptDetailResourceImpl implements EsbGetScriptDetailResource {
    private final ScriptService scriptService;
    private final ScriptAuthService scriptAuthService;
    private final AppScopeMappingService appScopeMappingService;

    public EsbGetScriptDetailResourceImpl(ScriptService scriptService,
                                          ScriptAuthService scriptAuthService,
                                          AppScopeMappingService appScopeMappingService) {
        this.scriptService = scriptService;
        this.scriptAuthService = scriptAuthService;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v2_get_script_detail"})
    public EsbResp<EsbScriptDTO> getScriptDetail(EsbGetScriptDetailRequest request) {
        request.fillAppResourceScope(appScopeMappingService);
        ValidateResult checkResult = checkRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Get script detail, request is illegal!");
            throw new InvalidParamException(checkResult);
        }


        Long appId = request.getAppId();
        ScriptDTO scriptVersion = scriptService.getScriptVersion(request.getScriptVersionId());
        if (scriptVersion == null) {
            log.warn("Cannot find scriptVersion by id {}", request.getScriptVersionId());
            throw new NotFoundException(ErrorCode.SCRIPT_VERSION_NOT_EXIST);
        }
        String scriptId = scriptVersion.getId();
        // 非公共脚本鉴权
        if (!scriptVersion.isPublicScript()) {
            AuthResult authResult =
                scriptAuthService.authViewScript(request.getUserName(), request.getAppResourceScope(), scriptId, null);
            if (!authResult.isPass()) {
                throw new PermissionDeniedException(authResult);
            }
        }
        if (!scriptVersion.isPublicScript()) {
            if (appId == null || appId < 1) {
                log.warn("AppId:{} is empty or illegal", request.getAppId());
                throw new InvalidParamException(ErrorCode.MISSING_OR_ILLEGAL_PARAM);
            } else {
                if (!scriptVersion.getAppId().equals(appId)) {
                    log.warn("Script:{} is not in app:{}", request.getScriptVersionId(), request.getAppId());
                    throw new NotFoundException(ErrorCode.SCRIPT_NOT_IN_APP);
                }
            }
        }

        return EsbResp.buildSuccessResp(convertToEsbScriptDTO(scriptVersion));
    }

    private EsbScriptDTO convertToEsbScriptDTO(ScriptDTO script) {
        if (script == null) {
            return null;
        }
        EsbScriptDTO esbScript = new EsbScriptDTO();
        esbScript.setLastModifyUser(script.getLastModifyUser());
        esbScript.setCreateTime(DateUtils.formatUnixTimestamp(script.getCreateTime(), ChronoUnit.MILLIS, "yyyy-MM-dd " +
            "HH:mm:ss", ZoneId.of("UTC")));
        esbScript.setAppId(script.getAppId());
        esbScript.setPublicScript(script.isPublicScript());
        esbScript.setId(script.getScriptVersionId());
        esbScript.setCreator(script.getCreator());
        esbScript.setLastModifyTime(DateUtils.formatUnixTimestamp(script.getLastModifyTime(), ChronoUnit.MILLIS,
            "yyyy-MM-dd HH:mm:ss", ZoneId.of("UTC")));
        esbScript.setName(script.getName());
        esbScript.setScriptType(script.getType());
        esbScript.setVersion(script.getVersion());
        return esbScript;
    }

    private ValidateResult checkRequest(EsbGetScriptDetailRequest request) {
        if (request.getScriptVersionId() == null || request.getScriptVersionId() < 1) {
            log.warn("ScriptVersionId:{} is empty or illegal!", request.getScriptVersionId());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "id");
        }
        return ValidateResult.pass();
    }
}
