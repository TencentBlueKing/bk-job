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

package com.tencent.bk.job.crontab.model.esb.v3.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.model.EsbReq;
import com.tencent.bk.job.common.esb.model.job.v3.EsbGlobalVarV3DTO;
import com.tencent.bk.job.common.exception.InvalidParamException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @since 26/2/2020 16:33
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EsbSaveCronV3Request extends EsbReq {

    /**
     * 业务 ID
     */
    @JsonProperty("bk_biz_id")
    private Long appId;

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

    public boolean validate() {
        if (appId == null || appId <= 0) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"bk_biz_id", "bk_biz_id must be a positive long number"});
        }
        if (id != null && id < 0) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"id", "id must be a positive long number when updating"});
        }
        if (id == null || id == 0) {
            if (planId == null || planId <= 0) {
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                    new String[]{"job_plan_id", "job_plan_id must be a positive long number"});
            }
            if (StringUtils.isBlank(name)) {
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                    new String[]{"name", "name cannot be null or blank when create"});
            }
            if (StringUtils.isBlank(cronExpression) && (executeTime == null || executeTime <= 0)) {
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                    new String[]{"expression/execute_time", "expression/execute_time cannot both be null or invalid"});
            }
        } else {
            boolean hasChange = false;
            if (planId != null && planId > 0) {
                hasChange = true;
            }
            if (StringUtils.isNotBlank(name)) {
                hasChange = true;
            } else {
                name = null;
            }
            if (StringUtils.isNotBlank(cronExpression)) {
                hasChange = true;
            } else {
                cronExpression = null;
            }
            if (executeTime != null && executeTime > 0) {
                hasChange = true;
            } else {
                executeTime = null;
            }
            if (!hasChange) {
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                    new String[]{"job_plan_id/name/expression/execute_time",
                        "At least one of job_plan_id/name/expression/execute_time must be given to update cron " + id
                    });
            }
        }
        return true;
    }
}
