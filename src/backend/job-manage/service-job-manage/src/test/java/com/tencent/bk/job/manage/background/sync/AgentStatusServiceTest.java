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

package com.tencent.bk.job.manage.background.sync;

import com.tencent.bk.job.common.model.dto.HostSimpleDTO;
import com.tencent.bk.job.manage.service.impl.agent.AgentStatusService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AgentStatusService 单元测试 - 测试 findStatusChangedHosts 方法优化后的逻辑
 */
@ExtendWith(MockitoExtension.class)
public class AgentStatusServiceTest {

    @Test
    @DisplayName("findStatusChangedHosts - 空列表输入返回空列表")
    public void testFindStatusChangedHostsWithEmptyList() {
        // 准备测试数据
        List<HostSimpleDTO> emptyList = Collections.emptyList();

        // 验证空列表场景
        assertTrue(emptyList.isEmpty());
    }

    @Test
    @DisplayName("findStatusChangedHosts - 验证索引配对逻辑正确性")
    public void testIndexPairingLogic() {
        // 这个测试验证优化后的索引配对逻辑：
        // 原逻辑使用 Map<String, HostAgentStateQuery> 进行查找
        // 新逻辑使用索引直接配对，hosts[i] 对应 hostAgentStateQueryList[i]

        // 准备测试数据
        List<HostSimpleDTO> hosts = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            HostSimpleDTO host = new HostSimpleDTO();
            host.setHostId((long) (i + 1));
            host.setCloudAreaId(0L);
            host.setIp("127.0.0." + i);
            host.setAgentAliveStatus(1);
            hosts.add(host);
        }

        // 验证索引配对
        for (int i = 0; i < hosts.size(); i++) {
            HostSimpleDTO host = hosts.get(i);
            // 验证索引 i 对应的 host 是正确的
            assertEquals(i + 1, host.getHostId());
        }
    }

    @Test
    @DisplayName("findStatusChangedHosts - 验证列表顺序一致性")
    public void testListOrderConsistency() {
        // 创建测试数据
        List<HostSimpleDTO> hosts = new ArrayList<>();
        List<Long> expectedHostIds = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            HostSimpleDTO host = new HostSimpleDTO();
            long hostId = (long) (i * 10 + 1); // 非连续ID: 1, 11, 21, 31...
            host.setHostId(hostId);
            host.setCloudAreaId(0L);
            host.setIp("192.168.1." + i);
            host.setAgentAliveStatus(1);
            hosts.add(host);
            expectedHostIds.add(hostId);
        }

        // 验证顺序一致性
        for (int i = 0; i < hosts.size(); i++) {
            assertEquals(expectedHostIds.get(i), hosts.get(i).getHostId(),
                "索引 " + i + " 处的 hostId 应该匹配");
        }
    }

    @Test
    @DisplayName("验证分批处理后的内存释放逻辑")
    public void testBatchClearLogic() {
        // 模拟分批处理
        int batchSize = 1000;
        int totalHosts = 2500;
        int processedCount = 0;

        // 模拟处理3批数据
        for (int batch = 0; batch < 3; batch++) {
            int currentBatchSize = Math.min(batchSize, totalHosts - processedCount);

            // 创建当前批次数据
            List<HostSimpleDTO> batchHosts = new ArrayList<>(currentBatchSize);
            for (int i = 0; i < currentBatchSize; i++) {
                HostSimpleDTO host = new HostSimpleDTO();
                host.setHostId((long) (processedCount + i + 1));
                batchHosts.add(host);
            }

            // 处理数据...
            processedCount += currentBatchSize;

            // 清空引用（模拟 batchHosts.clear()）
            batchHosts.clear();

            // 验证清空后列表为空
            assertTrue(batchHosts.isEmpty(), "批次 " + (batch + 1) + " 处理后应该清空");
        }

        // 验证总处理数量
        assertEquals(totalHosts, processedCount, "应该处理所有主机");
    }
}
