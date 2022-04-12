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

package com.tencent.bk.job.manage.api.iam.impl;

import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.iam.service.BaseIamCallbackService;
import com.tencent.bk.job.common.iam.util.IamRespUtil;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.manage.api.iam.IamTaskTemplateCallbackResource;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.query.TaskTemplateQuery;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO;
import com.tencent.bk.sdk.iam.dto.callback.request.IamSearchCondition;
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.SearchInstanceResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
public class IamTaskTemplateCallbackResourceImpl extends BaseIamCallbackService
    implements IamTaskTemplateCallbackResource {

    private final TaskTemplateService templateService;
    private final ApplicationService applicationService;

    @Autowired
    public IamTaskTemplateCallbackResourceImpl(TaskTemplateService templateService,
                                               ApplicationService applicationService) {
        this.templateService = templateService;
        this.applicationService = applicationService;
    }

    private InstanceInfoDTO convert(TaskTemplateInfoDTO templateInfo) {
        InstanceInfoDTO instanceInfo = new InstanceInfoDTO();
        instanceInfo.setId(String.valueOf(templateInfo.getId()));
        instanceInfo.setDisplayName(templateInfo.getName());
        return instanceInfo;
    }

    private TaskTemplateQuery getBasicQueryCondition(CallbackRequestDTO callbackRequest) {
        IamSearchCondition searchCondition = IamSearchCondition.fromReq(callbackRequest);
        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(searchCondition.getStart().intValue());
        baseSearchCondition.setLength(searchCondition.getLength().intValue());
        Long appId = applicationService.getAppIdByScope(extractResourceScopeCondition(searchCondition));
        return TaskTemplateQuery.builder()
            .appId(appId)
            .baseSearchCondition(baseSearchCondition)
            .build();
    }

    @Override
    protected ListInstanceResponseDTO listInstanceResp(CallbackRequestDTO callbackRequest) {
        TaskTemplateQuery query = getBasicQueryCondition(callbackRequest);

        PageData<TaskTemplateInfoDTO> templateInfoPageData =
            templateService.listPageTaskTemplatesBasicInfo(query, null);

        return IamRespUtil.getListInstanceRespFromPageData(templateInfoPageData, this::convert);
    }

    @Override
    protected SearchInstanceResponseDTO searchInstanceResp(CallbackRequestDTO callbackRequest) {
        TaskTemplateQuery query = getBasicQueryCondition(callbackRequest);
        query.setName(callbackRequest.getFilter().getKeyword());
        PageData<TaskTemplateInfoDTO> templateDTOPageData =
            templateService.listPageTaskTemplatesBasicInfo(query, null);

        return IamRespUtil.getSearchInstanceRespFromPageData(templateDTOPageData, this::convert);
    }

    @Override
    protected CallbackBaseResponseDTO fetchInstanceResp(
        CallbackRequestDTO callbackRequest
    ) {
        IamSearchCondition searchCondition = IamSearchCondition.fromReq(callbackRequest);
        List<Object> instanceAttributeInfoList = new ArrayList<>();
        List<Long> templateIdList = new ArrayList<>();
        for (String instanceId : searchCondition.getIdList()) {
            try {
                long id = Long.parseLong(instanceId);
                templateIdList.add(id);
            } catch (NumberFormatException e) {
                log.error("Parse template id failed!|{}", instanceId, e);
            }
        }
        List<TaskTemplateInfoDTO> taskTemplateInfoDTOList =
            templateService.listTaskTemplateBasicInfoByIds(templateIdList);
        Map<Long, TaskTemplateInfoDTO> templateInfoDTOMap = new HashMap<>(taskTemplateInfoDTOList.size());
        Set<Long> appIdSet = new HashSet<>();
        for (TaskTemplateInfoDTO templateInfoDTO : taskTemplateInfoDTOList) {
            templateInfoDTOMap.put(templateInfoDTO.getId(), templateInfoDTO);
            appIdSet.add(templateInfoDTO.getAppId());
        }
        // Job app --> CMDB biz/businessSet转换
        Map<Long, ResourceScope> appIdScopeMap = applicationService.getScopeByAppIds(appIdSet);
        for (String instanceId : searchCondition.getIdList()) {
            try {
                long id = Long.parseLong(instanceId);
                TaskTemplateInfoDTO templateInfoDTO = templateInfoDTOMap.get(id);
                if (templateInfoDTO == null) {
                    return getNotFoundRespById(instanceId);
                }
                Long appId = templateInfoDTO.getAppId();
                // 拓扑路径构建
                List<PathInfoDTO> path = new ArrayList<>();
                PathInfoDTO rootNode = getPathNodeByAppId(appId, appIdScopeMap);
                PathInfoDTO templateNode = new PathInfoDTO();
                templateNode.setType(ResourceTypeId.TEMPLATE);
                templateNode.setId(templateInfoDTO.getId().toString());
                rootNode.setChild(templateNode);
                path.add(rootNode);
                // 实例组装
                InstanceInfoDTO instanceInfo = new InstanceInfoDTO();
                instanceInfo.setId(instanceId);
                instanceInfo.setDisplayName(templateInfoDTO.getName());
                instanceInfo.setPath(path);
                instanceAttributeInfoList.add(instanceInfo);
            } catch (NumberFormatException e) {
                log.error("Parse object id failed!|{}", instanceId, e);
            }
        }

        FetchInstanceInfoResponseDTO fetchInstanceInfoResponse = new FetchInstanceInfoResponseDTO();
        fetchInstanceInfoResponse.setCode(0L);
        fetchInstanceInfoResponse.setData(instanceAttributeInfoList);
        return fetchInstanceInfoResponse;
    }

    @Override
    public CallbackBaseResponseDTO callback(CallbackRequestDTO callbackRequest) {
        return baseCallback(callbackRequest);
    }
}
