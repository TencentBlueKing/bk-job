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

export const generateHostRealId = host => `${host.cloudAreaInfo.id}:${host.ip}`;
export const parseIdInfo = id => id.match(/^#([^#]+)#(.+)$/).slice(1);

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

export const mergeInputHost = (first, second) => {
    const result = [];
    const firstHostReadIdMap = {};
    first.forEach((item) => {
        result.push(item);
        firstHostReadIdMap[item.realId] = true;
    });
    second.forEach((item) => {
        if (!firstHostReadIdMap[item.realId]) {
            result.push(item);
        }
    });
    
    return result;
};

export const mergeTopologyHost = (target, preList, lastList) => {
    const lastListMap = {};
    lastList.forEach((item) => {
        lastListMap[item.realId] = true;
    });
    
    // 如果是删除操作
    if (preList.length > lastList.length) {
    // 找到删除的主机
        const deleteHostRealIdMap = {};
        preList.forEach((item) => {
            const currentHostRealId = item.realId;
            if (!lastListMap[currentHostRealId]) {
                deleteHostRealIdMap[currentHostRealId] = true;
            }
        });
        const result = [];
        // 删除操作
        target.forEach((item) => {
            if (!deleteHostRealIdMap[item.realId]) {
                result.push(item);
            }
        });
        return result;
    }
    
    // 添加主机——和IP输入逻辑相同
    return mergeInputHost(target, lastList);
};

export const resetTree = (target, calllback) => {
    if (!target || target.length < 1) {
        return;
    }

    target.forEach((curNode) => {
        calllback(curNode);
        resetTree(curNode.children, calllback);
    });
};

export const toggleActive = (target, active, value) => {
    const arrayAdd = (list, value) => [
        ...new Set([
            ...list, value,
        ]),
    ];
    const arrayRemove = (list, value) => {
        const s = new Set(list);
        s.delete(value);
        return [
            ...s,
        ];
    };
    if (target.length > 0) {
        return arrayAdd(active, value);
    }
    return arrayRemove(active, value);
};

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
    hostList.forEach((currentHost) => {
        const repeat = hostMap[currentHost.ip] > 1;
        const realHost = Object.assign({
            realId: generateHostRealId(currentHost),
            repeat,
        }, currentHost);
        // 对主机进行分组
        const currentIp = currentHost.ip;
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
