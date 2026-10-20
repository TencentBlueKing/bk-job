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

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.file_gateway.config.ArtifactoryConfig;
import com.tencent.bk.job.file_gateway.consts.FileSourceInfoConsts;
import com.tencent.bk.job.file_gateway.consts.FileSourceTypeEnum;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceWhiteInfoDAO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("FileSourceValidateServiceImpl 文件源信息校验测试")
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
    @DisplayName("当前环境制品库域名及其子域名且解析为非环回地址时应放行")
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
        "http://sitelocal.bkrepo.example.com",
        "http://linklocal.bkrepo.example.com/latest/meta-data/",
        "http://multicast.bkrepo.example.com",
        "http://ula.bkrepo.example.com"
    })
    @DisplayName("当前环境子域名解析为局域网、链路本地、组播、ULA 等非环回非通配地址时应放行")
    void shouldAllowLanResolvedEnvHost(String url) {
        FileSourceValidateServiceImpl service = buildService(ENV_BASE_URL);
        assertThatCode(() -> service.checkBkArtifactoryBaseUrl(url)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "http://loopback.bkrepo.example.com",
        "http://anylocal.bkrepo.example.com",
        "http://unresolvable.bkrepo.example.com"
    })
    @DisplayName("当前环境子域名解析为环回、通配地址或无法解析时应拦截")
    void shouldRejectLoopbackOrUnresolvableEnvHost(String url) {
        FileSourceValidateServiceImpl service = buildService(ENV_BASE_URL);
        assertThatThrownBy(() -> service.checkBkArtifactoryBaseUrl(url))
            .isInstanceOf(InvalidParamException.class);
    }

    @Test
    @DisplayName("解析为环回地址的地址在白名单中时应放行")
    void shouldAllowLoopbackResolvedHostInWhiteList() {
        FileSourceValidateServiceImpl service = buildService(ENV_BASE_URL);
        String url = "http://loopback.bkrepo.example.com";
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

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        "cos.example.com",
        "https://cos.example.com",
        "loopback.cos.example.com",
        "127.0.0.1",
        "ftp://cos.example.com"
    })
    @DisplayName("腾讯云 COS 文件源尚未支持，不论接入点是否合法均拒绝")
    void shouldRejectTencentCloudCos(String endPointDomain) {
        FileSourceValidateServiceImpl service = buildService(ENV_BASE_URL);
        Map<String, Object> fileSourceInfoMap = new HashMap<>();
        fileSourceInfoMap.put(FileSourceInfoConsts.KEY_COS_END_POINT_DOMAIN, endPointDomain);
        String typeCode = FileSourceTypeEnum.TENCENT_CLOUD_COS.name();
        assertThatThrownBy(() -> service.checkFileSource(typeCode, fileSourceInfoMap))
            .isInstanceOfSatisfying(InvalidParamException.class, e -> {
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FILE_SOURCE_TYPE_NOT_SUPPORTED);
                assertThat(e.getErrorParams()).containsExactly(typeCode);
            });
        assertThatThrownBy(() -> service.checkFileSource(typeCode, null))
            .isInstanceOf(InvalidParamException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"OTHER_TYPE", " ", "blueking_artifactory"})
    @DisplayName("除蓝鲸制品库外的其他类型（含未知、空类型编码）均尚未支持，一律拒绝")
    void shouldRejectNonArtifactoryType(String typeCode) {
        FileSourceValidateServiceImpl service = buildService(ENV_BASE_URL);
        Map<String, Object> fileSourceInfoMap = new HashMap<>();
        fileSourceInfoMap.put(FileSourceInfoConsts.KEY_BK_ARTIFACTORY_BASE_URL, "http://sub.bkrepo.example.com");
        assertThatThrownBy(() -> service.checkFileSource(typeCode, fileSourceInfoMap))
            .isInstanceOfSatisfying(InvalidParamException.class, e -> {
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FILE_SOURCE_TYPE_NOT_SUPPORTED);
                assertThat(e.getErrorParams()).containsExactly(typeCode);
            });
        assertThatThrownBy(() -> service.checkFileSource(typeCode, null))
            .isInstanceOfSatisfying(InvalidParamException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FILE_SOURCE_TYPE_NOT_SUPPORTED));
    }

    @Test
    @DisplayName("checkFileSource 对制品库类型校验根地址：合法放行，非法报根地址不合法")
    void shouldCheckArtifactoryBaseUrl() {
        FileSourceValidateServiceImpl service = buildService(ENV_BASE_URL);
        String artifactory = FileSourceTypeEnum.BLUEKING_ARTIFACTORY.name();
        Map<String, Object> validInfo = new HashMap<>();
        validInfo.put(FileSourceInfoConsts.KEY_BK_ARTIFACTORY_BASE_URL, "http://sub.bkrepo.example.com");
        Map<String, Object> invalidInfo = new HashMap<>();
        invalidInfo.put(FileSourceInfoConsts.KEY_BK_ARTIFACTORY_BASE_URL, "http://loopback.bkrepo.example.com");

        assertThatCode(() -> service.checkFileSource(artifactory, validInfo)).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.checkFileSource(artifactory, invalidInfo))
            .isInstanceOfSatisfying(InvalidParamException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.BK_ARTIFACTORY_BASE_URL_INVALID));
        assertThatThrownBy(() -> service.checkFileSource(artifactory, null))
            .isInstanceOfSatisfying(InvalidParamException.class, e ->
                assertThat(e.getErrorCode()).isEqualTo(ErrorCode.BK_ARTIFACTORY_BASE_URL_INVALID));
    }

    @ParameterizedTest
    @ValueSource(strings = {"TENCENT_CLOUD_COS", "OTHER_TYPE"})
    @DisplayName("不支持的文件源类型被拒绝时应输出 WARN 日志并带上原因与文件源类型")
    void shouldLogUnsupportedFileSourceType(String typeCode) {
        FileSourceValidateServiceImpl service = buildService(ENV_BASE_URL);
        List<String> messages = captureWarnLogs(() -> service.checkFileSource(typeCode, new HashMap<>()));
        assertThat(messages).anySatisfy(message ->
            assertThat(message).contains("file source type not supported").contains(typeCode));
    }

    @Test
    @DisplayName("类型编码中的控制字符不应原样写入日志")
    void shouldSanitizeTypeCodeInLog() {
        FileSourceValidateServiceImpl service = buildService(ENV_BASE_URL);
        List<String> messages = captureWarnLogs(() -> service.checkFileSource("EVIL\r\nFORGED", null));
        assertThat(messages).anySatisfy(message ->
            assertThat(message).contains("EVILFORGED").doesNotContain("\n").doesNotContain("\r"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "http://u:secretPwd@loopback.bkrepo.example.com/generic",
        "http://u:secretPwd@exa mple.com/generic"
    })
    @DisplayName("制品库根地址被拒绝时日志中不应输出 userinfo")
    void shouldNotLogBkArtifactoryUserInfo(String url) {
        FileSourceValidateServiceImpl service = buildService(ENV_BASE_URL);
        List<String> messages = captureWarnLogs(() -> service.checkBkArtifactoryBaseUrl(url));
        assertThat(messages).isNotEmpty();
        assertThat(messages).allSatisfy(message ->
            assertThat(message).doesNotContain("secretPwd").doesNotContain("@"));
    }

    private static List<String> captureWarnLogs(Runnable action) {
        Logger logger = (Logger) LoggerFactory.getLogger(FileSourceValidateServiceImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThatThrownBy(action::run).isInstanceOf(InvalidParamException.class);
        } finally {
            logger.detachAppender(appender);
        }
        return appender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .collect(Collectors.toList());
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
