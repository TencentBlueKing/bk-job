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

package com.tencent.bk.job.common.redis.util;


import com.tencent.bk.job.common.util.ThreadUtils;
import io.lettuce.core.RedisCommandInterruptedException;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

@Slf4j
public class RedisKeyHeartBeatThread extends Thread {
    private final RedisTemplate<String, String> redisTemplate;
    @Setter
    private volatile boolean runFlag;
    private final String redisKey;
    private final String redisValue;
    private final Long expireTimeMillis;
    private final Long periodMillis;

    public RedisKeyHeartBeatThread(RedisTemplate<String, String> redisTemplate,
                                   String redisKey,
                                   String redisValue,
                                   Long expireTimeMillis,
                                   Long periodMillis) {
        this.redisTemplate = redisTemplate;
        this.redisKey = redisKey;
        this.redisValue = redisValue;
        this.expireTimeMillis = expireTimeMillis;
        this.periodMillis = periodMillis;
        this.runFlag = true;
    }

    public void stopAtOnce() {
        setRunFlag(false);
        Boolean result = redisTemplate.delete(redisKey);
        log.debug("stopAtOnce, delete redis key:{}, result={}", redisKey, result);
        interrupt();
    }

    @Override
    public void run() {
        try {
            while (runFlag) {
                redisTemplate.opsForValue().set(redisKey, redisValue, expireTimeMillis, TimeUnit.MILLISECONDS);
                ThreadUtils.sleep(periodMillis, false);
            }
        } catch (Throwable t) {
            String msg = MessageFormatter.format(
                "RedisKeyHeartBeatThread {} quit unexpectedly:",
                this.getName()
            ).getMessage();
            // 主动终止线程产生的异常只打印调试级别日志
            if (!causeByStopAtOnceInterrupt(t)) {
                log.error(msg, t);
            } else {
                log.debug(msg, t);
            }
        } finally {
            deleteRedisKeySafely();
        }
    }

    private void deleteRedisKeySafely() {
        try {
            Boolean result = redisTemplate.delete(redisKey);
            log.debug("delete redis key:{}, result={}", redisKey, result);
        } catch (Throwable e) {
            // 主动终止线程产生的异常只打印调试级别日志
            if (!causeByStopAtOnceInterrupt(e)) {
                log.error("Delete redis key fail", e);
            } else {
                log.debug("Delete redis key fail", e);
            }
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean causeByStopAtOnceInterrupt(Throwable t) {
        if (runFlag) {
            return false;
        }
        if (t instanceof RedisSystemException) {
            Throwable cause = t.getCause();
            return cause instanceof RedisCommandInterruptedException;
        }
        return false;
    }
}
