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

package com.tencent.bk.job.execute.engine.result.ha;


import com.tencent.bk.job.common.util.ThreadUtils;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 结果处理任务keepalive管理
 */
@Slf4j
@Component
@EnableScheduling
public class ResultHandleTaskKeepaliveManager {
    private static final String RUNNING_TASK_ZSET_KEY = "running:result:task";
    private final Object lock = new Object();
    private RedisTemplate redisTemplate;
    private Map<String, KeepaliveInfo> runningTasks = new ConcurrentHashMap<>();

    @Autowired
    public ResultHandleTaskKeepaliveManager(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addRunningTaskKeepaliveInfo(String taskId) {
        KeepaliveInfo keepaliveInfo = updateTaskKeepaliveInfo(taskId);
        if (keepaliveInfo != null) {
            log.info("Add running task keepalive info: {}", keepaliveInfo);
            this.runningTasks.put(taskId, keepaliveInfo);
        }
    }

    private KeepaliveInfo updateTaskKeepaliveInfo(String taskId) {
        int maxWaitingSeconds = 600;
        while (maxWaitingSeconds > 0) {
            try {
                long timestamp = System.currentTimeMillis();
                redisTemplate.opsForZSet().add(RUNNING_TASK_ZSET_KEY, taskId, timestamp);
                return new KeepaliveInfo(taskId, timestamp);
            } catch (Throwable e) {
                log.error("Update task keepalive info error, taskId: {}! Wait for 5 seconds retry!", taskId);
                ThreadUtils.sleep(5000L);
                maxWaitingSeconds -= 5;
            }
        }
        return null;
    }

    public void stopKeepaliveInfoTask(String taskId) {
        log.info("Stop keepalive info task : {}", taskId);
        KeepaliveInfo keepaliveInfo = runningTasks.get(taskId);
        if (keepaliveInfo == null) {
            log.warn("Keepalive info task already stopped, taskId: {}!", taskId);
            return;
        }
        synchronized (lock) {
            this.runningTasks.remove(taskId);
            removeTaskKeepaliveInfoFromRedis(taskId);
        }
    }

    public void removeTaskKeepaliveInfoFromRedis(String taskId) {
        int maxWaitingSeconds = 600;
        while (maxWaitingSeconds > 0) {
            try {
                redisTemplate.opsForZSet().remove(RUNNING_TASK_ZSET_KEY, taskId);
                return;
            } catch (Throwable e) {
                log.error("Remove task keepalive info error, taskId: {}! Wait for 5 seconds retry!", taskId);
                ThreadUtils.sleep(5000L);
                maxWaitingSeconds -= 5;
            }
        }
    }

    public Set<String> getNotAliveTaskIds() {
        try {
            long oneMinuteBefore = System.currentTimeMillis() - 60000L;
            Set<String> timeoutTaskIds = redisTemplate.opsForZSet().rangeByScore(RUNNING_TASK_ZSET_KEY, -1,
                oneMinuteBefore);
            if (timeoutTaskIds == null) {
                return Collections.emptySet();
            }
            return timeoutTaskIds;
        } catch (Throwable e) {
            return Collections.emptySet();
        }
    }

    @Scheduled(cron = "0/10 * * * * ?")
    public void refreshTaskKeepaliveInfo() {
        log.info("Refresh task keepalive info start...");
        if (!runningTasks.isEmpty()) {
            Set<String> refreshTaskIds = new HashSet<>();
            long startInMills = System.currentTimeMillis();
            synchronized (lock) {
                Collection<KeepaliveInfo> keepaliveInfos = runningTasks.values();
                long currentTimestamp = System.currentTimeMillis();
                keepaliveInfos.stream().filter(keepaliveInfo -> currentTimestamp - keepaliveInfo.getTimestamp() > 5000L)
                    .forEach(keepaliveInfo -> {
                        String taskId = keepaliveInfo.getTaskId();
                        try {
                            // 二次确认，防止在运行期间任务被移除
                            if (runningTasks.get(taskId) != null) {
                                redisTemplate.opsForZSet().add(RUNNING_TASK_ZSET_KEY, taskId, currentTimestamp);
                                keepaliveInfo.setTimestamp(currentTimestamp);
                                refreshTaskIds.add(taskId);
                            }
                        } catch (Throwable e) {
                            String errorMsg = "Refresh task keepalive info fail, taskId: " + taskId;
                            log.error(errorMsg, e);
                        }

                    });
            }
            log.info("Refresh task keepalive info done! taskSize: {},refreshTaskIds: {}", refreshTaskIds.size(),
                refreshTaskIds);
            long cost = System.currentTimeMillis() - startInMills;
            if (cost > 1000L) {
                log.info("Refresh task keepalive info is slow, taskSize: {}, cost: {}", refreshTaskIds.size(), cost);
            }
        } else {
            log.info("Running tasks is empty. Skip refresh keepaliveInfo");
        }
    }

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    private static class KeepaliveInfo {
        private String taskId;
        private long timestamp;
    }
}
