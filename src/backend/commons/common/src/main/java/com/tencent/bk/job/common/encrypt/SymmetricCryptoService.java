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

import com.tencent.bk.sdk.crypto.cryptor.CryptorMetaDefinition;
import com.tencent.bk.sdk.crypto.cryptor.SymmetricCryptor;
import com.tencent.bk.sdk.crypto.cryptor.SymmetricCryptorFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 对称加密服务
 */
@SuppressWarnings("unused")
@Slf4j
@Service
public class SymmetricCryptoService {

    private final CryptoConfigService cryptoConfigService;
    private final Map<String, SymmetricCryptor> cryptorMap = new HashMap<>();

    @Autowired
    public SymmetricCryptoService(CryptoConfigService cryptoConfigService) {
        this.cryptoConfigService = cryptoConfigService;
    }

    /**
     * 从密文的前缀元数据中解析出使用的加密算法名称
     *
     * @param cipher 密文
     * @return 加密算法名称，如果密文不包含指定前缀的元数据则返回null
     */
    public String getAlgorithmFromCipher(String cipher) {
        String prefix = CryptorMetaDefinition.getCipherMetaPrefix();
        if (cipher.startsWith(prefix)) {
            int indexOfPrefixLastChar = cipher.indexOf(CryptorMetaDefinition.getCipherMetaSuffix());
            if (indexOfPrefixLastChar < 0) {
                return null;
            }
            return cipher.substring(prefix.length(), indexOfPrefixLastChar);
        }
        return null;
    }

    /**
     * 从密文的前缀元数据中解析出使用的加密算法名称
     *
     * @param cipherIns 密文输入流
     * @return 加密算法名称，如果密文不包含指定前缀的元数据则返回null
     */
    public String getAlgorithmFromCipherStream(BufferedInputStream cipherIns) {
        String prefix = CryptorMetaDefinition.getCipherMetaPrefix();
        String suffix = CryptorMetaDefinition.getCipherMetaSuffix();
        int algorithmMaxLength = 100;
        int cipherMetaMaxLength = prefix.length() + suffix.length() + algorithmMaxLength;
        cipherIns.mark(cipherMetaMaxLength);
        byte[] realPrefixBytes = new byte[prefix.length()];
        try {
            int n = cipherIns.read(realPrefixBytes);
            if (n < prefix.length()) {
                log.info("Cannot find enough cipherMetaPrefix bytes: expected={}, actually={}", prefix.length(), n);
                return null;
            }
            if (!Arrays.equals(realPrefixBytes, prefix.getBytes())) {
                log.info(
                    "Cannot find cipherMetaPrefix: expected={}, actually={}",
                    Arrays.toString(prefix.getBytes()),
                    Arrays.toString(realPrefixBytes)
                );
                return null;
            }
            byte[] algorithmWithSuffixBytes = new byte[algorithmMaxLength + suffix.length()];
            n = cipherIns.read(algorithmWithSuffixBytes);
            String algorithmWithSuffix = new String(algorithmWithSuffixBytes);
            int indexOfSuffix = algorithmWithSuffix.indexOf(suffix);
            if (indexOfSuffix == -1) {
                log.info(
                    "Cannot find cipherMetaSuffix: algorithmWithSuffixBytes={}, suffixBytes={}",
                    Arrays.toString(algorithmWithSuffixBytes),
                    suffix.getBytes()
                );
                return null;
            }
            return algorithmWithSuffix.substring(0, indexOfSuffix);
        } catch (Exception e) {
            log.warn("Fail to read cipherMetaPrefix from cipherIns", e);
            return null;
        } finally {
            try {
                cipherIns.reset();
            } catch (IOException e) {
                log.error("Fail to reset cipherIns", e);
            }
        }
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
