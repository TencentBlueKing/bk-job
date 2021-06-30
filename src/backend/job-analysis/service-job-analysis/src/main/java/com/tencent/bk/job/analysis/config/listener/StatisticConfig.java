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

package com.tencent.bk.job.analysis.config.listener;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class StatisticConfig {

    @Value("${job.analysis.statistics.expire.enable:true}")
    private Boolean enableExpire;

    @Value("${job.analysis.statistics.expire.days:366}")
    private Integer expireDays;

    @Value("${job.analysis.statistics.frequency.hours:1}")
    private Integer intervalHours;

    @Value("${job.analysis.statistics.enable:true}")
    private Boolean enable;

    @Value("${job.analysis.statistics.mom.days:1}")
    private Integer momDays;

    @Value("${job.analysis.statistics.yoy.days:7}")
    private Integer yoyDays;

    @Value("${job.analysis.statistics.tag.num.max:100}")
    private Integer maxTagNum;

    /**
     * 过去多少天执行过任务的业务视为活跃
     */
    @Value("${job.analysis.app.active.days:3}")
    private Integer appActiveDays;
}
