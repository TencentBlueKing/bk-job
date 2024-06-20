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
import com.tencent.bk.job.common.service.quota.RunningJobResourceQuotaStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 正在执行作业配置限制管理
 */
@Component
@Slf4j
public class RunningJobResourceQuotaManager {

    private final RedisTemplate<String, String> redisTemplate;
    private final RunningJobResourceQuotaStore runningJobResourceQuotaStore;

    private static final String RESOURCE_SCOPE_RUNNING_JOB_ZSET_KEY_PREFIX = "job:execute:running:job:resource_scope:";
    private static final String APP_RUNNING_JOB_ZSET_KEY_PREFIX = "job:execute:running:job:app:";

    private static final String CHECK_QUOTA_LUA_SCRIPT =
        "local resource_scope_key = KEYS[1]\n" +
            "local resource_scope_limit = tonumber(ARGV[1])\n" +
            "local running_job_count_resource_scope = tonumber(redis.call('zcard', resource_scope_key) or \"0\")\n" +
            "redis.call('expire', resource_scope_key, 86400)\n" +
            "if running_job_count_resource_scope >= resource_scope_limit then\n" +
            "  return 2\n" +
            "end\n" +
            "if KEYS[2] ~= \"None\" then\n" +
            "  local app_key = KEYS[2]\n" +
            "  local app_limit = tonumber(ARGV[2])\n" +
            "  local running_job_count_app = tonumber(redis.call('zcard', app_key) or \"0\")\n" +
            "  \n" +
            "  redis.call('expire', app_key, 86400)\n" +
            "  \n" +
            "  if running_job_count_app >= app_limit then\n" +
            "    return 3\n" +
            "  end\n" +
            "end\n" +
            "return 1";


    public RunningJobResourceQuotaManager(StringRedisTemplate redisTemplate,
                                          RunningJobResourceQuotaStore runningJobResourceQuotaStore) {
        this.redisTemplate = redisTemplate;
        this.runningJobResourceQuotaStore = runningJobResourceQuotaStore;
    }

    public void addJob(String appCode, ResourceScope resourceScope, long jobInstanceId) {
        String resourceScopeZSetKey = buildResourceScopeRunningJobZSetKey(resourceScope);
        long currentTimestamp = System.currentTimeMillis();
        redisTemplate.opsForZSet().add(resourceScopeZSetKey, String.valueOf(jobInstanceId), currentTimestamp);
        if (StringUtils.isNotEmpty(appCode)) {
            String appZSetKey = buildAppRunningJobZSetKey(appCode);
            redisTemplate.opsForZSet().add(appZSetKey, String.valueOf(jobInstanceId), currentTimestamp);
        }
    }

    public void removeJob(String appCode, ResourceScope resourceScope, long jobInstanceId) {
        String resourceScopeZSetKey = buildResourceScopeRunningJobZSetKey(resourceScope);
        redisTemplate.opsForZSet().remove(resourceScopeZSetKey, String.valueOf(jobInstanceId));
        if (StringUtils.isNotEmpty(appCode)) {
            String appZSetKey = buildAppRunningJobZSetKey(appCode);
            redisTemplate.opsForZSet().remove(appZSetKey, String.valueOf(jobInstanceId));
        }
    }

    /**
     * 业务是否超过正在执行的任务数量配额
     *
     * @param appCode       作业发起的蓝鲸应用 code
     * @param resourceScope 资源管理空间
     */
    public ResourceQuotaCheckResultEnum checkResourceQuotaLimit(String appCode, ResourceScope resourceScope) {
        long startTime = System.currentTimeMillis();
        RedisScript<Integer> script = RedisScript.of(CHECK_QUOTA_LUA_SCRIPT, Integer.class);

        // 是否通过第三方应用方式调用作业平台产生的作业
        boolean isJobFrom3rdApp = StringUtils.isNotEmpty(appCode);

        List<String> keyList = new ArrayList<>();
        keyList.add("job:execute:running:job:resource:scope:" + resourceScope.getResourceScopeUniqueId());
        keyList.add(isJobFrom3rdApp ? "job:execute:running:job:app:" + appCode : "None");

        long resourceScopeLimit = runningJobResourceQuotaStore.getQuotaLimitByResourceScope(resourceScope);

        Integer checkResourceQuotaResult;
        if (isJobFrom3rdApp) {
            long appLimit = runningJobResourceQuotaStore.getQuotaLimitByAppCode(appCode);
            checkResourceQuotaResult = redisTemplate.execute(script, keyList, resourceScopeLimit, appLimit);
        } else {
            checkResourceQuotaResult = redisTemplate.execute(script, keyList, resourceScopeLimit);
        }

        long cost = System.currentTimeMillis() - startTime;
        if (log.isDebugEnabled()) {
            log.debug("CheckResourceQuotaLimit cost: {} ms", cost);
        }
        return ResourceQuotaCheckResultEnum.valOf(checkResourceQuotaResult);
    }

    private String buildResourceScopeRunningJobZSetKey(ResourceScope resourceScope) {
        return RESOURCE_SCOPE_RUNNING_JOB_ZSET_KEY_PREFIX
            + resourceScope.getType().getValue() + ":" + resourceScope.getId();
    }

    private String buildAppRunningJobZSetKey(String appCode) {
        return APP_RUNNING_JOB_ZSET_KEY_PREFIX + appCode;
    }
}
