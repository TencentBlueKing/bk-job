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

package com.tencent.bk.job.manage.quota.limit;

import com.tencent.bk.job.common.exception.JobMicroServiceBootException;
import com.tencent.bk.job.common.metrics.CommonMetricTags;
import com.tencent.bk.job.common.metrics.CommonMetricValues;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.resource.quota.ResourceQuotaLimit;
import com.tencent.bk.job.common.service.quota.ResourceQuotaCheckResultEnum;
import com.tencent.bk.job.common.service.quota.SendNotifyResourceQuotaStore;
import com.tencent.bk.job.common.util.date.DateUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.extern.slf4j.Slf4j;
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
import java.util.List;
import java.util.stream.Collectors;

/**
 * 每天发送消息通知配额管理器
 */
@Component
@Slf4j
public class SendNotifyResourceQuotaManager {
    private final RedisTemplate<String, String> redisTemplate;
    private final SendNotifyResourceQuotaStore sendNotifyResourceQuotaStore;
    private final MeterRegistry meterRegistry;

    // lua脚本内容
    private static String CHECK_AND_INCREMENT_LUA_SCRIPT;
    private static String ROLLBACK_LUA_SCRIPT;

    // 发送消息通知次数key前缀，需拼接当天的日期字符串,
    private static final String REDIS_KEY_PREFIX_SEND_NOTIFY_COUNT = "job:manage:send:notify:count:";
    // 资源维度统计发送通知次数的Key前缀, 需拼接当天的日期字符串
    private static final String REDIS_KEY_PREFIX_RESOURCE_SCOPE_SEND_NOTIFY_COUNT =
        "job:manage:send:notify:count:resource_scope:";
    // 用户维度统计发送通知次数的Key前缀, 需拼接当天的日期字符串
    private static final String REDIS_KEY_PREFIX_USER_SEND_NOTIFY_COUNT = "job:manage:send:notify:count:user:";

    // Redis key按天存储，过期时间为1天
    private static final String REDIS_KEY_TTL = "86400";

    private static final String METRIC_SEND_NOTIFY_RESOURCE_QUOTA_LIMIT_EXCEED_TOTAL =
        "send_notify_resource_quota_limit_exceed_total";

    // 慢日志阈值，单位毫秒
    private static final long SLOW_LOG_THRESHOLD_MS = 10;

    static {
        loadLuaScript();
    }

    private static void loadLuaScript() {
        try {
            log.info("Load send notify resource quota lua script start");
            ROLLBACK_LUA_SCRIPT = readContentFromClasspathFile("lua/rollback_send_notify.lua");
            CHECK_AND_INCREMENT_LUA_SCRIPT = readContentFromClasspathFile("lua/check_and_increment_send_notify.lua");
            log.info("Load send notify resource quota lua script successfully!");
        } catch (Throwable e) {
            throw new JobMicroServiceBootException("Load send notify resource quota lua script error", e);
        }
    }

    private static String readContentFromClasspathFile(String path) throws Exception {
        try (InputStream inputStream =
                 new ClassPathResource(path).getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    public SendNotifyResourceQuotaManager(StringRedisTemplate redisTemplate,
                                          SendNotifyResourceQuotaStore sendNotifyResourceQuotaStore,
                                          MeterRegistry meterRegistry) {
        this.redisTemplate = redisTemplate;
        this.sendNotifyResourceQuotaStore = sendNotifyResourceQuotaStore;
        this.meterRegistry = meterRegistry;
    }

    /**
     * 校验消息通知发送限制，校验通过则增加计数
     */
    public ResourceQuotaCheckResultEnum checkAndIncrement(ResourceScope resourceScope, String userId) {
        if (!sendNotifyResourceQuotaStore.isQuotaLimitEnabled()) {
            return ResourceQuotaCheckResultEnum.NO_LIMIT;
        }

        long startTime = System.currentTimeMillis();
        long systemLimit = sendNotifyResourceQuotaStore.getSystemQuotaLimit();
        long resourceScopeLimit = resourceScope == null ? ResourceQuotaLimit.UNLIMITED_VALUE
            : sendNotifyResourceQuotaStore.getQuotaLimitByResourceScope(resourceScope);
        long userLimit = userId == null ? ResourceQuotaLimit.UNLIMITED_VALUE
            : sendNotifyResourceQuotaStore.getQuotaLimitByUserId(userId);
        String scopeUniqueId = resourceScope != null ? resourceScope.toResourceScopeUniqueId() : null;

        String result = redisTemplate.execute(
            RedisScript.of(CHECK_AND_INCREMENT_LUA_SCRIPT, String.class),
            getLuaScriptKeys(),
            REDIS_KEY_TTL,
            scopeUniqueId,
            userId,
            String.valueOf(systemLimit),
            String.valueOf(resourceScopeLimit),
            String.valueOf(userLimit)
        );

        long cost = System.currentTimeMillis() - startTime;
        if (cost > SLOW_LOG_THRESHOLD_MS) {
            log.warn("SLOW: Increment send notify limit time over {}ms, resourceScope={}, userId={}, cost: {} ms",
                SLOW_LOG_THRESHOLD_MS, scopeUniqueId, userId, cost);
        } else {
            log.debug("Increment send notify limit, resourceScope={}, userId={}, cost: {} ms",
                scopeUniqueId, userId, cost);
        }

        ResourceQuotaCheckResultEnum checkResult = ResourceQuotaCheckResultEnum.valOf(result);
        if (checkResult.isExceedLimit()) {
            recordExceedQuotaLimitRecord(checkResult, resourceScope, userId);
        }
        return checkResult;
    }

    /**
     * 回滚消息通知发送计数
     */
    public void rollback(ResourceScope resourceScope, String userId) {
        if (!sendNotifyResourceQuotaStore.isQuotaLimitEnabled()) {
            return ;
        }

        long startTime = System.currentTimeMillis();
        String scopeUniqueId = resourceScope != null ? resourceScope.toResourceScopeUniqueId() : null;
        redisTemplate.execute(
            RedisScript.of(ROLLBACK_LUA_SCRIPT, Void.class),
            getLuaScriptKeys(),
            scopeUniqueId,
            userId
        );
        long cost = System.currentTimeMillis() - startTime;
        if (cost > SLOW_LOG_THRESHOLD_MS) {
            log.warn("SLOW: Rollback send notify limit time over {}ms, resourceScope={}, userId={}, cost={}ms",
                SLOW_LOG_THRESHOLD_MS, scopeUniqueId, userId, cost);
        } else {
            log.debug("Rollback send notify limit, resourceScope={}, userId={}, cost={}ms",
                scopeUniqueId, userId, cost);
        }
    }

    private List<String> getLuaScriptKeys() {
        // redis key按天存储
        List<String> luaScriptKeys = new ArrayList<>();
        String dateStr = DateUtils.getCurrentDateStr();
        luaScriptKeys.add(REDIS_KEY_PREFIX_SEND_NOTIFY_COUNT + dateStr);
        luaScriptKeys.add(REDIS_KEY_PREFIX_RESOURCE_SCOPE_SEND_NOTIFY_COUNT + dateStr);
        luaScriptKeys.add(REDIS_KEY_PREFIX_USER_SEND_NOTIFY_COUNT + dateStr);
        return luaScriptKeys;
    }

    private void recordExceedQuotaLimitRecord(
        ResourceQuotaCheckResultEnum checkResult,
        ResourceScope resourceScope,
        String userId
    ) {
        String resourceScopeTag =
            resourceScope != null ? resourceScope.toResourceScopeUniqueId() : CommonMetricValues.NONE;
        String userTag = StringUtils.isNotBlank(userId) ? userId : CommonMetricValues.NONE;
        meterRegistry.counter(
            METRIC_SEND_NOTIFY_RESOURCE_QUOTA_LIMIT_EXCEED_TOTAL,
            Tags.of(
                CommonMetricTags.KEY_RESOURCE_SCOPE, resourceScopeTag,
                CommonMetricTags.KEY_USER_ID, userTag,
                CommonMetricTags.KEY_QUOTA_TYPE, checkResult.getValue()
            )
        ).increment();
    }
}
