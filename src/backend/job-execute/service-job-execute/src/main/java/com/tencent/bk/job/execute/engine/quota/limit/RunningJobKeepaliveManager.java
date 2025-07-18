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

package com.tencent.bk.job.execute.engine.quota.limit;


import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.execute.constants.RedisKeys;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 正在执行中的作业存活状态维持
 */
@Slf4j
@Component
@EnableScheduling
public class RunningJobKeepaliveManager {
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
                updateJobLatestAliveTimestamp(jobInstanceId, timestamp);
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

    private void updateJobLatestAliveTimestamp(Long jobInstanceId, long currentTimestamp) {
        RedisSerializer<String> stringRedisSerializer = redisTemplate.getStringSerializer();
        byte[] key = stringRedisSerializer.serialize(RedisKeys.RUNNING_JOB_ZSET_KEY);
        byte[] member = stringRedisSerializer.serialize(String.valueOf(jobInstanceId));
        redisTemplate.execute((RedisCallback<Object>) connection -> {
            connection.zAdd(
                Objects.requireNonNull(key),
                currentTimestamp,
                Objects.requireNonNull(member),
                // 只有 member 存在的时候才会更新，避免写入一些异常的作业数据
                RedisZSetCommands.ZAddArgs.ifExists()
            );
            return null;
        });
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

    /**
     * 定时刷新作业存活心跳。 1min周期
     */
    @Scheduled(cron = "0 * * * * ?")
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
                // 设置刷新间隔最小为 30 秒，降低写 redis 的频率
                .filter(keepaliveTask -> currentTimestamp - keepaliveTask.getTimestamp() > 30000L)
                .forEach(keepaliveTask -> {
                    long jobInstanceId = keepaliveTask.getJobInstanceId();
                    try {
                        // 二次确认，防止在运行期间任务被移除
                        if (runningJobKeepaliveTasks.get(jobInstanceId) != null) {
                            updateJobLatestAliveTimestamp(jobInstanceId, currentTimestamp);
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
