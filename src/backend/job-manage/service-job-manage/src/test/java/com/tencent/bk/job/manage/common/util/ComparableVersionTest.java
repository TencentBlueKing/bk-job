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

package com.tencent.bk.job.manage.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

/**
 * 版本号大小比较测试
 */
public class ComparableVersionTest {

    @Test
    @DisplayName("testComparableVersion")
    public void testComparableVersion() {
        assertThat(new VersionComparatorUtil("3.3.8")
            .compareTo(new VersionComparatorUtil("3.6.1")))
            .isEqualTo(-1);
        assertThat(new VersionComparatorUtil("3.3.8.11")
            .compareTo(new VersionComparatorUtil("3.3.8.12")))
            .isEqualTo(-1);
        assertThat(new VersionComparatorUtil("v3.3.8.15")
            .compareTo(new VersionComparatorUtil("v3.3.8.12")))
            .isEqualTo(1);
        assertThat(new VersionComparatorUtil("3.3.8.11")
            .compareTo(new VersionComparatorUtil("3.3.8.12")))
            .isEqualTo(-1);
        assertThat(new VersionComparatorUtil("3.3.8.12")
            .compareTo(new VersionComparatorUtil("3.3.8.12")))
            .isEqualTo(0);
        assertThat(new VersionComparatorUtil("3.6.5-beta.1")
            .compareTo(new VersionComparatorUtil("3.6.5-beta.2")))
            .isEqualTo(-1);
        assertThat(new VersionComparatorUtil("v3.6.5-beta.1")
            .compareTo(new VersionComparatorUtil("3.6.5-alpha.1")))
            .isEqualTo(1);
        assertThat(new VersionComparatorUtil("3.6.5-alpha.5")
            .compareTo(new VersionComparatorUtil("3.6.5")))
            .isEqualTo(-1);
        assertThat(new VersionComparatorUtil("3.6.2-rc.3-all")
            .compareTo(new VersionComparatorUtil("3.6.2-rc.2-all")))
            .isEqualTo(1);

    }
}
