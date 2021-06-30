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

package com.tencent.bk.job.crontab.model.inner.request;

import com.tencent.bk.job.common.util.date.DateUtils;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * @since 18/2/2020 15:53
 */
@Data
public class ServiceAddInnerCronJobRequestDTO {

    /**
     * 系统标识，取自 path variable
     * <p>
     * 仅可用英文字母、数字、- _
     */
    private String systemId;

    /**
     * 作业唯一 key，取自 path variable
     * <p>
     * 同系统下必须唯一，仅可用英文字母、数字、- _
     */
    private String jobKey;

    /**
     * 任务描述
     * <p>
     * 非必填
     */
    private String description;

    /**
     * 定时表达式
     * <p>
     * 非必填，但不可与 executeTime 同时为空
     */
    private String cronExpression;

    /**
     * 时区，默认为 +8 时区
     */
    private Integer timeZone;

    /**
     * 执行时间
     * <p>
     * 非必填，但不可与 cronExpression 同时为空
     */
    private Long executeTime;

    /**
     * 重复次数
     * <p>
     * 不填默认只执行一次，与 executeTime 配合
     */
    private Integer repeatCount;

    /**
     * 重复间隔
     * <p>
     * 与 repeatCount 配合生效，单位秒
     */
    private Integer repeatInterval;

    /**
     * 开始时间
     * <p>
     * 不填默认无
     */
    private Long startAt;

    /**
     * 结束时间
     * <p>
     * 不填默认无
     */
    private Long endAt;

    /**
     * 回调 uri
     * <p>
     * 相对服务的 uri，不需要完整路径
     */
    private String callbackUri;

    /**
     * 回调 POST 请求体
     * <p>
     * 为空则发 GET 请求
     */
    private String callbackBody;

    /**
     * 回调失败时的执行次数
     */
    private Integer retryCount;

    public boolean validate() {
        if (StringUtils.isBlank(systemId) || StringUtils.isBlank(jobKey)) {
            return false;
        }
        if (StringUtils.isNotBlank(cronExpression)) {
            executeTime = 0L;
        } else if (executeTime > DateUtils.currentTimeSeconds()) {
            cronExpression = null;
        } else {
            return false;
        }
        if (StringUtils.isBlank(callbackUri)) {
            return false;
        } else {
            if (!callbackUri.startsWith("/")) {
                callbackUri = "/" + callbackUri;
            }
        }
        return true;

    }
}
