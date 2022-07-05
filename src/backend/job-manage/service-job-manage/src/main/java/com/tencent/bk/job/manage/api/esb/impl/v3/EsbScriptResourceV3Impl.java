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

package com.tencent.bk.job.manage.api.esb.impl.v3;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.job.v3.EsbPageDataV3;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.manage.api.esb.v3.EsbScriptV3Resource;
import com.tencent.bk.job.manage.auth.ScriptAuthService;
import com.tencent.bk.job.manage.common.consts.JobResourceStatusEnum;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetScriptListV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetScriptVersionDetailV3Req;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetScriptVersionListV3Req;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbScriptV3DTO;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbScriptVersionDetailV3DTO;
import com.tencent.bk.job.manage.model.query.ScriptQuery;
import com.tencent.bk.job.manage.service.ScriptService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class EsbScriptResourceV3Impl implements EsbScriptV3Resource {
    private final ScriptService scriptService;
    private final ScriptAuthService scriptAuthService;
    private final AppScopeMappingService appScopeMappingService;

    public EsbScriptResourceV3Impl(ScriptService scriptService,
                                   ScriptAuthService scriptAuthService,
                                   AppScopeMappingService appScopeMappingService) {
        this.scriptService = scriptService;
        this.scriptAuthService = scriptAuthService;
        this.appScopeMappingService = appScopeMappingService;
    }


    @Override
    public EsbResp<EsbPageDataV3<EsbScriptV3DTO>> getScriptList(String username,
                                                                String appCode,
                                                                Long bizId,
                                                                String scopeType,
                                                                String scopeId,
                                                                String name,
                                                                Integer scriptLanguage,
                                                                Integer start,
                                                                Integer length) {
        EsbGetScriptListV3Req request = new EsbGetScriptListV3Req();
        request.setUserName(username);
        request.setAppCode(appCode);
        request.setBizId(bizId);
        request.setScopeType(scopeType);
        request.setScopeId(scopeId);
        request.setName(name);
        request.setScriptLanguage(scriptLanguage);
        request.setStart(start);
        request.setLength(length);
        return getScriptListUsingPost(request);
    }

    @Override
    public EsbResp<EsbPageDataV3<EsbScriptVersionDetailV3DTO>> getScriptVersionList(String username,
                                                                                    String appCode,
                                                                                    Long bizId,
                                                                                    String scopeType,
                                                                                    String scopeId,
                                                                                    String scriptId,
                                                                                    boolean returnScriptContent,
                                                                                    Integer start,
                                                                                    Integer length) {
        EsbGetScriptVersionListV3Req request = new EsbGetScriptVersionListV3Req();
        request.setUserName(username);
        request.setAppCode(appCode);
        request.setBizId(bizId);
        request.setScopeType(scopeType);
        request.setScopeId(scopeId);
        request.setScriptId(scriptId);
        request.setReturnScriptContent(returnScriptContent);
        request.setStart(start);
        request.setLength(length);
        return getScriptVersionListUsingPost(request);
    }

    @Override
    public EsbResp<EsbScriptVersionDetailV3DTO> getScriptVersionDetail(String username,
                                                                       String appCode,
                                                                       Long bizId,
                                                                       String scopeType,
                                                                       String scopeId,
                                                                       Long scriptVersionId,
                                                                       String scriptId,
                                                                       String version) {
        EsbGetScriptVersionDetailV3Req request = new EsbGetScriptVersionDetailV3Req();
        request.setUserName(username);
        request.setAppCode(appCode);
        request.setBizId(bizId);
        request.setScopeType(scopeType);
        request.setScopeId(scopeId);
        request.setId(scriptVersionId);
        request.setScriptId(scriptId);
        request.setVersion(version);
        return getScriptVersionDetailUsingPost(request);
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_get_script_list"})
    public EsbResp<EsbPageDataV3<EsbScriptV3DTO>> getScriptListUsingPost(EsbGetScriptListV3Req request) {
        request.fillAppResourceScope(appScopeMappingService);
        checkEsbGetScriptListV3Req(request);

        long appId = request.getAppId();
        ScriptQuery scriptQuery = new ScriptQuery();
        scriptQuery.setAppId(appId);
        scriptQuery.setPublicScript(false);
        scriptQuery.setName(request.getName());
        // 如果script_type=0,表示查询所有类型,不需要传查询条件
        if (request.getScriptLanguage() != null && request.getScriptLanguage() > 0) {
            scriptQuery.setType(request.getScriptLanguage());
        }
        scriptQuery.setStatus(JobResourceStatusEnum.ONLINE.getValue());

        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(request.getStart());
        baseSearchCondition.setLength(request.getLength());

        PageData<ScriptDTO> pageScripts = scriptService.listPageScript(scriptQuery, baseSearchCondition);
        setOnlineScriptVersionInfo(pageScripts.getData());

        EsbPageDataV3<EsbScriptV3DTO> result = EsbPageDataV3.from(pageScripts, ScriptDTO::toEsbScriptV3DTO);
        return EsbResp.buildSuccessResp(result);
    }

    private void checkEsbGetScriptListV3Req(EsbGetScriptListV3Req request) {
        request.adjustPageParam();
        // 如果script_type=0,表示查询所有类型
        if (request.getScriptLanguage() != null
            && request.getScriptLanguage() > 0
            && ScriptTypeEnum.valueOf(request.getScriptLanguage()) == null) {
            log.warn("Param [type]:[{}] is illegal!", request.getScriptLanguage());
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "type");
        }
    }

    private void setOnlineScriptVersionInfo(List<ScriptDTO> scripts) {
        if (scripts != null && !scripts.isEmpty()) {
            List<String> scriptIdList = new ArrayList<>();
            for (ScriptDTO script : scripts) {
                scriptIdList.add(script.getId());
            }
            Map<String, ScriptDTO> onlineScriptMap = scriptService.batchGetOnlineScriptVersionByScriptIds(scriptIdList);

            for (ScriptDTO script : scripts) {
                ScriptDTO onlineScriptVersion = onlineScriptMap.get(script.getId());
                if (onlineScriptVersion != null) {
                    script.setScriptVersionId(onlineScriptVersion.getScriptVersionId());
                    script.setVersion(onlineScriptVersion.getVersion());
                }
            }
        }
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_get_script_version_list"})
    public EsbResp<EsbPageDataV3<EsbScriptVersionDetailV3DTO>> getScriptVersionListUsingPost(
        EsbGetScriptVersionListV3Req request) {
        request.fillAppResourceScope(appScopeMappingService);
        checkEsbGetScriptVersionListV3Req(request);

        long appId = request.getAppId();
        ScriptQuery scriptQuery = new ScriptQuery();
        scriptQuery.setAppId(appId);
        scriptQuery.setPublicScript(false);
        scriptQuery.setId(request.getScriptId());

        BaseSearchCondition baseSearchCondition = BaseSearchCondition.pageCondition(request.getStart(),
            request.getLength());

        PageData<ScriptDTO> pageScriptVersions = scriptService.listPageScriptVersion(scriptQuery, baseSearchCondition);

        batchAuthViewScript(request.getUserName(), request.getAppResourceScope(), pageScriptVersions.getData());

        EsbPageDataV3<EsbScriptVersionDetailV3DTO> result = EsbPageDataV3.from(pageScriptVersions,
            ScriptDTO::toEsbScriptVersionDetailV3DTO);
        if (request.getReturnScriptContent() == null || !request.getReturnScriptContent()) {
            if (CollectionUtils.isNotEmpty(result.getData())) {
                result.getData().forEach(scriptVersion -> scriptVersion.setContent(null));
            }
        }
        return EsbResp.buildSuccessResp(result);
    }

    private void checkEsbGetScriptVersionListV3Req(EsbGetScriptVersionListV3Req request) {
        if (StringUtils.isBlank(request.getScriptId())) {
            log.warn("Param [script_id] is empty!");
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "script_id");
        }
    }

    private void batchAuthViewScript(String username, AppResourceScope appResourceScope, List<ScriptDTO> scripts) {
        if (CollectionUtils.isNotEmpty(scripts)) {
            // 鉴权
            List<String> resourceIds =
                scripts.stream().map(ScriptDTO::getId).distinct().collect(Collectors.toList());
            if (!resourceIds.isEmpty()) {
                AuthResult authResult = scriptAuthService.batchAuthResultViewScript(username,
                    appResourceScope, resourceIds);
                if (!authResult.isPass()) {
                    throw new PermissionDeniedException(authResult);
                }
            }
        }
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_get_script_version_detail"})
    public EsbResp<EsbScriptVersionDetailV3DTO> getScriptVersionDetailUsingPost(
        EsbGetScriptVersionDetailV3Req request) {
        request.fillAppResourceScope(appScopeMappingService);
        checkEsbGetScriptVersionDetailV3Req(request);

        long appId = request.getAppId();
        String scriptId = request.getScriptId();
        String version = request.getVersion();
        Long id = request.getId();
        ScriptDTO scriptVersion;
        if (id != null && id > 0) {
            scriptVersion = scriptService.getScriptVersion(null, appId, id);
        } else {
            scriptVersion = scriptService.getByScriptIdAndVersion(null, appId, scriptId, version);
        }

        if (scriptVersion != null) {
            AuthResult authResult = scriptAuthService.authViewScript(request.getUserName(), request.getAppResourceScope(),
                scriptVersion.getId(), null);
            if (!authResult.isPass()) {
                throw new PermissionDeniedException(authResult);
            }
        }

        EsbScriptVersionDetailV3DTO result = null;
        if (scriptVersion != null) {
            result = scriptVersion.toEsbScriptVersionDetailV3DTO();
        }

        return EsbResp.buildSuccessResp(result);
    }

    private void checkEsbGetScriptVersionDetailV3Req(EsbGetScriptVersionDetailV3Req request) {
        if (request.getId() != null && request.getId() > 0) {
            // 如果ID合法，那么忽略其他参数
            return;
        }

        if (StringUtils.isBlank(request.getScriptId())) {
            log.warn("Param [script_id] is empty!");
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "script_id");
        }

        if (StringUtils.isBlank(request.getVersion())) {
            log.warn("Param [version] is empty!");
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "version");
        }
    }
}
