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

package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.job.common.cc.config.CmdbConfig;
import com.tencent.bk.job.common.cc.model.InstanceTopologyDTO;
import com.tencent.bk.job.common.cc.model.req.GetTopoNodePathReq;
import com.tencent.bk.job.common.cc.sdk.BizCmdbClient;
import com.tencent.bk.job.common.esb.config.BkApiConfig;
import com.tencent.bk.job.common.gse.service.QueryAgentStatusClient;
import com.tencent.bk.job.common.util.CustomCollectionUtils;
import com.tencent.bk.job.execute.model.DynamicServerTopoNodeDTO;
import com.tencent.bk.job.execute.service.TopoService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@DependsOn({"cmdbConfigSetter"})
@Service
@Slf4j
public class TopoServiceImpl implements TopoService {
    private final BizCmdbClient ccClient;

    @Autowired
    public TopoServiceImpl(
            BkApiConfig bkApiConfig,
            CmdbConfig cmdbConfig,
            QueryAgentStatusClient queryAgentStatusClient,
            MeterRegistry meterRegistry
    ) {
        ccClient = new BizCmdbClient(bkApiConfig, cmdbConfig, queryAgentStatusClient, meterRegistry);
    }

    @Override
    public List<InstanceTopologyDTO> batchGetTopoNodeHierarchy(long appId, List<DynamicServerTopoNodeDTO> topoNodes) {
        GetTopoNodePathReq req = new GetTopoNodePathReq();
        req.setBizId(appId);
        topoNodes.forEach(topoNode -> req.add(topoNode.getNodeType(), topoNode.getTopoNodeId()));
        List<InstanceTopologyDTO> hierarchyNodes = ccClient.getTopoInstancePath(req);
        log.debug("Get topo node hierarchy, req:{}, result:{}", req, hierarchyNodes);
        if (CustomCollectionUtils.isEmptyCollection(hierarchyNodes)) {
            return Collections.emptyList();
        }
        return hierarchyNodes;
    }
}
