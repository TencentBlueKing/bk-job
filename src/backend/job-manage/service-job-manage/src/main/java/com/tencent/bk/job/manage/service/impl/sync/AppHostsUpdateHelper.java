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

package com.tencent.bk.job.manage.service.impl.sync;

import com.tencent.bk.job.common.redis.util.RedisKeyHeartBeatThread;
import com.tencent.bk.job.common.util.ip.IpUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class AppHostsUpdateHelper {


    private static final String machineIp = IpUtils.getFirstMachineIP();
    private static final String REDIS_KEY_UPDATE_APP_HOSTS_LOCK_PREFIX = "update-app-hosts-lock:";
    // 当前正在同步业务主机的机器IP
    private static final String REDIS_KEY_UPDATE_APP_HOSTS_RUNNING_MACHINE_PREFIX = "update-app-hosts-running-machine:";
    private static final Map<String, RedisKeyHeartBeatThread> heartBeatThreadMap =
        Collections.synchronizedMap(new HashMap<>());
    private final RedisTemplate<String, String> redisTemplate;

    public AppHostsUpdateHelper(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String getAppHostsUpdatingMachine(Long appId) {
        String runningMachineKey = REDIS_KEY_UPDATE_APP_HOSTS_RUNNING_MACHINE_PREFIX + appId;
        return redisTemplate.opsForValue().get(runningMachineKey);
    }

    public void waitAndStartAppHostsUpdating(Long appId) {
        waitForAppHostsUpdatingLock(appId);
        startToUpdateAppHosts(appId);
    }

    public void waitForAppHostsUpdatingLock(Long appId) {
        waitForAppHostsUpdatingLock(appId, 50);
    }

    public void waitForAppHostsUpdatingLock(Long appId, int intervalMills) {
        waitForAppHostsUpdatingLock(appId, intervalMills, 2000);
    }

    public void waitForAppHostsUpdatingLock(Long appId, int intervalMills, int printMills) {
        int waitMills = 0;
        while (isAppHostsUpdating(appId)) {
            try {
                Thread.sleep(intervalMills);
            } catch (InterruptedException e) {
                log.warn("Sleep interrupted", e);
            }
            waitMills += intervalMills;
            if (waitMills % printMills == 0) {
                log.info("wait {} release {} appHosts updating lock for {}ms", appId,
                    getAppHostsUpdatingMachine(appId), waitMills);
            }
        }
    }

    public boolean isAppHostsUpdating(Long appId) {
        String updatingMachine = getAppHostsUpdatingMachine(appId);
        return StringUtils.isNotBlank(updatingMachine);
    }

    public void startToUpdateAppHosts(Long appId) {
        String runningMachineKey = REDIS_KEY_UPDATE_APP_HOSTS_RUNNING_MACHINE_PREFIX + appId;
        RedisKeyHeartBeatThread appHostUpdateRedisKeyHeartBeatThread = new RedisKeyHeartBeatThread(
            redisTemplate,
            runningMachineKey,
            machineIp + "-" + Thread.currentThread().getName(),
            5000L,
            4000L
        );
        appHostUpdateRedisKeyHeartBeatThread.setName(
            "[" + appHostUpdateRedisKeyHeartBeatThread.getId()
                + "]-appHostUpdateRedisKeyHeartBeatThread-" + appId
        );
        appHostUpdateRedisKeyHeartBeatThread.start();
        heartBeatThreadMap.put(Thread.currentThread().getName(), appHostUpdateRedisKeyHeartBeatThread);
    }

    public void endToUpdateAppHosts(Long appId) {
        String key = Thread.currentThread().getName();
        RedisKeyHeartBeatThread heartBeatThread = heartBeatThreadMap.get(key);
        if (heartBeatThread != null) {
            heartBeatThread.stopAtOnce();
            heartBeatThreadMap.remove(key);
        }
    }
}
