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

package com.tencent.bk.job.manage.model.web.vo.script;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 脚本同步结果VO
 */
@Getter
@Setter
@ApiModel("脚本同步结果")
public class ScriptSyncResultVO {
    public static final int SYNC_SUCCESS = 1;
    public static final int SYNC_FAIL = 2;

    @ApiModelProperty("业务ID")
    private Long appId;
    @ApiModelProperty("脚本ID")
    private String scriptId;
    @ApiModelProperty("步骤引用的脚本版本ID")
    private Long scriptVersionId;
    @ApiModelProperty("步骤引用的脚本版本号")
    private String scriptVersion;
    @ApiModelProperty("脚本名称")
    private String scriptName;
    @ApiModelProperty("步骤ID")
    private Long stepId;
    @ApiModelProperty("步骤名称")
    private String stepName;
    @ApiModelProperty("同步状态,1-成功，2-失败")
    private Integer syncStatus;
    @ApiModelProperty("脚本状态")
    private Integer scriptStatus;
    @ApiModelProperty("脚本状态描述")
    private String scriptStatusDesc;
    @ApiModelProperty("作业模板ID")
    private Long templateId;
    @ApiModelProperty("作业模板名称")
    private String templateName;
    @ApiModelProperty("失败原因")
    private String failMsg;

}
