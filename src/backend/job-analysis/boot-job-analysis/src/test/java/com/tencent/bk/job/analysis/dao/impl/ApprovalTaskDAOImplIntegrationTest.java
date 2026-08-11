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

package com.tencent.bk.job.analysis.dao.impl;

import com.tencent.bk.job.analysis.approval.consts.ApprovalStatusEnum;
import com.tencent.bk.job.analysis.dao.ApprovalTaskDAO;
import com.tencent.bk.job.analysis.model.dto.ApprovalTaskDTO;
import com.tencent.bk.job.common.mysql.util.JooqConfigurationUtil;
import org.h2.jdbcx.JdbcDataSource;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultDSLContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 审批任务 DAO 集成测试。
 * <p>
 * 这里锁定的三条语义是审批放行链路的地基：CAS 并发只能有一个成功、ticketId 绑定幂等、
 * 清理任务跳过 EXECUTING。任一条有洞都会以"偶发重复执行"或"排障线索被静默删除"的形式暴露，
 * 且都难以复现，必须在 DB 层就锁死。
 * <p>
 * 刻意不用 {@code @SpringBootTest}：本用例要验证的是 SQL 的并发与幂等语义，
 * 而 job-analysis 的完整上下文启动依赖 Redis 等外部组件，把它们拖进来只会让这几条关键断言变得不可靠。
 * 这里直接在 H2 上按 job-analysis 生产环境同一套 jOOQ 配置（{@link JooqConfigurationUtil}、MySQL 方言）
 * 构造 DAO，建表脚本复用 init_schema.sql。
 */
class ApprovalTaskDAOImplIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(ApprovalTaskDAOImplIntegrationTest.class);

    /**
     * 并发放行的线程数。取值只要大于 1 即可暴露问题，取 10 是为了让竞争足够明显
     */
    private static final int CONCURRENT_THREAD_COUNT = 10;

    private static ApprovalTaskDAO approvalTaskDAO;

    @BeforeAll
    static void initDataSource() throws SQLException {
        JdbcDataSource dataSource = new JdbcDataSource();
        // DB_CLOSE_DELAY=-1 让内存库在连接归还后仍然存活；LOCK_TIMEOUT 给并发用例留出等锁时间
        dataSource.setURL("jdbc:h2:mem:job-analysis-approval-task;MODE=MYSQL;NON_KEYWORDS=VALUE;"
            + "DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000");
        dataSource.setUser("job");
        dataSource.setPassword("job_db_password");
        initSchema(dataSource);
        approvalTaskDAO = new ApprovalTaskDAOImpl(new DefaultDSLContext(
            JooqConfigurationUtil.getConfiguration(new DataSourceConnectionProvider(dataSource))));
    }

    private static void initSchema(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("RUNSCRIPT FROM 'classpath:init_schema.sql'");
        }
    }

    @Test
    @DisplayName("并发放行同一个审批任务，只有一个线程 CAS 成功")
    void givenConcurrentConsumeThenOnlyOneThreadSucceeds() throws InterruptedException {
        String approvalTaskId = insertPendingTask(System.currentTimeMillis() + 600_000L);

        long now = System.currentTimeMillis();
        AtomicInteger successCount = new AtomicInteger(0);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_THREAD_COUNT);
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREAD_COUNT);
        try {
            for (int i = 0; i < CONCURRENT_THREAD_COUNT; i++) {
                String approver = "approver_" + i;
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        int affectedRows = approvalTaskDAO.casConsumeToExecuting(
                            approvalTaskId, approver, now, now, now);
                        if (affectedRows == 1) {
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        // 抢锁失败等价于 CAS 失败，不计入成功
                        log.info("Cas consume failed, approver: {}", approver, e);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
            startLatch.countDown();
            assertThat(doneLatch.await(30, TimeUnit.SECONDS)).isTrue();
        } finally {
            executor.shutdownNow();
        }

        assertThat(successCount.get()).isEqualTo(1);
        ApprovalTaskDTO task = approvalTaskDAO.getByApprovalTaskId(approvalTaskId);
        assertThat(task.getStatus()).isEqualTo(ApprovalStatusEnum.EXECUTING.name());
        // 审批人与消费时刻必须来自那次成功的 CAS，不能被失败线程覆盖
        assertThat(task.getApprover()).startsWith("approver_");
        assertThat(task.getConsumedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("已消费的审批任务不能再次 CAS 消费")
    void givenConsumedTaskThenCasConsumeFails() {
        String approvalTaskId = insertPendingTask(System.currentTimeMillis() + 600_000L);
        long now = System.currentTimeMillis();

        assertThat(approvalTaskDAO.casConsumeToExecuting(approvalTaskId, "approver1", now, now, now)).isEqualTo(1);
        assertThat(approvalTaskDAO.casConsumeToExecuting(approvalTaskId, "approver2", now, now, now)).isEqualTo(0);

        ApprovalTaskDTO task = approvalTaskDAO.getByApprovalTaskId(approvalTaskId);
        assertThat(task.getApprover()).isEqualTo("approver1");
    }

    @Test
    @DisplayName("已过期的审批任务不能被 CAS 消费")
    void givenExpiredTaskThenCasConsumeFails() {
        long now = System.currentTimeMillis();
        // 过期判定是 expire_at > now，等于 now 也必须拒绝
        String approvalTaskId = insertPendingTask(now);

        assertThat(approvalTaskDAO.casConsumeToExecuting(approvalTaskId, "approver1", now, now, now)).isEqualTo(0);
        assertThat(approvalTaskDAO.getByApprovalTaskId(approvalTaskId).getStatus())
            .isEqualTo(ApprovalStatusEnum.PENDING.name());
    }

    @Test
    @DisplayName("ticketId 绑定幂等：首次写入生效，后续不同取值一律不覆盖")
    void givenTicketIdBoundThenBindAgainTakesNoEffect() {
        String approvalTaskId = insertPendingTask(System.currentTimeMillis() + 600_000L);

        assertThat(approvalTaskDAO.bindTicketIdIfAbsent(approvalTaskId, "ticket_1")).isEqualTo(1);
        assertThat(approvalTaskDAO.getByApprovalTaskId(approvalTaskId).getApprovalTicketId()).isEqualTo("ticket_1");

        // 换一个单据号再绑：必须不生效，否则单据可被顶替，放行时的绑定校验就形同虚设
        assertThat(approvalTaskDAO.bindTicketIdIfAbsent(approvalTaskId, "ticket_2")).isEqualTo(0);
        // 用原值再绑一次也不应重复写入
        assertThat(approvalTaskDAO.bindTicketIdIfAbsent(approvalTaskId, "ticket_1")).isEqualTo(0);
        assertThat(approvalTaskDAO.getByApprovalTaskId(approvalTaskId).getApprovalTicketId()).isEqualTo("ticket_1");
    }

    @Test
    @DisplayName("清理任务跳过 EXECUTING 状态的任务")
    void givenExecutingTaskThenCleanUpSkipsIt() {
        // 用一段远早于其他用例的 create_time，保证本用例的删除不会波及其他用例的数据
        long baseCreateTime = 1_000L;
        String pendingTaskId = insertTask(ApprovalStatusEnum.PENDING, baseCreateTime + 1);
        String executedTaskId = insertTask(ApprovalStatusEnum.EXECUTED, baseCreateTime + 2);
        String executingTaskId = insertTask(ApprovalStatusEnum.EXECUTING, baseCreateTime + 3);

        int deletedRows = approvalTaskDAO.deleteByCreateTimeBefore(baseCreateTime + 10, 100);

        assertThat(deletedRows).isEqualTo(2);
        assertThat(approvalTaskDAO.getByApprovalTaskId(pendingTaskId)).isNull();
        assertThat(approvalTaskDAO.getByApprovalTaskId(executedTaskId)).isNull();
        // EXECUTING 的任务正是需要人工排障的对象，被静默删掉就再也查不出"到底执行了没有"
        assertThat(approvalTaskDAO.getByApprovalTaskId(executingTaskId)).isNotNull();
    }

    @Test
    @DisplayName("清理任务按 limit 分批删除")
    void givenLimitThenCleanUpDeletesInBatch() {
        long baseCreateTime = 2_000L;
        List<String> taskIds = IntStream.range(0, 5)
            .mapToObj(i -> insertTask(ApprovalStatusEnum.EXECUTED, baseCreateTime + i))
            .collect(Collectors.toList());

        assertThat(approvalTaskDAO.deleteByCreateTimeBefore(baseCreateTime + 10, 2)).isEqualTo(2);
        assertThat(approvalTaskDAO.deleteByCreateTimeBefore(baseCreateTime + 10, 2)).isEqualTo(2);
        assertThat(approvalTaskDAO.deleteByCreateTimeBefore(baseCreateTime + 10, 2)).isEqualTo(1);
        assertThat(approvalTaskDAO.deleteByCreateTimeBefore(baseCreateTime + 10, 2)).isEqualTo(0);

        taskIds.forEach(taskId -> assertThat(approvalTaskDAO.getByApprovalTaskId(taskId)).isNull());
    }

    private String insertPendingTask(long expireAt) {
        return insertTask(ApprovalStatusEnum.PENDING, System.currentTimeMillis(), expireAt);
    }

    private String insertTask(ApprovalStatusEnum status, long createTime) {
        return insertTask(status, createTime, System.currentTimeMillis() + 600_000L);
    }

    private String insertTask(ApprovalStatusEnum status, long createTime, long expireAt) {
        ApprovalTaskDTO task = new ApprovalTaskDTO();
        task.setApprovalTaskId(UUID.randomUUID().toString());
        task.setTenantId("default");
        task.setAppId(2L);
        task.setOperationType("FAST_EXECUTE_SCRIPT");
        task.setOperationParams("{\"bk_scope_type\":\"biz\"}");
        task.setParamsSchemaVersion(1);
        task.setResolvedSummary("{\"totalExecuteObjectCount\":1}");
        task.setCreator("admin");
        task.setAppCode("bk_test");
        task.setApprovalChannel("IMATE");
        task.setStatus(status.name());
        task.setExpireAt(expireAt);
        task.setCreateTime(createTime);
        assertThat(approvalTaskDAO.insertApprovalTask(task)).isGreaterThan(0L);
        return task.getApprovalTaskId();
    }
}
