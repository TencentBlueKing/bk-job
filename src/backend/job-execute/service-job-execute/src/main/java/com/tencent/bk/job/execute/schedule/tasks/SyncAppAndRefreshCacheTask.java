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

import com.google.common.collect.Sets;
import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.execute.model.db.CacheAppDO;
import com.tencent.bk.job.execute.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SyncAppAndRefreshCacheTask {
    private final ApplicationService applicationService;
    private final RedisTemplate<Object, Object> redisTemplate;

    @Autowired
    public SyncAppAndRefreshCacheTask(ApplicationService applicationService,
                                      @Qualifier("jsonRedisTemplate") RedisTemplate<Object, Object> redisTemplate) {
        this.applicationService = applicationService;
        this.redisTemplate = redisTemplate;
    }

    private Map<Long, Set<Long>> getDeptAppMap(List<ApplicationDTO> allApps) {
        Map<Long, Set<Long>> deptAppMap = new HashMap<>();
        for (ApplicationDTO app : allApps) {
            if (app.getAppType() == AppTypeEnum.NORMAL) {
                if (app.getOperateDeptId() != null && app.getOperateDeptId() > 0) {
                    Set<Long> deptApps = deptAppMap.computeIfAbsent(app.getOperateDeptId(), k -> new HashSet<>());
                    deptApps.add(app.getId());
                }
            }
        }
        return deptAppMap;
    }

    private void calcRealSubAppIds(List<ApplicationDTO> allApps, Map<Long, Set<Long>> deptAppMap) {
        for (ApplicationDTO app : allApps) {
            if (app.getAppType() == AppTypeEnum.APP_SET) {
                log.info("AppSet:{}, deptId:{}, subAppIds:{}", app.getId(), app.getOperateDeptId(),
                    app.getSubBizIds());
                Set<Long> subAppIds = new HashSet<>();
                if (app.getSubBizIds() != null) {
                    subAppIds.addAll(app.getSubBizIds());
                }
                if (app.getOperateDeptId() != null && app.getOperateDeptId() > 0) {
                    Set<Long> deptAppIds = deptAppMap.get(app.getOperateDeptId());
                    if (deptAppIds != null) {
                        subAppIds.addAll(deptAppIds);
                    }
                }
                app.setSubBizIds(new ArrayList<>(subAppIds));
                log.info("AppSet:{} contains sub apps:{}", app.getId(), subAppIds);
            }
        }
    }

    public void execute() {
        StopWatch watch = new StopWatch("sync-apps");
        try {
            log.info("Get all apps from job-manage!");
            watch.start("get-apps-from-job-manage");
            List<ApplicationDTO> allApps = applicationService.listAllApps();
            log.debug("Get all apps from job-manage, result:{}", allApps);
            watch.stop();

            if (allApps == null || allApps.isEmpty()) {
                log.warn("Get empty app list from job-manage, skip execution");
                return;
            }
            Set<String> allAppIds =
                allApps.stream().map(app -> String.valueOf(app.getId())).collect(Collectors.toSet());
            log.info("Get all apps from job-manage, appIds:{}", allAppIds);
            Map<Long, Set<Long>> deptAppMap = getDeptAppMap(allApps);
            log.info("Dept app map:{}", deptAppMap);
            calcRealSubAppIds(allApps, deptAppMap);

            watch.start("get-all-cache-apps");
            String appKey = "job:execute:apps";
            Set<String> cacheAppIds = redisTemplate.<String, CacheAppDO>opsForHash().keys(appKey);
            watch.stop();

            watch.start("delete-cache-app");
            Set<String> deleteAppIds = Sets.difference(cacheAppIds, allAppIds);
            if (!deleteAppIds.isEmpty()) {
                log.info("Delete app from cache, deleteAppIds:{}", deleteAppIds);
                for (String deleteAppId : deleteAppIds) {
                    redisTemplate.opsForHash().delete(appKey, deleteAppId);
                }
            }
            watch.stop();

            watch.start("refresh-cache-app");
            Map<String, CacheAppDO> cacheApps = new HashMap<>();
            for (ApplicationDTO app : allApps) {
                CacheAppDO cacheAppDO = CacheAppDO.fromApplicationInfoDTO(app);
                cacheApps.put(String.valueOf(app.getId()), cacheAppDO);
            }
            redisTemplate.opsForHash().putAll(appKey, cacheApps);
            watch.stop();

            log.info("Sync all apps successfully!");
        } catch (Exception e) {
            log.error("Sync and refresh cache fail", e);
        } finally {
            log.info("SyncAppAndRefreshCacheTask Statistic:{}", watch.prettyPrint());
        }
    }
}
