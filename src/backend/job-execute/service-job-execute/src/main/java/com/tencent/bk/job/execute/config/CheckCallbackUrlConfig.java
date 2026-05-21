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

package com.tencent.bk.job.execute.config;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 回调地址（callback_url）白名单校验配置
 * <p>
 * 用于 ESB v2/v3 执行类接口提交时校验 callback_url 是否合法，防止 SSRF。
 * 校验顺序：配置 baseUrl 前缀 → 当前部署环境域名（含子域） → DB baseUrl 前缀。
 */
@Getter
@Setter
@ToString
@ConfigurationProperties(prefix = "job.execute.check-callback-url")
@Component
public class CheckCallbackUrlConfig {

    /**
     * 是否启用 callback url 白名单校验。默认 true。
     * <p>
     * 关闭后，仅校验 URL 基本合法性（scheme/host 非空等），不做白名单匹配。
     */
    private boolean enabled = true;

    /**
     * 通过配置文件预置的允许 baseUrl 前缀列表。
     * <p>
     * 匹配规则：用户提交的 callback_url 以列表中任一项作为前缀即视为通过。
     * 元素必须以 http:// 或 https:// 开头。
     */
    private List<String> allowedBaseUrls = Collections.emptyList();

    /**
     * DB 白名单缓存有效期（秒）。默认 60 秒。
     * <p>
     * OP 接口对白名单做增删时会主动失效缓存；该 TTL 用于兜底，防止极端场景下缓存常驻。
     */
    private int dbCacheTtlSeconds = 60;
}
