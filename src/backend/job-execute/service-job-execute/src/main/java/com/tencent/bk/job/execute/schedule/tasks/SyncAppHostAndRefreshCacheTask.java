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

package com.tencent.bk.job.execute.schedule.tasks;

import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.ApplicationInfoDTO;
import com.tencent.bk.job.execute.model.db.CacheHostDO;
import com.tencent.bk.job.execute.service.ApplicationService;
import com.tencent.bk.job.execute.service.HostService;
import com.tencent.bk.job.manage.model.inner.ServiceHostInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SyncAppHostAndRefreshCacheTask {
    private final HostService hostService;
    private final ApplicationService applicationService;
    private final RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    public SyncAppHostAndRefreshCacheTask(HostService hostService,
                                          ApplicationService applicationService,
                                          @Qualifier("jsonRedisTemplate") RedisTemplate<Object, Object> redisTemplate) {
        this.hostService = hostService;
        this.applicationService = applicationService;
        this.redisTemplate = redisTemplate;
    }

    public void execute() {
        StopWatch watch = new StopWatch("sync-app-hosts");
        try {
            log.info("Get all apps from job-manage!");
            watch.start("sync-apps");
            List<ApplicationInfoDTO> allApps = applicationService.listAllApps();

            if (allApps == null || allApps.isEmpty()) {
                log.warn("Get empty app list from job-manage, skip execution");
                return;
            }
            log.info("Get all apps from job-manage, result:{}",
                allApps.stream().map(ApplicationInfoDTO::getId).collect(Collectors.toSet()));
            watch.stop();

            List<ApplicationInfoDTO> normalApps =
                allApps.stream().filter(app -> app.getAppType() == AppTypeEnum.NORMAL).collect(Collectors.toList());

            for (ApplicationInfoDTO app : normalApps) {
                try {
                    Set<CacheHostDO> appHosts = getAppHosts(watch, app);
                    refreshCache(watch, app.getId().toString(), appHosts);
                } catch (Throwable e) {
                    log.error("Refresh host cache fail", e);
                }
            }
            log.info("Sync and refresh all app hosts successfully!");
        } catch (Throwable e) {
            log.error("Sync host from job-manage fail", e);
        } finally {
            log.info("SyncAppHostAndRefreshCacheTask Statistic:{}", watch.prettyPrint());
        }
    }

    private Set<CacheHostDO> getAppHosts(StopWatch watch, ApplicationInfoDTO app) {
        try {
            Set<CacheHostDO> appHosts = new HashSet<>();
            watch.start("sync-app-hosts-" + app.getId());
            Long appId = app.getId();
            InternalResponse<List<ServiceHostInfoDTO>> syncHostResp = hostService.listSyncHosts(appId);
            log.info("Sync app hosts, appId:{}", appId);

            if (!syncHostResp.isSuccess()) {
                String errorMsg = "Sync app host fail, appId:" + appId;
                log.warn(errorMsg);
                return appHosts;
            }
            List<ServiceHostInfoDTO> hosts = syncHostResp.getData();
            if (hosts.isEmpty()) {
                log.info("Get hosts by appId, appId:{}, host size is 0", appId);
                return appHosts;
            }
            log.info("Get hosts by appId, appId:{}, host size:{}", appId, hosts.size());
            hosts.forEach(host -> appHosts.add(convertToCacheHost(host)));
            return appHosts;
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
        }
    }


    private void refreshCache(StopWatch watch, String appId, Set<CacheHostDO> appHosts) {
        try {
            watch.start("refresh-host-cache-" + appId);
            for (CacheHostDO host : appHosts) {
                String hostKey = "job:execute:host:" + getHostKey(host);
                redisTemplate.opsForValue().set(hostKey, host, 30, TimeUnit.MINUTES);
            }
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
        }
    }

    private CacheHostDO convertToCacheHost(ServiceHostInfoDTO host) {
        CacheHostDO cacheHost = new CacheHostDO();
        cacheHost.setAppId(host.getAppId());
        cacheHost.setCloudAreaId(host.getCloudAreaId());
        cacheHost.setHostId(host.getHostId());
        cacheHost.setIp(host.getIp());
        return cacheHost;
    }

    private String getHostKey(CacheHostDO host) {
        return host.getCloudAreaId() + ":" + host.getIp();
    }
}
