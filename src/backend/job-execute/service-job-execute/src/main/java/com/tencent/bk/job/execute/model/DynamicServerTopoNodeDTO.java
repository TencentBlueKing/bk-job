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

package com.tencent.bk.job.execute.model;

import com.tencent.bk.job.common.annotation.PersistenceObject;
import com.tencent.bk.job.common.model.dto.HostDTO;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 服务器动态分组
 */
@PersistenceObject
@Data
@NoArgsConstructor
public class DynamicServerTopoNodeDTO implements Cloneable {
    /**
     * cmdb分布式拓扑节点ID
     */
    private long topoNodeId;

    private String nodeType;

    /**
     * 分布式拓扑节点对应的静态IP
     */
    private List<HostDTO> ipList;

    public DynamicServerTopoNodeDTO(long topoNodeId, String nodeType) {
        this.topoNodeId = topoNodeId;
        this.nodeType = nodeType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DynamicServerTopoNodeDTO that = (DynamicServerTopoNodeDTO) o;
        return topoNodeId == that.topoNodeId &&
            nodeType.equals(that.nodeType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topoNodeId, nodeType);
    }

    public DynamicServerTopoNodeDTO clone() {
        DynamicServerTopoNodeDTO cloneTopoNode = new DynamicServerTopoNodeDTO();
        cloneTopoNode.setNodeType(nodeType);
        cloneTopoNode.setTopoNodeId(topoNodeId);
        if (ipList != null) {
            List<HostDTO> cloneIpList = new ArrayList<>(ipList.size());
            ipList.forEach(ip -> cloneIpList.add(ip.clone()));
            cloneTopoNode.setIpList(cloneIpList);
        }
        return cloneTopoNode;
    }
}
