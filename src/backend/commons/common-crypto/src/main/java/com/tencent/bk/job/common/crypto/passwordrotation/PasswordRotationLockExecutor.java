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

/**
 * 分布式锁执行器抽象：用于把锁实现（如 Redis 锁）从密码轮换框架中解耦。
 *
 * <p>{@link PasswordRotationStartupTrigger} 自身位于 {@code common-crypto} 模块，
 * 为避免强依赖 {@code common-redis}，将“拿锁 + 执行 + 释放锁”的过程通过本接口注入。
 *
 * <p>典型实现：使用 {@code DistributedUniqueTask} 包装的 Redis 分布式唯一任务。
 * 锁被其他实例持有时实现方可直接跳过，不视为异常。
 */
@FunctionalInterface
public interface PasswordRotationLockExecutor {

    /**
     * 在分布式锁保护下执行任务。
     *
     * @param lockKey 锁 Key，由调用方按业务/服务名拼接
     * @param task    需要在锁保护下执行的任务
     * @throws Exception 执行过程中的任何异常（含锁框架自身异常）
     */
    void runUnderLock(String lockKey, Runnable task) throws Exception;
}
