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

package com.tencent.bk.job.crontab.service;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.crontab.model.esb.v4.V4CronStatusEnum;
import com.tencent.bk.job.crontab.model.esb.v4.req.V4UpdateCronStatusRequest;

/**
 * v4 启停定时任务请求 -> 内部启停参数的转换。
 * <p>
 * 仓库里暂无 v4 直接启停定时任务接口，本转换器是从零新写的一份，参照 v3 的
 * EsbCronJobV3ResourceImpl.updateCronStatus，并按 v4 协议去掉了 bk_biz_id 兼容字段。
 * <p>
 * 校验与转换放在一起，供审批预检（dryRun）与放行执行共用，保证两次调用不产生行为漂移。
 * <p>
 * TODO 后续补齐 v4 直接启停定时任务接口后，v3 的校验逻辑应改为委托本转换器，合并为一份实现，
 * 避免 v3/v4 两份实现长期并存导致行为分叉。
 */
public class V4UpdateCronStatusRequestConverter {

    private V4UpdateCronStatusRequestConverter() {
    }

    /**
     * 校验请求并解析出目标启停状态
     *
     * @param request v4 请求
     * @return true 表示启动定时任务，false 表示暂停定时任务
     * @throws InvalidParamException 请求参数不合法
     */
    public static boolean convertToEnable(V4UpdateCronStatusRequest request) {
        validate(request);
        return V4CronStatusEnum.valOf(request.getStatus()).isEnabled();
    }

    public static void validate(V4UpdateCronStatusRequest request) {
        if (request.getId() == null || request.getId() <= 0) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"id", "id must be a positive integer"});
        }
        if (V4CronStatusEnum.valOf(request.getStatus()) == null) {
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"status", "status must be 1(enabled) or 0(disabled)"});
        }
    }
}
