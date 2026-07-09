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

package com.tencent.bk.job.common.cc.util;

import com.tencent.bk.job.common.cc.constants.KubeTopoNodeTypeEnum;
import com.tencent.bk.job.common.cc.model.container.KubeNodeID;
import com.tencent.bk.job.common.model.dto.KubeTopoDTO;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 容器拓扑路径工具：把 Web 入口形态的拓扑路径列表（{@link KubeTopoDTO}）解析为 CMDB 拓扑节点 ID 列表。
 * <p>
 * Web 入口的 id 已经是 CMDB 内部拓扑节点 ID，无需二次访问 CMDB。每条 topo 取其「最精细的已选节点」：
 * workload → namespace → cluster（cluster 必填兜底）。cmdb-sdk 查询与 job-execute 运行时解析共用同一套规则。
 */
public final class KubeTopoUtil {

    private KubeTopoUtil() {
    }

    /**
     * 逐条拓扑路径取最精细节点，汇总为 CMDB 拓扑节点 ID 列表。
     * 入参为空时返回空列表。
     */
    public static List<KubeNodeID> toNodeIdList(List<KubeTopoDTO> kubeTopoList) {
        if (CollectionUtils.isEmpty(kubeTopoList)) {
            return new ArrayList<>();
        }
        List<KubeNodeID> nodeIdList = new ArrayList<>(kubeTopoList.size());
        for (KubeTopoDTO topo : kubeTopoList) {
            KubeNodeID nodeId = toNodeId(topo);
            if (nodeId != null) {
                nodeIdList.add(nodeId);
            }
        }
        return nodeIdList;
    }

    /**
     * 单条拓扑路径取最精细节点：workload → namespace → cluster；均为空则返回 null。
     */
    public static KubeNodeID toNodeId(KubeTopoDTO topo) {
        if (topo == null) {
            return null;
        }
        if (topo.getWorkload() != null) {
            return new KubeNodeID(topo.getWorkload().getKind(), topo.getWorkload().getId());
        }
        if (topo.getNamespace() != null) {
            return new KubeNodeID(KubeTopoNodeTypeEnum.NAMESPACE.getValue(), topo.getNamespace().getId());
        }
        if (topo.getCluster() != null) {
            return new KubeNodeID(KubeTopoNodeTypeEnum.CLUSTER.getValue(), topo.getCluster().getId());
        }
        return null;
    }
}
