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

package com.tencent.bk.job.execute.engine.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.tencent.bk.job.common.annotation.PersistenceObject;
import com.tencent.bk.job.common.constant.ExecuteObjectTypeEnum;
import com.tencent.bk.job.common.gse.v2.model.Agent;
import com.tencent.bk.job.common.gse.v2.model.ExecuteObjectGseKey;
import com.tencent.bk.job.common.model.dto.Container;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.model.ExecuteObjectCompositeKey;
import com.tencent.bk.job.execute.model.web.vo.ExecuteObjectVO;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * 作业执行对象通用模型
 */
@Setter
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
@PersistenceObject
@Slf4j
public class ExecuteObject implements Cloneable {
    /**
     * 执行对象 ID
     */
    private String id;

    /**
     * 执行对象类型
     *
     * @see ExecuteObjectTypeEnum
     */
    private ExecuteObjectTypeEnum type;

    /**
     * 执行对象资源 ID（主机 ID/容器 ID)
     */
    private Long resourceId;

    /**
     * 容器
     */
    private Container container;

    /**
     * 主机
     */
    private HostDTO host;

    /**
     * 执行对象对应的 <GSE 执行目标 KEY>。不会被持久化
     */
    @JsonIgnore
    private ExecuteObjectGseKey executeObjectGseKey;

    public ExecuteObject(Container container) {
        this.type = ExecuteObjectTypeEnum.CONTAINER;
        this.container = container;
        this.resourceId = container.getId();
    }

    public ExecuteObject(HostDTO host) {
        this.type = ExecuteObjectTypeEnum.HOST;
        this.host = host;
        this.resourceId = host.getHostId();
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static ExecuteObjectTypeEnum fromExecuteObjectTypeValue(int type) {
        return ExecuteObjectTypeEnum.valOf(type);
    }

    @Override
    public ExecuteObject clone() {
        ExecuteObject clone = new ExecuteObject();
        clone.setId(id);
        clone.setType(type);
        clone.setResourceId(resourceId);
        if (host != null) {
            clone.setHost(host.clone());
        }
        if (container != null) {
            clone.setContainer(container.clone());
        }
        return clone;
    }

    @JsonIgnore
    public boolean isHostExecuteObject() {
        return type == ExecuteObjectTypeEnum.HOST;
    }

    @JsonIgnore
    public boolean isContainerExecuteObject() {
        return type == ExecuteObjectTypeEnum.CONTAINER;
    }

    public ExecuteObjectGseKey toExecuteObjectGseKey() {
        if (executeObjectGseKey != null) {
            return executeObjectGseKey;
        }
        if (isHostExecuteObject()) {
            executeObjectGseKey = ExecuteObjectGseKey.ofHost(host.getAgentId());
        } else {
            executeObjectGseKey = ExecuteObjectGseKey.ofContainer(container.getAgentId(), container.getContainerId());
        }
        return executeObjectGseKey;
    }

    @JsonIgnore
    public boolean isAgentIdEmpty() {
        if (isHostExecuteObject()) {
            return StringUtils.isEmpty(getHost().getAgentId());
        } else {
            return StringUtils.isEmpty(getContainer().getAgentId());
        }
    }

    public Agent toGseAgent() {
        Agent agent = new Agent();
        if (isHostExecuteObject()) {
            agent.setAgentId(host.getAgentId());
        } else {
            agent.setAgentId(container.getAgentId());
            agent.setContainerId(container.getContainerId());
        }
        return agent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecuteObject that = (ExecuteObject) o;
        if (id != null) {
            // 优先使用 ID 计算
            return id.equals(that.getId());
        } else if (type != null && resourceId != null) {
            return type == that.getType() && resourceId.equals(that.getResourceId());
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(type.getValue(), resourceId);
    }

    /**
     * 获取可读性的执行对象名称
     */
    @JsonIgnore
    public String getExecuteObjectName() {
        if (isHostExecuteObject()) {
            return host.getPrimaryIp();
        } else if (isContainerExecuteObject()) {
            return container.getContainerId();
        } else {
            return null;
        }
    }

    public ExecuteObjectCompositeKey toExecuteObjectCompositeKey(
        ExecuteObjectCompositeKey.CompositeKeyType compositeKeyType) {
        switch (compositeKeyType) {
            case EXECUTE_OBJECT_ID:
                return ExecuteObjectCompositeKey.ofExecuteObjectId(id);
            case RESOURCE_ID:
                return ExecuteObjectCompositeKey.ofExecuteObjectResource(type, resourceId);
            case HOST_CLOUD_IP:
                return ExecuteObjectCompositeKey.ofHostIp(host.toCloudIp());
        }
        return null;
    }

    public ExecuteObjectVO toExecuteObjectVO() {
        ExecuteObjectVO vo = new ExecuteObjectVO();
        vo.setType(type);
        vo.setExecuteObjectResourceId(resourceId);
        if (host != null) {
            vo.setHost(host.toHostInfoVO());
        }
        if (container != null) {
            vo.setContainer(container.toContainerVO());
        }
        return vo;
    }

    /**
     * 判断是否支持执行对象特性
     */
    public boolean isSupportExecuteObjectFeature() {
        return id != null;
    }
}
