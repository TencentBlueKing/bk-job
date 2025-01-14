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

/**
 * 添加任务到Quartz的结果
 */
@Data
public class AddJobToQuartzResult {
    /**
     * 定时任务基本信息
     */
    private CronJobBasicInfoDTO cronJobBasicInfo;
    /**
     * 是否成功
     */
    private boolean success;
    /**
     * 提示信息
     */
    private String message;
    /**
     * 异常信息
     */
    private Exception exception;

    /**
     * 构造失败结果
     *
     * @param cronJobBasicInfo 定时任务基本信息
     * @param message          提示信息
     * @return 失败结果
     */
    public static AddJobToQuartzResult failResult(CronJobBasicInfoDTO cronJobBasicInfo, String message) {
        AddJobToQuartzResult result = new AddJobToQuartzResult();
        result.setCronJobBasicInfo(cronJobBasicInfo);
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

    /**
     * 构造失败结果
     *
     * @param cronJobBasicInfo 定时任务基本信息
     * @param message          提示信息
     * @param exception        异常信息
     * @return 失败结果
     */
    public static AddJobToQuartzResult failResult(CronJobBasicInfoDTO cronJobBasicInfo,
                                                  String message,
                                                  Exception exception) {
        AddJobToQuartzResult result = new AddJobToQuartzResult();
        result.setCronJobBasicInfo(cronJobBasicInfo);
        result.setSuccess(false);
        result.setMessage(message);
        result.setException(exception);
        return result;
    }

    /**
     * 构造成功结果
     *
     * @param cronJobBasicInfo 定时任务基本信息
     * @return 成功结果
     */
    public static AddJobToQuartzResult successResult(CronJobBasicInfoDTO cronJobBasicInfo) {
        AddJobToQuartzResult result = new AddJobToQuartzResult();
        result.setCronJobBasicInfo(cronJobBasicInfo);
        result.setSuccess(true);
        return result;
    }
}
