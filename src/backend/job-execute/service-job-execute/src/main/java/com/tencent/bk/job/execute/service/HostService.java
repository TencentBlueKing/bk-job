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

package com.tencent.bk.job.execute.service;

import com.tencent.bk.job.common.cc.model.CcInstanceDTO;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.manage.model.inner.ServiceHostDTO;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 服务器服务
 */
public interface HostService {

    /**
     * 批量获取主机信息
     *
     * @param hostIps 主机Ip列表
     * @return 主机信息
     */
    Map<HostDTO, ServiceHostDTO> batchGetHosts(List<HostDTO> hostIps);

    /**
     * 获取主机信息
     *
     * @param host 主机
     * @return 主机信息
     */
    ServiceHostDTO getHost(HostDTO host);

    /**
     * 查询主机在白名单中允许的操作
     *
     * @param appId 业务ID
     * @param host  主机
     * @return 允许的操作
     */
    List<String> getHostAllowedAction(long appId, HostDTO host);

    /**
     * 判断IP白名单是否配置该主机的对应操作
     *
     * @param appId  业务ID
     * @param host   主机
     * @param action 操作
     * @return 是否配置规则
     */
    boolean isMatchWhiteIpRule(long appId, HostDTO host, String action);

    /**
     * 检查主机是否在业务下
     *
     * @param appId   Job业务ID
     * @param hostIps 主机列表
     * @return 非法的主机
     */
    List<HostDTO> checkAppHosts(Long appId, Collection<HostDTO> hostIps);

    /**
     * 获取动态分组主机
     *
     * @param appId   业务ID
     * @param groupId 动态分组ID
     * @return 主机列表
     */
    List<HostDTO> getIpByDynamicGroupId(long appId, String groupId);

    /**
     * 根据topo节点获取主机
     *
     * @param appId       Job业务ID
     * @param ccInstances topo节点列表
     * @return 主机列表
     */
    List<HostDTO> getIpByTopoNodes(long appId, List<CcInstanceDTO> ccInstances);

    /**
     * 获取主机云区域名称
     *
     * @param cloudAreaId 云区域ID
     * @return 云区域名称
     */
    String getCloudAreaName(long cloudAreaId);
}
