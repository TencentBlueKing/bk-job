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

import com.tencent.bk.sdk.crypto.cryptor.consts.CryptorNames;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 加密配置服务
 *
 * <p>除提供原有的密钥/算法读取能力外，为密码轮换引入试错链：
 * 加密时仅使用主密钥；解密时先尝试主密钥，再按 {@link #getAllPasswordsInTryOrder()} 中历史密码
 * 降序（{@code usedPasswords} 列表从后往前）依次试错，直到成功或全部失败。
 */
@SuppressWarnings("unused")
@Slf4j
public class CryptoConfigService {

    private final EncryptConfig encryptConfig;
    private final Map<String, String> scenarioAlgorithms;

    /**
     * 按试错顺序排列的所有密钥（主密钥在前，usedPasswords 按配置顺序的<strong>降序</strong>紧随其后）。
     * 用于解密未知由哪把密钥加密的历史密文（常规读取场景）。
     */
    @Getter
    private final List<String> allPasswordsInTryOrder;

    /**
     * 密码轮换迁移场景专用的试错顺序：
     * {@code [usedPasswords[N-1], usedPasswords[N-2], ..., usedPasswords[0], activePassword]}。
     *
     * <p>设计意图：迁移由进度表（{@code crypto_password_rotation_progress}）支持断点续处理，
     * 待迁移行大概率仍是上一次密码加密，因此优先用 {@code usedPasswords} 中最新一项试解密可以
     * 一次命中；主密钥放在<strong>末尾兜底</strong>，用于覆盖"已迁移完成但未记录到进度表的边界数据"。
     */
    @Getter
    private final List<String> allPasswordsInTryOrderForRotation;

    public CryptoConfigService(EncryptConfig encryptConfig) {
        this.encryptConfig = encryptConfig;
        if (encryptConfig != null) {
            this.scenarioAlgorithms = trimKeyValues(encryptConfig.getScenarioAlgorithms());
            this.allPasswordsInTryOrder = buildTryOrder(encryptConfig);
            this.allPasswordsInTryOrderForRotation = buildRotationTryOrder(encryptConfig);
        } else {
            this.scenarioAlgorithms = null;
            this.allPasswordsInTryOrder = Collections.emptyList();
            this.allPasswordsInTryOrderForRotation = Collections.emptyList();
        }
    }

    private static List<String> buildTryOrder(EncryptConfig encryptConfig) {
        List<String> tryOrder = new ArrayList<>();
        String activePassword = encryptConfig.getPassword();
        if (StringUtils.isNotEmpty(activePassword)) {
            tryOrder.add(activePassword);
        }
        List<String> usedPasswords = encryptConfig.getUsedPasswords();
        if (usedPasswords != null) {
            // usedPasswords 按使用先后配置；解密旧数据时按降序（最新历史密码优先）试错
            for (int i = usedPasswords.size() - 1; i >= 0; i--) {
                String used = usedPasswords.get(i);
                if (StringUtils.isNotEmpty(used) && !tryOrder.contains(used)) {
                    tryOrder.add(used);
                }
            }
        }
        return Collections.unmodifiableList(tryOrder);
    }

    /**
     * 构造迁移场景试错顺序：上一次密码（{@code usedPasswords} 末项）优先，主密钥兜底。
     */
    private static List<String> buildRotationTryOrder(EncryptConfig encryptConfig) {
        List<String> rotationOrder = new ArrayList<>();
        List<String> usedPasswords = encryptConfig.getUsedPasswords();
        if (usedPasswords != null) {
            // usedPasswords 按使用先后配置；迁移解密时按降序（上一次使用的密码优先）试错
            for (int i = usedPasswords.size() - 1; i >= 0; i--) {
                String used = usedPasswords.get(i);
                if (StringUtils.isNotEmpty(used) && !rotationOrder.contains(used)) {
                    rotationOrder.add(used);
                }
            }
        }
        String activePassword = encryptConfig.getPassword();
        // 主密钥放在末尾兜底：处理"已迁移完成但未记录到进度表的边界数据"
        if (StringUtils.isNotEmpty(activePassword) && !rotationOrder.contains(activePassword)) {
            rotationOrder.add(activePassword);
        }
        return Collections.unmodifiableList(rotationOrder);
    }

    /**
     * 是否存在可用于密码轮换迁移的"历史密钥"。
     *
     * <p>判断依据：{@link #allPasswordsInTryOrderForRotation} 在主密钥之外是否还有其他元素。
     * 由 {@link #buildRotationTryOrder(EncryptConfig)} 的构造规则可推导出，当且仅当满足以下任一情形时返回 {@code false}：
     * <ol>
     *     <li>{@code usedPasswords} 为空 / 全部为空字符串；</li>
     *     <li>{@code usedPasswords} 内所有非空项都与主密码相同（理论上已被 {@link EncryptConfig#validate()} 拦截，此处再短路兜底）。</li>
     * </ol>
     *
     * <p>用于密码轮换迁移启动前的短路判断：无历史密钥即意味着没有旧数据需要迁移，无需触发任务。
     */
    public boolean hasHistoricalPasswordsForRotation() {
        return allPasswordsInTryOrderForRotation != null && allPasswordsInTryOrderForRotation.size() > 1;
    }

    private Map<String, String> trimKeyValues(Map<String, String> map) {
        if (map == null) {
            return null;
        }
        Map<String, String> resultMap = new HashMap<>();
        map.forEach((key, value) -> {
            if (key != null) {
                key = key.trim();
            }
            if (value != null) {
                value = value.trim();
            }
            resultMap.put(key, value);
        });
        return resultMap;
    }

    /**
     * 获取对称加密密钥
     *
     * @return 对称加密密钥
     */
    public String getSymmetricPassword() {
        return encryptConfig.getPassword();
    }

    /**
     * 计算主密钥的短指纹（SHA-256 前 8 字节 hex，共 16 字符）。
     * 用于密码轮换进度表中标识"本轮迁移目标密钥"，区分不同轮次的迁移。
     * 不会暴露密钥明文。
     */
    public String computeActivePasswordFingerprint() {
        return computePasswordFingerprint(encryptConfig.getPassword());
    }

    /**
     * 计算任意密码的短指纹（SHA-256 前 8 字节 hex）
     */
    public static String computePasswordFingerprint(String password) {
        if (password == null) {
            throw new IllegalArgumentException("password must not be null");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(16);
            for (int i = 0; i < 8; i++) {
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /**
     * 获取SM2私钥字符串
     */
    public String getSm2PrivateKey() {
        return encryptConfig.getSm2PrivateKey();
    }

    /**
     * 获取SM2公钥
     * @return
     */
    public String getSm2PublicKey() {
        return encryptConfig.getSm2PublicKey();
    }

    /**
     * 根据加密场景获取需要使用的加密算法
     *
     * @param cryptoScenarioEnum 加密场景枚举值
     * @return 加密算法标识
     */
    public String getSymmetricAlgorithmByScenario(CryptoScenarioEnum cryptoScenarioEnum) {
        if (cryptoScenarioEnum == null) {
            return getDefaultSymmetricAlgorithm();
        }
        if (scenarioAlgorithms != null && scenarioAlgorithms.containsKey(cryptoScenarioEnum.getValue())) {
            return scenarioAlgorithms.get(cryptoScenarioEnum.getValue());
        }
        if (StringUtils.isNotEmpty(cryptoScenarioEnum.getAlgorithm())) {
            return cryptoScenarioEnum.getAlgorithm();
        }
        return getDefaultSymmetricAlgorithm();
    }

    private String getDefaultSymmetricAlgorithm() {
        if (encryptConfig.getType() == CryptoTypeEnum.SHANGMI) {
            return CryptorNames.SM4;
        }
        return JobCryptorNames.AES_CBC;
    }
}
