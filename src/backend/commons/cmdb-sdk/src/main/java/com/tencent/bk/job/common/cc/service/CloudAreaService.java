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

package com.tencent.bk.job.common.cc.service;

import com.tencent.bk.job.common.cc.config.CmdbConfig;
import com.tencent.bk.job.common.cc.model.CcCloudAreaInfoDTO;
import com.tencent.bk.job.common.cc.sdk.BizCmdbClient;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.esb.config.BkApiConfig;
import com.tencent.bk.job.common.gse.service.QueryAgentStatusClient;
import com.tencent.bk.job.common.i18n.locale.LocaleUtils;
import com.tencent.bk.job.common.util.StringUtil;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.DependsOn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 云区域信息服务
 * <p>
 * 使用前务必在任何一个类引入一次，否则不会初始化
 *
 * @since 23/12/2019 22:48
 */

@DependsOn({"cmdbConfigSetter"})
@Slf4j
public class CloudAreaService {
    private static final Map<Long, String> CLOUD_AREA_NAME_MAP = new ConcurrentHashMap<>();
    private static IBizCmdbClient esbBizCmdbClient;
    private static List<CcCloudAreaInfoDTO> fullCloudAreaInfoList;

    public CloudAreaService(BkApiConfig bkApiConfig, CmdbConfig cmdbConfig, QueryAgentStatusClient queryAgentStatusClient,
                            MeterRegistry meterRegistry) {
        CloudAreaNameCacheThread cloudAreaNameCacheThread = new CloudAreaNameCacheThread();
        esbBizCmdbClient = new BizCmdbClient(bkApiConfig, cmdbConfig, LocaleUtils.LANG_EN_US, queryAgentStatusClient,
            meterRegistry);
        cloudAreaNameCacheThread.start();
    }

    public static String getCloudAreaNameFromCache(Long cloudAreaId) {
        if (MapUtils.isNotEmpty(CLOUD_AREA_NAME_MAP)) {
            if (StringUtils.isNotBlank(CLOUD_AREA_NAME_MAP.get(cloudAreaId))) {
                return CLOUD_AREA_NAME_MAP.get(cloudAreaId);
            }
        }
        return String.valueOf(cloudAreaId);
    }

    private static List<CcCloudAreaInfoDTO> getCloudAreaListFromCc() {
        List<CcCloudAreaInfoDTO> cloudAreaInfoList = esbBizCmdbClient.getCloudAreaList();
        if (cloudAreaInfoList == null) {
            return new ArrayList<>();
        }
        return cloudAreaInfoList;
    }

    public String getCloudAreaName(Long cloudAreaId) {
        if (cloudAreaId == null) {
            return "Unknown";
        }

        if (MapUtils.isNotEmpty(CLOUD_AREA_NAME_MAP)) {
            if (StringUtils.isNotBlank(CLOUD_AREA_NAME_MAP.get(cloudAreaId))) {
                return CLOUD_AREA_NAME_MAP.get(cloudAreaId);
            }
        } else {
            List<CcCloudAreaInfoDTO> cloudAreaInfoList = getCloudAreaList();
            if (cloudAreaInfoList != null) {
                for (CcCloudAreaInfoDTO cloudAreaInfo : cloudAreaInfoList) {
                    if (cloudAreaId.equals(cloudAreaInfo.getId())) {
                        if (StringUtils.isNotBlank(cloudAreaInfo.getName())) {
                            return cloudAreaInfo.getName();
                        } else {
                            return String.valueOf(cloudAreaId);
                        }
                    }
                }
            }
        }
        return String.valueOf(cloudAreaId);
    }

    public List<CcCloudAreaInfoDTO> getCloudAreaList() {
        return fullCloudAreaInfoList;
    }

    private List<Long> getAllCloudAreaIds() {
        return getCloudAreaList().stream()
            .map(CcCloudAreaInfoDTO::getId)
            .collect(Collectors.toList());
    }

    /**
     * 查找名称符合搜索关键字（与任意一个关键字匹配即可）的云区域，并返回其ID
     *
     * @param searchContents 搜索关键字集合
     * @return 符合条件的云区域ID列表
     */
    public List<Long> getAnyNameMatchedCloudAreaIds(Collection<String> searchContents) {
        if (searchContents == null) {
            return getAllCloudAreaIds();
        }
        List<Long> cloudAreaIds;
        cloudAreaIds = new ArrayList<>();
        List<CcCloudAreaInfoDTO> allCloudAreaInfos = getCloudAreaList();
        for (CcCloudAreaInfoDTO it : allCloudAreaInfos) {
            if (StringUtil.matchAnySearchContent(it.getName(), searchContents)) {
                cloudAreaIds.add(it.getId());
            }
        }
        log.debug("filter by cloudAreaIds={}", cloudAreaIds);
        return cloudAreaIds;
    }

    static class CloudAreaNameCacheThread extends Thread {
        @Override
        public void run() {
            this.setName("Cloud-Area-Info-Sync-Thread");
            while (true) {
                String uuid = UUID.randomUUID().toString();
                long start = System.currentTimeMillis();
                try {
                    log.debug("{}|Cloud area info syncing start...", uuid);
                    List<CcCloudAreaInfoDTO> cloudAreaInfoList = getCloudAreaListFromCc();
                    if (CollectionUtils.isNotEmpty(cloudAreaInfoList)) {
                        Iterator<CcCloudAreaInfoDTO> cloudAreaInfoIterator = cloudAreaInfoList.iterator();
                        while (cloudAreaInfoIterator.hasNext()) {
                            CcCloudAreaInfoDTO cloudAreaInfo = cloudAreaInfoIterator.next();
                            if (cloudAreaInfo != null && cloudAreaInfo.getId() != null
                                && StringUtils.isNotBlank(cloudAreaInfo.getName())) {
                                CLOUD_AREA_NAME_MAP.put(cloudAreaInfo.getId(), cloudAreaInfo.getName());
                            } else {
                                cloudAreaInfoIterator.remove();
                            }
                        }
                        fullCloudAreaInfoList = cloudAreaInfoList;
                    }
                    log.debug("{}|Cloud area info syncing finished in {}!", uuid, System.currentTimeMillis() - start);
                } catch (Exception e) {
                    log.error("{}|Error while process cloud area name!", uuid, e);
                }
                try {
                    Thread.sleep(60_000L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

}
