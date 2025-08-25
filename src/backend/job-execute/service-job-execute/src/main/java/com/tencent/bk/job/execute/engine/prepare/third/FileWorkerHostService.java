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

package com.tencent.bk.job.execute.engine.prepare.third;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.execute.service.HostService;
import com.tencent.bk.job.manage.model.inner.ServiceHostDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 第三方源文件File-Worker的主机解析服务
 */
@Slf4j
@Primary
@Component
public class FileWorkerHostService {

    private final HostService hostService;
    private final TenantEnvService tenantEnvService;

    @Autowired
    public FileWorkerHostService(HostService hostService, TenantEnvService tenantEnvService) {
        this.hostService = hostService;
        this.tenantEnvService = tenantEnvService;
    }

    // 查询file-worker对应主机信息使用60s缓存，避免短时间内多次重复查询
    private final LoadingCache<Triple<Long, String, String>, HostDTO> fileWorkerHostCache = CacheBuilder.newBuilder()
        .maximumSize(1).expireAfterWrite(60, TimeUnit.SECONDS).
        build(new CacheLoader<Triple<Long, String, String>, HostDTO>() {
                  @SuppressWarnings("all")
                  @Override
                  public HostDTO load(Triple<Long, String, String> triple) {
                      return parseFileWorkerHost(triple.getLeft(), triple.getMiddle(), triple.getRight());
                  }
              }
        );

    public HostDTO parseFileWorkerHostWithCache(Long cloudAreaId, String ipProtocol, String ip) {
        try {
            return fileWorkerHostCache.get(buildCacheKey(cloudAreaId, ipProtocol, ip));
        } catch (ExecutionException e) {
            log.error("Fail to parseFileWorkerHostWithCache", e);
            return null;
        }
    }

    private Triple<Long, String, String> buildCacheKey(Long cloudAreaId, String ipProtocol, String ip) {
        return Triple.of(cloudAreaId, ipProtocol, ip);
    }

    private HostDTO parseFileWorkerHost(Long cloudAreaId, String ipProtocol, String ip) {
        if (StringUtils.isBlank(ipProtocol)) {
            ipProtocol = IpUtils.inferProtocolByIp(ip);
            log.info("ipProtocol is null or blank, use {} inferred by ip {}", ipProtocol, ip);
        }
        HostDTO hostDTO;
        if (IpUtils.PROTOCOL_IP_V6.equalsIgnoreCase(ipProtocol)) {
            String fileWorkerTenantId = tenantEnvService.getJobMachineTenantId();
            ServiceHostDTO fileWorkerHost = hostService.getHostByCloudIpv6(
                fileWorkerTenantId,
                cloudAreaId,
                ip
            );
            hostDTO = ServiceHostDTO.toHostDTO(fileWorkerHost);
        } else {
            hostDTO = ServiceHostDTO.toHostDTO(
                hostService.getHostFromCacheOrDB(
                    tenantEnvService.getJobMachineTenantId(),
                    new HostDTO(cloudAreaId, ip)
                )
            );
        }
        if (log.isDebugEnabled()) {
            log.debug("host get by ({},{},{}) is {}", ipProtocol, cloudAreaId, ip, hostDTO);
        }
        return hostDTO;
    }
}
