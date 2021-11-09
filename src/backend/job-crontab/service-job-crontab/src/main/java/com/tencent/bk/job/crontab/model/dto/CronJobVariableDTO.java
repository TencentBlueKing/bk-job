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

package com.tencent.bk.job.crontab.model.dto;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.esb.model.job.v3.EsbGlobalVarV3DTO;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.crontab.model.CronJobVariableVO;
import com.tencent.bk.job.crontab.model.inner.ServerDTO;
import com.tencent.bk.job.execute.model.inner.ServiceTaskVariable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @since 3/10/2019 17:14
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class CronJobVariableDTO {
    /**
     * 变量 ID
     */
    private Long id;

    /**
     * 变量名
     */
    private String name;

    /**
     * 变量类型
     *
     * @see TaskVariableTypeEnum
     */
    private TaskVariableTypeEnum type;

    /**
     * 普通变量值
     */
    private String value;

    /**
     * 主机类型变量值
     */
    private ServerDTO server;

    public static CronJobVariableVO toVO(CronJobVariableDTO variableInfo) {
        CronJobVariableVO taskVariable = new CronJobVariableVO();
        taskVariable.setId(variableInfo.getId());
        taskVariable.setName(variableInfo.getName());
        taskVariable.setType(variableInfo.getType().getType());
        if (TaskVariableTypeEnum.HOST_LIST == variableInfo.getType()) {
            taskVariable.setTargetValue(ServerDTO.toTargetVO(variableInfo.getServer()));
        } else {
            if (variableInfo.getType().needMask()) {
                taskVariable.setValue(variableInfo.getType().getMask());
            } else {
                taskVariable.setValue(variableInfo.getValue());
            }
        }
        return taskVariable;
    }

    public static CronJobVariableDTO fromVO(CronJobVariableVO variableVO) {
        CronJobVariableDTO variableInfo = new CronJobVariableDTO();
        variableInfo.setId(variableVO.getId());
        variableInfo.setName(variableVO.getName());
        variableInfo.setType(TaskVariableTypeEnum.valOf(variableVO.getType()));
        if (variableInfo.getType() == null) {
            throw new InvalidParamException(ErrorCode.WRONG_VARIABLE_TYPE);
        }
        if (TaskVariableTypeEnum.HOST_LIST == variableInfo.getType()) {
            variableInfo.setServer(ServerDTO.fromTargetVO(variableVO.getTargetValue()));
        } else {
            variableInfo.setValue(variableVO.getValue());
        }
        return variableInfo;
    }

    public static ServiceTaskVariable toServiceTaskVariable(CronJobVariableDTO variableInfo) {
        if (variableInfo == null) {
            return null;
        }
        ServiceTaskVariable taskVariable = new ServiceTaskVariable();
        taskVariable.setId(variableInfo.getId());
        taskVariable.setType(variableInfo.getType().getType());
        switch (variableInfo.getType()) {
            case STRING:
            case CIPHER:
            case INDEX_ARRAY:
            case ASSOCIATIVE_ARRAY:
                taskVariable.setStringValue(variableInfo.getValue());
                break;
            case NAMESPACE:
                taskVariable.setNamespaceValue(variableInfo.getValue());
                break;
            case HOST_LIST:
                taskVariable.setServerValue(ServerDTO.toServiceServer(variableInfo.getServer()));
                break;
            default:
                break;
        }
        return taskVariable;
    }

    public static EsbGlobalVarV3DTO toEsbGlobalVarV3(CronJobVariableDTO cronJobVariableDTO) {
        if (cronJobVariableDTO == null) {
            return null;
        }
        EsbGlobalVarV3DTO esbGlobalVar = new EsbGlobalVarV3DTO();
        esbGlobalVar.setId(cronJobVariableDTO.getId());
        esbGlobalVar.setName(cronJobVariableDTO.getName());
        esbGlobalVar.setType(cronJobVariableDTO.getType().getType());
        esbGlobalVar.setValue(cronJobVariableDTO.getValue());
        esbGlobalVar.setServer(ServerDTO.toEsbServerV3(cronJobVariableDTO.getServer()));
        return esbGlobalVar;
    }
}
