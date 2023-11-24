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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.StringJoiner;

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
    private String id;

    /**
     * 容器 ID
     */
    @JsonProperty("containerId")
    private String containerId;


    /**
     * 容器所在 Node 对应的主机ID
     */
    @JsonProperty("hostId")
    private Long hostId;

    /**
     * 容器所在 Node 对应的 Agent ID
     */
    @JsonProperty("agentId")
    private String agentId;


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
    @SuppressWarnings("all")
    public Container clone() {
        Container clone = new Container();
        clone.setHostId(hostId);
        clone.setAgentId(agentId);
        clone.setId(id);
        clone.setContainerId(containerId);
        clone.setPodLabels(podLabels);
        clone.setClusterId(clusterId);
        clone.setNamespace(namespace);
        clone.setPodName(podName);
        return clone;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Container.class.getSimpleName() + "[", "]")
            .add("id='" + id + "'")
            .add("containerId='" + containerId + "'")
            .add("hostId=" + hostId)
            .add("agentId='" + agentId + "'")
            .add("clusterId='" + clusterId + "'")
            .add("namespace='" + namespace + "'")
            .add("podName='" + podName + "'")
            .add("podLabels=" + podLabels)
            .toString();
    }
}
