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

import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.quota.ResourceScopeResourceQuotaManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 正在执行作业配置限制管理
 */
@Component
public class RunningJobQuoteManager {

    private final RedisTemplate<String, String> redisTemplate;
    private final ResourceScopeResourceQuotaManager resourceScopeResourceQuotaManager;

    // 3天过期
    private static final int EXPIRE_DAYS = 3;
    private static final String ZSET_KEY_PREFIX = "job:execute:running:job:";


    public RunningJobQuoteManager(StringRedisTemplate redisTemplate,
                                  ResourceScopeResourceQuotaManager resourceScopeResourceQuotaManager) {
        this.redisTemplate = redisTemplate;
        this.resourceScopeResourceQuotaManager = resourceScopeResourceQuotaManager;
    }

    public void addJob(ResourceScope resourceScope, long jobInstanceId) {
        String key = buildKey(resourceScope);
        redisTemplate.opsForZSet().add(key, String.valueOf(jobInstanceId), System.currentTimeMillis());
    }

    public void removeJob(ResourceScope resourceScope, long jobInstanceId) {
        String key = buildKey(resourceScope);
        redisTemplate.opsForZSet().remove(key, String.valueOf(jobInstanceId));
    }

    /**
     * 业务是否超过正在执行的任务数量配额
     *
     * @param resourceScope 资源管理空间
     */
    public boolean isExceedJobQuotaLimit(ResourceScope resourceScope) {
        String key = buildKey(resourceScope);
        Long currentRunningJobCount = redisTemplate.opsForZSet().zCard(key);
        if (currentRunningJobCount == null) {
            currentRunningJobCount = 0L;
        }
        long limit = resourceScopeResourceQuotaManager.getJobInstanceQuota(resourceScope);

        return currentRunningJobCount < limit;
    }

    private String buildKey(ResourceScope resourceScope) {
        return ZSET_KEY_PREFIX + resourceScope.getType().getValue() + ":" + resourceScope.getId();
    }
}
