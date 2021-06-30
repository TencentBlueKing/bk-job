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

package com.tencent.bk.job.execute.model.web.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DangerousRecordVO {
    /**
     * ID
     */
    @ApiModelProperty("记录ID")
    private Long id;
    /**
     * 规则ID
     */
    @ApiModelProperty("规则ID")
    private Long ruleId;
    /**
     * 规则表达式
     */
    @ApiModelProperty("规则表达式")
    private String ruleExpression;
    /**
     * 业务ID
     */
    @ApiModelProperty("业务ID")
    private Long appId;
    /**
     * 业务名称
     */
    @ApiModelProperty("业务名称")
    private String appName;
    /**
     * 执行人
     */
    @ApiModelProperty("执行人")
    private String operator;
    /**
     * 脚本语言
     */
    @ApiModelProperty("脚本语言,1:shell,2:bat,3:perl,4:python,5:PowerShell,6:sql")
    private Integer scriptLanguage;
    /**
     * 脚本内容
     */
    @ApiModelProperty("脚本内容")
    private String scriptContent;
    /**
     * 记录创建时间
     */
    @ApiModelProperty("记录创建时间")
    private Long createTime;
    /**
     * 启动方式
     *
     * @see com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum
     */
    @ApiModelProperty("启动方式,1:页面执行,2:API调用,3:定时调用")
    private Integer startupMode;
    /**
     * 调用应用方
     */
    @ApiModelProperty("调用应用方")
    private String client;
    /**
     * 高危脚本处理动作
     */
    @ApiModelProperty("高危脚本处理动作")
    private Integer action;
    /**
     * 脚本检查结果
     */
    @ApiModelProperty("脚本检查结果")
    private List<ScriptCheckResultItemVO> checkResultItems;
    /**
     * 扩展数据
     */
    private Map<String, String> extData;
}
