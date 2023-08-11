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

package com.tencent.bk.job.common.crypto;

import com.tencent.bk.sdk.crypto.cryptor.consts.CryptorNames;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 加密配置服务
 */
@SuppressWarnings("unused")
@Slf4j
public class CryptoConfigService {

    private final EncryptConfig encryptConfig;
    private final Map<String, String> scenarioAlgorithms;

    public CryptoConfigService(EncryptConfig encryptConfig) {
        this.encryptConfig = encryptConfig;
        if (encryptConfig != null) {
            this.scenarioAlgorithms = trimKeyValues(encryptConfig.getScenarioAlgorithms());
        } else {
            this.scenarioAlgorithms = null;
        }
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
        return getDefaultSymmetricAlgorithm();
    }

    private String getDefaultSymmetricAlgorithm() {
        if (encryptConfig.getType() == CryptoTypeEnum.SHANGMI) {
            return CryptorNames.SM4;
        }
        return JobCryptorNames.AES_CBC;
    }
}
