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
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbPageData;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.manage.api.esb.EsbGetScriptListResource;
import com.tencent.bk.job.manage.common.consts.JobResourceStatusEnum;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.esb.EsbScriptDTO;
import com.tencent.bk.job.manage.model.esb.request.EsbGetScriptListRequest;
import com.tencent.bk.job.manage.model.query.ScriptQuery;
import com.tencent.bk.job.manage.service.ScriptService;
import com.tencent.bk.job.manage.service.auth.EsbAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class EsbGetScriptListResourceImpl implements EsbGetScriptListResource {
    private final ScriptService scriptService;
    private final MessageI18nService i18nService;
    private final EsbAuthService authService;

    public EsbGetScriptListResourceImpl(ScriptService scriptService, MessageI18nService i18nService,
                                        EsbAuthService authService) {
        this.scriptService = scriptService;
        this.i18nService = i18nService;
        this.authService = authService;
    }


    @Override
    @EsbApiTimed(value = "esb.api", extraTags = {"api_name", "v2_get_script_list"})
    public EsbResp<EsbPageData<EsbScriptDTO>> getScriptList(EsbGetScriptListRequest request) {
        ValidateResult checkResult = checkRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Get script list, request is illegal!");
            throw new InvalidParamException(checkResult);
        }

        boolean isQueryPublicScript = (request.getPublicScript() != null && request.getPublicScript());
        boolean returnScriptContent = (request.getReturnScriptContent() != null && request.getReturnScriptContent());

        long appId = request.getAppId();
        if (isQueryPublicScript) {
            appId = JobConstants.PUBLIC_APP_ID;
        }

        ScriptQuery scriptQuery = new ScriptQuery();
        scriptQuery.setAppId(appId);
        scriptQuery.setPublicScript(isQueryPublicScript);
        scriptQuery.setName(request.getScriptName());
        // 如果script_type=0,表示查询所有类型,不需要传查询条件
        if (request.getScriptType() != null && request.getScriptType() > 0) {
            scriptQuery.setType(request.getScriptType());
        }
        scriptQuery.setStatus(JobResourceStatusEnum.ONLINE.getValue());

        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(request.getStart());
        baseSearchCondition.setLength(request.getLength());

        PageData<ScriptDTO> pageScripts = scriptService.listPageScriptVersion(scriptQuery, baseSearchCondition);
        if (returnScriptContent) {
            List<ScriptDTO> scriptDTOList = pageScripts.getData();
            if (scriptDTOList == null) {
                scriptDTOList = new ArrayList<>();
            }
            // 鉴权
            Map<String, String> idNameMap = new HashMap<>();
            // 过滤掉公共脚本
            List<String> resourceIds = scriptDTOList.parallelStream().filter(it -> !it.isPublicScript()).map(it -> {
                idNameMap.put(it.getId(), it.getName());
                return it.getId();
            }).collect(Collectors.toList());
            if (!resourceIds.isEmpty()) {
                EsbResp authFailResp = authService.batchAuthJobResources(request.getUserName(), ActionId.VIEW_SCRIPT,
                    appId, ResourceTypeEnum.SCRIPT, resourceIds, idNameMap);
                if (authFailResp != null) {
                    return authFailResp;
                }
            }
        }
        EsbPageData<EsbScriptDTO> result = convertToPageEsbScriptDTO(pageScripts, returnScriptContent);
        return EsbResp.buildSuccessResp(result);
    }

    private EsbPageData<EsbScriptDTO> convertToPageEsbScriptDTO(PageData<ScriptDTO> pageScripts,
                                                                boolean isReturnScriptContent) {
        EsbPageData<EsbScriptDTO> esbPageData = new EsbPageData<EsbScriptDTO>();
        if (pageScripts == null || pageScripts.getData() == null || pageScripts.getData().isEmpty()) {
            esbPageData.setStart(0);
            esbPageData.setTotal(0L);
            esbPageData.setData(Collections.emptyList());
            esbPageData.setPageSize(10);
            return esbPageData;
        }

        List<EsbScriptDTO> esbScriptList = new ArrayList<>();
        for (ScriptDTO script : pageScripts.getData()) {
            EsbScriptDTO esbScript = new EsbScriptDTO();
            esbScript.setAppId(script.getAppId());
            esbScript.setId(script.getScriptVersionId());
            esbScript.setCreator(script.getCreator());
            esbScript.setCreateTime(DateUtils.formatUnixTimestamp(script.getCreateTime(), ChronoUnit.MILLIS, "yyyy-MM" +
                "-dd HH:mm:ss", ZoneId.of("UTC")));
            esbScript.setLastModifyUser(script.getLastModifyUser());
            esbScript.setLastModifyTime(DateUtils.formatUnixTimestamp(script.getLastModifyTime(), ChronoUnit.MILLIS,
                "yyyy-MM-dd HH:mm:ss", ZoneId.of("UTC")));
            esbScript.setName(script.getName());
            esbScript.setPublicScript(script.isPublicScript());
            esbScript.setScriptType(script.getType());
            esbScript.setVersion(script.getVersion());
            if (isReturnScriptContent) {
                esbScript.setContent(script.getContent());
            }
            esbScriptList.add(esbScript);
        }

        esbPageData.setStart(pageScripts.getStart());
        esbPageData.setTotal(pageScripts.getTotal());
        esbPageData.setData(esbScriptList);
        esbPageData.setPageSize(pageScripts.getPageSize());
        return esbPageData;
    }

    private ValidateResult checkRequest(EsbGetScriptListRequest request) {
        boolean queryPublicScript = false;
        if (request.getPublicScript() != null && request.getPublicScript()) {
            queryPublicScript = true;
        }
        if (!queryPublicScript && (request.getAppId() == null || request.getAppId() < 1)) {
            log.warn("AppId is empty or illegal!");
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "bk_biz_id");
        }
        if (request.getStart() == null || request.getStart() < 0) {
            request.setStart(0);
        }
        // 如果script_type=0,表示查询所有类型
        if (request.getScriptType() != null && request.getScriptType() > 0
            && ScriptTypeEnum.valueOf(request.getScriptType()) == null) {
            log.warn("ScriptType:{} is illegal!", request.getScriptType());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "script_type");
        }
        if (request.getLength() == null || request.getLength() == 0) {
            request.setLength(Integer.MAX_VALUE);
        }
        return ValidateResult.pass();
    }

}
