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

package com.tencent.bk.job.analysis.service;

import com.tencent.bk.job.analysis.consts.DistributionMetricEnum;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.statistics.consts.StatisticsConstants;
import org.springframework.stereotype.Service;

@Service
public class MetricResourceReslover {
    public String resloveResource(DistributionMetricEnum metric) {
        if (DistributionMetricEnum.HOST_SYSTEM_TYPE == metric) {
            return StatisticsConstants.RESOURCE_HOST_OF_ALL_APP;
        } else if (DistributionMetricEnum.STEP_TYPE == metric) {
            return StatisticsConstants.RESOURCE_TASK_TEMPLATE_STEP;
        } else if (DistributionMetricEnum.SCRIPT_TYPE == metric) {
            return StatisticsConstants.RESOURCE_SCRIPT;
        } else if (DistributionMetricEnum.SCRIPT_VERSION_STATUS == metric) {
            return StatisticsConstants.RESOURCE_SCRIPT_VERSION;
        } else if (DistributionMetricEnum.CRON_STATUS == metric) {
            return StatisticsConstants.RESOURCE_CRON;
        } else if (DistributionMetricEnum.CRON_TYPE == metric) {
            return StatisticsConstants.RESOURCE_CRON;
        } else if (DistributionMetricEnum.TAG == metric) {
            return StatisticsConstants.RESOURCE_TAG;
        } else if (DistributionMetricEnum.ACCOUNT_TYPE == metric) {
            return StatisticsConstants.RESOURCE_ACCOUNT_OF_ALL_APP;
        } else {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, new String[]{"metric"});
        }
    }
}
