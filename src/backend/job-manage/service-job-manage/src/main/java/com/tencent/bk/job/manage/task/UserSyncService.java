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

package com.tencent.bk.job.manage.task;

import com.tencent.bk.job.common.model.dto.BkUserDTO;
import com.tencent.bk.job.common.paas.model.OpenApiTenant;
import com.tencent.bk.job.common.paas.user.UserMgrApiClient;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.redis.util.RedisKeyHeartBeatThread;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.manage.service.UserCacheService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户同步服务
 */
@Slf4j
@Component
public class UserSyncService {

    private static final String REDIS_KEY_SYNC_USER_JOB_LOCK = "sync-user-job-lock";
    private static final String machineIp = IpUtils.getFirstMachineIP();

    static {
        try {
            //进程重启首先尝试释放上次加上的锁避免死锁
            LockUtils.releaseDistributedLock(REDIS_KEY_SYNC_USER_JOB_LOCK, machineIp);
        } catch (Throwable t) {
            log.info("Redis key:" + REDIS_KEY_SYNC_USER_JOB_LOCK + " does not need to be released, ignore");
        }
    }

    private final String REDIS_KEY_SYNC_USER_JOB_RUNNING_MACHINE = "sync-user-job-running-machine";
    private final RedisTemplate<String, String> redisTemplate;
    private final UserMgrApiClient userMgrApiClient;
    private final UserCacheService userCacheService;

    @Autowired
    public UserSyncService(UserMgrApiClient userMgrApiClient,
                           UserCacheService userCacheService,
                           RedisTemplate<String, String> redisTemplate) {
        this.userMgrApiClient = userMgrApiClient;
        this.userCacheService = userCacheService;
        this.redisTemplate = redisTemplate;
    }

    public boolean execute() {
        log.info("syncUser arranged");
        boolean lockGotten = LockUtils.tryGetDistributedLock(
            REDIS_KEY_SYNC_USER_JOB_LOCK, machineIp, 5000);
        if (!lockGotten) {
            log.info("syncUser lock not gotten, return");
            return false;
        }
        String runningMachine = redisTemplate.opsForValue().get(REDIS_KEY_SYNC_USER_JOB_RUNNING_MACHINE);
        if (StringUtils.isNotBlank(runningMachine)) {
            //已有同步线程在跑，不再同步
            log.warn("sync user thread already running on {}", runningMachine);
            return false;
        }
        // 开一个心跳子线程，维护当前机器正在同步用户的状态
        RedisKeyHeartBeatThread userSyncRedisKeyHeartBeatThread = new RedisKeyHeartBeatThread(
            redisTemplate,
            REDIS_KEY_SYNC_USER_JOB_RUNNING_MACHINE,
            machineIp,
            5000L,
            4000L
        );
        userSyncRedisKeyHeartBeatThread.setName("userSyncRedisKeyHeartBeatThread");
        userSyncRedisKeyHeartBeatThread.start();
        log.info("Begin sync all tenant users");
        boolean isAllSuccess = true;
        try {
            List<OpenApiTenant> allTenants = userMgrApiClient.listAllTenant();
            if (CollectionUtils.isEmpty(allTenants)) {
                log.info("Empty tenant list, skip sync");
            } else {
                log.info("Sync user, tenantList: {}",
                    allTenants.stream().map(OpenApiTenant::getId).collect(Collectors.toList()));
            }
            for (OpenApiTenant tenant : allTenants) {
                boolean isSuccess = syncUsersByTenant(tenant.getId());
                isAllSuccess = isAllSuccess && isSuccess;
            }

        } catch (Throwable t) {
            log.error("FATAL: syncUser thread fail", t);
        } finally {
            userSyncRedisKeyHeartBeatThread.setRunFlag(false);
            log.info("Sync all tenant users done, result: {}", isAllSuccess);
        }
        return true;
    }

    private boolean syncUsersByTenant(String tenantId) {
        log.info("Sync user by tenant : {}", tenantId);
        boolean isSuccess = true;
        try {
            // 1.获取租户下的所有用户列表
            List<BkUserDTO> remoteUserList = userMgrApiClient.getAllUserList(tenantId);
            Set<BkUserDTO> remoteUserSet = CollectionUtils.isEmpty(remoteUserList) ?
                Collections.emptySet(): new HashSet<>(remoteUserList);

            // 2.计算差异数据
            Set<BkUserDTO> localUserSet = new HashSet<>(userCacheService.listTenantUsers(tenantId));
            Set<BkUserDTO> addUsers = remoteUserSet.stream()
                .filter(user -> !localUserSet.contains(user)).collect(Collectors.toSet());
            log.info("[{}] New users : {}",
                tenantId,
                addUsers.stream().map(BkUserDTO::getFullName).collect(Collectors.joining(",")));
            Set<BkUserDTO> deleteUsers = localUserSet.stream()
                .filter(user -> !remoteUserSet.contains(user)).collect(Collectors.toSet());
            log.info("[{}] Delete users : {}",
                tenantId,
                deleteUsers.stream().map(BkUserDTO::getFullName).collect(Collectors.joining(",")));

            // 3.保存
            userCacheService.batchPatchUsers(deleteUsers, addUsers);
        } catch (Throwable t) {
            log.error("Sync user fail", t);
            isSuccess = false;
        }
        return isSuccess;
    }
}
