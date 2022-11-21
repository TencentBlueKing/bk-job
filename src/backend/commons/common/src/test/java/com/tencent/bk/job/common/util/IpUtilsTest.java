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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class IpUtilsTest {
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

    @Test
    void testCheckIpv6() {
        assertThat(IpUtils.checkIpv6("::1")).isTrue();
        assertThat(IpUtils.checkIpv6("0::1")).isTrue();
        assertThat(IpUtils.checkIpv6("1::1")).isTrue();
        assertThat(IpUtils.checkIpv6("1:1::1")).isTrue();
        assertThat(IpUtils.checkIpv6("0:0:0:0:0:0:0:0")).isTrue();
        assertThat(IpUtils.checkIpv6("00:00::00:00:00")).isTrue();
        assertThat(IpUtils.checkIpv6("00:00:00:00:00:00:00:00")).isTrue();
        assertThat(IpUtils.checkIpv6("01:23:45:67:89:ab:cd:ef")).isTrue();
        assertThat(IpUtils.checkIpv6("001:023:45:67:89:ab:cd:ef")).isTrue();
        assertThat(IpUtils.checkIpv6("1101:23:45:67:89:ab:cd:ef")).isTrue();
        assertThat(IpUtils.checkIpv6("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff")).isTrue();
        assertThat(IpUtils.checkIpv6("0ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff")).isFalse();
        assertThat(IpUtils.checkIpv6("ff:ff:ff:ff:ff:ff:ff:fg")).isFalse();
        assertThat(IpUtils.checkIpv6("1::1::1")).isFalse();
        assertThat(IpUtils.checkIpv6("1::1::123")).isFalse();
        assertThat(IpUtils.checkIpv6("127.0.0.1")).isFalse();
    }

    @Test
    void testRemoveBkCloudId() {
        String ip = "0:127.0.0.1";
        String result = IpUtils.removeBkCloudId(ip);
        assertThat(result).isEqualTo("127.0.0.1");

        ip = "0:0000:0000:0000:0000:0000:0000:0000:0001";
        result = IpUtils.removeBkCloudId(ip);
        assertThat(result).isEqualTo("0000:0000:0000:0000:0000:0000:0000:0001");

        ip = "127.0.0.1";
        result = IpUtils.removeBkCloudId(ip);
        assertThat(result).isEqualTo("127.0.0.1");

        result = IpUtils.removeBkCloudId(null);
        assertThat(result).isNull();
    }
}
