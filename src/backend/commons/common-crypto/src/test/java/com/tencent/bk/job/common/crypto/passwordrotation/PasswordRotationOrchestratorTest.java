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
import com.tencent.bk.job.common.crypto.JobCryptorNames;
import com.tencent.bk.job.common.crypto.SymmetricCryptoService;
import com.tencent.bk.sdk.crypto.cryptor.SymmetricCryptor;
import com.tencent.bk.sdk.crypto.cryptor.SymmetricCryptorFactory;
import com.tencent.bk.sdk.crypto.exception.CryptoException;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link PasswordRotationOrchestrator} 端到端单测：
 * 用一个内存表 + Fake FieldRewriter 验证：
 * 1) 单批迁移把旧密钥加密的行重加密为主密钥加密；
 * 2) 已经是主密钥的行被跳过、计数器正确累加；
 * 3) 断点续处理：连续两次 runAll 能完整推进所有数据；
 * 4) 乐观更新失败（旧密文已被业务并发改写）被跳过并不阻塞迁移；
 * 5) runUntilAllDone 在 N 轮 batch 后能正确把所有 rewriter 推进到 DONE 并退出；
 * 6) runUntilAllDone 在 rewriter 持续抛错时不会无限循环：超过阈值后挂起并退出；
 * 7) 首次启动时调用 {@link FieldRewriter#countRemaining()} 把待迁移总行数写入 totalRows，
 *    后续 batch 不再重复统计；rewriter 返回 -1 时 totalRows 保持 null。
 */
class PasswordRotationOrchestratorTest {

    private static final String ACTIVE_KEY = "active-master-key-2026";
    private static final String OLD_KEY = "legacy-master-key-2021";

    private SymmetricCryptoService cryptoService;
    private CryptoConfigService cryptoConfigService;
    private SymmetricCryptor rawCryptor;

    @BeforeEach
    void setUp() {
        EncryptConfig encryptConfig = new EncryptConfig();
        encryptConfig.setType(CryptoTypeEnum.CLASSIC);
        encryptConfig.setPassword(ACTIVE_KEY);
        encryptConfig.setUsedPasswords(new ArrayList<>(Collections.singletonList(OLD_KEY)));
        encryptConfig.init();

        cryptoConfigService = new CryptoConfigService(encryptConfig);
        cryptoService = new SymmetricCryptoService(cryptoConfigService);
        rawCryptor = SymmetricCryptorFactory.getCryptor(JobCryptorNames.AES_CBC);
    }

    @Test
    void runAllReEncryptsRowsFromOldKeyToActiveKey() {
        InMemoryRewriter rewriter = new InMemoryRewriter("t", "f");
        rewriter.insert("1", rawCryptor.encrypt(OLD_KEY, "plain-1"));
        rewriter.insert("2", rawCryptor.encrypt(OLD_KEY, "plain-2"));
        rewriter.insert("3", rawCryptor.encrypt(ACTIVE_KEY, "plain-3"));

        InMemoryProgressDAO dao = new InMemoryProgressDAO();
        PasswordRotationConfig config = new PasswordRotationConfig();
        config.setBatchSize(10);
        config.setSleepMsBetweenBatch(0);
        PasswordRotationOrchestrator orchestrator = newOrchestrator(dao, rewriter, config);

        orchestrator.runAll();

        // 降序扫描第一批：处理 3、2、1；lastPk 为当前批次最小主键 "1"。
        // orchestrator 已去掉 isAlreadyOnActiveKey fast path，"3"（已是主密钥加密）会被试错链末位的
        // 主密钥兜底命中并做一次幂等重加密，统计为 RE_ENCRYPTED 而不是 SKIPPED。
        PasswordRotationProgress p = dao.firstProgress();
        assertThat(p.getProcessedRows()).isEqualTo(3);
        assertThat(p.getReEncryptedRows()).isEqualTo(3);
        assertThat(p.getSkippedRows()).isEqualTo(0);
        assertThat(p.getLastProcessedPk()).isEqualTo("1");

        // 第二轮：fetchBatch 返回空 -> 标记 DONE
        orchestrator.runAll();
        assertThat(dao.firstProgress().isDone()).isTrue();

        // 校验内存行确实被改写
        for (Map.Entry<String, String> row : rewriter.rows.entrySet()) {
            String cipher = row.getValue();
            assertThat(encryptedWithActiveKey(cipher))
                .as("row %s 应当已使用主密钥加密", row.getKey())
                .isTrue();
        }
    }

    @Test
    void boundaryRowAlreadyOnActiveKeyIsHandledByFallbackToActiveKey() {
        // 模拟"已迁移但未记录到进度表的边界数据"：行已是 active 加密、但 progress 仍把它纳入扫描范围。
        // 去掉 fast path 后，应由试错链末位的主密钥兜底解密成功，重加密 + update 一次后仍是 active 加密。
        InMemoryRewriter rewriter = new InMemoryRewriter("t", "f");
        rewriter.insert("1", rawCryptor.encrypt(ACTIVE_KEY, "already-on-active"));

        InMemoryProgressDAO dao = new InMemoryProgressDAO();
        PasswordRotationConfig config = new PasswordRotationConfig();
        config.setBatchSize(10);
        config.setSleepMsBetweenBatch(0);
        PasswordRotationOrchestrator orchestrator = newOrchestrator(dao, rewriter, config);

        orchestrator.runAll();

        PasswordRotationProgress p = dao.firstProgress();
        assertThat(p.getProcessedRows()).isEqualTo(1);
        // 边界数据被无意义但幂等地重加密一次，统计为 RE_ENCRYPTED 是当前设计有意为之的取舍
        assertThat(p.getReEncryptedRows()).isEqualTo(1);
        // 经过重加密后该行仍应可被主密钥解密
        assertThat(cryptoService.decrypt(rewriter.rows.get("1"), JobCryptorNames.AES_CBC))
            .isEqualTo("already-on-active");
        assertThat(encryptedWithActiveKey(rewriter.rows.get("1"))).isTrue();
    }

    @Test
    void runAllSupportsResumeAcrossMultipleInvocations() {
        InMemoryRewriter rewriter = new InMemoryRewriter("t", "f");
        for (int i = 1; i <= 7; i++) {
            rewriter.insert(String.valueOf(i), rawCryptor.encrypt(OLD_KEY, "plain-" + i));
        }
        InMemoryProgressDAO dao = new InMemoryProgressDAO();
        PasswordRotationConfig config = new PasswordRotationConfig();
        config.setBatchSize(3);
        config.setSleepMsBetweenBatch(0);
        PasswordRotationOrchestrator orchestrator = newOrchestrator(dao, rewriter, config);

        orchestrator.runAll();
        assertThat(dao.firstProgress().getProcessedRows()).isEqualTo(3);
        assertThat(dao.firstProgress().getLastProcessedPk()).isEqualTo("5");

        orchestrator.runAll();
        assertThat(dao.firstProgress().getProcessedRows()).isEqualTo(6);
        assertThat(dao.firstProgress().getLastProcessedPk()).isEqualTo("2");

        orchestrator.runAll();
        assertThat(dao.firstProgress().getProcessedRows()).isEqualTo(7);
        assertThat(dao.firstProgress().getLastProcessedPk()).isEqualTo("1");

        orchestrator.runAll();
        assertThat(dao.firstProgress().isDone()).isTrue();
        assertThat(dao.firstProgress().getReEncryptedRows()).isEqualTo(7);
    }

    @Test
    void runUntilAllDoneDrivesAllRewritersToDoneInOneCall() {
        InMemoryRewriter rewriter = new InMemoryRewriter("t", "f");
        // 17 行旧密钥数据，batchSize=3 -> 至少需要 6 轮 runAll + 1 轮空批，runUntilAllDone 应一次性跑完
        for (int i = 1; i <= 17; i++) {
            rewriter.insert(String.format("%02d", i), rawCryptor.encrypt(OLD_KEY, "plain-" + i));
        }
        InMemoryProgressDAO dao = new InMemoryProgressDAO();
        PasswordRotationConfig config = new PasswordRotationConfig();
        config.setBatchSize(3);
        config.setSleepMsBetweenBatch(0);
        PasswordRotationOrchestrator orchestrator = newOrchestrator(dao, rewriter, config);

        orchestrator.runUntilAllDone();

        PasswordRotationProgress p = dao.firstProgress();
        assertThat(p.isDone()).as("runUntilAllDone 应在一次调用内把全部 17 行处理完").isTrue();
        assertThat(p.getReEncryptedRows()).isEqualTo(17);
        // 校验所有行已被改写为主密钥加密
        for (Map.Entry<String, String> row : rewriter.rows.entrySet()) {
            assertThat(encryptedWithActiveKey(row.getValue()))
                .as("row %s 应当已使用主密钥加密", row.getKey())
                .isTrue();
        }
    }

    @Test
    void runUntilAllDoneStopsAfterSuspendingFailingRewriter() {
        // 持续抛错的 rewriter：每次 fetchBatch 都抛异常
        FieldRewriter alwaysFailing = new InMemoryRewriter("t", "f") {
            @Override
            public List<FieldBatchRow> fetchBatch(String lastProcessedPkCursor, int batchSize) {
                throw new RuntimeException("simulated database failure");
            }
        };
        InMemoryProgressDAO dao = new InMemoryProgressDAO();
        PasswordRotationConfig config = new PasswordRotationConfig();
        config.setBatchSize(10);
        config.setSleepMsBetweenBatch(0);
        PasswordRotationOrchestrator orchestrator = newOrchestrator(dao, alwaysFailing, config);

        long startMs = System.currentTimeMillis();
        orchestrator.runUntilAllDone();
        long elapsedMs = System.currentTimeMillis() - startMs;

        // 触发上限 MAX_CONSECUTIVE_ERRORS_PER_REWRITER 次后挂起 -> runAll 返回 true -> runUntilAllDone 退出；
        // 不应卡住超过若干秒
        assertThat(elapsedMs)
            .as("rewriter 持续异常时 runUntilAllDone 必须在挂起阈值后及时退出")
            .isLessThan(5_000L);
    }

    @Test
    void progressIsPersistedAfterEveryRowSoCrashLosesAtMostOneRow() {
        // 模拟 batch 中途 kill：rewriter 在处理完前 2 行后抛异常；
        // 验证 progress 表已经记录到第 2 行（而不是整批未提交）。
        InMemoryRewriter rewriter = new InMemoryRewriter("t", "f");
        for (int i = 1; i <= 5; i++) {
            rewriter.insert(String.format("%02d", i), rawCryptor.encrypt(OLD_KEY, "plain-" + i));
        }
        FieldRewriter failingAfterTwo = new FieldRewriter() {
            int processed = 0;

            @Override
            public String tableName() {
                return rewriter.tableName();
            }

            @Override
            public String fieldName() {
                return rewriter.fieldName();
            }

            @Override
            public List<FieldBatchRow> fetchBatch(String lastProcessedPkCursor, int batchSize) {
                return rewriter.fetchBatch(lastProcessedPkCursor, batchSize);
            }

            @Override
            public int updateRow(String pkCursor, String oldCipher, String newCipher) {
                if (++processed > 2) {
                    // 模拟第 3 行处理时进程崩溃
                    throw new RuntimeException("simulated crash mid-batch");
                }
                return rewriter.updateRow(pkCursor, oldCipher, newCipher);
            }
        };
        InMemoryProgressDAO dao = new InMemoryProgressDAO();
        PasswordRotationConfig config = new PasswordRotationConfig();
        config.setBatchSize(10);
        config.setSleepMsBetweenBatch(0);
        PasswordRotationOrchestrator orchestrator = newOrchestrator(dao, failingAfterTwo, config);

        // runAll 内部会捕获 rewriter 抛出的异常并跳过；关键是 progress 已记录前两行
        orchestrator.runAll();

        PasswordRotationProgress p = dao.firstProgress();
        assertThat(p.getProcessedRows())
            .as("前 2 行处理后应已立即落库，崩溃只丢失第 3 行的更新")
            .isEqualTo(2);
        assertThat(p.getReEncryptedRows()).isEqualTo(2);
        // 降序扫描：先处理 "05" → "04"；崩溃发生在更新 "03" 时
        assertThat(p.getLastProcessedPk()).isEqualTo("04");
    }

    @Test
    void optimisticLockFailureIsCountedAsSkip() {
        InMemoryRewriter rewriter = new InMemoryRewriter("t", "f") {
            @Override
            public int updateRow(String pkCursor, String oldCipher, String newCipher) {
                // 模拟乐观锁失败：业务并发修改了该行，UPDATE 影响 0 行
                return 0;
            }
        };
        rewriter.insert("1", rawCryptor.encrypt(OLD_KEY, "plain-1"));
        InMemoryProgressDAO dao = new InMemoryProgressDAO();
        PasswordRotationConfig config = new PasswordRotationConfig();
        config.setBatchSize(10);
        config.setSleepMsBetweenBatch(0);
        PasswordRotationOrchestrator orchestrator = newOrchestrator(dao, rewriter, config);

        orchestrator.runAll();

        PasswordRotationProgress p = dao.firstProgress();
        assertThat(p.getReEncryptedRows()).isZero();
        assertThat(p.getSkippedRows()).isEqualTo(1);
        assertThat(p.getProcessedRows()).isEqualTo(1);
    }

    @Test
    void totalRowsIsCountedOnceOnFirstRunAndKeptThereafter() {
        // 首次启动时调用一次 countRemaining 把 totalRows 写入进度表；
        // 后续 runAll（包括跨进程"续跑"模拟）都不应再调 countRemaining，避免大表反复扫描
        InMemoryRewriter rewriter = new InMemoryRewriter("t", "f");
        for (int i = 1; i <= 5; i++) {
            rewriter.insert(String.format("%02d", i), rawCryptor.encrypt(OLD_KEY, "plain-" + i));
        }
        InMemoryProgressDAO dao = new InMemoryProgressDAO();
        PasswordRotationConfig config = new PasswordRotationConfig();
        config.setBatchSize(2);
        config.setSleepMsBetweenBatch(0);
        PasswordRotationOrchestrator orchestrator = newOrchestrator(dao, rewriter, config);

        orchestrator.runAll();
        PasswordRotationProgress after1st = dao.firstProgress();
        assertThat(after1st.getTotalRows())
            .as("首次 runAll 必须把待迁移行数写入 totalRows")
            .isEqualTo(5L);
        assertThat(rewriter.countRemainingCallCount).isEqualTo(1);

        // 后续 runAll 不应再调 countRemaining（断点续处理时不再扫表统计）
        orchestrator.runAll();
        orchestrator.runAll();
        assertThat(rewriter.countRemainingCallCount)
            .as("totalRows 已存在时不应再次调用 countRemaining")
            .isEqualTo(1);
        assertThat(dao.firstProgress().getTotalRows()).isEqualTo(5L);
    }

    @Test
    void countRemainingReturningNegativeKeepsTotalRowsNull() {
        // rewriter 选择不实现 countRemaining（返回 -1）时，totalRows 应保持 null，
        // 进度日志只显示已处理数；不影响实际迁移流程
        InMemoryRewriter rewriter = new InMemoryRewriter("t", "f") {
            @Override
            public long countRemaining() {
                countRemainingCallCount++;
                return -1L;
            }
        };
        for (int i = 1; i <= 3; i++) {
            rewriter.insert(String.format("%02d", i), rawCryptor.encrypt(OLD_KEY, "plain-" + i));
        }
        InMemoryProgressDAO dao = new InMemoryProgressDAO();
        PasswordRotationConfig config = new PasswordRotationConfig();
        config.setBatchSize(10);
        config.setSleepMsBetweenBatch(0);
        PasswordRotationOrchestrator orchestrator = newOrchestrator(dao, rewriter, config);

        orchestrator.runUntilAllDone();

        PasswordRotationProgress p = dao.firstProgress();
        assertThat(p.isDone()).isTrue();
        assertThat(p.getReEncryptedRows()).isEqualTo(3L);
        assertThat(p.getTotalRows())
            .as("rewriter 返回 -1 时不写入 totalRows，保持 null")
            .isNull();
    }

    @Test
    void rewriterReturningUnchangedSkipsUpdateAndCountsAsSkipped() {
        // rewriter 显式告知"本行无需变更"时，orchestrator 必须跳过 updateRow，
        // 不能以原值更新（典型场景：cron_job.variable_value JSON 不含 CIPHER 子项）
        java.util.concurrent.atomic.AtomicInteger updateRowCallCount = new java.util.concurrent.atomic.AtomicInteger();
        InMemoryRewriter rewriter = new InMemoryRewriter("t", "f") {
            @Override
            public ReEncryptResult reEncryptToActive(SymmetricCryptoService svc, String value) {
                return ReEncryptResult.unchanged();
            }

            @Override
            public int updateRow(String pkCursor, String oldCipher, String newCipher) {
                updateRowCallCount.incrementAndGet();
                return super.updateRow(pkCursor, oldCipher, newCipher);
            }
        };
        rewriter.insert("1", "{\"vars\":[{\"type\":\"HOST_LIST\",\"value\":\"h\"}]}");
        rewriter.insert("2", "{\"vars\":[]}");

        InMemoryProgressDAO dao = new InMemoryProgressDAO();
        PasswordRotationConfig config = new PasswordRotationConfig();
        config.setBatchSize(10);
        config.setSleepMsBetweenBatch(0);
        PasswordRotationOrchestrator orchestrator = newOrchestrator(dao, rewriter, config);

        orchestrator.runUntilAllDone();

        PasswordRotationProgress p = dao.firstProgress();
        assertThat(p.isDone()).isTrue();
        assertThat(p.getProcessedRows()).isEqualTo(2L);
        assertThat(p.getReEncryptedRows()).as("unchanged 不应计入 reEncrypted").isZero();
        assertThat(p.getSkippedRows()).as("unchanged 必须计入 skipped").isEqualTo(2L);
        assertThat(updateRowCallCount.get())
            .as("rewriter 返回 unchanged 时 orchestrator 不应再调 updateRow")
            .isZero();
    }

    private PasswordRotationOrchestrator newOrchestrator(InMemoryProgressDAO dao,
                                                    FieldRewriter rewriter,
                                                    PasswordRotationConfig config) {
        return new PasswordRotationOrchestrator(
            cryptoService,
            cryptoConfigService,
            dao,
            Collections.singletonList(rewriter),
            config,
            null  // 不上报指标
        );
    }

    /**
     * 测试断言专用：判定密文是否能被当前主密钥<strong>单次</strong>解密成功
     * （等价于"已使用主密钥加密"）。
     *
     * <p>不能用 {@code cryptoService.decrypt(...)} 替代，因为后者会走主密钥→历史密码的
     * 试错链，旧密钥加密的密文也能解密成功，无法用来判定是否"已是主密钥加密"。
     */
    private boolean encryptedWithActiveKey(String cipher) {
        if (StringUtils.isEmpty(cipher)) {
            return true;
        }
        try {
            rawCryptor.decrypt(ACTIVE_KEY, cipher);
            return true;
        } catch (CryptoException e) {
            return false;
        }
    }

    /**
     * 内存中的进度 DAO：按 fingerprint+table+field 维护一行记录
     */
    private static class InMemoryProgressDAO implements PasswordRotationProgressDAO {
        private final Map<String, PasswordRotationProgress> store = new HashMap<>();
        private long nextId = 1L;

        @Override
        public synchronized PasswordRotationProgress loadOrCreate(String fp, String table, String field) {
            String key = fp + "|" + table + "|" + field;
            PasswordRotationProgress p = store.get(key);
            if (p == null) {
                p = new PasswordRotationProgress();
                p.setId(nextId++);
                p.setTargetPasswordFingerprint(fp);
                p.setTableName(table);
                p.setFieldName(field);
                p.setStatus(PasswordRotationProgressStatus.PENDING.name());
                store.put(key, p);
            }
            return p;
        }

        @Override
        public synchronized void updateProgress(PasswordRotationProgress progress) {
            String key = progress.getTargetPasswordFingerprint() + "|"
                + progress.getTableName() + "|" + progress.getFieldName();
            store.put(key, progress);
        }

        PasswordRotationProgress firstProgress() {
            return store.values().iterator().next();
        }
    }

    /**
     * 内存中的表 + FieldRewriter 实现：行按 pk 降序返回
     */
    private static class InMemoryRewriter implements FieldRewriter {
        private final String tableName;
        private final String fieldName;
        final java.util.TreeMap<String, String> rows = new java.util.TreeMap<>();
        int countRemainingCallCount = 0;

        InMemoryRewriter(String tableName, String fieldName) {
            this.tableName = tableName;
            this.fieldName = fieldName;
        }

        void insert(String pk, String cipher) {
            rows.put(pk, cipher);
        }

        @Override
        public String tableName() {
            return tableName;
        }

        @Override
        public String fieldName() {
            return fieldName;
        }

        @Override
        public List<FieldBatchRow> fetchBatch(String lastProcessedPkCursor, int batchSize) {
            java.util.NavigableMap<String, String> scanMap = lastProcessedPkCursor == null
                ? rows.descendingMap()
                : rows.headMap(lastProcessedPkCursor, false).descendingMap();
            List<FieldBatchRow> result = new ArrayList<>();
            int n = 0;
            for (Map.Entry<String, String> e : scanMap.entrySet()) {
                if (n++ >= batchSize) {
                    break;
                }
                result.add(new FieldBatchRow(e.getKey(), e.getValue()));
            }
            return result;
        }

        @Override
        public int updateRow(String pkCursor, String oldCipher, String newCipher) {
            String current = rows.get(pkCursor);
            if (current == null || !current.equals(oldCipher)) {
                return 0;
            }
            rows.put(pkCursor, newCipher);
            return 1;
        }

        @Override
        public long countRemaining() {
            countRemainingCallCount++;
            return rows.size();
        }
    }
}
