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

import com.tencent.bk.sdk.crypto.cryptor.SymmetricCryptor;
import com.tencent.bk.sdk.crypto.cryptor.SymmetricCryptorFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 对称加密服务
 */
@SuppressWarnings("unused")
@Slf4j
public class SymmetricCryptoService {

    private final CryptoConfigService cryptoConfigService;
    private final Map<String, SymmetricCryptor> cryptorMap = new HashMap<>();

    public SymmetricCryptoService(CryptoConfigService cryptoConfigService) {
        this.cryptoConfigService = cryptoConfigService;
    }

    /**
     * 对流数据加密，加密后的数据写入输出流中
     *
     * @param key                密钥
     * @param in                 输入流
     * @param out                输出流
     * @param cryptoScenarioEnum 加密场景
     */
    public void encrypt(String key,
                        InputStream in,
                        OutputStream out,
                        CryptoScenarioEnum cryptoScenarioEnum) {
        String algorithm = cryptoConfigService.getSymmetricAlgorithmByScenario(cryptoScenarioEnum);
        SymmetricCryptor cryptor = cryptorMap.computeIfAbsent(algorithm, SymmetricCryptorFactory::getCryptor);
        cryptor.encrypt(key, in, out);
    }

    /**
     * 对明文信息加密，返回Base64编码的加密后的密文信息
     *
     * @param message            要加密的明文信息，不可为空
     * @param cryptoScenarioEnum 加密场景
     * @return Base64编码的加密后的密文信息
     */
    public String encryptToBase64Str(String message, CryptoScenarioEnum cryptoScenarioEnum) {
        return encryptToBase64Str(message, cryptoConfigService.getSymmetricAlgorithmByScenario(cryptoScenarioEnum));
    }

    /**
     * 对明文信息加密，返回Base64编码的加密后的密文信息，明文为空则原样返回
     *
     * @param message   要加密的明文信息
     * @param algorithm 加密算法，不可为空
     * @return Base64编码的加密后的密文信息
     */
    public String encryptToBase64Str(String message, String algorithm) {
        assert StringUtils.isNotEmpty(algorithm);
        if (StringUtils.isEmpty(message)) {
            return message;
        }
        SymmetricCryptor cryptor = cryptorMap.computeIfAbsent(algorithm, SymmetricCryptorFactory::getCryptor);
        return cryptor.encrypt(cryptoConfigService.getSymmetricPassword(), message);
    }

    /**
     * 对流数据解密，解密后的数据写入输出流中
     *
     * @param key       密钥
     * @param in        输入流
     * @param out       输出流
     * @param algorithm 解密算法
     */
    public void decrypt(String key,
                        BufferedInputStream in,
                        OutputStream out,
                        String algorithm) {
        SymmetricCryptor cryptor = cryptorMap.computeIfAbsent(algorithm, SymmetricCryptorFactory::getCryptor);
        cryptor.decrypt(key, in, out);
    }

    /**
     * 对Base64编码的加密后的密文信息解密，返回解密后的明文，密文为空则原样返回
     *
     * @param base64EncryptedMessage Base64编码的加密后的密文信息
     * @param algorithm              加密算法，不可为空
     * @return 解密后的明文信息
     */
    public String decrypt(String base64EncryptedMessage, String algorithm) {
        assert StringUtils.isNotEmpty(algorithm);
        if (StringUtils.isEmpty(base64EncryptedMessage)) {
            return base64EncryptedMessage;
        }
        SymmetricCryptor cryptor = cryptorMap.computeIfAbsent(algorithm, SymmetricCryptorFactory::getCryptor);
        return cryptor.decrypt(
            cryptoConfigService.getSymmetricPassword(),
            base64EncryptedMessage
        );
    }
}
