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

package com.tencent.bk.job.execute.auth.impl;

import com.google.common.collect.Lists;
import com.tencent.bk.job.common.cc.model.result.HostBizRelationDTO;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.util.ConcurrencyUtil;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.sdk.iam.service.TopoPathService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
 * 主机拓扑路径服务，根据主机ID从CMDB接口获取主机拓扑路径
 */
@Slf4j
@Service("cmdbTopoPathService")
public class CMDBTopoPathService implements TopoPathService {

    private final IBizCmdbClient bizCmdbClient;
    private final ExecutorService executorService;

    @Autowired
    public CMDBTopoPathService(IBizCmdbClient bizCmdbClient,
                               @Qualifier("getHostTopoPathExecutor")
                               ExecutorService executorService) {
        this.bizCmdbClient = bizCmdbClient;
        this.executorService = executorService;
    }

    @Override
    public Map<String, List<String>> getTopoPathByHostIds(Set<String> hostIds) {
        List<Long> hostIdList = hostIds.stream().map(Long::parseLong).collect(Collectors.toList());
        // CMDB接口限制每次最多查询500台主机
        int batchSize = 500;
        List<List<Long>> hostIdsSubList = Lists.partition(hostIdList, batchSize);
        // 多线程并发拉取
        List<HostBizRelationDTO> hostBizRelationDTOList = ConcurrencyUtil.getResultWithThreads(
            hostIdsSubList,
            executorService,
            pHostIdList ->
                bizCmdbClient.findHostBizRelations(
                    JobContextUtil.getTenantId(),
                    pHostIdList
                )
        );
        return buildTopoPathMap(hostBizRelationDTOList);
    }

    private Map<String, List<String>> buildTopoPathMap(List<HostBizRelationDTO> hostBizRelationDTOList) {
        if (CollectionUtils.isEmpty(hostBizRelationDTOList)) {
            return Collections.emptyMap();
        }
        Map<String, List<String>> topoPathMap = new HashMap<>();
        hostBizRelationDTOList.forEach(hostBizRelationDTO -> {
            Long hostId = hostBizRelationDTO.getHostId();
            Long bizId = hostBizRelationDTO.getBizId();
            Long setId = hostBizRelationDTO.getSetId();
            Long moduleId = hostBizRelationDTO.getModuleId();
            String topoPath = buildTopoPath(bizId, setId, moduleId);
            String hostIdKey = hostId.toString();
            if (!topoPathMap.containsKey(hostIdKey)) {
                topoPathMap.put(hostIdKey, new ArrayList<>());
            }
            topoPathMap.get(hostIdKey).add(topoPath);
        });
        return topoPathMap;
    }

    private String buildTopoPath(Long bizId, Long setId, Long moduleId) {
        return "/" + ResourceTypeId.BIZ + "," + bizId
            + "/" + ResourceTypeId.SET + "," + setId
            + "/" + ResourceTypeId.MODULE + "," + moduleId
            + "/";
    }
}
