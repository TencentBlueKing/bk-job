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
 * {@link FieldRewriter#reEncryptToActive} 的返回结果。
 *
 * <p>承载"重加密后是否真的产生了变化 + 新密文"两个信号，
 * 用于让 {@link PasswordRotationOrchestrator} 决定是否发起 UPDATE：
 * <ul>
 *   <li>{@link #isChanged()} 为 {@code false}：本行无需更新（典型场景：JSON 复合字段
 *       不含 CIPHER 子项、或所有子项均无需重加密），orchestrator 直接跳过 UPDATE，
 *       避免无意义的"以原值更新"占用乐观锁与 binlog；</li>
 *   <li>{@link #isChanged()} 为 {@code true}：{@link #getReEncryptedValue()} 是重加密后的新值，
 *       orchestrator 会以 {@code WHERE pk=? AND cipher=oldCipher} 发起乐观更新。</li>
 * </ul>
 *
 * <p>对于单字段密文（非 JSON）的 rewriter，由于 AES/CBC 每次加密使用随机 IV，
 * 重加密后的字面值必然不同，应一律返回 {@link #changed(String)}。
 */
public final class ReEncryptResult {

    /**
     * 单例：无变更，避免重复创建对象。
     */
    private static final ReEncryptResult UNCHANGED = new ReEncryptResult(false, null);

    private final boolean changed;
    private final String reEncryptedValue;

    private ReEncryptResult(boolean changed, String reEncryptedValue) {
        this.changed = changed;
        this.reEncryptedValue = reEncryptedValue;
    }

    /**
     * 本行无需更新（orchestrator 应跳过 UPDATE）。
     */
    public static ReEncryptResult unchanged() {
        return UNCHANGED;
    }

    /**
     * 本行需要更新为新密文 {@code reEncryptedValue}。
     */
    public static ReEncryptResult changed(String reEncryptedValue) {
        return new ReEncryptResult(true, reEncryptedValue);
    }

    public boolean isChanged() {
        return changed;
    }

    public String getReEncryptedValue() {
        return reEncryptedValue;
    }
}
