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

package com.tencent.bk.job.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class CompareUtilTest {

    @Test
    void testCompareVersion() {
        // 点分数字版本号
        assertThat(CompareUtil.compareVersion("", "3.3.3.2")).isEqualTo(-1);
        assertThat(CompareUtil.compareVersion("3.", "3.3.3.2")).isEqualTo(-1);
        assertThat(CompareUtil.compareVersion("3.3", "3.3.3.2")).isEqualTo(-1);
        assertThat(CompareUtil.compareVersion("3.3.3", "3.3.3.2")).isEqualTo(-1);
        assertThat(CompareUtil.compareVersion("3.3.3.", "3.3.3.2")).isEqualTo(-1);
        assertThat(CompareUtil.compareVersion("3.3.3.1", "3.3.3.2")).isEqualTo(-1);

        assertThat(CompareUtil.compareVersion("3.3.3.10", "3.3.3.2")).isEqualTo(1);
        assertThat(CompareUtil.compareVersion("3.4.3.1", "3.3.3.2")).isEqualTo(1);
        assertThat(CompareUtil.compareVersion("3.4.3", "3.3.3.2")).isEqualTo(1);
        assertThat(CompareUtil.compareVersion("3.4", "3.3.3.2")).isEqualTo(1);
        assertThat(CompareUtil.compareVersion("3.3.3.10", "3.3.3.2")).isEqualTo(1);

        assertThat(CompareUtil.compareVersion("", "")).isEqualTo(0);
        assertThat(CompareUtil.compareVersion("", " ")).isEqualTo(0);
        assertThat(CompareUtil.compareVersion("  ", " ")).isEqualTo(0);
        assertThat(CompareUtil.compareVersion("3.3.3.2", "3.3.3.2")).isEqualTo(0);
        // 语义化版本号
        assertThat(CompareUtil.compareVersion("", "3.3.3.2")).isEqualTo(-1);
        assertThat(CompareUtil.compareVersion("", "3.3.3.alpha")).isEqualTo(-1);
        assertThat(CompareUtil.compareVersion("", "3.3.3.alpha.1")).isEqualTo(-1);
        assertThat(CompareUtil.compareVersion("", "3.3.3.beta")).isEqualTo(-1);
        assertThat(CompareUtil.compareVersion("3.3.3.alpha", "3.3.3.beta")).isEqualTo(-1);
        assertThat(CompareUtil.compareVersion("3.3.3.alpha", "3.3.3.rc")).isEqualTo(-1);
        assertThat(CompareUtil.compareVersion("3.3.3.alpha", "3.3.3.rc.1")).isEqualTo(-1);
        assertThat(CompareUtil.compareVersion("3.3.3.beta.10", "3.3.3.rc")).isEqualTo(-1);
        assertThat(CompareUtil.compareVersion("3.3.3.rc", "3.3.3.0")).isEqualTo(-1);
        assertThat(CompareUtil.compareVersion("3.3.3.rc.2", "3.3.3.0")).isEqualTo(-1);

        assertThat(CompareUtil.compareVersion("3.3.3.10", "3.3.3.2")).isEqualTo(1);
        assertThat(CompareUtil.compareVersion("3.3.3.alpha.1", "3.3.3.alpha")).isEqualTo(1);
        assertThat(CompareUtil.compareVersion("3.3.3.alpha.2", "3.3.3.alpha.1")).isEqualTo(1);
        assertThat(CompareUtil.compareVersion("3.3.3.alpha.10", "3.3.3.alpha.2")).isEqualTo(1);
        assertThat(CompareUtil.compareVersion("3.3.3.beta.1", "3.3.3.alpha.2")).isEqualTo(1);
        assertThat(CompareUtil.compareVersion("3.3.3.1", "3.3.3.alpha")).isEqualTo(1);
        assertThat(CompareUtil.compareVersion("3.3.3.1", "3.3.3.alpha.1")).isEqualTo(1);
        assertThat(CompareUtil.compareVersion("3.3.3.1", "3.3.3.beta")).isEqualTo(1);
        assertThat(CompareUtil.compareVersion("3.3.3.1", "3.3.3.beta.1")).isEqualTo(1);
        assertThat(CompareUtil.compareVersion("3.3.3.1", "3.3.3.rc.1")).isEqualTo(1);
        assertThat(CompareUtil.compareVersion("3.3.3-alpha.1", "3.3.3-alpha")).isEqualTo(1);
        assertThat(CompareUtil.compareVersion("3.3.3.alpha.2", "3.3.3-alpha.1")).isEqualTo(1);
        assertThat(CompareUtil.compareVersion("3.3.3-alpha.2", "3.3.3.alpha.1")).isEqualTo(1);

        assertThat(CompareUtil.compareVersion("3.3.3-alpha", "3.3.3-alpha")).isEqualTo(0);
        assertThat(CompareUtil.compareVersion("3.3.3.beta.10", "3.3.3-beta.10")).isEqualTo(0);
        assertThat(CompareUtil.compareVersion("3.3.3-rc.1", "3.3.3-rc.1 ")).isEqualTo(0);
        assertThat(CompareUtil.compareVersion("3.3.3-alpha.1", "3.3.3-alpha.1")).isEqualTo(0);
        assertThat(CompareUtil.compareVersion("3.3.3.alpha", "3.3.3.alpha")).isEqualTo(0);
        assertThat(CompareUtil.compareVersion("3.3.3.alpha.1", "3.3.3.alpha.1")).isEqualTo(0);
    }
}
