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

import com.tencent.bk.job.common.crypto.CryptoScenarioEnum;
import com.tencent.bk.job.common.crypto.SymmetricCryptoService;
import com.tencent.bk.sdk.crypto.cryptor.consts.CryptorNames;
import com.tencent.bk.sdk.crypto.util.CryptorMetaUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * 审批参数快照中单个敏感值的加解密。
 */
@Service
public class SensitiveValueCryptor {

    private final SymmetricCryptoService symmetricCryptoService;

    public SensitiveValueCryptor(SymmetricCryptoService symmetricCryptoService) {
        this.symmetricCryptoService = symmetricCryptoService;
    }

    /**
     * 加密失败让异常向上传播：发起接口报错好过把明文写进库里
     */
    public String encrypt(String plainText) {
        if (StringUtils.isEmpty(plainText)) {
            return plainText;
        }
        return symmetricCryptoService.encryptToBase64Str(plainText, CryptoScenarioEnum.APPROVAL_PARAMS_SNAPSHOT);
    }

    /**
     * 算法名取自密文自带的前缀；取不到说明这段值没被本平台加密过，按不加密（{@link CryptorNames#NONE}）原样返回
     */
    public String decrypt(String cipherText) {
        if (StringUtils.isEmpty(cipherText)) {
            return cipherText;
        }
        String algorithm = CryptorMetaUtil.getCryptorNameFromCipher(cipherText);
        return symmetricCryptoService.decrypt(cipherText,
            StringUtils.isBlank(algorithm) ? CryptorNames.NONE : algorithm);
    }

    /**
     * 值是否为本平台产出的密文。密文自带算法名前缀，据此可判断某个字段当初是否被加密过
     */
    public boolean isCipherText(String value) {
        return StringUtils.isNotEmpty(value)
            && StringUtils.isNotBlank(CryptorMetaUtil.getCryptorNameFromCipher(value));
    }
}
