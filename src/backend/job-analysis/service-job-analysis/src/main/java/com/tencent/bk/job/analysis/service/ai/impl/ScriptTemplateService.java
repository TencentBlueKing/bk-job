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

package com.tencent.bk.job.analysis.service.ai.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.api.inner.ServiceScriptTemplateResource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ScriptTemplateService {

    private final ServiceScriptTemplateResource scriptTemplateResource;
    private final LoadingCache<Integer, String> scriptTemplateCache = CacheBuilder.newBuilder()
        .maximumSize(6).expireAfterWrite(1, TimeUnit.DAYS).
        build(new CacheLoader<Integer, String>() {
                  @SuppressWarnings("all")
                  @Override
                  public String load(Integer scriptType) {
                      return getScriptTemplateIndeed(scriptType);
                  }
              }
        );

    @Autowired
    public ScriptTemplateService(ServiceScriptTemplateResource scriptTemplateResource) {
        this.scriptTemplateResource = scriptTemplateResource;
    }

    public String getScriptTemplate(Integer scriptType) {
        try {
            return scriptTemplateCache.get(scriptType);
        } catch (ExecutionException e) {
            log.warn("Fail to getScriptTemplate by type:{}", scriptType);
            return getScriptTemplateIndeed(scriptType);
        }
    }

    public String getScriptTemplateIndeed(Integer scriptType) {
        InternalResponse<String> resp = scriptTemplateResource.getScriptTemplate(scriptType);
        if (log.isDebugEnabled()) {
            log.debug("resp={}", JsonUtils.toJson(resp));
        }
        return resp.getData();
    }
}
