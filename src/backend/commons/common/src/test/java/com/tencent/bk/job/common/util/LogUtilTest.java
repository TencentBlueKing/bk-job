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

import com.tencent.bk.job.common.util.ip.IpUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class LogUtilTest {
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
        String ip = IpUtils.revertIpFromLongStr(ipLongStr);
        assertThat(ip).isEqualTo("127.0.0.1");
    }
}
