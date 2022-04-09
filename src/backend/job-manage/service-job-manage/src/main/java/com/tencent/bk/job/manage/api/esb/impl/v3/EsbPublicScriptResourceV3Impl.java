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
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.manage.api.esb.v3.EsbPublicScriptV3Resource;
import com.tencent.bk.job.manage.common.consts.JobResourceStatusEnum;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetPublicScriptListV3Request;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetPublicScriptVersionDetailV3Request;
import com.tencent.bk.job.manage.model.esb.v3.request.EsbGetPublicScriptVersionListV3Request;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbScriptV3DTO;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbScriptVersionDetailV3DTO;
import com.tencent.bk.job.manage.model.query.ScriptQuery;
import com.tencent.bk.job.manage.service.ScriptService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tencent.bk.job.common.constant.JobConstants.PUBLIC_APP_ID;

@RestController
@Slf4j
public class EsbPublicScriptResourceV3Impl implements EsbPublicScriptV3Resource {

    private final ScriptService scriptService;

    @Autowired
    public EsbPublicScriptResourceV3Impl(ScriptService scriptService) {
        this.scriptService = scriptService;
    }

    @Override
    public EsbResp<EsbPageDataV3<EsbScriptV3DTO>> getPublicScriptList(String username,
                                                                      String appCode,
                                                                      String name,
                                                                      Integer scriptLanguage,
                                                                      Integer start,
                                                                      Integer length) {
        EsbGetPublicScriptListV3Request request = new EsbGetPublicScriptListV3Request();
        request.setUserName(username);
        request.setAppCode(appCode);
        request.setName(name);
        request.setScriptLanguage(scriptLanguage);
        request.setStart(start);
        request.setLength(length);
        return getPublicScriptListUsingPost(request);
    }

    @Override
    public EsbResp<EsbPageDataV3<EsbScriptVersionDetailV3DTO>> getPublicScriptVersionList(String username,
                                                                                          String appCode,
                                                                                          String scriptId,
                                                                                          boolean returnScriptContent,
                                                                                          Integer start,
                                                                                          Integer length) {
        EsbGetPublicScriptVersionListV3Request request = new EsbGetPublicScriptVersionListV3Request();
        request.setUserName(username);
        request.setAppCode(appCode);
        request.setScriptId(scriptId);
        request.setReturnScriptContent(returnScriptContent);
        request.setStart(start);
        request.setLength(length);
        return getPublicScriptVersionListUsingPost(request);
    }

    @Override
    public EsbResp<EsbScriptVersionDetailV3DTO> getPublicScriptVersionDetail(String username,
                                                                             String appCode,
                                                                             Long scriptVersionId,
                                                                             String scriptId,
                                                                             String version) {
        EsbGetPublicScriptVersionDetailV3Request request = new EsbGetPublicScriptVersionDetailV3Request();
        request.setUserName(username);
        request.setAppCode(appCode);
        request.setId(scriptVersionId);
        request.setScriptId(scriptId);
        request.setVersion(version);
        return getPublicScriptVersionDetailUsingPost(request);
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_get_public_script_list"})
    public EsbResp<EsbPageDataV3<EsbScriptV3DTO>> getPublicScriptListUsingPost(
        EsbGetPublicScriptListV3Request request) {
        checkEsbGetPublicScriptListV3Req(request);

        ScriptQuery scriptQuery = new ScriptQuery();
        scriptQuery.setPublicScript(true);
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

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_get_public_script_version_list"})
    public EsbResp<EsbPageDataV3<EsbScriptVersionDetailV3DTO>> getPublicScriptVersionListUsingPost(
        EsbGetPublicScriptVersionListV3Request request) {
        checkEsbGetPublicScriptVersionListV3Req(request);

        ScriptQuery scriptQuery = new ScriptQuery();
        scriptQuery.setPublicScript(true);
        scriptQuery.setId(request.getScriptId());

        BaseSearchCondition baseSearchCondition = BaseSearchCondition.pageCondition(request.getStart(),
            request.getLength());

        PageData<ScriptDTO> pageScriptVersions = scriptService.listPageScriptVersion(scriptQuery, baseSearchCondition);

        EsbPageDataV3<EsbScriptVersionDetailV3DTO> result = EsbPageDataV3.from(pageScriptVersions,
            ScriptDTO::toEsbScriptVersionDetailV3DTO);
        if (request.getReturnScriptContent() == null || !request.getReturnScriptContent()) {
            if (CollectionUtils.isNotEmpty(result.getData())) {
                result.getData().forEach(scriptVersion -> scriptVersion.setContent(null));
            }
        }
        return EsbResp.buildSuccessResp(result);
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v3_get_public_script_version_detail"})
    public EsbResp<EsbScriptVersionDetailV3DTO> getPublicScriptVersionDetailUsingPost(
        EsbGetPublicScriptVersionDetailV3Request request) {
        checkEsbGetPublicScriptVersionDetailV3Req(request);

        String scriptId = request.getScriptId();
        String version = request.getVersion();
        Long id = request.getId();
        ScriptDTO scriptVersion;
        if (id != null && id > 0) {
            scriptVersion = scriptService.getScriptVersion(null, PUBLIC_APP_ID, id);
        } else {
            scriptVersion = scriptService.getByScriptIdAndVersion(null, PUBLIC_APP_ID, scriptId, version);
        }

        EsbScriptVersionDetailV3DTO result = null;
        if (scriptVersion != null) {
            result = scriptVersion.toEsbScriptVersionDetailV3DTO();
        }

        return EsbResp.buildSuccessResp(result);
    }

    private void checkEsbGetPublicScriptListV3Req(EsbGetPublicScriptListV3Request request) {
        request.adjustPageParam(20, 1000);
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

    private void checkEsbGetPublicScriptVersionListV3Req(EsbGetPublicScriptVersionListV3Request request) {
        if (StringUtils.isBlank(request.getScriptId())) {
            log.warn("Param [script_id] is empty!");
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "script_id");
        }
    }

    private void checkEsbGetPublicScriptVersionDetailV3Req(EsbGetPublicScriptVersionDetailV3Request request) {
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
