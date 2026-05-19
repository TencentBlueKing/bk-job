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
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 * 通用的旧密码加密数据轮换迁移触发器：
 * 监听 {@link ApplicationReadyEvent}，在应用完全就绪后启动一个守护线程，
 * 等待 {@code initialDelayMs} 毫秒后通过 {@link PasswordRotationLockExecutor} 拿到分布式锁，
 * 调用 {@link PasswordRotationOrchestrator#runUntilAllDone()} 一次性把所有旧密码数据迁移完成；
 * 全部 DONE 即退出，整个服务生命周期内不再触发；中途被 kill 由进度表续跑。
 *
 * <p>各微服务在自身的 AutoConfiguration 中以 {@code @Bean} 方式构造本类实例，
 * 通过构造函数传入对应的 {@code applicationName}（如 {@code job-manage}）即可。
 */
@Slf4j
public class PasswordRotationStartupTrigger {

    private static final String LOCK_KEY_PREFIX = "password-rotation-lock-";
    private static final String TASK_NAME_PREFIX = "password-rotation-";
    private static final String THREAD_NAME_PREFIX = "password-rotation-startup-";

    @Getter
    private final String applicationName;
    private final PasswordRotationOrchestrator orchestrator;
    private final PasswordRotationConfig config;
    private final PasswordRotationLockExecutor lockExecutor;
    private final CryptoConfigService cryptoConfigService;

    public PasswordRotationStartupTrigger(String applicationName,
                                          PasswordRotationOrchestrator orchestrator,
                                          PasswordRotationConfig config,
                                          PasswordRotationLockExecutor lockExecutor,
                                          CryptoConfigService cryptoConfigService) {
        this.applicationName = applicationName;
        this.orchestrator = orchestrator;
        this.config = config;
        this.lockExecutor = lockExecutor;
        this.cryptoConfigService = cryptoConfigService;
    }

    /**
     * 应用完全就绪后触发一次性迁移。
     * 启用开关由 {@code job.encrypt.oldDataPasswordRotation.enabled} 控制（默认 true）。
     *
     * <p>短路：当 {@link CryptoConfigService#hasHistoricalPasswordsForRotation()} 为 false 时
     * （历史密码为空，或唯一一项与主密码相同），不存在需要迁移的旧密文，直接跳过启动迁移。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!config.isEnabled()) {
            log.info("PasswordRotation disabled by config, skip startup migration for {}", applicationName);
            return;
        }
        if (!cryptoConfigService.hasHistoricalPasswordsForRotation()) {
            log.info(
                "PasswordRotation: no historical passwords configured (usedPasswords empty "
                    + "or identical to active password), skip startup migration for {}",
                applicationName
            );
            return;
        }
        Thread worker = new Thread(this::doRunInBackground, THREAD_NAME_PREFIX + applicationName);
        worker.setDaemon(true);
        worker.start();
        log.info(
            "PasswordRotation: startup migration scheduled for {}, initialDelayMs={}",
            applicationName, config.getInitialDelayMs()
        );
    }

    private void doRunInBackground() {
        try {
            if (config.getInitialDelayMs() > 0) {
                Thread.sleep(config.getInitialDelayMs());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("PasswordRotation {} initial delay interrupted, abort", applicationName);
            return;
        }
        String lockKey = LOCK_KEY_PREFIX + applicationName;
        try {
            log.info("PasswordRotation for {} arranged", applicationName);
            lockExecutor.runUnderLock(lockKey, orchestrator::runUntilAllDone);
        } catch (Exception e) {
            log.error("PasswordRotation {} startup migration failed", applicationName, e);
        }
    }

    /**
     * 内部任务名（不含锁 key 前缀），可供实现 {@link PasswordRotationLockExecutor} 的一方使用，
     * 便于在分布式锁框架中作为任务标识。
     */
    public static String buildTaskName(String applicationName) {
        return TASK_NAME_PREFIX + applicationName;
    }
}
