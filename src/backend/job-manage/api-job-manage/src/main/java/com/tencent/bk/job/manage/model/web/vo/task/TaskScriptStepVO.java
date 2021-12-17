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

import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.model.vo.TaskTargetVO;
import com.tencent.bk.job.common.util.JobContextUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;

/**
 * @since 16/10/2019 14:46
 */
@Data
@ApiModel("任务脚本步骤信息")
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
    @NotNull(message = "{validation.constraints.InvalidJobTimeout_empty.message}")
    @Range(min = JobConstants.MIN_JOB_TIMEOUT_SECONDS, max= JobConstants.MAX_JOB_TIMEOUT_SECONDS,
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

    public boolean validate(boolean isCreate) {
        if (scriptSource == null || scriptSource <= 0) {
            JobContextUtil.addDebugMessage("Missing script step type!");
            return false;
        }
        if (status == null || status > 0b11 || status < 0) {
            status = 0;
        }
        switch (scriptSource) {
            case 1:
                if (StringUtils.isBlank(content)) {
                    JobContextUtil.addDebugMessage("Local step missing content!");
                    return false;
                }
                scriptId = null;
                scriptVersionId = null;
                break;
            case 2:
            case 3:
                if (StringUtils.isBlank(scriptId) || scriptVersionId == null || scriptVersionId <= 0) {
                    JobContextUtil.addDebugMessage("Link script missing script id or script version id!");
                    return false;
                }
                content = null;
                break;
            default:
                JobContextUtil.addDebugMessage("Unknown script type!");
                return false;
        }
        if (account == null || account <= 0) {
            JobContextUtil.addDebugMessage("Missing execute account!");
            return false;
        }
        if (StringUtils.isNotBlank(scriptParam) && scriptParam.length() > 5000) {
            scriptParam = scriptParam.substring(0, 5000);
        }
        if (timeout == null) {
            timeout = 3600L;
        }
        if (ignoreError == null || ignoreError < 0) {
            ignoreError = 0;
        }
        return executeTarget.validate(isCreate);
    }
}
