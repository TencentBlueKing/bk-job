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

package com.tencent.bk.job.manage.background.event.cmdb;

import com.tencent.bk.job.manage.service.CmdbEventCursorManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * CMDB事件游标管理器的Redis实现，将游标数据存储于Redis中
 */
@Slf4j
@Service
public class CmdbEventCursorManagerRedisImpl implements CmdbEventCursorManager {

    private static final String REDIS_KEY_CMDB_EVENT_LATEST_CURSOR_PREFIX = "job:manage:cmdbEventLatestCursor:";
    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public CmdbEventCursorManagerRedisImpl(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 尝试从Redis加载最近的已处理事件的游标
     *
     * @param tenantId            租户ID
     * @param watcherResourceName 监听的资源名称
     * @return 游标
     */
    @Override
    public String tryToLoadLatestCursor(String tenantId, String watcherResourceName) {
        try {
            return loadLatestCursor(tenantId, watcherResourceName);
        } catch (Throwable t) {
            String message = MessageFormatter.format(
                "Fail to loadLatestCursor for (tenantId={}, watcherResourceName={})",
                tenantId,
                watcherResourceName
            ).getMessage();
            log.error(message, t);
            return null;
        }
    }

    /**
     * 从Redis加载最近的已处理事件的游标
     *
     * @param tenantId            租户ID
     * @param watcherResourceName 监听的资源名称
     * @return 游标
     */
    private String loadLatestCursor(String tenantId, String watcherResourceName) {
        String redisKey = buildRedisKey(tenantId, watcherResourceName);
        String latestCursor = redisTemplate.opsForValue().get(redisKey);
        log.info(
            "Loaded latestCursor(tenantId={}, watcherResourceName={}) from redis: {}",
            tenantId,
            watcherResourceName,
            latestCursor
        );
        if (StringUtils.isBlank(latestCursor)) {
            return null;
        }
        return latestCursor;
    }

    /**
     * 尝试将最近的已处理事件的游标保存到Redis
     *
     * @param tenantId            租户ID
     * @param watcherResourceName 监听的资源名称
     * @param latestCursor        最近的已处理过的事件的游标
     */
    @Override
    public void tryToSaveLatestCursor(String tenantId, String watcherResourceName, String latestCursor) {
        try {
            saveLatestCursor(tenantId, watcherResourceName, latestCursor);
            if (log.isDebugEnabled()) {
                log.debug(
                    "Saved latestCursor to redis: tenantId={}, watcherResourceName={}, cursor={}",
                    tenantId,
                    watcherResourceName,
                    latestCursor
                );
            }
        } catch (Throwable t) {
            String message = MessageFormatter.format(
                "Fail to saveLatestCursor for (tenantId={}, watcherResourceName={})",
                tenantId,
                watcherResourceName
            ).getMessage();
            log.error(message, t);
        }
    }

    /**
     * 保存最近的已处理事件的游标到Redis
     *
     * @param tenantId            租户ID
     * @param watcherResourceName 监听的资源名称
     * @param latestCursor        最近的已处理过的事件的游标
     */
    private void saveLatestCursor(String tenantId, String watcherResourceName, String latestCursor) {
        if (StringUtils.isBlank(latestCursor)) {
            log.warn(
                "Do not save blank cursor(tenantId={}, watcherResourceName={}):{}, ignore",
                tenantId,
                watcherResourceName,
                latestCursor
            );
            return;
        }
        String redisKey = buildRedisKey(tenantId, watcherResourceName);
        redisTemplate.opsForValue().set(redisKey, latestCursor);
    }

    private String buildRedisKey(String tenantId, String watcherResourceName) {
        return REDIS_KEY_CMDB_EVENT_LATEST_CURSOR_PREFIX + watcherResourceName + ":" + tenantId;
    }
}
