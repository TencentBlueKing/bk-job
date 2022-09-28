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

package com.tencent.bk.job.manage.service.host.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.bk.job.common.cc.sdk.CmdbClientFactory;
import com.tencent.bk.job.common.cc.sdk.IBizCmdbClient;
import com.tencent.bk.job.common.util.JobContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OsTypeService {

    private final LoadingCache<String, Map<String, String>> osTypeMapCache = CacheBuilder.newBuilder()
        .maximumSize(2).expireAfterWrite(30, TimeUnit.MINUTES).
            build(new CacheLoader<String, Map<String, String>>() {
                      @Override
                      public Map<String, String> load(String lang) {
                          IBizCmdbClient bizCmdbClient = CmdbClientFactory.getCmdbClient(lang);
                          return bizCmdbClient.getOsTypeIdNameMap();
                      }
                  }
            );

    private String getOsTypeNameById(String osTypeId) throws Exception {
        Map<String, String> osTypeIdNameMap = osTypeMapCache.get(JobContextUtil.getUserLang());
        return osTypeIdNameMap.get(osTypeId);
    }

    public String getOsTypeNameOrDefault(String osTypeId, String defaultValue) {
        if (osTypeId == null) {
            return defaultValue;
        }
        try {
            return getOsTypeNameById(osTypeId);
        } catch (Exception e) {
            log.warn("Fail to getOsTypeNameById", e);
            return defaultValue;
        }
    }

}
