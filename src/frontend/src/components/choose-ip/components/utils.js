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

/**
 * @desc 生成主机的唯一标识 KEY
 * @param { Object } host
 * @returns { String }
 */
export const generateHostRealId = host => `${host.cloudAreaInfo.id}:${host.ip}`;

/**
 * @desc 从节点的唯一标识中解析节点的信息
 * @param { String } id
 * @returns { Array }
 */
export const parseIdInfo = id => id.match(/^#([^#]+)#(.+)$/).slice(1);

/**
 * @desc 遍历 topo 数据构造 bk-big-tree 需要的数据结构
 * @param { Array } target api 返回 topo 数据
 * @param { Number } level 层级
 * @returns { Boolean }
 */
export const bigTreeTransformTopologyOfTopology = (target, level = 0) => {
  if (!target || target.length < 1) {
    return [];
  }

  return target.map((item) => {
    const { instanceId, instanceName, child, objectId, count } = item;

    const children = bigTreeTransformTopologyOfTopology(child, level + 1);

    return Object.freeze({
      id: `#${objectId}#${instanceId}`,
      name: instanceName,
      level,
      children,
      payload: {
        count,
      },
    });
  });
};

/**
 * @desc 过滤 topo 数据
 * @param { Array } topologyTreeData topo 数据
 * @param { Boolean } showEmpty 是否显示主机数为空的 topo 节点
 * @returns { Array }
 */
export const filterTopology = (topologyTreeData, showEmpty = true) => {
  if (!topologyTreeData || topologyTreeData.length < 1) {
    return [];
  }
  if (showEmpty) {
    return topologyTreeData;
  }
  return topologyTreeData.reduce((result, item) => {
    if (item.level === 0) {
      result.push({
        ...item,
        children: filterTopology(item.children, showEmpty),
      });
    } else if (item.level > 0 && item.payload.count > 0) {
      result.push({
        ...item,
        children: filterTopology(item.children, showEmpty),
      });
    }
    return result;
  }, []);
};

/**
 * @desc 统计主机的状态信息。总数、有效主机数、无效主机数
 * @param { Boolean } name
 * @returns { Boolean }
 */
export const statisticsHost = (list) => {
  // 主机的唯一标记是ip + 云区域
  const total = list.length;
  let fail = 0;
  // eslint-disable-next-line no-plusplus
  for (let i = 0; i < total; i++) {
    const { alive } = list[i];
    if (alive !== 1) {
      // eslint-disable-next-line no-plusplus
      fail++;
    }
  }
  const success = total - fail;
  return {
    total,
    success,
    fail,
  };
};

/**
 * @desc 获取指定节点下面的所有节点
 * @param { Object } node
 * @returns { Array }
 */
export const findAllChildNodeId = (node) => {
  const childs = [];
  if (node.children) {
    node.children.forEach((curNode) => {
      childs.push(curNode.id);
      childs.push(...findAllChildNodeId(curNode));
    });
  }
  return childs;
};

/**
 * @desc 合并手动输入的主机 IP 到已选的静态 IP中
 * @param { Array } first 已选的主机列表
 * @param { Array } second 手动输入的主机列表
 * @returns { Boolean }
 */
export const mergeInputHost = (first, second) => {
  const result = [...first];
  const firstHostIdMap = first.reduce((result, ipInfo) => {
    result[ipInfo.hostId] = true;
    return result;
  }, {});
  second.forEach((ipInfo) => {
    if (!firstHostIdMap[ipInfo.hostId]) {
      result.push(ipInfo);
    }
  });

  return result;
};

export const mergeTopologyHost = (target, preList, lastList) => {
  const preHostIdMap = preList.reduce((result, ipInfo) => {
    result[ipInfo.hostId] = true;
    return result;
  }, {});

  const result = [];
  const resultMemo = {};
  // 删除 target 中 preList 相关的数据
  target.forEach((ipInfo) => {
    if (!preHostIdMap[ipInfo.hostId]) {
      result.push(ipInfo);
      resultMemo[ipInfo.hostId] = true;
    }
  });
  // 追加 lastList 数据
  lastList.forEach((ipInfo) => {
    if (!resultMemo[ipInfo.hostId]) {
      result.push(ipInfo);
    }
  });

  return result;
};

/**
 * @desc 遍历 topo 数据执行 calllback
 * @param { Array } topoList
 * @param { Function } calllback
 */
export const resetTree = (topoList, calllback) => {
  if (!topoList || topoList.length < 1) {
    return;
  }

  topoList.forEach((curNode) => {
    calllback(curNode);
    resetTree(curNode.children, calllback);
  });
};

/**
 * @desc 主机列表排序
 * @param { Array } hostList
 * @returns { Array }
 */
export const sortHost = (hostList) => {
  const hostMap = {};
  hostList.forEach((item) => {
    const currentIp = item.ip;
    if (!hostMap[currentIp]) {
      hostMap[currentIp] = 1;
    } else {
      hostMap[currentIp] += 1;
    }
  });

  // map以ip做为key因为在同一个分组里面需要根据ip排序
  const repeatMap = {};
  const uniqueFailMap = {};
  const uniqueNormalMap = {};
  hostList.forEach((currentHostInfo) => {
    const repeat = hostMap[currentHostInfo.ip] > 1;
    const realHost = Object.assign({
      repeat,
    }, currentHostInfo);
    // 对主机进行分组
    const currentIp = currentHostInfo.ip;
    if (repeat) {
      // 重复ip为一组，对应值为数组（多个主机有相同的ip）
      if (!repeatMap[currentIp]) {
        repeatMap[currentIp] = [];
      }
      repeatMap[currentIp].push(realHost);
    } else {
      if (realHost.alive) {
        // 正常非重复主机为一组
        uniqueNormalMap[currentIp] = realHost;
      } else {
        // 异常非重复主机为一组
        uniqueFailMap[currentIp] = realHost;
      }
    }
  });
  const result = [];

  Object.keys(repeatMap).forEach((ip) => {
    const currentRepeatList = repeatMap[ip].sort((pre, next) => pre.alive - next.alive);
    result.push(...currentRepeatList);
  });

  Object.values(uniqueFailMap).forEach((value) => {
    result.push(value);
  });

  Object.values(uniqueNormalMap).forEach((value) => {
    result.push(value);
  });

  return result;
};
