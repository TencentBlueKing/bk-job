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

package com.tencent.bk.job.common.sharding.mysql.config;

import com.tencent.bk.job.common.sharding.mysql.ShardingConfigParseException;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
import org.apache.shardingsphere.infra.config.algorithm.AlgorithmConfiguration;
import org.apache.shardingsphere.infra.config.mode.ModeConfiguration;
import org.apache.shardingsphere.infra.config.rule.RuleConfiguration;
import org.apache.shardingsphere.mode.repository.standalone.StandalonePersistRepositoryConfiguration;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableReferenceRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.keygen.KeyGenerateStrategyConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.HintShardingStrategyConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.ShardingStrategyConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;
import org.jooq.ConnectionProvider;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * 分库分表组件 ShardingSphere 配置
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ShardingProperties.class})
@ConditionalOnProperty(value = "mysql.sharding.enabled", havingValue = "true")
@Slf4j
public class ShardingDatasourceAutoConfiguration {

    @Qualifier("jobShardingTransactionManager")
    @Bean(name = "jobShardingTransactionManager")
    @DependsOn("jobShardingDataSource")
    public DataSourceTransactionManager transactionManager(
        @Qualifier("jobShardingDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Qualifier("job-sharding-jdbc-template")
    @Bean(name = "job-sharding-jdbc-template")
    public JdbcTemplate jdbcTemplate(@Qualifier("jobShardingDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Qualifier("job-sharding-dsl-context")
    @Bean(name = "job-sharding-dsl-context")
    public DSLContext dslContext(@Qualifier("job-sharding-jooq-conf") org.jooq.Configuration configuration) {
        return new DefaultDSLContext(configuration);
    }

    @Qualifier("job-sharding-jooq-conf")
    @Bean(name = "job-sharding-jooq-conf")
    public org.jooq.Configuration jooqConf(
        @Qualifier("job-sharding-conn-provider") ConnectionProvider connectionProvider) {
        return new DefaultConfiguration().derive(connectionProvider).derive(SQLDialect.MYSQL);
    }

    @Qualifier("job-sharding-conn-provider")
    @Bean(name = "job-sharding-conn-provider")
    public ConnectionProvider connectionProvider(
        @Qualifier("jobShardingTransactionAwareDataSource") DataSource dataSource) {
        return new DataSourceConnectionProvider(dataSource);
    }

    @Qualifier("jobShardingTransactionAwareDataSource")
    @Bean(name = "jobShardingTransactionAwareDataSource")
    public TransactionAwareDataSourceProxy
    transactionAwareDataSourceProxy(@Qualifier("jobShardingDataSource") DataSource dataSource) {
        return new TransactionAwareDataSourceProxy(dataSource);
    }
}
