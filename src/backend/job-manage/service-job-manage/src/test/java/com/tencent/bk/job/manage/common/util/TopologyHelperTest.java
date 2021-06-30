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

package com.tencent.bk.job.manage.common.util;

import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
import com.tencent.bk.job.manage.common.TopologyHelper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TopologyHelperTest {

    @Test
    public void testFindTopologyPath() {
        InstanceTopologyDTO treeNode = new InstanceTopologyDTO();
        treeNode.setObjectName("业务");
        treeNode.setObjectId("biz");
        treeNode.setInstanceId(2L);
        treeNode.setInstanceName("蓝鲸");
        InstanceTopologyDTO setNode = new InstanceTopologyDTO();
        setNode.setObjectName("集合");
        setNode.setObjectId("set");
        setNode.setInstanceId(3L);
        setNode.setInstanceName("测试集合");
        InstanceTopologyDTO moduleNode1 = new InstanceTopologyDTO();
        moduleNode1.setObjectName("模块");
        moduleNode1.setObjectId("module");
        moduleNode1.setInstanceId(4L);
        moduleNode1.setInstanceName("测试模块1");
        InstanceTopologyDTO moduleNode2 = new InstanceTopologyDTO();
        moduleNode2.setObjectName("模块");
        moduleNode2.setObjectId("module");
        moduleNode2.setInstanceId(5L);
        moduleNode2.setInstanceName("测试模块2");
        InstanceTopologyDTO moduleNode3 = new InstanceTopologyDTO();
        moduleNode3.setObjectName("模块");
        moduleNode3.setObjectId("module");
        moduleNode3.setInstanceId(6L);
        moduleNode3.setInstanceName("测试模块3");
        List<InstanceTopologyDTO> moduleList = new ArrayList<>();
        moduleList.add(moduleNode1);
        moduleList.add(moduleNode2);
        moduleList.add(moduleNode3);
        setNode.setChild(moduleList);
        List<InstanceTopologyDTO> setList = new ArrayList<>();
        setList.add(setNode);
        treeNode.setChild(setList);
        InstanceTopologyDTO targetNode1 = new InstanceTopologyDTO();
        targetNode1.setObjectId("module");
        targetNode1.setInstanceId(5L);
        InstanceTopologyDTO targetNode2 = new InstanceTopologyDTO();
        targetNode2.setObjectId("set");
        targetNode2.setInstanceId(3L);
        InstanceTopologyDTO targetNode3 = new InstanceTopologyDTO();
        targetNode3.setObjectId("module");
        targetNode3.setInstanceId(6L);
        InstanceTopologyDTO notExistsNode = new InstanceTopologyDTO();
        notExistsNode.setObjectId("module");
        notExistsNode.setInstanceId(15L);
        assertThat(TopologyHelper.findTopoPath(treeNode, targetNode1)).hasSize(3).element(0).isEqualTo(treeNode);
        assertThat(TopologyHelper.findTopoPath(treeNode, targetNode1)).hasSize(3).element(1).isEqualTo(setNode);
        assertThat(TopologyHelper.findTopoPath(treeNode, targetNode1)).hasSize(3).element(2).isEqualTo(moduleNode2);
        assertThat(TopologyHelper.findTopoPath(treeNode, notExistsNode)).isNull();
        List<InstanceTopologyDTO> targetNodes = new ArrayList<>();
        targetNodes.add(targetNode1);
        targetNodes.add(targetNode2);
        targetNodes.add(targetNode3);
        targetNodes.add(notExistsNode);
        List<List<InstanceTopologyDTO>> pathList = TopologyHelper.findTopoPaths(treeNode, targetNodes);
        for (List<InstanceTopologyDTO> path : pathList) {
            System.out.println(path);
        }
        assertThat(pathList).hasSize(4);
        assertThat(pathList).element(0).asList().hasSize(3);
        assertThat(pathList).element(1).asList().hasSize(2);
        assertThat(pathList).element(2).asList().hasSize(3);
        assertThat(pathList).element(3).isNull();
    }
}
