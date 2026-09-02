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

package com.tencent.bk.job.common.web.feign;

import com.tencent.bk.job.common.esb.exception.OpenApiPropagatedException;
import com.tencent.bk.job.common.esb.model.v4.V4ErrorCodeEnum;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.NotFoundException;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 服务间调用的错误解码。
 * <p>
 * 两类下游响应体走两条完全不同的还原路径，且<b>必须互不干扰</b>：inner 接口的 InternalResponse 沿用
 * "内部错误一律降级为 InternalException"的老语义；OpenAPI 的错误体则原样带回，因为那里面的错误
 * 通常源自最上层调用方填的参数，降级成"内部错误"会让用户无从修正。
 */
class FeignErrorDecoderTest {

    private final FeignErrorDecoder decoder = new FeignErrorDecoder();

    @Nested
    @DisplayName("下游是 OpenAPI 接口")
    class OpenApiResponseTest {

        @Test
        @DisplayName("4xx 错误体原样带回，并标为下游已明确拒绝")
        void given4xxThenPropagateAndMarkRejected() {
            String body = "{\"error\":{\"code\":\"INVALID_ARGUMENT\",\"message\":\"账号不存在: root\","
                + "\"details\":[{\"code\":\"1244003\",\"message\":\"账号不存在: root\"}]}}";

            Exception exception = decoder.decode("someMethod", buildResponse(400, body));

            assertThat(exception).isInstanceOf(OpenApiPropagatedException.class);
            OpenApiPropagatedException propagated = (OpenApiPropagatedException) exception;
            // 消息必须是下游渲染好的那一句，重新渲染会丢掉占位符里的账号名
            assertThat(propagated.getError().getMessage()).isEqualTo("账号不存在: root");
            assertThat(propagated.getError().getDetails()).hasSize(1);
            assertThat(propagated.getError().getDetails().get(0).getCode()).isEqualTo("1244003");
            assertThat(propagated.getV4ErrorCode()).isEqualTo(V4ErrorCodeEnum.INVALID_ARGUMENT);
            assertThat(propagated.isRejectedByDownstream()).isTrue();
        }

        @Test
        @DisplayName("5xx 不算明确拒绝：下游内部出错时操作是否已生效并不确定")
        void given5xxThenNotMarkedAsRejected() {
            String body = "{\"error\":{\"code\":\"INTERNAL\",\"message\":\"内部错误\"}}";

            Exception exception = decoder.decode("someMethod", buildResponse(500, body));

            assertThat(exception).isInstanceOf(OpenApiPropagatedException.class);
            assertThat(((OpenApiPropagatedException) exception).isRejectedByDownstream()).isFalse();
        }

        @Test
        @DisplayName("无法识别的语义化错误码按内部错误处理")
        void givenUnknownCodeThenFallbackToInternal() {
            String body = "{\"error\":{\"code\":\"SOME_NEW_CODE\",\"message\":\"未知\"}}";

            Exception exception = decoder.decode("someMethod", buildResponse(400, body));

            assertThat(((OpenApiPropagatedException) exception).getV4ErrorCode())
                .isEqualTo(V4ErrorCodeEnum.INTERNAL);
        }
    }

    @Nested
    @DisplayName("下游是 inner 接口")
    class InternalResponseTest {

        @Test
        @DisplayName("INVALID_PARAM 仍降级为 InternalException，老语义不受 OpenAPI 分支影响")
        void givenInvalidParamThenStillDegradeToInternal() {
            // errorType=2 即 INVALID_PARAM
            String body = "{\"code\":1244003,\"errorType\":2,\"errorMsg\":\"参数错误\",\"success\":false}";

            Exception exception = decoder.decode("someMethod", buildResponse(400, body));

            assertThat(exception).isInstanceOf(InternalException.class);
        }

        @Test
        @DisplayName("NOT_FOUND 仍还原为 NotFoundException")
        void givenNotFoundThenRestoreNotFoundException() {
            // errorType=6 即 NOT_FOUND
            String body = "{\"code\":1244004,\"errorType\":6,\"errorMsg\":\"资源不存在\",\"success\":false}";

            Exception exception = decoder.decode("someMethod", buildResponse(404, body));

            assertThat(exception).isInstanceOf(NotFoundException.class);
        }
    }

    @Nested
    @DisplayName("响应体无法识别")
    class UnrecognizedBodyTest {

        @Test
        @DisplayName("响应体为空时返回原始 FeignException")
        void givenEmptyBodyThenReturnOriginalException() {
            Exception exception = decoder.decode("someMethod", buildResponse(500, ""));

            assertThat(exception).isInstanceOf(FeignException.class);
            assertThat(exception).isNotInstanceOf(OpenApiPropagatedException.class);
        }

        @Test
        @DisplayName("既非 InternalResponse 也非 OpenAPI 错误体时返回原始 FeignException")
        void givenIrrelevantJsonThenReturnOriginalException() {
            Exception exception = decoder.decode("someMethod", buildResponse(502, "{\"foo\":\"bar\"}"));

            assertThat(exception).isInstanceOf(FeignException.class);
            assertThat(exception).isNotInstanceOf(OpenApiPropagatedException.class);
        }
    }

    private Response buildResponse(int status, String body) {
        Request request = Request.create(
            Request.HttpMethod.POST,
            "http://job-execute/esb/api/v4/fast_execute_script",
            Collections.emptyMap(),
            null,
            StandardCharsets.UTF_8,
            new RequestTemplate()
        );
        return Response.builder()
            .status(status)
            .reason("error")
            .request(request)
            .headers(Collections.emptyMap())
            .body(body, StandardCharsets.UTF_8)
            .build();
    }
}
