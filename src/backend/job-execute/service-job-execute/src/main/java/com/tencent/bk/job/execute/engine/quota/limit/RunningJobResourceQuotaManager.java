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

import com.tencent.bk.job.common.exception.JobMicroServiceBootException;
import com.tencent.bk.job.common.metrics.CommonMetricTags;
import com.tencent.bk.job.common.metrics.CommonMetricValues;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.service.quota.RunningJobResourceQuotaStore;
import com.tencent.bk.job.execute.constants.RedisKeys;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 正在执行作业配额限制管理
 */
@Component
@Slf4j
public class RunningJobResourceQuotaManager {

    private final RedisTemplate<String, String> redisTemplate;
    private final RunningJobResourceQuotaStore runningJobResourceQuotaStore;

    private final MeterRegistry meterRegistry;

    private static final String RESOURCE_SCOPE_RUNNING_JOB_COUNT_HASH_KEY =
        "job:execute:running:job:count:resource_scope";
    private static final String APP_RUNNING_JOB_COUNT_HASH_KEY = "job:execute:running:job:count:app";

    private static final List<String> LUA_SCRIPT_KEYS = new ArrayList<>();

    private static String CHECK_QUOTA_LUA_SCRIPT;

    private static String ADD_JOB_LUA_SCRIPT;
    private static String REMOVE_JOB_LUA_SCRIPT;

    private static final String METRIC_RUNNING_JOB_RESOURCE_QUOTA_LIMIT_EXCEED_TOTAL =
        "job_running_job_resource_quota_limit_exceed_total";

    private static final long JOB_EXPIRE_TIME = 3600 * 1000L;

    static {
        LUA_SCRIPT_KEYS.add(RedisKeys.RUNNING_JOB_ZSET_KEY);
        LUA_SCRIPT_KEYS.add(RESOURCE_SCOPE_RUNNING_JOB_COUNT_HASH_KEY);
        LUA_SCRIPT_KEYS.add(APP_RUNNING_JOB_COUNT_HASH_KEY);

        loadLuaScript();
    }

    private static void loadLuaScript() {
        try {
            log.info("Load running job resource quota lua script start");

            CHECK_QUOTA_LUA_SCRIPT = readContentFromClasspathFile("lua/check_running_job_quota_limit.lua");
            ADD_JOB_LUA_SCRIPT = readContentFromClasspathFile("lua/add_running_job.lua");
            REMOVE_JOB_LUA_SCRIPT = readContentFromClasspathFile("lua/remove_running_job.lua");

            log.info("Load running job resource quota lua script successfully!");
        } catch (Throwable e) {
            throw new JobMicroServiceBootException("Load running job resource quota lua script error", e);
        }
    }

    private static String readContentFromClasspathFile(String path) throws Exception {
        try (InputStream inputStream =
                 new ClassPathResource(path).getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }


    public RunningJobResourceQuotaManager(StringRedisTemplate redisTemplate,
                                          RunningJobResourceQuotaStore runningJobResourceQuotaStore,
                                          MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.runningJobResourceQuotaStore = runningJobResourceQuotaStore;
        this.meterRegistry = meterRegistry;
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
    public ResourceQuotaCheckResultEnum checkResourceQuotaLimit(String appCode,
                                                                ResourceScope resourceScope) {
        if (!runningJobResourceQuotaStore.isQuotaLimitEnabled()) {
            return ResourceQuotaCheckResultEnum.NO_LIMIT;
        }
        long startTime = System.currentTimeMillis();
        RedisScript<String> script = RedisScript.of(CHECK_QUOTA_LUA_SCRIPT, String.class);

        long systemLimit = runningJobResourceQuotaStore.getSystemQuotaLimit();
        long resourceScopeLimit = runningJobResourceQuotaStore.getQuotaLimitByResourceScope(resourceScope);
        // 是否通过第三方应用 Open API 方式调用作业平台产生的作业
        boolean isJobFrom3rdApp = StringUtils.isNotEmpty(appCode);
        long appLimit = isJobFrom3rdApp ? runningJobResourceQuotaStore.getQuotaLimitByAppCode(appCode) :
            Long.MAX_VALUE;

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
        ResourceQuotaCheckResultEnum checkResult = ResourceQuotaCheckResultEnum.valOf(checkResourceQuotaResult);
        if (checkResult.isExceedLimit()) {
            recordExceedQuotaLimitRecord(appCode, resourceScope);
        }

        return checkResult;
    }

    private void recordExceedQuotaLimitRecord(String appCode, ResourceScope resourceScope) {
        meterRegistry.counter(
                METRIC_RUNNING_JOB_RESOURCE_QUOTA_LIMIT_EXCEED_TOTAL,
                Tags.of(CommonMetricTags.KEY_RESOURCE_SCOPE, resourceScope.toResourceScopeUniqueId())
                    .and(CommonMetricTags.KEY_APP_CODE, StringUtils.isNotBlank(appCode) ?
                        appCode : CommonMetricValues.NONE))
            .increment();
    }

    public long getRunningJobTotal() {
        Long count = redisTemplate.opsForZSet().size(RedisKeys.RUNNING_JOB_ZSET_KEY);
        return count != null ? count : 0L;
    }

    public Map<String, Long> getAppRunningJobCount() {
        Map<String, String> countMap =
            redisTemplate.<String, String>opsForHash().entries(APP_RUNNING_JOB_COUNT_HASH_KEY);
        if (countMap.isEmpty()) {
            return null;
        }
        return filterEmptyCountAndConvert(countMap);
    }

    public Map<String, Long> getResourceScopeRunningJobCount() {
        Map<String, String> countMap =
            redisTemplate.<String, String>opsForHash().entries(RESOURCE_SCOPE_RUNNING_JOB_COUNT_HASH_KEY);
        if (countMap.isEmpty()) {
            return null;
        }
        return filterEmptyCountAndConvert(countMap);
    }

    private Map<String, Long> filterEmptyCountAndConvert(Map<String, String> map) {
        Map<String, Long> finalMap = new HashMap<>();
        map.forEach((k, v) -> {
            long count = Long.parseLong(v);
            if (count == 0) {
                return;
            }
            finalMap.put(k, count);
        });
        return finalMap;
    }

    public Set<Long> getNotAliveJobInstanceIds() {
        try {
            // 1 小时过期
            long EXPIRE_AT = System.currentTimeMillis() - JOB_EXPIRE_TIME;
            Set<String> notAliveJobInstanceIds = redisTemplate.opsForZSet()
                .rangeByScore(
                    RedisKeys.RUNNING_JOB_ZSET_KEY,
                    -1,
                    EXPIRE_AT
                );
            if (CollectionUtils.isEmpty(notAliveJobInstanceIds)) {
                return Collections.emptySet();
            }
            return notAliveJobInstanceIds.stream().map(Long::parseLong).collect(Collectors.toSet());
        } catch (Throwable e) {
            return Collections.emptySet();
        }
    }
}
