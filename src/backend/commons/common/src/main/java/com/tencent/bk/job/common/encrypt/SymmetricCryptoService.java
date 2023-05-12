/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.job.common.encrypt;

import com.google.common.base.Charsets;
import com.tencent.bk.job.common.util.Base64Util;
import com.tencent.bk.sdk.gm.cryptor.Cryptor;
import com.tencent.bk.sdk.gm.cryptor.CryptorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 对称加密服务
 */
@SuppressWarnings("unused")
@Slf4j
@Service
public class SymmetricCryptoService {

    private final Map<String, Cryptor> cryptorMap = new HashMap<>();

    @Value("${job.encrypt.password:}")
    private String encryptPassword;

    @Value("${job.encrypt.default-symmetric-algorithm:None}")
    private String defaultSymmetricAlgorithm;

    /**
     * 对明文信息加密，返回Base64编码的加密后的密文信息，使用默认加密算法
     *
     * @param message 要加密的明文信息
     * @return Base64编码的加密后的密文信息
     */
    public String encryptToBase64Str(String message) {
        return encryptToBase64Str(message, defaultSymmetricAlgorithm);
    }

    /**
     * 对明文信息加密，返回Base64编码的加密后的密文信息
     *
     * @param message   要加密的明文信息
     * @param algorithm 加密算法
     * @return Base64编码的加密后的密文信息
     */
    public String encryptToBase64Str(String message, String algorithm) {
        Cryptor cryptor = cryptorMap.computeIfAbsent(algorithm, CryptorFactory::getCryptor);
        byte[] encryptedMessage = cryptor.encrypt(
            encryptPassword.getBytes(Charsets.UTF_8),
            message.getBytes(Charsets.UTF_8)
        );
        return Base64Util.encodeContentToStr(encryptedMessage);
    }

    /**
     * 对Base64编码的加密后的密文信息解密，返回解密后的明文，使用默认加密算法
     *
     * @param base64EncryptedMessage Base64编码的加密后的密文信息
     * @return 解密后的明文信息
     */
    public String decrypt(String base64EncryptedMessage) {
        return decrypt(base64EncryptedMessage, defaultSymmetricAlgorithm);
    }

    /**
     * 对Base64编码的加密后的密文信息解密，返回解密后的明文
     *
     * @param base64EncryptedMessage Base64编码的加密后的密文信息
     * @param algorithm              加密算法
     * @return 解密后的明文信息
     */
    public String decrypt(String base64EncryptedMessage, String algorithm) {
        Cryptor cryptor = cryptorMap.computeIfAbsent(algorithm, CryptorFactory::getCryptor);
        byte[] rawEncryptedMessage = Base64Util.decodeContentToByte(base64EncryptedMessage);
        byte[] decryptedMessage = cryptor.decrypt(
            encryptPassword.getBytes(Charsets.UTF_8),
            rawEncryptedMessage
        );
        return new String(decryptedMessage, Charsets.UTF_8);
    }
}
