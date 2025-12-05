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

package com.tencent.bk.job.gateway.web.server;

import com.tencent.bk.job.gateway.web.server.provider.AccessLogMetadataProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.netty.http.server.logging.AccessLogArgProvider;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Access Log元数据收集器，汇总所有已注册的AccessLogMetadataProvider提供的元数据
 */
@Slf4j
public class AccessLogMetadataCollector {

    private final List<AccessLogMetadataProvider> providers;

    @Autowired
    public AccessLogMetadataCollector(List<AccessLogMetadataProvider> providers) {
        this.providers = providers;
    }

    public Map<String, Object> collect(AccessLogArgProvider accessLogArgProvider) {
        Map<String, Object> result = new LinkedHashMap<>();

        for (AccessLogMetadataProvider provider : providers) {
            try {
                result.putAll(provider.extract(accessLogArgProvider));
            } catch (Exception e) {
                log.warn("AccessLog provider {} collect failed: {}",
                    provider.getClass().getSimpleName(), e.getMessage());
            }
        }
        return result;
    }
}
