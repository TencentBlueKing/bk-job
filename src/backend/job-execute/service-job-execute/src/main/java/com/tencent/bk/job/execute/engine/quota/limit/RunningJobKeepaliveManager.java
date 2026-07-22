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


import com.tencent.bk.job.common.annotation.ScheduledOnOperationTimeZone;
import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.execute.constants.RedisKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 正在执行中的作业存活状态维持
 */
@Slf4j
@Component
@EnableScheduling
public class RunningJobKeepaliveManager {
    private final RedisTemplate<String, String> redisTemplate;
    private final Map<Long, KeepaliveTask> runningJobKeepaliveTasks = new ConcurrentHashMap<>();

    @Autowired
    public RunningJobKeepaliveManager(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 新增一个作业存活心跳引用。
     * <p>
     * 心跳按 jobInstanceId 维护引用计数：同一作业在并行错峰模式下会有多个批次的结果处理任务并发共用一条心跳，
     * 每个结果处理任务投递时递增计数，任务完成时递减计数，只有计数归 0 才真正停止刷新，
     * 避免中途批次完成就把仍在运行的长作业心跳提前停掉。
     *
     * @param jobInstanceId 作业实例 ID
     */
    public void addKeepaliveTask(long jobInstanceId) {
        // 先刷新一次作业存活时间戳，保证 ZSet score 及时写入（保持 ifExists 语义）
        Long timestamp = updateTaskKeepaliveInfo(jobInstanceId);
        if (timestamp == null) {
            // 刷新失败（超过最大重试时间），不纳入心跳引用计数管理
            return;
        }
        runningJobKeepaliveTasks.compute(jobInstanceId, (id, task) -> {
            if (task == null) {
                KeepaliveTask newTask = new KeepaliveTask(id, timestamp);
                int refCount = newTask.incrementRefCount();
                log.info("Add running job keepalive task: {}, refCount: {}", newTask, refCount);
                return newTask;
            }
            task.setTimestamp(timestamp);
            int refCount = task.incrementRefCount();
            log.info("Increment running job keepalive task refCount, jobInstanceId: {}, refCount: {}",
                id, refCount);
            return task;
        });
    }

    private Long updateTaskKeepaliveInfo(long jobInstanceId) {
        int maxWaitingSeconds = 600;
        while (maxWaitingSeconds > 0) {
            try {
                long timestamp = System.currentTimeMillis();
                updateJobLatestAliveTimestamp(jobInstanceId, timestamp);
                return timestamp;
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

    /**
     * 释放一个作业存活心跳引用。
     * <p>
     * 递减引用计数，仅当计数归 0（该作业最后一个结果处理任务结束）时才真正移除并停止刷新；
     * 计数仍大于 0 时保留心跳、不移除、不告警。对不存在的 key（真正的重复 stop 或 add 未成功注册）
     * 保持 already stopped 告警且不会产生负计数。
     *
     * @param jobInstanceId 作业实例 ID
     */
    public void stopKeepaliveTask(long jobInstanceId) {
        log.info("Stop running job keepalive task : {}", jobInstanceId);
        runningJobKeepaliveTasks.compute(jobInstanceId, (id, task) -> {
            if (task == null) {
                // 该作业已无存活心跳（真正的重复 stop，或 add 未成功注册），不产生负计数
                log.warn("keepalive task task already stopped, jobInstanceId: {}!", id);
                return null;
            }
            int refCount = task.decrementRefCount();
            if (refCount <= 0) {
                // 引用计数归 0，最后一个结果处理任务已结束，真正停止刷新并移除
                log.info("Remove running job keepalive task, jobInstanceId: {}", id);
                return null;
            }
            // 仍有其它结果处理任务（并行错峰批次）在运行，保留心跳继续刷新
            log.info("Decrement running job keepalive task refCount, jobInstanceId: {}, refCount: {}",
                id, refCount);
            return task;
        });
    }

    /**
     * 定时刷新作业存活心跳。 1min周期
     */
    @ScheduledOnOperationTimeZone(cron = "0 * * * * ?")
    public void refreshTaskKeepaliveInfo() {
        log.info("Refresh running job keepalive task start...");
        if (runningJobKeepaliveTasks.isEmpty()) {
            return;
        }
        Set<Long> refreshJobInstanceIds = new HashSet<>();
        long startInMills = System.currentTimeMillis();
        long currentTimestamp = System.currentTimeMillis();
        // 每作业仅一条 entry（无论并行错峰有多少批次），无需按批次刷新
        runningJobKeepaliveTasks.values().stream()
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
        log.info("Refresh running job keepalive task done! taskSize: {},refreshJobInstanceIds: {}",
            refreshJobInstanceIds.size(), refreshJobInstanceIds);
        long cost = System.currentTimeMillis() - startInMills;
        if (cost > 1000L) {
            log.info("Refresh running job keepalive task is slow, taskSize: {}, cost: {}",
                refreshJobInstanceIds.size(), cost);
        }
    }

    /**
     * 当前被管理的作业心跳数量（每作业一条 entry）。仅供可观测与测试使用。
     */
    int getRunningJobKeepaliveTaskCount() {
        return runningJobKeepaliveTasks.size();
    }

    /**
     * 指定作业当前的心跳引用计数，不存在返回 0。仅供可观测与测试使用。
     *
     * @param jobInstanceId 作业实例 ID
     */
    int getRefCount(long jobInstanceId) {
        KeepaliveTask task = runningJobKeepaliveTasks.get(jobInstanceId);
        return task == null ? 0 : task.getRefCount();
    }

    /**
     * 作业存活心跳任务。按 jobInstanceId 维护，内部持有引用计数：
     * 并行错峰模式下同一作业的多个批次结果处理任务共用一条心跳，计数归 0 才真正停止。
     * 所有对 refCount 的读写均发生在 {@link ConcurrentHashMap#compute} 内（持有分段锁），
     * 使用 AtomicInteger 表达意图并保证可见性。
     */
    private static class KeepaliveTask {
        private final long jobInstanceId;
        private volatile long timestamp;
        private final AtomicInteger refCount = new AtomicInteger(0);

        KeepaliveTask(long jobInstanceId, long timestamp) {
            this.jobInstanceId = jobInstanceId;
            this.timestamp = timestamp;
        }

        long getJobInstanceId() {
            return jobInstanceId;
        }

        long getTimestamp() {
            return timestamp;
        }

        void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }

        int incrementRefCount() {
            return refCount.incrementAndGet();
        }

        int decrementRefCount() {
            return refCount.decrementAndGet();
        }

        int getRefCount() {
            return refCount.get();
        }

        @Override
        public String toString() {
            return "KeepaliveTask{jobInstanceId=" + jobInstanceId
                + ", timestamp=" + timestamp
                + ", refCount=" + refCount.get() + '}';
        }
    }
}
