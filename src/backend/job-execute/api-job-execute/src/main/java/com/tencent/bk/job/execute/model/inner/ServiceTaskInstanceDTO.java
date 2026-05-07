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

package com.tencent.bk.job.execute.model.inner;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "作业实例")
@Data
public class ServiceTaskInstanceDTO {
    /**
     * id
     */
    @Schema(description = "任务 ID")
    private Long id;

    /**
     * 执行方案id
     */
    @Schema(description = "执行方案 ID")
    private Long taskId;

    /**
     * id
     */
    @Schema(description = "定时任务 ID")
    private Long cronTaskId;

    /**
     * 执行方案id
     */
    @Schema(description = "作业模板 ID")
    private Long templateId;

    /**
     * 是否调试执行方案
     */
    @Schema(description = "是否调试执行方案")
    private Boolean debugTask;

    /**
     * 业务id
     */
    @Schema(description = "业务ID")
    private Long appId;

    /**
     * 名称
     */
    @Schema(description = "任务名称")
    private String name;

    /**
     * 执行人
     */
    @Schema(description = "执行人")
    private String operator;

    /**
     * 启动方式
     */
    @Schema(description = "启动方式，1-页面执行、2-API调用、3-定时执行")
    private Integer startupMode;

    /**
     * 启动方式
     */
    @Schema(description = "启动方式名称")
    private String startupModeDesc;

    /**
     * 状态
     */
    @Schema(description = "任务状态")
    private Integer status;

    /**
     * 任务状态描述
     */
    @Schema(description = "任务状态描述")
    private String statusDesc;

    /**
     * 开始时间
     */
    @Schema(description = "开始时间")
    private Long startTime;

    /**
     * 结束时间
     */
    @Schema(description = "结束时间")
    private Long endTime;

    /**
     * 总耗时，单位：秒
     */
    @Schema(description = "总耗时")
    private float totalTime;

    /**
     * 创建时间
     */
    @Schema(description = "创建时间")
    private Long createTime;

    /**
     * 作业执行类型
     */
    @Schema(description = "任务类型,0-作业执行,1-脚本执行,2-文件分发")
    private Integer type;

    @Schema(description = "任务类型描述")
    private String typeDesc;
}
