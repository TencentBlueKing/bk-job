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

package com.tencent.bk.job.common.crypto.scenario;

import com.tencent.bk.job.common.crypto.ASymmetricCryptoService;
import com.tencent.bk.job.common.crypto.CryptoConfigService;
import com.tencent.bk.job.common.crypto.CryptoScenarioEnum;
import com.tencent.bk.sdk.crypto.cryptor.consts.CryptorNames;
import com.tencent.bk.sdk.crypto.exception.SM2DecryptException;
import com.tencent.bk.sdk.crypto.util.Base64Util;
import com.tencent.bk.sdk.crypto.util.SM2Util;
import com.tencent.kona.crypto.spec.SM2PrivateKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.security.KeyFactory;
import java.security.PrivateKey;

/**
 * 敏感数据解密服务
 */
@Slf4j
public class SensitiveDataCryptoService {

    private final ASymmetricCryptoService aSymmetricCryptoService;
    private final CryptoConfigService cryptoConfigService;

    public SensitiveDataCryptoService(ASymmetricCryptoService aSymmetricCryptoService,
                                      CryptoConfigService cryptoConfigService) {
        this.aSymmetricCryptoService = aSymmetricCryptoService;
        this.cryptoConfigService = cryptoConfigService;
    }

    /**
     * 获取SM2公钥字符串
     */
    public String getPublicKeyStr() {
       return cryptoConfigService.getSM2PublicKey();
    }

    /**
     * 获取加密算法名称
     * @return
     */
    public String getAlgorithm() {
        return cryptoConfigService.getSymmetricAlgorithmByScenario(CryptoScenarioEnum.SENSITIVE_DATA);
    }

    /**
     * SM2解密
     * @param algorithm 加密算法
     * @param encryptedData 密文
     * @return 解密后的明文
     */
    public String decryptIfNeeded(String algorithm, String encryptedData) {
        if (StringUtils.isEmpty(encryptedData)) {
            log.warn("EncryptedData is empty.");
            return encryptedData;
        }
        if (StringUtils.isEmpty(algorithm)) {
            algorithm = cryptoConfigService.getSymmetricAlgorithmByScenario(CryptoScenarioEnum.SENSITIVE_DATA);
        }
        // 暂时只支持SM2解密
        if (!CryptorNames.SM2.equals(algorithm)) {
            log.warn("Decrypt fail, only supports SM2 algorithm, algorithm: {}", algorithm);
            return encryptedData;
        }

        PrivateKey privateKey = loadSM2PrivateKey(cryptoConfigService.getSM2PrivateKey());
        String decrypted = aSymmetricCryptoService.decrypt(privateKey,
            encryptedData,
            CryptoScenarioEnum.SENSITIVE_DATA
        );
        return decrypted;
    }

    /**
     * SM2私钥字符串 -> SM2 PrivateKey
     */
    private PrivateKey loadSM2PrivateKey(String base64PrivateKey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(CryptorNames.SM2, SM2Util.PROVIDER_NAME_KONA_CRYPTO);
            SM2PrivateKeySpec privateKeySpec = new SM2PrivateKeySpec(Base64Util.decodeContentToByte(base64PrivateKey));
            PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
            return privateKey;
        } catch (Exception e) {
            throw new SM2DecryptException("Invalid SM2 private key", e);
        }
    }
}
