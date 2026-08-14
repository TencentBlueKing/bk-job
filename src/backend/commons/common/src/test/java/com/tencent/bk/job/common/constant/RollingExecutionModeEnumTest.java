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

package com.tencent.bk.job.common.constant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("滚动批次执行模式枚举")
class RollingExecutionModeEnumTest {

    @Test
    @DisplayName("valOf 合法值")
    void valOf() {
        assertThat(RollingExecutionModeEnum.valOf(1)).isEqualTo(RollingExecutionModeEnum.SERIAL);
        assertThat(RollingExecutionModeEnum.valOf(2)).isEqualTo(RollingExecutionModeEnum.PARALLEL);
    }

    @Test
    @DisplayName("isValid 判断")
    void isValid() {
        assertThat(RollingExecutionModeEnum.isValid(1)).isTrue();
        assertThat(RollingExecutionModeEnum.isValid(2)).isTrue();
        assertThat(RollingExecutionModeEnum.isValid(0)).isFalse();
        assertThat(RollingExecutionModeEnum.isValid(3)).isFalse();
        assertThat(RollingExecutionModeEnum.isValid(null)).isFalse();
    }

    @Test
    @DisplayName("isParallel 判断")
    void isParallel() {
        assertThat(RollingExecutionModeEnum.isParallel(2)).isTrue();
        assertThat(RollingExecutionModeEnum.isParallel(1)).isFalse();
        assertThat(RollingExecutionModeEnum.isParallel(null)).isFalse();
    }

    @Test
    @DisplayName("getValue 与序号对应")
    void getValue() {
        assertThat(RollingExecutionModeEnum.SERIAL.getValue()).isEqualTo(1);
        assertThat(RollingExecutionModeEnum.PARALLEL.getValue()).isEqualTo(2);
    }
}
