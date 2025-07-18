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

package com.tencent.bk.job.execute.model.web.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.constant.CompatibleType;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.constant.MySQLTextDataType;
import com.tencent.bk.job.common.model.vo.TaskTargetVO;
import com.tencent.bk.job.common.validation.EndWith;
import com.tencent.bk.job.common.validation.MaxLength;
import com.tencent.bk.job.common.validation.NotExceedMySQLTextFieldLength;
import com.tencent.bk.job.execute.model.web.vo.RollingConfigVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;

/**
 * 脚本执行请求
 */
@Data
@ApiModel("快速执行脚本请求报文")
public class WebFastExecuteScriptRequest {
    /**
     * 脚本执行任务名称
     */
    @ApiModelProperty(value = "脚本执行任务名称", required = true)
    private String name;
    /**
     * 脚本内容
     */
    @ApiModelProperty(value = "脚本内容，BASE64编码，当手动录入的时候使用此参数")
    @NotExceedMySQLTextFieldLength(
        fieldName = "scriptContent",
        fieldType = MySQLTextDataType.MEDIUMTEXT,
        base64 = true
    )
    private String content;

    @ApiModelProperty(value = "脚本ID,当引用脚本的时候传该参数")
    private String scriptId;

    @ApiModelProperty(value = "脚本版本ID,当引用脚本的时候传该参数")
    private Long scriptVersionId;

    /**
     * 执行账号
     */
    @ApiModelProperty(value = "执行账号ID", required = true)
    private Long account;

    /**
     * 脚本来源
     */
    @ApiModelProperty(value = "脚本来源，1-本地脚本 2-引用业务脚本 3-引用公共脚本", required = true)
    private Integer scriptSource;

    /**
     * 脚本类型
     */
    @ApiModelProperty(value = "脚本类型，1：shell，2：bat，3：perl，4：python，5：powershell，6：sql", required = true)
    private Integer scriptLanguage;

    /**
     * 脚本参数
     */
    @ApiModelProperty(value = "脚本参数")
    @NotExceedMySQLTextFieldLength(
        fieldName = "scriptParam",
        fieldType = MySQLTextDataType.TEXT,
        base64 = false
    )
    private String scriptParam;

    /**
     * 自定义Windows解释器路径
     */
    @ApiModelProperty(value = "自定义Windows解释器路径，对Linux机器不生效，对SQL脚本不生效")
    @EndWith(fieldName = "windowsInterpreter", value = ".exe",
        message = "{validation.constraints.WinInterpreterInvalidSuffix.message}")
    @MaxLength(value = 260,
        message = "{validation.constraints.WindowsInterpreterExceedMaxLength.message}")
    private String windowsInterpreter;

    /**
     * 执行超时时间
     */
    @ApiModelProperty(value = "执行超时时间，单位秒", required = true)
    @NotNull(message = "{validation.constraints.InvalidJobTimeout_empty.message}")
    @Range(
        min = JobConstants.MIN_JOB_TIMEOUT_SECONDS,
        max = JobConstants.MAX_JOB_TIMEOUT_SECONDS,
        message = "{validation.constraints.InvalidJobTimeout_outOfRange.message}"
    )
    private Integer timeout;

    /**
     * 目标执行对象
     */
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.DEPLOY,
        explain = "使用 taskTarget 参数替换。发布完成后可以删除")
    @ApiModelProperty(hidden = true)
    private TaskTargetVO targetServers;

    /**
     * 目标执行对象
     */
    @ApiModelProperty(value = "执行目标", required = true)
    private TaskTargetVO taskTarget;

    /**
     * 是否敏感参数 0-否，1-是
     */
    @ApiModelProperty(value = "是否敏感参数 0-否，1-是。默认0")
    private Integer secureParam = 0;

    @ApiModelProperty(value = "是否是重做任务")
    @JsonProperty("isRedoTask")
    private boolean redoTask;

    @ApiModelProperty(value = "任务实例ID,重做的时候需要传入")
    private Long taskInstanceId;

    @ApiModelProperty(value = "滚动配置, 滚动执行需要传入")
    private RollingConfigVO rollingConfig;

    @ApiModelProperty(value = "是否启用滚动执行")
    private boolean rollingEnabled;

    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.DEPLOY,
        explain = "发布完成后可以删除")
    public TaskTargetVO getTaskTarget() {
        return taskTarget != null ? taskTarget : targetServers;
    }

    /**
     * 获取去除首尾空格后的windowsInterpreter
     *
     * @return Trim后的windowsInterpreter
     */
    public String getTrimmedWindowsInterpreter() {
        return windowsInterpreter != null ? windowsInterpreter.trim() : null;
    }
}
