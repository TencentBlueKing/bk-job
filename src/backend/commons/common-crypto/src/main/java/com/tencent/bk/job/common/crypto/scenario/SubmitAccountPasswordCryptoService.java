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
import com.tencent.bk.sdk.crypto.exception.CryptoException;
import com.tencent.bk.sdk.crypto.exception.SM2DecryptException;
import com.tencent.bk.sdk.crypto.util.Base64Util;
import com.tencent.bk.sdk.crypto.util.SM2Util;
import com.tencent.kona.crypto.KonaCryptoProvider;
import com.tencent.kona.crypto.spec.SM2PrivateKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.slf4j.helpers.MessageFormatter;

import java.io.IOException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Security;

/**
 * 前端给后端提交账号的密码解密
 */
@Slf4j
public class SubmitAccountPasswordCryptoService {

    private final ASymmetricCryptoService aSymmetricCryptoService;
    private final CryptoConfigService cryptoConfigService;

    public SubmitAccountPasswordCryptoService(ASymmetricCryptoService aSymmetricCryptoService,
                                              CryptoConfigService cryptoConfigService) {
        this.aSymmetricCryptoService = aSymmetricCryptoService;
        this.cryptoConfigService = cryptoConfigService;
        KonaCryptoProvider konaCryptoProvider = new KonaCryptoProvider();
        if (null == Security.getProvider(konaCryptoProvider.getName())) {
            Security.addProvider(konaCryptoProvider);
        }
    }

    /**
     * 获取SM2的pem公钥
     */
    public String getPemPublicKey() {
       return sm2RawPublicKeyToPem(cryptoConfigService.getSm2PublicKey());
    }

    /**
     * 获取加密算法名称
     */
    public String getAlgorithm() {
        return cryptoConfigService.getSymmetricAlgorithmByScenario(CryptoScenarioEnum.SUBMIT_ACCOUNT_PASSWORD);
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
            algorithm = cryptoConfigService.getSymmetricAlgorithmByScenario(CryptoScenarioEnum.SUBMIT_ACCOUNT_PASSWORD);
        }
        // 暂时只支持SM2解密
        if (!CryptorNames.SM2.equals(algorithm)) {
            String msg = MessageFormatter.format(
                "Decrypt fail, only supports SM2 algorithm, algorithm: {}",
                algorithm
            ).getMessage();
            log.warn(msg);
            throw new CryptoException(msg);
        }

        PrivateKey privateKey = loadSM2PrivateKey(cryptoConfigService.getSm2PrivateKey());
        return aSymmetricCryptoService.decrypt(privateKey, encryptedData, CryptoScenarioEnum.SUBMIT_ACCOUNT_PASSWORD);
    }

    /**
     * SM2私钥字符串 -> SM2 PrivateKey
     */
    private PrivateKey loadSM2PrivateKey(String base64PrivateKey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(CryptorNames.SM2, SM2Util.PROVIDER_NAME_KONA_CRYPTO);
            SM2PrivateKeySpec privateKeySpec = new SM2PrivateKeySpec(Base64Util.decodeContentToByte(base64PrivateKey));
            return keyFactory.generatePrivate(privateKeySpec);
        } catch (Exception e) {
            throw new SM2DecryptException("Invalid SM2 private key", e);
        }
    }

    /**
     * SM2原始公钥 -> PEM公钥
     */
    public static String sm2RawPublicKeyToPem(String rawPublicKeyBase64) {
        byte[] raw = Base64Util.decodeContentToByte(rawPublicKeyBase64);
        if (raw.length != 65 || raw[0] != 0x04) {
            throw new CryptoException("Illegal SM2 original public key.");
        }
        AlgorithmIdentifier algId = new AlgorithmIdentifier(GMObjectIdentifiers.sm2p256v1);
        SubjectPublicKeyInfo spki = new SubjectPublicKeyInfo(algId, raw);
        String base64;
        try {
            base64 = Base64Util.encodeContentToStr(spki.getEncoded());
        } catch (IOException e) {
            throw new CryptoException("SM2 curve identifier encoding error.", e);
        }
        return "-----BEGIN PUBLIC KEY-----\n"
            + base64.replaceAll("(.{64})", "$1\n")
            + "\n-----END PUBLIC KEY-----";
    }
}
