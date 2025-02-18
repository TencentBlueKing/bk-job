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

import com.tencent.bk.job.common.model.dto.Container;
import com.tencent.bk.job.common.model.dto.HostDTO;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 作业实例中包含的执行对象
 */
@Getter
@ToString
@Slf4j
public class TaskInstanceExecuteObjects {

    /**
     * 当前作业实例是否包含主机执行对象
     */
    @Setter
    private boolean containsAnyHost;
    /**
     * 当前作业实例是否包含容器执行对象
     */
    @Setter
    private boolean containsAnyContainer;

    /**
     * 合法的主机（在当前业务下)
     */
    private List<HostDTO> validHosts;
    /**
     * 不存在的主机
     */
    private List<HostDTO> notExistHosts;
    /**
     * 在其他业务下的主机
     */
    private List<HostDTO> notInAppHosts;

    /**
     * 合法的容器（在当前业务下)
     */
    private Set<Container> validContainers;
    /**
     * 不存在的容器ID列表
     */
    @Setter
    private Set<Long> notExistContainerIds;
    /**
     * 主机白名单
     * key=hostId, value: 允许的操作列表
     */
    @Setter
    private Map<Long, List<String>> whiteHostAllowActions;
    /**
     * 全量主机 Map - key: hostId
     */
    private final Map<Long, HostDTO> hostsByHostId = new HashMap<>();
    /**
     * 全量主机 Map - key: cloudIp
     */
    private final Map<String, HostDTO> hostsByCloudIp = new HashMap<>();

    public void addContainers(Collection<Container> containers) {
        if (validContainers == null) {
            validContainers = new HashSet<>();
        }
        validContainers.addAll(containers);
    }

    public void addContainer(Container container) {
        if (validContainers == null) {
            validContainers = new HashSet<>();
        }
        validContainers.add(container);
    }

    public void setNotExistHosts(List<HostDTO> notExistHosts) {
        this.notExistHosts = notExistHosts;
        putHostMap(notExistHosts);
    }

    public void setNotInAppHosts(List<HostDTO> notInAppHosts) {
        this.notInAppHosts = notInAppHosts;
        putHostMap(notInAppHosts);
    }

    public void setValidHosts(List<HostDTO> validHosts) {
        this.validHosts = validHosts;
        putHostMap(validHosts);
    }

    private void putHostMap(List<HostDTO> hosts) {
        if (CollectionUtils.isNotEmpty(hosts)) {
            hosts.forEach(host -> {
                if (host.getHostId() != null) {
                    hostsByHostId.put(host.getHostId(), host);
                }
                if (host.toCloudIp() != null) {
                    hostsByCloudIp.put(host.toCloudIp(), host);
                }
            });
        }
    }

    public HostDTO queryByHostKey(HostDTO host) {
        if (host.getHostId() != null) {
            return hostsByHostId.get(host.getHostId());
        } else if (host.toCloudIp() != null) {
            return hostsByCloudIp.get(host.toCloudIp());
        } else {
            log.info("Host not found, host: {}", host);
            return null;
        }
    }
}
