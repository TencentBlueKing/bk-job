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

package com.tencent.bk.job.execute.model.esb.v3.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * get_job_instance_list, 查询作业执行历史
 */
@Getter
@Setter
@ToString
public class EsbGetJobInstanceListV3Request extends EsbAppScopeReq {

    /**
     * 作业执行实例 ID
     */
    @JsonProperty("job_instance_id")
    private Long taskInstanceId;

    @JsonProperty("create_time_start")
    private Long createTimeStart;

    @JsonProperty("create_time_end")
    private Long createTimeEnd;

    /**
     * 定时任务ID
     */
    @JsonProperty("job_cron_id")
    private Long cronId;

    private String operator;

    /**
     * 任务名称
     */
    @JsonProperty("name")
    private String taskName;

    /**
     * 执行方式
     */
    @JsonProperty("launch_mode")
    private Integer startupMode;

    /**
     * 任务类型
     */
    @JsonProperty("type")
    private Integer taskType;

    /**
     * 任务状态
     */
    @JsonProperty("status")
    private Integer taskStatus;

    /**
     * 执行目标服务器IP
     */
    @JsonProperty("ip")
    private String ip;

    /**
     * 分页返回记录起始位置
     */
    private Integer start;

    /**
     * 返回记录数量
     */
    private Integer length;
}
