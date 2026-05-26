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
import com.tencent.bk.sdk.crypto.exception.CryptoException;
import com.tencent.bk.sdk.crypto.util.CryptorMetaUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对称加密服务
 *
 * <p>本类的语义在引入密码轮换后扩展为：
 * <ul>
 *     <li><b>加密</b>：始终使用 {@link CryptoConfigService#getSymmetricPassword()} 主密钥；
 *         密文格式保持 SDK 原生输出，不引入版本号前缀；</li>
 *     <li><b>解密</b>：按 {@link CryptoConfigService#getAllPasswordsInTryOrder()} 顺序依次试错，
 *         主密钥优先，失败后再试 usedPasswords；全部失败抛 {@link PasswordRotationDecryptException}。</li>
 * </ul>
 * <p>
 */
@SuppressWarnings("unused")
@Slf4j
public class SymmetricCryptoService {

    private final CryptoConfigService cryptoConfigService;
    private final Map<String, SymmetricCryptor> cryptorMap = new ConcurrentHashMap<>();

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
     * 对Base64编码的加密后的密文信息解密，返回解密后的明文，密文为空则原样返回。
     *
     * <p>按主密钥→usedPasswords 顺序依次试错解密；
     * 全部失败抛 {@link PasswordRotationDecryptException}。
     *
     * @param base64EncryptedMessage Base64编码的加密后的密文信息
     * @param algorithm              加密算法，不可为空
     * @return 解密后的明文信息
     */
    public String decrypt(String base64EncryptedMessage, String algorithm) {
        return decryptWithTryOrder(base64EncryptedMessage, algorithm,
            cryptoConfigService.getAllPasswordsInTryOrder());
    }

    /**
     * 密码轮换迁移场景的解密：先用上一次密码（{@code usedPasswords} 末项）试错，
     * 失败再依次试更早的历史密码，最后用主密钥兜底。
     *
     * <p>设计意图：迁移由进度表支持断点续处理，待迁移行大概率仍是上一次密码加密，
     * 上一次密码优先可一次命中；主密钥兜底用于"已迁移完成但未记录到进度表的边界数据"。
     *
     * @param base64EncryptedMessage Base64编码的密文，为空则原样返回
     * @param algorithm              加密算法，不可为空
     * @return 解密后的明文
     */
    public String decryptForRotation(String base64EncryptedMessage, String algorithm) {
        return decryptWithTryOrder(base64EncryptedMessage, algorithm,
            cryptoConfigService.getAllPasswordsInTryOrderForRotation());
    }

    /**
     * 通用试错解密：按给定的密钥试错链依次解密，全部失败抛 {@link PasswordRotationDecryptException}。
     * 试错链为空时退化为主密钥单次解密。
     */
    private String decryptWithTryOrder(String base64EncryptedMessage,
                                       String algorithm,
                                       List<String> tryOrder) {
        assert StringUtils.isNotEmpty(algorithm);
        if (StringUtils.isEmpty(base64EncryptedMessage)) {
            return base64EncryptedMessage;
        }
        SymmetricCryptor cryptor = cryptorMap.computeIfAbsent(algorithm, SymmetricCryptorFactory::getCryptor);
        if (tryOrder == null || tryOrder.isEmpty()) {
            // 兜底：试错链未初始化时，直接使用主密钥
            return cryptor.decrypt(cryptoConfigService.getSymmetricPassword(), base64EncryptedMessage);
        }
        CryptoException lastException = null;
        for (int i = 0; i < tryOrder.size(); i++) {
            String candidate = tryOrder.get(i);
            try {
                String plain = cryptor.decrypt(candidate, base64EncryptedMessage);
                if (i > 0 && log.isDebugEnabled()) {
                    log.debug("Cipher decrypted with try-order index {} (algorithm={})", i, algorithm);
                }
                return plain;
            } catch (CryptoException e) {
                lastException = e;
            }
        }
        throw new PasswordRotationDecryptException(
            "Failed to decrypt cipher with any configured key (tried " + tryOrder.size()
                + " keys). The cipher may be encrypted by an unknown historical password,"
                + " or the data is corrupted.",
            lastException
        );
    }

    /**
     * 解密给定密文后，用当前主密钥重新加密并返回（常规场景）。
     *
     * <p>解密试错顺序：主密钥 → usedPasswords 降序（与 {@link #decrypt(String, String)} 一致）。
     * 密码轮换迁移场景应使用 {@link #reEncryptToActiveForRotation(String)} —— 后者把"上一次密码"
     * 放在试错链最前面，主密钥末位兜底，命中概率与性能更优。
     *
     * @param cipher 旧密钥加密的密文
     * @return 用主密钥重新加密后的密文
     * @throws PasswordRotationDecryptException 若所有已知密钥都无法解密
     */
    public String reEncryptToActive(String cipher) {
        if (StringUtils.isEmpty(cipher)) {
            return cipher;
        }
        String algorithm = resolveAlgorithm(cipher);
        if (StringUtils.isEmpty(algorithm)) {
            algorithm = cryptoConfigService.getSymmetricAlgorithmByScenario(null);
        }
        String plain = decrypt(cipher, algorithm);
        return encryptToBase64Str(plain, algorithm);
    }

    /**
     * 密码轮换迁移场景的"按需重加密"：解密时优先使用上一次密码，再用主密钥兜底；
     * 解密成功后统一用主密钥加密返回。
     *
     * <p>与 {@link #reEncryptToActive(String)} 的差别仅在解密试错顺序：
     * 本方法把"上一次密码"放在试错链最前面（迁移时大概率一次命中），主密钥放在末尾兜底。
     *
     * @param cipher 旧密钥加密的密文
     * @return 用主密钥重新加密后的密文
     * @throws PasswordRotationDecryptException 若所有已知密钥都无法解密
     */
    public String reEncryptToActiveForRotation(String cipher) {
        if (StringUtils.isEmpty(cipher)) {
            return cipher;
        }
        String algorithm = resolveAlgorithm(cipher);
        if (StringUtils.isEmpty(algorithm)) {
            algorithm = cryptoConfigService.getSymmetricAlgorithmByScenario(null);
        }
        String plain = decryptForRotation(cipher, algorithm);
        return encryptToBase64Str(plain, algorithm);
    }

    /**
     * 解析密文以决定使用的算法：先从 SDK 原生前缀读取，读不到则返回 null。
     */
    private String resolveAlgorithm(String cipher) {
        return CryptorMetaUtil.getCryptorNameFromCipher(cipher);
    }
}
