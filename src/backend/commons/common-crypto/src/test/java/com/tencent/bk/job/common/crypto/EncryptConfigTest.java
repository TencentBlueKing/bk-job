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
import org.springframework.beans.factory.BeanInitializationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 验证 {@link EncryptConfig} 的启动期强校验逻辑覆盖所有失败分支并放行合法配置。
 */
class EncryptConfigTest {

    @Test
    void rejectsEmptyPassword() {
        EncryptConfig config = new EncryptConfig();
        config.setPassword("");
        assertThatThrownBy(config::init)
            .isInstanceOf(BeanInitializationException.class)
            .hasMessageContaining("must not be empty");
    }

    @Test
    void rejectsNullPassword() {
        EncryptConfig config = new EncryptConfig();
        assertThatThrownBy(config::init)
            .isInstanceOf(BeanInitializationException.class)
            .hasMessageContaining("must not be empty");
    }

    @Test
    void acceptsAnyPasswordValueWithoutHardcodedBlacklist() {
        // 校验只做结构性约束：任何非空字符串都应当被接受，不存在硬编码的密码黑名单
        EncryptConfig config = new EncryptConfig();
        config.setPassword("legacy-default-style-key");
        config.setUsedPasswords(new ArrayList<>());
        config.init();
        assertThat(config.getPassword()).isEqualTo("legacy-default-style-key");
    }

    @Test
    void rejectsEmptyUsedPasswordEntry() {
        EncryptConfig config = new EncryptConfig();
        config.setPassword("my-strong-key-001");
        config.setUsedPasswords(Arrays.asList("legacy-key-A", ""));
        assertThatThrownBy(config::init)
            .isInstanceOf(BeanInitializationException.class)
            .hasMessageContaining("usedPasswords[1]");
    }

    @Test
    void rejectsDuplicatedUsedPasswords() {
        EncryptConfig config = new EncryptConfig();
        config.setPassword("my-strong-key-001");
        config.setUsedPasswords(Arrays.asList("history-key-A", "history-key-A"));
        assertThatThrownBy(config::init)
            .isInstanceOf(BeanInitializationException.class)
            .hasMessageContaining("duplicated entry");
    }

    @Test
    void acceptsValidConfigWithoutHistoricalPasswords() {
        EncryptConfig config = new EncryptConfig();
        config.setPassword("my-strong-key-001");
        config.setUsedPasswords(new ArrayList<>());
        // should not throw
        config.init();
        assertThat(config.getPassword()).isEqualTo("my-strong-key-001");
    }

    @Test
    void acceptsValidConfigWithUsedPasswords() {
        EncryptConfig config = new EncryptConfig();
        config.setPassword("my-strong-key-001");
        config.setUsedPasswords(Arrays.asList("legacy-key-B", "legacy-key-A"));
        config.init();
        assertThat(config.getUsedPasswords()).hasSize(2);
    }

    @Test
    void acceptsNullUsedPasswordsList() {
        EncryptConfig config = new EncryptConfig();
        config.setPassword("my-strong-key-001");
        config.setUsedPasswords(null);
        config.init();
        assertThat(config.getUsedPasswords()).isNull();
    }

    @Test
    void acceptsEmptyUsedPasswordsList() {
        EncryptConfig config = new EncryptConfig();
        config.setPassword("my-strong-key-001");
        config.setUsedPasswords(Collections.emptyList());
        config.init();
        assertThat(config.getUsedPasswords()).isEmpty();
    }
}
