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

package com.tencent.bk.job.manage.model.dto.task;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.util.EsbDTOAppScopeMappingHelper;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.manage.common.consts.task.TaskTemplateStatusEnum;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbTemplateInfoV3DTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskTemplateDTO;
import com.tencent.bk.job.manage.model.web.request.TaskTemplateCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.request.TemplateBasicInfoUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.task.TaskTemplateVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 27/9/2019 12:32
 */
@Slf4j
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class TaskTemplateInfoDTO {

    /**
     * 模版 ID
     */
    private Long id;

    /**
     * 业务 ID
     */
    private Long appId;

    /**
     * 模版名称
     */
    private String name;

    /**
     * 模版描述
     */
    private String description;

    /**
     * 模版创建人
     */
    private String creator;

    /**
     * 模版状态
     */
    private TaskTemplateStatusEnum status;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 更新用户
     */
    private String lastModifyUser;

    /**
     * 更新时间
     */
    private Long lastModifyTime;

    /**
     * 模版标签 ID 列表
     */
    private List<TagDTO> tags;

    /**
     * 首个步骤 ID
     */
    private Long firstStepId;

    /**
     * 末尾步骤 ID
     */
    private Long lastStepId;

    /**
     * 是否需要更新
     */
    private Integer scriptStatus;

    /**
     * 模版包含的步骤列表
     */
    private List<TaskStepDTO> stepList;

    /**
     * 模版包含的变量列表
     */
    private List<TaskVariableDTO> variableList;

    /**
     * 模版版本
     */
    private String version;

    public static TaskTemplateVO toVO(TaskTemplateInfoDTO templateInfo) {
        TaskTemplateVO templateVO = new TaskTemplateVO();
        templateVO.setId(templateInfo.getId());
        templateVO.setName(templateInfo.getName());
        if (CollectionUtils.isNotEmpty(templateInfo.getTags())) {
            templateVO.setTags(templateInfo.getTags().stream().map(TagDTO::toVO).collect(Collectors.toList()));
        }
        templateVO.setStatus(templateInfo.getStatus().getStatus());
        templateVO.setCreator(templateInfo.getCreator());
        templateVO.setCreateTime(templateInfo.getCreateTime());
        templateVO.setLastModifyUser(templateInfo.getLastModifyUser());
        templateVO.setLastModifyTime(templateInfo.getLastModifyTime());
        templateVO.setScriptStatus(templateInfo.getScriptStatus());
        templateVO.setDescription(templateInfo.getDescription());
        templateVO.setVersion(templateInfo.getVersion());

        if (templateInfo.getVariableList() != null) {
            templateVO.setVariableList(
                templateInfo.getVariableList().stream().map(TaskVariableDTO::toVO).collect(Collectors.toList()));
        }

        if (templateInfo.getStepList() != null) {
            templateVO
                .setStepList(templateInfo.getStepList().stream().map(TaskStepDTO::toVO).collect(Collectors.toList()));
        }

        return templateVO;
    }

    public static TaskTemplateInfoDTO fromReq(String username, Long appId,
                                              TaskTemplateCreateUpdateReq templateCreateUpdateReq) {

        TaskTemplateInfoDTO templateInfo = fromBasicReq(username, appId, templateCreateUpdateReq);
        if (CollectionUtils.isNotEmpty(templateCreateUpdateReq.getSteps())) {
            templateInfo.setStepList(
                templateCreateUpdateReq.getSteps().stream().map(TaskStepDTO::fromVO).collect(Collectors.toList()));
        } else {
            templateInfo.setStepList(Collections.emptyList());
        }
        if (CollectionUtils.isNotEmpty(templateCreateUpdateReq.getVariables())) {
            templateInfo.setVariableList(templateCreateUpdateReq.getVariables().stream().map(TaskVariableDTO::fromVO)
                .collect(Collectors.toList()));
        } else {
            templateInfo.setVariableList(Collections.emptyList());
        }

        return templateInfo;
    }

    public static TaskTemplateInfoDTO fromBasicReq(String username, Long appId,
                                                   TemplateBasicInfoUpdateReq templateBasicInfoUpdateReq) {
        if (appId == null || appId <= 0) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }

        TaskTemplateInfoDTO templateInfo = new TaskTemplateInfoDTO();
        templateInfo.setAppId(appId);
        templateInfo.setLastModifyUser(username);
        templateInfo.setLastModifyTime(DateUtils.currentTimeSeconds());

        if (templateBasicInfoUpdateReq.getId() == null || templateBasicInfoUpdateReq.getId() <= 0) {
            templateInfo.setCreator(username);
            templateInfo.setStatus(TaskTemplateStatusEnum.NEW);
        } else {
            templateInfo.setId(templateBasicInfoUpdateReq.getId());
        }
        templateInfo.setName(templateBasicInfoUpdateReq.getName());
        templateInfo.setDescription(templateBasicInfoUpdateReq.getDescription());
        if (CollectionUtils.isNotEmpty(templateBasicInfoUpdateReq.getTags())) {
            templateInfo.setTags(
                templateBasicInfoUpdateReq.getTags().stream().map(TagDTO::fromVO).collect(Collectors.toList()));
        } else {
            templateInfo.setTags(Collections.emptyList());
        }

        return templateInfo;
    }

    public static ServiceTaskTemplateDTO toServiceDTO(TaskTemplateInfoDTO templateInfo) {
        if (templateInfo == null) {
            return null;
        }
        ServiceTaskTemplateDTO serviceTemplate = new ServiceTaskTemplateDTO();
        serviceTemplate.setId(templateInfo.getId());
        serviceTemplate.setAppId(templateInfo.getAppId());
        serviceTemplate.setName(templateInfo.getName());
        if (CollectionUtils.isNotEmpty(templateInfo.getTags())) {
            serviceTemplate.setTags(templateInfo.getTags().stream()
                .map(TagDTO::toServiceDTO).collect(Collectors.toList()));
        }
        serviceTemplate.setStatus(templateInfo.getStatus().getStatus());
        serviceTemplate.setCreator(templateInfo.getCreator());
        serviceTemplate.setCreateTime(templateInfo.getCreateTime());
        serviceTemplate.setLastModifyUser(templateInfo.getLastModifyUser());
        serviceTemplate.setLastModifyTime(templateInfo.getLastModifyTime());
        serviceTemplate.setScriptStatus(templateInfo.getScriptStatus());
        serviceTemplate.setDescription(templateInfo.getDescription());
        serviceTemplate.setVersion(templateInfo.getVersion());

        if (templateInfo.getVariableList() != null) {
            serviceTemplate.setVariableList(
                templateInfo.getVariableList().stream()
                    .map(TaskVariableDTO::toServiceDTO).collect(Collectors.toList()));
        }

        if (templateInfo.getStepList() != null) {
            serviceTemplate
                .setStepList(templateInfo.getStepList().stream()
                    .map(TaskStepDTO::toServiceDTO).collect(Collectors.toList()));
        }
        return serviceTemplate;
    }

    public static EsbTemplateInfoV3DTO toEsbTemplateInfoV3DTO(TaskTemplateInfoDTO templateInfo) {
        if (templateInfo == null) {
            return null;
        }
        EsbTemplateInfoV3DTO template = new EsbTemplateInfoV3DTO();
        template.setId(templateInfo.getId());
        EsbDTOAppScopeMappingHelper.fillEsbAppScopeDTOByAppId(templateInfo.getAppId(), template);
        template.setName(templateInfo.getName());
        template.setCreator(templateInfo.getCreator());
        template.setCreateTime(templateInfo.getCreateTime());
        template.setLastModifyUser(templateInfo.getLastModifyUser());
        template.setLastModifyTime(templateInfo.getLastModifyTime());
        template.setDescription(templateInfo.getDescription());

        if (CollectionUtils.isNotEmpty(templateInfo.getVariableList())) {
            template.setGlobalVarList(
                templateInfo.getVariableList().stream()
                    .map(TaskVariableDTO::toEsbGlobalVarV3).collect(Collectors.toList()));
        }

        if (CollectionUtils.isNotEmpty(templateInfo.getStepList())) {
            template
                .setStepList(templateInfo.getStepList().stream()
                    .map(TaskStepDTO::toEsbStepV3).collect(Collectors.toList()));
        }
        return template;
    }
}
