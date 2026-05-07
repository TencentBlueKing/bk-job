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

package com.tencent.bk.job.manage.model.inner;

import com.tencent.bk.job.manage.api.common.constants.JobResourceStatusEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskScriptSourceEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "任务脚本步骤信息")
public class ServiceTaskScriptStepDTO {
    @Schema(description = "脚本ID")
    private String scriptId;

    @Schema(description = "脚本版本ID")
    private Long scriptVersionId;

    /**
     * @see JobResourceStatusEnum
     */
    @Schema(description = "脚本状态")
    private Integer scriptStatus;

    /**
     * 脚本引用类型
     *
     * @see TaskScriptSourceEnum
     */
    @Schema(description = "脚本引用类型")
    private Integer scriptSource;

    /**
     * 脚本类型
     *
     * @see com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum
     */
    @Schema(description = "脚本语言类型")
    private Integer scriptType;

    /**
     * 脚本类型
     *
     * @see com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum
     */
    @Schema(description = "脚本语言类型")
    private Integer type;

    @Schema(description = "脚本内容")
    private String content;

    @Schema(description = "脚本参数")
    private String scriptParam;

    @Schema(description = "Windows解释器")
    private String windowsInterpreter;

    @Schema(description = "脚本超时时间")
    private Long scriptTimeout;

    @Schema(description = "执行账户")
    private ServiceAccountDTO account;

    @Schema(description = "执行目标")
    private ServiceTaskTargetDTO executeTarget;

    @Schema(description = "敏感参数")
    private Boolean secureParam;

    @Schema(description = "是否自动忽略错误")
    private Boolean ignoreError;
}
