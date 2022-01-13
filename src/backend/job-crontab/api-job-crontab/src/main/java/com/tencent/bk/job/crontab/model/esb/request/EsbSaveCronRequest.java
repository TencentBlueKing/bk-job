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

package com.tencent.bk.job.crontab.model.esb.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.model.EsbReq;
import com.tencent.bk.job.common.exception.InvalidParamException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;

/**
 * @since 26/2/2020 16:33
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class EsbSaveCronRequest extends EsbReq {

    /**
     * 业务 ID
     */
    @JsonProperty("bk_biz_id")
    private Long appId;

    /**
     * 定时任务ID，更新定时任务时，必须传这个值
     */
    @JsonProperty("cron_id")
    private Long id;

    /**
     * 要定时执行的作业的作业ID
     */
    @JsonProperty("bk_job_id")
    private Long planId;

    /**
     * 定时作业名称，新建时必填，修改时选填
     */
    @JsonProperty("cron_name")
    private String name;

    /**
     * 定时任务的 cron 表达式
     * <p>
     * 新建时必填，修改时选填，各字段含义为：分 时 日 月 周，如: 0/5 * * * ? 表示每5分钟执行一次
     */
    @JsonProperty("cron_expression")
    private String cronExpression;

    private boolean hasChange() {
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
        return hasChange;
    }

    public void validate() {
        if (appId == null || appId <= 0) {
            throw new InvalidParamException(
                ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new Object[]{
                    "bk_biz_id",
                    "bk_biz_id must be a positive number"
                });
        }
        if (id != null && id <= 0) {
            throw new InvalidParamException(
                ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new Object[]{
                    "id",
                    "id must be a positive number"
                });
        }
        if (id == null) {
            if (planId == null || planId <= 0) {
                throw new InvalidParamException(
                    ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                    new Object[]{
                        "bk_job_id",
                        "bk_job_id must be a positive number"
                    });
            }
            if (StringUtils.isBlank(name)) {
                throw new InvalidParamException(
                    ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                    new Object[]{
                        "cron_name",
                        "cron_name cannot be blank"
                    });
            }
            if (StringUtils.isBlank(cronExpression)) {
                throw new InvalidParamException(
                    ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                    new Object[]{
                        "cron_expression",
                        "cron_expression cannot be blank"
                    });
            }
        } else {

            if (!hasChange()) {
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                    new String[]{"bk_job_id/cron_name/cron_expression",
                        "At least one of bk_job_id/cron_name/cron_expression must be given to update cron " + id
                    });
            }
        }
    }
}
