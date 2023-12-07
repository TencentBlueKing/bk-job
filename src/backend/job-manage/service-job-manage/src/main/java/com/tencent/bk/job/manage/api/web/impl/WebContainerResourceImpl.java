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

package com.tencent.bk.job.manage.api.web.impl;

import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.manage.api.web.WebContainerResource;
import com.tencent.bk.job.manage.model.web.request.chooser.ListTopologyTreesReq;
import com.tencent.bk.job.manage.model.web.request.chooser.container.ContainerCheckReq;
import com.tencent.bk.job.manage.model.web.request.chooser.container.ContainerDetailReq;
import com.tencent.bk.job.manage.model.web.request.chooser.container.ContainerIdWithMeta;
import com.tencent.bk.job.manage.model.web.request.chooser.container.ListContainerByTopologyNodesReq;
import com.tencent.bk.job.manage.model.web.vo.chooser.container.ContainerTopologyNodeVO;
import com.tencent.bk.job.manage.model.web.vo.chooser.container.ContainerVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
public class WebContainerResourceImpl implements WebContainerResource {
    @Override
    public Response<List<ContainerTopologyNodeVO>> listTopologyTrees(String username,
                                                                     AppResourceScope appResourceScope,
                                                                     String scopeType,
                                                                     String scopeId,
                                                                     ListTopologyTreesReq req) {
        List<ContainerTopologyNodeVO> workloads = new ArrayList<>();
        ContainerTopologyNodeVO workload = new ContainerTopologyNodeVO();
        workload.setInstanceId(1L);
        workload.setInstanceName("bk-job-execute");
        workload.setObjectId("deployment");
        workload.setObjectName("deployment");
        workload.setCount(1);
        workloads.add(workload);

        List<ContainerTopologyNodeVO> namespaces = new ArrayList<>();
        ContainerTopologyNodeVO namespace = new ContainerTopologyNodeVO();
        namespace.setInstanceId(1L);
        namespace.setInstanceName("bk-job-dev");
        namespace.setObjectId("namespace");
        namespace.setObjectName("namespace");
        namespace.setCount(1);
        namespace.setChild(workloads);
        namespaces.add(namespace);

        List<ContainerTopologyNodeVO> clusters = new ArrayList<>();
        ContainerTopologyNodeVO cluster = new ContainerTopologyNodeVO();
        cluster.setInstanceId(1L);
        cluster.setInstanceName("BCS-K8S-12312");
        cluster.setObjectId("cluster");
        cluster.setObjectName("cluster");
        cluster.setCount(1);
        cluster.setChild(namespaces);
        clusters.add(cluster);

        ContainerTopologyNodeVO topo = new ContainerTopologyNodeVO();
        topo.setInstanceId(2L);
        topo.setInstanceName("蓝鲸");
        topo.setObjectId("biz");
        topo.setObjectName("业务");
        topo.setCount(1);
        topo.setChild(clusters);

        return Response.buildSuccessResp(
            Collections.singletonList(
                topo
            )
        );
    }

    @Override
    public Response<PageData<ContainerVO>> listContainerByTopologyNodes(String username,
                                                                        AppResourceScope appResourceScope,
                                                                        String scopeType,
                                                                        String scopeId,
                                                                        ListContainerByTopologyNodesReq req) {
        ContainerVO containerVO = new ContainerVO();
        containerVO.setId(1L);
        containerVO.setName("job-execute");
        containerVO.setPodName("bk-job-execute-6c5c88cdb9-pwthx");
        containerVO.setUid("docker://076f9622ff3f2f6e0822dc1ae7b0c26c8e451110f75aec0908349bd923dfce5c");
        Map<String, String> podLabels = new HashMap<>();
        podLabels.put("app.kubernetes.io/component", "job-execute");
        podLabels.put("app.kubernetes.io/instance", "bk-job");
        containerVO.setPodLabels(podLabels);

        PageData<ContainerVO> result = new PageData<>();
        result.setData(Collections.singletonList(containerVO));
        result.setStart(0);
        result.setTotal(1L);
        return Response.buildSuccessResp(result);
    }

    @Override
    public Response<PageData<ContainerIdWithMeta>> listContainerIdByTopologyNodes(
        String username,
        AppResourceScope appResourceScope,
        String scopeType,
        String scopeId,
        ListContainerByTopologyNodesReq req) {

        ContainerIdWithMeta containerIdWithMeta = new ContainerIdWithMeta();
        containerIdWithMeta.setContainerId(1L);

        PageData<ContainerIdWithMeta> result = new PageData<>();
        result.setData(Collections.singletonList(containerIdWithMeta));
        result.setStart(0);
        result.setTotal(1L);

        return Response.buildSuccessResp(result);
    }

    @Override
    public Response<List<ContainerVO>> checkContainers(String username,
                                                       AppResourceScope appResourceScope,
                                                       String scopeType,
                                                       String scopeId,
                                                       ContainerCheckReq req) {
        ContainerVO containerVO = new ContainerVO();
        containerVO.setId(1L);
        containerVO.setName("job-execute");
        containerVO.setPodName("bk-job-execute-6c5c88cdb9-pwthx");
        containerVO.setUid("docker://076f9622ff3f2f6e0822dc1ae7b0c26c8e451110f75aec0908349bd923dfce5c");
        Map<String, String> podLabels = new HashMap<>();
        podLabels.put("app.kubernetes.io/component", "job-execute");
        podLabels.put("app.kubernetes.io/instance", "bk-job");
        containerVO.setPodLabels(podLabels);
        return Response.buildSuccessResp(Collections.singletonList(containerVO));
    }

    @Override
    public Response<List<ContainerVO>> getContainerDetails(String username,
                                                           AppResourceScope appResourceScope,
                                                           String scopeType,
                                                           String scopeId,
                                                           ContainerDetailReq req) {
        ContainerVO containerVO = new ContainerVO();
        containerVO.setId(1L);
        containerVO.setName("job-execute");
        containerVO.setPodName("bk-job-execute-6c5c88cdb9-pwthx");
        containerVO.setUid("docker://076f9622ff3f2f6e0822dc1ae7b0c26c8e451110f75aec0908349bd923dfce5c");
        Map<String, String> podLabels = new HashMap<>();
        podLabels.put("app.kubernetes.io/component", "job-execute");
        podLabels.put("app.kubernetes.io/instance", "bk-job");
        containerVO.setPodLabels(podLabels);
        return Response.buildSuccessResp(Collections.singletonList(containerVO));
    }
}
