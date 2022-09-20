/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 *
 * ---------------------------------------------------
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

/* eslint-disable no-param-reassign */

import * as IpSelectrAdapter from '@components/ip-selector/adapter';

import DynamicGroupInfoVO from '@domain/variable-object/dynamic-group-info';
import NodeInfoVO from '@domain/variable-object/node-info';

import HostManageSource from '../source/host-manage';

export default {
    // 获取拓扑节点树
    fetchTopology () {
        return HostManageSource.getAllTopology()
            .then(({ data }) => data);
    },
    
    // 获取拓扑节点数包含主机
    fetchAllTopologyHost () {
        return HostManageSource.getAllTopologyWithHost()
            .then(({ data }) => data);
    },
    // 获取节点的主机
    fetchNodeInfo (params) {
        return HostManageSource.getHostByNode(params)
            .then(({ data }) => data.map(item => new NodeInfoVO(item)));
    },
    // 获取节点详情
    fetchNodeDetail (params) {
        return HostManageSource.getNodeInfo(params)
            .then(({ data }) => data);
    },
    
    // ip输入获取主机列表
    fetchHostOfHost (params) {
        return HostManageSource.getHostByHost(params)
            .then(({ data }) => data);
    },
    
    // IP选择器根据拓扑节点获取其子节点
    fetchChildOfNode (params) {
        return HostManageSource.getChildOfNode(params)
            .then(({ data }) => data);
    },
    fetchHostStatistics (params) {
        return HostManageSource.getHostStatistics(params)
            .then(({ data }) => data);
    },
    // 获取业务拓扑主机列表（包含主机）
    fetchTopologyWithCount () {
        return HostManageSource.getAllTopologyWithCount()
            .then(({ data }) => IpSelectrAdapter.topologyHostCount(data));
    },
    // 获取节点拓扑路径
    fetchNodePath (params) {
        return HostManageSource.getNodePath(params)
            .then(({ data }) => IpSelectrAdapter.topologyQueryPath(data));
    },
    // 获取动态分列表
    fetchDynamicGroup (params) {
        return HostManageSource.getAllDynamicGroup(params)
            .then(({ data }) => IpSelectrAdapter.dynamicGroups(data));
    },
    // 获取动态分组的主机列表
    fetchHostOfDynamicGroup (params) {
        return HostManageSource.getHostByDynamicGroupId(params)
            .then(({ data }) => data.map(item => new DynamicGroupInfoVO(item)));
    },
    
    fetchTopologyHost (params) {
        return HostManageSource.getTopologyHost(params)
            .then(({ data }) => IpSelectrAdapter.topolopyHostsNodes(data));
    },
    fetchTopogyHostIdList (params) {
        return HostManageSource.getTopologyNodeAllHostId(params)
            .then(({ data }) => IpSelectrAdapter.topolopyHostIdsNodes(data));
    },
    
    fetchBatchNodeAgentStatistics (params) {
        return HostManageSource.getBatchNodeAgentStatistics(params)
            .then(({ data }) => IpSelectrAdapter.hostAgentStatisticsNodes(data));
    },
    fetchDynamicGroupHost (params) {
        return HostManageSource.getDynamicGroupHost(params)
            .then(({ data }) => IpSelectrAdapter.hostsDynamicGroup(data));
    },
    fetchBatchGroupAgentStatistics (params) {
        return HostManageSource.getBatchGroupAgentStatistics(params)
            .then(({ data }) => IpSelectrAdapter.hostAgentStatisticsDynamicGroups(data));
    },
    fetchInputParseHostList (params) {
        return HostManageSource.getInputParseHostList({
            ...params,
            meta: {
                actionScope: window.IPInputScope,
            },
        })
            .then(({ data }) => IpSelectrAdapter.hostCheck(data));
    },
    fetchHostInfoByHostId (params) {
        return HostManageSource.getHostInfoByHostId(params)
            .then(({ data }) => IpSelectrAdapter.hostsDetails(data));
    },
};
