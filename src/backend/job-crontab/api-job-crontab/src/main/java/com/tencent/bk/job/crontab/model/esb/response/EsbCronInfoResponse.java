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

package com.tencent.bk.job.crontab.model.esb.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @since 26/2/2020 20:58
 */
@Data
public class EsbCronInfoResponse {

    /**
     * 业务ID
     */
    @JsonProperty("bk_biz_id")
    private Long appId;

    /**
     * 作业模板 ID
     */
    @JsonProperty("bk_job_id")
    private Long planId;

    /**
     * 作业名称
     */
    @JsonProperty("job_name")
    private String planName;

    /**
     * 定时作业ID
     */
    @JsonProperty("cron_id")
    private Long id;

    /**
     * 定时作业名称
     */
    @JsonProperty("cron_name")
    private String name;

    /**
     * 定时作业状态：1.已启动、2.已暂停
     */
    @JsonProperty("cron_status")
    private Integer status;

    /**
     * 定时任务的 cron 表达式
     */
    @JsonProperty("cron_expression")
    private String cronExpression;

    /**
     * 作业创建人帐号
     */
    @JsonProperty("creator")
    private String creator;

    /**
     * 创建时间，YYYY-MM-DD HH:mm:ss格式
     */
    @JsonProperty("create_time")
    private String createTime;

    /**
     * 作业修改人帐号
     */
    @JsonProperty("last_modify_user")
    private String lastModifyUser;

    /**
     * 最后修改时间，YYYY-MM-DD HH:mm:ss格式
     */
    @JsonProperty("last_modify_time")
    private String lastModifyTime;

}
