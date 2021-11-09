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
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeEnum;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.permission.AuthResultVO;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.manage.api.inner.ServiceTaskTemplateResource;
import com.tencent.bk.job.manage.common.consts.JobResourceStatusEnum;
import com.tencent.bk.job.manage.common.util.IamPathUtil;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import com.tencent.bk.job.manage.model.inner.ServiceIdNameCheckDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskTemplateDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskVariableDTO;
import com.tencent.bk.job.manage.model.query.TaskTemplateQuery;
import com.tencent.bk.job.manage.model.web.request.TaskTemplateCreateUpdateReq;
import com.tencent.bk.job.manage.service.AbstractTaskVariableService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@RestController
public class ServiceTaskTemplateResourceImpl implements ServiceTaskTemplateResource {

    private final TaskTemplateService templateService;
    private final AbstractTaskVariableService taskVariableService;
    private final WebAuthService authService;

    @Autowired
    public ServiceTaskTemplateResourceImpl(
        TaskTemplateService templateService,
        @Qualifier("TaskTemplateVariableServiceImpl") AbstractTaskVariableService taskVariableService,
        WebAuthService authService
    ) {
        this.templateService = templateService;
        this.taskVariableService = taskVariableService;
        this.authService = authService;
    }

    @Override
    public InternalResponse<Boolean> sendScriptUpdateMessage(
        Long appId,
        String scriptId,
        Long scriptVersionId,
        Integer status
    ) {
        return InternalResponse.buildSuccessResp(templateService.updateScriptStatus(appId, scriptId, scriptVersionId,
            JobResourceStatusEnum.getJobResourceStatus(status)));
    }

    @Override
    public InternalResponse<ServiceTaskTemplateDTO> getTemplateById(String username, Long appId, Long templateId) {
        TaskTemplateInfoDTO templateInfo = templateService.getTaskTemplateById(appId, templateId);
        if (templateInfo == null) {
            throw new NotFoundException(ErrorCode.TEMPLATE_NOT_EXIST);
        }
        ServiceTaskTemplateDTO serviceTaskTemplateDTO = TaskTemplateInfoDTO.toServiceDTO(templateInfo);
        return InternalResponse.buildSuccessResp(serviceTaskTemplateDTO);
    }

    @Override
    public InternalResponse<ServiceTaskTemplateDTO> getTemplateById(Long templateId) {
        TaskTemplateInfoDTO templateInfo = templateService.getTemplateById(templateId);
        if (templateInfo == null) {
            throw new NotFoundException(ErrorCode.TEMPLATE_NOT_EXIST);
        }
        ServiceTaskTemplateDTO serviceTaskTemplateDTO = TaskTemplateInfoDTO.toServiceDTO(templateInfo);
        return InternalResponse.buildSuccessResp(serviceTaskTemplateDTO);
    }

    @Override
    public InternalResponse<String> getTemplateNameById(Long templateId) {
        return InternalResponse.buildSuccessResp(templateService.getTemplateName(templateId));
    }

    @Override
    public InternalResponse<Long> saveTemplateForMigration(
        String username,
        Long appId,
        Long templateId,
        Long createTime,
        Long lastModifyTime,
        String lastModifyUser,
        Integer requestSource,
        TaskTemplateCreateUpdateReq taskTemplateCreateUpdateReq
    ) {
        JobContextUtil.setAllowMigration(true);
        AuthResultVO authResultVO;
        if (templateId > 0) {
            taskTemplateCreateUpdateReq.setId(templateId);
            authResultVO = authService.auth(true, username, ActionId.EDIT_JOB_TEMPLATE,
                ResourceTypeEnum.TEMPLATE, templateId.toString(),
                IamPathUtil.buildTemplatePathInfo(appId));
        } else {
            authResultVO = authService.auth(
                true,
                username,
                ActionId.CREATE_JOB_TEMPLATE,
                ResourceTypeEnum.BUSINESS,
                appId.toString(),
                null
            );
        }
        if (!authResultVO.isPass()) {
            if (requestSource != null && requestSource == 1) {
                throw new InternalException(ErrorCode.INTERNAL_ERROR);
            } else {
                log.warn("Skip perm check for migration!");
            }
        }
        if (taskTemplateCreateUpdateReq.validate()) {
            TaskTemplateInfoDTO templateInfo = TaskTemplateInfoDTO.fromReq(username, appId,
                taskTemplateCreateUpdateReq);
            if (templateInfo != null) {
                templateInfo.setCreator(username);
                Long finalTemplateId = templateService.saveTaskTemplateForMigration(templateInfo, createTime,
                    lastModifyTime, lastModifyUser);
                authService.registerResource(finalTemplateId.toString(), taskTemplateCreateUpdateReq.getName(),
                    ResourceId.TEMPLATE, username, null);
                return InternalResponse.buildSuccessResp(finalTemplateId);
            } else {
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
            }
        } else {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
    }

    @Override
    public InternalResponse<ServiceIdNameCheckDTO> checkIdAndName(Long appId, Long templateId, String name) {
        boolean idResult = templateService.checkTemplateId(templateId);
        boolean nameResult = templateService.checkTemplateName(appId, 0L, name);

        ServiceIdNameCheckDTO idNameCheck = new ServiceIdNameCheckDTO();
        idNameCheck.setIdCheckResult(idResult ? 1 : 0);
        idNameCheck.setNameCheckResult(nameResult ? 1 : 0);
        return InternalResponse.buildSuccessResp(idNameCheck);
    }

    @Override
    public InternalResponse<List<ServiceTaskVariableDTO>> getTemplateVariable(String username, Long appId,
                                                                         Long templateId) {
        List<TaskVariableDTO> taskVariableList = taskVariableService.listVariablesByParentId(templateId);
        if (CollectionUtils.isNotEmpty(taskVariableList)) {
            List<ServiceTaskVariableDTO> variableList =
                taskVariableList.parallelStream().map(TaskVariableDTO::toServiceDTO).collect(Collectors.toList());
            return InternalResponse.buildSuccessResp(variableList);
        }
        return InternalResponse.buildSuccessResp(Collections.emptyList());
    }

    @Override
    public InternalResponse<PageData<ServiceTaskTemplateDTO>> listPageTaskTemplates(
        Long appId,
        Integer start,
        Integer pageSize) {

        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(start);
        baseSearchCondition.setLength(pageSize);

        TaskTemplateQuery query = TaskTemplateQuery.builder().appId(appId)
            .baseSearchCondition(baseSearchCondition).build();
        PageData<TaskTemplateInfoDTO> templateListPage = templateService.listPageTaskTemplates(query);

        PageData<ServiceTaskTemplateDTO> resultData = new PageData<>();
        resultData.setTotal(templateListPage.getTotal());
        resultData.setStart(templateListPage.getStart());
        resultData.setPageSize(templateListPage.getPageSize());
        resultData.setData(templateListPage.getData().parallelStream().map(TaskTemplateInfoDTO::toServiceDTO)
            .collect(Collectors.toList()));
        return InternalResponse.buildSuccessResp(resultData);
    }
}
