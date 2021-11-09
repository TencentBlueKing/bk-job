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

import com.tencent.bk.job.common.util.JobContextUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @since 16/10/2019 10:37
 */
@Data
@ApiModel("任务步骤信息")
public class TaskStepVO {

    @ApiModelProperty("步骤 ID 仅在更新、删除时填写")
    private Long id;

    @ApiModelProperty("步骤类型 1-脚本 2-文件 3-人工确认")
    private Integer type;

    @ApiModelProperty("步骤名称")
    private String name;

    @ApiModelProperty("模版中的步骤 ID 用于模版步骤匹配、同步")
    private Long templateStepId;

    @ApiModelProperty("脚本步骤信息")
    private TaskScriptStepVO scriptStepInfo;

    @ApiModelProperty("文件步骤信息")
    private TaskFileStepVO fileStepInfo;

    @ApiModelProperty("审批步骤信息")
    private TaskApprovalStepVO approvalStepInfo;

    @ApiModelProperty(value = "删除 0-不删除 1-删除，仅在删除时填写")
    private Integer delete;

    @ApiModelProperty(value = "是否启用 0-未启用 1-启用")
    private Integer enable;

    @ApiModelProperty(value = "引用的全局变量")
    private List<String> refVariables;

    public boolean validate(boolean isCreate) {
        if (isCreate) {
            if (id != null && id > 0) {
                JobContextUtil.addDebugMessage("Create request has step id!");
                return false;
            }
            if (StringUtils.isBlank(name)) {
                JobContextUtil.addDebugMessage("Create request missing step name!");
                return false;
            }
        }
        if (delete != null && delete == 1) {
            return true;
        }
        switch (type) {
            case 1:
                if (scriptStepInfo == null || !scriptStepInfo.validate(isCreate)) {
                    JobContextUtil.addDebugMessage("Script step info validate failed!");
                    return false;
                }
                break;
            case 2:
                if (fileStepInfo == null || !fileStepInfo.validate(isCreate)) {
                    JobContextUtil.addDebugMessage("File step info validate failed!");
                    return false;
                }
                break;
            case 3:
                if (approvalStepInfo == null || !approvalStepInfo.validate(isCreate)) {
                    JobContextUtil.addDebugMessage("Approval step info validate failed!");
                    return false;
                }
                break;
            default:
                return false;
        }
        return true;
    }
}
