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

package com.tencent.bk.job.manage.model.web.vo.task;

import com.tencent.bk.job.common.model.vo.UserRoleInfoVO;
import com.tencent.bk.job.common.util.JobContextUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * @since 16/10/2019 14:47
 */
@Data
@ApiModel("任务审批步骤信息")
public class TaskApprovalStepVO {

    @ApiModelProperty(value = "审批类型 暂未启用 1-任意人审批 2-所有人审批")
    private Integer approvalType;

    @ApiModelProperty("审批人")
    private UserRoleInfoVO approvalUser;

    @ApiModelProperty("审批消息")
    private String approvalMessage;

    @ApiModelProperty("通知渠道")
    private List<String> notifyChannel;

    public boolean validate(boolean isCreate) {
        if (notifyChannel == null) {
            notifyChannel = Collections.emptyList();
        }
        if (approvalMessage == null) {
            approvalMessage = "";
        }
        if (approvalUser == null) {
            JobContextUtil.addDebugMessage("Approval step must have user!");
            return false;
        }
        return approvalUser.validate();
    }
}
