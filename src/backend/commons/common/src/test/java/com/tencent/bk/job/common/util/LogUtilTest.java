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
    void testTailLog() {
        // null 原样返回
        assertThat(LogUtil.tailLog(null, 10)).isNull();

        // 空串不截断
        assertThat(LogUtil.tailLog("", 10)).isEqualTo("");

        // 未超限，原样返回
        assertThat(LogUtil.tailLog("abc", 10)).isEqualTo("abc");
        assertThat(LogUtil.tailLog("1234567890", 10)).isEqualTo("1234567890");

        // 超限，无换行 → 直接返回末尾截取
        assertThat(LogUtil.tailLog("abcdefghijk", 5)).isEqualTo("ghijk");

        // 超限，有换行 → 去掉首个不完整行
        // "line1\nline2\nline3" 长度17，取末尾10个字符 = "e2\nline3\n" 不对...
        // 让我构造精确的用例
        String text = "AAA\nBBB\nCCC\nDDD\nEEE";
        // 长度: A(0)A(1)A(2)\n(3)B(4)B(5)B(6)\n(7)C(8)C(9)C(10)\n(11)D(12)D(13)D(14)\n(15)E(16)E(17)E(18) = 19 chars
        // tailLog(text, 10) → 取末尾10: "C\nDDD\nEEE", 第一个\n在index=1 → 去掉"C\n" → "DDD\nEEE"
        assertThat(LogUtil.tailLog(text, 10)).isEqualTo("DDD\nEEE");

        // 超限，截取点恰好在行首 → 第一个字符就是\n → 去掉它，保留后面完整内容
        // "XX\nYYYY" 长度7，取末尾5: "\nYYYY", firstNewline=0 → 去掉"\n" → "YYYY"
        assertThat(LogUtil.tailLog("XX\nYYYY", 5)).isEqualTo("YYYY");

        // 超限，截取尾部只有一行（无换行符） → 直接返回
        assertThat(LogUtil.tailLog("first\nsecondlong", 10)).isEqualTo("secondlong");

        // 超限，截取尾部换行在最后一个字符 → 不满足 firstNewline < tail.length()-1 → 直接返回
        String text2 = "AAABBB\n";
        // 长度7，取末尾5: "BBB\n" → wait that's 4 chars. Let me recalculate
        // "AAABBB\n" = 7 chars, tailLog(text2, 5) → 取末尾5: "ABBB\n"
        // firstNewline = 4 (最后一个字符), tail.length()-1 = 4, 条件 4 < 4 不成立 → 返回 "ABBB\n"
        assertThat(LogUtil.tailLog("AAABBB\n", 5)).isEqualTo("ABBB\n");
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
