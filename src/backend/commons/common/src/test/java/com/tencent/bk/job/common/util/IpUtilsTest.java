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
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

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
        assertThat(IpUtils.checkIpv6("::192.168.0.1")).isTrue();
        assertThat(IpUtils.checkIpv6("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff")).isTrue();
        assertThat(IpUtils.checkIpv6("0ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff")).isFalse();
        assertThat(IpUtils.checkIpv6("::192.168.0.1/96")).isFalse();
        assertThat(IpUtils.checkIpv6("ff:ff:ff:ff:ff:ff:ff:fg")).isFalse();
        assertThat(IpUtils.checkIpv6("1::1::1")).isFalse();
        assertThat(IpUtils.checkIpv6("1::1::123")).isFalse();
        assertThat(IpUtils.checkIpv6("127.0.0.1")).isFalse();
    }

    @Test
    void testExtractIp() {
        String cloudIp = "0:127.0.0.1";
        String result = IpUtils.extractIp(cloudIp);
        assertThat(result).isEqualTo("127.0.0.1");

        cloudIp = "0:0000:0000:0000:0000:0000:0000:0000:0001";
        result = IpUtils.extractIp(cloudIp);
        assertThat(result).isEqualTo("0000:0000:0000:0000:0000:0000:0000:0001");

        cloudIp = "127.0.0.1";
        result = IpUtils.extractIp(cloudIp);
        assertThat(result).isEqualTo("127.0.0.1");

        result = IpUtils.extractIp(null);
        assertThat(result).isNull();
    }

    @Test
    void testParseCleanIpAndCloudIps() {
        Pair<Set<String>, Set<String>> emptyPair = IpUtils.parseCleanIpv4AndCloudIpv4s(null);
        assertThat(emptyPair.getLeft().size()).isEqualTo(0);
        assertThat(emptyPair.getRight().size()).isEqualTo(0);

        emptyPair = IpUtils.parseCleanIpv4AndCloudIpv4s(new ArrayList<>());
        assertThat(emptyPair.getLeft().size()).isEqualTo(0);
        assertThat(emptyPair.getRight().size()).isEqualTo(0);

        List<String> ipv4OrCloudIpv4List = new ArrayList<>();
        ipv4OrCloudIpv4List.add("127.0.0.1");
        ipv4OrCloudIpv4List.add(" 127.0.0.1");
        ipv4OrCloudIpv4List.add("0:127 .0.0.1");
        ipv4OrCloudIpv4List.add("0:[127 .0.0.2]");
        ipv4OrCloudIpv4List.add("127.0.0.2");
        ipv4OrCloudIpv4List.add("  127.0.0.2  \n");
        ipv4OrCloudIpv4List.add("1:[127.0.0.3] \r\n");
        Pair<Set<String>, Set<String>> pair = IpUtils.parseCleanIpv4AndCloudIpv4s(ipv4OrCloudIpv4List);
        Set<String> ipv4Set = pair.getLeft();
        Set<String> cloudIpv4Set = pair.getRight();
        assertThat(ipv4Set.size()).isEqualTo(2);
        assertThat(ipv4Set.contains("127.0.0.1")).isTrue();
        assertThat(ipv4Set.contains("127.0.0.2")).isTrue();
        assertThat(cloudIpv4Set.size()).isEqualTo(3);
        assertThat(cloudIpv4Set.contains("0:127.0.0.1")).isTrue();
        assertThat(cloudIpv4Set.contains("0:127.0.0.2")).isTrue();
        assertThat(cloudIpv4Set.contains("1:127.0.0.3")).isTrue();
    }

    @Test
    void testParseCleanIpv6AndCloudIpv6s() {
        Pair<Set<String>, Set<Pair<Long, String>>> emptyPair = IpUtils.parseFullIpv6AndCloudIpv6s(null);
        assertThat(emptyPair.getLeft().size()).isEqualTo(0);
        assertThat(emptyPair.getRight().size()).isEqualTo(0);

        emptyPair = IpUtils.parseFullIpv6AndCloudIpv6s(new ArrayList<>());
        assertThat(emptyPair.getLeft().size()).isEqualTo(0);
        assertThat(emptyPair.getRight().size()).isEqualTo(0);

        List<String> ipv6OrCloudIpv6List = new ArrayList<>();
        ipv6OrCloudIpv6List.add("FFFF::1");
        ipv6OrCloudIpv6List.add(" FFFF:ABCD::1");
        ipv6OrCloudIpv6List.add("0:[FFFF::1]");
        ipv6OrCloudIpv6List.add("0:[FFFF:1234::2]");
        ipv6OrCloudIpv6List.add("[FFFF:1234::2]");
        ipv6OrCloudIpv6List.add("  FFFF:1234::2  \n");
        ipv6OrCloudIpv6List.add("1:[[FFFF:1234::2]] \r\n");
        Pair<Set<String>, Set<Pair<Long, String>>> pair = IpUtils.parseFullIpv6AndCloudIpv6s(ipv6OrCloudIpv6List);
        Set<String> ipv6Set = pair.getLeft();
        Set<Pair<Long, String>> cloudIpv6Set = pair.getRight();
        assertThat(ipv6Set.size()).isEqualTo(3);
        assertThat(ipv6Set.contains("FFFF:0000:0000:0000:0000:0000:0000:0001")).isTrue();
        assertThat(ipv6Set.contains("FFFF:ABCD:0000:0000:0000:0000:0000:0001")).isTrue();
        assertThat(ipv6Set.contains("FFFF:1234:0000:0000:0000:0000:0000:0002")).isTrue();
        assertThat(cloudIpv6Set.size()).isEqualTo(3);
        assertThat(cloudIpv6Set.contains(Pair.of(0L, "FFFF:0000:0000:0000:0000:0000:0000:0001"))).isTrue();
        assertThat(cloudIpv6Set.contains(Pair.of(0L, "FFFF:1234:0000:0000:0000:0000:0000:0002"))).isTrue();
        assertThat(cloudIpv6Set.contains(Pair.of(1L, "FFFF:1234:0000:0000:0000:0000:0000:0002"))).isTrue();
    }

    @Test
    void testInferProtocolByIp() {
        // Ipv6正例
        assertThat(IpUtils.inferProtocolByIp("::1")).isEqualTo(IpUtils.PROTOCOL_IP_V6);
        assertThat(IpUtils.inferProtocolByIp("0::1")).isEqualTo(IpUtils.PROTOCOL_IP_V6);
        assertThat(IpUtils.inferProtocolByIp("1::1")).isEqualTo(IpUtils.PROTOCOL_IP_V6);
        assertThat(IpUtils.inferProtocolByIp("1:1::1")).isEqualTo(IpUtils.PROTOCOL_IP_V6);
        assertThat(IpUtils.inferProtocolByIp("0:0:0:0:0:0:0:0")).isEqualTo(IpUtils.PROTOCOL_IP_V6);
        assertThat(IpUtils.inferProtocolByIp("00:00::00:00:00")).isEqualTo(IpUtils.PROTOCOL_IP_V6);
        assertThat(IpUtils.inferProtocolByIp("00:00:00:00:00:00:00:00")).isEqualTo(IpUtils.PROTOCOL_IP_V6);
        assertThat(IpUtils.inferProtocolByIp("01:23:45:67:89:ab:cd:ef")).isEqualTo(IpUtils.PROTOCOL_IP_V6);
        assertThat(IpUtils.inferProtocolByIp("001:023:45:67:89:ab:cd:ef")).isEqualTo(IpUtils.PROTOCOL_IP_V6);
        assertThat(IpUtils.inferProtocolByIp("1101:23:45:67:89:ab:cd:ef")).isEqualTo(IpUtils.PROTOCOL_IP_V6);
        assertThat(IpUtils.inferProtocolByIp("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff")).isEqualTo(IpUtils.PROTOCOL_IP_V6);
        // Ipv6负例
        assertThat(IpUtils.inferProtocolByIp("0ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff")).isNotEqualTo(IpUtils.PROTOCOL_IP_V6);
        assertThat(IpUtils.inferProtocolByIp("ff:ff:ff:ff:ff:ff:ff:fg")).isNotEqualTo(IpUtils.PROTOCOL_IP_V6);
        assertThat(IpUtils.inferProtocolByIp("1::1::1")).isNotEqualTo(IpUtils.PROTOCOL_IP_V6);
        assertThat(IpUtils.inferProtocolByIp("1::1::123")).isNotEqualTo(IpUtils.PROTOCOL_IP_V6);
        assertThat(IpUtils.inferProtocolByIp("127.0.0.1")).isNotEqualTo(IpUtils.PROTOCOL_IP_V6);
        // Ipv4正例
        assertThat(IpUtils.inferProtocolByIp("127.0.0.1")).isEqualTo(IpUtils.PROTOCOL_IP_V4);
        assertThat(IpUtils.inferProtocolByIp("192.168.1.1")).isEqualTo(IpUtils.PROTOCOL_IP_V4);
        // Ipv4负例
        assertThat(IpUtils.inferProtocolByIp("::1")).isNotEqualTo(IpUtils.PROTOCOL_IP_V4);
        assertThat(IpUtils.inferProtocolByIp("01:23:45:67:89:ab:cd:ef")).isNotEqualTo(IpUtils.PROTOCOL_IP_V4);
    }

    @Test
    void testGetFullIpv6ByCompressedOne() {
        assertThatThrownBy(() -> IpUtils.getFullIpv6ByCompressedOne(null));
        assertThatThrownBy(() -> IpUtils.getFullIpv6ByCompressedOne(""));
        assertThat(IpUtils.getFullIpv6ByCompressedOne("::"))
            .isEqualTo("0000:0000:0000:0000:0000:0000:0000:0000");
        assertThat(IpUtils.getFullIpv6ByCompressedOne("::1"))
            .isEqualTo("0000:0000:0000:0000:0000:0000:0000:0001");
        assertThat(IpUtils.getFullIpv6ByCompressedOne("0::1"))
            .isEqualTo("0000:0000:0000:0000:0000:0000:0000:0001");
        assertThat(IpUtils.getFullIpv6ByCompressedOne("00::1"))
            .isEqualTo("0000:0000:0000:0000:0000:0000:0000:0001");
        assertThat(IpUtils.getFullIpv6ByCompressedOne("0000::1"))
            .isEqualTo("0000:0000:0000:0000:0000:0000:0000:0001");
        assertThatThrownBy(() -> IpUtils.getFullIpv6ByCompressedOne("00000::1"));
        assertThatThrownBy(() -> IpUtils.getFullIpv6ByCompressedOne("10000::1"));
        assertThat(IpUtils.getFullIpv6ByCompressedOne("1:0000::1"))
            .isEqualTo("0001:0000:0000:0000:0000:0000:0000:0001");
        assertThat(IpUtils.getFullIpv6ByCompressedOne("1:1:1:1:1:1:1:1"))
            .isEqualTo("0001:0001:0001:0001:0001:0001:0001:0001");
        assertThat(IpUtils.getFullIpv6ByCompressedOne("1:1:1:0:0:1:1:1"))
            .isEqualTo("0001:0001:0001:0000:0000:0001:0001:0001");
        assertThat(IpUtils.getFullIpv6ByCompressedOne("ffff:1:1:0:0:1:1:ffff"))
            .isEqualTo("ffff:0001:0001:0000:0000:0001:0001:ffff");
        assertThat(IpUtils.getFullIpv6ByCompressedOne("FFFF:1:1:0:0:1:1:FFFF"))
            .isEqualTo("FFFF:0001:0001:0000:0000:0001:0001:FFFF");
        assertThatThrownBy(() -> IpUtils.getFullIpv6ByCompressedOne("aaffff:1:1:0:0:1:1:bbffff"));
        assertThat(IpUtils.getFullIpv6ByCompressedOne("ffff:0001:0001:0000:0000:0001:0001:ffff"))
            .isEqualTo("ffff:0001:0001:0000:0000:0001:0001:ffff");
        // 兼容IPv4地址的用例
        assertThat(IpUtils.getFullIpv6ByCompressedOne("::FFFF:192.168.0.1"))
            .isEqualTo("0000:0000:0000:0000:0000:0000:FFFF:192.168.0.1");
    }

    @Test
    void testExtractBkCloudId() {
        String cloudIp = "0:127.0.0.1";
        Long bkCloudId = IpUtils.extractBkCloudId(cloudIp);
        assertThat(bkCloudId).isEqualTo(0L);

        String cloudIpv6 = "0:0000:0000:0000:0000:0000:0000:0000:0000";
        bkCloudId = IpUtils.extractBkCloudId(cloudIpv6);
        assertThat(bkCloudId).isEqualTo(0L);
    }

    @Test
    void testGetFirstIpFromMultiIp() {
        assertThat(IpUtils.getFirstIpFromMultiIp(null, ",")).isNull();
        assertThat(IpUtils.getFirstIpFromMultiIp("", ",")).isEqualTo("");
        assertThat(IpUtils.getFirstIpFromMultiIp("192.168.1.1", ",")).isEqualTo("192.168.1.1");
        assertThat(IpUtils.getFirstIpFromMultiIp("192.168.1.1,", ",")).isEqualTo("192.168.1.1");
        assertThat(IpUtils.getFirstIpFromMultiIp("192.168.1.1,192.168.1.2", ",")).isEqualTo("192.168.1.1");
    }
}
