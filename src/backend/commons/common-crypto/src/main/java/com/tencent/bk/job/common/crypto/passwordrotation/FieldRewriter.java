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

import com.tencent.bk.job.common.crypto.SymmetricCryptoService;

import java.util.List;

/**
 * 密码轮换字段重写器接口。
 *
 * <p>每张需要做密码轮换的数据库表 + 字段，对应一个 {@code FieldRewriter} 实现。
 * 实现类由各微服务以 Spring Bean 的形式注册，{@link PasswordRotationOrchestrator} 会自动扫描并驱动执行。
 *
 * <p>主键游标 (pkCursor) 以字符串承载，由实现自定义语义（如自增 ID 的 long 字符串、UUID、时间戳等）。
 * {@link #fetchBatch} 必须严格按主键<strong>降序</strong>返回，且 pkCursor &lt; lastProcessedPkCursor
 * （首次为 null 表示从最大主键开始向下扫描）。
 */
public interface FieldRewriter {

    /**
     * 目标表名（仅用于进度记录与日志）
     */
    String tableName();

    /**
     * 目标字段名（仅用于进度记录与日志）
     */
    String fieldName();

    /**
     * 分批拉取需要处理的行。
     *
     * @param lastProcessedPkCursor 上次批次最后处理的主键游标，首次调用时为 null
     * @param batchSize             每批最大行数
     * @return 按 pkCursor 降序排列的行列表；若为空则代表已全部处理完毕
     */
    List<FieldBatchRow> fetchBatch(String lastProcessedPkCursor, int batchSize);

    /**
     * 将单行的密文更新为重加密后的新密文。
     *
     * <p>实现应使用 {@code WHERE pk = ? AND cipher_field = oldCipher} 的乐观更新，
     * 避免并发写覆盖业务修改：若 UPDATE 影响行数为 0 则跳过（已被业务更新，等下轮处理）。
     *
     * @param pkCursor  行主键游标
     * @param oldCipher 当前密文（用于乐观锁校验）
     * @param newCipher 重加密后的新密文
     * @return 实际更新的行数（0 或 1）
     */
    int updateRow(String pkCursor, String oldCipher, String newCipher);

    /**
     * 将一行的当前值重加密为"主密钥加密"（密码轮换迁移场景），并通过 {@link ReEncryptResult}
     * 显式告知 orchestrator 本行是否真的产生了变化。
     *
     * <p>默认实现：当值本身是一段密文时，调用
     * {@link SymmetricCryptoService#reEncryptToActiveForRotation(String)}，
     * 即解密时优先使用"上一次密码"试错，主密钥末位兜底。
     * 由于 AES/CBC 每次加密使用随机 IV，重加密后的字面值必然不同，
     * 因此默认实现一律返回 {@link ReEncryptResult#changed(String)}。
     *
     * <p>对于 JSON 复合字段，实现应解析后对每个 CIPHER 子字段做重加密、再序列化为 JSON 返回，
     * 子字段重加密也应使用 {@link SymmetricCryptoService#reEncryptToActiveForRotation(String)}。
     * <strong>当 JSON 中不含任何需要重加密的 CIPHER 子项时，必须返回
     * {@link ReEncryptResult#unchanged()}</strong>，避免 orchestrator 发起一次"以原值更新"的无意义 UPDATE。
     */
    default ReEncryptResult reEncryptToActive(SymmetricCryptoService svc, String value) {
        return ReEncryptResult.changed(svc.reEncryptToActiveForRotation(value));
    }

    /**
     * 统计本 rewriter 在数据库中"待迁移的总行数"，<strong>仅在首次启动时调用一次</strong>，
     * 结果写入 {@code crypto_password_rotation_progress.total_rows}，用于进度日志展示。
     *
     * <p>实现应与 {@link #fetchBatch(String, int)} 的过滤条件保持一致
     * （通常为 {@code fieldName IS NOT NULL} 再加业务过滤），
     * 这样 {@code processedRows / totalRows} 才有可比的语义。
     *
     * <p>默认返回 {@code -1L} 表示"未实现/未知"，此时进度日志只会显示已处理数。
     * 大表 rewriter 若担心 count 耗时过长，可保持默认或对返回值加自身条件兜底。
     *
     * @return 待迁移总行数；返回值 {@code < 0} 视为"未知"
     */
    default long countRemaining() {
        return -1L;
    }
}
