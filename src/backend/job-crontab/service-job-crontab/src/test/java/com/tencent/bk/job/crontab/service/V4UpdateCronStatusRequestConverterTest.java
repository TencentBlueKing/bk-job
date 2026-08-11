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

import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.crontab.common.constants.CronStatusEnum;
import com.tencent.bk.job.crontab.model.esb.v4.req.V4UpdateCronStatusRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * v4 启停定时任务请求转换的字段级对账单测。
 */
class V4UpdateCronStatusRequestConverterTest {

    @Test
    @DisplayName("状态 1 转换为启动，状态 2 转换为暂停")
    void convertStatusToEnable() {
        assertThat(V4UpdateCronStatusRequestConverter.convertToEnable(
            request(1L, CronStatusEnum.RUNNING.getStatus()))).isTrue();
        assertThat(V4UpdateCronStatusRequestConverter.convertToEnable(
            request(1L, CronStatusEnum.STOPPING.getStatus()))).isFalse();
    }

    @Test
    @DisplayName("定时任务 ID 非法时判为非法参数")
    void validateId() {
        assertThatThrownBy(() -> V4UpdateCronStatusRequestConverter.convertToEnable(request(null, 1)))
            .isInstanceOf(InvalidParamException.class);
        assertThatThrownBy(() -> V4UpdateCronStatusRequestConverter.convertToEnable(request(0L, 1)))
            .isInstanceOf(InvalidParamException.class);
    }

    @Test
    @DisplayName("状态取值不在枚举内时判为非法参数")
    void validateStatus() {
        assertThatThrownBy(() -> V4UpdateCronStatusRequestConverter.convertToEnable(request(1L, null)))
            .isInstanceOf(InvalidParamException.class);
        assertThatThrownBy(() -> V4UpdateCronStatusRequestConverter.convertToEnable(request(1L, 3)))
            .isInstanceOf(InvalidParamException.class);
    }

    private V4UpdateCronStatusRequest request(Long id, Integer status) {
        V4UpdateCronStatusRequest request = new V4UpdateCronStatusRequest();
        request.setAppId(2L);
        request.setId(id);
        request.setStatus(status);
        return request;
    }
}
