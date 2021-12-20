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
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.esb.model.job.v3.EsbGlobalVarV3DTO;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.manage.model.inner.ServiceTaskVariableDTO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskVariableVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

/**
 * @since 3/10/2019 17:14
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class TaskVariableDTO {
    private Long id;

    private Long templateId;

    private Long planId;

    private Long instanceId;

    private String name;

    private TaskVariableTypeEnum type;

    private String defaultValue;

    private String description;

    private Boolean changeable;

    private Boolean required;

    private Boolean delete;

    public static TaskVariableVO toVO(TaskVariableDTO variableInfo) {
        TaskVariableVO taskVariable = new TaskVariableVO();
        taskVariable.setId(variableInfo.getId());
        taskVariable.setName(variableInfo.getName());
        taskVariable.setType(variableInfo.getType().getType());
        if (TaskVariableTypeEnum.HOST_LIST == variableInfo.getType()) {
            taskVariable
                .setDefaultTargetValue(TaskTargetDTO.toVO(TaskTargetDTO.fromString(variableInfo.getDefaultValue())));
        } else {
            if (variableInfo.getType().needMask()) {
                taskVariable.setDefaultValue(variableInfo.getType().getMask());
            } else {
                taskVariable.setDefaultValue(variableInfo.getDefaultValue());
            }
        }
        taskVariable.setDescription(variableInfo.getDescription());
        taskVariable.setChangeable(variableInfo.getChangeable() ? 1 : 0);
        taskVariable.setRequired(variableInfo.getRequired() ? 1 : 0);
        return taskVariable;
    }

    public static TaskVariableDTO fromVO(TaskVariableVO variableVO) {
        TaskVariableDTO variableInfo = new TaskVariableDTO();
        variableInfo.setId(variableVO.getId());
        if (variableVO.getDelete() != null) {
            variableInfo.setDelete(variableVO.getDelete() == 1);
            if (variableInfo.getDelete()) {
                return variableInfo;
            }
        } else {
            variableInfo.setDelete(false);
        }
        variableInfo.setName(variableVO.getName());
        variableInfo.setType(TaskVariableTypeEnum.valOf(variableVO.getType()));
        if (variableInfo.getType() == null) {
            throw new InvalidParamException(ErrorCode.WRONG_VARIABLE_TYPE);
        }
        if (TaskVariableTypeEnum.HOST_LIST == variableInfo.getType()) {
            variableInfo.setDefaultValue(TaskTargetDTO.fromVO(variableVO.getDefaultTargetValue()).toString());
        } else {
            variableInfo.setDefaultValue(variableVO.getDefaultValue());
        }
        if (StringUtils.isNotBlank(variableVO.getDescription())) {
            variableInfo.setDescription(variableVO.getDescription());
        } else {
            variableInfo.setDescription("");
        }
        if (TaskVariableTypeEnum.NAMESPACE == variableInfo.getType()) {
            variableInfo.setChangeable(true);
        } else {
            variableInfo.setChangeable(variableVO.getChangeable() == 1);
        }
        variableInfo.setRequired(variableVO.getRequired() == 1);
        return variableInfo;
    }

    public static ServiceTaskVariableDTO toServiceDTO(TaskVariableDTO taskVariable) {
        if (taskVariable == null) {
            return null;
        }
        ServiceTaskVariableDTO serviceTaskVariable = new ServiceTaskVariableDTO();
        serviceTaskVariable.setId(taskVariable.getId());
        serviceTaskVariable.setName(taskVariable.getName());
        serviceTaskVariable.setType(taskVariable.getType().getType());
        if (TaskVariableTypeEnum.HOST_LIST == taskVariable.getType()) {
            TaskTargetDTO taskTarget = TaskTargetDTO.fromString(taskVariable.getDefaultValue());
            if (taskTarget != null) {
                serviceTaskVariable.setDefaultTargetValue(taskTarget.toServiceTaskTargetDTO());
            }
        } else {
            serviceTaskVariable.setDefaultValue(taskVariable.getDefaultValue());
        }
        serviceTaskVariable.setChangeable(taskVariable.getChangeable());
        serviceTaskVariable.setRequired(taskVariable.getRequired());
        return serviceTaskVariable;
    }

    public static EsbGlobalVarV3DTO toEsbGlobalVarV3(TaskVariableDTO taskVariable) {
        if (taskVariable == null) {
            return null;
        }
        EsbGlobalVarV3DTO esbGlobalVar = new EsbGlobalVarV3DTO();
        esbGlobalVar.setId(taskVariable.getId());
        esbGlobalVar.setName(taskVariable.getName());
        esbGlobalVar.setType(taskVariable.getType().getType());
        esbGlobalVar.setDescription(taskVariable.getDescription());
        esbGlobalVar.setRequired(taskVariable.getRequired() ? 1 : 0);

        if (TaskVariableTypeEnum.HOST_LIST == taskVariable.getType()) {
            esbGlobalVar.setServer(
                TaskTargetDTO.toEsbServerV3(
                    TaskTargetDTO.fromString(
                        taskVariable.getDefaultValue()
                    )
                )
            );
        } else {
            if (taskVariable.getType().needMask()) {
                esbGlobalVar.setValue(taskVariable.getType().getMask());
            } else {
                esbGlobalVar.setValue(taskVariable.getDefaultValue());
            }
        }
        return esbGlobalVar;
    }
}
