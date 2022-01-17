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

package com.tencent.bk.job.execute.model.web.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.execute.model.web.vo.ExecuteTargetVO;
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
    private String scriptParam;

    /**
     * 执行超时时间
     */
    @ApiModelProperty(value = "执行超时时间，单位秒", required = true)
    @NotNull(message = "{validation.constraints.InvalidJobTimeout_empty.message}")
    @Range(min = JobConstants.MIN_JOB_TIMEOUT_SECONDS, max= JobConstants.MAX_JOB_TIMEOUT_SECONDS,
        message = "{validation.constraints.InvalidJobTimeout_outOfRange.message}")
    private Integer timeout;

    /**
     * 目标服务器
     */
    private ExecuteTargetVO targetServers;

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

}
