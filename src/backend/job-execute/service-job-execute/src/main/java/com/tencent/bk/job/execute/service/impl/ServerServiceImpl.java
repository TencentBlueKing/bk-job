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

package com.tencent.bk.job.execute.service.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.bk.job.common.cc.model.CcCloudAreaInfoDTO;
import com.tencent.bk.job.common.cc.model.CcCloudIdDTO;
import com.tencent.bk.job.common.cc.model.CcGroupHostPropDTO;
import com.tencent.bk.job.common.cc.model.CcInstanceDTO;
import com.tencent.bk.job.common.cc.sdk.CmdbClientFactory;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.common.exception.ObtainHostServiceException;
import com.tencent.bk.job.execute.service.ApplicationService;
import com.tencent.bk.job.execute.service.ServerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@DependsOn({"cmdbConfigSetter"})
@Slf4j
@Service
public class ServerServiceImpl implements ServerService {
    private final ApplicationService applicationService;
    private LoadingCache<Long, String> cloudAreaNameCache = CacheBuilder.newBuilder()
        .maximumSize(10000).expireAfterWrite(1, TimeUnit.HOURS).
            build(new CacheLoader<Long, String>() {
                      @Override
                      public String load(Long cloudAreaId) throws Exception {
                          IBizCmdbClient bizCmdbClient = CmdbClientFactory.getCcClient();
                          List<CcCloudAreaInfoDTO> cloudAreaList = bizCmdbClient.getCloudAreaList();
                          if (cloudAreaList == null || cloudAreaList.isEmpty()) {
                              log.warn("Get all cloud area return empty!");
                              return "Unknown";
                          }
                          log.info("Get all cloud area, result={}", JsonUtils.toJson(cloudAreaList));
                          for (CcCloudAreaInfoDTO cloudArea : cloudAreaList) {
                              if (cloudArea.getId().equals(cloudAreaId)) {
                                  return cloudArea.getName();
                              }
                          }
                          log.info("No found cloud area for cloudAreaId:{}", cloudAreaId);
                          return "Unknown";
                      }
                  }
            );

    @Autowired
    public ServerServiceImpl(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @Override
    public List<IpDTO> getIpByDynamicGroupId(long appId, String groupId) throws ObtainHostServiceException {
        IBizCmdbClient bizCmdbClient = CmdbClientFactory.getCcClient();
        try {
            ApplicationDTO appInfo = applicationService.getAppById(appId);
            List<CcGroupHostPropDTO> ccgroupHostList = bizCmdbClient.getCustomGroupIp(appId,
                appInfo.getBkSupplierAccount(), "admin", groupId);
            List<IpDTO> ips = new ArrayList<>();
            if (ccgroupHostList == null || ccgroupHostList.isEmpty()) {
                return ips;
            }
            for (CcGroupHostPropDTO hostProp : ccgroupHostList) {
                List<CcCloudIdDTO> hostCloudIdList = hostProp.getCloudIdList();
                if (hostCloudIdList == null || hostCloudIdList.isEmpty()) {
                    log.warn("Get ip by dynamic group id, cmdb return illegal host, skip it!appId={}, groupId={}, " +
                        "hostIp={}", appId, groupId, hostProp.getIp());
                    continue;
                }
                CcCloudIdDTO hostCloudId = hostCloudIdList.get(0);
                if (hostCloudId == null) {
                    log.warn("Get ip by dynamic group id, cmdb return illegal host, skip it!appId={}, groupId={}, " +
                        "hostIp={}", appId, groupId, hostProp.getIp());
                    continue;
                }
                IpDTO ip = new IpDTO(hostCloudId.getInstanceId(), hostProp.getIp());
                ips.add(ip);
            }
            log.info("Get hosts by groupId, appId={}, groupId={}, hosts={}", appId, groupId, ips);
            return ips;
        } catch (Exception e) {
            throw new ObtainHostServiceException();
        }
    }

    @Override
    public List<IpDTO> getIpByTopoNodes(long appId, List<CcInstanceDTO> ccInsts) {
        IBizCmdbClient bizCmdbClient = CmdbClientFactory.getCcClient();
        List<ApplicationHostDTO> apphostInfos = bizCmdbClient.getHosts(appId, ccInsts);
        List<IpDTO> ips = new ArrayList<>();
        if (apphostInfos == null || apphostInfos.isEmpty()) {
            return ips;
        }
        for (ApplicationHostDTO hostProp : apphostInfos) {
            IpDTO ip = new IpDTO(hostProp.getCloudAreaId(), hostProp.getIp());
            ips.add(ip);
        }
        log.info("Get hosts by cc topo nodes, appId={}, nodes={}, hosts={}", appId, ccInsts, ips);
        return ips;
    }

    @Override
    public String getCloudAreaName(long appId, long cloudAreaId) {
        try {
            return cloudAreaNameCache.get(cloudAreaId);
        } catch (Exception e) {
            log.warn("Fail to get cloud area name", e);
            return "Unknown";
        }
    }
}
