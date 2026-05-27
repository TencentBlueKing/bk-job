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

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 {@link CryptoConfigService#hasHistoricalPasswordsForRotation()} 的短路判定：
 * 仅当存在不同于主密钥的历史密码时才返回 true。
 */
class CryptoConfigServiceTest {

    private static final String ACTIVE_KEY = "active-master-key-2026";
    private static final String OLD_KEY_1 = "legacy-master-key-001";

    private static CryptoConfigService buildService(String activePassword, List<String> usedPasswords) {
        EncryptConfig config = new EncryptConfig();
        config.setType(CryptoTypeEnum.CLASSIC);
        config.setPassword(activePassword);
        config.setUsedPasswords(usedPasswords);
        config.init();
        return new CryptoConfigService(config);
    }

    @Test
    void hasHistoricalPasswordsForRotation_returnsFalse_whenUsedPasswordsNull() {
        CryptoConfigService service = buildService(ACTIVE_KEY, null);
        assertThat(service.hasHistoricalPasswordsForRotation()).isFalse();
        assertThat(service.getAllPasswordsInTryOrderForRotation()).containsExactly(ACTIVE_KEY);
    }

    @Test
    void hasHistoricalPasswordsForRotation_returnsFalse_whenUsedPasswordsEmpty() {
        CryptoConfigService service = buildService(ACTIVE_KEY, Collections.emptyList());
        assertThat(service.hasHistoricalPasswordsForRotation()).isFalse();
        assertThat(service.getAllPasswordsInTryOrderForRotation()).containsExactly(ACTIVE_KEY);
    }

    @Test
    void hasHistoricalPasswordsForRotation_returnsTrue_whenSingleDistinctHistoricalPassword() {
        CryptoConfigService service = buildService(ACTIVE_KEY, Collections.singletonList(OLD_KEY_1));
        assertThat(service.hasHistoricalPasswordsForRotation()).isTrue();
        // 期望试错顺序为 [历史密码, 主密钥]
        assertThat(service.getAllPasswordsInTryOrderForRotation()).containsExactly(OLD_KEY_1, ACTIVE_KEY);
    }

    @Test
    void hasHistoricalPasswordsForRotation_returnsTrue_whenMultipleHistoricalPasswords() {
        CryptoConfigService service = buildService(ACTIVE_KEY, Arrays.asList(OLD_KEY_1, "another-old"));
        assertThat(service.hasHistoricalPasswordsForRotation()).isTrue();
    }
}
