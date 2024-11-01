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

package com.tencent.bk.job.execute.auth.impl;

import com.tencent.bk.job.common.redis.BaseRedisCache;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.execute.config.IamHostTopoPathProperties;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 主机拓扑路径缓存
 */
@Slf4j
public class HostTopoPathCache extends BaseRedisCache {

    private final RedisTemplate<String, Object> redisTemplate;
    private final IamHostTopoPathProperties iamHostTopoPathProperties;

    public HostTopoPathCache(RedisTemplate<String, Object> redisTemplate,
                             IamHostTopoPathProperties iamHostTopoPathProperties,
                             MeterRegistry meterRegistry) {
        super(meterRegistry, "HostTopoPathCache");
        this.redisTemplate = redisTemplate;
        this.iamHostTopoPathProperties = iamHostTopoPathProperties;
    }

    /**
     * 批量根据hostId查询缓存的主机拓扑路径实例
     *
     * @param hostIds 主机ID列表
     * @return 缓存的主机拓扑路径列表, 按照传入的hostIds排序。如果该主机不存在，List中对应索引的值为null
     */
    public List<HostTopoPathEntry> batchGetHostTopoPathByHostIds(List<Long> hostIds) {
        List<String> keys = hostIds.stream().map(this::buildHostIdKey).collect(Collectors.toList());
        return getHostTopoPathByKeys(keys);
    }

    private List<HostTopoPathEntry> getHostTopoPathByKeys(List<String> keys) {
        long hitCount = 0;
        long missCount = 0;
        try {
            List<Object> results = redisTemplate.opsForValue().multiGet(keys);
            // 通过 Object 间接强制转换 List
            List<HostTopoPathEntry> foundHosts = results == null ?
                Collections.emptyList() : (List<HostTopoPathEntry>) (Object) results;

            // multiGet 获取到的 list 中的元素可能为 null （如果key 不存在)
            hitCount = foundHosts.stream().filter(Objects::nonNull).count();
            missCount = keys.size() - hitCount;

            return foundHosts;
        } catch (Exception e) {
            log.warn("Batch get host in cache exception", e);
            hitCount = 0;
            missCount = keys.size();
            return Collections.emptyList();
        } finally {
            if (hitCount > 0) {
                addHits(hitCount);
            }
            if (missCount > 0) {
                addMisses(missCount);
            }
        }
    }

    /**
     * 删除缓存中的主机拓扑路径实例
     *
     * @param hostId 主机ID
     */
    public void deleteHostTopoPath(Long hostId) {
        String hostIdKey = buildHostIdKey(hostId);
        redisTemplate.delete(hostIdKey);
    }

    /**
     * 更新缓存中的主机拓扑路径实例
     *
     * @param hostTopoPathEntry 主机拓扑路径实例
     */
    public void addOrUpdateHostTopoPath(HostTopoPathEntry hostTopoPathEntry) {
        String timeFormat = "yyyy-MM-dd HH:mm:ss.SSS";
        hostTopoPathEntry.setCacheTime(TimeUtil.formatTime(System.currentTimeMillis(), timeFormat));
        log.debug(
            "Update hostTopoPath cache, hostId: {}, hostTopoPath: {}",
            hostTopoPathEntry.getHostId(),
            hostTopoPathEntry
        );
        String hostIdKey = buildHostIdKey(hostTopoPathEntry.getHostId());
        redisTemplate.opsForValue().set(
            hostIdKey,
            hostTopoPathEntry,
            iamHostTopoPathProperties.getCache().getExpireSeconds(),
            TimeUnit.SECONDS
        );
    }

    /**
     * 批量更新缓存中的主机拓扑路径实例
     *
     * @param hostTopoPathList 主机拓扑路径缓存实例列表
     */
    public void batchAddOrUpdateHostTopoPaths(List<HostTopoPathEntry> hostTopoPathList) {
        if (CollectionUtils.isEmpty(hostTopoPathList)) {
            return;
        }
        if (hostTopoPathList.size() == 1) {
            addOrUpdateHostTopoPath(hostTopoPathList.get(0));
            return;
        }

        long start = System.currentTimeMillis();
        redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public Object execute(@NotNull RedisOperations operations) throws DataAccessException {
                hostTopoPathList.forEach(host -> addOrUpdateHostTopoPath(host));
                return null;
            }
        });
        long cost = System.currentTimeMillis() - start;
        if (cost > 1000) {
            log.info(
                "BatchAddOrUpdateHostTopoPaths slow, hostTopoPathListSize: {}, cost: {}",
                hostTopoPathList.size(),
                cost
            );
        }
    }

    private String buildHostIdKey(long hostId) {
        return "job:execute:hostTopoPath:hostId:" + hostId;
    }
}
