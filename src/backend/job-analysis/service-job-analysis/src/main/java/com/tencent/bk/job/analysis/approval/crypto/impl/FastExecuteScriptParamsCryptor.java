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

package com.tencent.bk.job.analysis.approval.crypto.impl;

import com.tencent.bk.job.analysis.approval.consts.ApprovalOperationTypeEnum;
import com.tencent.bk.job.analysis.approval.crypto.ApprovalDisplayMasker;
import com.tencent.bk.job.analysis.approval.crypto.ApprovalDisplayParams;
import com.tencent.bk.job.analysis.approval.crypto.ApprovalParamsCryptor;
import com.tencent.bk.job.analysis.approval.crypto.SensitiveValueCryptor;
import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.job.execute.model.esb.v3.EsbCustomHostPasswordDTO;
import com.tencent.bk.job.execute.model.esb.v4.req.V4FastExecuteScriptRequest;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 快速执行脚本的参数加解密。
 * <p>
 * 加密的只有凭据性质的字段：自定义主机密码，以及调用方声明为敏感参数（param_sensitive）的脚本参数。
 * 脚本内容不加密——它是审批要审的对象本身，加密既挡不住风险也让排查问题时看不到执行的究竟是什么；
 * 未声明敏感的脚本参数同理，是审批人判断风险的必要信息。
 */
@Service
public class FastExecuteScriptParamsCryptor implements ApprovalParamsCryptor<V4FastExecuteScriptRequest> {

    private static final String FIELD_SCRIPT_CONTENT = "script_content";

    private final SensitiveValueCryptor valueCryptor;
    private final ApprovalDisplayMasker displayMasker;

    public FastExecuteScriptParamsCryptor(SensitiveValueCryptor valueCryptor,
                                          ApprovalDisplayMasker displayMasker) {
        this.valueCryptor = valueCryptor;
        this.displayMasker = displayMasker;
    }

    @Override
    public ApprovalOperationTypeEnum getOperationType() {
        return ApprovalOperationTypeEnum.FAST_EXECUTE_SCRIPT;
    }

    @Override
    public Class<V4FastExecuteScriptRequest> getParamsClass() {
        return V4FastExecuteScriptRequest.class;
    }

    @Override
    public void encrypt(V4FastExecuteScriptRequest params) {
        if (params.isParamSensitive()) {
            params.setScriptParam(valueCryptor.encrypt(params.getScriptParam()));
        }
        forEachPassword(params, password ->
            password.setEncryptedPassword(valueCryptor.encrypt(password.getEncryptedPassword())));
    }

    @Override
    public void decrypt(V4FastExecuteScriptRequest params) {
        if (params.isParamSensitive()) {
            params.setScriptParam(valueCryptor.decrypt(params.getScriptParam()));
        }
        forEachPassword(params, password ->
            password.setEncryptedPassword(valueCryptor.decrypt(password.getEncryptedPassword())));
    }

    @Override
    public ApprovalDisplayParams desensitize(V4FastExecuteScriptRequest params) {
        ApprovalDisplayParams displayParams = ApprovalDisplayParams.of(params);

        // 脚本正文是审批人要审的对象本身，摘到单独的章节里原样展示，参数区只留指向该章节的说明
        String scriptContent = decodeBase64(params.getContent());
        if (StringUtils.isNotEmpty(scriptContent)) {
            displayParams.getPlainTextBlocks()
                .add(new ApprovalDisplayParams.PlainTextBlock(FIELD_SCRIPT_CONTENT, scriptContent));
            params.setContent(displayMasker.scriptInSection());
        }

        if (StringUtils.isNotEmpty(params.getScriptParam())) {
            params.setScriptParam(params.isParamSensitive()
                ? displayMasker.mask() : decodeBase64(params.getScriptParam()));
        }

        forEachPassword(params, password -> {
            if (StringUtils.isNotEmpty(password.getEncryptedPassword())) {
                password.setEncryptedPassword(displayMasker.passwordProvided());
            }
        });
        return displayParams;
    }

    private void forEachPassword(V4FastExecuteScriptRequest params, PasswordHandler handler) {
        List<EsbCustomHostPasswordDTO> passwords = params.getHostPasswordList();
        if (CollectionUtils.isEmpty(passwords)) {
            return;
        }
        for (EsbCustomHostPasswordDTO password : passwords) {
            if (password != null) {
                handler.handle(password);
            }
        }
    }

    /**
     * 脚本内容与脚本参数按协议是 BASE64，展示时须先解码，否则审批人看到的是一串看不出风险的乱码
     */
    private String decodeBase64(String value) {
        if (StringUtils.isEmpty(value)) {
            return value;
        }
        String decoded = Base64Util.decodeContentToStrStrictly(value);
        // 解不出可读文本说明调用方传的就不是 BASE64，原样展示：展示原值好过让审批人看到一串乱码
        return decoded == null ? value : decoded;
    }

    private interface PasswordHandler {
        void handle(EsbCustomHostPasswordDTO password);
    }
}
