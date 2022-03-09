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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.util.json.DecimalFormatJsonSerializer;
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@ApiModel("作业实例")
@Data
public class TaskInstanceVO {
    /**
     * id
     */
    @ApiModelProperty("任务 ID")
    private Long id;

    /**
     * 执行方案id
     */
    @ApiModelProperty("执行方案 ID")
    private Long taskId;

    /**
     * id
     */
    @ApiModelProperty("定时任务 ID")
    private Long cronTaskId;

    /**
     * 执行方案id
     */
    @ApiModelProperty("作业模板 ID")
    private Long templateId;

    /**
     * 是否调试执行方案
     */
    @ApiModelProperty("是否调试执行方案")
    private Boolean debugTask;

    /**
     * 业务id
     */
    @ApiModelProperty("业务ID")
    @CompatibleImplementation(explain = "为了无损发布保留的历史字段，发布完成需要删除", version = "3.5.1")
    private Long appId;

    /**
     * 资源范围类型
     */
    @ApiModelProperty(value = "资源范围类型", allowableValues = "biz-业务,biz_set-业务集")
    private String scopeType;

    /**
     * 资源范围ID
     */
    @ApiModelProperty("资源范围ID")
    private String scopeId;

    /**
     * 名称
     */
    @ApiModelProperty("任务名称")
    private String name;

    /**
     * 执行人
     */
    @ApiModelProperty("执行人")
    private String operator;

    /**
     * 启动方式
     */
    @ApiModelProperty("启动方式，1-页面执行、2-API调用、3-定时执行")
    private Integer startupMode;

    /**
     * 启动方式
     */
    @ApiModelProperty("启动方式名称")
    private String startupModeDesc;

    /**
     * 状态
     */
    @ApiModelProperty("任务状态")
    private Integer status;

    /**
     * 任务状态描述
     */
    @ApiModelProperty("任务状态描述")
    private String statusDesc;

    /**
     * 开始时间
     */
    @ApiModelProperty("开始时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long startTime;

    /**
     * 结束时间
     */
    @ApiModelProperty("结束时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long endTime;

    /**
     * 总耗时，单位：秒
     */
    @ApiModelProperty("总耗时")
    @JsonSerialize(using = DecimalFormatJsonSerializer.class)
    private Long totalTime;

    /**
     * 创建时间
     */
    @ApiModelProperty("创建时间")
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long createTime;

    /**
     * 作业执行类型
     */
    @ApiModelProperty("任务类型,0-作业执行,1-脚本执行,2-文件分发")
    private Integer type;

    @ApiModelProperty("任务类型描述")
    private String typeDesc;

    @ApiModelProperty("是否可以查看")
    private Boolean canView;

    @ApiModelProperty("是否可以执行")
    private Boolean canExecute;
}
