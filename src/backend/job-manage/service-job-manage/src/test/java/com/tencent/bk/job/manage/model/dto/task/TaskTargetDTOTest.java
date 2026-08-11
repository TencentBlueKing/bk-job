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

package com.tencent.bk.job.manage.model.dto.task;

import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class TaskTargetDTOTest {

    private ApplicationHostDTO buildHost(Long hostId, Long cloudAreaId, String ip, String ipv6) {
        ApplicationHostDTO host = new ApplicationHostDTO();
        host.setHostId(hostId);
        host.setCloudAreaId(cloudAreaId);
        host.setIp(ip);
        host.setIpv6(ipv6);
        return host;
    }

    private Map<Long, ApplicationHostDTO> hostIdMapping(ApplicationHostDTO... hosts) {
        Map<Long, ApplicationHostDTO> mapping = new HashMap<>();
        for (ApplicationHostDTO host : hosts) {
            mapping.put(host.getHostId(), host);
        }
        return mapping;
    }

    @Test
    @DisplayName("IPv6主机(IPv4为空)按hostId精确匹配，hostId不会被退化键覆盖")
    void fillHostDetailForIpv6OnlyHosts() {
        ApplicationHostDTO node1 = buildHost(101L, 0L, null, "2001:db8::1");
        ApplicationHostDTO node2 = buildHost(102L, 0L, null, "2001:db8::2");
        List<ApplicationHostDTO> hostList = Arrays.asList(node1, node2);

        Map<Long, ApplicationHostDTO> hostIdHostMapping = hostIdMapping(
            buildHost(101L, 0L, null, "2001:db8::1"),
            buildHost(102L, 0L, null, "2001:db8::2"));
        // 模拟CMDB/缓存中存量的退化键，改动前它会命中并覆盖所有主机的hostId
        Map<String, ApplicationHostDTO> cloudIpHostMapping = new HashMap<>();
        cloudIpHostMapping.put("0:null", buildHost(102L, 0L, null, "2001:db8::2"));

        TaskTargetDTO.fillHostDetail(hostList, hostIdHostMapping, cloudIpHostMapping);

        assertThat(node1.getHostId()).isEqualTo(101L);
        assertThat(node2.getHostId()).isEqualTo(102L);
        assertThat(node1.getHostId()).isNotEqualTo(node2.getHostId());
    }

    @Test
    @DisplayName("IPv6主机(IPv4为空)不产生退化查询键0:null")
    void collectCloudIpsSkipDegradedKeyForIpv6OnlyHosts() {
        List<ApplicationHostDTO> hostList = Arrays.asList(
            buildHost(101L, 0L, null, "2001:db8::1"),
            buildHost(102L, 0L, null, "2001:db8::2"));

        Set<String> cloudIps = TaskTargetDTO.collectCloudIps(hostList);
        Set<Long> hostIds = TaskTargetDTO.collectHostIds(hostList);

        assertThat(cloudIps).isEmpty();
        assertThat(cloudIps).doesNotContain("0:null");
        assertThat(hostIds).containsExactlyInAnyOrder(101L, 102L);
    }

    @Test
    @DisplayName("脏hostId(-1、0)不作为查询键，只收集有效hostId")
    void collectHostIdsSkipInvalidHostId() {
        List<ApplicationHostDTO> hostList = Arrays.asList(
            buildHost(-1L, 0L, "127.0.0.1", null),
            buildHost(0L, 0L, "127.0.0.1", null),
            buildHost(null, 0L, "127.0.0.1", null),
            buildHost(101L, 0L, "127.0.0.1", null));

        Set<Long> hostIds = TaskTargetDTO.collectHostIds(hostList);

        assertThat(hostIds).containsExactly(101L);
        assertThat(hostIds).doesNotContain(-1L, 0L);
    }

    @Test
    @DisplayName("仅有cloudIp无hostId时按cloudIp匹配并回填主机信息")
    void fillHostDetailByCloudIpWhenHostIdAbsent() {
        ApplicationHostDTO node = buildHost(null, 0L, "127.0.0.1", null);
        List<ApplicationHostDTO> hostList = Collections.singletonList(node);

        assertThat(TaskTargetDTO.collectCloudIps(hostList)).containsExactly("0:127.0.0.1");
        assertThat(TaskTargetDTO.collectHostIds(hostList)).isEmpty();

        ApplicationHostDTO hostInDb = buildHost(201L, 0L, "127.0.0.1", "2001:db8::1");
        hostInDb.setAgentId("0:127.0.0.1");
        hostInDb.setOsName("linux");
        Map<String, ApplicationHostDTO> cloudIpHostMapping = new HashMap<>();
        cloudIpHostMapping.put("0:127.0.0.1", hostInDb);

        TaskTargetDTO.fillHostDetail(hostList, Collections.emptyMap(), cloudIpHostMapping);

        assertThat(node.getHostId()).isEqualTo(201L);
        assertThat(node.getAgentId()).isEqualTo("0:127.0.0.1");
        assertThat(node.getIpv6()).isEqualTo("2001:db8::1");
        assertThat(node.getOsName()).isEqualTo("linux");
    }

    @Test
    @DisplayName("hostId与cloudIp指向不同主机时以hostId为准")
    void fillHostDetailPreferHostIdWhenBothMatched() {
        ApplicationHostDTO node = buildHost(101L, 0L, "127.0.0.1", null);
        List<ApplicationHostDTO> hostList = Collections.singletonList(node);

        Map<Long, ApplicationHostDTO> hostIdHostMapping =
            hostIdMapping(buildHost(101L, 0L, "127.0.0.2", null));
        Map<String, ApplicationHostDTO> cloudIpHostMapping = new HashMap<>();
        cloudIpHostMapping.put("0:127.0.0.1", buildHost(301L, 0L, "127.0.0.1", null));

        TaskTargetDTO.fillHostDetail(hostList, hostIdHostMapping, cloudIpHostMapping);

        assertThat(node.getHostId()).isEqualTo(101L);
        assertThat(node.getIp()).isEqualTo("127.0.0.2");
    }

    @Test
    @DisplayName("hostId无效时回退cloudIp匹配并修正hostId")
    void fillHostDetailFallbackToCloudIpWhenHostIdInvalid() {
        ApplicationHostDTO node = buildHost(-1L, 0L, "127.0.0.1", null);
        List<ApplicationHostDTO> hostList = Collections.singletonList(node);

        assertThat(TaskTargetDTO.collectHostIds(hostList)).isEmpty();

        Map<String, ApplicationHostDTO> cloudIpHostMapping = new HashMap<>();
        cloudIpHostMapping.put("0:127.0.0.1", buildHost(401L, 0L, "127.0.0.1", null));

        TaskTargetDTO.fillHostDetail(hostList, Collections.emptyMap(), cloudIpHostMapping);

        assertThat(node.getHostId()).isEqualTo(401L);
    }

    @Test
    @DisplayName("hostId与cloudIp均匹配不到时hostId置为-1")
    void fillHostDetailSetHostIdToMinusOneWhenNotMatched() {
        ApplicationHostDTO node = buildHost(101L, 0L, "127.0.0.1", null);
        List<ApplicationHostDTO> hostList = Collections.singletonList(node);

        TaskTargetDTO.fillHostDetail(hostList, Collections.emptyMap(), Collections.emptyMap());

        assertThat(node.getHostId()).isEqualTo(-1L);
    }

    @Test
    @DisplayName("云区域与IP均缺失时不产生null:null查询键，且匹配不到后hostId为-1")
    void fillHostDetailWhenNoValidQueryKey() {
        ApplicationHostDTO node = buildHost(null, null, null, null);
        List<ApplicationHostDTO> hostList = Collections.singletonList(node);

        Set<String> cloudIps = TaskTargetDTO.collectCloudIps(hostList);
        assertThat(cloudIps).isEmpty();
        assertThat(cloudIps).doesNotContain("null:null");
        assertThat(TaskTargetDTO.collectHostIds(hostList)).isEmpty();

        TaskTargetDTO.fillHostDetail(hostList, Collections.emptyMap(), Collections.emptyMap());

        assertThat(node.getHostId()).isEqualTo(-1L);
    }
}
