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

package com.tencent.bk.job.execute.model.converter;

import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.inner.ServiceTaskInstanceDTO;
import com.tencent.bk.job.execute.model.web.vo.TaskInstanceVO;

import javax.validation.constraints.NotNull;
import java.util.Objects;

public class TaskInstanceConverter {
    public static TaskInstanceVO convertToTaskInstanceVO(@NotNull TaskInstanceDTO taskInstanceDTO,
                                                         @NotNull MessageI18nService i18nService) {
        TaskInstanceVO taskInstanceVO = new TaskInstanceVO();
        taskInstanceVO.setId(taskInstanceDTO.getId());
        taskInstanceVO.setAppId(taskInstanceDTO.getAppId());
        taskInstanceVO.setName(taskInstanceDTO.getName());
        taskInstanceVO.setOperator(taskInstanceDTO.getOperator());
        taskInstanceVO.setStartupMode(taskInstanceDTO.getStartupMode());
        taskInstanceVO.setStartupModeDesc(
            i18nService.getI18n(TaskStartupModeEnum.getStartupMode(taskInstanceDTO.getStartupMode()).getI18nKey()));
        taskInstanceVO.setStatus(taskInstanceDTO.getStatus());
        taskInstanceVO.setStatusDesc(
            i18nService.getI18n(Objects.requireNonNull(
                RunStatusEnum.valueOf(taskInstanceDTO.getStatus())).getI18nKey()));
        taskInstanceVO.setType(taskInstanceDTO.getType());
        taskInstanceVO.setTypeDesc(
            i18nService.getI18n(Objects.requireNonNull(TaskTypeEnum.valueOf(taskInstanceDTO.getType())).getI18nKey()));
        taskInstanceVO.setTaskId(taskInstanceDTO.getTaskId());
        taskInstanceVO.setTemplateId(taskInstanceDTO.getTaskTemplateId());
        taskInstanceVO.setDebugTask(taskInstanceDTO.isDebugTask());
        taskInstanceVO.setTotalTime(taskInstanceDTO.getTotalTime());
        taskInstanceVO.setCreateTime(taskInstanceDTO.getCreateTime());
        taskInstanceVO.setStartTime(taskInstanceDTO.getStartTime());
        taskInstanceVO.setEndTime(taskInstanceDTO.getEndTime());
        return taskInstanceVO;
    }

    public static ServiceTaskInstanceDTO convertToServiceTaskInstanceDTO(@NotNull TaskInstanceDTO taskInstanceDTO,
                                                                         @NotNull MessageI18nService i18nService) {
        ServiceTaskInstanceDTO serviceTaskInstance = new ServiceTaskInstanceDTO();
        serviceTaskInstance.setId(taskInstanceDTO.getId());
        serviceTaskInstance.setAppId(taskInstanceDTO.getAppId());
        serviceTaskInstance.setOperator(taskInstanceDTO.getOperator());
        serviceTaskInstance.setName(taskInstanceDTO.getName());
        serviceTaskInstance.setStartupMode(taskInstanceDTO.getStartupMode());
        serviceTaskInstance.setStartupModeDesc(
            i18nService.getI18n(TaskStartupModeEnum.getStartupMode(taskInstanceDTO.getStartupMode()).getI18nKey()));
        serviceTaskInstance.setType(taskInstanceDTO.getType());
        serviceTaskInstance.setTypeDesc(
            i18nService.getI18n(Objects.requireNonNull(TaskTypeEnum.valueOf(taskInstanceDTO.getType())).getI18nKey()));
        serviceTaskInstance.setStatus(taskInstanceDTO.getStatus());
        serviceTaskInstance.setStatusDesc(
            i18nService.getI18n(Objects.requireNonNull(RunStatusEnum.valueOf(taskInstanceDTO.getStatus()))
                .getI18nKey()));
        serviceTaskInstance.setDebugTask(taskInstanceDTO.isDebugTask());
        serviceTaskInstance.setTaskId(taskInstanceDTO.getTaskId());
        serviceTaskInstance.setTemplateId(taskInstanceDTO.getTaskTemplateId());
        serviceTaskInstance.setStartTime(taskInstanceDTO.getStartTime());
        serviceTaskInstance.setEndTime(taskInstanceDTO.getEndTime());
        serviceTaskInstance.setTotalTime(taskInstanceDTO.getTotalTime());
        serviceTaskInstance.setCreateTime(taskInstanceDTO.getCreateTime());
        return serviceTaskInstance;
    }
}
