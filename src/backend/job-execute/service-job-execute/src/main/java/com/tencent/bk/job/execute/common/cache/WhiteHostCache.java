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

package com.tencent.bk.job.execute.common.cache;

import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.api.inner.ServiceWhiteIPResource;
import com.tencent.bk.job.manage.model.inner.ServiceWhiteIPInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 主机白名单本地缓存
 */
@Component
@Slf4j
public class WhiteHostCache {
    private final ServiceWhiteIPResource whiteIpResource;

    private volatile boolean isWhiteIpConfigLoaded = false;
    /**
     * 主机白名单缓存， key: hostId, value: 白名单配置
     */
    private final Map<Long, ServiceWhiteIPInfo> whiteHostConfig = new HashMap<>();

    private static final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private static final Lock rLock = rwLock.readLock();
    private static final Lock wLock = rwLock.writeLock();

    @Autowired
    public WhiteHostCache(ServiceWhiteIPResource whiteIpResource) {
        this.whiteIpResource = whiteIpResource;
    }

    @Scheduled(cron = "0 * * * * ?")
    public void syncWhiteIpConfig() {
        log.info("Sync white host config!");
        isWhiteIpConfigLoaded = true;
        long start = System.currentTimeMillis();
        InternalResponse<List<ServiceWhiteIPInfo>> resp = whiteIpResource.listWhiteIPInfos();
        if (resp == null || !resp.isSuccess()) {
            log.warn("Get all white host config return fail resp! resp: {}", JsonUtils.toJson(resp));
            return;
        }
        log.info("Sync white host config, resp: {}", JsonUtils.toJson(resp));

        refreshCache(resp.getData());

        long cost = System.currentTimeMillis() - start;
        if (cost > 1000L) {
            log.warn("Sync white host config is slow, cost: {}", cost);
        }
        log.info("Sync white host config success!");
    }

    private void refreshCache(List<ServiceWhiteIPInfo> whiteIpInfos) {
        try {
            wLock.lock();
            whiteHostConfig.clear();
            whiteIpInfos.forEach(whiteIpInfo -> {
                if (whiteIpInfo.getHostId() != null) {
                    whiteHostConfig.put(whiteIpInfo.getHostId(), whiteIpInfo);
                }
            });
            log.info("Refresh white host cache success. whiteHostConfig: {}", JsonUtils.toJson(whiteHostConfig));
        } finally {
            wLock.unlock();
        }
    }

    public List<String> getHostAllowedAction(long appId, long hostId) {
        try {
            if (!isWhiteIpConfigLoaded) {
                syncWhiteIpConfig();
            }
            try {
                rLock.lock();
                ServiceWhiteIPInfo whiteIPInfo = whiteHostConfig.get(hostId);
                if (whiteIPInfo == null) {
                    return null;
                }
                if (whiteIPInfo.isForAllApp()) {
                    return whiteIPInfo.getAllAppActionScopeList();
                } else {
                    if (whiteIPInfo.getAppIdActionScopeMap() != null
                        && !whiteIPInfo.getAppIdActionScopeMap().isEmpty()) {
                        return whiteIPInfo.getAppIdActionScopeMap().get(appId);
                    } else {
                        return null;
                    }
                }
            } finally {
                rLock.unlock();
            }
        } catch (Exception e) {
            log.warn("GetHostAllowedAction fail!", e);
            return null;
        }
    }

}
