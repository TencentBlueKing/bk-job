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

package com.tencent.bk.job.execute.service;

import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.job.common.validation.ValidationGroups;
import com.tencent.bk.job.execute.model.esb.v4.req.V4FastExecuteScriptRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * v4 快速执行脚本请求的 BASE64 字段校验单测。
 * <p>
 * 协议要求 BASE64 的字段收到明文时，宽松解码器不会报错而是解出一串二进制垃圾：明文 "111" 会变成
 * 0xD7 0x5D，既被当作脚本参数下发执行，又让审批人在审批内容里看到乱码。这里断言这类取值在入口就被拒。
 */
class V4FastExecuteScriptRequestBase64ValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
    }

    @Test
    @DisplayName("脚本参数不是 BASE64 时校验不通过")
    void givenNotBase64ScriptParamThenReject() {
        V4FastExecuteScriptRequest request = new V4FastExecuteScriptRequest();
        request.setScriptParam("111");

        assertThat(validator.validateProperty(request, "scriptParam")).isNotEmpty();
    }

    @Test
    @DisplayName("脚本参数是 BASE64 时校验通过")
    void givenBase64ScriptParamThenPass() {
        V4FastExecuteScriptRequest request = new V4FastExecuteScriptRequest();
        request.setScriptParam(Base64Util.encodeContentToStr("111"));

        assertThat(validator.validateProperty(request, "scriptParam")).isEmpty();
    }

    @Test
    @DisplayName("脚本内容不是 BASE64 时校验不通过")
    void givenNotBase64ScriptContentThenReject() {
        V4FastExecuteScriptRequest request = new V4FastExecuteScriptRequest();
        request.setContent("echo 1");

        assertThat(validator.validateProperty(request, "content",
            ValidationGroups.Script.ScriptContent.class)).isNotEmpty();
    }

    @Test
    @DisplayName("脚本内容是 BASE64 时校验通过")
    void givenBase64ScriptContentThenPass() {
        V4FastExecuteScriptRequest request = new V4FastExecuteScriptRequest();
        request.setContent(Base64Util.encodeContentToStr("echo 1"));

        assertThat(validator.validateProperty(request, "content",
            ValidationGroups.Script.ScriptContent.class)).isEmpty();
    }
}
