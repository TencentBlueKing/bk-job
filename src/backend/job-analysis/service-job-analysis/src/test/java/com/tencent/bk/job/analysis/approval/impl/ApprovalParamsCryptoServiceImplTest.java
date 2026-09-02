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
import com.tencent.bk.job.analysis.approval.crypto.ApprovalParamsCryptorTestSupport;
import com.tencent.bk.job.analysis.approval.crypto.ApprovalParamsCryptorRegistry;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.crontab.model.esb.v4.req.V4UpdateCronStatusRequest;
import com.tencent.bk.job.execute.model.esb.v3.EsbCustomHostPasswordDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4ExecuteJobPlanRequest;
import com.tencent.bk.job.execute.model.esb.v4.req.V4FastExecuteScriptRequest;
import com.tencent.bk.job.execute.model.esb.v4.req.V4GlobalVarDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 单元测试 - 参数快照与请求对象的互转及敏感字段加解密。
 * <p>
 * 用例刻意断言序列化出来的 JSON 文本，这样敏感字段一旦漏加密，本用例立刻失败 ——
 * 否则漏加密不会有任何报错，只会安静地把明文写进库里。
 */
class ApprovalParamsCryptoServiceImplTest {

    private static final long PLAN_ID = 100L;
    private static final String SCRIPT_CONTENT = "rm -rf /data/tmp";
    private static final String SCRIPT_PARAM = "--token=abcdefg";
    private static final String HOST_PASSWORD = "encrypted-password-blob";
    private static final String CIPHER_VAR_NAME = "db_password";
    private static final String CIPHER_VAR_VALUE = "db-root-password";
    private static final String PLAIN_VAR_NAME = "port";
    private static final String PLAIN_VAR_VALUE = "8080";

    private ApprovalParamsCryptorTestSupport support;
    private ApprovalParamsCryptoServiceImpl cryptoService;

    @BeforeEach
    void setUp() {
        support = new ApprovalParamsCryptorTestSupport();
        support.givenPlanVars(PLAN_ID,
            support.cipherVar(1L, CIPHER_VAR_NAME),
            support.stringVar(2L, PLAIN_VAR_NAME));
        cryptoService = new ApprovalParamsCryptoServiceImpl(
            new ApprovalParamsCryptorRegistry(support.allCryptors()));
    }

    @Test
    @DisplayName("快速执行脚本：敏感的脚本参数与自定义主机密码不以明文落库，且可原样还原")
    void givenFastExecuteScriptThenEncryptSensitiveFields() {
        V4FastExecuteScriptRequest request = buildFastExecuteScriptRequest();
        request.setParamSensitive(true);

        String snapshot = cryptoService.encryptToSnapshot(ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT, request);

        assertThat(snapshot).doesNotContain(SCRIPT_PARAM);
        assertThat(snapshot).doesNotContain(HOST_PASSWORD);
        // 非敏感字段原样保留，否则放行时执行不出正确结果
        assertThat(snapshot).contains("test-task");

        V4FastExecuteScriptRequest restored = (V4FastExecuteScriptRequest) cryptoService.decryptFromSnapshot(
            ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT, snapshot);
        assertThat(restored.getScriptParam()).isEqualTo(SCRIPT_PARAM);
        assertThat(restored.getHostPasswordList().get(0).getEncryptedPassword()).isEqualTo(HOST_PASSWORD);
        assertThat(restored.getName()).isEqualTo("test-task");
    }

    @Test
    @DisplayName("脚本内容不加密：它是审批要审的对象，加密只会让人看不到执行的究竟是什么")
    void givenFastExecuteScriptThenScriptContentNotEncrypted() {
        V4FastExecuteScriptRequest request = buildFastExecuteScriptRequest();
        request.setParamSensitive(true);

        String snapshot = cryptoService.encryptToSnapshot(ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT, request);

        assertThat(snapshot).contains(SCRIPT_CONTENT);

        V4FastExecuteScriptRequest restored = (V4FastExecuteScriptRequest) cryptoService.decryptFromSnapshot(
            ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT, snapshot);
        assertThat(restored.getContent()).isEqualTo(SCRIPT_CONTENT);
    }

    @Test
    @DisplayName("未声明为敏感参数的脚本参数不加密，审批人要靠它判断风险")
    void givenNonSensitiveScriptParamThenNotEncrypted() {
        V4FastExecuteScriptRequest request = buildFastExecuteScriptRequest();

        String snapshot = cryptoService.encryptToSnapshot(ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT, request);

        assertThat(snapshot).contains(SCRIPT_PARAM);
        // 主机密码与脚本参数是否敏感无关，任何情况下都不落明文
        assertThat(snapshot).doesNotContain(HOST_PASSWORD);

        V4FastExecuteScriptRequest restored = (V4FastExecuteScriptRequest) cryptoService.decryptFromSnapshot(
            ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT, snapshot);
        assertThat(restored.getScriptParam()).isEqualTo(SCRIPT_PARAM);
        assertThat(restored.getHostPasswordList().get(0).getEncryptedPassword()).isEqualTo(HOST_PASSWORD);
    }

    @Test
    @DisplayName("加密不改动调用方传入的请求对象，否则后续预检拿到的就是密文")
    void givenEncryptThenSourceParamsUntouched() {
        V4FastExecuteScriptRequest request = buildFastExecuteScriptRequest();
        request.setParamSensitive(true);

        cryptoService.encryptToSnapshot(ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT, request);

        assertThat(request.getContent()).isEqualTo(SCRIPT_CONTENT);
        assertThat(request.getScriptParam()).isEqualTo(SCRIPT_PARAM);
        assertThat(request.getHostPasswordList().get(0).getEncryptedPassword()).isEqualTo(HOST_PASSWORD);
    }

    @Test
    @DisplayName("启动执行方案：只有密文类型的全局变量取值被加密，普通变量原样落库")
    void givenExecuteJobPlanThenEncryptCipherVarOnly() {
        String snapshot = cryptoService.encryptToSnapshot(
            ApprovalOperationTypeEnum.EXECUTE_JOB_PLAN, buildExecuteJobPlanRequest());

        assertThat(snapshot).doesNotContain(CIPHER_VAR_VALUE);
        assertThat(snapshot).contains(PLAIN_VAR_VALUE);

        V4ExecuteJobPlanRequest restored = (V4ExecuteJobPlanRequest) cryptoService.decryptFromSnapshot(
            ApprovalOperationTypeEnum.EXECUTE_JOB_PLAN, snapshot);
        assertThat(restored.getGlobalVars().get(0).getValue()).isEqualTo(CIPHER_VAR_VALUE);
        assertThat(restored.getGlobalVars().get(1).getValue()).isEqualTo(PLAIN_VAR_VALUE);
    }

    @Test
    @DisplayName("审批期间变量由密文类型改为普通类型时仍然解密，否则会把密文当取值下发执行")
    void givenVarTypeChangedToPlainThenStillDecrypt() {
        String snapshot = cryptoService.encryptToSnapshot(
            ApprovalOperationTypeEnum.EXECUTE_JOB_PLAN, buildExecuteJobPlanRequest());
        support.givenPlanVars(PLAN_ID,
            support.stringVar(1L, CIPHER_VAR_NAME),
            support.stringVar(2L, PLAIN_VAR_NAME));

        V4ExecuteJobPlanRequest restored = (V4ExecuteJobPlanRequest) cryptoService.decryptFromSnapshot(
            ApprovalOperationTypeEnum.EXECUTE_JOB_PLAN, snapshot);

        assertThat(restored.getGlobalVars().get(0).getValue()).isEqualTo(CIPHER_VAR_VALUE);
    }

    @Test
    @DisplayName("审批期间变量由普通类型改为密文类型时原样保留，不去解密一段明文")
    void givenVarTypeChangedToCipherThenKeepPlainValue() {
        String snapshot = cryptoService.encryptToSnapshot(
            ApprovalOperationTypeEnum.EXECUTE_JOB_PLAN, buildExecuteJobPlanRequest());
        support.givenPlanVars(PLAN_ID,
            support.cipherVar(1L, CIPHER_VAR_NAME),
            support.cipherVar(2L, PLAIN_VAR_NAME));

        V4ExecuteJobPlanRequest restored = (V4ExecuteJobPlanRequest) cryptoService.decryptFromSnapshot(
            ApprovalOperationTypeEnum.EXECUTE_JOB_PLAN, snapshot);

        assertThat(restored.getGlobalVars().get(1).getValue()).isEqualTo(PLAIN_VAR_VALUE);
    }

    @Test
    @DisplayName("变量在执行方案里查不到时按需要加密处理，宁可多加密也不能漏掉密码类变量")
    void givenUnknownVarThenEncrypt() {
        V4ExecuteJobPlanRequest request = new V4ExecuteJobPlanRequest();
        request.setPlanId(PLAN_ID);
        V4GlobalVarDTO unknown = new V4GlobalVarDTO();
        unknown.setName("not_in_plan");
        unknown.setValue(CIPHER_VAR_VALUE);
        request.setGlobalVars(Collections.singletonList(unknown));

        String snapshot = cryptoService.encryptToSnapshot(ApprovalOperationTypeEnum.EXECUTE_JOB_PLAN, request);

        assertThat(snapshot).doesNotContain(CIPHER_VAR_VALUE);
    }

    @Test
    @DisplayName("无敏感字段的操作类型：快照与原请求等价")
    void givenOperationWithoutSensitiveFieldThenSnapshotUnchanged() {
        V4UpdateCronStatusRequest request = new V4UpdateCronStatusRequest();
        request.setId(1L);
        request.setStatus(1);

        String snapshot = cryptoService.encryptToSnapshot(ApprovalOperationTypeEnum.UPDATE_CRON_STATUS, request);

        assertThat(snapshot).isEqualTo(JsonUtils.toJson(request));
        V4UpdateCronStatusRequest restored = (V4UpdateCronStatusRequest) cryptoService.decryptFromSnapshot(
            ApprovalOperationTypeEnum.UPDATE_CRON_STATUS, snapshot);
        assertThat(restored.getId()).isEqualTo(1L);
        assertThat(restored.getStatus()).isEqualTo(1);
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

    private V4ExecuteJobPlanRequest buildExecuteJobPlanRequest() {
        V4ExecuteJobPlanRequest request = new V4ExecuteJobPlanRequest();
        request.setPlanId(PLAN_ID);
        V4GlobalVarDTO cipherVar = new V4GlobalVarDTO();
        cipherVar.setId(1L);
        cipherVar.setName(CIPHER_VAR_NAME);
        cipherVar.setValue(CIPHER_VAR_VALUE);
        V4GlobalVarDTO plainVar = new V4GlobalVarDTO();
        plainVar.setId(2L);
        plainVar.setName(PLAIN_VAR_NAME);
        plainVar.setValue(PLAIN_VAR_VALUE);
        request.setGlobalVars(Arrays.asList(cipherVar, plainVar));
        return request;
    }
}
