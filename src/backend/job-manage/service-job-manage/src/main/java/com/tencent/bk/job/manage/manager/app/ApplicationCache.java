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

package com.tencent.bk.job.manage.manager.app;

import com.google.common.collect.Sets;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.dao.ApplicationDAO;
import com.tencent.bk.job.manage.model.db.CacheAppDO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Job 业务缓存
 */
@Slf4j
@Component
public class ApplicationCache {

    private final ApplicationDAO applicationDAO;
    private final RedisTemplate<Object, Object> redisTemplate;
    private final String APP_HASH_KEY = "job:manage:apps";
    private final String SCOPE_HASH_KEY = "job:manage:scopes";

    @Autowired
    public ApplicationCache(ApplicationDAO applicationDAO,
                            @Qualifier("jsonRedisTemplate") RedisTemplate<Object, Object> redisTemplate) {
        this.applicationDAO = applicationDAO;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 根据Job业务ID获取业务
     *
     * @param appId Job业务ID
     * @return 业务
     */
    public ApplicationDTO getApplication(long appId) {
        ApplicationDTO application = null;
        Object appObj = redisTemplate.opsForHash().get(APP_HASH_KEY, String.valueOf(appId));
        if (appObj != null) {
            application = CacheAppDO.toApplicationDTO((CacheAppDO) appObj);
        }
        return application;
    }

    /**
     * 根据资源范围获取Job业务
     *
     * @param scopeType 资源范围类型
     * @param scopeId   资源范围ID
     * @return 业务
     */
    public ApplicationDTO getApplication(String scopeType, String scopeId) {
        ApplicationDTO application = null;
        Object appObj = redisTemplate.opsForHash().get(SCOPE_HASH_KEY, buildScopeKey(new ResourceScope(scopeType,
            scopeId)));
        if (appObj != null) {
            application = CacheAppDO.toApplicationDTO((CacheAppDO) appObj);
        }
        return application;
    }

    /**
     * 根据资源范围获取Job业务
     *
     * @param scope 资源范围
     * @return 业务
     */
    public ApplicationDTO getApplication(ResourceScope scope) {
        return getApplication(scope.getType().getValue(), scope.getId());
    }

    /**
     * 根据业务ID批量获取业务列表
     *
     * @param appIds 业务ID列表
     * @return 业务列表
     */
    public List<ApplicationDTO> listApplicationsByAppIds(Collection<Long> appIds) {
        List<ApplicationDTO> applicationList = new ArrayList<>();
        List<CacheAppDO> cacheApps = redisTemplate.<String, CacheAppDO>opsForHash()
            .multiGet(APP_HASH_KEY, buildAppKeys(appIds));

        if (CollectionUtils.isNotEmpty(cacheApps)) {
            applicationList = cacheApps.stream().map(CacheAppDO::toApplicationDTO).collect(Collectors.toList());
        }
        return applicationList;
    }

    private Collection<String> buildAppKeys(Collection<Long> appIds) {
        return appIds.stream().map(String::valueOf).collect(Collectors.toSet());
    }

    /**
     * 从缓存中删除业务
     *
     * @param appId 业务ID
     */
    public void deleteApp(Long appId) {
        String requestId = null;
        try {
            requestId = lock();
            if (requestId == null) {
                return;
            }

            CacheAppDO cacheApp = redisTemplate.<String, CacheAppDO>opsForHash().get(APP_HASH_KEY,
                String.valueOf(appId));
            if (cacheApp != null) {
                redisTemplate.<String, CacheAppDO>opsForHash().delete(APP_HASH_KEY, String.valueOf(cacheApp.getId()));
                String scopeKey = buildScopeKey(cacheApp.getScopeType(), cacheApp.getScopeId());
                redisTemplate.<String, CacheAppDO>opsForHash().delete(SCOPE_HASH_KEY, scopeKey);
            }
            log.info("Delete app from cache successfully! app: {}", cacheApp);
        } finally {
            unlock(requestId);
        }
    }

    /**
     * 获取更新缓存的分布式锁
     *
     * @return 如果获取到锁，那么返回requestId;否则返回null
     */
    private String lock() {
        String requestId = UUID.randomUUID().toString();
        if (!LockUtils.lock("refresh-app-lock", requestId, 30_000, 30)) {
            log.info("Fail to get refresh app lock");
            return null;
        } else {
            return requestId;
        }
    }

    /**
     * 释放更新缓存的分布式锁
     *
     * @param requestId 请求标识
     * @return 是否释放成功
     */
    private boolean unlock(String requestId) {
        if (requestId == null) {
            return true;
        }
        return LockUtils.releaseDistributedLock("refresh-app-lock", requestId);
    }

    /**
     * 更新缓存中的业务
     *
     * @param updatedApplication 业务
     */
    public void addOrUpdateApp(ApplicationDTO updatedApplication) {
        String requestId = null;
        try {
            requestId = lock();
            if (requestId == null) {
                return;
            }

            String scopeKey = buildScopeKey(updatedApplication.getScope());
            CacheAppDO cacheApp = CacheAppDO.fromApplicationDTO(updatedApplication);
            redisTemplate.<String, CacheAppDO>opsForHash().put(SCOPE_HASH_KEY, scopeKey, cacheApp);
            redisTemplate.<String, CacheAppDO>opsForHash().put(APP_HASH_KEY, String.valueOf(updatedApplication.getId()),
                cacheApp);
            log.info("Update app successfully! app: {}", cacheApp);
        } finally {
            unlock(requestId);
        }
    }

    /**
     * 全量刷新缓存
     */
    public void refreshCache() {
        String requestId = null;
        try {
            requestId = lock();
            if (requestId == null) {
                return;
            }

            List<ApplicationDTO> allApps = applicationDAO.listAllApps();
            if (CollectionUtils.isEmpty(allApps)) {
                log.warn("Get empty app list from MySQL, skip refresh");
                return;
            }
            log.info("Get all apps from MySQL, apps:{}", JsonUtils.toJson(allApps));

            refreshAppCache(allApps);
            refreshScopeCache(allApps);

            log.info("Refresh app cache successfully!");
        } finally {
            unlock(requestId);
        }
    }

    private void refreshAppCache(List<ApplicationDTO> allApps) {
        StopWatch watch = new StopWatch("refreshAppCache");
        try {
            Set<String> allAppIds =
                allApps.stream().map(app -> String.valueOf(app.getId())).collect(Collectors.toSet());

            watch.start("loadAllAppsFromRedis");
            Set<String> cacheAppIds = redisTemplate.<String, CacheAppDO>opsForHash().keys(APP_HASH_KEY);
            watch.stop();

            watch.start("deleteNotExistApps");
            Set<String> deleteAppIds = Sets.difference(cacheAppIds, allAppIds);
            if (!deleteAppIds.isEmpty()) {
                log.info("Delete app from cache, deleteAppIds:{}", deleteAppIds);
                for (String deleteAppId : deleteAppIds) {
                    redisTemplate.opsForHash().delete(APP_HASH_KEY, deleteAppId);
                }
            }
            watch.stop();

            watch.start("updateAvailableApps");
            Map<String, CacheAppDO> cacheApps = new HashMap<>();
            for (ApplicationDTO app : allApps) {
                CacheAppDO cacheAppDO = CacheAppDO.fromApplicationDTO(app);
                cacheApps.put(String.valueOf(app.getId()), cacheAppDO);
            }
            redisTemplate.opsForHash().putAll(APP_HASH_KEY, cacheApps);
            watch.stop();
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
            log.info("Refresh app cache, report:{}", watch.prettyPrint());
        }
    }

    private void refreshScopeCache(List<ApplicationDTO> allApps) {
        StopWatch watch = new StopWatch("refreshScopeCache");
        try {
            Set<String> allScopeKeys =
                allApps.stream().map(app -> buildScopeKey(app.getScope())).collect(Collectors.toSet());

            watch.start("loadAllScopesFromRedis");
            Set<String> cacheScopeKeys = redisTemplate.<String, CacheAppDO>opsForHash().keys(SCOPE_HASH_KEY);
            watch.stop();

            watch.start("deleteNotExistScopes");
            Set<String> deleteScopeKeys = Sets.difference(cacheScopeKeys, allScopeKeys);
            if (!deleteScopeKeys.isEmpty()) {
                log.info("Delete scope from cache, deleteScopeKeys:{}", deleteScopeKeys);
                for (String deleteScopeKey : deleteScopeKeys) {
                    redisTemplate.opsForHash().delete(SCOPE_HASH_KEY, deleteScopeKey);
                }
            }
            watch.stop();

            watch.start("updateAvailableScopes");
            Map<String, CacheAppDO> cacheApps = new HashMap<>();
            for (ApplicationDTO app : allApps) {
                CacheAppDO cacheAppDO = CacheAppDO.fromApplicationDTO(app);
                cacheApps.put(buildScopeKey(app.getScope()), cacheAppDO);
            }
            redisTemplate.opsForHash().putAll(SCOPE_HASH_KEY, cacheApps);
            watch.stop();
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
            log.info("Refresh scope cache, report:{}", watch.prettyPrint());
        }
    }

    private String buildScopeKey(ResourceScope scope) {
        return scope.getType().getValue() + ":" + scope.getId();
    }

    private String buildScopeKey(String scopeType, String scopeId) {
        return scopeType + ":" + scopeId;
    }


}
