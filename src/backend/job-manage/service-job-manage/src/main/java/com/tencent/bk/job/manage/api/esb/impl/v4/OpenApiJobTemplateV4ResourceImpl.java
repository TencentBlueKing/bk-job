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

package com.tencent.bk.job.manage.api.esb.impl.v4;

import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.audit.annotations.AuditRequestBody;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.json.SecondToMillisUtil;
import com.tencent.bk.job.file_gateway.api.inner.ServiceFileSourceResource;
import com.tencent.bk.job.file_gateway.model.resp.inner.ServiceFileSourceBasicInfoDTO;
import com.tencent.bk.job.manage.api.common.ExecuteAccountVariableValidator;
import com.tencent.bk.job.manage.api.esb.v4.OpenApiJobTemplateV4Resource;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4CreateJobTemplateRequest;
import com.tencent.bk.job.manage.model.esb.v4.req.V4UpdateJobTemplateRequest;
import com.tencent.bk.job.manage.model.esb.v4.resp.OpenApiV4JobTemplateDetailDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.OpenApiV4JobTemplateWriteResultDTO;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class OpenApiJobTemplateV4ResourceImpl implements OpenApiJobTemplateV4Resource {

    private final TaskTemplateService templateService;
    private final AppScopeMappingService appScopeMappingService;
    private final ServiceFileSourceResource fileSourceResource;
    private final OpenApiV4JobTemplateWriteConverter writeConverter;
    private final ExecuteAccountVariableValidator executeAccountVariableValidator;

    @Autowired
    public OpenApiJobTemplateV4ResourceImpl(TaskTemplateService templateService,
                                            AppScopeMappingService appScopeMappingService,
                                            ServiceFileSourceResource fileSourceResource,
                                            OpenApiV4JobTemplateWriteConverter writeConverter,
                                            ExecuteAccountVariableValidator executeAccountVariableValidator) {
        this.templateService = templateService;
        this.appScopeMappingService = appScopeMappingService;
        this.fileSourceResource = fileSourceResource;
        this.writeConverter = writeConverter;
        this.executeAccountVariableValidator = executeAccountVariableValidator;
    }

    @Override
    @AuditEntry(actionId = ActionId.VIEW_JOB_TEMPLATE)
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v4_get_job_template_detail"})
    public EsbV4Response<OpenApiV4JobTemplateDetailDTO> getJobTemplateDetail(String username,
                                                                             String appCode,
                                                                             String scopeType,
                                                                             String scopeId,
                                                                             Long id) {
        Long appId = appScopeMappingService.getAppIdByScope(scopeType, scopeId);
        User user = JobContextUtil.getUser();
        TaskTemplateInfoDTO templateInfo = templateService.getTaskTemplate(user, appId, id);
        OpenApiV4JobTemplateDetailDTO data = OpenApiV4JobTemplateConverter.toDetailDTO(
            templateInfo, appScopeMappingService, queryFileSourceCodeMap(user.getTenantId(), templateInfo)
        );
        return EsbV4Response.success(data);
    }

    @Override
    @AuditEntry(actionId = ActionId.CREATE_JOB_TEMPLATE)
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v4_create_job_template"})
    public EsbV4Response<OpenApiV4JobTemplateWriteResultDTO> createJobTemplate(
        String username,
        String appCode,
        @AuditRequestBody V4CreateJobTemplateRequest request) {

        request.fillAppResourceScope(appScopeMappingService);
        Long appId = request.getAppId();
        User user = JobContextUtil.getUser();

        TaskTemplateInfoDTO templateInfo = writeConverter.toCreateTemplateInfo(username, appId, request);
        executeAccountVariableValidator.validate(appId, templateInfo.getStepList(), templateInfo.getVariableList());

        // 鉴权、名称查重、脚本与文件源引用校验、IAM 实例注册都在服务层完成
        TaskTemplateInfoDTO createdTemplate = templateService.saveTaskTemplate(user, templateInfo);
        return EsbV4Response.success(toWriteResult(appId, createdTemplate));
    }

    @Override
    @AuditEntry(actionId = ActionId.EDIT_JOB_TEMPLATE)
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v4_update_job_template"})
    public EsbV4Response<OpenApiV4JobTemplateWriteResultDTO> updateJobTemplate(
        String username,
        String appCode,
        @AuditRequestBody V4UpdateJobTemplateRequest request) {

        request.fillAppResourceScope(appScopeMappingService);
        Long appId = request.getAppId();
        User user = JobContextUtil.getUser();

        TaskTemplateInfoDTO existingTemplate = templateService.getTaskTemplateById(appId, request.getId());
        if (existingTemplate == null) {
            throw new NotFoundException(ErrorCode.TEMPLATE_NOT_EXIST);
        }

        TaskTemplateInfoDTO templateInfo =
            writeConverter.toUpdateTemplateInfo(username, appId, request, existingTemplate);
        executeAccountVariableValidator.validate(appId, templateInfo.getStepList(), templateInfo.getVariableList());

        TaskTemplateInfoDTO updatedTemplate = templateService.updateTaskTemplate(user, templateInfo);
        return EsbV4Response.success(toWriteResult(appId, updatedTemplate));
    }

    private OpenApiV4JobTemplateWriteResultDTO toWriteResult(Long appId, TaskTemplateInfoDTO templateInfo) {
        OpenApiV4JobTemplateWriteResultDTO result = new OpenApiV4JobTemplateWriteResultDTO();
        ResourceScope scope = appScopeMappingService.getScopeByAppId(appId);
        if (scope != null) {
            result.setScopeType(scope.getType().getValue());
            result.setScopeId(scope.getId());
        }
        result.setJobTemplateId(templateInfo.getId());
        result.setName(templateInfo.getName());
        result.setCreator(templateInfo.getCreator());
        result.setCreateTime(SecondToMillisUtil.toMillis(templateInfo.getCreateTime()));
        result.setLastModifyTime(SecondToMillisUtil.toMillis(templateInfo.getLastModifyTime()));
        return result;
    }

    /**
     * 批量反查模板引用到的文件源 code。文件源已被删除时该 ID 不会出现在结果中，展示层按 null 处理。
     */
    private Map<Integer, String> queryFileSourceCodeMap(String tenantId, TaskTemplateInfoDTO templateInfo) {
        Set<Integer> fileSourceIds = OpenApiV4JobTemplateConverter.extractFileSourceIds(templateInfo);
        if (CollectionUtils.isEmpty(fileSourceIds)) {
            return Collections.emptyMap();
        }
        List<ServiceFileSourceBasicInfoDTO> basicInfoList = fileSourceResource
            .listFileSourceBasicInfoByIds(tenantId, new ArrayList<>(fileSourceIds))
            .getData();
        if (CollectionUtils.isEmpty(basicInfoList)) {
            return Collections.emptyMap();
        }
        return basicInfoList.stream()
            .filter(basicInfo -> basicInfo.getCode() != null)
            .collect(Collectors.toMap(ServiceFileSourceBasicInfoDTO::getId, ServiceFileSourceBasicInfoDTO::getCode));
    }
}
