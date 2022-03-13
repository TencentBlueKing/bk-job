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

import com.tencent.bk.job.backup.client.ServiceTemplateResourceClient;
import com.tencent.bk.job.backup.client.WebTemplateResourceClient;
import com.tencent.bk.job.backup.service.TaskTemplateService;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.manage.model.inner.ServiceIdNameCheckDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskVariableDTO;
import com.tencent.bk.job.manage.model.web.request.TaskTemplateCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.task.TaskTemplateVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 29/7/2020 17:46
 */
@Slf4j
@Service
public class TaskTemplateServiceImpl implements TaskTemplateService {
    private final WebTemplateResourceClient webTemplateResourceClient;
    private final ServiceTemplateResourceClient serviceTemplateResourceClient;
    private final AppScopeMappingService appScopeMappingService;

    @Autowired
    public TaskTemplateServiceImpl(WebTemplateResourceClient webTemplateResourceClient,
                                   ServiceTemplateResourceClient serviceTemplateResourceClient,
                                   AppScopeMappingService appScopeMappingService) {
        this.webTemplateResourceClient = webTemplateResourceClient;
        this.serviceTemplateResourceClient = serviceTemplateResourceClient;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    public TaskTemplateVO getTemplateById(String username, Long appId, Long id) {
        try {
            ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(appId);
            Response<TaskTemplateVO> templateByIdResponse =
                webTemplateResourceClient.getTemplateById(username, null, resourceScope.getType().getValue(),
                    resourceScope.getId(), id);
            if (templateByIdResponse != null) {
                if (0 == templateByIdResponse.getCode()) {
                    return templateByIdResponse.getData();
                } else {
                    log.error("Get template by id failed!|{}|{}|{}|{}", username, appId, id, templateByIdResponse);
                }
            }
        } catch (Exception e) {
            log.error("Error while getting template info!|{}|{}|{}", username, appId, id, e);
        }
        return null;
    }

    @Override
    public ServiceIdNameCheckDTO checkIdAndName(Long appId, long id, String name) {
        try {
            InternalResponse<ServiceIdNameCheckDTO> idNameCheckResponse =
                serviceTemplateResourceClient.checkIdAndName(appId, id, name);
            if (idNameCheckResponse != null) {
                if (0 == idNameCheckResponse.getCode()) {
                    return idNameCheckResponse.getData();
                } else {
                    log.error("Check id and name failed!|{}|{}|{}|{}", appId, id, name, idNameCheckResponse);
                }
            }
        } catch (Exception e) {
            log.error("Error while check id and name!|{}|{}|{}", appId, id, name, e);
        }
        return null;
    }

    @Override
    public Long saveTemplate(String username, Long appId, TaskTemplateVO taskTemplate) {
        try {
            TaskTemplateCreateUpdateReq templateCreateUpdateReq = new TaskTemplateCreateUpdateReq();
            templateCreateUpdateReq.setId(taskTemplate.getId());
            templateCreateUpdateReq.setName(taskTemplate.getName());
            templateCreateUpdateReq.setDescription(taskTemplate.getDescription());

            if (CollectionUtils.isNotEmpty(taskTemplate.getStepList())) {
                templateCreateUpdateReq.setSteps(taskTemplate.getStepList().stream()
                    .peek(taskStepVO -> taskStepVO.setId(0L)).collect(Collectors.toList()));
            } else {
                log.warn("Template step can not be empty!");
                return null;
            }
            if (CollectionUtils.isNotEmpty(taskTemplate.getVariableList())) {
                templateCreateUpdateReq.setVariables(taskTemplate.getVariableList().stream().peek(taskVariableVO -> {
                    taskVariableVO.setId(0L);
                    taskVariableVO.setDelete(0);
                }).collect(Collectors.toList()));
            }
            templateCreateUpdateReq.setTags(taskTemplate.getTags());

            InternalResponse<Long> saveTemplateResult = serviceTemplateResourceClient.saveTemplateForMigration(
                username,
                appId,
                taskTemplate.getId(),
                null,
                null,
                null,
                1,
                templateCreateUpdateReq
            );

            if (saveTemplateResult != null) {
                if (0 == saveTemplateResult.getCode()) {
                    return saveTemplateResult.getData();
                } else {
                    log.error("Save template failed!|{}|{}|{}|{}", username, appId, templateCreateUpdateReq,
                        saveTemplateResult);
                }
            }
        } catch (Exception e) {
            log.error("Error while trying to save template!|{}|{}|{}", username, appId, taskTemplate, e);
        }
        return null;
    }

    @Override
    public List<ServiceTaskVariableDTO> getTemplateVariable(String username, Long appId, Long templateId) {
        try {
            InternalResponse<List<ServiceTaskVariableDTO>> templateVariableResponse =
                serviceTemplateResourceClient.getTemplateVariable(username, appId, templateId);
            if (templateVariableResponse != null) {
                if (0 == templateVariableResponse.getCode()) {
                    return templateVariableResponse.getData();
                } else {
                    log.error("Get template variable failed!|{}|{}|{}|{}", username, appId, templateId,
                        templateVariableResponse);
                }
            }
        } catch (Exception e) {
            log.error("Error while getting template variable info!|{}|{}|{}", username, appId, templateId);
        }
        return Collections.emptyList();
    }
}
