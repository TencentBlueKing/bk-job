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
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.manage.common.consts.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbStepV3DTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskStepDTO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskStepVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 2/10/2019 20:30
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class TaskStepDTO {

    private Long id;

    private Long templateStepId;

    private Long templateId;

    private Long planId;

    private Long instanceId;

    private TaskStepTypeEnum type;

    private String name;

    private Long previousStepId;

    private Long nextStepId;

    private Long scriptStepId;

    private Long fileStepId;

    private Long approvalStepId;

    private TaskScriptStepDTO scriptStepInfo;

    private TaskFileStepDTO fileStepInfo;

    private TaskApprovalStepDTO approvalStepInfo;

    private Integer delete;

    private Integer enable;

    /**
     * 步骤引用的变量
     */
    private List<TaskVariableDTO> refVariables;

    public static TaskStepVO toVO(TaskStepDTO taskStep) {
        if (taskStep == null) {
            return null;
        }
        TaskStepVO stepVO = new TaskStepVO();
        stepVO.setId(taskStep.getId());
        stepVO.setTemplateStepId(taskStep.getTemplateStepId());
        stepVO.setType(taskStep.getType().getType());
        stepVO.setName(taskStep.getName());
        stepVO.setScriptStepInfo(TaskScriptStepDTO.toVO(taskStep.getScriptStepInfo()));
        stepVO.setFileStepInfo(TaskFileStepDTO.toVO(taskStep.getFileStepInfo()));
        stepVO.setApprovalStepInfo(TaskApprovalStepDTO.toVO(taskStep.getApprovalStepInfo()));
        stepVO.setEnable(taskStep.getEnable());
        if (CollectionUtils.isNotEmpty(taskStep.getRefVariables())) {
            stepVO.setRefVariables(taskStep.getRefVariables().stream().map(TaskVariableDTO::getName)
                .distinct().collect(Collectors.toList()));
        }
        return stepVO;
    }

    public static TaskStepDTO fromVO(TaskStepVO stepVO) {
        TaskStepDTO taskStep = new TaskStepDTO();
        taskStep.setId(stepVO.getId());
        taskStep.setType(TaskStepTypeEnum.valueOf(stepVO.getType()));
        taskStep.setName(stepVO.getName());
        if (stepVO.getTemplateStepId() != null && stepVO.getTemplateStepId() > 0) {
            taskStep.setTemplateStepId(stepVO.getTemplateStepId());
        }
        taskStep.setEnable(stepVO.getEnable());
        if (stepVO.getDelete() != null) {
            taskStep.setDelete(stepVO.getDelete());
            if (taskStep.getDelete() == 1) {
                return taskStep;
            }
        } else {
            taskStep.setDelete(0);
        }
        if (taskStep.getType() == null) {
            throw new InvalidParamException(ErrorCode.WRONG_STEP_TYPE);
        }
        switch (taskStep.getType()) {
            case SCRIPT:
                taskStep.setScriptStepInfo(TaskScriptStepDTO.fromVO(stepVO.getId(), stepVO.getScriptStepInfo()));
                break;
            case FILE:
                taskStep.setFileStepInfo(TaskFileStepDTO.fromVO(stepVO.getId(), stepVO.getFileStepInfo()));
                break;
            case APPROVAL:
                taskStep.setApprovalStepInfo(TaskApprovalStepDTO.fromVO(stepVO.getId(), stepVO.getApprovalStepInfo()));
                break;
            default:
                throw new InvalidParamException(ErrorCode.WRONG_STEP_TYPE);
        }
        return taskStep;
    }

    public static EsbStepV3DTO toEsbStepV3(TaskStepDTO taskStep) {
        if (taskStep == null) {
            return null;
        }
        EsbStepV3DTO esbStep = new EsbStepV3DTO();
        esbStep.setId(taskStep.getId());
        esbStep.setName(taskStep.getName());
        esbStep.setType(taskStep.getType().getType());
        switch (taskStep.getType()) {
            case SCRIPT:
                esbStep.setScriptInfo(TaskScriptStepDTO.toEsbScriptInfoV3(taskStep.getScriptStepInfo()));
                break;
            case FILE:
                esbStep.setFileInfo(TaskFileStepDTO.toEsbFileInfoV3(taskStep.getFileStepInfo()));
                break;
            case APPROVAL:
                esbStep.setApprovalInfo(TaskApprovalStepDTO.toEsbApprovalInfoV3(taskStep.getApprovalStepInfo()));
                break;
            default:
                return esbStep;
        }
        return esbStep;
    }

    public static ServiceTaskStepDTO toServiceDTO(TaskStepDTO taskStep) {
        if (taskStep == null) {
            return null;
        }
        ServiceTaskStepDTO serviceTaskStep = new ServiceTaskStepDTO();
        serviceTaskStep.setId(taskStep.getId());
        serviceTaskStep.setName(taskStep.getName());
        serviceTaskStep.setType(taskStep.getType().getType());
        serviceTaskStep.setEnable(taskStep.getEnable());
        switch (taskStep.getType()) {
            case SCRIPT:
                serviceTaskStep.setScriptStepInfo(TaskScriptStepDTO.toServiceScriptInfo(taskStep.getScriptStepInfo()));
                break;
            case FILE:
                serviceTaskStep.setFileStepInfo(TaskFileStepDTO.toServiceFileInfo(taskStep.getFileStepInfo()));
                break;
            case APPROVAL:
                serviceTaskStep.setApprovalStepInfo(
                    TaskApprovalStepDTO.toServiceApprovalInfo(
                        taskStep.getApprovalStepInfo()
                    )
                );
                break;
            default:
                return serviceTaskStep;
        }
        return serviceTaskStep;
    }
}
