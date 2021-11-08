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

package com.tencent.bk.job.analysis.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.bk.job.analysis.client.ApplicationResourceClient;
import com.tencent.bk.job.analysis.model.dto.SimpleAppInfoDTO;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.manage.model.inner.ServiceApplicationDTO;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AppService {

    private static final String KEY_CACHE_APP_INFO_LIST = "KEY_CACHE_APP_INFO_LIST";
    protected final ApplicationResourceClient applicationResourceClient;
    private LoadingCache<Long, String> appIdNameCache = CacheBuilder.newBuilder()
        .maximumSize(3000).expireAfterWrite(30, TimeUnit.SECONDS).
            build(new CacheLoader<Long, String>() {
                      @Override
                      public String load(Long appId) throws Exception {
                          return getAppNameById(appId);
                      }
                  }
            );

    private LoadingCache<String, List<ServiceApplicationDTO>> appInfoCache = CacheBuilder.newBuilder()
        .maximumSize(3000).expireAfterWrite(30, TimeUnit.SECONDS).
            build(new CacheLoader<String, List<ServiceApplicationDTO>>() {
                      @Override
                      public List<ServiceApplicationDTO> load(String key) throws Exception {
                          if (KEY_CACHE_APP_INFO_LIST.equals(key)) {
                              return listLocalDBApps();
                          } else {
                              return null;
                          }
                      }
                  }
            );

    @Autowired
    public AppService(ApplicationResourceClient applicationResourceClient) {
        this.applicationResourceClient = applicationResourceClient;
    }

    public String getAppNameFromCache(Long appId) {
        try {
            return appIdNameCache.get(appId);
        } catch (ExecutionException e) {
            log.warn("Fail to get app from cache", e);
            return null;
        }
    }

    public String getAppNameById(Long appId) {
        ServiceApplicationDTO serviceApplicationDTO = applicationResourceClient.queryAppById(appId);
        if (serviceApplicationDTO == null) {
            String msg = String.format("Cannot find appName by id=%s, app may be deleted", appId);
            log.warn(msg);
            throw new RuntimeException(msg);
        }
        return serviceApplicationDTO.getName();
    }

    public List<SimpleAppInfoDTO> getSimpleAppInfoByIds(List<Long> appIdList) {
        if (appIdList == null) return null;
        if (appIdList.isEmpty()) return Collections.emptyList();
        List<SimpleAppInfoDTO> resultList = new ArrayList<>();
        List<ServiceApplicationDTO> serviceApplicationDTOList = listLocalDBAppsFromCache();
        if (serviceApplicationDTOList == null) {
            log.error("Fail to listLocalDBAppsFromCache, serviceApplicationDTOList is null");
            throw new InternalException(ErrorCode.INTERNAL_ERROR);
        }
        Map<Long, SimpleAppInfoDTO> map = new HashMap<>();
        for (ServiceApplicationDTO serviceApplicationDTO : serviceApplicationDTOList) {
            map.putIfAbsent(serviceApplicationDTO.getId(), new SimpleAppInfoDTO(serviceApplicationDTO.getId(),
                serviceApplicationDTO.getName()));
        }
        for (Long appId : appIdList) {
            SimpleAppInfoDTO appInfoDTO = map.get(appId);
            if (appInfoDTO != null) {
                resultList.add(appInfoDTO);
            } else {
                // 业务已被删除，但Job仍然保留了其历史执行记录，忽略
                log.warn("appId {} was deleted, ignore", appId);
            }
        }
        return resultList;
    }

    public synchronized List<ServiceApplicationDTO> listLocalDBAppsFromCache() {
        try {
            return appInfoCache.get(KEY_CACHE_APP_INFO_LIST);
        } catch (ExecutionException e) {
            log.warn("Fail to get appInfoList from cache", e);
            return null;
        }
    }

    public List<ServiceApplicationDTO> listLocalDBApps() {
        val resp = applicationResourceClient.listLocalDBApps(-1);
        List<ServiceApplicationDTO> apps = resp.getData();
        return apps;
    }
}
