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

package com.tencent.bk.job.common.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.annotation.PersistenceObject;
import com.tencent.bk.job.common.model.openapi.v4.OpenApiContainerDTO;
import com.tencent.bk.job.common.model.vo.ContainerVO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Objects;

/**
 * 作业执行对象-容器模型
 */
@Setter
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@PersistenceObject
@Slf4j
public class Container implements Cloneable {
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
     * 容器所在 Node 对应的 Agent ID
     */
    @JsonProperty("nodeAgentId")
    private String nodeAgentId;

    /**
     * 容器所在集群 ID
     */
    @JsonProperty("clusterId")
    private String clusterId;

    /**
     * 容器所在命名空间
     */
    @JsonProperty("namespace")
    private String namespace;

    /**
     * 容器所在 POD 名称
     */
    @JsonProperty("podName")
    private String podName;

    /**
     * 容器所在 POD 名称
     */
    @JsonProperty("podLabels")
    private Map<String, String> podLabels;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Container container = (Container) o;
        return id.equals(container.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    @SuppressWarnings("all")
    public Container clone() {
        Container clone = new Container();
        clone.setId(id);
        clone.setNodeHostId(nodeHostId);
        clone.setNodeAgentId(nodeAgentId);
        clone.setContainerId(containerId);
        clone.setPodLabels(podLabels);
        clone.setClusterId(clusterId);
        clone.setNamespace(namespace);
        clone.setPodName(podName);
        clone.setName(name);
        return clone;
    }

    public ContainerVO toContainerVO() {
        ContainerVO vo = new ContainerVO();
        vo.setId(id);
        vo.setName(name);
        vo.setUid(containerId);
        vo.setNodeHostId(nodeHostId);
        vo.setPodName(podName);
        vo.setPodLabels(podLabels);
        return vo;
    }

    public OpenApiContainerDTO toOpenApiContainerDTO() {
        OpenApiContainerDTO openApiContainerDTO = new OpenApiContainerDTO();
        openApiContainerDTO.setContainerId(containerId);
        openApiContainerDTO.setId(id);
        openApiContainerDTO.setNodeHostId(nodeHostId);
        openApiContainerDTO.setName(name);
        return openApiContainerDTO;
    }

    public void updatePropsByContainer(Container container) {
        this.containerId = container.getContainerId();
        this.nodeHostId = container.getNodeHostId();
        this.nodeAgentId = container.getNodeAgentId();
        this.clusterId = container.getClusterId();
        this.namespace = container.getNamespace();
        this.podName = container.getPodName();
        this.podLabels = container.getPodLabels();
        this.name = container.getName();
    }
}
