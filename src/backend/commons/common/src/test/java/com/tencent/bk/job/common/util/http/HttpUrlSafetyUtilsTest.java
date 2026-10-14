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

package com.tencent.bk.job.common.util.http;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("HttpUrlSafetyUtils URL 安全解析测试")
class HttpUrlSafetyUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "http://bkrepo.example.com",
        "https://bkrepo.example.com/generic",
        "HTTP://BKREPO.example.com"
    })
    void parseHttpUrlHostShouldReturnHost(String url) {
        assertThat(HttpUrlSafetyUtils.parseHttpUrlHost(url)).isEqualTo("bkrepo.example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "ftp://bkrepo.example.com",
        "file:///etc/passwd",
        "gopher://bkrepo.example.com",
        "bkrepo.example.com",
        "http://",
        " "
    })
    void parseHttpUrlHostShouldRejectInvalid(String url) {
        assertThat(HttpUrlSafetyUtils.parseHttpUrlHost(url)).isNull();
    }

    @Test
    void parseHttpUrlOrBareHostShouldAcceptBareHost() {
        assertThat(HttpUrlSafetyUtils.parseHttpUrlOrBareHost("cos.example.com")).isEqualTo("cos.example.com");
        assertThat(HttpUrlSafetyUtils.parseHttpUrlOrBareHost("cos.example.com:443")).isEqualTo("cos.example.com");
        assertThat(HttpUrlSafetyUtils.parseHttpUrlOrBareHost("https://cos.example.com/path"))
            .isEqualTo("cos.example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "cos.example.com/other",
        "ftp://cos.example.com",
        "cos.example.com?x=1"
    })
    void parseHttpUrlOrBareHostShouldRejectInvalid(String endpoint) {
        assertThat(HttpUrlSafetyUtils.parseHttpUrlOrBareHost(endpoint)).isNull();
    }

    @Test
    void isHostOrChildHostShouldMatchExactAndSubdomain() {
        assertThat(HttpUrlSafetyUtils.isHostOrChildHost("bkrepo.example.com", "bkrepo.example.com")).isTrue();
        assertThat(HttpUrlSafetyUtils.isHostOrChildHost("sub.bkrepo.example.com", "bkrepo.example.com")).isTrue();
        assertThat(HttpUrlSafetyUtils.isHostOrChildHost("evilbkrepo.example.com", "bkrepo.example.com")).isFalse();
        assertThat(HttpUrlSafetyUtils.isHostOrChildHost("bkrepo.example.com.evil.com", "bkrepo.example.com")).isFalse();
        assertThat(HttpUrlSafetyUtils.isHostOrChildHost(".bkrepo.example.com", "bkrepo.example.com")).isFalse();
        assertThat(HttpUrlSafetyUtils.isHostOrChildHost("a..bkrepo.example.com", "bkrepo.example.com")).isFalse();
    }

    private static boolean resolvedToLocalHost(InetAddress... addresses) {
        return HttpUrlSafetyUtils.isResolvedToLocalHostAddress("target.example.com", host -> addresses);
    }

    @Test
    @DisplayName("解析为环回或通配地址时视为本机地址")
    void isResolvedToLocalHostAddressShouldMatchLoopbackAndAnyLocal() throws UnknownHostException {
        InetAddress loopback = mock(InetAddress.class);
        when(loopback.isLoopbackAddress()).thenReturn(true);

        assertThat(resolvedToLocalHost(loopback)).isTrue();
        assertThat(resolvedToLocalHost(InetAddress.getByName("127.0.0.1"))).isTrue();
        assertThat(resolvedToLocalHost(InetAddress.getByAddress(new byte[4]))).isTrue();
        assertThat(resolvedToLocalHost(InetAddress.getByAddress(new byte[16]))).isTrue();
    }

    @Test
    @DisplayName("解析失败、空结果或空地址时按失败关闭视为本机地址")
    void isResolvedToLocalHostAddressShouldFailClosed() {
        assertThat(HttpUrlSafetyUtils.isResolvedToLocalHostAddress(
            "unresolvable.example.com", host -> {
                throw new UnknownHostException(host);
            })).isTrue();
        assertThat(resolvedToLocalHost()).isTrue();
        assertThat(HttpUrlSafetyUtils.isResolvedToLocalHostAddress("target.example.com", host -> null)).isTrue();
        assertThat(resolvedToLocalHost((InetAddress) null)).isTrue();
        assertThat(HttpUrlSafetyUtils.isResolvedToLocalHostAddress(" ", host -> new InetAddress[0])).isTrue();
    }

    @Test
    @DisplayName("站点本地、链路本地、组播、IPv6 ULA 地址均放行")
    void isResolvedToLocalHostAddressShouldAllowOtherInternalAddresses() throws UnknownHostException {
        InetAddress siteLocal = mock(InetAddress.class);
        when(siteLocal.isSiteLocalAddress()).thenReturn(true);
        InetAddress multicast = mock(InetAddress.class);
        when(multicast.isMulticastAddress()).thenReturn(true);
        byte[] ula = new byte[16];
        ula[0] = (byte) 0xfd;
        ula[15] = 1;

        assertThat(resolvedToLocalHost(siteLocal)).isFalse();
        assertThat(resolvedToLocalHost(
            InetAddress.getByAddress(new byte[]{(byte) 169, (byte) 254, 1, 2}))).isFalse();
        assertThat(resolvedToLocalHost(multicast)).isFalse();
        assertThat(resolvedToLocalHost(InetAddress.getByAddress(ula))).isFalse();
    }

    @Test
    @DisplayName("多个解析结果中只要有一个通配地址即视为本机地址")
    void isResolvedToLocalHostAddressShouldMatchAnyOfMultipleAddresses() throws UnknownHostException {
        InetAddress siteLocal = mock(InetAddress.class);
        when(siteLocal.isSiteLocalAddress()).thenReturn(true);

        assertThat(resolvedToLocalHost(siteLocal, InetAddress.getByAddress(new byte[4]))).isTrue();
        assertThat(resolvedToLocalHost(
            InetAddress.getByAddress(new byte[]{(byte) 169, (byte) 254, 1, 2}), siteLocal)).isFalse();
    }

    @Test
    void isValidWhitelistHttpBaseUrl() {
        assertThat(HttpUrlSafetyUtils.isValidWhitelistHttpBaseUrl("http://bkrepo.example.com")).isTrue();
        assertThat(HttpUrlSafetyUtils.isValidWhitelistHttpBaseUrl("https://bkrepo.example.com/generic")).isTrue();
        assertThat(HttpUrlSafetyUtils.isValidWhitelistHttpBaseUrl("ftp://bkrepo.example.com")).isFalse();
        assertThat(HttpUrlSafetyUtils.isValidWhitelistHttpBaseUrl("http://user@bkrepo.example.com")).isFalse();
        assertThat(HttpUrlSafetyUtils.isValidWhitelistHttpBaseUrl("http://bkrepo.example.com?x=1")).isFalse();
        assertThat(HttpUrlSafetyUtils.isValidWhitelistHttpBaseUrl("http://bkrepo.example.com#x")).isFalse();
    }

    @Test
    @DisplayName("日志输出的 URL 应去掉 userinfo")
    void toLogSafeUrlShouldStripUserInfo() {
        assertThat(HttpUrlSafetyUtils.toLogSafeUrl("http://u:secretPwd@example.com/path?x=1", 512))
            .isEqualTo("http://example.com/path?x=1");
        assertThat(HttpUrlSafetyUtils.toLogSafeUrl("https://user:secretPwd@example.com:8443/a@b", 512))
            .isEqualTo("https://example.com:8443/a@b");
        assertThat(HttpUrlSafetyUtils.toLogSafeUrl("user:secretPwd@cos.example.com", 512))
            .isEqualTo("cos.example.com");
        assertThat(HttpUrlSafetyUtils.toLogSafeUrl("secretUser@cos.example.com:8080", 512))
            .isEqualTo("cos.example.com:8080");
        assertThat(HttpUrlSafetyUtils.toLogSafeUrl(null, 512)).isEqualTo("");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "http://example.com/path?x=1",
        "https://example.com:8443/a@b#frag",
        "cos.example.com",
        "cos.example.com:8080"
    })
    @DisplayName("无 userinfo 时日志输出与原串一致")
    void toLogSafeUrlShouldKeepUrlWithoutUserInfo(String url) {
        assertThat(HttpUrlSafetyUtils.toLogSafeUrl(url, 512)).isEqualTo(url);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "http://u:secretPwd@exa mple.com/x",
        "http://u:secret/Pwd@example.com/x",
        "http://u:secretPwd@ex_ample.com",
        "u:secretPwd@exa mple.com",
        "http://u:secretPwd@@example.com",
        "user:secretPwd@cos.example.com:notaport"
    })
    @DisplayName("畸形 URL 也不能在日志中输出密码")
    void toLogSafeUrlShouldNotLeakPasswordForMalformedUrl(String url) {
        assertThat(HttpUrlSafetyUtils.toLogSafeUrl(url, 512))
            .doesNotContain("secret")
            .doesNotContain("@");
    }

    @Test
    @DisplayName("日志输出的 URL 应去掉控制字符并截断")
    void toLogSafeUrlShouldSanitize() {
        assertThat(HttpUrlSafetyUtils.toLogSafeUrl("http://u:p@example.com/a\r\nb", 512))
            .isEqualTo("http://example.com/ab");
        assertThat(HttpUrlSafetyUtils.toLogSafeUrl("http://example.com/abcdef", 18))
            .isEqualTo("http://example.com...");
    }

    @Test
    @DisplayName("允许 k8s Service 短名与带命名空间的主机名")
    void allowKubernetesDnsHost() {
        assertThat(HttpUrlSafetyUtils.isAllowedServiceHost("bk-job-file-worker")).isTrue();
        assertThat(HttpUrlSafetyUtils.isAllowedServiceHost(
            "bk-job-file-worker-abcde.bk-job-file-worker.bk-job-gray")).isTrue();
        assertThat(HttpUrlSafetyUtils.isAllowedServiceHost(
            "bk-job-file-worker.bk-job-gray.svc.cluster.local")).isTrue();
    }

    @Test
    @DisplayName("允许 IP 字面量，拒绝 host 中夹带 path/userinfo")
    void allowIpLiteralRejectHostInjection() {
        assertThat(HttpUrlSafetyUtils.isAllowedServiceHost("127.0.0.1")).isTrue();
        assertThat(HttpUrlSafetyUtils.isAllowedServiceHost("127.0.0.1/latest/meta-data")).isFalse();
        assertThat(HttpUrlSafetyUtils.isAllowedServiceHost("worker.svc@127.0.0.1")).isFalse();
        assertThat(HttpUrlSafetyUtils.isAllowedServiceHost("worker.svc?x=1")).isFalse();
        assertThat(HttpUrlSafetyUtils.isAllowedServiceHost("")).isFalse();
    }

    @Test
    @DisplayName("IPv6 字面量拼 URL 时自动加中括号")
    void wrapIpv6HostForUrl() {
        assertThat(HttpUrlSafetyUtils.hostForUrl("::1")).isEqualTo("[::1]");
        assertThat(HttpUrlSafetyUtils.hostForUrl("bk-job-file-worker")).isEqualTo("bk-job-file-worker");
        assertThat(HttpUrlSafetyUtils.hostForUrl("[::1]")).isEqualTo("[::1]");
    }

    @Test
    @DisplayName("只允许 http/https，拒绝 userinfo 与其它协议")
    void rejectNonHttpAndUserInfo() {
        assertThat(HttpUrlSafetyUtils.parseSafeInternalHttpUri("ftp://127.0.0.1/x")).isNull();
        assertThat(HttpUrlSafetyUtils.parseSafeInternalHttpUri("file:///etc/passwd")).isNull();
        assertThat(HttpUrlSafetyUtils.parseSafeInternalHttpUri("http://user@example.com/x")).isNull();
        assertThat(HttpUrlSafetyUtils.parseSafeInternalHttpUri("/relative")).isNull();
        assertThat(HttpUrlSafetyUtils.parseSafeInternalHttpUri(null)).isNull();
    }

    @Test
    @DisplayName("回环地址允许（同机/本地部署 Worker）")
    void allowLoopbackLiteral() {
        URI uri = HttpUrlSafetyUtils.parseSafeInternalHttpUri("http://127.0.0.1:19809/actuator/health");
        assertThat(uri).isNotNull();
        assertThat(uri.getHost()).isEqualTo("127.0.0.1");
        assertThat(uri.getPort()).isEqualTo(19809);
    }

    @Test
    @DisplayName("解析到链路本地地址时拒绝")
    void rejectResolvedLinkLocal() throws UnknownHostException {
        InetAddress linkLocal = Mockito.mock(InetAddress.class);
        Mockito.when(linkLocal.isLinkLocalAddress()).thenReturn(true);
        Mockito.when(linkLocal.isAnyLocalAddress()).thenReturn(false);
        Mockito.when(linkLocal.isMulticastAddress()).thenReturn(false);

        URI uri = HttpUrlSafetyUtils.parseSafeInternalHttpUri(
            "http://bk-job-file-worker.bk-job-gray:19809/actuator/health",
            host -> new InetAddress[]{linkLocal}
        );
        assertThat(uri).isNull();
        assertThat(HttpUrlSafetyUtils.isBlockedInternalHttpTarget(linkLocal)).isTrue();
    }

    @Test
    @DisplayName("K8s 主机名解析到站点本地地址时允许")
    void allowResolvedSiteLocalForK8sHost() throws UnknownHostException {
        InetAddress siteLocal = Mockito.mock(InetAddress.class);
        Mockito.when(siteLocal.isLinkLocalAddress()).thenReturn(false);
        Mockito.when(siteLocal.isAnyLocalAddress()).thenReturn(false);
        Mockito.when(siteLocal.isMulticastAddress()).thenReturn(false);

        URI uri = HttpUrlSafetyUtils.parseSafeInternalHttpUri(
            "http://bk-job-file-worker.bk-job-gray:19809/worker/api/list",
            host -> {
                assertThat(host).isEqualTo("bk-job-file-worker.bk-job-gray");
                return new InetAddress[]{siteLocal};
            }
        );
        assertThat(uri).isNotNull();
        assertThat(HttpUrlSafetyUtils.isBlockedInternalHttpTarget(siteLocal)).isFalse();
    }
}
