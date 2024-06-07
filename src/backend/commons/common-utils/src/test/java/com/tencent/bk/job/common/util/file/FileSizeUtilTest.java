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

package com.tencent.bk.job.common.util.file;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

public class FileSizeUtilTest {
    @Test
    void testGetFileSizeStr() {
        assertThat(FileSizeUtil.getFileSizeStr(null)).isEqualTo("--");
        assertThat(FileSizeUtil.getFileSizeStr(0L)).isEqualTo("0.00B");
        assertThat(FileSizeUtil.getFileSizeStr(1023L)).isEqualTo("1023.00B");
        assertThat(FileSizeUtil.getFileSizeStr(1024L)).isEqualTo("1.00KB");
        assertThat(FileSizeUtil.getFileSizeStr(1024 * 1024L)).isEqualTo("1.00MB");
        assertThat(FileSizeUtil.getFileSizeStr(1024 * 1024L - 1)).isEqualTo("1024.00KB");
        assertThat(FileSizeUtil.getFileSizeStr(1024 * 1024 * 1024L)).isEqualTo("1.00GB");
        assertThat(FileSizeUtil.getFileSizeStr(1024 * 1024 * 1024 * 1024L)).isEqualTo("1.00TB");
        assertThat(FileSizeUtil.getFileSizeStr(1024 * 1024 * 1024 * 1024L * 1024L)).isEqualTo("1.00PB");
        assertThat(FileSizeUtil.getFileSizeStr(1025 * 1024 * 1024 * 1024L * 1024L * 1024L)).isEqualTo("1025.00PB");
    }

    @Test
    void testParseFileSizeBytes() {
        assertThat(FileSizeUtil.parseFileSizeBytes("1B")).isEqualTo(1L);
        assertThat(FileSizeUtil.parseFileSizeBytes("1KB")).isEqualTo(1024L);
        assertThat(FileSizeUtil.parseFileSizeBytes("1.5KB")).isEqualTo(1536L);
        assertThat(FileSizeUtil.parseFileSizeBytes("1MB")).isEqualTo(1024L * 1024L);
        assertThat(FileSizeUtil.parseFileSizeBytes("1GB")).isEqualTo(1024L * 1024L * 1024L);
        assertThat(FileSizeUtil.parseFileSizeBytes("1TB")).isEqualTo(1024L * 1024L * 1024L * 1024L);
        assertThat(FileSizeUtil.parseFileSizeBytes("1PB")).isEqualTo(1024L * 1024L * 1024L * 1024L * 1024L);
        assertThatThrownBy(() ->
            FileSizeUtil.parseFileSizeBytes("1XB")
        ).isInstanceOf(IllegalArgumentException.class);
    }
}
