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

import com.tencent.bk.job.common.model.dto.UserRoleInfoDTO;
import com.tencent.bk.job.manage.common.consts.task.TaskApprovalTypeEnum;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbApprovalStepV3DTO;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbUserRoleInfoV3DTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskApprovalStepDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskApprovalUserDTO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskApprovalStepVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @since 3/10/2019 17:06
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class TaskApprovalStepDTO {
    private Long id;

    private Long stepId;

    private TaskApprovalTypeEnum approvalType;

    private UserRoleInfoDTO approvalUser;

    private String approvalMessage;

    private List<String> notifyChannel;

    public static TaskApprovalStepVO toVO(TaskApprovalStepDTO approvalStepInfo) {
        if (approvalStepInfo == null) {
            return null;
        }

        TaskApprovalStepVO approvalStep = new TaskApprovalStepVO();
        approvalStep.setApprovalType(approvalStepInfo.getApprovalType().getType());
        approvalStep.setApprovalUser(UserRoleInfoDTO.toVO(approvalStepInfo.getApprovalUser()));
        approvalStep.setApprovalMessage(approvalStepInfo.getApprovalMessage());
        approvalStep.setNotifyChannel(approvalStepInfo.getNotifyChannel());
        return approvalStep;
    }

    public static TaskApprovalStepDTO fromVO(Long stepId, TaskApprovalStepVO approvalStepVO) {
        if (approvalStepVO == null) {
            return null;
        }
        TaskApprovalStepDTO approvalStep = new TaskApprovalStepDTO();
        approvalStep.setStepId(stepId);
        if (TaskApprovalTypeEnum.valueOf(approvalStepVO.getApprovalType()) == null) {
            approvalStep.setApprovalType(TaskApprovalTypeEnum.ANYONE);
        } else {
            approvalStep.setApprovalType(TaskApprovalTypeEnum.valueOf(approvalStepVO.getApprovalType()));
        }
        approvalStep.setApprovalUser(UserRoleInfoDTO.fromVO(approvalStepVO.getApprovalUser()));
        approvalStep.setApprovalMessage(approvalStepVO.getApprovalMessage());
        approvalStep.setNotifyChannel(approvalStepVO.getNotifyChannel());
        return approvalStep;
    }

    public static EsbApprovalStepV3DTO toEsbApprovalInfoV3(TaskApprovalStepDTO approvalStepInfo) {
        if (approvalStepInfo == null) {
            return null;
        }
        EsbApprovalStepV3DTO esbApprovalStep = new EsbApprovalStepV3DTO();
        esbApprovalStep.setApprovalType(approvalStepInfo.getApprovalType().getType());
        esbApprovalStep.setApprovalUser(EsbUserRoleInfoV3DTO.fromUserRoleInfo(approvalStepInfo.getApprovalUser()));
        esbApprovalStep.setApprovalMessage(approvalStepInfo.getApprovalMessage());
        esbApprovalStep.setNotifyChannel(approvalStepInfo.getNotifyChannel());
        return esbApprovalStep;
    }

    public static ServiceTaskApprovalStepDTO toServiceApprovalInfo(TaskApprovalStepDTO approvalStepInfo) {
        if (approvalStepInfo == null) {
            return null;
        }
        ServiceTaskApprovalStepDTO serviceApprovalStep = new ServiceTaskApprovalStepDTO();
        serviceApprovalStep.setApprovalType(approvalStepInfo.getApprovalType().getType());
        serviceApprovalStep.setApprovalUser(ServiceTaskApprovalUserDTO.fromDTO(approvalStepInfo.getApprovalUser()));
        serviceApprovalStep.setApprovalMessage(approvalStepInfo.getApprovalMessage());
        serviceApprovalStep.setChannels(approvalStepInfo.getNotifyChannel());
        return serviceApprovalStep;
    }
}
