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
import com.tencent.bk.job.manage.api.iam.IamTaskPlanCallbackResource;
import com.tencent.bk.job.manage.model.dto.TaskPlanQueryDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanBasicInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.service.ApplicationService;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import com.tencent.bk.sdk.iam.dto.PathInfoDTO;
import com.tencent.bk.sdk.iam.dto.callback.request.CallbackRequestDTO;
import com.tencent.bk.sdk.iam.dto.callback.request.IamSearchCondition;
import com.tencent.bk.sdk.iam.dto.callback.response.CallbackBaseResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.FetchInstanceInfoResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.InstanceInfoDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.ListInstanceResponseDTO;
import com.tencent.bk.sdk.iam.dto.callback.response.SearchInstanceResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.helpers.MessageFormatter;
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
public class IamTaskPlanCallbackResourceImpl extends BaseIamCallbackService
    implements IamTaskPlanCallbackResource {

    private final TaskPlanService planService;
    private final ApplicationService applicationService;

    @Autowired
    public IamTaskPlanCallbackResourceImpl(TaskPlanService planService,
                                           ApplicationService applicationService) {
        this.planService = planService;
        this.applicationService = applicationService;
    }

    private InstanceInfoDTO convert(TaskPlanInfoDTO planInfoDTO) {
        InstanceInfoDTO instanceInfo = new InstanceInfoDTO();
        instanceInfo.setId(String.valueOf(planInfoDTO.getId()));
        instanceInfo.setDisplayName(planInfoDTO.getName());
        return instanceInfo;
    }

    private Pair<TaskPlanQueryDTO, BaseSearchCondition> getBasicQueryCondition(CallbackRequestDTO callbackRequest) {
        IamSearchCondition searchCondition = IamSearchCondition.fromReq(callbackRequest);
        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(searchCondition.getStart().intValue());
        baseSearchCondition.setLength(searchCondition.getLength().intValue());

        TaskPlanQueryDTO planQuery = new TaskPlanQueryDTO();
        planQuery.setTemplateId(Long.parseLong(callbackRequest.getFilter().getParent().getId()));
        return Pair.of(planQuery, baseSearchCondition);
    }

    @Override
    protected ListInstanceResponseDTO listInstanceResp(CallbackRequestDTO callbackRequest) {
        Pair<TaskPlanQueryDTO, BaseSearchCondition> basicQueryCond =
            getBasicQueryCondition(callbackRequest);

        TaskPlanQueryDTO planQuery = basicQueryCond.getLeft();
        BaseSearchCondition baseSearchCondition = basicQueryCond.getRight();
        PageData<TaskPlanInfoDTO> planDTOPageData = planService.listPageTaskPlansBasicInfo(planQuery,
            baseSearchCondition, null);

        return IamRespUtil.getListInstanceRespFromPageData(planDTOPageData, this::convert);
    }

    @Override
    protected SearchInstanceResponseDTO searchInstanceResp(CallbackRequestDTO callbackRequest) {
        Pair<TaskPlanQueryDTO, BaseSearchCondition> basicQueryCond =
            getBasicQueryCondition(callbackRequest);

        TaskPlanQueryDTO planQuery = basicQueryCond.getLeft();
        BaseSearchCondition baseSearchCondition = basicQueryCond.getRight();

        planQuery.setName(callbackRequest.getFilter().getKeyword());
        PageData<TaskPlanInfoDTO> planDTOPageData = planService.listPageTaskPlansBasicInfo(planQuery,
            baseSearchCondition, null);

        return IamRespUtil.getSearchInstanceRespFromPageData(planDTOPageData, this::convert);
    }

    private InstanceInfoDTO buildInstance(TaskPlanBasicInfoDTO planBasicInfoDTO,
                                          Map<Long, ResourceScope> appIdScopeMap) {
        Long appId = planBasicInfoDTO.getAppId();
        // 拓扑路径构建
        List<PathInfoDTO> path = new ArrayList<>();
        PathInfoDTO rootNode = getPathNodeByAppId(appId, appIdScopeMap);
        PathInfoDTO templateNode = new PathInfoDTO();
        templateNode.setType(ResourceTypeId.TEMPLATE);
        templateNode.setId(planBasicInfoDTO.getTemplateId().toString());
        rootNode.setChild(templateNode);
        PathInfoDTO planNode = new PathInfoDTO();
        planNode.setType(ResourceTypeId.PLAN);
        planNode.setId(planBasicInfoDTO.getId().toString());
        templateNode.setChild(planNode);
        path.add(rootNode);
        // 实例组装
        InstanceInfoDTO instanceInfo = new InstanceInfoDTO();
        instanceInfo.setId(planBasicInfoDTO.getId().toString());
        instanceInfo.setDisplayName(planBasicInfoDTO.getName());
        instanceInfo.setPath(path);
        return instanceInfo;
    }

    @Override
    protected CallbackBaseResponseDTO fetchInstanceResp(
        CallbackRequestDTO callbackRequest
    ) {
        IamSearchCondition searchCondition = IamSearchCondition.fromReq(callbackRequest);
        List<Object> instanceAttributeInfoList = new ArrayList<>();
        List<Long> planIdList = new ArrayList<>();
        for (String instanceId : searchCondition.getIdList()) {
            try {
                long id = Long.parseLong(instanceId);
                planIdList.add(id);
            } catch (NumberFormatException e) {
                String msg = MessageFormatter.format(
                    "Parse plan id failed!|{}",
                    instanceId
                ).getMessage();
                log.error(msg, e);
            }
        }
        List<TaskPlanBasicInfoDTO> planBasicInfoDTOList = planService.listTaskPlanByIds(planIdList);
        Map<Long, TaskPlanBasicInfoDTO> planBasicInfoDTOMap = new HashMap<>(planBasicInfoDTOList.size());
        Set<Long> appIdSet = new HashSet<>();
        for (TaskPlanBasicInfoDTO taskPlanBasicInfoDTO : planBasicInfoDTOList) {
            planBasicInfoDTOMap.put(taskPlanBasicInfoDTO.getId(), taskPlanBasicInfoDTO);
            appIdSet.add(taskPlanBasicInfoDTO.getAppId());
        }
        // Job app --> CMDB biz/businessSet转换
        Map<Long, ResourceScope> appIdScopeMap = applicationService.getScopeByAppIds(appIdSet);
        for (String instanceId : searchCondition.getIdList()) {
            long id = Long.parseLong(instanceId);
            TaskPlanBasicInfoDTO planBasicInfoDTO = planBasicInfoDTOMap.get(id);
            if (planBasicInfoDTO == null) {
                logNotExistId(id);
                continue;
            }
            try {
                InstanceInfoDTO instanceInfo = buildInstance(planBasicInfoDTO, appIdScopeMap);
                instanceAttributeInfoList.add(instanceInfo);
            } catch (Exception e) {
                logBuildInstanceFailure(planBasicInfoDTO, e);
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
