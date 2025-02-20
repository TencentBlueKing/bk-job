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

package com.tencent.bk.job.manage.service.cloudarea;

import com.tencent.bk.job.common.cc.model.CcCloudAreaInfoDTO;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.manage.model.inner.resp.TenantDTO;
import com.tencent.bk.job.manage.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 管控区域信息缓存服务
 * TODO: 整体缓存方案待重构
 */
@Slf4j
@Service
public class BkNetService {
    private final TenantService tenantService;
    private final IBizCmdbClient bizCmdbClient;
    private final Map<String, Map<Long, String>> tenantCloudAreaIdNameMap = new ConcurrentHashMap<>();
    private final Map<String, List<CcCloudAreaInfoDTO>> tenantFullCloudAreaInfoListMap = new ConcurrentHashMap<>();

    @Autowired
    public BkNetService(IBizCmdbClient bizCmdbClient, TenantService tenantService) {
        this.tenantService = tenantService;
        this.bizCmdbClient = bizCmdbClient;
        new CloudAreaNameCacheThread().start();
    }

    /**
     * 根据ID获取云区域名称
     *
     * @param cloudAreaId 云区域ID
     * @return 云区域名称
     */
    public String getCloudAreaNameFromCache(Long cloudAreaId) {
        String tenantId = JobContextUtil.getTenantId();
        Map<Long, String> cloudAreaIdNameMap = getCloudAreaIdNameMap(tenantId);
        if (MapUtils.isNotEmpty(cloudAreaIdNameMap)) {
            if (StringUtils.isNotBlank(cloudAreaIdNameMap.get(cloudAreaId))) {
                return cloudAreaIdNameMap.get(cloudAreaId);
            }
        }
        return String.valueOf(cloudAreaId);
    }

    /**
     * 查找名称符合搜索关键字（与任意一个关键字匹配即可）的云区域，并返回其ID
     *
     * @param searchContents 搜索关键字集合
     * @return 符合条件的云区域ID列表
     */
    public List<Long> getAnyNameMatchedCloudAreaIds(Collection<String> searchContents) {
        String tenantId = JobContextUtil.getTenantId();
        if (searchContents == null) {
            return getAllCloudAreaIds(tenantId);
        }
        List<Long> cloudAreaIds;
        cloudAreaIds = new ArrayList<>();
        List<CcCloudAreaInfoDTO> allCloudAreaInfos = getCloudAreaList(tenantId);
        for (CcCloudAreaInfoDTO it : allCloudAreaInfos) {
            if (StringUtil.matchAnySearchContent(it.getName(), searchContents)) {
                cloudAreaIds.add(it.getId());
            }
        }
        log.debug("tenantId={}, filter by cloudAreaIds={}", tenantId, cloudAreaIds);
        return cloudAreaIds;
    }

    private Map<Long, String> getCloudAreaIdNameMap(String tenantId) {
        return tenantCloudAreaIdNameMap.computeIfAbsent(tenantId, key -> new ConcurrentHashMap<>());
    }

    private List<CcCloudAreaInfoDTO> getCloudAreaListFromCc(String tenantId) {
        List<CcCloudAreaInfoDTO> cloudAreaInfoList = bizCmdbClient.getCloudAreaList(tenantId);
        if (cloudAreaInfoList == null) {
            return new ArrayList<>();
        }
        return cloudAreaInfoList;
    }

    private List<CcCloudAreaInfoDTO> getCloudAreaList(String tenantId) {
        return tenantFullCloudAreaInfoListMap.computeIfAbsent(tenantId, key -> new ArrayList<>());
    }

    private List<Long> getAllCloudAreaIds(String tenantId) {
        return getCloudAreaList(tenantId).stream()
            .map(CcCloudAreaInfoDTO::getId)
            .collect(Collectors.toList());
    }

    class CloudAreaNameCacheThread extends Thread {
        @SuppressWarnings("InfiniteLoopStatement")
        @Override
        public void run() {
            this.setName("Cloud-Area-Info-Sync-Thread");
            while (true) {
                List<TenantDTO> tenantDTOList = tenantService.listEnabledTenant();
                for (TenantDTO tenantDTO : tenantDTOList) {
                    try {
                        cacheCloudAreaInfo(tenantDTO.getId());
                    } catch (Exception e) {
                        String msg = MessageFormatter.format(
                            "cacheCloudAreaInfo error, tenantId={}",
                            tenantDTO.getId()
                        ).toString();
                        log.error(msg, e);
                    }
                }
                ThreadUtils.sleep(60_000L);
            }
        }

        private void cacheCloudAreaInfo(String tenantId) {
            long start = System.currentTimeMillis();
            log.debug("CloudAreaInfo sync start...");
            List<CcCloudAreaInfoDTO> cloudAreaInfoList = getCloudAreaListFromCc(tenantId);
            Map<Long, String> cloudAreaIdMap = getCloudAreaIdNameMap(tenantId);
            if (CollectionUtils.isNotEmpty(cloudAreaInfoList)) {
                Iterator<CcCloudAreaInfoDTO> cloudAreaInfoIterator = cloudAreaInfoList.iterator();
                while (cloudAreaInfoIterator.hasNext()) {
                    CcCloudAreaInfoDTO cloudAreaInfo = cloudAreaInfoIterator.next();
                    if (cloudAreaInfo != null && cloudAreaInfo.getId() != null
                        && StringUtils.isNotBlank(cloudAreaInfo.getName())) {
                        cloudAreaIdMap.put(cloudAreaInfo.getId(), cloudAreaInfo.getName());
                    } else {
                        cloudAreaInfoIterator.remove();
                    }
                }
                tenantFullCloudAreaInfoListMap.put(tenantId, cloudAreaInfoList);
            }
            log.debug(
                "CloudAreaInfo(tenantId={}) sync finished in {}ms",
                tenantId,
                System.currentTimeMillis() - start
            );
        }
    }

}
