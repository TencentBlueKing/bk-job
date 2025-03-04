package com.tencent.bk.job.common.paas.user;

import com.tencent.bk.job.common.paas.model.OpenApiTenant;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 租户信息缓存
 */
public class TenantLocalCache {

    private final IUserApiClient userMgrApiClient;

    private ScheduledThreadPoolExecutor refresher = new ScheduledThreadPoolExecutor(1);


    private final Map<String, OpenApiTenant> tenantCache = new HashMap<>();

    private volatile boolean initialed = false;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();


    public TenantLocalCache(IUserApiClient userMgrApiClient) {
        this.userMgrApiClient = userMgrApiClient;
//        refresher.scheduleAtFixedRate(this::loadCache, )
    }

    public OpenApiTenant get(String tenantId) {
        if (!initialed) {
            loadCache();
            initialed = true;
        }
        return null;
    }

    private void loadCache() {

    }
}
