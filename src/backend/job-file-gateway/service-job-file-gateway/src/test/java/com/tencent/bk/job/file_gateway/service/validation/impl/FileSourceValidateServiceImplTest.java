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
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceWhiteInfoDAO;
import com.tencent.bk.job.file_gateway.model.req.esb.v3.EsbCreateOrUpdateFileSourceV3Req;
import com.tencent.bk.job.file_gateway.model.req.web.FileSourceCreateUpdateReq;
import com.tencent.bk.job.file_gateway.validate.ValidFileSourceInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DisplayName("FileSourceValidateServiceImpl 文件源前缀名校验测试")
class FileSourceValidateServiceImplTest {

    private FileSourceValidateServiceImpl buildService() {
        ArtifactoryConfig artifactoryConfig = new ArtifactoryConfig();
        artifactoryConfig.setArtifactoryBaseUrl("http://bkrepo.example.com");
        FileSourceWhiteInfoDAO whiteInfoDAO = mock(FileSourceWhiteInfoDAO.class);
        return new FileSourceValidateServiceImpl(artifactoryConfig, whiteInfoDAO);
    }

    @Test
    @DisplayName("checkFilePrefix：null、空串、${UUID} 以及字母数字下划线连字符（<=64）应放行")
    void shouldAllowValidFilePrefix() {
        FileSourceValidateServiceImpl service = buildService();
        assertThatCode(() -> service.checkFilePrefix(null)).doesNotThrowAnyException();
        assertThatCode(() -> service.checkFilePrefix("")).doesNotThrowAnyException();
        assertThatCode(() -> service.checkFilePrefix("${UUID}")).doesNotThrowAnyException();
        assertThatCode(() -> service.checkFilePrefix("prefix_01-abc")).doesNotThrowAnyException();
        assertThatCode(() -> service.checkFilePrefix("a".repeat(64))).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "..",
        "../",
        "../../etc/cron.d/x",
        "..\\..\\windows\\win.ini",
        "/etc/passwd",
        "\\windows\\system32",
        "a/b",
        "a\\b",
        "a..b",
        "prefix.txt",
        "prefix\0",
        " ",
        "prefix with space",
        "${OTHER}",
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    })
    @DisplayName("checkFilePrefix：含路径穿越、路径分隔符、点号、空字节、空白或超长字符串应拦截并打印原因日志")
    void shouldRejectInvalidFilePrefix(String invalidPrefix) {
        FileSourceValidateServiceImpl service = buildService();
        Logger logger = (Logger) LoggerFactory.getLogger(FileSourceValidateServiceImpl.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThatThrownBy(() -> service.checkFilePrefix(invalidPrefix))
                .isInstanceOfSatisfying(InvalidParamException.class, e ->
                    assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME));
        } finally {
            logger.detachAppender(appender);
        }
        List<String> messages = appender.list.stream()
            .map(ILoggingEvent::getFormattedMessage)
            .collect(Collectors.toList());
        assertThat(messages).anySatisfy(message ->
            assertThat(message).contains("FileSource filePrefix rejected: reason="));
    }

    @Test
    @DisplayName("ValidFileSourceInfo 校验器：Web 与 ESB 请求体对非法 filePrefix 应拒绝，对合法 Map 兼容")
    void validFileSourceInfoShouldRejectIllegalFilePrefix() {
        ValidFileSourceInfo.Validator validator = new ValidFileSourceInfo.Validator();

        Map<String, Object> validMap = new HashMap<>();
        validMap.put(FileSourceInfoConsts.KEY_BK_ARTIFACTORY_BASE_URL, "http://bkrepo.example.com");
        assertThat(validator.isValid(validMap, null)).isTrue();

        FileSourceCreateUpdateReq webReq = new FileSourceCreateUpdateReq();
        webReq.setCode("fs_web");
        webReq.setFilePrefix("../../etc/cron.d/x");
        assertThat(validator.isValid(webReq, null)).isFalse();

        webReq.setFilePrefix("${UUID}");
        assertThat(validator.isValid(webReq, null)).isTrue();

        webReq.setFilePrefix("custom_prefix-1");
        assertThat(validator.isValid(webReq, null)).isTrue();

        EsbCreateOrUpdateFileSourceV3Req esbReq = new EsbCreateOrUpdateFileSourceV3Req();
        esbReq.setCode("fs_esb");
        esbReq.setFilePrefix("../outside_");
        assertThat(validator.isValid(esbReq, null)).isFalse();

        esbReq.setFilePrefix("safe_prefix");
        assertThat(validator.isValid(esbReq, null)).isTrue();
    }
}
