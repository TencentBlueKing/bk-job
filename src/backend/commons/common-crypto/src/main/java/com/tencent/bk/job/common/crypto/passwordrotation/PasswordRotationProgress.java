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

import lombok.Data;

/**
 * 密码轮换进度记录（对应 crypto_password_rotation_progress 表中的一行）
 */
@Data
public class PasswordRotationProgress {

    /**
     * 主键（数据库自增 ID）
     */
    private Long id;

    /**
     * 本轮迁移的目标密钥指纹（主密钥的 SHA-256 前 8 字节 hex）。
     * 密钥变更时此值会不同，可借此区分不同轮次。
     */
    private String targetPasswordFingerprint;

    /**
     * 目标表名
     */
    private String tableName;

    /**
     * 目标字段名
     */
    private String fieldName;

    /**
     * 上次已处理到的主键游标字符串（用于断点续处理，支持任意 PK 类型）
     */
    private String lastProcessedPk;

    /**
     * 处理的总行数
     */
    private long processedRows;

    /**
     * 实际完成重加密的行数
     */
    private long reEncryptedRows;

    /**
     * 跳过（已是主密钥加密、或密文为空）的行数
     */
    private long skippedRows;

    /**
     * 进度状态：PENDING / RUNNING / DONE / FAILED
     */
    private String status;

    /**
     * 最近一次错误信息（FAILED 时有值）
     */
    private String lastError;

    /**
     * 首次启动时一次性统计的"待迁移总行数"（与 {@code FieldRewriter#fetchBatch} 的过滤条件保持一致）。
     * 仅用于进度展示和大致剩余时间估算，<strong>不</strong>参与迁移控制逻辑。
     * <ul>
     *   <li>{@code null}：尚未统计（旧记录或本轮首次启动尚未跑到 count 步骤）；</li>
     *   <li>{@code >= 0}：表为空或扫描到的真实行数；一旦写入便不再重新统计，避免大表反复扫描。</li>
     * </ul>
     */
    private Long totalRows;

    public boolean isDone() {
        return PasswordRotationProgressStatus.DONE.name().equals(status);
    }
}
