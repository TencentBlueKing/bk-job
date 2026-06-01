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

package com.tencent.bk.job.execute.service.validation;

/**
 * 回调地址（callback_url）合法性与白名单校验 Service。
 * <p>
 * 用于 ESB v2/v3 执行类接口提交时拦截非法的 callback_url，防止 SSRF。
 */
public interface CallbackUrlValidateService {

    /**
     * 判定一个 callback_url 是否合法且在白名单中。
     * <p>
     * 校验顺序：
     * <ol>
     *   <li>URL 基本合法性（scheme 必须为 http/https，host 非空）；</li>
     *   <li>若开关 {@code job.execute.check-callback-url.enabled=false}，仅做合法性校验，跳过白名单匹配；</li>
     *   <li>命中配置 {@code allowedBaseUrls} 中任一 baseUrl 前缀；</li>
     *   <li>命中当前部署环境域名 {@code bk.bkDomain} 或其子域名；</li>
     *   <li>命中 DB 白名单 {@code callback_url_white_info.base_url} 中任一前缀（带缓存）。</li>
     * </ol>
     *
     * @param callbackUrl 待校验地址，{@code null} 或空串直接返回 {@code true}（由外层 @NotBlank 等控制）
     * @return 是否通过校验
     */
    boolean isValid(String callbackUrl);

    /**
     * 校验白名单 baseUrl 入参格式（仅供 OP 接口新增白名单时使用）：必须以 http:// 或 https:// 开头。
     * 不合法抛 {@link com.tencent.bk.job.common.exception.InvalidParamException}。
     *
     * @param baseUrl 待校验 baseUrl
     */
    void validateWhitelistBaseUrl(String baseUrl);

    /**
     * 失效 DB 白名单缓存（OP 接口对白名单做增删后调用，保证立即生效）。
     */
    void invalidateCache();
}
