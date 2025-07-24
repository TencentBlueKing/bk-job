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

package com.tencent.bk.job.manage.model.web.vo.task;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.util.check.IlegalCharChecker;
import com.tencent.bk.job.common.util.check.MaxLengthChecker;
import com.tencent.bk.job.common.util.check.NotEmptyChecker;
import com.tencent.bk.job.common.util.check.StringCheckHelper;
import com.tencent.bk.job.common.util.check.TrimChecker;
import com.tencent.bk.job.common.util.check.exception.StringCheckException;
import com.tencent.bk.job.manage.api.common.constants.task.TaskStepTypeEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.validation.Valid;
import java.util.List;

@Data
@ApiModel("任务步骤信息")
@Slf4j
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
    @Valid
    private TaskScriptStepVO scriptStepInfo;

    @ApiModelProperty("文件步骤信息")
    @Valid
    private TaskFileStepVO fileStepInfo;

    @ApiModelProperty("审批步骤信息")
    @Valid
    private TaskApprovalStepVO approvalStepInfo;

    @ApiModelProperty(value = "删除 0-不删除 1-删除，仅在删除时填写")
    private Integer delete;

    @ApiModelProperty(value = "是否启用 0-未启用 1-启用")
    private Integer enable;

    @ApiModelProperty(value = "引用的全局变量")
    private List<String> refVariables;

    public void validate(boolean isCreate) throws InvalidParamException {
        if (isCreate) {
            if (id != null && id > 0) {
                log.warn("Create request has step id!");
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
            }
            if (StringUtils.isBlank(name)) {
                log.warn("Create request missing step name!");
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
            }
        }
        if (delete != null && delete == 1) {
            return;
        }
        try {
            StringCheckHelper stepCheckHelper = new StringCheckHelper(new TrimChecker(), new NotEmptyChecker(),
                new IlegalCharChecker(), new MaxLengthChecker(60));
            this.name = stepCheckHelper.checkAndGetResult(name);
        } catch (StringCheckException e) {
            log.warn("Step name is invalid, stepName: {}", name);
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        TaskStepTypeEnum stepType = TaskStepTypeEnum.valueOf(type);
        switch (stepType) {
            case SCRIPT:
                if (scriptStepInfo == null) {
                    log.warn("Empty script step");
                    throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
                }
                scriptStepInfo.validate(isCreate);
                break;
            case FILE:
                if (fileStepInfo == null) {
                    log.warn("Empty file step");
                    throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
                }
                fileStepInfo.validate(isCreate);
                break;
            case APPROVAL:
                if (approvalStepInfo == null) {
                    log.warn("Empty approval step");
                    throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
                }
                approvalStepInfo.validate(isCreate);
                break;
            default:
                log.warn("Invalid step type: {}", type);
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
    }
}
