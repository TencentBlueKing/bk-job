/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.bk.job.common.model.dto;

import com.tencent.bk.job.common.annotation.PersistenceObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 容器拓扑路径（Web 入口的内部流转 / 持久化形态）。
 * <p>
 * 一条 topo 表示一个精确的 cluster→namespace→workload 路径：cluster 必填，namespace / workload 可选。
 * 只持久化 CMDB 内部 ID：ID 用于运行时 CMDB 查询；展示名不落库，回显时前端拿 id 走别的接口查名。
 * 运行时对每条 topo 取其最精细的已选节点（workload → namespace → cluster）作为拓扑节点。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@PersistenceObject
public class KubeTopoDTO implements Cloneable {

    private KubeClusterObjectDTO cluster;
    private KubeNamespaceObjectDTO namespace;
    private KubeWorkloadObjectDTO workload;

    @Override
    public KubeTopoDTO clone() {
        KubeTopoDTO clone = new KubeTopoDTO();
        if (cluster != null) {
            clone.setCluster(cluster.clone());
        }
        if (namespace != null) {
            clone.setNamespace(namespace.clone());
        }
        if (workload != null) {
            clone.setWorkload(workload.clone());
        }
        return clone;
    }
}
