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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.model.vo.ContainerVO;
import com.tencent.bk.job.manage.model.inner.ServiceTargetContainerDTO;
import lombok.Data;

import java.util.Map;

@Data
public class TaskTargetContainerDTO {
    /**
     * 容器在 cmdb 注册的 ID
     */
    @JsonProperty("id")
    private Long id;

    /**
     * 容器 UID
     */
    @JsonProperty("containerId")
    private String containerId;

    /**
     * 容器名称
     */
    private String name;

    /**
     * 容器所在 Node 对应的主机ID
     */
    @JsonProperty("nodeHostId")
    private Long nodeHostId;

    /**
     * 所在 Node 的 ip
     */
    @JsonProperty("nodeIp")
    private String nodeIp;

    /**
     * 容器所在 Node 对应的 Agent ID
     */
    @JsonProperty("nodeAgentId")
    private String nodeAgentId;

    /**
     * cluster在cmdb中的唯一ID
     */
    @JsonProperty("clusterId")
    private Long clusterId;

    /**
     * 集群 ID
     */
    @JsonProperty("clusterUID")
    private String clusterUID;

    /**
     * 集群名称
     */
    @JsonProperty("clusterName")
    private String clusterName;

    /**
     * namespace在cmdb中的唯一ID
     */
    @JsonProperty("namespaceId")
    private Long namespaceId;

    /**
     * 命名空间名称
     */
    @JsonProperty("namespace")
    private String namespace;

    /**
     * POD 名称
     */
    @JsonProperty("podName")
    private String podName;

    /**
     * pod labels
     */
    @JsonProperty("podLabels")
    private Map<String, String> podLabels;

    /**
     * workload 类型(Deployment/Job ...)
     */
    @JsonProperty("workloadType")
    private String workloadType;

    /**
     * workload在cmdb中的唯一ID
     */
    @JsonProperty("workloadId")
    private Long workloadId;

    @JsonIgnore
    public static TaskTargetContainerDTO fromContainerVO(ContainerVO vo) {
        TaskTargetContainerDTO container = new TaskTargetContainerDTO();
        container.setId(vo.getId());
        container.setName(vo.getName());
        container.setContainerId(vo.getUid());
        container.setNodeHostId(vo.getNodeHostId());
        container.setNodeIp(vo.getNodeIp());
        container.setPodName(vo.getPodName());
        container.setPodLabels(vo.getPodLabels());
        container.setClusterId(vo.getClusterId());
        container.setClusterUID(vo.getClusterUID());
        container.setClusterName(vo.getClusterName());
        container.setNamespaceId(vo.getNamespaceId());
        container.setNamespace(vo.getNamespace());
        container.setWorkloadType(vo.getWorkloadType());
        return container;
    }

    public ContainerVO toContainerVO() {
        ContainerVO containerVO = new ContainerVO();
        containerVO.setId(id);
        containerVO.setName(name);
        containerVO.setUid(containerId);
        containerVO.setNodeHostId(nodeHostId);
        containerVO.setNodeIp(nodeIp);
        containerVO.setPodName(podName);
        containerVO.setPodLabels(podLabels);
        containerVO.setClusterId(clusterId);
        containerVO.setClusterUID(clusterUID);
        containerVO.setClusterName(clusterName);
        containerVO.setNamespaceId(namespaceId);
        containerVO.setNamespace(namespace);
        containerVO.setWorkloadType(workloadType);
        return containerVO;
    }

    public ServiceTargetContainerDTO toServiceTargetContainerDTO() {
        ServiceTargetContainerDTO container = new ServiceTargetContainerDTO();
        container.setId(id);
        container.setName(name);
        container.setNodeHostId(nodeHostId);
        container.setNodeIp(nodeIp);
        container.setNodeAgentId(nodeAgentId);
        container.setClusterId(clusterId);
        container.setClusterUID(clusterUID);
        container.setClusterName(clusterName);
        container.setNamespaceId(namespaceId);
        container.setNamespace(namespace);
        container.setWorkloadType(workloadType);
        container.setWorkloadId(workloadId);
        return container;
    }
}
