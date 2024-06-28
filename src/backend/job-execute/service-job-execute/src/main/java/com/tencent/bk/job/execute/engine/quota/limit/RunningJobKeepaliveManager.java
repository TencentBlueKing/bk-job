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

package com.tencent.bk.job.execute.engine.quota.limit;


import com.tencent.bk.job.common.util.ThreadUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 正在执行中的作业存活状态维持
 */
@Slf4j
@Component
@EnableScheduling
public class RunningJobKeepaliveManager {
    private static final String RUNNING_JOB_ZSET_KEY = "job:execute:running:job";
    private final Object lock = new Object();
    private final RedisTemplate<String, String> redisTemplate;
    private final Map<Long, KeepaliveTask> runningJobKeepaliveTasks = new ConcurrentHashMap<>();

    @Autowired
    public RunningJobKeepaliveManager(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addKeepaliveTask(long jobInstanceId) {
        KeepaliveTask keepaliveTask = updateTaskKeepaliveInfo(jobInstanceId);
        if (keepaliveTask != null) {
            log.info("Add running job keepalive task: {}", keepaliveTask);
            this.runningJobKeepaliveTasks.put(jobInstanceId, keepaliveTask);
        }
    }

    private KeepaliveTask updateTaskKeepaliveInfo(long jobInstanceId) {
        int maxWaitingSeconds = 600;
        while (maxWaitingSeconds > 0) {
            try {
                long timestamp = System.currentTimeMillis();
                redisTemplate.opsForZSet().add(RUNNING_JOB_ZSET_KEY, String.valueOf(jobInstanceId), timestamp);
                return new KeepaliveTask(jobInstanceId, timestamp);
            } catch (Throwable e) {
                log.error("Update running job keepalive task error, jobInstanceId: " + jobInstanceId, e);
                log.info("Wait for 5 seconds retry!");
                ThreadUtils.sleep(5000L);
                maxWaitingSeconds -= 5;
            }
        }
        return null;
    }

    public void stopKeepaliveTask(long jobInstanceId) {
        log.info("Stop running job keepalive task : {}", jobInstanceId);
        KeepaliveTask keepaliveTask = runningJobKeepaliveTasks.get(jobInstanceId);
        if (keepaliveTask == null) {
            log.warn("keepalive task task already stopped, jobInstanceId: {}!", jobInstanceId);
            return;
        }
        synchronized (lock) {
            this.runningJobKeepaliveTasks.remove(jobInstanceId);
        }
    }

    @Scheduled(cron = "* 0/1 * * * ?")
    public void refreshTaskKeepaliveInfo() {
        log.info("Refresh running job keepalive task start...");
        if (runningJobKeepaliveTasks.isEmpty()) {
            return;
        }
        Set<Long> refreshJobInstanceIds = new HashSet<>();
        long startInMills = System.currentTimeMillis();
        synchronized (lock) {
            Collection<KeepaliveTask> keepaliveTasks = runningJobKeepaliveTasks.values();
            long currentTimestamp = System.currentTimeMillis();
            keepaliveTasks.stream()
                // 设置刷新间隔最小为 30 秒，避免频繁写 redis
                .filter(keepaliveTask -> currentTimestamp - keepaliveTask.getTimestamp() > 30000L)
                .forEach(keepaliveTask -> {
                    long jobInstanceId = keepaliveTask.getJobInstanceId();
                    try {
                        // 二次确认，防止在运行期间任务被移除
                        if (runningJobKeepaliveTasks.get(jobInstanceId) != null) {
                            redisTemplate.opsForZSet().add(RUNNING_JOB_ZSET_KEY, String.valueOf(jobInstanceId),
                                currentTimestamp);
                            keepaliveTask.setTimestamp(currentTimestamp);
                            refreshJobInstanceIds.add(jobInstanceId);
                        }
                    } catch (Throwable e) {
                        String errorMsg =
                            "Refresh running job keepalive task fail, jobInstanceId: " + jobInstanceId;
                        log.error(errorMsg, e);
                    }
                });
        }
        log.info("Refresh running job keepalive task done! taskSize: {},refreshJobInstanceIds: {}",
            refreshJobInstanceIds.size(), refreshJobInstanceIds);
        long cost = System.currentTimeMillis() - startInMills;
        if (cost > 1000L) {
            log.info("Refresh running job keepalive task is slow, taskSize: {}, cost: {}",
                refreshJobInstanceIds.size(), cost);
        }
    }

    @Getter
    @Setter
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    private static class KeepaliveTask {
        private long jobInstanceId;
        private long timestamp;
    }
}
