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

package com.tencent.bk.job.common.cc.util;

import com.tencent.bk.job.common.cc.model.BriefTopologyDTO;
import com.tencent.bk.job.common.cc.model.CcInstanceDTO;
import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class TopologyUtil {

    public static void printTopo(InstanceTopologyDTO appTopology) {
        printTopo("", appTopology);
    }

    public static void printTopo(String prefix, InstanceTopologyDTO appTopology) {
        if (appTopology == null) {
            log.info(prefix + "(null,null)");
            return;
        }
        log.info(prefix + "(" + appTopology.getObjectId() + "," + appTopology.getInstanceId() + ")");
        List<InstanceTopologyDTO> childs = appTopology.getChild();
        if (childs != null && !childs.isEmpty()) {
            childs.forEach(child -> printTopo(prefix + "\t", child));
        }
    }

    public static List<Long> findModuleIdsFromTopo(InstanceTopologyDTO appTopology) {
        List<Long> moduleIdList = new ArrayList<>();
        if (appTopology.getObjectId() != null && appTopology.getObjectId().equals("module")) {
            moduleIdList.add(appTopology.getInstanceId());
        } else {
            List<InstanceTopologyDTO> childList = appTopology.getChild();
            if (childList != null) {
                childList.forEach(child -> moduleIdList.addAll(findModuleIdsFromTopo(child)));
            }
        }
        return moduleIdList;
    }

    /**
     * 从业务拓扑树中找到某个节点
     *
     * @param appTopology   业务拓扑树
     * @param ccInstanceDTO 要查找的节点
     * @return 查找到的子拓扑树
     */
    public static InstanceTopologyDTO findNodeFromTopo(InstanceTopologyDTO appTopology, CcInstanceDTO ccInstanceDTO) {
        if (appTopology == null) {
            log.warn("appTopology is null, return null");
            return null;
        }
        if (ccInstanceDTO == null) {
            log.warn("appTopology is not null, but targetNode is null, return null");
            return null;
        }
        if (appTopology.getObjectId().equals(ccInstanceDTO.getObjectType())) {
            if (appTopology.getInstanceId().equals(ccInstanceDTO.getInstanceId())) {
                return appTopology;
            } else {
                return null;
            }
        } else {
            if (CollectionUtils.isNotEmpty(appTopology.getChild())) {
                for (InstanceTopologyDTO child : appTopology.getChild()) {
                    InstanceTopologyDTO resultNode = findNodeFromTopo(child, ccInstanceDTO);
                    if (resultNode != null) {
                        return resultNode;
                    }
                }
            }
            return null;
        }
    }

    public static InstanceTopologyDTO convert(BriefTopologyDTO.Node node) {
        if (node == null) return null;
        InstanceTopologyDTO instanceTopologyDTO = new InstanceTopologyDTO();
        instanceTopologyDTO.setObjectId(node.getObj());
        instanceTopologyDTO.setInstanceId((long) node.getId());
        instanceTopologyDTO.setInstanceName(node.getName());
        List<InstanceTopologyDTO> childList = new ArrayList<>();
        List<BriefTopologyDTO.Node> childNodeList = node.getChildNodeList();
        if (childNodeList != null && !childNodeList.isEmpty()) {
            for (BriefTopologyDTO.Node childNode : childNodeList) {
                childList.add(convert(childNode));
            }
        }
        instanceTopologyDTO.setChild(childList);
        return instanceTopologyDTO;
    }

    public static InstanceTopologyDTO convert(BriefTopologyDTO briefTopologyDTO) {
        if (briefTopologyDTO == null || briefTopologyDTO.getBiz() == null) return null;
        // 构造业务节点
        InstanceTopologyDTO instanceTopologyDTO = new InstanceTopologyDTO();
        instanceTopologyDTO.setObjectId("biz");
        instanceTopologyDTO.setObjectName("业务");
        instanceTopologyDTO.setInstanceId(briefTopologyDTO.getBiz().getBizId());
        instanceTopologyDTO.setInstanceName(briefTopologyDTO.getBiz().getBizName());
        List<BriefTopologyDTO.Node> idleNodeList = briefTopologyDTO.getIdleNodeList();
        List<BriefTopologyDTO.Node> childNodeList = briefTopologyDTO.getChildNodeList();
        List<InstanceTopologyDTO> childList = new ArrayList<>();
        if (idleNodeList != null && !idleNodeList.isEmpty()) {
            for (BriefTopologyDTO.Node idleNode : idleNodeList) {
                childList.add(convert(idleNode));
            }
        }
        if (childNodeList != null && !childNodeList.isEmpty()) {
            for (BriefTopologyDTO.Node childNode : childNodeList) {
                childList.add(convert(childNode));
            }
        }
        instanceTopologyDTO.setChild(childList);
        return instanceTopologyDTO;
    }

    private static String getKey(InstanceTopologyDTO node) {
        return node.getObjectId() + "_" + node.getInstanceId();
    }

    /**
     * 将低一级的子节点合并入拓扑树中
     *
     * @param tp    拓扑树
     * @param child 要合入的子节点
     * @return 合入后的拓扑树
     */
    private static InstanceTopologyDTO mergeChildIntoTopology(InstanceTopologyDTO tp, InstanceTopologyDTO child) {
        if (child == null) {
            return tp;
        }
        Set<String> tpChildSet = tp.getChild().stream().map(TopologyUtil::getKey).collect(Collectors.toSet());
        if (tpChildSet.contains(getKey(child))) {
            //找到tp1中这个child进行合并更新
            for (int i = 0; i < tp.getChild().size(); i++) {
                if (tp.getChild().get(i).getInstanceId().equals(child.getInstanceId())) {
                    tp.getChild().set(i, mergeTopology(tp.getChild().get(i), child));
                    break;
                }
            }
        } else {
            tp.getChild().add(child);
        }
        return tp;
    }

    /**
     * 合并两颗层级相差小于2的拓扑树
     *
     * @param topologyDTOs 要合并的拓扑树数组
     * @return 合并后的拓扑树
     */
    public static InstanceTopologyDTO mergeTopology(InstanceTopologyDTO... topologyDTOs) {
        try {
            return mergeTopologyIndeed(topologyDTOs);
        } catch (Exception e) {
            for (InstanceTopologyDTO topologyDTO : topologyDTOs) {
                TopologyUtil.printTopo(topologyDTO);
                log.info("==============================");
            }
            throw new RuntimeException("fail to mergeTopology", e);
        }
    }

    private static InstanceTopologyDTO mergeTopologyIndeed(InstanceTopologyDTO... topologyDTOs) {
        Map<String, Integer> weightMap = new HashMap<>();
        weightMap.put("biz", 1);
        weightMap.put("set", 2);
        weightMap.put("module", 3);
        if (topologyDTOs.length == 1) {
            return topologyDTOs[0];
        } else if (topologyDTOs.length == 2) {
            InstanceTopologyDTO tp1 = topologyDTOs[0];
            InstanceTopologyDTO tp2 = topologyDTOs[1];
            if (tp1 == null) {
                return tp2;
            }
            if (tp2 == null) {
                return tp1;
            }
            //根节点层级相同
            if (tp1.getObjectId().equals(tp2.getObjectId())) {
                //但实例不同，无法合并
                if (!tp1.getInstanceId().equals(tp2.getInstanceId())) {
                    throw new RuntimeException("can not merge different instances of same level");
                } else {
                    if (CollectionUtils.isNotEmpty(tp2.getChild())) {
                        for (InstanceTopologyDTO child2 : tp2.getChild()) {
                            tp1 = mergeChildIntoTopology(tp1, child2);
                        }
                    }
                    return tp1;
                }
            } else if (Math.abs(weightMap.get(tp1.getObjectId()) - weightMap.get(tp2.getObjectId())) >= 2) {
                //相差两级及以上无法合并
                throw new RuntimeException("can not merge different instances beyond 2 levels");
            } else {
                //根节点相差一级
                if (weightMap.get(tp1.getObjectId()) < weightMap.get(tp2.getObjectId())) {
                    //tp2往tp1中合入
                    tp1 = mergeChildIntoTopology(tp1, tp2);
                    return tp1;
                } else {
                    //交换顺序再合并
                    return mergeTopology(tp2, tp1);
                }
            }
        } else {
            return mergeTopology(mergeTopology(Arrays.copyOf(topologyDTOs, topologyDTOs.length - 1)),
                topologyDTOs[topologyDTOs.length - 1]);
        }
    }

}
