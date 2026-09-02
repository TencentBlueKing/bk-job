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

package com.tencent.bk.job.analysis.approval.crypto;

import com.tencent.bk.job.analysis.approval.crypto.impl.CreateJobPlanParamsCryptor;
import com.tencent.bk.job.analysis.approval.crypto.impl.ExecuteJobPlanParamsCryptor;
import com.tencent.bk.job.analysis.approval.crypto.impl.FastExecuteScriptParamsCryptor;
import com.tencent.bk.job.analysis.approval.crypto.impl.FastTransferFileParamsCryptor;
import com.tencent.bk.job.analysis.approval.crypto.impl.SaveCronParamsCryptor;
import com.tencent.bk.job.analysis.approval.crypto.impl.UpdateCronStatusParamsCryptor;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.crypto.CryptoScenarioEnum;
import com.tencent.bk.job.common.crypto.SymmetricCryptoService;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.manage.api.inner.ServiceTaskPlanResource;
import com.tencent.bk.job.manage.api.inner.ServiceTaskTemplateResource;
import com.tencent.bk.job.manage.model.inner.ServiceTaskVariableTypeDTO;
import com.tencent.bk.sdk.crypto.cryptor.consts.CryptorNames;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 加密类单测的公共装配：桩掉对称加密与 job-manage 的变量类型查询，其余用真实实现。
 * <p>
 * 加密桩用 BASE64 替身而非"加个前缀"，这样"密文里不含明文"这类断言才有意义。
 */
public class ApprovalParamsCryptorTestSupport {

    /**
     * 与真实密文一样带 SDK 的算法名前缀，{@link SensitiveValueCryptor#isCipherText} 据此判断值是否加密过
     */
    public static final String CIPHER_PREFIX = "[Cipher:::" + CryptorNames.AES + "]";

    public static final String MASK = "******";

    private final ServiceTaskPlanResource taskPlanResource = mock(ServiceTaskPlanResource.class);
    private final ServiceTaskTemplateResource taskTemplateResource = mock(ServiceTaskTemplateResource.class);
    private final SensitiveValueCryptor valueCryptor;
    private final ApprovalDisplayMasker displayMasker;
    private final GlobalVarCryptor globalVarCryptor;
    private final CipherVarMatcherResolver matcherResolver;

    public ApprovalParamsCryptorTestSupport() {
        this.valueCryptor = new SensitiveValueCryptor(stubSymmetricCryptoService());
        this.displayMasker = new ApprovalDisplayMasker(stubI18nService());
        this.globalVarCryptor = new GlobalVarCryptor(valueCryptor, displayMasker);
        this.matcherResolver = new CipherVarMatcherResolver(taskPlanResource, taskTemplateResource);
    }

    public List<ApprovalParamsCryptor<?>> allCryptors() {
        return Arrays.asList(
            new FastExecuteScriptParamsCryptor(valueCryptor, displayMasker),
            new FastTransferFileParamsCryptor(),
            new ExecuteJobPlanParamsCryptor(globalVarCryptor, matcherResolver),
            new CreateJobPlanParamsCryptor(globalVarCryptor, matcherResolver),
            new SaveCronParamsCryptor(globalVarCryptor, matcherResolver),
            new UpdateCronStatusParamsCryptor());
    }

    public void givenPlanVars(long planId, ServiceTaskVariableTypeDTO... variables) {
        InternalResponse<List<ServiceTaskVariableTypeDTO>> resp = successResp(variables);
        when(taskPlanResource.listPlanGlobalVarTypes(planId)).thenReturn(resp);
    }

    public void givenTemplateVars(long templateId, ServiceTaskVariableTypeDTO... variables) {
        InternalResponse<List<ServiceTaskVariableTypeDTO>> resp = successResp(variables);
        when(taskTemplateResource.listTemplateGlobalVarTypes(templateId)).thenReturn(resp);
    }

    /**
     * 不走 {@code buildSuccessResp}：它要取 i18n 文案，而单测里没有 Spring 上下文
     */
    private InternalResponse<List<ServiceTaskVariableTypeDTO>> successResp(ServiceTaskVariableTypeDTO... variables) {
        InternalResponse<List<ServiceTaskVariableTypeDTO>> resp = new InternalResponse<>();
        resp.setSuccess(true);
        resp.setCode(ErrorCode.RESULT_OK);
        resp.setData(new ArrayList<>(Arrays.asList(variables)));
        return resp;
    }

    public ServiceTaskVariableTypeDTO cipherVar(Long id, String name) {
        return variable(id, name, TaskVariableTypeEnum.CIPHER);
    }

    public ServiceTaskVariableTypeDTO stringVar(Long id, String name) {
        return variable(id, name, TaskVariableTypeEnum.STRING);
    }

    public SensitiveValueCryptor getValueCryptor() {
        return valueCryptor;
    }

    private ServiceTaskVariableTypeDTO variable(Long id, String name, TaskVariableTypeEnum type) {
        ServiceTaskVariableTypeDTO variable = new ServiceTaskVariableTypeDTO();
        variable.setId(id);
        variable.setName(name);
        variable.setType(type.getType());
        return variable;
    }

    private SymmetricCryptoService stubSymmetricCryptoService() {
        SymmetricCryptoService symmetricCryptoService = mock(SymmetricCryptoService.class);
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
        return symmetricCryptoService;
    }

    /**
     * i18n 替身原样回显 key，便于断言"用了哪个文案"而不依赖具体译文
     */
    private MessageI18nService stubI18nService() {
        MessageI18nService i18nService = mock(MessageI18nService.class);
        when(i18nService.getI18n(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        return i18nService;
    }
}
