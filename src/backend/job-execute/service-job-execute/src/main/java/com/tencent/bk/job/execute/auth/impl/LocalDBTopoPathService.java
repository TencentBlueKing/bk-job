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

package com.tencent.bk.job.execute.auth.impl;

import com.google.common.collect.Lists;
import com.tencent.bk.job.common.util.ConcurrencyUtil;
import com.tencent.bk.job.manage.api.inner.ServiceHostResource;
import com.tencent.bk.job.manage.model.inner.request.ServiceBatchGetHostToposReq;
import com.tencent.bk.job.manage.model.inner.resp.ServiceHostTopoDTO;
import com.tencent.bk.sdk.iam.service.TopoPathService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * 主机拓扑路径服务，根据主机ID从本地DB缓存数据获取主机拓扑路径
 */
@Slf4j
@Service("localDBTopoPathService")
public class LocalDBTopoPathService implements TopoPathService {

    private final ServiceHostResource hostResource;
    private final ExecutorService executorService;

    @Autowired
    public LocalDBTopoPathService(@Lazy ServiceHostResource serviceHostResource,
                                  @Qualifier("getHostTopoPathExecutor")
                                  ExecutorService executorService) {
        this.hostResource = serviceHostResource;
        this.executorService = executorService;
    }

    @Override
    public Map<String, List<String>> getTopoPathByHostIds(Set<String> hostIds) {
        if (CollectionUtils.isEmpty(hostIds)) {
            return Collections.emptyMap();
        }
        List<Long> hostIdList = hostIds.stream().map(Long::parseLong).collect(Collectors.toList());
        // 分批从job-manage查询，每次最多查询5000台主机
        int batchSize = 5000;
        List<List<Long>> hostIdsSubList = Lists.partition(hostIdList, batchSize);
        // 多线程并发拉取
        List<ServiceHostTopoDTO> hostTopoList = ConcurrencyUtil.getResultWithThreads(
            hostIdsSubList,
            executorService,
            (subList) -> hostResource.batchGetHostTopos(new ServiceBatchGetHostToposReq(subList)).getData()
        );
        Map<String, List<String>> hostTopoPathMap = new HashMap<>();
        hostTopoList.forEach(hostTopoDTO -> {
            String hostIdStr = hostTopoDTO.getHostId().toString();
            if (!hostTopoPathMap.containsKey(hostIdStr)) {
                hostTopoPathMap.put(hostIdStr, new ArrayList<>());
            }
            hostTopoPathMap.get(hostIdStr).add(buildTopoPath(hostTopoDTO));
        });
        return hostTopoPathMap;
    }

    private String buildTopoPath(ServiceHostTopoDTO hostTopo) {
        return "/biz," + hostTopo.getBizId() +
            "/set," + hostTopo.getSetId()
            + "/module," + hostTopo.getModuleId() + "/";
    }
}
