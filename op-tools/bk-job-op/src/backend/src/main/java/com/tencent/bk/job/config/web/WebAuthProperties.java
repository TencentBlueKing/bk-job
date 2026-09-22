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

package com.tencent.bk.job.config.web;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * OP Web页面的登录态与白名单配置。
 *
 * <p>登录协议与作业平台主项目的自定义登录（{@code paas.login.custom}）一致：
 * {@code GET {api-url}/user/get_info/?app_code={app-code}&{token-name}={ticket}}，
 * 响应 {@code {"ret":0,"data":{"username":"xxx"}}}。协议是确定的，因此只保留部署时确实会变的配置项。</p>
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "job.op.web")
public class WebAuthProperties {

    /**
     * 页面总开关。
     *
     * <p>为false时只关闭前端SPA路由的服务端回落（用户无法通过 /issue-status 进入页面），
     * {@code /issueStatus/list} 与 {@code /web/userInfo} 仍可访问且鉴权强度不降级。
     * 这个开关不是安全开关，安全边界由接口鉴权保证。</p>
     */
    private boolean enabled = true;

    private Auth auth = new Auth();

    @Data
    public static class Auth {

        /**
         * 逗号分隔的用户白名单。留空表示任何人都不允许访问，绝不是放行所有人
         */
        private String whitelist = "";

        /**
         * 是否允许白名单中的 "*" 通配所有已登录用户。仅供本地调试，生产必须为false
         */
        private boolean allowWildcard = false;

        private Login login = new Login();

        /**
         * 归一化后的白名单，小写去空格，避免每次请求都切分字符串
         */
        private Set<String> normalizedWhitelist = Collections.emptySet();
    }

    @Data
    public static class Login {

        /**
         * 承载登录态的Cookie名称
         */
        private String tokenName = "bk_ticket";

        /**
         * 统一登录页地址，未登录时302跳转的目标。为空时页面请求返回503提示页，
         * 而不是重定向到空地址造成无限重定向
         */
        private String loginUrl = "";

        /**
         * 回跳参数名，登录成功后由登录页带用户跳回
         */
        private String redirectParamName = "c_url";

        /**
         * 统一登录服务的API根地址，用于根据ticket查询用户信息。为空时所有Web访问都被视为未登录
         */
        private String apiUrl = "";

        /**
         * 调用登录服务时声明的应用标识
         */
        private String appCode = "job";

        private int connectTimeoutMs = 2000;

        private int readTimeoutMs = 3000;

        private Cache cache = new Cache();
    }

    @Data
    public static class Cache {

        /**
         * 有效ticket的缓存秒数
         */
        private int validSeconds = 300;

        /**
         * 无效ticket的负缓存秒数
         */
        private int invalidSeconds = 30;
    }

    @PostConstruct
    public void init() {
        Login login = auth.getLogin();
        auth.setNormalizedWhitelist(parseWhitelist(auth.getWhitelist()));

        log.info("OP web config: enabled={}, whitelistSize={}, apiUrl={}, loginUrl={}, appCode={}, tokenName={}",
            enabled,
            auth.getNormalizedWhitelist().size(),
            StringUtils.isBlank(login.getApiUrl()) ? "<NOT CONFIGURED>" : login.getApiUrl(),
            StringUtils.isBlank(login.getLoginUrl()) ? "<NOT CONFIGURED>" : login.getLoginUrl(),
            login.getAppCode(),
            login.getTokenName());
        if (StringUtils.isBlank(login.getApiUrl())) {
            log.warn("job.op.web.auth.login.api-url is not configured, "
                + "all web page/API access will be rejected as unauthenticated");
        }
        if (auth.getNormalizedWhitelist().isEmpty()) {
            log.warn("job.op.web.auth.whitelist is empty, all users will be rejected with 403 even after login");
        }
    }

    /**
     * 判断用户是否在白名单内。白名单为空时一律拒绝，避免漏配白名单的环境把页面完全暴露
     */
    public boolean isUserAllowed(String username) {
        if (StringUtils.isBlank(username)) {
            return false;
        }
        Set<String> whitelist = auth.getNormalizedWhitelist();
        if (whitelist.isEmpty()) {
            return false;
        }
        if (auth.isAllowWildcard() && whitelist.contains("*")) {
            return true;
        }
        return whitelist.contains(username.trim().toLowerCase());
    }

    private Set<String> parseWhitelist(String value) {
        if (StringUtils.isBlank(value)) {
            return Collections.emptySet();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String item : StringUtils.split(value, ',')) {
            String trimmed = item.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            result.add(trimmed.toLowerCase());
        }
        return Collections.unmodifiableSet(result);
    }
}
