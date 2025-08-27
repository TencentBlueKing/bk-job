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

package com.tencent.bk.job.crontab.model.esb.v3.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.tencent.bk.job.common.esb.model.EsbAppScopeDTO;
import com.tencent.bk.job.common.esb.model.job.v3.EsbGlobalVarV3DTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * 定时任务
 */
@Getter
@Setter
@ToString
public class EsbCronInfoV3DTO extends EsbAppScopeDTO {

    /**
     * 作业模板 ID
     */
    @JsonProperty("job_plan_id")
    @JsonPropertyDescription("Job plan id")
    private Long planId;

    /**
     * 定时作业ID
     */
    @JsonPropertyDescription("Cron job id")
    private Long id;

    /**
     * 定时作业名称
     */
    @JsonPropertyDescription("Cron job name")
    private String name;

    /**
     * 定时作业状态：1.已启动、2.已暂停
     */
    @JsonPropertyDescription("Cron job status, 1 - Started, 2 - Stopped")
    private Integer status;

    /**
     * 定时任务的 cron 表达式
     */
    @JsonProperty("expression")
    @JsonPropertyDescription("Cron job expression")
    private String cronExpression;

    /**
     * 定时任务的变量信息
     */
    @JsonProperty("global_var_list")
    @JsonPropertyDescription("Global variables")
    private List<EsbGlobalVarV3DTO> globalVarList;

    /**
     * 作业创建人帐号
     */
    @JsonPropertyDescription("Creator")
    private String creator;

    /**
     * 创建时间，毫秒时间戳
     */
    @JsonProperty("create_time")
    @JsonPropertyDescription("Create time")
    private Long createTime;

    /**
     * 作业修改人帐号
     */
    @JsonProperty("last_modify_user")
    @JsonPropertyDescription("Last modify user")
    private String lastModifyUser;

    /**
     * 最后修改时间，毫秒时间戳
     */
    @JsonProperty("last_modify_time")
    @JsonPropertyDescription("Last modify time")
    private Long lastModifyTime;

}
