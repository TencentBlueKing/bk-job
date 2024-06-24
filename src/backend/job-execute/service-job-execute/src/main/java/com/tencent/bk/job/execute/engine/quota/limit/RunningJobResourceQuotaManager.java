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
import java.util.Map;

/**
 * 正在执行作业配置限制管理
 */
@Component
@Slf4j
public class RunningJobResourceQuotaManager {

    private final RedisTemplate<String, String> redisTemplate;
    private final RunningJobResourceQuotaStore runningJobResourceQuotaStore;

    private static final String RESOURCE_SCOPE_RUNNING_JOB_COUNT_HASH_KEY =
        "job:execute:running:job:count:resource_scope";
    private static final String APP_RUNNING_JOB_COUNT_HASH_KEY = "job:execute:running:job:count:app";
    private static final String RUNNING_JOB_HASH_KEY = "job:execute:running:job";

    private static final List<String> LUA_SCRIPT_KEYS = new ArrayList<>();

    static {
        LUA_SCRIPT_KEYS.add(RUNNING_JOB_HASH_KEY);
        LUA_SCRIPT_KEYS.add(RESOURCE_SCOPE_RUNNING_JOB_COUNT_HASH_KEY);
        LUA_SCRIPT_KEYS.add(APP_RUNNING_JOB_COUNT_HASH_KEY);
    }

    private static final String CHECK_QUOTA_LUA_SCRIPT =
        "local system_running_job_hash_key = KEYS[1]\n" +
            "local resource_scope_job_count_hash_key = KEYS[2]\n" +
            "local app_job_count_hash_key = KEYS[3]\n" +
            "local resource_scope = ARGV[1]\n" +
            "local app_code = ARGV[2]\n" +
            "\n" +
            "local system_limit = tonumber(ARGV[3])\n" +
            "local system_count = tonumber(redis.call('hlen', system_running_job_hash_key) or \"0\")\n" +
            "if system_count >= system_limit then\n" +
            "  return \"system_quota_limit\"\n" +
            "end\n" +
            "\n" +
            "local resource_scope_limit = tonumber(ARGV[4])\n" +
            "local resource_scope_count = tonumber(redis.call('hget', resource_scope_job_count_hash_key) or \"0\")\n" +
            "if resource_scope_count >= resource_scope_limit then\n" +
            "  return \"resource_scope_quota_limit\"\n" +
            "end\n" +
            "\n" +
            "if app_code ~= \"None\" then\n" +
            "  local app_limit = tonumber(ARGV[5])\n" +
            "  local app_count = tonumber(redis.call('hget', app_job_count_hash_key) or \"0\")\n" +
            "  if app_count >= app_limit then\n" +
            "    return \"app_quota_limit\"\n" +
            "  end\n" +
            "end\n" +
            "\n" +
            "return \"no_limit\"";

    private static final String ADD_JOB_LUA_SCRIPT =
        "local system_running_job_hash_key = KEYS[1]\n" +
            "local resource_scope_job_count_hash_key = KEYS[2]\n" +
            "local app_job_count_hash_key = KEYS[3]\n" +
            "local job_id = ARGV[1]\n" +
            "local resource_scope = ARGV[2]\n" +
            "local app_code = ARGV[3]\n" +
            "local job_create_time = ARGV[4]\n" +
            "\n" +
            "local add_result = redis.call('hsetnx', system_running_job_hash_key, job_id, job_create_time)\n" +
            "if add_result == 1 then\n" +
            "  redis.call('hincrby', resource_scope_job_count_hash_key, resource_scope, 1)\n" +
            "  \n" +
            "  if app_code ~= \"None\" then\n" +
            "    redis.call('hincrby', app_job_count_hash_key, app_code, 1)\n" +
            "  end\n" +
            "end";
    private static final String REMOVE_JOB_LUA_SCRIPT =
        "local system_running_job_hash_key = KEYS[1]\n" +
            "local resource_scope_job_count_hash_key = KEYS[2]\n" +
            "local app_job_count_hash_key = KEYS[3]\n" +
            "local job_id = ARGV[1]\n" +
            "local resource_scope = ARGV[2]\n" +
            "local app_code = ARGV[3]\n" +
            "\n" +
            "local del_result = redis.call('hdel', system_running_job_hash_key, job_id)\n" +
            "if del_result == 1 then\n" +
            "  redis.call('hincrby', resource_scope_job_count_hash_key, resource_scope, -1)\n" +
            "  \n" +
            "  if app_code ~= \"None\" then\n" +
            "    redis.call('hincrby', app_job_count_hash_key, app_code, -1)\n" +
            "  end\n" +
            "end";

    public RunningJobResourceQuotaManager(StringRedisTemplate redisTemplate,
                                          RunningJobResourceQuotaStore runningJobResourceQuotaStore) {
        this.redisTemplate = redisTemplate;
        this.runningJobResourceQuotaStore = runningJobResourceQuotaStore;
    }

    public void addJob(String appCode, ResourceScope resourceScope, long jobInstanceId) {
        long startTime = System.currentTimeMillis();
        RedisScript<Void> script = RedisScript.of(ADD_JOB_LUA_SCRIPT, Void.class);

        redisTemplate.execute(
            script,
            LUA_SCRIPT_KEYS,
            String.valueOf(jobInstanceId),
            resourceScope.toResourceScopeUniqueId(),
            convertAppCode(appCode),
            String.valueOf(System.currentTimeMillis())
        );

        long cost = System.currentTimeMillis() - startTime;
        if (log.isDebugEnabled()) {
            log.debug("RunningJobResourceQuotaManager - Add job cost : {} ms", cost);
        }
    }

    private String convertAppCode(String appCode) {
        return StringUtils.isNotBlank(appCode) ? appCode : "None";
    }

    public void removeJob(String appCode, ResourceScope resourceScope, long jobInstanceId) {
        long startTime = System.currentTimeMillis();
        RedisScript<Void> script = RedisScript.of(REMOVE_JOB_LUA_SCRIPT, Void.class);

        redisTemplate.execute(
            script,
            LUA_SCRIPT_KEYS,
            String.valueOf(jobInstanceId),
            resourceScope.toResourceScopeUniqueId(),
            convertAppCode(appCode)
        );

        long cost = System.currentTimeMillis() - startTime;
        if (log.isDebugEnabled()) {
            log.debug("RunningJobResourceQuotaManager - Remove job cost : {} ms", cost);
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
        RedisScript<String> script = RedisScript.of(CHECK_QUOTA_LUA_SCRIPT, String.class);


        long systemLimit = runningJobResourceQuotaStore.getSystemQuotaLimit();
        long resourceScopeLimit = runningJobResourceQuotaStore.getQuotaLimitByResourceScope(resourceScope);
        // 是否通过第三方应用 Open API 方式调用作业平台产生的作业
        boolean isJobFrom3rdApp = StringUtils.isNotEmpty(appCode);
        long appLimit = isJobFrom3rdApp ? runningJobResourceQuotaStore.getQuotaLimitByAppCode(appCode) : Long.MAX_VALUE;

        String checkResourceQuotaResult = redisTemplate.execute(
            script,
            LUA_SCRIPT_KEYS,
            resourceScope.toResourceScopeUniqueId(),
            convertAppCode(appCode),
            String.valueOf(systemLimit),
            String.valueOf(resourceScopeLimit),
            String.valueOf(appLimit)
        );

        long cost = System.currentTimeMillis() - startTime;
        if (log.isDebugEnabled()) {
            log.debug("CheckRunningJobResourceQuotaLimit cost: {} ms", cost);
        }
        return ResourceQuotaCheckResultEnum.valOf(checkResourceQuotaResult);
    }

    public long getRunningJobTotal() {
        return redisTemplate.opsForHash().size(RUNNING_JOB_HASH_KEY);
    }

    public Map<String, Long> getAppRunningJobCount() {
        return redisTemplate.<String, Long>opsForHash().entries(APP_RUNNING_JOB_COUNT_HASH_KEY);
    }

    public Map<String, Long> getResourceScopeRunningJobCount() {
        return redisTemplate.<String, Long>opsForHash().entries(RESOURCE_SCOPE_RUNNING_JOB_COUNT_HASH_KEY);
    }
}
