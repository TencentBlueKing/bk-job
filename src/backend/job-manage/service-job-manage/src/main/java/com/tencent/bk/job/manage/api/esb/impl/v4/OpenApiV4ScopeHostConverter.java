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

package com.tencent.bk.job.manage.api.esb.impl.v4;

import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.manage.model.esb.v4.req.V4TopoNodeDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4HostTopoNodeDTO;
import com.tencent.bk.job.manage.model.esb.v4.resp.V4ScopeHostDTO;
import com.tencent.bk.job.manage.model.web.request.chooser.host.BizTopoNode;
import com.tencent.bk.job.manage.model.web.vo.CcTopologyNodeVO;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * OpenAPI V4 资源范围主机/拓扑相关内部模型与对外 DTO 的转换器。
 */
public class OpenApiV4ScopeHostConverter {

    private OpenApiV4ScopeHostConverter() {
    }

    /**
     * 将内部拓扑树节点递归转换为对外 V4 拓扑树节点（去除 lazy 及主机明细字段，默认全部展开）。
     *
     * @param node 内部拓扑树节点
     * @return 对外 V4 拓扑树节点；入参为 null 时返回 null
     */
    public static V4HostTopoNodeDTO toHostTopoNodeDTO(CcTopologyNodeVO node) {
        if (node == null) {
            return null;
        }
        V4HostTopoNodeDTO dto = new V4HostTopoNodeDTO();
        dto.setObjectId(node.getObjectId());
        dto.setObjectName(node.getObjectName());
        dto.setInstanceId(node.getInstanceId());
        dto.setInstanceName(node.getInstanceName());
        dto.setHostCount(node.getCount());
        if (CollectionUtils.isNotEmpty(node.getChild())) {
            dto.setChild(
                node.getChild().stream()
                    .map(OpenApiV4ScopeHostConverter::toHostTopoNodeDTO)
                    .collect(Collectors.toList())
            );
        }
        return dto;
    }

    /**
     * 将内部主机模型转换为对外 V4 主机模型（不对外暴露 agentId）。
     *
     * @param host 内部主机模型
     * @return 对外 V4 主机模型；入参为 null 时返回 null
     */
    public static V4ScopeHostDTO toScopeHostDTO(ApplicationHostDTO host) {
        if (host == null) {
            return null;
        }
        V4ScopeHostDTO dto = new V4ScopeHostDTO();
        dto.setHostId(host.getHostId());
        dto.setIp(host.getIp());
        dto.setIpv6(host.getIpv6());
        dto.setCloudAreaId(host.getCloudAreaId());
        dto.setCloudAreaName(host.getCloudAreaName());
        dto.setHostName(host.getHostName());
        dto.setOsName(host.getOsName());
        dto.setOsType(host.getOsType());
        dto.setAlive(Boolean.TRUE.equals(host.getGseAgentAlive()) ? 1 : 0);
        return dto;
    }

    /**
     * 将对外拓扑节点列表转换为内部拓扑节点列表。
     *
     * @param topoNodeList 对外拓扑节点列表
     * @return 内部拓扑节点列表；入参为空时返回 null
     */
    public static List<BizTopoNode> toBizTopoNodeList(List<V4TopoNodeDTO> topoNodeList) {
        if (CollectionUtils.isEmpty(topoNodeList)) {
            return null;
        }
        return topoNodeList.stream()
            .map(node -> {
                BizTopoNode bizTopoNode = new BizTopoNode();
                bizTopoNode.setObjectId(node.getObjectId());
                bizTopoNode.setInstanceId(node.getInstanceId());
                return bizTopoNode;
            })
            .collect(Collectors.toList());
    }
}
