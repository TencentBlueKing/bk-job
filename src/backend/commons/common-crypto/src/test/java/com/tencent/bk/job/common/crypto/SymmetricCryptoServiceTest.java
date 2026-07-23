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
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 端到端验证 {@link SymmetricCryptoService} 的密码轮换试错行为：
 * 1) 新加密产物为 SDK 原生格式（不含 V2 包装）；
 * 2) 解密按主密钥→usedPasswords 降序试错；
 * 3) 所有密钥都失败时抛 {@link PasswordRotationDecryptException}；
 * 4) {@code reEncryptToActive} 能把旧密文重加密为主密钥密文。
 */
class SymmetricCryptoServiceTest {

    private static final String ACTIVE_KEY = "active-master-key-2026";
    private static final String OLD_KEY_1 = "legacy-master-key-001";
    private static final String OLD_KEY_2 = "another-historical-key";

    private CryptoConfigService cryptoConfigService;
    private SymmetricCryptoService cryptoService;
    private SymmetricCryptor rawCryptor;

    @BeforeEach
    void setUp() {
        EncryptConfig encryptConfig = new EncryptConfig();
        encryptConfig.setType(CryptoTypeEnum.CLASSIC);
        encryptConfig.setPassword(ACTIVE_KEY);
        encryptConfig.setUsedPasswords(Arrays.asList(OLD_KEY_1, OLD_KEY_2));
        encryptConfig.init();

        cryptoConfigService = new CryptoConfigService(encryptConfig);
        cryptoService = new SymmetricCryptoService(cryptoConfigService);
        rawCryptor = SymmetricCryptorFactory.getCryptor(JobCryptorNames.AES_CBC);
    }

    @Test
    void encryptUsesActiveMasterKey() {
        String cipher = cryptoService.encryptToBase64Str("hello", JobCryptorNames.AES_CBC);
        // 主密钥加密的密文，应能被主密钥直接解密；同时也能通过试错链解密
        String byActive = rawCryptor.decrypt(ACTIVE_KEY, cipher);
        assertThat(byActive).isEqualTo("hello");
    }

    @Test
    void encryptDecryptRoundTrip() {
        String plain = "the-quick-brown-fox-jumps-over-the-lazy-dog";
        String cipher = cryptoService.encryptToBase64Str(plain, JobCryptorNames.AES_CBC);
        String decrypted = cryptoService.decrypt(cipher, JobCryptorNames.AES_CBC);
        assertThat(decrypted).isEqualTo(plain);
    }

    @Test
    void decryptHandlesCipherFromUsedPasswordIndex0() {
        // 用 OLD_KEY_1 直接通过 SDK 加密，试错链应在 usedPasswords[0] 处命中
        String legacyCipher = rawCryptor.encrypt(OLD_KEY_1, "legacy-plaintext");
        String decrypted = cryptoService.decrypt(legacyCipher, JobCryptorNames.AES_CBC);
        assertThat(decrypted).isEqualTo("legacy-plaintext");
    }

    @Test
    void decryptHandlesCipherFromUsedPasswordIndex1() {
        String legacyCipher = rawCryptor.encrypt(OLD_KEY_2, "another-legacy");
        String decrypted = cryptoService.decrypt(legacyCipher, JobCryptorNames.AES_CBC);
        assertThat(decrypted).isEqualTo("another-legacy");
    }

    @Test
    void decryptUnknownKeyCipherDoesNotReturnOriginalPlaintext() {
        // 用一个未配置的密钥加密，所有已知密钥都无法真正还原出原始明文。
        //
        // 注意：AES/CBC/PKCS5Padding 用错误密钥解密时并不保证一定抛异常 ——
        // 只要解密后最后一字节碰巧是有效 PKCS5 padding（约 1/256 概率），
        // doFinal 就会成功返回一段"垃圾明文"。试错链上有 N 把密钥时，
        // 全部失败（即用例通过）的概率约为 (1 - 1/256)^N，因此原先"必须抛异常"的
        // 断言是概率性 flaky：单跑经常通过，testAll 大量运行时偶发失败。
        //
        // 这里放宽为"要么抛 PasswordRotationDecryptException，要么解出的字符串不等于原文"，
        // 与业务语义一致（错误密钥不可能还原原始敏感数据），且是确定性断言。
        String plain = "secret";
        String unknownCipher = rawCryptor.encrypt("totally-unknown-key", plain);
        try {
            String decrypted = cryptoService.decrypt(unknownCipher, JobCryptorNames.AES_CBC);
            assertThat(decrypted).isNotEqualTo(plain);
        } catch (PasswordRotationDecryptException e) {
            assertThat(e).hasMessageContaining("any configured key");
        }
    }

    @Test
    void decryptCorruptedCipherDoesNotReturnOriginalPlaintext() {
        // 同上：损坏后的密文用任一密钥解密时，仍有极小概率碰巧通过 PKCS5 padding 校验，
        // 因此断言语义放宽为"要么抛异常，要么解出的字符串不等于原文"。
        String plain = "data";
        String valid = cryptoService.encryptToBase64Str(plain, JobCryptorNames.AES_CBC);
        String corrupted = valid.substring(0, valid.length() - 4) + "@@@@";
        try {
            String decrypted = cryptoService.decrypt(corrupted, JobCryptorNames.AES_CBC);
            assertThat(decrypted).isNotEqualTo(plain);
        } catch (PasswordRotationDecryptException e) {
            // 预期路径：所有密钥都无法解密
        }
    }

    @Test
    void emptyOrNullCipherReturnedAsIs() {
        assertThat(cryptoService.decrypt(null, JobCryptorNames.AES_CBC)).isNull();
        assertThat(cryptoService.decrypt("", JobCryptorNames.AES_CBC)).isEmpty();
    }

    @Test
    void reEncryptToActiveProducesActiveKeyCipherWithSamePlaintext() {
        String legacyCipher = rawCryptor.encrypt(OLD_KEY_1, "to-be-rotated");
        String newCipher = cryptoService.reEncryptToActive(legacyCipher);

        assertThat(encryptedWithActiveKey(newCipher)).isTrue();
        assertThat(cryptoService.decrypt(newCipher, JobCryptorNames.AES_CBC)).isEqualTo("to-be-rotated");
        // 重加密后已无法用旧密钥还原出原始明文。
        // 注意：AES/CBC/PKCS5Padding 用错误密钥解密不保证一定抛异常（约 1/256 概率碰巧通过 padding 校验），
        // 因此这里断言"要么抛异常，要么解出乱码"，避免概率性 flaky。
        try {
            String decryptedByOldKey = rawCryptor.decrypt(OLD_KEY_1, newCipher);
            assertThat(decryptedByOldKey).isNotEqualTo("to-be-rotated");
        } catch (RuntimeException e) {
            // 预期路径：旧密钥解不了主密钥加密的密文
        }
    }

    @Test
    void reEncryptToActiveStillSucceedsWhenAlreadyActive() {
        String first = cryptoService.encryptToBase64Str("idempotent", JobCryptorNames.AES_CBC);
        String second = cryptoService.reEncryptToActive(first);
        // 因为 AES/CBC 每次加密会用新 IV，re-encrypt 后密文不一定相等，
        // 但都能被主密钥解密回到原文
        assertThat(encryptedWithActiveKey(second)).isTrue();
        assertThat(cryptoService.decrypt(second, JobCryptorNames.AES_CBC)).isEqualTo("idempotent");
    }

    @Test
    void reEncryptEmptyOrNullReturnsAsIs() {
        assertThat(cryptoService.reEncryptToActive(null)).isNull();
        assertThat(cryptoService.reEncryptToActive("")).isEmpty();
    }

    @Test
    void cryptoConfigServiceWithoutHistoricalPasswordsStillWorks() {
        EncryptConfig encryptConfig = new EncryptConfig();
        encryptConfig.setType(CryptoTypeEnum.CLASSIC);
        encryptConfig.setPassword("only-active");
        encryptConfig.setUsedPasswords(Collections.emptyList());
        encryptConfig.init();
        CryptoConfigService svc = new CryptoConfigService(encryptConfig);

        SymmetricCryptoService cs = new SymmetricCryptoService(svc);
        String cipher = cs.encryptToBase64Str("plain", JobCryptorNames.AES_CBC);
        assertThat(cs.decrypt(cipher, JobCryptorNames.AES_CBC)).isEqualTo("plain");
        assertThat(svc.getAllPasswordsInTryOrder()).containsExactly("only-active");
    }

    @Test
    void allPasswordsInTryOrderContainsActiveFirstThenUsedInDescendingOrder() {
        assertThat(cryptoConfigService.getAllPasswordsInTryOrder())
            .containsExactly(ACTIVE_KEY, OLD_KEY_2, OLD_KEY_1);
    }

    @Test
    void allPasswordsInTryOrderForRotationPutsLastUsedFirstAndActiveLast() {
        // 迁移场景：上一次使用的密码（usedPasswords 末项 OLD_KEY_2）优先；
        // 然后是更早的历史密码；主密钥放在最后兜底
        assertThat(cryptoConfigService.getAllPasswordsInTryOrderForRotation())
            .containsExactly(OLD_KEY_2, OLD_KEY_1, ACTIVE_KEY);
    }

    @Test
    void reEncryptToActiveForRotationDecryptsWithLastUsedPasswordThenReEncryptsToActive() {
        // 用 OLD_KEY_2（即"上一次使用的密码"）加密的密文，应能被迁移版本解密 + 用主密钥重新加密
        String legacyCipher = rawCryptor.encrypt(OLD_KEY_2, "rotate-me");
        String newCipher = cryptoService.reEncryptToActiveForRotation(legacyCipher);

        assertThat(encryptedWithActiveKey(newCipher)).isTrue();
        assertThat(cryptoService.decrypt(newCipher, JobCryptorNames.AES_CBC)).isEqualTo("rotate-me");
    }

    @Test
    void reEncryptToActiveForRotationFallsBackToActiveKeyForBoundaryAlreadyActiveRow() {
        // 模拟"已迁移但未记录到进度表的边界数据"：实际是主密钥加密的行
        // 由于上一次密码 / 其他历史密码都无法解密，最终由主密钥兜底解密成功
        String activeCipher = cryptoService.encryptToBase64Str("boundary-row", JobCryptorNames.AES_CBC);
        String newCipher = cryptoService.reEncryptToActiveForRotation(activeCipher);

        assertThat(encryptedWithActiveKey(newCipher)).isTrue();
        assertThat(cryptoService.decrypt(newCipher, JobCryptorNames.AES_CBC)).isEqualTo("boundary-row");
    }

    @Test
    void reEncryptToActiveForRotationRejectsUnknownKeyCipher() {
        // 未知密钥加密的密文：走试错链后，理想情况全部失败并抛 PasswordRotationDecryptException；
        // 但 AES/CBC/PKCS5Padding 有约 1/256 的概率碰巧通过 padding 校验产生乱码明文，
        // 此时 reEncryptToActiveForRotation 会用主密钥重新加密"乱码"，不抛异常。
        // 因此这里放宽为"要么抛异常，要么重加密后的密文用主密钥解不出原文"。
        String plain = "x";
        String unknown = rawCryptor.encrypt("nobody-knows-this-key", plain);
        try {
            String newCipher = cryptoService.reEncryptToActiveForRotation(unknown);
            assertThat(cryptoService.decrypt(newCipher, JobCryptorNames.AES_CBC)).isNotEqualTo(plain);
        } catch (PasswordRotationDecryptException e) {
            // 预期路径：所有密钥都无法解密
        }
    }

    /**
     * 测试断言专用：判定密文是否能被当前主密钥<strong>单次</strong>解密成功
     * （等价于"已使用主密钥加密"）。
     *
     * <p>不能用 {@code cryptoService.decrypt(...)} 替代，因为后者会走主密钥→历史密码的
     * 试错链，旧密钥加密的密文也能解密成功，无法用来判定是否"已是主密钥加密"。
     */
    private boolean encryptedWithActiveKey(String cipher) {
        if (StringUtils.isEmpty(cipher)) {
            return true;
        }
        try {
            rawCryptor.decrypt(ACTIVE_KEY, cipher);
            return true;
        } catch (CryptoException e) {
            return false;
        }
    }
}
