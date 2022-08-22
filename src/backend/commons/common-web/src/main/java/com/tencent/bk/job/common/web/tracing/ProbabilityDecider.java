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

package com.tencent.bk.job.common.web.tracing;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.http.HttpRequest;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ProbabilityDecider implements Decider {

    private final double probability;

    public ProbabilityDecider(double probability) {
        this.probability = probability;
    }

    private final LoadingCache<HttpRequest, Double> probabilityCache = CacheBuilder.newBuilder()
        .maximumSize(300).expireAfterWrite(60, TimeUnit.SECONDS).
            build(new CacheLoader<HttpRequest, Double>() {
                      @Override
                      public Double load(HttpRequest request) {
                          return Math.random();
                      }
                  }
            );

    private double getDecideValue(HttpRequest request) {
        try {
            return probabilityCache.get(request);
        } catch (ExecutionException e) {
            log.error("Fail to getDecideValue from cache", e);
            return 0;
        }
    }

    @Override
    public Boolean decide(HttpRequest request) {
        if (getDecideValue(request) >= probability) {
            log.debug("Ignore sample {} by probability {}", request.path(), probability);
            return false;
        }
        return null;
    }
}
