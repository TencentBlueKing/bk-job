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

import com.tencent.bk.job.common.util.JobUUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 支持心跳的Redis锁配置
 */
@Setter
@Getter
@NoArgsConstructor
public class HeartBeatRedisLockConfig {

    /**
     * 锁过期时间，单位：毫秒
     */
    private long expireTimeMillis = 5_000L;
    /**
     * 锁心跳间隔时间，单位：毫秒
     */
    private long periodMillis = 2_000L;
    /**
     * 心跳线程名称
     */
    private String heartBeatThreadName = "redisKeyHeartBeatThread-" + JobUUID.getUUID().substring(0, 8);

    public static HeartBeatRedisLockConfig getDefault() {
        return new HeartBeatRedisLockConfig();
    }

    public HeartBeatRedisLockConfig(String heartBeatThreadName, long expireTimeMillis, long periodMillis) {
        this.heartBeatThreadName = heartBeatThreadName;
        this.expireTimeMillis = expireTimeMillis;
        this.periodMillis = periodMillis;
    }
}
