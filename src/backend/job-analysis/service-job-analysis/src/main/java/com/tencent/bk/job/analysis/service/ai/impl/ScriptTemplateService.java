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

/**
 * 脚本模板服务
 */
@Slf4j
@Service
public class ScriptTemplateService {

    private final ServiceScriptTemplateResource scriptTemplateResource;
    /**
     * 最大脚本模板数量
     */
    private final int maxScriptTemplateNum = 6;
    /**
     * 脚本模板缓存过期时间（天）
     */
    private final int scriptTemplateCacheExpireDays = 1;

    /**
     * 各种语言类型的脚本模板数量有限且很少变动，使用本地缓存提高加载速率，也避免频繁的服务调用
     */
    private final LoadingCache<Integer, String> scriptTemplateCache = CacheBuilder.newBuilder()
        .maximumSize(maxScriptTemplateNum).expireAfterWrite(scriptTemplateCacheExpireDays, TimeUnit.DAYS)
        .build(new CacheLoader<Integer, String>() {
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

    /**
     * 根据脚本类型获取脚本模板，优先从缓存获取，如果缓存未命中则通过服务调用获取
     *
     * @param scriptType 脚本类型
     * @return 脚本模板
     */
    public String getScriptTemplate(Integer scriptType) {
        try {
            return scriptTemplateCache.get(scriptType);
        } catch (ExecutionException e) {
            log.warn("Fail to getScriptTemplate by type:{}", scriptType);
            return getScriptTemplateIndeed(scriptType);
        }
    }

    /**
     * 通过服务调用获取脚本模板
     *
     * @param scriptType 脚本类型
     * @return 脚本模板
     */
    public String getScriptTemplateIndeed(Integer scriptType) {
        InternalResponse<String> resp = scriptTemplateResource.getScriptTemplate(scriptType);
        if (log.isDebugEnabled()) {
            log.debug("resp={}", JsonUtils.toJson(resp));
        }
        return resp.getData();
    }
}
