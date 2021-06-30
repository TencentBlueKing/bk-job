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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
            childs.forEach(child -> {
                printTopo(prefix + "\t", child);
            });
        }
    }

    public static List<Long> findModuleIdsFromTopo(InstanceTopologyDTO appTopology) {
        List<Long> moduleIdList = new ArrayList<>();
        if (appTopology.getObjectId() != null && appTopology.getObjectId().equals("module")) {
            moduleIdList.add(appTopology.getInstanceId());
        } else {
            List<InstanceTopologyDTO> childList = appTopology.getChild();
            if (childList != null) {
                childList.forEach(child -> {
                    moduleIdList.addAll(findModuleIdsFromTopo(child));
                });
            }
        }
        return moduleIdList;
    }

    /**
     * 从业务拓扑树中找到某个节点
     *
     * @param appTopology
     * @param ccInstanceDTO
     * @return
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
            if (appTopology.getChild() == null || appTopology.getChild().isEmpty()) {
                return null;
            } else {
                for (InstanceTopologyDTO child : appTopology.getChild()) {
                    InstanceTopologyDTO resultNode = findNodeFromTopo(child, ccInstanceDTO);
                    if (resultNode != null) {
                        return resultNode;
                    }
                }
                return null;
            }
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

    public static void main(String[] args) {
        InstanceTopologyDTO appTopology = new InstanceTopologyDTO();
        appTopology.setObjectId("biz");
        appTopology.setInstanceId(2L);
        List<InstanceTopologyDTO> childList = new ArrayList<>();
        InstanceTopologyDTO child1 = new InstanceTopologyDTO();
        child1.setObjectId("set");
        child1.setInstanceId(3L);
        childList.add(child1);
        InstanceTopologyDTO child2 = new InstanceTopologyDTO();
        child2.setObjectId("set");
        child2.setInstanceId(4L);
        InstanceTopologyDTO child3 = new InstanceTopologyDTO();
        child3.setObjectId("module");
        child3.setInstanceId(5L);
        child2.setChild(Collections.singletonList(child3));
        childList.add(child2);
        appTopology.setChild(childList);
        InstanceTopologyDTO child4 = new InstanceTopologyDTO();
        child4.setObjectId("set");
        child4.setInstanceId(6L);
        InstanceTopologyDTO child5 = new InstanceTopologyDTO();
        child5.setObjectId("module");
        child5.setInstanceId(7L);
        child4.setChild(Collections.singletonList(child5));
        childList.add(child4);
        appTopology.setChild(childList);
        printTopo(appTopology);
    }
}
