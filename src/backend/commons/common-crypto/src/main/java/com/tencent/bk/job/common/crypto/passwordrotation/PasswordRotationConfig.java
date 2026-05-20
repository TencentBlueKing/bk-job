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

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 旧密码加密数据轮换迁移任务配置。
 *
 * <p>触发方式：服务启动后等待 {@link #initialDelayMs} 毫秒触发一次，由后台线程
 * 在分布式锁保护下持续运行直到所有 rewriter 都标记为 DONE，跑完即退出；
 * 若中途被 kill，下次服务启动会从 {@code crypto_password_rotation_progress.last_processed_pk}
 * 续跑。全部 DONE 后不会再被触发。
 */
@ConfigurationProperties(prefix = "job.encrypt.old-data-password-rotation")
@Getter
@Setter
public class PasswordRotationConfig {

    /**
     * 是否开启旧密码加密数据轮换迁移任务，默认 true
     */
    private boolean enabled = true;

    /**
     * 每批处理的最大行数，默认 500
     */
    private int batchSize = 500;

    /**
     * 每批处理完成后等待的毫秒数，用于限速，默认 100ms
     */
    private long sleepMsBetweenBatch = 100L;

    /**
     * 是否同时迁移执行实例历史大表（step_instance_script、task_instance_variable 等）。
     * 这些表行数极大，默认关闭，仅在运维确认影响可接受时显式开启。
     */
    private boolean includeExecutionHistoryTables = false;

    /**
     * 服务启动（ApplicationReadyEvent）后首次触发前的延迟毫秒数，默认 10000ms（10s），
     * 给 Spring 应用完全就绪留缓冲。
     */
    private long initialDelayMs = 10_000L;
}
