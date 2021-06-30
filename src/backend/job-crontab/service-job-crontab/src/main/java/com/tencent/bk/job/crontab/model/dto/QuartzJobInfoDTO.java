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

package com.tencent.bk.job.crontab.model.dto;

import lombok.Data;
import org.quartz.JobDataMap;

/**
 * @since 28/2/2020 19:59
 */
@Data
public class QuartzJobInfoDTO {
    /**
     * 名称
     */
    private String name;

    /**
     * 分组
     */
    private String group;

    /**
     * 描述
     */
    private String description;

    /**
     * 定时表达式
     */
    private String cronExpression;

    /**
     * 时区
     */
    private Integer timeZone;

    /**
     * 执行时间
     */
    private Long executeTime;

    /**
     * 重复次数
     */
    private Integer repeatCount;

    /**
     * 重复间隔
     */
    private Integer repeatInterval;

    /**
     * 开始时间
     */
    private Long startAt;

    /**
     * 结束时间
     */
    private Long endAt;

    /**
     * 上次执行时间
     */
    private Long lastFiredTime;

    /**
     * 下次执行时间
     */
    private Long nextFiredTime;

    /**
     * 作业信息
     */
    private JobDataMap jobDataMap;

}
