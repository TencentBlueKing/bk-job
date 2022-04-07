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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

@Slf4j
public class RedisKeyHeartBeatThread extends Thread {
    private final RedisTemplate<String, String> redisTemplate;
    private volatile boolean runFlag;
    private final String redisKey;
    private final String redisValue;
    private final Long expireTimeMillis;
    private final Long periodMillis;

    public RedisKeyHeartBeatThread(RedisTemplate<String, String> redisTemplate, String redisKey, String redisValue,
                                   Long expireTimeMillis, Long periodMillis) {
        this.redisTemplate = redisTemplate;
        this.redisKey = redisKey;
        this.redisValue = redisValue;
        this.expireTimeMillis = expireTimeMillis;
        this.periodMillis = periodMillis;
        this.runFlag = true;
    }

    public void stopAtOnce() {
        setRunFlag(false);
        redisTemplate.delete(redisKey);
    }

    public void setRunFlag(boolean runFlag) {
        this.runFlag = runFlag;
    }

    @Override
    public void run() {
        try {
            while (runFlag) {
                redisTemplate.opsForValue().set(redisKey, redisValue, expireTimeMillis, TimeUnit.MILLISECONDS);
                ThreadUtils.sleep(periodMillis);
            }
            redisTemplate.delete(redisKey);
            log.info("RedisKeyHeartBeatThread {} quit normally", this.getName());
        } catch (Throwable t) {
            log.error("RedisKeyHeartBeatThread {} quit unexpectedly:", this.getName(), t);
            redisTemplate.delete(redisKey);
        }
    }
}
