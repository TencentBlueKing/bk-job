/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.bk.job.manage.api.iam.impl;

import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.iam.service.BaseIamCallbackService;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.manage.model.dto.ScriptBasicDTO;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.query.ScriptQuery;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO;
import com.tencent.bk.sdk.iam.dto.callback.request.IamSearchCondition;
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractScriptCallbackHelper extends BaseIamCallbackService {

    private final ApplicationService applicationService;

    protected AbstractScriptCallbackHelper(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    protected InstanceInfoDTO convert(ScriptDTO script) {
        InstanceInfoDTO instanceInfo = new InstanceInfoDTO();
        instanceInfo.setId(String.valueOf(script.getId()));
        instanceInfo.setDisplayName(script.getName());
        return instanceInfo;
    }

    protected ScriptQuery buildBasicScriptQuery(CallbackRequestDTO callbackRequest) {
        IamSearchCondition searchCondition = IamSearchCondition.fromReq(callbackRequest);
        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(searchCondition.getStart().intValue());
        baseSearchCondition.setLength(searchCondition.getLength().intValue());

        ScriptQuery scriptQuery = new ScriptQuery();
        boolean isPublicScript = isPublicScript();
        scriptQuery.setPublicScript(isPublicScript);
        if (!isPublicScript) {
            Long appId = applicationService.getAppIdByScope(extractResourceScopeCondition(searchCondition));
            scriptQuery.setAppId(appId);
        }
        scriptQuery.setBaseSearchCondition(baseSearchCondition);
        return scriptQuery;
    }

    protected CallbackBaseResponseDTO buildFetchInstanceResp(List<String> queryScriptIdList,
                                                             List<ScriptBasicDTO> scriptBasicDTOList) {
        List<Object> instanceAttributeInfoList = new ArrayList<>();
        Map<String, ScriptBasicDTO> scriptBasicDTOMap = new HashMap<>(scriptBasicDTOList.size());
        Set<Long> appIdSet = new HashSet<>();
        for (ScriptBasicDTO scriptBasicDTO : scriptBasicDTOList) {
            scriptBasicDTOMap.put(scriptBasicDTO.getId(), scriptBasicDTO);
            appIdSet.add(scriptBasicDTO.getAppId());
        }
        // Job app --> CMDB biz/businessSet转换
        Map<Long, ResourceScope> appIdScopeMap = applicationService.getScopeByAppIds(appIdSet);
        for (String id : queryScriptIdList) {
            ScriptBasicDTO scriptBasicDTO = scriptBasicDTOMap.get(id);
            if (scriptBasicDTO == null) {
                logNotExistId(id);
                continue;
            }
            try {
                InstanceInfoDTO instanceInfo = buildInstance(scriptBasicDTO, appIdScopeMap);
                instanceAttributeInfoList.add(instanceInfo);
            } catch (Exception e) {
                logBuildInstanceFailure(scriptBasicDTO, e);
            }
        }

        FetchInstanceInfoResponseDTO fetchInstanceInfoResponse = new FetchInstanceInfoResponseDTO();
        fetchInstanceInfoResponse.setCode(0L);
        fetchInstanceInfoResponse.setData(instanceAttributeInfoList);
        return fetchInstanceInfoResponse;
    }

    protected InstanceInfoDTO buildInstance(ScriptBasicDTO scriptBasicDTO,
                                            Map<Long, ResourceScope> appIdScopeMap) {
        // 拓扑路径构建
        List<PathInfoDTO> path = new ArrayList<>();
        PathInfoDTO rootNode = new PathInfoDTO();
        if (isPublicScript()) {
            // 公共脚本
            rootNode.setType(ResourceTypeId.PUBLIC_SCRIPT);
            rootNode.setId(scriptBasicDTO.getId());
        } else {
            // 业务脚本
            Long appId = scriptBasicDTO.getAppId();
            rootNode = getPathNodeByAppId(appId, appIdScopeMap);
            PathInfoDTO scriptNode = new PathInfoDTO();
            scriptNode.setType(ResourceTypeId.SCRIPT);
            scriptNode.setId(scriptBasicDTO.getId());
            rootNode.setChild(scriptNode);
        }
        path.add(rootNode);
        // 实例组装
        InstanceInfoDTO instanceInfo = new InstanceInfoDTO();
        instanceInfo.setId(scriptBasicDTO.getId());
        instanceInfo.setDisplayName(scriptBasicDTO.getName());
        instanceInfo.setPath(path);
        return instanceInfo;
    }

    abstract boolean isPublicScript();
}
