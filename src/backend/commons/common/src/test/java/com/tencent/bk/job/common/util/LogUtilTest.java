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

package com.tencent.bk.job.common.util;

import com.tencent.bk.job.common.util.ip.IpUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class LogUtilTest {

    @Test
    void testStripControlChars() {
        // null 返回空串
        assertThat(LogUtil.stripControlChars(null)).isEqualTo("");
        // 空串不变
        assertThat(LogUtil.stripControlChars("")).isEqualTo("");
        // 普通字符串不受影响
        assertThat(LogUtil.stripControlChars("hello world")).isEqualTo("hello world");
        // 移除换行、回车
        assertThat(LogUtil.stripControlChars("line1\r\nline2")).isEqualTo("line1line2");
        assertThat(LogUtil.stripControlChars("line1\nline2")).isEqualTo("line1line2");
        // 移除制表符
        assertThat(LogUtil.stripControlChars("col1\tcol2")).isEqualTo("col1col2");
        // 移除 NULL 字符
        assertThat(LogUtil.stripControlChars("ab\0cd")).isEqualTo("abcd");
        // 移除 DEL (0x7F)
        assertThat(LogUtil.stripControlChars("ab\u007Fcd")).isEqualTo("abcd");
        // 保留中文和可见特殊字符
        assertThat(LogUtil.stripControlChars("你好！@#$%")).isEqualTo("你好！@#$%");
        // CRLF 注入场景
        assertThat(LogUtil.stripControlChars("Mozilla/5.0\r\nX-Injected: evil"))
            .isEqualTo("Mozilla/5.0X-Injected: evil");
    }

    @Test
    void testSanitizeForLog() {
        // null 返回空串
        assertThat(LogUtil.sanitizeForLog(null, 10)).isEqualTo("");
        // 短于限制不截断
        assertThat(LogUtil.sanitizeForLog("short", 10)).isEqualTo("short");
        // 恰好等于限制不截断
        assertThat(LogUtil.sanitizeForLog("1234567890", 10)).isEqualTo("1234567890");
        // 超出限制截断并追加 "..."
        assertThat(LogUtil.sanitizeForLog("12345678901", 10)).isEqualTo("1234567890...");
        // 截断在控制字符清理之前执行：前5个原始字符为 a,b,c,\r,\n，清理后为 "abc..."
        assertThat(LogUtil.sanitizeForLog("abc\r\ndef\tghi", 5)).isEqualTo("abc...");
        // 控制字符在截断范围内被清理
        assertThat(LogUtil.sanitizeForLog("a\nb\nc", 100)).isEqualTo("abc");
    }

    @Test
    void testBuildListLog() {
        assertThat(LogUtil.buildListLog(null, 1)).isEqualTo("");
        List<String> list = new ArrayList<>();
        assertThat(LogUtil.buildListLog(list, 1)).isEqualTo("[]");
        list.add("1");
        assertThat(LogUtil.buildListLog(list, 0)).isEqualTo("[]");
        assertThat(LogUtil.buildListLog(list, -1)).isEqualTo("[1]");
        assertThat(LogUtil.buildListLog(list, 1)).isEqualTo("[1]");
        assertThat(LogUtil.buildListLog(list, 2)).isEqualTo("[1]");
        list.add("2");
        list.add("3");
        assertThat(LogUtil.buildListLog(list, 2)).isEqualTo("(3 elements)[1,2,...]");
    }

    @Test
    void testLong2Ip() {
        long ipLong = 2130706433L;
        String ip = IpUtils.revertIpFromLong(ipLong);
        assertThat(ip).isEqualTo("127.0.0.1");
    }

    @Test
    void testIp2Long() {
        String ip = "127.0.0.1";
        long ipLong = IpUtils.getStringIpToLong(ip);
        assertThat(ipLong).isEqualTo(2130706433L);
    }

    @Test
    void testLongStr2Ip() {
        String ipLongStr = "2130706433";
        String ip = IpUtils.revertIpFromNumericalStr(ipLongStr);
        assertThat(ip).isEqualTo("127.0.0.1");
    }
}
