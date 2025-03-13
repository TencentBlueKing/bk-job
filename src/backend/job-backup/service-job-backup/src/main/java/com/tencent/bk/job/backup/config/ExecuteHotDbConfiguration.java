/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.job.backup.config;

import com.tencent.bk.job.backup.archive.ArchiveTablePropsStorage;
import com.tencent.bk.job.backup.archive.JobInstanceSubTableArchivers;
import com.tencent.bk.job.backup.archive.dao.JobInstanceColdDAO;
import com.tencent.bk.job.backup.archive.dao.ds.JobExecuteVerticalShardingDSLContextProvider;
import com.tencent.bk.job.backup.archive.dao.impl.FileSourceTaskLogRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.GseFileAgentTaskRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.GseFileExecuteObjTaskRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.GseScriptAgentTaskRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.GseScriptExecuteObjTaskRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.GseTaskRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.JobInstanceHotRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.OperationLogRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.RollingConfigRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.StepInstanceConfirmRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.StepInstanceFileRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.StepInstanceRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.StepInstanceRollingTaskRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.StepInstanceScriptRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.StepInstanceVariableRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.TaskInstanceHostRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.TaskInstanceVariableRecordDAO;
import com.tencent.bk.job.backup.archive.impl.FileSourceTaskLogArchiver;
import com.tencent.bk.job.backup.archive.impl.GseFileAgentTaskArchiver;
import com.tencent.bk.job.backup.archive.impl.GseFileExecuteObjTaskArchiver;
import com.tencent.bk.job.backup.archive.impl.GseScriptAgentTaskArchiver;
import com.tencent.bk.job.backup.archive.impl.GseScriptExecuteObjTaskArchiver;
import com.tencent.bk.job.backup.archive.impl.GseTaskArchiver;
import com.tencent.bk.job.backup.archive.impl.OperationLogArchiver;
import com.tencent.bk.job.backup.archive.impl.RollingConfigArchiver;
import com.tencent.bk.job.backup.archive.impl.StepInstanceArchiver;
import com.tencent.bk.job.backup.archive.impl.StepInstanceConfirmArchiver;
import com.tencent.bk.job.backup.archive.impl.StepInstanceFileArchiver;
import com.tencent.bk.job.backup.archive.impl.StepInstanceRollingTaskArchiver;
import com.tencent.bk.job.backup.archive.impl.StepInstanceScriptArchiver;
import com.tencent.bk.job.backup.archive.impl.StepInstanceVariableArchiver;
import com.tencent.bk.job.backup.archive.impl.TaskInstanceHostArchiver;
import com.tencent.bk.job.backup.archive.impl.TaskInstanceVariableArchiver;
import com.tencent.bk.job.common.mysql.dynamic.ds.DSLContextProvider;
import com.tencent.bk.job.common.mysql.dynamic.ds.DataSourceMode;
import com.tencent.bk.job.common.mysql.dynamic.ds.StandaloneDSLContextProvider;
import com.tencent.bk.job.common.mysql.dynamic.ds.VerticalShardingDSLContextProvider;
import lombok.extern.slf4j.Slf4j;
import org.jooq.ConnectionProvider;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;

/**
 * 归档-job-execute 热 DB 配置
 */
@Configuration("executeHotDbConfiguration")
@ConditionalOnExpression("${job.backup.archive.execute.enabled:false}")
@Slf4j
public class ExecuteHotDbConfiguration {

    @ConditionalOnProperty(value = "job.backup.archive.execute.mariadb.dataSourceMode",
        havingValue = DataSourceMode.Constants.STANDALONE, matchIfMissing = false)
    protected static class JobExecuteStandaloneDslContextConfiguration {
        @Qualifier("job-execute-data-source")
        @Bean(name = "job-execute-data-source")
        @ConfigurationProperties(prefix = "spring.datasource.job-execute")
        public DataSource dataSource() {
            return DataSourceBuilder.create().build();
        }

        @Qualifier("job-execute-transaction-manager")
        @Bean(name = "job-execute-transaction-manager")
        @DependsOn("job-execute-data-source")
        public DataSourceTransactionManager transactionManager(
            @Qualifier("job-execute-data-source") DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Qualifier("job-execute-jdbc-template")
        @Bean(name = "job-execute-jdbc-template")
        public JdbcTemplate jdbcTemplate(@Qualifier("job-execute-data-source") DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Qualifier("job-execute-dsl-context")
        @Bean(name = "job-execute-dsl-context")
        public DSLContext dslContext(@Qualifier("job-execute-jooq-conf") org.jooq.Configuration configuration) {
            log.info("Init DSLContext job-execute-standalone");
            return new DefaultDSLContext(configuration);
        }

        @Qualifier("job-execute-jooq-conf")
        @Bean(name = "job-execute-jooq-conf")
        public org.jooq.Configuration jooqConf(
            @Qualifier("job-execute-conn-provider") ConnectionProvider connectionProvider) {
            return new DefaultConfiguration().derive(connectionProvider).derive(SQLDialect.MYSQL);
        }

        @Qualifier("job-execute-conn-provider")
        @Bean(name = "job-execute-conn-provider")
        public ConnectionProvider connectionProvider(
            @Qualifier("job-execute-transaction-aware-data-source") DataSource dataSource) {
            return new DataSourceConnectionProvider(dataSource);
        }

        @Qualifier("job-execute-transaction-aware-data-source")
        @Bean(name = "job-execute-transaction-aware-data-source")
        public TransactionAwareDataSourceProxy
        transactionAwareDataSourceProxy(@Qualifier("job-execute-data-source") DataSource dataSource) {
            return new TransactionAwareDataSourceProxy(dataSource);
        }

        @Qualifier("job-execute-dsl-context-provider")
        @Bean(name = "job-execute-dsl-context-provider")
        public StandaloneDSLContextProvider standaloneDSLContextProvider(
            @Qualifier("job-execute-dsl-context") DSLContext dslContext
        ) {
            log.info("Init StandaloneDSLContextProvider");
            return new StandaloneDSLContextProvider(dslContext);
        }
    }

    @ConditionalOnProperty(value = "job.backup.archive.execute.mariadb.dataSourceMode",
        havingValue = DataSourceMode.Constants.VERTICAL_SHARDING, matchIfMissing = false)
    protected static class VerticalDslContextConfiguration {
        // 配置垂直分片数据源-a
        @Qualifier("job-execute-data-source-a")
        @Bean(name = "job-execute-data-source-a")
        @ConfigurationProperties(prefix = "spring.datasource.job-execute-vertical-a")
        public DataSource dataSourceA() {
            return DataSourceBuilder.create().build();
        }

        @Qualifier("job-execute-transaction-manager-a")
        @Bean(name = "job-execute-transaction-manager-a")
        @DependsOn("job-execute-data-source-a")
        public DataSourceTransactionManager transactionManagerA(
            @Qualifier("job-execute-data-source-a") DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Qualifier("job-execute-jdbc-template-a")
        @Bean(name = "job-execute-jdbc-template-a")
        public JdbcTemplate jdbcTemplateA(@Qualifier("job-execute-data-source-a") DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Qualifier("job-execute-dsl-context-a")
        @Bean(name = "job-execute-dsl-context-a")
        public DSLContext dslContextA(@Qualifier("job-execute-jooq-conf-a") org.jooq.Configuration configuration) {
            log.info("Init DSLContext job-execute-vertical-a");
            return new DefaultDSLContext(configuration);
        }

        @Qualifier("job-execute-jooq-conf-a")
        @Bean(name = "job-execute-jooq-conf-a")
        public org.jooq.Configuration jooqConfA(
            @Qualifier("job-execute-conn-provider-a") ConnectionProvider connectionProvider) {
            return new DefaultConfiguration().derive(connectionProvider).derive(SQLDialect.MYSQL);
        }

        @Qualifier("job-execute-conn-provider-a")
        @Bean(name = "job-execute-conn-provider-a")
        public ConnectionProvider connectionProviderA(
            @Qualifier("job-execute-transaction-aware-data-source-a") DataSource dataSource) {
            return new DataSourceConnectionProvider(dataSource);
        }

        @Qualifier("job-execute-transaction-aware-data-source-a")
        @Bean(name = "job-execute-transaction-aware-data-source-a")
        public TransactionAwareDataSourceProxy
        transactionAwareDataSourceProxyA(@Qualifier("job-execute-data-source-a") DataSource dataSource) {
            return new TransactionAwareDataSourceProxy(dataSource);
        }

        // 配置垂直分片数据源-b
        @Qualifier("job-execute-data-source-b")
        @Bean(name = "job-execute-data-source-b")
        @ConfigurationProperties(prefix = "spring.datasource.job-execute-vertical-b")
        public DataSource dataSourceB() {
            return DataSourceBuilder.create().build();
        }

        @Qualifier("job-execute-transaction-manager-b")
        @Bean(name = "job-execute-transaction-manager-b")
        @DependsOn("job-execute-data-source-b")
        public DataSourceTransactionManager transactionManagerB(
            @Qualifier("job-execute-data-source-b") DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Qualifier("job-execute-jdbc-template-b")
        @Bean(name = "job-execute-jdbc-template-b")
        public JdbcTemplate jdbcTemplateB(@Qualifier("job-execute-data-source-b") DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Qualifier("job-execute-dsl-context-b")
        @Bean(name = "job-execute-dsl-context-b")
        public DSLContext dslContextB(@Qualifier("job-execute-jooq-conf-b") org.jooq.Configuration configuration) {
            log.info("Init DSLContext job-execute-vertical-b");
            return new DefaultDSLContext(configuration);
        }

        @Qualifier("job-execute-jooq-conf-b")
        @Bean(name = "job-execute-jooq-conf-b")
        public org.jooq.Configuration jooqConfB(
            @Qualifier("job-execute-conn-provider-b") ConnectionProvider connectionProvider) {
            return new DefaultConfiguration().derive(connectionProvider).derive(SQLDialect.MYSQL);
        }

        @Qualifier("job-execute-conn-provider-b")
        @Bean(name = "job-execute-conn-provider-b")
        public ConnectionProvider connectionProviderB(
            @Qualifier("job-execute-transaction-aware-data-source-b") DataSource dataSource) {
            return new DataSourceConnectionProvider(dataSource);
        }

        @Qualifier("job-execute-transaction-aware-data-source-b")
        @Bean(name = "job-execute-transaction-aware-data-source-b")
        public TransactionAwareDataSourceProxy
        transactionAwareDataSourceProxyB(@Qualifier("job-execute-data-source-b") DataSource dataSource) {
            return new TransactionAwareDataSourceProxy(dataSource);
        }

        // 配置垂直分片数据源-c
        @Qualifier("job-execute-data-source-c")
        @Bean(name = "job-execute-data-source-c")
        @ConfigurationProperties(prefix = "spring.datasource.job-execute-vertical-c")
        public DataSource dataSourceC() {
            return DataSourceBuilder.create().build();
        }

        @Qualifier("job-execute-transaction-manager-c")
        @Bean(name = "job-execute-transaction-manager-c")
        @DependsOn("job-execute-data-source-c")
        public DataSourceTransactionManager transactionManagerC(
            @Qualifier("job-execute-data-source-c") DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Qualifier("job-execute-jdbc-template-c")
        @Bean(name = "job-execute-jdbc-template-c")
        public JdbcTemplate jdbcTemplateC(@Qualifier("job-execute-data-source-c") DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Qualifier("job-execute-dsl-context-c")
        @Bean(name = "job-execute-dsl-context-c")
        public DSLContext dslContextC(@Qualifier("job-execute-jooq-conf-c") org.jooq.Configuration configuration) {
            log.info("Init DSLContext job-execute-vertical-c");
            return new DefaultDSLContext(configuration);
        }

        @Qualifier("job-execute-jooq-conf-c")
        @Bean(name = "job-execute-jooq-conf-c")
        public org.jooq.Configuration jooqConfC(
            @Qualifier("job-execute-conn-provider-c") ConnectionProvider connectionProvider) {
            return new DefaultConfiguration().derive(connectionProvider).derive(SQLDialect.MYSQL);
        }

        @Qualifier("job-execute-conn-provider-c")
        @Bean(name = "job-execute-conn-provider-c")
        public ConnectionProvider connectionProviderC(
            @Qualifier("job-execute-transaction-aware-data-source-c") DataSource dataSource) {
            return new DataSourceConnectionProvider(dataSource);
        }

        @Qualifier("job-execute-transaction-aware-data-source-c")
        @Bean(name = "job-execute-transaction-aware-data-source-c")
        public TransactionAwareDataSourceProxy
        transactionAwareDataSourceProxyC(@Qualifier("job-execute-data-source-c") DataSource dataSource) {
            return new TransactionAwareDataSourceProxy(dataSource);
        }

        @Qualifier("job-execute-dsl-context-provider")
        @Bean(name = "job-execute-dsl-context-provider")
        public VerticalShardingDSLContextProvider verticalShardingDSLContextProvider(
            @Qualifier("job-execute-dsl-context-a") DSLContext dslContextA,
            @Qualifier("job-execute-dsl-context-b") DSLContext dslContextB,
            @Qualifier("job-execute-dsl-context-c") DSLContext dslContextC
        ) {
            log.info("Init VerticalShardingDSLContextProvider");
            return new JobExecuteVerticalShardingDSLContextProvider(
                dslContextA,
                dslContextB,
                dslContextC
            );
        }
    }

    /**
     * job-execute hot DB DAO 配置
     */
    protected static class ExecuteHotDaoConfiguration {

        @Bean(name = "taskInstanceRecordDAO")
        public JobInstanceHotRecordDAO taskInstanceRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init TaskInstanceRecordDAO");
            return new JobInstanceHotRecordDAO(dslContextProvider);
        }

        @Bean(name = "stepInstanceRecordDAO")
        public StepInstanceRecordDAO stepInstanceRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init StepInstanceRecordDAO");
            return new StepInstanceRecordDAO(dslContextProvider);
        }

        @Bean
        public StepInstanceArchiver stepInstanceArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            StepInstanceRecordDAO stepInstanceRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new StepInstanceArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                stepInstanceRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "stepInstanceScriptRecordDAO")
        public StepInstanceScriptRecordDAO stepInstanceScriptRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init StepInstanceScriptRecordDAO");
            return new StepInstanceScriptRecordDAO(dslContextProvider);
        }

        @Bean
        public StepInstanceScriptArchiver stepInstanceScriptArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            StepInstanceScriptRecordDAO stepInstanceScriptRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new StepInstanceScriptArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                stepInstanceScriptRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "stepInstanceFileRecordDAO")
        public StepInstanceFileRecordDAO stepInstanceFileRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init StepInstanceFileRecordDAO");
            return new StepInstanceFileRecordDAO(dslContextProvider);
        }

        @Bean
        public StepInstanceFileArchiver stepInstanceFileArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            StepInstanceFileRecordDAO stepInstanceFileRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new StepInstanceFileArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                stepInstanceFileRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "stepInstanceConfirmRecordDAO")
        public StepInstanceConfirmRecordDAO stepInstanceConfirmRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init StepInstanceConfirmRecordDAO");
            return new StepInstanceConfirmRecordDAO(dslContextProvider);
        }

        @Bean
        public StepInstanceConfirmArchiver stepInstanceConfirmArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            StepInstanceConfirmRecordDAO stepInstanceConfirmRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new StepInstanceConfirmArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                stepInstanceConfirmRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "stepInstanceVariableRecordDAO")
        public StepInstanceVariableRecordDAO stepInstanceVariableRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init StepInstanceVariableRecordDAO");
            return new StepInstanceVariableRecordDAO(dslContextProvider);
        }

        @Bean
        public StepInstanceVariableArchiver stepInstanceVariableArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            StepInstanceVariableRecordDAO stepInstanceVariableRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new StepInstanceVariableArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                stepInstanceVariableRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "taskInstanceVariableRecordDAO")
        public TaskInstanceVariableRecordDAO taskInstanceVariableRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init TaskInstanceVariableRecordDAO");
            return new TaskInstanceVariableRecordDAO(dslContextProvider);
        }

        @Bean
        public TaskInstanceVariableArchiver taskInstanceVariableArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            TaskInstanceVariableRecordDAO taskInstanceVariableRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new TaskInstanceVariableArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                taskInstanceVariableRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "operationLogRecordDAO")
        public OperationLogRecordDAO operationLogRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init OperationLogRecordDAO");
            return new OperationLogRecordDAO(dslContextProvider);
        }

        @Bean
        public OperationLogArchiver operationLogArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            OperationLogRecordDAO operationLogRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new OperationLogArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                operationLogRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "fileSourceTaskLogRecordDAO")
        public FileSourceTaskLogRecordDAO fileSourceTaskLogRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init FileSourceTaskRecordDAO");
            return new FileSourceTaskLogRecordDAO(dslContextProvider);
        }

        @Bean
        public FileSourceTaskLogArchiver fileSourceTaskLogArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            FileSourceTaskLogRecordDAO fileSourceTaskLogRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new FileSourceTaskLogArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                fileSourceTaskLogRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "gseTaskRecordDAO")
        public GseTaskRecordDAO gseTaskRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init GseTaskRecordDAO");
            return new GseTaskRecordDAO(dslContextProvider);
        }

        @Bean
        public GseTaskArchiver gseTaskArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            GseTaskRecordDAO gseTaskRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new GseTaskArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                gseTaskRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "gseScriptAgentTaskRecordDAO")
        public GseScriptAgentTaskRecordDAO gseScriptAgentTaskRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init GseScriptAgentTaskRecordDAO");
            return new GseScriptAgentTaskRecordDAO(dslContextProvider);
        }

        @Bean
        public GseScriptAgentTaskArchiver gseScriptAgentTaskArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            GseScriptAgentTaskRecordDAO gseScriptAgentTaskRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new GseScriptAgentTaskArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                gseScriptAgentTaskRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "gseFileAgentTaskRecordDAO")
        public GseFileAgentTaskRecordDAO gseFileAgentTaskRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init GseFileAgentTaskRecordDAO");
            return new GseFileAgentTaskRecordDAO(dslContextProvider);
        }

        @Bean
        public GseFileAgentTaskArchiver gseFileAgentTaskArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            GseFileAgentTaskRecordDAO gseFileAgentTaskRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new GseFileAgentTaskArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                gseFileAgentTaskRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "gseScriptExecuteObjTaskRecordDAO")
        public GseScriptExecuteObjTaskRecordDAO gseScriptExecuteObjTaskRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init GseScriptExecuteObjTaskRecordDAO");
            return new GseScriptExecuteObjTaskRecordDAO(dslContextProvider);
        }

        @Bean
        public GseScriptExecuteObjTaskArchiver gseScriptExecuteObjTaskArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            GseScriptExecuteObjTaskRecordDAO gseScriptExecuteObjTaskRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new GseScriptExecuteObjTaskArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                gseScriptExecuteObjTaskRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "gseFileExecuteObjTaskRecordDAO")
        public GseFileExecuteObjTaskRecordDAO gseFileExecuteObjTaskRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init GseFileExecuteObjTaskRecordDAO");
            return new GseFileExecuteObjTaskRecordDAO(dslContextProvider);
        }

        @Bean
        public GseFileExecuteObjTaskArchiver gseFileExecuteObjTaskArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            GseFileExecuteObjTaskRecordDAO gseFileExecuteObjTaskRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new GseFileExecuteObjTaskArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                gseFileExecuteObjTaskRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "stepInstanceRollingTaskRecordDAO")
        public StepInstanceRollingTaskRecordDAO stepInstanceRollingTaskRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init StepInstanceRollingTaskRecordDAO");
            return new StepInstanceRollingTaskRecordDAO(dslContextProvider);
        }

        @Bean
        public StepInstanceRollingTaskArchiver stepInstanceRollingTaskArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            StepInstanceRollingTaskRecordDAO stepInstanceRollingTaskRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new StepInstanceRollingTaskArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                stepInstanceRollingTaskRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "rollingConfigRecordDAO")
        public RollingConfigRecordDAO rollingConfigRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init RollingConfigRecordDAO");
            return new RollingConfigRecordDAO(dslContextProvider);
        }

        @Bean
        public RollingConfigArchiver rollingConfigArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            RollingConfigRecordDAO rollingConfigRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new RollingConfigArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                rollingConfigRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean(name = "taskInstanceHostRecordDAO")
        public TaskInstanceHostRecordDAO taskInstanceHostRecordDAO(
            @Qualifier("job-execute-dsl-context-provider") DSLContextProvider dslContextProvider) {
            log.info("Init TaskInstanceHostRecordDAO");
            return new TaskInstanceHostRecordDAO(dslContextProvider);
        }

        @Bean
        public TaskInstanceHostArchiver taskInstanceHostArchiver(
            ObjectProvider<JobInstanceColdDAO> jobInstanceColdDAOObjectProvider,
            TaskInstanceHostRecordDAO taskInstanceHostRecordDAO,
            ArchiveTablePropsStorage archiveTablePropsStorage
        ) {
            return new TaskInstanceHostArchiver(
                jobInstanceColdDAOObjectProvider.getIfAvailable(),
                taskInstanceHostRecordDAO,
                archiveTablePropsStorage);
        }

        @Bean
        public JobInstanceSubTableArchivers jobInstanceSubTableArchivers(
            FileSourceTaskLogArchiver fileSourceTaskLogArchiver,
            GseFileAgentTaskArchiver gseFileAgentTaskArchiver,
            GseFileExecuteObjTaskArchiver gseFileExecuteObjTaskArchiver,
            GseScriptAgentTaskArchiver gseScriptAgentTaskArchiver,
            GseScriptExecuteObjTaskArchiver gseScriptExecuteObjTaskArchiver,
            GseTaskArchiver gseTaskArchiver,
            OperationLogArchiver operationLogArchiver,
            RollingConfigArchiver rollingConfigArchiver,
            StepInstanceArchiver stepInstanceArchiver,
            StepInstanceConfirmArchiver stepInstanceConfirmArchiver,
            StepInstanceFileArchiver stepInstanceFileArchiver,
            StepInstanceScriptArchiver stepInstanceScriptArchiver,
            StepInstanceRollingTaskArchiver stepInstanceRollingTaskArchiver,
            StepInstanceVariableArchiver stepInstanceVariableArchiver,
            TaskInstanceHostArchiver taskInstanceHostArchiver,
            TaskInstanceVariableArchiver taskInstanceVariableArchiver) {
            return new JobInstanceSubTableArchivers(
                fileSourceTaskLogArchiver,
                gseFileAgentTaskArchiver,
                gseFileExecuteObjTaskArchiver,
                gseScriptAgentTaskArchiver,
                gseScriptExecuteObjTaskArchiver,
                gseTaskArchiver,
                operationLogArchiver,
                rollingConfigArchiver,
                stepInstanceArchiver,
                stepInstanceConfirmArchiver,
                stepInstanceFileArchiver,
                stepInstanceScriptArchiver,
                stepInstanceRollingTaskArchiver,
                stepInstanceVariableArchiver,
                taskInstanceHostArchiver,
                taskInstanceVariableArchiver);
        }

    }
}
