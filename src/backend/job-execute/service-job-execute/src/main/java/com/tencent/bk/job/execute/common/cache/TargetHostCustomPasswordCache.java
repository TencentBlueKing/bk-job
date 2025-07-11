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

package com.tencent.bk.job.execute.common.cache;

import com.tencent.bk.job.execute.model.AgentCustomPasswordDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 目标主机密码缓存
 */
@Slf4j
@Component
public class TargetHostCustomPasswordCache {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String KEY_TARGET_AGENT_PWD_CACHE_PREFIX = "job:execute:agent:pwd:";

    /**
     * 作业执行失败，密码1h过期
     */
    private static final Long TASK_FAIL_PWD_EXPIRE_TIME = 3600L;

    /**
     * 密码缓存的最长时间1天
     */
    private static final Long MAX_EXPIRE_TIME = 86400L;

    public TargetHostCustomPasswordCache(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addCache(List<AgentCustomPasswordDTO> passwordList, Long taskInstanceId) {
        if (CollectionUtils.isEmpty(passwordList)) {
            return;
        }
        String key = buildCacheKey(taskInstanceId);
        redisTemplate.opsForValue().set(key, passwordList, MAX_EXPIRE_TIME, TimeUnit.SECONDS);
    }

    public void deleteCache(Long taskInstanceId) {
        redisTemplate.delete(buildCacheKey(taskInstanceId));
    }

    @SuppressWarnings("unchecked")
    public List<AgentCustomPasswordDTO> getCache(Long taskInstanceId) {
        Object cached = redisTemplate.opsForValue().get(buildCacheKey(taskInstanceId));
        if (cached instanceof List) {
            try {
                return (List<AgentCustomPasswordDTO>) cached;
            } catch (ClassCastException e) {
                log.warn("Agent custom pwd cache type mismatch for taskInstanceId={}", taskInstanceId, e);
            }
        }
        return Collections.emptyList();
    }

    public void refreshExpire(Long taskInstanceId) {
        redisTemplate.expire(buildCacheKey(taskInstanceId), TASK_FAIL_PWD_EXPIRE_TIME, TimeUnit.SECONDS);
    }

    private String buildCacheKey(Long taskInstanceId) {
        return KEY_TARGET_AGENT_PWD_CACHE_PREFIX + taskInstanceId;
    }
}
