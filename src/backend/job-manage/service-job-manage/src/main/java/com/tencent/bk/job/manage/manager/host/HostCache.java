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

import com.tencent.bk.job.common.constant.ResourceScopeTypeEnum;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.IpDTO;
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
    private final AppScopeMappingService appScopeMappingService;
    private final String HOST_KEY_PREFIX = "job:manage:host:";

    @Autowired
    public HostCache(@Qualifier("jsonRedisTemplate") RedisTemplate<Object, Object> redisTemplate,
                     AppScopeMappingService appScopeMappingService) {
        this.redisTemplate = redisTemplate;
        this.appScopeMappingService = appScopeMappingService;
    }

    /**
     * 批量查询缓存的主机
     *
     * @param hosts 主机列表
     * @return 缓存的主机列表, 按照传入的hosts排序。如果该主机不存在，List中对应索引的值为null
     */
    public List<CacheHostDO> batchGetHosts(List<IpDTO> hosts) {
        List<String> hostKeys = hosts.stream().map(this::buildHostKey).collect(Collectors.toList());
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
        String hostKey = buildHostKey(applicationHostDTO);
        redisTemplate.delete(hostKey);
    }

    /**
     * 更新缓存中的主机
     *
     * @param applicationHostDTO 主机
     */
    public void addOrUpdateHost(ApplicationHostDTO applicationHostDTO) {
        String hostKey = buildHostKey(applicationHostDTO);
        CacheHostDO cacheHost = new CacheHostDO();
        cacheHost.setBizId(applicationHostDTO.getBizId());
        if (applicationHostDTO.getAppId() == null) {
            cacheHost.setAppId(appScopeMappingService.getAppIdByScope(ResourceScopeTypeEnum.BIZ_SET.getValue(),
                String.valueOf(applicationHostDTO.getAppId())));
        } else {
            cacheHost.setAppId(applicationHostDTO.getAppId());
        }
        cacheHost.setCloudAreaId(applicationHostDTO.getCloudAreaId());
        cacheHost.setIp(applicationHostDTO.getIp());
        cacheHost.setHostId(applicationHostDTO.getHostId());
        redisTemplate.opsForValue().set(hostKey, cacheHost, 1, TimeUnit.DAYS);
    }

    private String buildHostKey(ApplicationHostDTO applicationHostDTO) {
        return buildHostKey(applicationHostDTO.getCloudAreaId(),
            applicationHostDTO.getIp());
    }

    private String buildHostKey(IpDTO host) {
        String cloudIp = host.getCloudAreaId() + ":" + host.getIp();
        return HOST_KEY_PREFIX + cloudIp;
    }

    private String buildHostKey(Long cloudAreaId, String ip) {
        String cloudIp = cloudAreaId + ":" + ip;
        return HOST_KEY_PREFIX + cloudIp;
    }
}
