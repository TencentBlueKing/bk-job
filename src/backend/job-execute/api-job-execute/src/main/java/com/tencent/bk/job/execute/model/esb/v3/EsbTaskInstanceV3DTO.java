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

package com.tencent.bk.job.execute.model.esb.v3;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EsbTaskInstanceV3DTO {
    /**
     * id
     */
    @JsonProperty("job_instance_id")
    private Long id;

    /**
     * 执行方案id
     */
    @JsonProperty("job_plan_id")
    private Long taskId;

    /**
     * 作业模板id
     */
    @JsonProperty("job_template_id")
    private Long templateId;

    /**
     * 业务id
     */
    @JsonProperty("bk_biz_id")
    private Long appId;

    /**
     * 名称
     */
    private String name;

    /**
     * 执行人
     */
    private String operator;

    /**
     * 启动方式，1-页面执行、2-API调用、3-定时执行
     */
    @JsonProperty("launch_mode")
    private Integer startupMode;

    /**
     * 任务状态。1 -  等待执行，2 - 正在执行，3 - 执行成功，4 - 执行失败，7 - 等待确认，10 - 强制终止中，11 - 强制终止成功，13 - 确认终止
     */
    @JsonProperty("status")
    private Integer status;

    /**
     * 开始时间
     */
    @JsonProperty("start_time")
    private Long startTime;

    /**
     * 开始时间
     */
    @JsonProperty("end_time")
    private Long endTime;

    /**
     * 总耗时，单位：毫秒
     */
    @JsonProperty("total_time")
    private Long totalTime;

    /**
     * 创建时间
     */
    @JsonProperty("create_time")
    private Long createTime;

    /**
     * 任务类型,0-作业执行,1-脚本执行,2-文件分发
     */
    @JsonProperty("type")
    private Integer type;
}
