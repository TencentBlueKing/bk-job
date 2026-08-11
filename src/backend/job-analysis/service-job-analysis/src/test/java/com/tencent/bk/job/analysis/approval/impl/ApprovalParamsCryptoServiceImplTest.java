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

package com.tencent.bk.job.analysis.approval.impl;

import com.tencent.bk.job.analysis.approval.consts.ApprovalOperationTypeEnum;
import com.tencent.bk.job.common.crypto.CryptoScenarioEnum;
import com.tencent.bk.job.common.crypto.SymmetricCryptoService;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.crontab.model.esb.v4.req.V4UpdateCronStatusRequest;
import com.tencent.bk.job.execute.model.esb.v3.EsbCustomHostPasswordDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4ExecuteJobPlanRequest;
import com.tencent.bk.job.execute.model.esb.v4.req.V4FastExecuteScriptRequest;
import com.tencent.bk.job.execute.model.esb.v4.req.V4GlobalVarDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 单元测试 - 参数快照敏感字段加解密。
 * <p>
 * 用例刻意从真实 DTO 序列化出 JSON 再断言，这样敏感字段的 JSON 名一旦改动（改名、换 @JsonProperty），
 * 本用例立刻失败 —— 否则漏登记敏感字段不会有任何报错，只会安静地把明文写进库里。
 */
class ApprovalParamsCryptoServiceImplTest {

    private static final String CIPHER_PREFIX = "CIPHER::";
    private static final String SCRIPT_CONTENT = "rm -rf /data/tmp";
    private static final String SCRIPT_PARAM = "--token=abcdefg";
    private static final String HOST_PASSWORD = "encrypted-password-blob";
    private static final String CIPHER_VAR_VALUE = "db-root-password";

    private ApprovalParamsCryptoServiceImpl cryptoService;

    @BeforeEach
    void setUp() {
        SymmetricCryptoService symmetricCryptoService = mock(SymmetricCryptoService.class);
        // 用 Base64 替身而非"加前缀"，这样断言"密文里不含明文"才有意义
        when(symmetricCryptoService.encryptToBase64Str(anyString(),
            eq(CryptoScenarioEnum.APPROVAL_PARAMS_SNAPSHOT)))
            .thenAnswer(invocation -> CIPHER_PREFIX + Base64.getEncoder().encodeToString(
                ((String) invocation.getArgument(0)).getBytes(StandardCharsets.UTF_8)));
        when(symmetricCryptoService.decrypt(anyString(), anyString()))
            .thenAnswer(invocation -> {
                String cipher = invocation.getArgument(0);
                if (!cipher.startsWith(CIPHER_PREFIX)) {
                    return cipher;
                }
                return new String(Base64.getDecoder().decode(cipher.substring(CIPHER_PREFIX.length())),
                    StandardCharsets.UTF_8);
            });
        cryptoService = new ApprovalParamsCryptoServiceImpl(symmetricCryptoService);
    }

    @Test
    @DisplayName("快速执行脚本：脚本内容、脚本参数、自定义主机密码都不以明文落库，且可原样还原")
    void givenFastExecuteScriptThenEncryptAllSensitiveFields() {
        String plainJson = JsonUtils.toJson(buildFastExecuteScriptRequest());

        String encrypted = cryptoService.encryptSensitiveFields(
            ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT, plainJson);

        assertThat(encrypted).doesNotContain(SCRIPT_CONTENT);
        assertThat(encrypted).doesNotContain(SCRIPT_PARAM);
        assertThat(encrypted).doesNotContain(HOST_PASSWORD);
        // 非敏感字段原样保留，否则放行时执行不出正确结果
        assertThat(encrypted).contains("test-task");

        V4FastExecuteScriptRequest restored = JsonUtils.fromJson(
            cryptoService.decryptSensitiveFields(ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT, encrypted),
            V4FastExecuteScriptRequest.class);
        assertThat(restored.getContent()).isEqualTo(SCRIPT_CONTENT);
        assertThat(restored.getScriptParam()).isEqualTo(SCRIPT_PARAM);
        assertThat(restored.getHostPasswordList().get(0).getEncryptedPassword()).isEqualTo(HOST_PASSWORD);
        assertThat(restored.getName()).isEqualTo("test-task");
    }

    @Test
    @DisplayName("启动执行方案：全局变量值一律加密，不区分变量类型")
    void givenExecuteJobPlanThenEncryptGlobalVarValue() {
        V4ExecuteJobPlanRequest request = new V4ExecuteJobPlanRequest();
        request.setPlanId(100L);
        V4GlobalVarDTO globalVar = new V4GlobalVarDTO();
        globalVar.setName("db_password");
        globalVar.setValue(CIPHER_VAR_VALUE);
        request.setGlobalVars(Collections.singletonList(globalVar));

        String encrypted = cryptoService.encryptSensitiveFields(
            ApprovalOperationTypeEnum.EXECUTE_JOB_PLAN, JsonUtils.toJson(request));

        assertThat(encrypted).doesNotContain(CIPHER_VAR_VALUE);
        V4ExecuteJobPlanRequest restored = JsonUtils.fromJson(
            cryptoService.decryptSensitiveFields(ApprovalOperationTypeEnum.EXECUTE_JOB_PLAN, encrypted),
            V4ExecuteJobPlanRequest.class);
        assertThat(restored.getGlobalVars().get(0).getValue()).isEqualTo(CIPHER_VAR_VALUE);
        assertThat(restored.getGlobalVars().get(0).getName()).isEqualTo("db_password");
    }

    @Test
    @DisplayName("无敏感字段的操作类型原样返回")
    void givenOperationWithoutSensitiveFieldThenReturnAsIs() {
        V4UpdateCronStatusRequest request = new V4UpdateCronStatusRequest();
        request.setId(1L);
        request.setStatus(1);
        String plainJson = JsonUtils.toJson(request);

        String encrypted = cryptoService.encryptSensitiveFields(
            ApprovalOperationTypeEnum.UPDATE_CRON_STATUS, plainJson);

        assertThat(encrypted).isEqualTo(plainJson);
        assertThat(cryptoService.decryptSensitiveFields(
            ApprovalOperationTypeEnum.UPDATE_CRON_STATUS, encrypted)).isEqualTo(plainJson);
    }

    @Test
    @DisplayName("空字符串与空快照不做处理")
    void givenBlankParamsThenReturnAsIs() {
        assertThat(cryptoService.encryptSensitiveFields(
            ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT, null)).isNull();
        assertThat(cryptoService.encryptSensitiveFields(
            ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT, "")).isEmpty();
    }

    private V4FastExecuteScriptRequest buildFastExecuteScriptRequest() {
        V4FastExecuteScriptRequest request = new V4FastExecuteScriptRequest();
        request.setName("test-task");
        request.setContent(SCRIPT_CONTENT);
        request.setScriptParam(SCRIPT_PARAM);
        EsbCustomHostPasswordDTO hostPassword = new EsbCustomHostPasswordDTO();
        hostPassword.setHostId(1L);
        hostPassword.setEncryptedPassword(HOST_PASSWORD);
        request.setHostPasswordList(Collections.singletonList(hostPassword));
        return request;
    }
}
