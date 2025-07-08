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

package com.tencent.bk.job.manage.model.web.request;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.util.check.IlegalCharChecker;
import com.tencent.bk.job.common.util.check.MaxLengthChecker;
import com.tencent.bk.job.common.util.check.NotEmptyChecker;
import com.tencent.bk.job.common.util.check.StringCheckHelper;
import com.tencent.bk.job.common.util.check.TrimChecker;
import com.tencent.bk.job.common.util.check.exception.StringCheckException;
import com.tencent.bk.job.manage.model.web.vo.task.TaskStepVO;
import com.tencent.bk.job.manage.model.web.vo.task.TaskVariableVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import javax.validation.Valid;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("作业模版新增、更新请求报文")
public class TaskTemplateCreateUpdateReq extends TemplateBasicInfoUpdateReq {
    /**
     * 模版步骤
     */
    @ApiModelProperty(value = "模版步骤, 新增、修改、删除时需要传入", required = true)
    @Valid
    private List<TaskStepVO> steps;

    /**
     * 模版变量
     */
    @ApiModelProperty(value = "模版变量, 新增、修改、删除时需要传入")
    private List<TaskVariableVO> variables;

    public void validate() throws InvalidParamException {
        // 模板名称检查
        try {
            StringCheckHelper stringCheckHelper = new StringCheckHelper(new TrimChecker(), new NotEmptyChecker(),
                new IlegalCharChecker(), new MaxLengthChecker(60));
            this.setName(stringCheckHelper.checkAndGetResult(this.getName()));
        } catch (StringCheckException e) {
            log.warn("Template name is invalid, name: {}", getName());
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        if (this.getDescription() == null) {
            this.setDescription("");
        }
        boolean isCreate = true;
        if (this.getId() != null && this.getId() > 0) {
            isCreate = false;
        }
        if (isCreate) {
            if (CollectionUtils.isEmpty(steps)) {
                log.warn("Empty template step!");
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
            }
        }
        if (CollectionUtils.isNotEmpty(steps)) {
            for (TaskStepVO step : steps) {
                step.validate(isCreate);
            }
        }
        if (CollectionUtils.isNotEmpty(variables)) {
            Set<String> variableNameList = new HashSet<>();
            for (TaskVariableVO variable : variables) {
                variable.validate(isCreate);
                if (variableNameList.contains(variable.getName())) {
                    log.warn("Variable name duplicated!");
                    throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
                }
                if (variable.getDelete() != 1) {
                    variableNameList.add(variable.getName());
                }
            }
        }
    }
}
