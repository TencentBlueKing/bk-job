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
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.esb.model.job.v3.EsbPageDataV3;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.manage.api.esb.v3.EsbScriptV3Resource;
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
import com.tencent.bk.job.manage.service.auth.EsbAuthService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class EsbScriptResourceV3Impl implements EsbScriptV3Resource {
    private final ScriptService scriptService;
    private final MessageI18nService i18nService;
    private final EsbAuthService authService;

    public EsbScriptResourceV3Impl(ScriptService scriptService, MessageI18nService i18nService,
                                   EsbAuthService authService) {
        this.scriptService = scriptService;
        this.i18nService = i18nService;
        this.authService = authService;
    }


    @Override
    public EsbResp<EsbPageDataV3<EsbScriptV3DTO>> getScriptList(String username,
                                                                String appCode,
                                                                Long appId,
                                                                String name,
                                                                Integer scriptLanguage,
                                                                Integer start,
                                                                Integer length) {
        EsbGetScriptListV3Req request = new EsbGetScriptListV3Req();
        request.setUserName(username);
        request.setAppCode(appCode);
        request.setAppId(appId);
        request.setName(name);
        request.setScriptLanguage(scriptLanguage);
        request.setStart(start);
        request.setLength(length);
        return getScriptListUsingPost(request);
    }

    @Override
    public EsbResp<EsbPageDataV3<EsbScriptVersionDetailV3DTO>> getScriptVersionList(String username,
                                                                                    String appCode,
                                                                                    Long appId,
                                                                                    String scriptId,
                                                                                    boolean returnScriptContent,
                                                                                    Integer start,
                                                                                    Integer length) {
        EsbGetScriptVersionListV3Req request = new EsbGetScriptVersionListV3Req();
        request.setUserName(username);
        request.setAppCode(appCode);
        request.setAppId(appId);
        request.setScriptId(scriptId);
        request.setReturnScriptContent(returnScriptContent);
        request.setStart(start);
        request.setLength(length);
        return getScriptVersionListUsingPost(request);
    }

    @Override
    public EsbResp<EsbScriptVersionDetailV3DTO> getScriptVersionDetail(String username,
                                                                       String appCode,
                                                                       Long appId,
                                                                       Long scriptVersionId,
                                                                       String scriptId,
                                                                       String version) {
        EsbGetScriptVersionDetailV3Req request = new EsbGetScriptVersionDetailV3Req();
        request.setUserName(username);
        request.setAppCode(appCode);
        request.setAppId(appId);
        request.setId(scriptVersionId);
        request.setScriptId(scriptId);
        request.setVersion(version);
        return getScriptVersionDetailUsingPost(request);
    }

    @Override
    @EsbApiTimed(value = "esb.api", extraTags = {"api_name", "v3_get_script_list"})
    public EsbResp<EsbPageDataV3<EsbScriptV3DTO>> getScriptListUsingPost(EsbGetScriptListV3Req request) {
        ValidateResult checkResult = checkRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Get script list, request is illegal!");
            throw new InvalidParamException(checkResult);
        }

        boolean isQueryPublicScript = request.getAppId() == JobConstants.PUBLIC_APP_ID;

        long appId = request.getAppId();

        ScriptQuery scriptQuery = new ScriptQuery();
        scriptQuery.setAppId(appId);
        scriptQuery.setPublicScript(isQueryPublicScript);
        scriptQuery.setName(request.getName());
        // 如果script_type=0,表示查询所有类型,不需要传查询条件
        if (request.getScriptLanguage() != null && request.getScriptLanguage() > 0) {
            scriptQuery.setType(request.getScriptLanguage());
        }
        scriptQuery.setStatus(JobResourceStatusEnum.ONLINE.getValue());

        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        int start = 0;
        if (request.getStart() != null && request.getStart() > 0) {
            start = request.getStart();
        }
        baseSearchCondition.setStart(start);
        int length = 20;
        if (request.getLength() != null && request.getLength() > 0) {
            length = request.getLength();
        }
        baseSearchCondition.setLength(length);

        PageData<ScriptDTO> pageScripts = scriptService.listPageScript(scriptQuery, baseSearchCondition);
        List<ScriptDTO> scriptDTOList = pageScripts.getData();
        if (scriptDTOList == null) {
            scriptDTOList = new ArrayList<>();
        }
        if (!isQueryPublicScript) {
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
        setOnlineScriptVersionInfo(pageScripts.getData());
        EsbPageDataV3<EsbScriptV3DTO> result = convertToPageEsbScriptV3DTO(pageScripts);
        return EsbResp.buildSuccessResp(result);
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

    private EsbPageDataV3<EsbScriptV3DTO> convertToPageEsbScriptV3DTO(PageData<ScriptDTO> pageScripts) {
        EsbPageDataV3<EsbScriptV3DTO> esbPageData = new EsbPageDataV3<>();
        if (pageScripts == null || pageScripts.getData() == null || pageScripts.getData().isEmpty()) {
            esbPageData.setStart(0);
            esbPageData.setTotal(0L);
            esbPageData.setData(Collections.emptyList());
            esbPageData.setLength(20);
            return esbPageData;
        }

        List<EsbScriptV3DTO> esbScriptList = new ArrayList<>();
        for (ScriptDTO script : pageScripts.getData()) {
            EsbScriptV3DTO esbScript = new EsbScriptV3DTO();
            esbScript.setId(script.getId());
            esbScript.setAppId(script.getAppId());
            esbScript.setName(script.getName());
            esbScript.setType(script.getType());
            esbScript.setCreator(script.getCreator());
            esbScript.setCreateTime(script.getCreateTime());
            esbScript.setLastModifyUser(script.getLastModifyUser());
            esbScript.setLastModifyTime(script.getLastModifyTime());
            esbScript.setOnlineScriptVersionId(script.getScriptVersionId());
            esbScriptList.add(esbScript);
        }

        esbPageData.setStart(pageScripts.getStart());
        esbPageData.setTotal(pageScripts.getTotal());
        esbPageData.setData(esbScriptList);
        esbPageData.setLength(pageScripts.getPageSize());
        return esbPageData;
    }

    private EsbScriptVersionDetailV3DTO toEsbScriptVerDtlV3DTO(ScriptDTO script, Boolean returnContent) {
        EsbScriptVersionDetailV3DTO esbScriptVersion = new EsbScriptVersionDetailV3DTO();
        esbScriptVersion.setId(script.getScriptVersionId());
        esbScriptVersion.setAppId(script.getAppId());
        esbScriptVersion.setScriptId(script.getId());
        esbScriptVersion.setVersion(script.getVersion());
        if (returnContent != null && returnContent) {
            esbScriptVersion.setContent(script.getContent());
        }
        esbScriptVersion.setStatus(script.getStatus());
        esbScriptVersion.setVersionDesc(script.getVersionDesc());
        esbScriptVersion.setCreator(script.getCreator());
        esbScriptVersion.setCreateTime(script.getCreateTime());
        esbScriptVersion.setLastModifyUser(script.getLastModifyUser());
        esbScriptVersion.setLastModifyTime(script.getLastModifyTime());
        return esbScriptVersion;
    }

    private EsbPageDataV3<EsbScriptVersionDetailV3DTO> toPageEsbScriptVerDtlV3DTO(
        PageData<ScriptDTO> pageScriptVersions,
        Boolean returnContent
    ) {
        EsbPageDataV3<EsbScriptVersionDetailV3DTO> esbPageData = new EsbPageDataV3<>();
        if (pageScriptVersions == null
            || pageScriptVersions.getData() == null
            || pageScriptVersions.getData().isEmpty()) {
            esbPageData.setStart(0);
            esbPageData.setTotal(0L);
            esbPageData.setData(Collections.emptyList());
            esbPageData.setLength(20);
            return esbPageData;
        }

        List<EsbScriptVersionDetailV3DTO> esbScriptList = new ArrayList<>();
        for (ScriptDTO script : pageScriptVersions.getData()) {
            EsbScriptVersionDetailV3DTO esbScriptVersion = toEsbScriptVerDtlV3DTO(script, returnContent);
            esbScriptList.add(esbScriptVersion);
        }

        esbPageData.setStart(pageScriptVersions.getStart());
        esbPageData.setTotal(pageScriptVersions.getTotal());
        esbPageData.setData(esbScriptList);
        esbPageData.setLength(pageScriptVersions.getPageSize());
        return esbPageData;
    }

    private ValidateResult checkRequest(EsbGetScriptListV3Req request) {
        if (request.getAppId() == null || request.getAppId() < -1) {
            log.warn("AppId is empty or illegal!");
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "bk_biz_id");
        }
        if (request.getStart() == null || request.getStart() < 0) {
            request.setStart(0);
        }
        // 如果script_type=0,表示查询所有类型
        if (request.getScriptLanguage() != null
            && request.getScriptLanguage() > 0
            && ScriptTypeEnum.valueOf(request.getScriptLanguage()) == null) {
            log.warn("type:{} is illegal!", request.getScriptLanguage());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "type");
        }
        if (request.getLength() == null || request.getLength() == 0) {
            request.setLength(Integer.MAX_VALUE);
        }
        return ValidateResult.pass();
    }

    private ValidateResult checkRequest(EsbGetScriptVersionListV3Req request) {
        if (request.getAppId() == null || request.getAppId() < -1) {
            log.warn("AppId is empty or illegal!");
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "bk_biz_id");
        }
        if (StringUtils.isBlank(request.getScriptId())) {
            log.warn("ScriptId is empty or illegal!");
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "script_id");
        }
        if (request.getStart() == null || request.getStart() < 0) {
            request.setStart(0);
        }
        if (request.getLength() == null || request.getLength() == 0) {
            request.setLength(Integer.MAX_VALUE);
        }
        return ValidateResult.pass();
    }

    @Override
    @EsbApiTimed(value = "esb.api", extraTags = {"api_name", "v3_get_script_version_list"})
    public EsbResp<EsbPageDataV3<EsbScriptVersionDetailV3DTO>> getScriptVersionListUsingPost(
        EsbGetScriptVersionListV3Req request
    ) {
        ValidateResult checkResult = checkRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Get scriptVersion list, request is illegal!");
            throw new InvalidParamException(checkResult);
        }

        boolean isQueryPublicScript = request.getAppId() == JobConstants.PUBLIC_APP_ID;

        long appId = request.getAppId();

        ScriptQuery scriptQuery = new ScriptQuery();
        scriptQuery.setAppId(appId);
        scriptQuery.setPublicScript(isQueryPublicScript);
        scriptQuery.setId(request.getScriptId());

        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        int start = 0;
        if (request.getStart() != null && request.getStart() > 0) {
            start = request.getStart();
        }
        baseSearchCondition.setStart(start);
        int length = 20;
        if (request.getLength() != null && request.getLength() > 0) {
            length = request.getLength();
        }
        baseSearchCondition.setLength(length);

        PageData<ScriptDTO> pageScriptVersions = scriptService.listPageScriptVersion(scriptQuery, baseSearchCondition);
        List<ScriptDTO> scriptVersionDTOList = pageScriptVersions.getData();
        if (scriptVersionDTOList == null) {
            scriptVersionDTOList = new ArrayList<>();
        }
        if (!isQueryPublicScript) {
            // 鉴权
            Map<String, String> idNameMap = new HashMap<>();
            // 过滤掉公共脚本
            List<String> resourceIds =
                scriptVersionDTOList.parallelStream().filter(it -> !it.isPublicScript()).map(it -> {
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
        EsbPageDataV3<EsbScriptVersionDetailV3DTO> result =
            toPageEsbScriptVerDtlV3DTO(pageScriptVersions, request.getReturnScriptContent());
        return EsbResp.buildSuccessResp(result);
    }

    private ValidateResult checkRequest(EsbGetScriptVersionDetailV3Req request) {
        if (request.getAppId() == null || request.getAppId() < -1) {
            log.warn("AppId is empty or illegal!");
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "bk_biz_id");
        }
        if (request.getId() != null && request.getId() > 0) {
            return ValidateResult.pass();
        }
        if (StringUtils.isBlank(request.getScriptId())) {
            log.warn("scriptId:{} is illegal!", request.getScriptId());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "script_id");
        }
        if (StringUtils.isBlank(request.getVersion())) {
            log.warn("version:{} is illegal!", request.getVersion());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "version");
        }
        return ValidateResult.pass();
    }

    @Override
    @EsbApiTimed(value = "esb.api", extraTags = {"api_name", "v3_get_script_version_detail"})
    public EsbResp<EsbScriptVersionDetailV3DTO> getScriptVersionDetailUsingPost(
        EsbGetScriptVersionDetailV3Req request) {
        ValidateResult checkResult = checkRequest(request);
        if (!checkResult.isPass()) {
            log.warn("Get scriptVersion list, request is illegal!");
            throw new InvalidParamException(checkResult);
        }

        long appId = request.getAppId();
        String scriptId = request.getScriptId();
        String version = request.getVersion();
        Long id = request.getId();
        ScriptDTO scriptVersion = null;
        if (id != null && id > 0) {
            scriptVersion = scriptService.getScriptVersion(null, appId, id);
        } else {
            scriptVersion = scriptService.getByScriptIdAndVersion(null, appId, scriptId, version);
        }
        return EsbResp.buildSuccessResp(toEsbScriptVerDtlV3DTO(scriptVersion, true));
    }
}
