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

package com.tencent.bk.job.consts;

import java.util.Collections;
import java.util.List;

/**
 * OP Web页面相关的路径常量。
 *
 * <p>这里是受Web登录态保护的路径的唯一事实来源：Controller的@GetMapping、拦截器注册的pathPatterns、
 * 以及拦截器内部判定“是否为接口请求”都从这里取值，避免同一组路径在多处各写一遍而发生漂移。</p>
 *
 * <p><b>注意</b>：存量的 {@code GET/POST /issueStatus} 接口被外部脚本裸调（不带任何凭证），
 * 必须保持免鉴权。因此这里的受保护路径只能是精确路径，
 * 绝不能使用 {@code /issueStatus/**} 这种通配写法——无论 PathPatternParser 还是 AntPathMatcher，
 * {@code /**} 都能匹配零个路径段，会把 {@code /issueStatus} 本身一起拦下。</p>
 */
public class WebPaths {

    /**
     * 存量的Issue状态读写接口，无鉴权，禁止纳入任何Web拦截器的拦截范围
     */
    public static final String ISSUE_STATUS = "/issueStatus";

    /**
     * Issue状态列表查询接口，供OP Web页面使用
     */
    public static final String ISSUE_STATUS_LIST = "/issueStatus/list";

    /**
     * 当前登录用户信息查询接口，供OP Web页面使用
     */
    public static final String WEB_USER_INFO = "/web/userInfo";

    /**
     * 前端SPA路由：Issue状态管理页
     */
    public static final String PAGE_ISSUE_STATUS = "/issue-status";

    /**
     * 前端SPA路由：无权限提示页
     */
    public static final String PAGE_NO_PERMISSION = "/no-permission";

    /**
     * 无权限提示页（静态HTML）
     */
    public static final String STATIC_403_PAGE = "/403.html";

    /**
     * 登录未配置/登录服务异常提示页（静态HTML）
     */
    public static final String STATIC_503_PAGE = "/503-login-unconfigured.html";

    /**
     * 会话接口路径。无论页面开关如何，这些路径始终受登录态+白名单保护
     */
    public static final List<String> SESSION_API_PATHS = Collections.unmodifiableList(
        List.of(ISSUE_STATUS_LIST, WEB_USER_INFO)
    );

    /**
     * 页面入口路径。仅当 job.op.web.enabled=true 时才存在并受保护
     */
    public static final List<String> PAGE_PATHS = Collections.unmodifiableList(
        List.of("/", "/index.html", PAGE_ISSUE_STATUS, PAGE_NO_PERMISSION)
    );

    /**
     * Web拦截器必须排除的路径。其中 {@link #ISSUE_STATUS} 是对存量裸调接口的二次兜底，
     * {@code /actuator/**} 被误拦会导致K8s探针失败、Pod反复重启
     */
    public static final List<String> EXCLUDE_PATHS = Collections.unmodifiableList(
        List.of(
            ISSUE_STATUS,
            "/api/**",
            "/mcp",
            "/mcp/**",
            "/actuator/**",
            "/checkServiceDependency",
            "/releaseStatus",
            STATIC_403_PAGE,
            STATIC_503_PAGE,
            "/static/**",
            "/js/**",
            "/css/**",
            "/img/**",
            "/fonts/**",
            "/favicon.ico"
        )
    );

    private WebPaths() {
    }
}
