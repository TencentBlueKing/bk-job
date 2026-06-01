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

package com.tencent.bk.job.execute.service.validation.impl;

import com.tencent.bk.job.common.config.BkConfig;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.execute.config.CheckCallbackUrlConfig;
import com.tencent.bk.job.execute.dao.CallbackUrlWhiteInfoDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CallbackUrlValidateServiceImplTest {

    private CheckCallbackUrlConfig config;
    private BkConfig bkConfig;
    private CallbackUrlWhiteInfoDAO dao;
    private CallbackUrlValidateServiceImpl service;

    @BeforeEach
    void setUp() throws Exception {
        config = new CheckCallbackUrlConfig();
        config.setEnabled(true);
        config.setAllowedBaseUrls(Collections.emptyList());
        config.setDbCacheTtlSeconds(60);

        bkConfig = new BkConfig();
        bkConfig.setBkDomain("");

        dao = mock(CallbackUrlWhiteInfoDAO.class);
        when(dao.listAllBaseUrls()).thenReturn(Collections.emptyList());

        service = new CallbackUrlValidateServiceImpl(config, bkConfig, dao);
        // 触发 @PostConstruct init() 构建 Caffeine 缓存
        Method init = CallbackUrlValidateServiceImpl.class.getDeclaredMethod("init");
        init.setAccessible(true);
        init.invoke(service);
    }

    @Nested
    @DisplayName("isValid - 基本合法性校验")
    class BasicValidation {

        @Test
        @DisplayName("null 或空串放行（由外层 @NotBlank 控制）")
        void blankPasses() {
            assertThat(service.isValid(null)).isTrue();
            assertThat(service.isValid("")).isTrue();
            assertThat(service.isValid("   ")).isTrue();
        }

        @Test
        @DisplayName("非 http/https scheme 拒绝")
        void rejectNonHttpScheme() {
            assertThat(service.isValid("ftp://example.com/")).isFalse();
            assertThat(service.isValid("file:///etc/passwd")).isFalse();
            assertThat(service.isValid("gopher://example.com/")).isFalse();
        }

        @Test
        @DisplayName("URL 格式不合法拒绝")
        void rejectMalformed() {
            assertThat(service.isValid("not a url")).isFalse();
            assertThat(service.isValid("http://")).isFalse();
        }

        @Test
        @DisplayName("包含 userinfo 的 URL 全局拒绝（防 SSRF 绕过）")
        void rejectUserinfoInjection() {
            // 即使 enabled=false，userinfo 形态依然全局拦截
            config.setEnabled(false);
            assertThat(service.isValid("https://trusted.com@evil.com/cb")).isFalse();
            assertThat(service.isValid("https://user:pass@evil.com/cb")).isFalse();
            assertThat(service.isValid("http://api.example.com@evil.com/cb")).isFalse();
            // enabled=true 且看似命中白名单的 userinfo 形态依然拦截
            config.setEnabled(true);
            config.setAllowedBaseUrls(Collections.singletonList("https://trusted.com/"));
            assertThat(service.isValid("https://trusted.com@evil.com/cb")).isFalse();
        }
    }

    @Nested
    @DisplayName("isValid - enabled=false 时仅校验合法性")
    class EnabledFalse {

        @Test
        @DisplayName("关闭白名单后任意合法 http(s) URL 都放行")
        void anyValidHttpUrlPasses() {
            config.setEnabled(false);
            assertThat(service.isValid("http://anyone.evil.com/")).isTrue();
            assertThat(service.isValid("https://127.0.0.1:8080/cb")).isTrue();
        }

        @Test
        @DisplayName("关闭白名单后非法 scheme 仍然拒绝")
        void invalidStillRejected() {
            config.setEnabled(false);
            assertThat(service.isValid("ftp://x")).isFalse();
        }
    }

    @Nested
    @DisplayName("isValid - 配置白名单 baseUrl 前缀匹配")
    class ConfigWhitelist {

        @Test
        @DisplayName("命中前缀放行")
        void prefixMatch() {
            config.setAllowedBaseUrls(Arrays.asList(
                "http://callback.example.com/",
                "https://api.partner.com/job/"
            ));
            assertThat(service.isValid("http://callback.example.com/cb/123")).isTrue();
            assertThat(service.isValid("https://api.partner.com/job/notify")).isTrue();
        }

        @Test
        @DisplayName("未命中前缀拒绝")
        void prefixMiss() {
            config.setAllowedBaseUrls(Collections.singletonList("http://allowed.com/"));
            assertThat(service.isValid("http://other.com/cb")).isFalse();
        }

        @Test
        @DisplayName("后缀混淆攻击拒绝：host 必须严格等值")
        void rejectSuffixConfusion() {
            // baseUrl 不以 / 结尾时旧版 startsWith 会被绕过；新版按 URI 解析必须 host 等值
            config.setAllowedBaseUrls(Collections.singletonList("https://trusted.com"));
            assertThat(service.isValid("https://trusted.com.evil.com/cb")).isFalse();
            assertThat(service.isValid("https://trusted.com-evil.com/cb")).isFalse();
            // 同 host 下应当放行
            assertThat(service.isValid("https://trusted.com/cb")).isTrue();
            assertThat(service.isValid("https://trusted.com/cb?x=1")).isTrue();
        }

        @Test
        @DisplayName("path 必须以 / 作为边界匹配，避免 /foo 命中 /foobar")
        void pathBoundaryMatch() {
            config.setAllowedBaseUrls(Collections.singletonList("https://api.com/foo"));
            assertThat(service.isValid("https://api.com/foo")).isTrue();
            assertThat(service.isValid("https://api.com/foo/")).isTrue();
            assertThat(service.isValid("https://api.com/foo/bar")).isTrue();
            assertThat(service.isValid("https://api.com/foobar")).isFalse();
            assertThat(service.isValid("https://api.com/foobar/x")).isFalse();
        }

        @Test
        @DisplayName("port 必须按协议默认端口归一化后等值")
        void portEqualsWithDefaultPortNormalization() {
            config.setAllowedBaseUrls(Collections.singletonList("https://api.com/"));
            // 显式 443 与默认端口等同
            assertThat(service.isValid("https://api.com:443/cb")).isTrue();
            // 非默认端口不等同
            assertThat(service.isValid("https://api.com:8443/cb")).isFalse();

            config.setAllowedBaseUrls(Collections.singletonList("http://api.com:8080/"));
            assertThat(service.isValid("http://api.com:8080/cb")).isTrue();
            assertThat(service.isValid("http://api.com/cb")).isFalse();
            assertThat(service.isValid("http://api.com:80/cb")).isFalse();
        }

        @Test
        @DisplayName("scheme/host 大小写不敏感")
        void schemeAndHostCaseInsensitive() {
            config.setAllowedBaseUrls(Collections.singletonList("https://Api.Example.COM/"));
            assertThat(service.isValid("HTTPS://api.example.com/cb")).isTrue();
            assertThat(service.isValid("https://API.EXAMPLE.COM/cb")).isTrue();
        }
    }

    @Nested
    @DisplayName("isValid - 当前部署环境 bkDomain 子域匹配")
    class BkDomainMatch {

        @Test
        @DisplayName("host 等于 bkDomain 放行")
        void hostEqualsDomain() {
            bkConfig.setBkDomain("bktencent.com");
            assertThat(service.isValid("http://bktencent.com/cb")).isTrue();
        }

        @Test
        @DisplayName("host 为 bkDomain 的子域放行")
        void hostIsSubdomain() {
            bkConfig.setBkDomain("bktencent.com");
            assertThat(service.isValid("https://job.bktencent.com/cb")).isTrue();
            assertThat(service.isValid("http://api.deeper.bktencent.com/notify")).isTrue();
        }

        @Test
        @DisplayName("host 与 bkDomain 无关拒绝")
        void hostUnrelated() {
            bkConfig.setBkDomain("bktencent.com");
            assertThat(service.isValid("http://evil-bktencent.com/cb")).isFalse();
            assertThat(service.isValid("http://bktencent.com.evil.com/cb")).isFalse();
        }

        @Test
        @DisplayName("bkDomain 为空时跳过该轮匹配")
        void bkDomainBlank() {
            bkConfig.setBkDomain("");
            assertThat(service.isValid("http://any.bktencent.com/cb")).isFalse();
        }
    }

    @Nested
    @DisplayName("isValid - DB 白名单匹配（带缓存）")
    class DbWhitelist {

        @Test
        @DisplayName("DB 中存在的 baseUrl 前缀命中放行")
        void dbPrefixMatch() {
            when(dao.listAllBaseUrls())
                .thenReturn(Collections.singletonList("https://db-allowed.com/"));
            service.invalidateCache();
            assertThat(service.isValid("https://db-allowed.com/cb/1")).isTrue();
        }

        @Test
        @DisplayName("缓存命中时不重复访问 DAO")
        void cacheHitDoesNotCallDao() {
            when(dao.listAllBaseUrls())
                .thenReturn(Collections.singletonList("https://cached.com/"));
            service.invalidateCache();

            assertThat(service.isValid("https://cached.com/a")).isTrue();
            assertThat(service.isValid("https://cached.com/b")).isTrue();
            assertThat(service.isValid("https://cached.com/c")).isTrue();
            // invalidate 后第一次触发 load，再之后 2 次走缓存
            verify(dao, times(1)).listAllBaseUrls();
        }

        @Test
        @DisplayName("DAO 抛异常时降级为空白名单，不影响其他校验路径")
        void daoFailureFailsClosedForDbButNotConfig() {
            when(dao.listAllBaseUrls()).thenThrow(new RuntimeException("db down"));
            config.setAllowedBaseUrls(Collections.singletonList("http://allowed.com/"));
            service.invalidateCache();
            // 配置白名单仍生效
            assertThat(service.isValid("http://allowed.com/x")).isTrue();
            // 仅 DB 校验路径下被拒绝
            assertThat(service.isValid("http://only-in-db.com/x")).isFalse();
        }
    }

    @Nested
    @DisplayName("isValid - 综合路径")
    class Composite {

        @Test
        @DisplayName("都不命中时拒绝")
        void allMiss() {
            config.setAllowedBaseUrls(Collections.singletonList("http://a.com/"));
            bkConfig.setBkDomain("bktencent.com");
            when(dao.listAllBaseUrls()).thenReturn(Collections.singletonList("http://b.com/"));
            service.invalidateCache();
            assertThat(service.isValid("http://evil.com/cb")).isFalse();
        }

        @Test
        @DisplayName("配置白名单优先命中时不访问 DAO")
        void configMatchSkipsDao() {
            config.setAllowedBaseUrls(Collections.singletonList("http://shortcut.com/"));
            assertThat(service.isValid("http://shortcut.com/cb")).isTrue();
            verify(dao, never()).listAllBaseUrls();
        }
    }

    @Nested
    @DisplayName("validateWhitelistBaseUrl - OP 接口入参格式校验")
    class ValidateWhitelistBaseUrl {

        @Test
        @DisplayName("合法 http(s) baseUrl 通过")
        void validPasses() {
            service.validateWhitelistBaseUrl("http://callback.example.com/");
            service.validateWhitelistBaseUrl("https://api.partner.com/job/cb/");
        }

        @Test
        @DisplayName("空值或非 http(s) 抛 InvalidParamException(1244036)")
        void invalidThrows() {
            assertThatThrownBy(() -> service.validateWhitelistBaseUrl(null))
                .isInstanceOf(InvalidParamException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CALLBACK_URL_WHITELIST_INVALID_BASE_URL);
            assertThatThrownBy(() -> service.validateWhitelistBaseUrl(""))
                .isInstanceOf(InvalidParamException.class);
            assertThatThrownBy(() -> service.validateWhitelistBaseUrl("ftp://x.com/"))
                .isInstanceOf(InvalidParamException.class);
            assertThatThrownBy(() -> service.validateWhitelistBaseUrl("http://"))
                .isInstanceOf(InvalidParamException.class);
        }

        @Test
        @DisplayName("baseUrl 不允许携带 userinfo / query / fragment")
        void rejectUserinfoQueryFragment() {
            assertThatThrownBy(() -> service.validateWhitelistBaseUrl("https://u:p@a.com/"))
                .isInstanceOf(InvalidParamException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CALLBACK_URL_WHITELIST_INVALID_BASE_URL);
            assertThatThrownBy(() -> service.validateWhitelistBaseUrl("https://trusted.com@evil.com/"))
                .isInstanceOf(InvalidParamException.class);
            assertThatThrownBy(() -> service.validateWhitelistBaseUrl("https://a.com/?x=1"))
                .isInstanceOf(InvalidParamException.class);
            assertThatThrownBy(() -> service.validateWhitelistBaseUrl("https://a.com/#frag"))
                .isInstanceOf(InvalidParamException.class);
        }
    }

    @Nested
    @DisplayName("invalidateCache - 显式失效缓存后重新加载")
    class InvalidateCache {

        @Test
        @DisplayName("invalidate 后下一次 isValid 会重新触发 DAO 调用")
        void invalidateTriggersReload() {
            // 第一次：DB 中暂无白名单
            when(dao.listAllBaseUrls())
                .thenReturn(Collections.emptyList())
                .thenReturn(Collections.singletonList("https://added.com/"));

            service.invalidateCache();
            assertThat(service.isValid("https://added.com/cb")).isFalse();
            verify(dao, times(1)).listAllBaseUrls();

            // 模拟 OP 新增白名单后 invalidateCache
            service.invalidateCache();
            assertThat(service.isValid("https://added.com/cb")).isTrue();
            verify(dao, times(2)).listAllBaseUrls();
        }
    }

}
