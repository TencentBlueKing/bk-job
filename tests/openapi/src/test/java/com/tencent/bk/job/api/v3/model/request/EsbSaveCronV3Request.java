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

package com.tencent.bk.job.api.v3.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.api.model.EsbAppScopeReq;
import com.tencent.bk.job.api.v3.model.EsbGlobalVarV3DTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @since 26/2/2020 16:33
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EsbSaveCronV3Request extends EsbAppScopeReq {

    /**
     * 定时任务ID，更新定时任务时，必须传这个值
     */
    private Long id;

    /**
     * 要定时执行的作业的作业ID
     */
    @JsonProperty("job_plan_id")
    private Long planId;

    /**
     * 定时作业名称，新建时必填，修改时选填
     */
    private String name;

    /**
     * 定时任务的 cron 表达式
     * <p>
     * 新建时必填，修改时选填，各字段含义为：分 时 日 月 周，如: 0/5 * * * ? 表示每5分钟执行一次
     */
    @JsonProperty("expression")
    private String cronExpression;

    /**
     * 单次执行的指定执行时间（Unix时间戳）
     * <p>
     * 不可与 cronExpression 同时为空
     */
    @JsonProperty("execute_time")
    private Long executeTime;

    /**
     * 定时任务的变量信息
     */
    @JsonProperty("global_var_list")
    private List<EsbGlobalVarV3DTO> globalVarList;


}
