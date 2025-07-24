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

package com.tencent.bk.job.manage.manager.host;

import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.redis.BaseRedisCache;
import com.tencent.bk.job.manage.model.db.CacheHostDO;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 主机缓存
 */
@Slf4j
@Component
public class HostCache extends BaseRedisCache {

    private final RedisTemplate<String, Object> redisTemplate;

    // 1天过期
    private static final int EXPIRE_DAYS = 1;

    @Autowired
    public HostCache(@Qualifier("jsonRedisTemplate") RedisTemplate<String, Object> redisTemplate,
                     MeterRegistry meterRegistry) {
        super(meterRegistry, "HostCache");
        this.redisTemplate = redisTemplate;
    }

    /**
     * 批量根据ip查询缓存的主机
     *
     * @param cloudIps 主机ip(云区域+IP)列表
     * @return 缓存的主机列表, 按照传入的hosts排序。如果该主机不存在，List中对应索引的值为null
     */
    public List<CacheHostDO> batchGetHostsByIps(List<String> cloudIps) {
        List<String> hostKeys = cloudIps.stream().map(this::buildHostIpKey).collect(Collectors.toList());
        return getHostsByKeys(hostKeys);
    }

    /**
     * 批量根据hostId查询缓存的主机
     *
     * @param hostIds 主机ID列表
     * @return 缓存的主机列表, 按照传入的hosts排序。如果该主机不存在，List中对应索引的值为null
     */
    public List<CacheHostDO> batchGetHostsByHostIds(List<Long> hostIds) {
        List<String> hostKeys = hostIds.stream().map(this::buildHostIdKey).collect(Collectors.toList());
        return getHostsByKeys(hostKeys);
    }

    private List<CacheHostDO> getHostsByKeys(List<String> hostKeys) {
        long hitCount = 0;
        long missCount = 0;
        try {
            List<Object> results = redisTemplate.opsForValue().multiGet(hostKeys);
            // 通过 Object 间接强制转换 List
            List<CacheHostDO> foundHosts = results == null ?
                Collections.emptyList() : (List<CacheHostDO>) (Object) results;

            // multiGet 获取到的 list 中的元素可能为 null （如果key 不存在)
            hitCount = foundHosts.stream().filter(Objects::nonNull).count();
            missCount = hostKeys.size() - hitCount;

            return foundHosts;
        } catch (Exception e) {
            log.warn("Batch get host in cache exception", e);
            hitCount = 0;
            missCount = hostKeys.size();
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
     * 删除缓存中的主机
     *
     * @param applicationHostDTO 主机
     */
    public void deleteHost(ApplicationHostDTO applicationHostDTO) {
        String hostIpKey = buildHostIpKey(applicationHostDTO);
        String hostIdKey = buildHostIdKey(applicationHostDTO);
        redisTemplate.delete(hostIpKey);
        redisTemplate.delete(hostIdKey);
    }

    /**
     * 批量删除缓存中的主机
     *
     * @param hosts 主机集合
     */
    public void batchDeleteHost(Collection<ApplicationHostDTO> hosts) {
        if (CollectionUtils.isEmpty(hosts)) {
            return;
        }
        Set<String> hostIpKeys = new HashSet<>();
        Set<String> hostIdKeys = new HashSet<>();
        for (ApplicationHostDTO host : hosts) {
            hostIpKeys.add(buildHostIpKey(host));
            hostIdKeys.add(buildHostIdKey(host));
        }
        redisTemplate.delete(hostIpKeys);
        redisTemplate.delete(hostIdKeys);
    }

    /**
     * 更新缓存中的主机
     *
     * @param applicationHostDTO 主机
     */
    public void addOrUpdateHost(ApplicationHostDTO applicationHostDTO) {
        CacheHostDO cacheHost = CacheHostDO.fromApplicationHostDTO(applicationHostDTO);
        cacheHost.setCacheTime(System.currentTimeMillis());
        log.info("Update host cache, hostId: {}, host: {}", cacheHost.getHostId(), cacheHost);
        String hostIpKey = buildHostIpKey(applicationHostDTO);
        String hostIdKey = buildHostIdKey(applicationHostDTO);
        redisTemplate.opsForValue().set(hostIpKey, cacheHost, EXPIRE_DAYS, TimeUnit.DAYS);
        redisTemplate.opsForValue().set(hostIdKey, cacheHost, EXPIRE_DAYS, TimeUnit.DAYS);
    }

    /**
     * 批量更新缓存中的主机
     *
     * @param hosts 主机列表
     */
    public void batchAddOrUpdateHosts(List<ApplicationHostDTO> hosts) {
        if (CollectionUtils.isEmpty(hosts)) {
            return;
        }
        if (hosts.size() == 1) {
            addOrUpdateHost(hosts.get(0));
            return;
        }

        long start = System.currentTimeMillis();
        redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public Object execute(@NotNull RedisOperations operations) throws DataAccessException {
                hosts.forEach(host -> addOrUpdateHost(host));
                return null;
            }
        });
        long cost = System.currentTimeMillis() - start;
        if (cost > 1000) {
            log.info("BatchAddOrUpdateHosts slow, hostSize: {}, cost: {}", hosts.size(), cost);
        }
    }


    private String buildHostIpKey(ApplicationHostDTO applicationHostDTO) {
        return buildHostIpKey(applicationHostDTO.getCloudAreaId(),
            applicationHostDTO.getIp());
    }

    private String buildHostIdKey(ApplicationHostDTO applicationHostDTO) {
        return buildHostIdKey(applicationHostDTO.getHostId());
    }

    private String buildHostIpKey(String cloudIp) {
        return "job:manage:host:" + cloudIp;
    }

    private String buildHostIdKey(long hostId) {
        return "job:manage:host:hostId:" + hostId;
    }

    private String buildHostIpKey(Long bkCloudId, String ip) {
        return buildHostIpKey(bkCloudId + ":" + ip);
    }
}
