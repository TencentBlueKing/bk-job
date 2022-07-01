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

package com.tencent.bk.job.manage.manager.host;

import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.manage.model.db.CacheHostDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 主机缓存
 */
@Slf4j
@Component
public class HostCache {

    private final RedisTemplate redisTemplate;

    @Autowired
    public HostCache(@Qualifier("jsonRedisTemplate") RedisTemplate<Object, Object> redisTemplate,
                     AppScopeMappingService appScopeMappingService) {
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
        try {
            return (List<CacheHostDO>) redisTemplate.opsForValue().multiGet(hostKeys);
        } catch (Exception e) {
            log.warn("Batch get host in cache exception", e);
            return Collections.emptyList();
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
     * 更新缓存中的主机
     *
     * @param applicationHostDTO 主机
     */
    public void addOrUpdateHost(ApplicationHostDTO applicationHostDTO) {
        CacheHostDO cacheHost = CacheHostDO.fromApplicationHostDTO(applicationHostDTO);
        String hostIpKey = buildHostIpKey(applicationHostDTO);
        String hostIdKey = buildHostIdKey(applicationHostDTO);
        redisTemplate.opsForValue().set(hostIpKey, cacheHost, 1, TimeUnit.DAYS);
        redisTemplate.opsForValue().set(hostIdKey, cacheHost, 1, TimeUnit.DAYS);
    }

    /**
     * 批量更新缓存中的主机
     *
     * @param hosts 主机列表
     */
    public void addOrUpdateHosts(List<ApplicationHostDTO> hosts) {
        hosts.forEach(this::addOrUpdateHost);
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
