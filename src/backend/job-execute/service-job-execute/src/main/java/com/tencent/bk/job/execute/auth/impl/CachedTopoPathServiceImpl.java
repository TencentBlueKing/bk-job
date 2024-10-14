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

import com.tencent.bk.sdk.iam.service.TopoPathService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class CachedTopoPathServiceImpl implements TopoPathService {

    private final TopoPathService delegate;
    private final HostTopoPathCache hostTopoPathCache;

    public CachedTopoPathServiceImpl(TopoPathService delegate, HostTopoPathCache hostTopoPathCache) {
        this.delegate = delegate;
        this.hostTopoPathCache = hostTopoPathCache;
    }

    @Override
    public Map<String, List<String>> getTopoPathByHostIds(Set<String> hostIds) {
        // 1.优先从缓存中获取拓扑路径
        List<Long> hostIdList = hostIds.stream().map(Long::parseLong).collect(Collectors.toList());
        List<HostTopoPathEntry> hostTopoPathEntryList = hostTopoPathCache.batchGetHostTopoPathByHostIds(hostIdList);
        if (CollectionUtils.isEmpty(hostTopoPathEntryList)) {
            if (log.isDebugEnabled()) {
                log.debug("Get empty hostTopoPath from cache, hostIds={}", hostIds);
            }
            return delegate.getTopoPathByHostIds(hostIds);
        }
        if (log.isDebugEnabled()) {
            log.debug(
                "Get {} hostTopoPath from cache, hostTopoPathEntryList={}",
                hostTopoPathEntryList.size(),
                hostTopoPathEntryList
            );
        }
        Map<String, List<String>> hostTopoPathMap = new HashMap<>(hostIds.size());
        Set<String> cachedHostIds = new HashSet<>();
        for (HostTopoPathEntry hostTopoPathEntry : hostTopoPathEntryList) {
            if (hostTopoPathEntry == null) {
                continue;
            }
            String hostIdStr = hostTopoPathEntry.getHostId().toString();
            cachedHostIds.add(hostIdStr);
            hostTopoPathMap.put(hostIdStr, hostTopoPathEntry.getTopoPathList());
        }
        // 2.计算出缓存中不存在的拓扑路径，从CMDB获取
        Collection<String> notCachedHostIds = CollectionUtils.subtract(hostIds, cachedHostIds);
        Map<String, List<String>> notCachedHostTopoPathMap =
            delegate.getTopoPathByHostIds(new HashSet<>(notCachedHostIds));
        hostTopoPathMap.putAll(notCachedHostTopoPathMap);
        // 3.将未缓存的拓扑路径信息更新到缓存
        updateHostTopoPathCache(notCachedHostTopoPathMap);
        return hostTopoPathMap;
    }

    /**
     * 更新主机拓扑路径信息到缓存
     *
     * @param hostTopoPathMap 主机拓扑路径信息Map
     */
    private void updateHostTopoPathCache(Map<String, List<String>> hostTopoPathMap) {
        List<HostTopoPathEntry> topoPathEntryList = new ArrayList<>();
        hostTopoPathMap.forEach((hostIdStr, topoPathList) ->
            topoPathEntryList.add(new HostTopoPathEntry(Long.parseLong(hostIdStr), topoPathList))
        );
        hostTopoPathCache.batchAddOrUpdateHostTopoPaths(topoPathEntryList);
        if (log.isDebugEnabled()) {
            log.debug(
                "updateHostTopoPathCache, size={}, topoPathEntryList={}",
                topoPathEntryList.size(),
                topoPathEntryList
            );
        }
    }
}
