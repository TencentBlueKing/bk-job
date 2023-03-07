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

import Request from '@utils/request';

import ModuleBase from './module-base';

class HostManage extends ModuleBase {
  constructor() {
    super();
    this.module = 'job-manage/web';
  }

  // 获取动态分组
  getAllDynamicGroup(params = {}) {
    return Request.post(`${this.path}/dynamicGroups`, {
      params,
      cache: 2000,
    });
  }

  // 获取动态分组主机列表
  getHostByDynamicGroupId({ id }) {
    return Request.get(`${this.path}/dynamicGroup/${id}`);
  }

  // 根据节点id获取机器列表
  getHostByNode(params) {
    return Request.post(`${this.path}/host/node`, {
      params,
    });
  }

  // 获取节点详情
  getNodeInfo(params = {}) {
    return Request.post(`${this.path}/node/detail`, {
      params,
    });
  }

  // 获取节点拓扑路径
  getNodePath(params = {}) {
    return Request.post(`${this.path}/nodes/queryPath`, {
      params,
    });
  }

  // 根据输入 IP 获取机器信息
  getHostByHost(params) {
    return Request.post(`${this.path}/host/check`, {
      params,
    });
  }

  // 获取业务拓扑列表
  getAllTopology() {
    return Request.get(`${this.path}/topology`, {
      cache: 2000,
    });
  }

  // 获取业务拓扑列表（含各节点主机数）
  getAllTopologyWithCount() {
    return Request.post(`${this.path}/topology/hostCount`, {
      cache: 2000,
    });
  }

  // 获取业务拓扑主机列表（包含主机）
  getAllTopologyWithHost() {
    return Request.get(`${this.path}/topology/host`, {
      cache: 2000,
    });
  }

  // IP选择器根据拓扑节点集合获取机器列表(支持分页)
  getTopologyHost(params = {}) {
    return Request.post(`${this.path}/topology/hosts/nodes`, {
      params,
    });
  }

  // IP选择器根据拓扑节点集合获取机器列表（纯IP），返回IP格式为[cloudId:IP]
  getTopologyNodeAllHostId(params = {}) {
    return Request.post(`${this.path}/topology/hostIds/nodes`, {
      params,
    });
  }

  // IP选择器根据拓扑节点获取其子节点（仅获取一级，childs为空）
  getChildOfNode(params) {
    return Request.post(`${this.path}/topology/nodes/children`, {
      params,
    });
  }

  // 根据主机、拓扑节点、分组获取主机统计信息
  getHostStatistics(params = {}) {
    return Request.post(`${this.path}/host/statistics`, {
      params,
    });
  }

  // 获取多个节点下的主机统计信息
  getBatchNodeAgentStatistics(params = {}) {
    return Request.post(`${this.path}/host/agentStatistics/nodes`, {
      params,
    });
  }

  // 分页查询某个动态分组下的主机列表
  getDynamicGroupHost(params = {}) {
    return Request.post(`${this.path}/hosts/dynamicGroup`, {
      params,
    });
  }

  // 通过分组ID获取多个动态分组的详情信息
  getBatchGroupInfo(params = {}) {
    return Request.post(`${this.path}/hosts/dynamicGroup`, {
      params,
    });
  }

  // 获取多个动态分组下的主机Agent状态统计信息
  getBatchGroupAgentStatistics(params = {}) {
    return Request.post(`${this.path}/host/agentStatistics/dynamicGroups`, {
      params,
    });
  }

  getInputParseHostList(params = {}) {
    return Request.post(`${this.path}/host/check`, {
      params,
    });
  }

  // 根据hostId批量查询主机详情信息
  getHostInfoByHostId(params = {}) {
    return Request.post(`${this.path}/hosts/details`, {
      params,
    });
  }
}

export default new HostManage();
