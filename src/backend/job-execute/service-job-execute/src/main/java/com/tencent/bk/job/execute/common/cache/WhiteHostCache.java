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

package com.tencent.bk.job.execute.common.cache;

import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.tenant.TenantDTO;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.remote.RemoteAppService;
import com.tencent.bk.job.manage.api.inner.ServiceTenantResource;
import com.tencent.bk.job.manage.api.inner.ServiceWhiteIPResource;
import com.tencent.bk.job.manage.model.inner.ServiceWhiteIPInfo;
import lombok.extern.slf4j.Slf4j;
import org.jooq.tools.StringUtils;
import org.slf4j.helpers.MessageFormatter;
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
    private final ServiceTenantResource tenantResource;
    private final ServiceWhiteIPResource whiteIpResource;
    private final RemoteAppService remoteAppService;

    private volatile boolean isWhiteIpConfigLoaded = false;
    /**
     * 主机白名单缓存， outerKey: 租户ID，innerKey: hostId, value: 白名单配置
     */
    private final Map<String, Map<Long, ServiceWhiteIPInfo>> tenantWhiteHostConfig = new HashMap<>();

    private static final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private static final Lock rLock = rwLock.readLock();
    private static final Lock wLock = rwLock.writeLock();

    @Autowired
    public WhiteHostCache(ServiceTenantResource tenantResource,
                          ServiceWhiteIPResource whiteIpResource,
                          RemoteAppService remoteAppService) {
        this.tenantResource = tenantResource;
        this.whiteIpResource = whiteIpResource;
        this.remoteAppService = remoteAppService;
    }

    @Scheduled(cron = "0 * * * * ?")
    public void syncWhiteIpConfigForAllTenants() {
        List<TenantDTO> tenantDTOList = tenantResource.listEnabledTenant().getData();
        for (TenantDTO tenantDTO : tenantDTOList) {
            try {
                syncWhiteIpConfig(tenantDTO.getId());
            } catch (Exception e) {
                String msg = MessageFormatter.format(
                    "syncWhiteIpConfig error, tenantId={}",
                    tenantDTO.getId()
                ).toString();
                log.error(msg, e);
            }
        }
    }

    public void syncWhiteIpConfig(String tenantId) {
        isWhiteIpConfigLoaded = true;
        long start = System.currentTimeMillis();
        InternalResponse<List<ServiceWhiteIPInfo>> resp = whiteIpResource.listWhiteIPInfosByTenantId(tenantId);
        if (resp == null || !resp.isSuccess()) {
            log.warn(
                "Get all white host config return fail resp! tenantId={}, resp={}",
                tenantId,
                JsonUtils.toJson(resp)
            );
            return;
        }
        log.info("syncWhiteIpConfig, tenantId={}, resp={}", tenantId, JsonUtils.toJson(resp));

        refreshCache(tenantId, resp.getData());

        long cost = System.currentTimeMillis() - start;
        if (cost > 1000L) {
            log.warn("syncWhiteIpConfig is slow, cost: {}", cost);
        }
        log.info("syncWhiteIpConfig success!");
    }

    private void refreshCache(String tenantId, List<ServiceWhiteIPInfo> whiteIpInfos) {
        try {
            wLock.lock();
            if (!tenantWhiteHostConfig.containsKey(tenantId)) {
                tenantWhiteHostConfig.put(tenantId, new HashMap<>());
            }
            Map<Long, ServiceWhiteIPInfo> whiteHostConfig = tenantWhiteHostConfig.get(tenantId);
            whiteHostConfig.clear();
            whiteIpInfos.forEach(whiteIpInfo -> {
                if (whiteIpInfo.getHostId() != null) {
                    whiteHostConfig.put(whiteIpInfo.getHostId(), whiteIpInfo);
                }
            });
            log.info(
                "Refresh white host cache success, tenantId={}, whiteHostConfig={}",
                tenantId,
                JsonUtils.toJson(whiteHostConfig)
            );
        } finally {
            wLock.unlock();
        }
    }

    public List<String> getHostAllowedAction(long appId, long hostId) {
        try {
            if (!isWhiteIpConfigLoaded) {
                syncWhiteIpConfigForAllTenants();
            }
            String tenantId = remoteAppService.getTenantIdByAppId(appId);
            if (StringUtils.isBlank(tenantId)) {
                log.warn("Cannot get tenantId by appId={}", appId);
                return null;
            }
            try {
                rLock.lock();
                Map<Long, ServiceWhiteIPInfo> whiteHostConfig = tenantWhiteHostConfig.get(tenantId);
                if (whiteHostConfig == null) {
                    return null;
                }
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
