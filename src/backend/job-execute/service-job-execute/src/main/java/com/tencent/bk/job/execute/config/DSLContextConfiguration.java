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

package com.tencent.bk.job.execute.config;

import com.tencent.bk.job.common.mysql.MySQLProperties;
import com.tencent.bk.job.common.mysql.dynamic.ds.HorizontalShardingDSLContextProvider;
import com.tencent.bk.job.common.mysql.dynamic.ds.MigrateDynamicDSLContextProvider;
import com.tencent.bk.job.common.mysql.dynamic.ds.StandaloneDSLContextProvider;
import com.tencent.bk.job.common.mysql.dynamic.ds.VerticalShardingDSLContextProvider;
import com.tencent.bk.job.common.sharding.mysql.config.ShardingDataSourceFactory;
import com.tencent.bk.job.common.sharding.mysql.config.ShardingProperties;
import com.tencent.bk.job.common.util.toggle.prop.PropToggleStore;
import com.tencent.bk.job.execute.dao.common.DSLContextProviderFactory;
import com.tencent.bk.job.execute.dao.common.JobExecuteVerticalShardingDSLContextProvider;
import com.tencent.bk.job.execute.dao.common.ShardingDbMigrateAspect;
import com.tencent.bk.job.execute.dao.sharding.ShardingMigrationRwModeMgr;
import lombok.extern.slf4j.Slf4j;
import org.jooq.ConnectionProvider;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
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
import java.sql.SQLException;

@Slf4j
@Configuration(value = "jobExecuteDSLContextConfiguration")
public class DSLContextConfiguration {

    @ConditionalOnProperty(value = "mysql.standalone.enabled", havingValue = "true")
    protected static class StandaloneDslContextConfiguration {
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
            return new DefaultConfiguration()
                .derive(connectionProvider)
                .derive(SQLDialect.MYSQL)
                .set(new Settings()
                    .withRenderCatalog(false)
                    .withRenderSchema(false)
                );
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

        @Qualifier("job-execute-standalone-dsl-context-provider")
        @Bean(name = "job-execute-standalone-dsl-context-provider")
        public StandaloneDSLContextProvider standaloneDSLContextProvider(
            @Qualifier("job-execute-dsl-context") DSLContext dslContext
        ) {
            log.info("Init StandaloneDSLContextProvider");
            return new StandaloneDSLContextProvider(dslContext);
        }
    }

    @ConditionalOnProperty(value = "mysql.verticalSharding.enabled", havingValue = "true")
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

        @Qualifier("job-execute-vertical-sharding-dsl-context-provider")
        @Bean(name = "job-execute-vertical-sharding-dsl-context-provider")
        public VerticalShardingDSLContextProvider verticalShardingDSLContextProvider(
            @Qualifier("job-execute-dsl-context-a") DSLContext dslContextA,
            @Qualifier("job-execute-dsl-context-b") DSLContext dslContextB,
            @Qualifier("job-execute-dsl-context-c") DSLContext dslContextC
        ) {
            log.info("Init JobExecuteVerticalShardingDSLContextProvider");
            return new JobExecuteVerticalShardingDSLContextProvider(
                dslContextA,
                dslContextB,
                dslContextC
            );
        }
    }

    /**
     * 水平分库分表配置
     */
    @ConditionalOnProperty(value = "mysql.sharding.enabled", havingValue = "true")
    protected static class HorizontalDslContextConfiguration {

        @Bean("jobExecuteShardingDataSource")
        DataSource jobExecuteShardingDataSource(ShardingProperties shardingProperties) throws SQLException {
            ShardingProperties.DatabaseProperties databaseProperties =
                shardingProperties.getDatabases().get("job_execute");
            log.info("Init jobExecuteShardingDataSource");
            return ShardingDataSourceFactory.createDataSource(databaseProperties);
        }

        @Qualifier("jobExecuteShardingTransactionManager")
        @Bean(name = "jobExecuteShardingTransactionManager")
        @DependsOn("jobExecuteShardingDataSource")
        public DataSourceTransactionManager transactionManager(
            @Qualifier("jobExecuteShardingDataSource") DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Qualifier("jobExecuteShardingJdbcTemplate")
        @Bean(name = "jobExecuteShardingJdbcTemplate")
        public JdbcTemplate jdbcTemplate(@Qualifier("jobExecuteShardingDataSource") DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Qualifier("jobExecuteShardingDslContext")
        @Bean(name = "jobExecuteShardingDslContext")
        public DSLContext dslContext(@Qualifier("jobExecuteShardingJooqConf") org.jooq.Configuration configuration) {
            return new DefaultDSLContext(configuration);
        }

        @Qualifier("jobExecuteShardingJooqConf")
        @Bean(name = "jobExecuteShardingJooqConf")
        public org.jooq.Configuration jooqConf(
            @Qualifier("jobExecuteShardingConnProvider") ConnectionProvider connectionProvider) {
            return new DefaultConfiguration().derive(connectionProvider).derive(SQLDialect.MYSQL);
        }

        @Qualifier("jobExecuteShardingConnProvider")
        @Bean(name = "jobExecuteShardingConnProvider")
        public ConnectionProvider connectionProvider(
            @Qualifier("jobShardingTransactionAwareDataSource") DataSource dataSource) {
            return new DataSourceConnectionProvider(dataSource);
        }

        @Qualifier("jobExecuteShardingTransactionAwareDataSource")
        @Bean(name = "jobExecuteShardingTransactionAwareDataSource")
        public TransactionAwareDataSourceProxy
        transactionAwareDataSourceProxy(@Qualifier("jobExecuteShardingDataSource") DataSource dataSource) {
            return new TransactionAwareDataSourceProxy(dataSource);
        }

        @Qualifier("jobExecuteShardingDslContextProvider")
        @Bean(name = "jobExecuteShardingDslContextProvider")
        public HorizontalShardingDSLContextProvider shardingDSLContextProvider(
            @Qualifier("jobExecuteShardingDslContext") DSLContext dslContext
        ) {
            log.info("Init HorizontalShardingDSLContextProvider");
            return new HorizontalShardingDSLContextProvider(dslContext);
        }
    }


    /**
     * Db 水平分库分表迁移配置
     */
    @ConditionalOnProperty(value = "mysql.migration.enabled", havingValue = "true")
    protected static class DbMigratingConfiguration {
        @Bean("jobExecuteMigrateDynamicDSLContextProvider")
        public MigrateDynamicDSLContextProvider migrateDynamicDSLContextProvider() {
            log.info("Init MigrateDynamicDSLContextProvider");
            return new MigrateDynamicDSLContextProvider();
        }

        @Bean("")
        public ShardingMigrationRwModeMgr shardingMigrationRwModeMgr(PropToggleStore propToggleStore) {
            log.info("Init ShardingMigrationRwModeMgr");
            return new ShardingMigrationRwModeMgr(propToggleStore);
        }

        @Bean("shardingDbMigrateAspect")
        public ShardingDbMigrateAspect shardingDbMigrateAspect(
            @Qualifier("jobExecuteMigrateDynamicDSLContextProvider")
            MigrateDynamicDSLContextProvider migrateDSLContextDynamicProvider,
            MySQLProperties mySQLProperties,
            ObjectProvider<StandaloneDSLContextProvider> standaloneDSLContextProviderObjectProvider,
            ObjectProvider<VerticalShardingDSLContextProvider> verticalShardingDSLContextProviderObjectProvider,
            HorizontalShardingDSLContextProvider horizontalShardingDSLContextProvider,
            ShardingMigrationRwModeMgr shardingMigrationRwModeMgr) {
            log.info("Init ShardingDbMigrateAspect");
            return new ShardingDbMigrateAspect(
                migrateDSLContextDynamicProvider,
                standaloneDSLContextProviderObjectProvider.getIfAvailable(),
                verticalShardingDSLContextProviderObjectProvider.getIfAvailable(),
                horizontalShardingDSLContextProvider,
                mySQLProperties,
                shardingMigrationRwModeMgr
            );
        }
    }

    @Bean("jobExecuteDslContextProviderFactory")
    public DSLContextProviderFactory dslContextProviderFactory(
        ObjectProvider<StandaloneDSLContextProvider> standaloneDSLContextProviderObjectProvider,
        ObjectProvider<VerticalShardingDSLContextProvider> verticalShardingDSLContextProviderObjectProvider,
        ObjectProvider<HorizontalShardingDSLContextProvider> horizontalShardingDSLContextProviderObjectProvider,
        ObjectProvider<MigrateDynamicDSLContextProvider> migrateDynamicDSLContextProviderObjectProvider,
        MySQLProperties mySQLProperties
    ) {
        log.info("Init JobExecuteDslContextProviderFactory");
        return new DSLContextProviderFactory(
            standaloneDSLContextProviderObjectProvider.getIfAvailable(),
            verticalShardingDSLContextProviderObjectProvider.getIfAvailable(),
            horizontalShardingDSLContextProviderObjectProvider.getIfAvailable(),
            migrateDynamicDSLContextProviderObjectProvider.getIfAvailable(),
            mySQLProperties
        );
    }
}
