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

package com.tencent.bk.job.manage.api.inner.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.User;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.mysql.JobTransactional;
import com.tencent.bk.job.common.paas.user.UserLocalCache;
import com.tencent.bk.job.common.tenant.TenantService;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.api.common.constants.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.api.inner.ServiceTaskTemplateResource;
import com.tencent.bk.job.manage.auth.TemplateAuthService;
import com.tencent.bk.job.manage.migration.AddHostIdForTemplateAndPlanMigrationTask;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskFileInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import com.tencent.bk.job.manage.model.inner.ServiceIdNameCheckDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskTemplateDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskVariableDTO;
import com.tencent.bk.job.manage.model.query.TaskTemplateQuery;
import com.tencent.bk.job.manage.model.web.request.TaskTemplateCreateUpdateReq;
import com.tencent.bk.job.manage.service.AbstractTaskVariableService;
import com.tencent.bk.job.manage.service.TagService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import com.tencent.bk.job.manage.service.template.impl.TemplateScriptStatusUpdateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Slf4j
@RestController
public class ServiceTaskTemplateResourceImpl implements ServiceTaskTemplateResource {

    private final TaskTemplateService templateService;
    private final AbstractTaskVariableService taskVariableService;
    private final TemplateAuthService templateAuthService;
    private final TagService tagService;
    private final AddHostIdForTemplateAndPlanMigrationTask addHostIdService;

    private final TemplateScriptStatusUpdateService templateScriptStatusUpdateService;

    private final TenantService tenantService;

    private final UserLocalCache userLocalCache;

    @Autowired
    public ServiceTaskTemplateResourceImpl(
            TaskTemplateService templateService,
            @Qualifier("TaskTemplateVariableServiceImpl") AbstractTaskVariableService taskVariableService,
            TemplateAuthService templateAuthService,
            TagService tagService,
            AddHostIdForTemplateAndPlanMigrationTask addHostIdService,
            TemplateScriptStatusUpdateService templateScriptStatusUpdateService,
            UserLocalCache userLocalCache,
            TenantService tenantService) {
        this.templateService = templateService;
        this.taskVariableService = taskVariableService;
        this.templateAuthService = templateAuthService;
        this.tagService = tagService;
        this.addHostIdService = addHostIdService;
        this.templateScriptStatusUpdateService = templateScriptStatusUpdateService;
        this.userLocalCache = userLocalCache;
        this.tenantService = tenantService;
    }

    @Override
    public InternalResponse<Boolean> sendScriptUpdateMessage(
        Long appId,
        String scriptId,
        Long scriptVersionId,
        Integer status
    ) {
        templateScriptStatusUpdateService.refreshTemplateScriptStatusByScript(scriptId, scriptVersionId);
        return InternalResponse.buildSuccessResp(null);
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
    @JobTransactional(transactionManager = "jobManageTransactionManager")
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
        User user = userLocalCache.getUser(tenantService.getTenantIdByAppId(appId), username);
        JobContextUtil.setUser(user);
        JobContextUtil.setAllowMigration(true);
        if (templateId > 0) {
            taskTemplateCreateUpdateReq.setId(templateId);
            if (requestSource != null && requestSource == JobConstants.REQUEST_SOURCE_JOB_BACKUP) {
                AuthResult authResult =
                    templateAuthService.authEditJobTemplate(user, new AppResourceScope(appId), templateId);
                if (!authResult.isPass()) {
                    throw new PermissionDeniedException(authResult);
                }
            } else {
                log.warn("Skip update perm check for migration!");
            }
        } else {
            if (requestSource != null && requestSource == JobConstants.REQUEST_SOURCE_JOB_BACKUP) {
                AuthResult authResult =
                    templateAuthService.authCreateJobTemplate(user, new AppResourceScope(appId));
                if (!authResult.isPass()) {
                    throw new PermissionDeniedException(authResult);
                }
            } else {
                log.warn("Skip create perm check for migration!");
            }
        }
        taskTemplateCreateUpdateReq.validate();

        TaskTemplateInfoDTO templateInfo = TaskTemplateInfoDTO.fromReq(username, appId,
            taskTemplateCreateUpdateReq);
        int count = addHostIdForHostsInStepAndVariables(templateInfo);
        log.info("{} hostIds added for template {}:{}", count, templateInfo.getAppId(), templateInfo.getName());
        bindExistTagsToImportedTemplate(appId, templateInfo);
        templateInfo.setCreator(username);
        Long finalTemplateId = templateService.saveTaskTemplateForMigration(templateInfo, createTime,
            lastModifyTime, lastModifyUser);
        templateAuthService.registerTemplate(
            user, finalTemplateId, taskTemplateCreateUpdateReq.getName());
        return InternalResponse.buildSuccessResp(finalTemplateId);
    }

    private int addHostIdForHostsInStepAndVariables(TaskTemplateInfoDTO templateInfo) {
        List<TaskTargetDTO> targetList = new ArrayList<>();
        // 步骤：收集主机信息
        List<TaskStepDTO> stepList = templateInfo.getStepList();
        for (TaskStepDTO step : stepList) {
            TaskStepTypeEnum type = step.getType();
            if (type == TaskStepTypeEnum.SCRIPT) {
                TaskTargetDTO executeTarget = step.getScriptStepInfo().getExecuteTarget();
                targetList.add(executeTarget);
            } else if (type == TaskStepTypeEnum.FILE) {
                TaskTargetDTO destinationHostList = step.getFileStepInfo().getDestinationHostList();
                targetList.add(destinationHostList);
                List<TaskFileInfoDTO> originFileList = step.getFileStepInfo().getOriginFileList();
                for (TaskFileInfoDTO originFile : originFileList) {
                    TaskTargetDTO host = originFile.getHost();
                    targetList.add(host);
                }
            }
        }
        // 变量：收集主机信息
        List<TaskVariableDTO> variableList = templateInfo.getVariableList();
        if (variableList == null) {
            variableList = new ArrayList<>();
        }
        Map<Long, TaskTargetDTO> varTargetMap = new HashMap<>();
        for (TaskVariableDTO taskVariableDTO : variableList) {
            if (taskVariableDTO.getType() == TaskVariableTypeEnum.HOST_LIST) {
                TaskTargetDTO target = TaskTargetDTO.fromJsonString(taskVariableDTO.getDefaultValue());
                if (target != null) {
                    targetList.add(target);
                    varTargetMap.put(taskVariableDTO.getId(), target);
                }
            }
        }
        return addHostIdForTargets(stepList, variableList, targetList, varTargetMap);
    }

    private int addHostIdForTargets(List<TaskStepDTO> stepList,
                                    List<TaskVariableDTO> variableList,
                                    List<TaskTargetDTO> targetList,
                                    Map<Long, TaskTargetDTO> varTargetMap) {
        int count = 0;
        // 批量查询更新
        addHostIdService.addIpAndHostIdMappings(targetList);
        for (TaskStepDTO step : stepList) {
            TaskStepTypeEnum type = step.getType();
            if (type == TaskStepTypeEnum.SCRIPT) {
                TaskTargetDTO executeTarget = step.getScriptStepInfo().getExecuteTarget();
                if (addHostIdService.fillHostId(executeTarget)) {
                    count += 1;
                }
            } else if (type == TaskStepTypeEnum.FILE) {
                TaskTargetDTO destinationHostList = step.getFileStepInfo().getDestinationHostList();
                if (addHostIdService.fillHostId(destinationHostList)) {
                    count += 1;
                }
                List<TaskFileInfoDTO> originFileList = step.getFileStepInfo().getOriginFileList();
                for (TaskFileInfoDTO originFile : originFileList) {
                    TaskTargetDTO host = originFile.getHost();
                    if (addHostIdService.fillHostId(host)) {
                        count += 1;
                    }
                }
            }
        }
        for (TaskVariableDTO taskVariableDTO : variableList) {
            if (taskVariableDTO.getType() == TaskVariableTypeEnum.HOST_LIST) {
                TaskTargetDTO target = varTargetMap.get(taskVariableDTO.getId());
                if (target != null) {
                    if (addHostIdService.fillHostId(target)) {
                        taskVariableDTO.setDefaultValue(JsonUtils.toJson(target));
                        count += 1;
                    }
                }
            }
        }
        return count;
    }

    /*
     * 为导入的模板绑定已有的同名标签
     */
    private void bindExistTagsToImportedTemplate(long appId, TaskTemplateInfoDTO templateInfo) {
        if (CollectionUtils.isNotEmpty(templateInfo.getTags())) {
            Set<String> tagNames = templateInfo.getTags().stream().map(TagDTO::getName).collect(Collectors.toSet());
            List<TagDTO> existTags = tagService.listTagsByAppId(appId);
            if (CollectionUtils.isEmpty(existTags)) {
                templateInfo.setTags(null);
                return;
            }
            templateInfo.setTags(existTags.stream().filter(tag -> tagNames.contains(tag.getName()))
                .collect(Collectors.toList()));
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
                taskVariableList.stream().map(TaskVariableDTO::toServiceDTO).collect(Collectors.toList());
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
        resultData.setData(templateListPage.getData().stream().map(TaskTemplateInfoDTO::toServiceDTO)
            .collect(Collectors.toList()));
        return InternalResponse.buildSuccessResp(resultData);
    }
}
