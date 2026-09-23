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

package com.tencent.bk.job.file_gateway.service.validation.impl;

import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.file_gateway.config.ArtifactoryConfig;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceWhiteInfoDAO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("FileSourceValidateServiceImpl 制品库根地址校验测试")
class FileSourceValidateServiceImplTest {

    private static final String ENV_BASE_URL = "http://bkrepo.example.com";

    private enum AddressType {
        PUBLIC, LOOPBACK, SITE_LOCAL, LINK_LOCAL, ANY_LOCAL, MULTICAST, IPV6_ULA
    }

    private static final Map<String, AddressType> DNS = new HashMap<>();

    static {
        DNS.put("bkrepo.example.com", AddressType.PUBLIC);
        DNS.put("sub.bkrepo.example.com", AddressType.PUBLIC);
        DNS.put("example.com", AddressType.PUBLIC);
        DNS.put("loopback.bkrepo.example.com", AddressType.LOOPBACK);
        DNS.put("sitelocal.bkrepo.example.com", AddressType.SITE_LOCAL);
        DNS.put("linklocal.bkrepo.example.com", AddressType.LINK_LOCAL);
        DNS.put("anylocal.bkrepo.example.com", AddressType.ANY_LOCAL);
        DNS.put("multicast.bkrepo.example.com", AddressType.MULTICAST);
        DNS.put("ula.bkrepo.example.com", AddressType.IPV6_ULA);
        DNS.put("cos.example.com", AddressType.PUBLIC);
        DNS.put("loopback.cos.example.com", AddressType.LOOPBACK);
        DNS.put("sitelocal.cos.example.com", AddressType.SITE_LOCAL);
    }

    /**
     * 按地址类型构造 InetAddress，避免在代码中出现真实 IP
     */
    private static InetAddress mockAddress(AddressType type) {
        InetAddress address = mock(InetAddress.class);
        when(address.isLoopbackAddress()).thenReturn(type == AddressType.LOOPBACK);
        when(address.isSiteLocalAddress()).thenReturn(type == AddressType.SITE_LOCAL);
        when(address.isLinkLocalAddress()).thenReturn(type == AddressType.LINK_LOCAL);
        when(address.isAnyLocalAddress()).thenReturn(type == AddressType.ANY_LOCAL);
        when(address.isMulticastAddress()).thenReturn(type == AddressType.MULTICAST);
        byte[] bytes = new byte[type == AddressType.IPV6_ULA ? 16 : 4];
        if (type == AddressType.IPV6_ULA) {
            bytes[0] = (byte) 0xfd;
        }
        when(address.getAddress()).thenReturn(bytes);
        return address;
    }

    private FileSourceWhiteInfoDAO whiteInfoDAO;

    private FileSourceValidateServiceImpl buildService(String envBaseUrl) {
        ArtifactoryConfig artifactoryConfig = new ArtifactoryConfig();
        artifactoryConfig.setArtifactoryBaseUrl(envBaseUrl);
        whiteInfoDAO = mock(FileSourceWhiteInfoDAO.class);
        when(whiteInfoDAO.exists(anyString(), anyString())).thenReturn(false);
        FileSourceValidateServiceImpl service = new FileSourceValidateServiceImpl(artifactoryConfig, whiteInfoDAO);
        service.setHostResolver(host -> {
            AddressType type = DNS.get(host);
            if (type == null) {
                throw new UnknownHostException(host);
            }
            return new InetAddress[]{mockAddress(type)};
        });
        return service;
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "http://bkrepo.example.com",
        "http://bkrepo.example.com/",
        "https://bkrepo.example.com/generic",
        "http://BKREPO.example.com",
        "http://sub.bkrepo.example.com",
        "http://sub.bkrepo.example.com:8080/bkrepo"
    })
    @DisplayName("当前环境制品库域名及其子域名且解析为非内网地址时应放行")
    void shouldAllowCurrentEnvHost(String url) {
        FileSourceValidateServiceImpl service = buildService(ENV_BASE_URL);
        assertThatCode(() -> service.checkBkArtifactoryBaseUrl(url)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("子路径部署模式下当前环境域名的子路径地址应放行")
    void shouldAllowSubPathDeployment() {
        FileSourceValidateServiceImpl service = buildService("http://example.com/bkrepo");
        assertThatCode(() -> service.checkBkArtifactoryBaseUrl("http://example.com/bkrepo"))
            .doesNotThrowAnyException();
        assertThatCode(() -> service.checkBkArtifactoryBaseUrl("http://bkrepo.example.com/bkrepo"))
            .doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "http://http://127.0.0.1:18888/latest/meta-data/.bkrepo.example.com",
        "http://127.0.0.1:18888/latest/meta-data/.bkrepo.example.com",
        "http://127.0.0.1:18888/x?.bkrepo.example.com",
        "http://127.0.0.1:18888/x#.bkrepo.example.com",
        "http://sub.bkrepo.example.com@127.0.0.1:18888",
        "http://evilbkrepo.example.com",
        "http://bkrepo.example.com.evil.com",
        "http://.bkrepo.example.com",
        "http://a..bkrepo.example.com",
        "http://com"
    })
    @DisplayName("host 不是当前环境制品库域名或子域名时应拦截")
    void shouldRejectHostNotOfCurrentEnv(String url) {
        FileSourceValidateServiceImpl service = buildService(ENV_BASE_URL);
        assertThatThrownBy(() -> service.checkBkArtifactoryBaseUrl(url))
            .isInstanceOf(InvalidParamException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "file:///etc/passwd",
        "ftp://bkrepo.example.com",
        "gopher://bkrepo.example.com",
        "dict://bkrepo.example.com",
        "jar:http://bkrepo.example.com!/",
        "bkrepo.example.com",
        "http://",
        " "
    })
    @DisplayName("非 http/https 协议或非法 URL 应拦截，即使在白名单中")
    void shouldRejectIllegalProtocol(String url) {
        FileSourceValidateServiceImpl service = buildService(ENV_BASE_URL);
        when(whiteInfoDAO.exists(anyString(), anyString())).thenReturn(true);
        assertThatThrownBy(() -> service.checkBkArtifactoryBaseUrl(url))
            .isInstanceOf(InvalidParamException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "http://loopback.bkrepo.example.com",
        "http://sitelocal.bkrepo.example.com",
        "http://linklocal.bkrepo.example.com/latest/meta-data/",
        "http://anylocal.bkrepo.example.com",
        "http://multicast.bkrepo.example.com",
        "http://ula.bkrepo.example.com",
        "http://unresolvable.bkrepo.example.com"
    })
    @DisplayName("当前环境子域名但解析为环回/内网/链路本地地址或无法解析时应拦截")
    void shouldRejectInternalResolvedHost(String url) {
        FileSourceValidateServiceImpl service = buildService(ENV_BASE_URL);
        assertThatThrownBy(() -> service.checkBkArtifactoryBaseUrl(url))
            .isInstanceOf(InvalidParamException.class);
    }

    @Test
    @DisplayName("解析为内网地址的地址在白名单中时应放行")
    void shouldAllowInternalResolvedHostInWhiteList() {
        FileSourceValidateServiceImpl service = buildService(ENV_BASE_URL);
        String url = "http://sitelocal.bkrepo.example.com";
        when(whiteInfoDAO.exists(anyString(), eq(url))).thenReturn(true);
        assertThatCode(() -> service.checkBkArtifactoryBaseUrl(url)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "not-a-url", "ftp://bkrepo.example.com"})
    @DisplayName("当前环境制品库地址未配置或非法时，非白名单地址一律拦截")
    void shouldRejectWhenEnvBaseUrlNotConfigured(String envBaseUrl) {
        FileSourceValidateServiceImpl service = buildService(envBaseUrl);
        assertThatThrownBy(() -> service.checkBkArtifactoryBaseUrl("http://bkrepo.example.com"))
            .isInstanceOf(InvalidParamException.class);
        assertThatThrownBy(() -> service.checkBkArtifactoryBaseUrl("http://any.domain.com"))
            .isInstanceOf(InvalidParamException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("当前环境制品库地址未配置时，白名单地址应放行")
    void shouldAllowWhiteListWhenEnvBaseUrlNotConfigured(String envBaseUrl) {
        FileSourceValidateServiceImpl service = buildService(envBaseUrl);
        String url = "http://other-env.bkrepo.com";
        when(whiteInfoDAO.exists(anyString(), eq(url))).thenReturn(true);
        assertThatCode(() -> service.checkBkArtifactoryBaseUrl(url)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("COS 接入点为合法域名时应放行，环回应拦截，站点本地地址可放行")
    void shouldValidateCosEndPointDomain() {
        FileSourceValidateServiceImpl service = buildService(ENV_BASE_URL);
        assertThatCode(() -> service.checkCosEndPointDomain("cos.example.com")).doesNotThrowAnyException();
        assertThatCode(() -> service.checkCosEndPointDomain("https://cos.example.com")).doesNotThrowAnyException();
        assertThatCode(() -> service.checkCosEndPointDomain("sitelocal.cos.example.com")).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.checkCosEndPointDomain("loopback.cos.example.com"))
            .isInstanceOf(InvalidParamException.class);
        assertThatThrownBy(() -> service.checkCosEndPointDomain("ftp://cos.example.com"))
            .isInstanceOf(InvalidParamException.class);
        assertThatThrownBy(() -> service.checkCosEndPointDomain("127.0.0.1"))
            .isInstanceOf(InvalidParamException.class);
    }

    @Test
    @DisplayName("写入白名单的制品库根地址必须是合法 http(s) URL")
    void shouldValidateWhiteBaseUrlFormat() {
        FileSourceValidateServiceImpl service = buildService(ENV_BASE_URL);
        assertThatCode(() -> service.validateWhiteBaseUrl("https://other.example.com")).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.validateWhiteBaseUrl("ftp://other.example.com"))
            .isInstanceOf(InvalidParamException.class);
        assertThatThrownBy(() -> service.validateWhiteBaseUrl("http://user@other.example.com"))
            .isInstanceOf(InvalidParamException.class);
    }
}
