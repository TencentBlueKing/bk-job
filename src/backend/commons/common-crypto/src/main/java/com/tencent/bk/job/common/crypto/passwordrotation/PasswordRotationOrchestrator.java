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
import com.tencent.bk.job.common.crypto.PasswordRotationDecryptException;
import com.tencent.bk.job.common.crypto.SymmetricCryptoService;
import com.tencent.bk.sdk.crypto.util.CryptorMetaUtil;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 密码轮换编排器：依次驱动所有注册的 {@link FieldRewriter} 完成批量重加密迁移。
 *
 * <p>每个微服务实例化一个此对象，注入该微服务自己的 {@link PasswordRotationProgressDAO}
 * 实现（使用自身数据库连接）和全部 {@link FieldRewriter} Bean。
 *
 * <p>触发方式：服务启动后由 {@code PasswordRotationStartupTrigger} 在分布式锁保护下
 * 调用一次 {@link #runUntilAllDone()}；该方法会循环执行 {@link #runAll()} 直到
 * 所有 rewriter 对应的进度行都标记为 DONE，期间被 kill 由 {@link PasswordRotationProgress}
 * 表保存的 {@code lastProcessedPk} 在下次启动时自动续跑。
 */
@Slf4j
public class PasswordRotationOrchestrator {

    /**
     * 单个 rewriter 在本进程内允许连续失败的最大次数。
     * 超过阈值后本进程内挂起该 rewriter（{@link #runAll()} 视其为已完成跳过），
     * 等待人工排查或服务重启后再次尝试。
     */
    static final int MAX_CONSECUTIVE_ERRORS_PER_REWRITER = 5;

    private final SymmetricCryptoService symmetricCryptoService;
    private final CryptoConfigService cryptoConfigService;
    private final PasswordRotationProgressDAO progressDAO;
    private final List<FieldRewriter> rewriters;
    private final PasswordRotationConfig config;
    private final MeterRegistry meterRegistry;

    /**
     * 进程内 per-rewriter 连续失败计数器，key = tableName + "#" + fieldName。
     */
    private final ConcurrentMap<String, Integer> consecutiveErrors = new ConcurrentHashMap<>();

    public PasswordRotationOrchestrator(SymmetricCryptoService symmetricCryptoService,
                                   CryptoConfigService cryptoConfigService,
                                   PasswordRotationProgressDAO progressDAO,
                                   List<FieldRewriter> rewriters,
                                   PasswordRotationConfig config,
                                   MeterRegistry meterRegistry) {
        this.symmetricCryptoService = symmetricCryptoService;
        this.cryptoConfigService = cryptoConfigService;
        this.progressDAO = progressDAO;
        this.rewriters = rewriters;
        this.config = config;
        this.meterRegistry = meterRegistry;
    }

    /**
     * 持续触发迁移直到所有 rewriter 都标记为 DONE。
     *
     * <p>该方法由 {@code PasswordRotationStartupTrigger} 在服务启动后通过
     * {@code DistributedUniqueTask} 在分布式锁保护下调用一次，跑完即退出，
     * 全部 DONE 后不会再被重复触发。中途被 kill，下次启动时会从 {@code crypto_password_rotation_progress}
     * 表的 {@code last_processed_pk} 续跑。
     *
     * <p>循环退出条件：
     * <ul>
     *   <li>{@link #runAll()} 返回 true（本轮所有 rewriter 都 DONE 或被本进程挂起）；</li>
     *   <li>当前线程被中断（如优雅停机）。</li>
     * </ul>
     */
    public void runUntilAllDone() {
        if (rewriters == null || rewriters.isEmpty()) {
            log.info("PasswordRotation: no rewriter registered, nothing to do");
            return;
        }
        long startMs = System.currentTimeMillis();
        int loop = 0;
        while (true) {
            loop++;
            boolean allDone;
            try {
                allDone = runAll();
            } catch (Exception e) {
                log.error(
                    "PasswordRotation: runUntilAllDone aborted at loop {} due to unexpected error, "
                        + "will resume on next service restart",
                    loop, e
                );
                return;
            }
            if (allDone) {
                log.info(
                    "PasswordRotation: all rewriters DONE after {} loop(s), elapsedMs={}",
                    loop, System.currentTimeMillis() - startMs
                );
                return;
            }
            if (Thread.currentThread().isInterrupted()) {
                log.warn(
                    "PasswordRotation: runUntilAllDone interrupted at loop {}, will resume on next service restart",
                    loop
                );
                return;
            }
        }
    }

    /**
     * 触发一轮迁移：对所有 rewriter 各处理一批数据。
     *
     * @return 本轮检测到所有 rewriter 都已 DONE（或被本进程挂起）则返回 true；
     *         只要还有 rewriter 仍有数据待处理就返回 false。
     */
    public boolean runAll() {
        if (rewriters == null || rewriters.isEmpty()) {
            return true;
        }
        String fingerprint = cryptoConfigService.computeActivePasswordFingerprint();
        log.info("PasswordRotationOrchestrator.runAll: targetFingerprint={}, rewriterCount={}",
            fingerprint, rewriters.size());

        boolean allDone = true;
        for (FieldRewriter rewriter : rewriters) {
            String rewriterKey = rewriter.tableName() + "#" + rewriter.fieldName();
            Integer errors = consecutiveErrors.get(rewriterKey);
            if (errors != null && errors >= MAX_CONSECUTIVE_ERRORS_PER_REWRITER) {
                // 本进程内已挂起：视作已完成跳过，避免循环卡住其他正常 rewriter；
                // 等下次服务重启时再尝试
                log.warn(
                    "PasswordRotation: rewriter table={}, field={} suspended in this process "
                        + "after {} consecutive errors, skip until next restart",
                    rewriter.tableName(), rewriter.fieldName(), errors
                );
                continue;
            }
            try {
                boolean rewriterDone = runSingleRewriter(fingerprint, rewriter);
                consecutiveErrors.remove(rewriterKey);
                if (!rewriterDone) {
                    allDone = false;
                }
            } catch (Exception e) {
                int newCount = consecutiveErrors.merge(rewriterKey, 1, Integer::sum);
                log.error(
                    "PasswordRotation: unexpected error for table={}, field={}, consecutiveErrors={}, "
                        + "skip to next rewriter",
                    rewriter.tableName(), rewriter.fieldName(), newCount, e
                );
                allDone = false;
            }
        }
        return allDone;
    }

    /**
     * 处理单个 rewriter 的一批数据。
     *
     * @return 该 rewriter 在本次调用结束后已经处于 DONE 状态则返回 true；否则返回 false。
     */
    private boolean runSingleRewriter(String fingerprint, FieldRewriter rewriter) throws InterruptedException {
        PasswordRotationProgress progress = progressDAO.loadOrCreate(
            fingerprint, rewriter.tableName(), rewriter.fieldName());

        if (progress.isDone()) {
            log.info("PasswordRotation: table={}, field={} already DONE, skip",
                rewriter.tableName(), rewriter.fieldName());
            return true;
        }

        // 首次启动统计待迁移总行数（仅作进度展示参考，失败不影响迁移流程）
        ensureTotalRowsCounted(rewriter, progress);

        progress.setStatus(PasswordRotationProgressStatus.RUNNING.name());
        String lastPk = progress.getLastProcessedPk();

        List<FieldBatchRow> batch = rewriter.fetchBatch(lastPk, config.getBatchSize());
        if (batch == null || batch.isEmpty()) {
            // 全表扫描完毕
            markRewriterDone(rewriter, progress);
            return true;
        }

        BatchStats stats = processBatch(rewriter, batch, progress);
        logBatchDone(rewriter, batch.size(), stats, progress);

        sleepBetweenBatchIfNeeded();
        // 本轮处理了一批数据，但表内可能还有更多记录待处理（下一次 fetchBatch 才会知道）
        return false;
    }

    /**
     * 首次启动时统计"待迁移总行数"并写入进度表。已统计过（{@code totalRows != null}）则跳过，
     * 避免大表反复扫描；rewriter 选择不实现（返回 -1）也跳过；count 抛错不影响迁移流程。
     */
    private void ensureTotalRowsCounted(FieldRewriter rewriter, PasswordRotationProgress progress) {
        if (progress.getTotalRows() != null) {
            return;
        }
        long total;
        try {
            total = rewriter.countRemaining();
        } catch (Exception e) {
            log.warn(
                "PasswordRotation: countRemaining failed for table={}, field={}, "
                    + "skip total_rows stat (will retry on next service restart)",
                rewriter.tableName(), rewriter.fieldName(), e
            );
            return;
        }
        if (total < 0) {
            // rewriter 显式选择不实现行数统计，保持 totalRows=null
            return;
        }
        progress.setTotalRows(total);
        progressDAO.updateProgress(progress);
        log.info(
            "PasswordRotation: counted totalRows={} for table={}, field={}",
            total, rewriter.tableName(), rewriter.fieldName()
        );
    }

    /**
     * 全表扫描完毕，将进度标记为 DONE 并持久化。
     */
    private void markRewriterDone(FieldRewriter rewriter, PasswordRotationProgress progress) {
        progress.setStatus(PasswordRotationProgressStatus.DONE.name());
        progressDAO.updateProgress(progress);
        log.info(
            "PasswordRotation DONE: table={}, field={}, reEncryptedRows={}, skippedRows={}, "
                + "processedRows={}, totalRows={}",
            rewriter.tableName(), rewriter.fieldName(),
            progress.getReEncryptedRows(), progress.getSkippedRows(),
            progress.getProcessedRows(), formatTotalRows(progress)
        );
    }

    /**
     * 遍历一批记录，对每行尝试重加密；每处理完一行立即把进度持久化到 DAO 并上报指标，
     * 避免本批未完成时进程被 kill 导致进度丢失，下次启动时重复扫描已处理行。
     */
    private BatchStats processBatch(FieldRewriter rewriter,
                                    List<FieldBatchRow> batch,
                                    PasswordRotationProgress progress) {
        BatchStats stats = new BatchStats();
        for (FieldBatchRow row : batch) {
            RowResult result = processSingleRow(rewriter, row, progress);
            updateProgressForRow(progress, row.getPkCursor(), result);
            reportRowMetric(rewriter, result);
            accumulateStats(stats, row.getPkCursor(), result);
        }
        return stats;
    }

    /**
     * 处理单行数据：空值计入 skipped；否则直接走"先解密再用主密钥加密 + 乐观更新"。
     * 解密失败的行只记录 lastError 与 failed 计数，不阻塞整批迁移。
     */
    private RowResult processSingleRow(FieldRewriter rewriter,
                                       FieldBatchRow row,
                                       PasswordRotationProgress progress) {
        String cipher = row.getCipherValue();
        if (StringUtils.isBlank(cipher)) {
            return RowResult.SKIPPED;
        }
        try {
            ReEncryptResult result = rewriter.reEncryptToActive(symmetricCryptoService, cipher);
            if (!result.isChanged()) {
                // rewriter 显式告知本行无需更新（典型场景：JSON 复合字段不含需重加密的 CIPHER 子项）
                // 跳过 UPDATE，避免以原值更新触发无意义的乐观锁 + binlog
                log.debug(
                    "PasswordRotation: rewriter reports no-op for table={}, field={}, pk={}",
                    rewriter.tableName(), rewriter.fieldName(), row.getPkCursor()
                );
                return RowResult.SKIPPED;
            }
            String reEncrypted = result.getReEncryptedValue();
            int updated = rewriter.updateRow(row.getPkCursor(), cipher, reEncrypted);
            if (updated > 0) {
                // 不输出密文原文，避免与历史密码组合后被批量解密；
                // 仅输出指纹 + 长度 + 算法标识与末 4 字节（如 [Cipher:::AES_CBC]****0Q==），便于排查
                log.info(
                    "PasswordRotation: table={}, field={}, pk={}, oldCipherFp={}, oldCipherMasked={}, "
                        + "newCipherMasked={}, oldLen={}, newLen={}, updatedRows={}",
                    rewriter.tableName(),
                    rewriter.fieldName(),
                    row.getPkCursor(),
                    CryptoConfigService.computePasswordFingerprint(cipher),
                    maskCipher(cipher),
                    maskCipher(reEncrypted),
                    cipher.length(),
                    reEncrypted.length(),
                    updated
                );
                return RowResult.RE_ENCRYPTED;
            }
            // 乐观更新失败：业务并发写了新值，等下轮处理
            log.info("PasswordRotation: optimistic update skipped table={}, field={}, pk={}",
                rewriter.tableName(), rewriter.fieldName(), row.getPkCursor());
            return RowResult.SKIPPED;
        } catch (PasswordRotationDecryptException e) {
            log.warn(
                "PasswordRotation: cannot decrypt with any key, mark suspicious and skip. "
                    + "table={}, field={}, pk={}",
                rewriter.tableName(), rewriter.fieldName(), row.getPkCursor()
            );
            // 记录到 lastError 但不阻塞迁移
            progress.setLastError(String.format(
                "pk=%s decrypt failed: %s", row.getPkCursor(), e.getMessage()));
            return RowResult.FAILED;
        }
    }

    /**
     * 对密文做脱敏掩码，仅保留可用于排查的"算法标识符 + 末 4 字符"信息。
     *
     * <p>例如：
     * <pre>
     * [Cipher:::AES_CBC]hr9gVbeo...0Q==  →  [Cipher:::AES_CBC]****0Q==
     * </pre>
     *
     * <p>算法标识符（{@code [Cipher:::ALG]}）来源于 {@link CryptorMetaUtil}，属于公开元信息，
     * 输出末 4 字符可在不同行之间做"是否同一密文"的肉眼比对，且不足以构成可解密信息泄漏。
     *
     * <p>降级策略：若密文不带算法标识（旧格式），输出"前 4 + **** + 后 4"；密文过短时按原文输出。
     */
    private static String maskCipher(String cipher) {
        if (cipher == null) {
            return "";
        }
        final int tailLen = 4;
        String cryptorName = null;
        try {
            cryptorName = CryptorMetaUtil.getCryptorNameFromCipher(cipher);
        } catch (Exception ignore) {
            // 解析失败兜底走无算法标识分支
        }
        if (StringUtils.isNotBlank(cryptorName)) {
            String prefix = CryptorMetaUtil.getCipherMetaPrefix()
                + cryptorName
                + CryptorMetaUtil.getCipherMetaSuffix();
            if (cipher.length() <= prefix.length()) {
                // 理论不会发生：只有元前缀没有密文体
                return prefix + "****";
            }
            String body = cipher.substring(prefix.length());
            if (body.length() <= tailLen) {
                // 密文体过短，掩码反失真，直接原样输出
                return cipher;
            }
            return prefix + "****" + body.substring(body.length() - tailLen);
        }
        // 无算法标识（旧格式密文）：头 4 + **** + 尾 4
        if (cipher.length() <= tailLen * 2) {
            return cipher;
        }
        return cipher.substring(0, tailLen) + "****" + cipher.substring(cipher.length() - tailLen);
    }

    /**
     * 单行处理结束后立即更新 progress 并持久化到 DAO。
     * failed 行同样计入 skippedRows，以保持"已处理总数 = reEncrypted + skipped"的不变式。
     */
    private void updateProgressForRow(PasswordRotationProgress progress, String pkCursor, RowResult result) {
        progress.setLastProcessedPk(pkCursor);
        progress.setProcessedRows(progress.getProcessedRows() + 1);
        if (result == RowResult.RE_ENCRYPTED) {
            progress.setReEncryptedRows(progress.getReEncryptedRows() + 1);
        } else {
            // SKIPPED 与 FAILED 都计入 skippedRows
            progress.setSkippedRows(progress.getSkippedRows() + 1);
        }
        progressDAO.updateProgress(progress);
    }

    /**
     * 单行处理结束后立即上报一次 Prometheus Counter（{@code meterRegistry} 为空时跳过）。
     *
     * <p>统一一个 metric {@code job.password_rotation.rows}，按 {@code table / field / result} 三个 tag 维度区分；
     * 总处理量可由 {@code sum without (result) (rate(job_password_rotation_rows_total[5m]))} 聚合得到，
     * 无需另起一个"processed"计数器。
     *
     * <p>Micrometer 内部按 name+tags 缓存 Counter 实例，重复 register 调用是幂等的。
     */
    private void reportRowMetric(FieldRewriter rewriter, RowResult result) {
        if (meterRegistry == null) {
            return;
        }
        Counter.builder("job.password_rotation.rows")
            .description("Password rotation row count, tagged by table/field/result (RE_ENCRYPTED / SKIPPED / FAILED)")
            .tags(
                "table", rewriter.tableName(),
                "field", rewriter.fieldName(),
                "result", result.name()
            )
            .register(meterRegistry)
            .increment();
    }

    /**
     * 累加 batch 维度统计，仅用于结束日志，不参与进度持久化（已在 updateProgressForRow 中实时落库）。
     */
    private void accumulateStats(BatchStats stats, String pkCursor, RowResult result) {
        stats.lastPk = pkCursor;
        switch (result) {
            case RE_ENCRYPTED:
                stats.reEncrypted++;
                break;
            case FAILED:
                stats.failed++;
                break;
            case SKIPPED:
            default:
                stats.skipped++;
                break;
        }
    }

    private void logBatchDone(FieldRewriter rewriter, int batchSize, BatchStats stats, PasswordRotationProgress progress) {
        log.info(
            "PasswordRotation batch done: table={}, field={}, batchSize={}, reEncrypted={}, skipped={}, "
                + "failed={}, processed={}/{}, progress={}, lastPk={}",
            rewriter.tableName(), rewriter.fieldName(),
            batchSize, stats.reEncrypted, stats.skipped, stats.failed,
            progress.getProcessedRows(), formatTotalRows(progress),
            formatProgressPercent(progress),
            stats.lastPk
        );
    }

    /**
     * 把 {@code totalRows} 渲染为日志友好的字符串：未知（null 或 < 0）时显示 "?"。
     */
    private static String formatTotalRows(PasswordRotationProgress p) {
        Long total = p.getTotalRows();
        if (total == null || total < 0) {
            return "?";
        }
        return total.toString();
    }

    /**
     * 渲染百分比："?" 当 totalRows 未知或 0；否则 {@code processed/total * 100}，对 > 100 cap 到 100（业务期间有新写入时可能超额）。
     */
    private static String formatProgressPercent(PasswordRotationProgress p) {
        Long total = p.getTotalRows();
        if (total == null || total <= 0) {
            return "?";
        }
        double pct = 100.0 * p.getProcessedRows() / total;
        if (pct > 100.0) {
            pct = 100.0;
        }
        return String.format("%.2f%%", pct);
    }

    private void sleepBetweenBatchIfNeeded() throws InterruptedException {
        if (config.getSleepMsBetweenBatch() > 0) {
            Thread.sleep(config.getSleepMsBetweenBatch());
        }
    }

    /**
     * 单行迁移结果：用于把"行级落库 + 指标上报 + batch 统计"三种关切解耦。
     */
    private enum RowResult {
        RE_ENCRYPTED, SKIPPED, FAILED
    }

    /**
     * 一批数据处理过程中累积的可变统计（仅用于 batch 结束日志）：
     * 仅在 orchestrator 内部线程局部使用，无并发问题，
     * 故用裸字段而非 AtomicLong 以减少对象与方法调用开销。
     */
    private static class BatchStats {
        long reEncrypted;
        long skipped;
        long failed;
        String lastPk;
    }
}
