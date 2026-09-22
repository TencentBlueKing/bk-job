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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * OP Web页面的登录态与白名单配置。
 *
 * <p>ticket校验接口的协议细节（方法、传参位置、参数名、附加参数、用户名字段路径、失效判定）全部外置为配置项，
 * 代码中不硬编码任何一项，联调时只改配置不改代码。所有配置项都有占位默认值，
 * 缺失关键配置时服务仍能正常启动，只是Web访问一律被拒绝（见 {@link WebAuthInterceptor}）。</p>
 *
 * <p>列表/映射类配置统一用逗号分隔的字符串承载，而不是YAML的List/Map：这些项需要通过环境变量注入，
 * 而环境变量注入YAML列表要写成带下标的形式，可读性与可维护性都很差。</p>
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
         * 承载登录态的Cookie名称
         */
        private String ticketCookieName = "bk_ticket";

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

        private Page page = new Page();

        private Verify verify = new Verify();

        private Cache cache = new Cache();
    }

    @Data
    public static class Page {

        /**
         * 统一登录页地址。为空时页面请求返回503提示页，而不是重定向到空地址造成无限重定向
         */
        private String url = "";

        /**
         * 回跳参数名，登录成功后由登录页带用户跳回
         */
        private String redirectParamName = "c_url";

        /**
         * 是否在跳转登录页时附加回跳参数
         */
        private boolean appendRedirect = true;
    }

    @Data
    public static class Verify {

        /**
         * ticket校验接口完整URL。为空时所有Web访问都被视为未登录，且不会发起HTTP请求
         */
        private String url = "";

        /**
         * HTTP方法，GET或POST
         */
        private String method = "GET";

        /**
         * ticket在请求中的参数名
         */
        private String ticketParamName = "bk_ticket";

        /**
         * ticket的传递位置
         */
        private TicketParamIn ticketParamIn = TicketParamIn.QUERY;

        /**
         * 附加查询参数，逗号分隔的 k=v 列表，用于携带 bk_app_code 等
         */
        private String extraQueryParams = "";

        /**
         * 附加请求头，逗号分隔的 k=v 列表，用于携带网关鉴权头等
         */
        private String extraHeaders = "";

        /**
         * 用户名在响应JSON中的路径，点号分隔，如 data.bk_username
         */
        private String usernameJsonPath = "data.bk_username";

        /**
         * 被判定为“ticket无效”（而非“服务异常”）的HTTP状态码，逗号分隔
         */
        private String invalidHttpStatuses = "400,401,403";

        /**
         * 业务返回码字段路径，留空表示不做业务码校验
         */
        private String resultJsonPath = "";

        /**
         * 业务返回码的成功取值，逗号分隔，按字符串比较
         */
        private String resultSuccessValues = "0,true";

        private int connectTimeoutMs = 2000;

        private int readTimeoutMs = 3000;

        private Map<String, String> normalizedExtraQueryParams = Collections.emptyMap();

        private Map<String, String> normalizedExtraHeaders = Collections.emptyMap();

        private Set<Integer> normalizedInvalidHttpStatuses = Collections.emptySet();

        private Set<String> normalizedResultSuccessValues = Collections.emptySet();
    }

    /**
     * ticket在校验请求中的位置
     */
    public enum TicketParamIn {
        QUERY,
        HEADER,
        BODY_JSON
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
        Verify verify = auth.getLogin().getVerify();
        auth.setNormalizedWhitelist(parseSet(auth.getWhitelist(), true));
        verify.setNormalizedExtraQueryParams(parseMap(verify.getExtraQueryParams(), "extra-query-params"));
        verify.setNormalizedExtraHeaders(parseMap(verify.getExtraHeaders(), "extra-headers"));
        verify.setNormalizedInvalidHttpStatuses(parseIntSet(verify.getInvalidHttpStatuses(), "invalid-http-statuses"));
        verify.setNormalizedResultSuccessValues(parseSet(verify.getResultSuccessValues(), false));

        log.info("OP web config: enabled={}, whitelistSize={}, verifyUrl={}, pageUrl={}, extraQueryParamKeys={}, "
                + "extraHeaderKeys={}",
            enabled,
            auth.getNormalizedWhitelist().size(),
            StringUtils.isBlank(verify.getUrl()) ? "<NOT CONFIGURED>" : verify.getUrl(),
            StringUtils.isBlank(auth.getLogin().getPage().getUrl()) ? "<NOT CONFIGURED>"
                : auth.getLogin().getPage().getUrl(),
            // 附加参数可能包含密钥，只打印key不打印value
            verify.getNormalizedExtraQueryParams().keySet(),
            verify.getNormalizedExtraHeaders().keySet());
        if (StringUtils.isBlank(verify.getUrl())) {
            log.warn("job.op.web.auth.login.verify.url is not configured, "
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

    private Set<String> parseSet(String value, boolean toLowerCase) {
        if (StringUtils.isBlank(value)) {
            return Collections.emptySet();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String item : StringUtils.split(value, ',')) {
            String trimmed = item.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            result.add(toLowerCase ? trimmed.toLowerCase() : trimmed);
        }
        return Collections.unmodifiableSet(result);
    }

    /**
     * 切分 k=v 列表。格式错误时直接抛异常让服务启动失败：配置写错必须在启动时暴露，
     * 不能静默忽略等到用户登录才发现
     */
    private Map<String, String> parseMap(String value, String configName) {
        if (StringUtils.isBlank(value)) {
            return Collections.emptyMap();
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (String item : StringUtils.split(value, ',')) {
            String trimmed = item.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int index = trimmed.indexOf('=');
            if (index <= 0) {
                throw new IllegalStateException(
                    "Invalid job.op.web.auth.login.verify." + configName + " item, expect k=v format");
            }
            result.put(trimmed.substring(0, index).trim(), trimmed.substring(index + 1).trim());
        }
        return Collections.unmodifiableMap(result);
    }

    private Set<Integer> parseIntSet(String value, String configName) {
        if (StringUtils.isBlank(value)) {
            return Collections.emptySet();
        }
        Set<Integer> result = new LinkedHashSet<>();
        for (String item : StringUtils.split(value, ',')) {
            String trimmed = item.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                result.add(Integer.parseInt(trimmed));
            } catch (NumberFormatException e) {
                throw new IllegalStateException(
                    "Invalid job.op.web.auth.login.verify." + configName + " item: " + trimmed, e);
            }
        }
        return Collections.unmodifiableSet(result);
    }
}
