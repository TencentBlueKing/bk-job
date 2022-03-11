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

package com.tencent.bk.job.backup.service.impl;

import com.tencent.bk.job.backup.client.ServicePlanResourceClient;
import com.tencent.bk.job.backup.client.WebPlanResourceClient;
import com.tencent.bk.job.backup.service.TaskPlanService;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.manage.model.inner.ServiceIdNameCheckDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskVariableDTO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskPlanVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 29/7/2020 17:46
 */
@Slf4j
@Service
public class TaskPlanServiceImpl implements TaskPlanService {
    private final WebPlanResourceClient webPlanResourceClient;
    private final ServicePlanResourceClient servicePlanResourceClient;
    private final AppScopeMappingService appScopeMappingService;

    @Autowired
    public TaskPlanServiceImpl(WebPlanResourceClient webPlanResourceClient,
                               ServicePlanResourceClient servicePlanResourceClient,
                               AppScopeMappingService appScopeMappingService) {
        this.webPlanResourceClient = webPlanResourceClient;
        this.servicePlanResourceClient = servicePlanResourceClient;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    public List<TaskPlanVO> getTaskPlanByIdList(String username, Long appId, Long templateId, List<Long> planIdList) {
        List<TaskPlanVO> taskPlanList = new ArrayList<>();
        try {
            if (CollectionUtils.isEmpty(planIdList)) {
                return taskPlanList;
            }
            for (Long planId : planIdList) {
                if (planId <= 0) {
                    log.warn("Invalid plan id {}!", planId);
                    continue;
                }
                log.debug("Fetching plan {}/{}/{} using {}", appId, templateId, planId, username);
                ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(appId);
                Response<TaskPlanVO> planByIdResponse =
                    webPlanResourceClient.getPlanById(username, appId, resourceScope.getType().getValue(),
                        resourceScope.getId(), templateId, planId);
                if (planByIdResponse != null) {
                    if (0 == planByIdResponse.getCode()) {
                        taskPlanList.add(planByIdResponse.getData());
                        log.debug("Fetching plan {}/{}/{} finished.", appId, templateId, planId);
                    } else {
                        log.warn("Fetching plan failed! {}/{}/{}|{}|{}", appId, templateId, planId,
                            planByIdResponse.getCode(), planByIdResponse.getErrorMsg());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error while getting plan info!|{}|{}|{}|{}", username, appId, templateId, planIdList, e);
        }

        return taskPlanList;
    }

    @Override
    public List<TaskPlanVO> listPlans(String username, Long appId, Long templateId) {
        try {
            ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(appId);
            Response<List<TaskPlanVO>> planListResponse =
                webPlanResourceClient.listPlans(username, appId, resourceScope.getType().getValue(),
                    resourceScope.getId(), templateId);
            if (planListResponse != null) {
                if (0 == planListResponse.getCode()) {
                    log.debug("Fetching plan list of {}/{} finished.", appId, templateId);
                    return planListResponse.getData();
                } else {
                    log.warn("Fetching plan list failed! {}/{}|{}|{}", appId, templateId, planListResponse.getCode(),
                        planListResponse.getErrorMsg());
                }
            }
        } catch (Exception e) {
            log.error("Error while list plan info by template id!|{}|{}|{}", username, appId, templateId, e);
        }
        return null;
    }

    @Override
    public ServiceIdNameCheckDTO checkIdAndName(Long appId, Long templateId, Long planId, String name) {
        try {
            InternalResponse<ServiceIdNameCheckDTO> idNameCheckResponse =
                servicePlanResourceClient.checkIdAndName(appId, templateId, planId, name);
            if (idNameCheckResponse != null) {
                if (0 == idNameCheckResponse.getCode()) {
                    return idNameCheckResponse.getData();
                } else {
                    log.error("Check id and name failed!|{}|{}|{}|{}|{}", appId, templateId, planId, name,
                        idNameCheckResponse);
                }
            }
        } catch (Exception e) {
            log.error("Error while chech id and name!|{}|{}|{}|{}", appId, templateId, planId, name);
        }
        return null;
    }

    @Override
    public Long savePlan(String username, Long appId, Long templateId, TaskPlanVO planInfo) {
        try {
            planInfo.setTemplateId(templateId);
            if (CollectionUtils.isNotEmpty(planInfo.getStepList())) {
                planInfo.setStepList(planInfo.getStepList().stream().peek(taskStepVO -> taskStepVO.setId(0L))
                    .collect(Collectors.toList()));
            } else {
                log.warn("Plan step can not be empty!");
                return null;
            }
            planInfo.setVariableList(planInfo.getVariableList());

            InternalResponse<Long> savePlanResult =
                servicePlanResourceClient.savePlanForImport(username, appId, templateId, null, planInfo);

            if (savePlanResult != null) {
                if (0 == savePlanResult.getCode()) {
                    return savePlanResult.getData();
                } else {
                    log.error("Save plan failed!|{}|{}|{}|{}|{}", username, appId, templateId, planInfo,
                        savePlanResult);
                }
            }
        } catch (Exception e) {
            log.error("Error while save plan!|{}|{}|{}|{}", username, appId, templateId, planInfo, e);
        }
        return null;

    }

    @Override
    public List<ServiceTaskVariableDTO> getPlanVariable(String username, Long appId, Long templateId, Long planId) {
        try {
            InternalResponse<List<ServiceTaskVariableDTO>> planVariableResponse =
                servicePlanResourceClient.getPlanVariable(username, appId, templateId, planId);
            if (planVariableResponse != null) {
                if (0 == planVariableResponse.getCode()) {
                    return planVariableResponse.getData();
                } else {
                    log.error("Get variable info failed!|{}|{}|{}|{}|{}", username, appId, templateId, planId,
                        planVariableResponse);
                }
            }
        } catch (Exception e) {
            log.error("Error while getting plan variable info!|{}|{}|{}|{}", username, appId, templateId, planId);
        }
        return Collections.emptyList();
    }
}
