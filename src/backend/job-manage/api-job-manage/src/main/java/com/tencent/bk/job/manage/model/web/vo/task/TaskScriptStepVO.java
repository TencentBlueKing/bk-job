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
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.model.vo.TaskTargetVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Range;

@Data
@ApiModel("任务脚本步骤信息")
@Slf4j
public class TaskScriptStepVO {

    @ApiModelProperty(value = "脚本类型 1-本地脚本 2-引用业务脚本 3-引用公共脚本")
    private Integer scriptSource;

    @ApiModelProperty("脚本 ID")
    private String scriptId;

    @ApiModelProperty("脚本版本 ID")
    private Long scriptVersionId;

    @ApiModelProperty("脚本内容")
    private String content;

    @ApiModelProperty("脚本语言")
    private Integer scriptLanguage;

    @ApiModelProperty("脚本参数")
    private String scriptParam;

    @ApiModelProperty("脚本超时时间")
    @Range(min = JobConstants.MIN_JOB_TIMEOUT_SECONDS, max = JobConstants.MAX_JOB_TIMEOUT_SECONDS,
        message = "{validation.constraints.InvalidJobTimeout_outOfRange.message}")
    private Long timeout;

    @ApiModelProperty("执行账户")
    private Long account;

    @ApiModelProperty("执行账户名称")
    private String accountName;

    @ApiModelProperty("执行目标")
    private TaskTargetVO executeTarget;

    @ApiModelProperty("敏感参数")
    private Integer secureParam;

    @ApiModelProperty("脚本状态 0 - 正常 1 - 需要更新 2 - 被禁用")
    private Integer status;

    @ApiModelProperty("忽略错误 0 - 不忽略 1 - 忽略")
    private Integer ignoreError;

    public void validate(boolean isCreate) throws InvalidParamException {
        if (scriptSource == null || scriptSource <= 0) {
            log.warn("Invalid script source : {}", scriptSource);
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        if (status == null || status > 0b11 || status < 0) {
            status = 0;
        }
        switch (scriptSource) {
            case 1:
                if (StringUtils.isBlank(content)) {
                    log.warn("Local step missing content!");
                    throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
                }
                scriptId = null;
                scriptVersionId = null;
                break;
            case 2:
            case 3:
                if (StringUtils.isBlank(scriptId) || scriptVersionId == null || scriptVersionId <= 0) {
                    log.warn("Link script missing script id or script version id!");
                    throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
                }
                content = null;
                break;
            default:
                log.warn("Unknown script type!");
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        if (account == null || account <= 0) {
            log.warn("Missing execute account!");
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        if (StringUtils.isNotBlank(scriptParam) && scriptParam.length() > 5000) {
            scriptParam = scriptParam.substring(0, 5000);
        }
        if (ignoreError == null || ignoreError < 0) {
            ignoreError = 0;
        }
        executeTarget.validate();
    }
}
