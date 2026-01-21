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

package com.tencent.bk.job.common.redis.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.Callable;

/**
 * 分布式唯一任务：在多个实例同时触发的情况下，只会有一个实例执行，唯一性通过同一个Redis Key保证
 */
@Slf4j
public class DistributedUniqueTask<V> {

    private final RedisTemplate<String, String> redisTemplate;
    private final String name;
    private final String redisKey;
    private final String redisValue;
    private final Callable<V> task;

    public DistributedUniqueTask(RedisTemplate<String, String> redisTemplate,
                                 String name,
                                 String redisKey,
                                 String redisValue,
                                 Callable<V> task) {
        this.redisTemplate = redisTemplate;
        this.name = name;
        this.redisKey = redisKey;
        this.redisValue = redisValue;
        this.task = task;
    }

    public V execute() throws Exception {
        HeartBeatRedisLockConfig config = HeartBeatRedisLockConfig.getDefault();
        config.setHeartBeatThreadName(name + "-RedisKeyHeartBeatThread");
        HeartBeatRedisLock lock = new HeartBeatRedisLock(
            redisTemplate,
            redisKey,
            redisValue,
            config
        );
        LockResult lockResult = lock.lock();
        if (!lockResult.isLockGotten()) {
            log.info(
                "lock {} gotten by another instance: {}, return",
                redisKey,
                lockResult.getLockValue()
            );
            return null;
        }
        try {
            return task.call();
        } finally {
            lockResult.tryToRelease();
        }
    }
}
