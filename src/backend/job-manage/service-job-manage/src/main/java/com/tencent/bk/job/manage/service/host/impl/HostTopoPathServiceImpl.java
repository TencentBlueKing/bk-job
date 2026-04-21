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

package com.tencent.bk.job.manage.service.host.impl;

import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.constant.CcNodeTypeEnum;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.vo.HostTopoPathVO;
import com.tencent.bk.job.manage.dao.HostTopoDAO;
import com.tencent.bk.job.manage.model.dto.HostTopoDTO;
import com.tencent.bk.job.manage.service.host.HostTopoPathService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class HostTopoPathServiceImpl implements HostTopoPathService {

    private final HostTopoDAO hostTopoDAO;
    private final IBizCmdbClient bizCmdbClient;

    @Autowired
    public HostTopoPathServiceImpl(HostTopoDAO hostTopoDAO,
                                   IBizCmdbClient bizCmdbClient) {
        this.hostTopoDAO = hostTopoDAO;
        this.bizCmdbClient = bizCmdbClient;
    }

    @Override
    public void fillTopoPathForHosts(String tenantId, long bizId, List<ApplicationHostDTO> hostList) {
        if (CollectionUtils.isEmpty(hostList)) {
            return;
        }

        List<Long> hostIds = hostList.stream()
            .map(ApplicationHostDTO::getHostId)
            .collect(Collectors.toList());

        // 1. 批量查询主机的拓扑关系
        List<HostTopoDTO> hostTopoList = hostTopoDAO.listHostTopoByHostIds(hostIds);
        if (CollectionUtils.isEmpty(hostTopoList)) {
            return;
        }

        // 2. 按hostId分组
        Map<Long, List<HostTopoDTO>> hostTopoMap = hostTopoList.stream()
            .collect(Collectors.groupingBy(HostTopoDTO::getHostId));

        // 3. 获取业务拓扑树，提取set和module的名称映射
        Map<Long, String> setNameMap = new HashMap<>();
        Map<Long, String> moduleNameMap = new HashMap<>();
        try {
            InstanceTopologyDTO topoTree = bizCmdbClient.getBizInstCompleteTopology(tenantId, bizId);
            if (topoTree != null) {
                extractNodeNames(topoTree, setNameMap, moduleNameMap);
            }
        } catch (Exception e) {
            log.warn("Failed to get biz topology for bizId={}, will fallback to show IDs", bizId, e);
        }

        // 4. 为每个主机组装拓扑路径信息
        for (ApplicationHostDTO host : hostList) {
            List<HostTopoDTO> topoRelations = hostTopoMap.get(host.getHostId());
            if (CollectionUtils.isEmpty(topoRelations)) {
                continue;
            }
            List<HostTopoPathVO> topoPathList = new ArrayList<>();
            for (HostTopoDTO topoRelation : topoRelations) {
                String setName = setNameMap.getOrDefault(
                    topoRelation.getSetId(),
                    String.valueOf(topoRelation.getSetId())
                );
                String moduleName = moduleNameMap.getOrDefault(
                    topoRelation.getModuleId(),
                    String.valueOf(topoRelation.getModuleId())
                );
                topoPathList.add(new HostTopoPathVO(setName, moduleName));
            }
            host.setTopoPathList(topoPathList);
        }
    }

    /**
     * 递归遍历拓扑树，提取set和module节点的ID→名称映射
     */
    private void extractNodeNames(InstanceTopologyDTO node,
                                  Map<Long, String> setNameMap,
                                  Map<Long, String> moduleNameMap) {
        if (node == null) {
            return;
        }
        if (CcNodeTypeEnum.SET.getType().equals(node.getObjectId())) {
            setNameMap.put(node.getInstanceId(), node.getInstanceName());
        } else if (CcNodeTypeEnum.MODULE.getType().equals(node.getObjectId())) {
            moduleNameMap.put(node.getInstanceId(), node.getInstanceName());
        }
        if (CollectionUtils.isNotEmpty(node.getChild())) {
            for (InstanceTopologyDTO child : node.getChild()) {
                extractNodeNames(child, setNameMap, moduleNameMap);
            }
        }
    }
}
