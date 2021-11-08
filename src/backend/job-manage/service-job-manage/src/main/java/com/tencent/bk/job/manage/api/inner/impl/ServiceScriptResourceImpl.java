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

package com.tencent.bk.job.manage.api.inner.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.manage.api.common.ScriptDTOBuilder;
import com.tencent.bk.job.manage.api.inner.ServiceScriptResource;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.dto.converter.ScriptConverter;
import com.tencent.bk.job.manage.model.inner.ServiceScriptDTO;
import com.tencent.bk.job.manage.model.web.request.ScriptCreateUpdateReq;
import com.tencent.bk.job.manage.service.ScriptService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class ServiceScriptResourceImpl implements ServiceScriptResource {
    private final ScriptService scriptService;
    private final MessageI18nService i18nService;

    private final ScriptDTOBuilder scriptDTOBuilder;

    @Autowired
    public ServiceScriptResourceImpl(MessageI18nService i18nService, ScriptService scriptService,
                                     ScriptDTOBuilder scriptDTOBuilder) {
        this.i18nService = i18nService;
        this.scriptService = scriptService;
        this.scriptDTOBuilder = scriptDTOBuilder;
    }

    @Override
    public InternalResponse<ServiceScriptDTO> getScriptByAppIdAndScriptVersionId(String username, Long appId,
                                                                                 Long scriptVersionId) {

        if (appId == null || appId < 0) {
            log.warn("Get script version by id, appId is empty");
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        if (scriptVersionId <= 0) {
            log.warn("Get script version by id, param scriptVersionId is empty");
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        ScriptDTO script = scriptService.getScriptVersion(username, appId, scriptVersionId);
        if (script == null) {
            log.warn("Get script version by id:{}, the script is not exist", scriptVersionId);
            throw new NotFoundException(ErrorCode.SCRIPT_NOT_EXIST, scriptVersionId);
        }
        ServiceScriptDTO scriptVersion = ScriptConverter.convertToServiceScriptDTO(script);
        return InternalResponse.buildSuccessResp(scriptVersion);
    }

    @Override
    public InternalResponse<ServiceScriptDTO> getScriptByScriptVersionId(Long scriptVersionId) {
        if (scriptVersionId == null || scriptVersionId <= 0) {
            log.warn("Get script version by id, param scriptVersionId is empty");
            throw new InvalidParamException(ErrorCode.MISSING_PARAM);
        }

        ScriptDTO script = scriptService.getScriptVersion(scriptVersionId);
        if (script == null) {
            log.warn("Get script version by id:{}, the script is not exist", scriptVersionId);
            throw new NotFoundException(ErrorCode.SCRIPT_NOT_EXIST, scriptVersionId);
        }

        ServiceScriptDTO scriptVersion = ScriptConverter.convertToServiceScriptDTO(script);
        return InternalResponse.buildSuccessResp(scriptVersion);
    }

    @Override
    public InternalResponse<Pair<String, Long>> createScriptWithVersionId(String username, Long createTime,
                                                                     Long lastModifyTime, String lastModifyUser,
                                                                     Integer scriptStatus, Long appId,
                                                                     ScriptCreateUpdateReq scriptCreateUpdateReq) {
        scriptCreateUpdateReq.setAppId(appId);
        if (log.isDebugEnabled()) {
            log.debug("createScriptWithVersionId,operator={},appId={},script={},status={}", username, appId,
                scriptCreateUpdateReq, scriptStatus);
        }
        ScriptDTO script = scriptDTOBuilder.buildFromCreateUpdateReq(scriptCreateUpdateReq);
        script.setAppId(appId);
        script.setCreator(username);
        if (StringUtils.isNotBlank(lastModifyUser)) {
            script.setLastModifyUser(lastModifyUser);
        } else {
            script.setLastModifyUser(username);
        }
        if (scriptStatus != null && scriptStatus > 0) {
            script.setStatus(scriptStatus);
        }
        return InternalResponse.buildSuccessResp(
            scriptService.createScriptWithVersionId(username, appId, script, createTime, lastModifyTime));
    }

    @Override
    public InternalResponse<ServiceScriptDTO> getBasicScriptInfo(String scriptId) {
        if (StringUtils.isEmpty(scriptId)) {
            log.warn("Get script by id, param scriptId is empty");
            throw new InvalidParamException(ErrorCode.MISSING_PARAM);
        }

        ScriptDTO script = scriptService.getScriptWithoutTagByScriptId(scriptId);
        if (script == null) {
            log.warn("Get script by id:{}, the script is not exist", scriptId);
            throw new NotFoundException(ErrorCode.SCRIPT_NOT_EXIST, scriptId);
        }
        ServiceScriptDTO serviceScript = ScriptConverter.convertToServiceScriptDTO(script);
        return InternalResponse.buildSuccessResp(serviceScript);
    }

    @Override
    public InternalResponse<ServiceScriptDTO> getOnlineScriptVersion(String scriptId) {
        if (StringUtils.isEmpty(scriptId)) {
            log.warn("Get online script by scriptId, param scriptId is empty");
            throw new InvalidParamException(ErrorCode.MISSING_PARAM);
        }

        ScriptDTO script = scriptService.getOnlineScriptVersionByScriptId(scriptId);
        if (script == null) {
            return InternalResponse.buildSuccessResp(null);
        }
        ServiceScriptDTO serviceScript = ScriptConverter.convertToServiceScriptDTO(script);
        return InternalResponse.buildSuccessResp(serviceScript);
    }
}
