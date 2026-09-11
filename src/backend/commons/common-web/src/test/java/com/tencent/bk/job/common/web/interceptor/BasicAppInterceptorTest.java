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

package com.tencent.bk.job.common.web.interceptor;

import com.tencent.bk.job.common.model.BasicApp;
import com.tencent.bk.job.common.service.CommonAppService;
import com.tencent.bk.job.common.web.model.RepeatableReadWriteHttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证内部请求的 appId 解析。重点是 body 为 JSON 数组时不能抛 {@link ClassCastException}——
 * 批量按 ID 查询类的内部接口（如 {@code /service/fileSource/basicInfo/listByIds}）直接收 List，
 * 路径与 query 里都没有 appId，会一路走到 body 解析。
 */
@DisplayName("BasicAppInterceptor 内部请求 appId 解析测试")
class BasicAppInterceptorTest {

    private static final long APP_ID = 2L;

    private CommonAppService appService;
    private BasicAppInterceptor.InternalAppParser parser;

    @BeforeEach
    void setUp() {
        appService = mock(CommonAppService.class);
        BasicApp app = new BasicApp();
        app.setId(APP_ID);
        when(appService.getApp(anyLong())).thenReturn(app);
        parser = new BasicAppInterceptor(appService).new InternalAppParser();
    }

    private RepeatableReadWriteHttpServletRequest postRequest(String servletPath, String body) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", servletPath);
        request.setServletPath(servletPath);
        request.setContent(body.getBytes(StandardCharsets.UTF_8));
        return new RepeatableReadWriteHttpServletRequest(request);
    }

    @Test
    @DisplayName("body 为 JSON 数组时返回 null，不抛异常")
    void parseApp_arrayBody_returnsNull() {
        RepeatableReadWriteHttpServletRequest request =
            postRequest("/service/fileSource/basicInfo/listByIds", "[1,2,3]");

        assertThatCode(() -> assertThat(parser.parseApp(request)).isNull())
            .doesNotThrowAnyException();
        verify(appService, never()).getApp(anyLong());
    }

    @Test
    @DisplayName("body 为 JSON 标量时返回 null，不抛异常")
    void parseApp_scalarBody_returnsNull() {
        RepeatableReadWriteHttpServletRequest request = postRequest("/service/foo", "123");

        assertThat(parser.parseApp(request)).isNull();
        verify(appService, never()).getApp(anyLong());
    }

    @Test
    @DisplayName("body 为 JSON 对象且含 appId 时按 body 解析")
    void parseApp_objectBodyWithAppId_returnsApp() {
        RepeatableReadWriteHttpServletRequest request =
            postRequest("/service/foo", "{\"appId\":" + APP_ID + "}");

        assertThat(parser.parseApp(request)).isNotNull();
        verify(appService).getApp(APP_ID);
    }

    @Test
    @DisplayName("body 为 JSON 对象但不含 appId 时返回 null")
    void parseApp_objectBodyWithoutAppId_returnsNull() {
        RepeatableReadWriteHttpServletRequest request = postRequest("/service/foo", "{\"name\":\"x\"}");

        assertThat(parser.parseApp(request)).isNull();
        verify(appService, never()).getApp(anyLong());
    }

    @Test
    @DisplayName("路径含 appId 时优先按路径解析，数组 body 不影响")
    void parseApp_appIdInPath_takesPrecedence() {
        RepeatableReadWriteHttpServletRequest request =
            postRequest("/service/app/" + APP_ID + "/fileSource/availability/check", "[1,2,3]");

        assertThat(parser.parseApp(request)).isNotNull();
        verify(appService).getApp(APP_ID);
    }
}
