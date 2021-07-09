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

package com.tencent.bk.job.execute.engine.util;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

public class MacroUtilTest {
    @Test
    void testResolveDate() {
        String str = "/tmp/[DATE:yyyy-MM-dd]/test/";
        // 2021-01-01 12:01:01
        long timestamp = 1609473661000L;
        ZoneId zoneId = ZoneOffset.systemDefault();
        String zoneIdStr = zoneId.getId();
        System.out.println("systemDefault zoneId=" + zoneIdStr);
        String result = null;
        if ("Asia/Shanghai".equals(zoneIdStr)) {
            result = MacroUtil.resolveDate(str, timestamp);
            assertThat(result).isEqualTo("/tmp/2021-01-01/test/");

            str = "/tmp/[DATE:YYYY-MM-dd]/test/";
            result = MacroUtil.resolveDate(str, timestamp);
            assertThat(result).isEqualTo("/tmp/2021-01-01/test/");

            str = "/tmp/[DATE:yyyy-MM-dd_HH:mm:ss]/test/";
            result = MacroUtil.resolveDate(str, timestamp);
            assertThat(result).isEqualTo("/tmp/2021-01-01_12:01:01/test/");

            str = "/tmp/[DATE:yyyy-MM-dd]/[DATE:HH:mm:ss]/test/";
            result = MacroUtil.resolveDate(str, timestamp);
            assertThat(result).isEqualTo("/tmp/2021-01-01/12:01:01/test/");
        }
        str = "/tmp/test/";
        result = MacroUtil.resolveDate(str, timestamp);
        assertThat(result).isEqualTo("/tmp/test/");

        str = "/tmp/test";
        result = MacroUtil.resolveDate(str, timestamp);
        assertThat(result).isEqualTo("/tmp/test");

        str = "/tmp/test/[";
        result = MacroUtil.resolveDate(str, timestamp);
        assertThat(result).isEqualTo("/tmp/test/[");

        str = "/tmp/test/[DATE:]";
        result = MacroUtil.resolveDate(str, timestamp);
        assertThat(result).isEqualTo("/tmp/test/[DATE:]");

        str = "/tmp/test/[DATE:PP-MM-dd]";
        result = MacroUtil.resolveDate(str, timestamp);
        assertThat(result).isEqualTo("/tmp/test/[DATE:PP-MM-dd]");
    }
}
