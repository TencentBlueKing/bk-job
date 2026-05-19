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

package com.tencent.bk.job.common.crypto.passwordrotation;

import com.tencent.bk.job.common.crypto.CryptoConfigService;
import com.tencent.bk.job.common.crypto.CryptoTypeEnum;
import com.tencent.bk.job.common.crypto.EncryptConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 {@link PasswordRotationStartupTrigger} 的"无历史密码则短路"行为：
 * <ul>
 *     <li>{@code usedPasswords} 为空 → 不进入分布式锁、不调用 orchestrator；</li>
 *     <li>配置了不同于主密钥的历史密码 → 走守护线程，最终会调用 lockExecutor 并执行 orchestrator。</li>
 * </ul>
 *
 * <p>"历史密码只有 1 个且与主密码相同"在 {@link EncryptConfig#validate()} 阶段就会 fail-fast，
 * 不会走到 trigger，因此该情况由 {@code EncryptConfigTest} 与 {@code CryptoConfigServiceTest} 联合保证。
 */
class PasswordRotationStartupTriggerTest {

    private static final String ACTIVE_KEY = "active-master-key-2026";
    private static final String OLD_KEY = "legacy-master-key-001";

    private static CryptoConfigService buildCryptoConfigService(List<String> usedPasswords) {
        EncryptConfig config = new EncryptConfig();
        config.setType(CryptoTypeEnum.CLASSIC);
        config.setPassword(ACTIVE_KEY);
        config.setUsedPasswords(usedPasswords);
        config.init();
        return new CryptoConfigService(config);
    }

    private static PasswordRotationOrchestrator emptyOrchestrator() {
        // PasswordRotationOrchestrator 的 runUntilAllDone 在空 rewriter 列表下安全返回，仍可用作"占位"实例。
        return new PasswordRotationOrchestrator(
            null, buildCryptoConfigService(Collections.emptyList()), null,
            Collections.emptyList(), new PasswordRotationConfig(), new SimpleMeterRegistry());
    }

    @Test
    void onApplicationReady_skipsWhenUsedPasswordsEmpty() {
        AtomicInteger lockInvocations = new AtomicInteger();
        PasswordRotationLockExecutor lockExecutor = (lockKey, task) -> lockInvocations.incrementAndGet();

        PasswordRotationConfig config = new PasswordRotationConfig();
        config.setInitialDelayMs(0L);

        PasswordRotationStartupTrigger trigger = new PasswordRotationStartupTrigger(
            "job-test", emptyOrchestrator(), config, lockExecutor,
            buildCryptoConfigService(Collections.emptyList())
        );

        trigger.onApplicationReady();
        // 短路后不会启动守护线程，无需等待；直接断言 lockExecutor 未被触发
        assertThat(lockInvocations).hasValue(0);
    }

    @Test
    void onApplicationReady_proceedsWhenHistoricalPasswordsConfigured() throws InterruptedException {
        AtomicInteger lockInvocations = new AtomicInteger();
        PasswordRotationLockExecutor lockExecutor = (lockKey, task) -> {
            lockInvocations.incrementAndGet();
            task.run();
        };

        PasswordRotationConfig config = new PasswordRotationConfig();
        config.setInitialDelayMs(0L);

        PasswordRotationStartupTrigger trigger = new PasswordRotationStartupTrigger(
            "job-test", emptyOrchestrator(), config, lockExecutor,
            buildCryptoConfigService(Collections.singletonList(OLD_KEY))
        );

        trigger.onApplicationReady();
        // 守护线程异步执行，等待最多 2 秒让 lockExecutor 被调用
        long deadline = System.currentTimeMillis() + 2000L;
        while (lockInvocations.get() == 0 && System.currentTimeMillis() < deadline) {
            TimeUnit.MILLISECONDS.sleep(20L);
        }
        assertThat(lockInvocations).hasValueGreaterThanOrEqualTo(1);
    }

    @Test
    void onApplicationReady_skipsWhenConfigDisabled() {
        AtomicInteger lockInvocations = new AtomicInteger();
        PasswordRotationLockExecutor lockExecutor = (lockKey, task) -> lockInvocations.incrementAndGet();

        PasswordRotationConfig config = new PasswordRotationConfig();
        config.setEnabled(false);
        config.setInitialDelayMs(0L);

        PasswordRotationStartupTrigger trigger = new PasswordRotationStartupTrigger(
            "job-test", emptyOrchestrator(), config, lockExecutor,
            buildCryptoConfigService(Collections.singletonList(OLD_KEY))
        );

        trigger.onApplicationReady();
        assertThat(lockInvocations).hasValue(0);
    }
}
