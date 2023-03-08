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
 * @desc 遍历查找step中使用到的全局变量
 * @param { Array } stepList
 * @returns { Array } 被使用的全局变量
 */
export const findUsedVariable = (stepList) => {
  const variableSet = new Set();
  stepList.forEach((step) => {
    step.refVariables.forEach((variableName) => {
      variableSet.add(variableName);
    });
  });

  return [
    ...variableSet,
  ];
};

/**
 * @desc 通过路由检测是否是功能脚本名模块
 * @param { Object } route
 * @returns { Boolean }
 */
export const checkPublicScript = (route) => {
  const { meta } = route;
  if (!meta) {
    return false;
  }
  if (meta.public) {
    return true;
  }
  return false;
};

/**
 * @desc 比较主机节点
 * @param { Object } preHost
 * @param { Object } nextHost
 * @returns { Boolean }
 */
export const compareHost = (preHost, nextHost) => {
  // 全都使用了主机变量
  if (nextHost.variable && nextHost.variable === preHost.variable) {
    return true;
  }
  // 服务文件主机手动添加
  // 目标服务器主机使用主机变量
  if (nextHost.variable) {
    return false;
  }
  // 全都手动添加对比值
  const {
    hostList: preIPList,
    nodeList: preNodeList,
    dynamicGroupList: preGroupList,
  } = preHost.hostNodeInfo;
  const {
    hostList: nextIPList,
    nodeList: nextNodeList,
    dynamicGroupList: nextGroupList,
  } = nextHost.hostNodeInfo;
    // 对比主机
  if (preIPList.length !== nextIPList.length) {
    return false;
  }
  const preIPMap = preIPList.reduce((result, host) => {
    result[host.hostId] = true;
    return result;
  }, {});
    // eslint-disable-next-line no-plusplus
  for (let i = 0; i < nextIPList.length; i++) {
    if (!preIPMap[nextIPList[i].hostId]) {
      return false;
    }
  }
  // 对比节点
  if (preNodeList.length !== nextNodeList.length) {
    return false;
  }
  const genNodeKey = node => `#${node.id}#${node.type}`;
  const taretNodeMap = preNodeList.reduce((result, node) => {
    result[genNodeKey(node)] = true;
    return result;
  }, {});
    // eslint-disable-next-line no-plusplus
  for (let i = 0; i < nextNodeList.length; i++) {
    if (!taretNodeMap[genNodeKey(nextNodeList[i])]) {
      return false;
    }
  }
  // 对比分组
  if (preGroupList.length !== nextGroupList.length) {
    return false;
  }
  const preGroupMap = preGroupList.reduce((result, groupId) => {
    result[groupId] = true;
    return result;
  }, {});
    // eslint-disable-next-line no-plusplus
  for (let i = 0; i < nextGroupList.length; i++) {
    if (!preGroupMap[nextGroupList[i]]) {
      return false;
    }
  }
  return true;
};

/**
 * @desc 检测分发文件源路径重复
 * @param { Array } fileSourceList
 * @returns { Boolean }
 */
export const detectionSourceFileDupLocation = (fileSourceList) => {
  const fileLocationMap = {};
  const pathReg = /([^/]+\/?)\*?$/;
  // 路径中以 * 结尾表示分发所有文件，可能和分发具体文件冲突
  let hasDirAllFile = false;
  let hasFile = false;
  // eslint-disable-next-line no-plusplus
  for (let i = 0; i < fileSourceList.length; i++) {
    const currentFileSource = fileSourceList[i];
    // eslint-disable-next-line no-plusplus
    for (let j = 0; j < currentFileSource.fileLocation.length; j++) {
      const currentFileLocation = currentFileSource.fileLocation[j];
      // 分发所有文件
      if (/\*$/.test(currentFileLocation)) {
        hasDirAllFile = true;
        if (hasFile) {
          return true;
        }
        continue;
      }
      // 分发具体的文件
      if (!/(\/|(\/\*))$/.test(currentFileLocation)) {
        hasFile = true;
        if (hasDirAllFile) {
          return true;
        }
      }
      const pathMatch = currentFileLocation.match(pathReg);
      if (pathMatch) {
        if (fileLocationMap[pathMatch[1]]) {
          return true;
        }
        fileLocationMap[pathMatch[1]] = 1;
      }
    }
  }
  return false;
};
