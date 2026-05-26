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

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 加密配置
 */
@ConfigurationProperties(prefix = "job.encrypt")
@Getter
@Setter
@Slf4j
public class EncryptConfig {

    private CryptoTypeEnum type;

    /**
     * 当前主密码：用于新数据加密，以及解密时的首选密码，不允许为空。
     */
    private String password;

    /**
     * 历史密码列表（按从新到旧排序）：仅用于解密历史未轮换完成的旧数据，
     * 不参与新数据的加密。可以为空，表示从未轮换过密码。
     */
    private List<String> usedPasswords = new ArrayList<>();

    private String sm2PrivateKey;

    private String sm2PublicKey;

    /**
     * 各个场景下使用的加密算法，不配置则使用默认算法
     */
    private Map<String, String> scenarioAlgorithms = new HashMap<>();

    @PostConstruct
    public void init() {
        validate();
        // 输出非敏感的诊断信息：主密码不可直接打印，仅打印是否配置 / usedPasswords 数量
        log.info(
            "EncryptConfig init: type={}, passwordConfigured={}, usedPasswordsCount={}, scenarioAlgorithms={}",
            type,
            StringUtils.isNotEmpty(password),
            usedPasswords == null ? 0 : usedPasswords.size(),
            scenarioAlgorithms
        );
    }

    /**
     * 启动期强校验：发现任何配置问题立刻 fail-fast，提示运维修复后再启动。
     */
    private void validate() {
        if (StringUtils.isEmpty(password)) {
            throw new BeanInitializationException(
                "job.encrypt.password must not be empty. Please configure a strong password (>= 16 chars recommended)"
                    + " in values.yaml before starting the service."
            );
        }
        if (usedPasswords != null) {
            for (int i = 0; i < usedPasswords.size(); i++) {
                String used = usedPasswords.get(i);
                if (StringUtils.isEmpty(used)) {
                    throw new BeanInitializationException(
                        "job.encrypt.usedPasswords[" + i + "] must not be empty"
                    );
                }
            }
            Set<String> deduplication = new HashSet<>();
            for (int i = 0; i < usedPasswords.size(); i++) {
                if (!deduplication.add(usedPasswords.get(i))) {
                    throw new BeanInitializationException(
                        "job.encrypt.usedPasswords contains duplicated entry at index " + i
                    );
                }
            }
        }
    }
}
