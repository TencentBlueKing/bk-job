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

package com.tencent.bk.job.manage.service.topo.impl;

import com.tencent.bk.job.common.cc.model.CcInstanceDTO;
import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
import com.tencent.bk.job.common.cc.sdk.CmdbClientFactory;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.cc.util.TopologyUtil;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.common.TopologyHelper;
import com.tencent.bk.job.manage.model.web.request.chooser.host.BizTopoNode;
import com.tencent.bk.job.manage.service.topo.BizTopoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class BizTopoServiceImpl implements BizTopoService {

    private final IBizCmdbClient bizCmdbClient;

    @Autowired
    public BizTopoServiceImpl(IBizCmdbClient bizCmdbClient) {
        this.bizCmdbClient = bizCmdbClient;
    }

    @Override
    public List<List<InstanceTopologyDTO>> queryBizNodePaths(Long bizId,
                                                             List<InstanceTopologyDTO> nodeList) {
        // 查业务拓扑树
        IBizCmdbClient bizCmdbClient = CmdbClientFactory.getCmdbClient(JobContextUtil.getUserLang());
        InstanceTopologyDTO appTopologyTree = bizCmdbClient.getBizInstTopology(bizId);
        // 搜索路径
        return TopologyHelper.findTopoPaths(appTopologyTree, nodeList);
    }


    /**
     * 找出多个拓扑节点下属的所有模块ID
     *
     * @param bizId           业务ID
     * @param appTopoNodeList 拓扑节点列表
     * @return 模块ID列表
     */
    public List<Long> findAllModuleIdsOfNodes(Long bizId,
                                              List<BizTopoNode> appTopoNodeList) {
        if (appTopoNodeList == null || appTopoNodeList.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> moduleIds = new ArrayList<>();
        // 查业务拓扑树
        InstanceTopologyDTO appTopologyTree = bizCmdbClient.getBizInstTopology(bizId);
        for (BizTopoNode treeNode : appTopoNodeList) {
            CcInstanceDTO ccInstanceDTO = new CcInstanceDTO(treeNode.getObjectId(), treeNode.getInstanceId());
            // 查拓扑节点完整信息
            InstanceTopologyDTO completeNode = TopologyUtil.findNodeFromTopo(appTopologyTree, ccInstanceDTO);
            if (completeNode == null) {
                log.warn(
                    "Cannot find node in topo, node:{}",
                    JsonUtils.toJson(ccInstanceDTO)
                );
                if (log.isDebugEnabled()) {
                    TopologyUtil.printTopo(appTopologyTree);
                }
                continue;
            }
            moduleIds.addAll(TopologyUtil.findModuleIdsFromTopo(completeNode));
        }
        return moduleIds;
    }
}
