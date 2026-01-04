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

package com.tencent.bk.job.common.crypto;

import com.tencent.bk.sdk.crypto.cryptor.ASymmetricCryptor;
import com.tencent.bk.sdk.crypto.cryptor.ASymmetricCryptorFactory;
import lombok.extern.slf4j.Slf4j;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

/**
 * 非对称加解密服务
 */
@SuppressWarnings("unused")
@Slf4j
public class ASymmetricCryptoService {

    private final CryptoConfigService cryptoConfigService;
    private final Map<String, ASymmetricCryptor> cryptorMap = new HashMap<>();

    public ASymmetricCryptoService(CryptoConfigService cryptoConfigService) {
        this.cryptoConfigService = cryptoConfigService;
    }

    /**
     * 对明文信息非对称加密，返回Base64编码后的密文信息
     *
     * @param publicKey 公钥
     * @param message 要加密的明文信息，不可为空
     * @param cryptoScenarioEnum 加密场景
     * @return Base64编码的加密后的密文信息
     */
    public String encrypt(PublicKey publicKey,
                          String message,
                          CryptoScenarioEnum cryptoScenarioEnum) {
        String algorithm = cryptoConfigService.getSymmetricAlgorithmByScenario(cryptoScenarioEnum);
        ASymmetricCryptor cryptor = cryptorMap.computeIfAbsent(algorithm, ASymmetricCryptorFactory::getCryptor);
        return cryptor.encrypt(publicKey, message);
    }

    /**
     * 对Base64编码的加密后的密文信息解密，返回解密后的明文
     *
     * @param privateKey 私钥
     * @param base64EncryptedMessage Base64编码的加密后的密文信息，不可为空
     * @param cryptoScenarioEnum 解密场景
     * @return 解密后的明文信息
     */
    public String decrypt(PrivateKey privateKey,
                        String base64EncryptedMessage,
                        CryptoScenarioEnum cryptoScenarioEnum) {
        String algorithm = cryptoConfigService.getSymmetricAlgorithmByScenario(cryptoScenarioEnum);
        ASymmetricCryptor cryptor = cryptorMap.computeIfAbsent(algorithm, ASymmetricCryptorFactory::getCryptor);
        return cryptor.decrypt(privateKey, base64EncryptedMessage);
    }
}
