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

import com.tencent.bk.job.common.sharding.mysql.config.MySQLProperties;
import com.tencent.bk.job.common.sharding.mysql.config.ShardingDataSourceFactory;
import com.tencent.bk.job.common.sharding.mysql.config.ShardingProperties;
import lombok.extern.slf4j.Slf4j;
import org.jooq.ConnectionProvider;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;
import java.sql.SQLException;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(value = "mysql.sharding.enabled", havingValue = "true")
@Slf4j
public class ShardingDataSourceConfiguration {

    @ConditionalOnProperty("mysql.sharding.shardingDataSources.job-execute-vertical")
    static class VerticalShardingConfiguration {
        @Bean("job-execute-vertical-data-source")
        public DataSource verticalShardingDataSource(MySQLProperties mySQLProperties) throws SQLException {
            log.info("Init job-execute-vertical sharding datasource start ...");
            ShardingProperties.ShardingDataSource shardingDataSource =
                mySQLProperties.getSharding().getShardingDataSources().get("job-execute-vertical");
            DataSource dataSource = ShardingDataSourceFactory.createShardDataSource(shardingDataSource);
            log.info("Init job-execute-vertical sharding datasource success !");
            return dataSource;
        }

        @Qualifier("job-execute-vertical-transaction-manager")
        @Bean(name = "job-execute-transaction-manager")
        @DependsOn("job-execute-vertical-data-source")
        public DataSourceTransactionManager transactionManager(
            @Qualifier("job-execute-vertical-data-source") DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Qualifier("job-execute-vertical-jdbc-template")
        @Bean(name = "job-execute-vertical-jdbc-template")
        public JdbcTemplate jdbcTemplate(@Qualifier("job-execute-vertical-data-source") DataSource dataSource) {
            return new JdbcTemplate(dataSource);
        }

        @Qualifier("job-execute-vertical-dsl-context")
        @Bean(name = "job-execute-vertical-dsl-context")
        public DSLContext dslContext(
            @Qualifier("job-execute-vertical-jooq-conf") org.jooq.Configuration configuration) {
            return new DefaultDSLContext(configuration);
        }

        @Qualifier("job-execute-vertical-jooq-conf")
        @Bean(name = "job-execute-vertical-jooq-conf")
        public org.jooq.Configuration jooqConf(
            @Qualifier("job-execute-vertical-conn-provider") ConnectionProvider connectionProvider) {
            return new DefaultConfiguration().derive(connectionProvider).derive(SQLDialect.MYSQL);
        }

        @Qualifier("job-execute-vertical-conn-provider")
        @Bean(name = "job-execute-vertical-conn-provider")
        public ConnectionProvider connectionProvider(
            @Qualifier("job-execute-vertical-transaction-aware-data-source") DataSource dataSource) {
            return new DataSourceConnectionProvider(dataSource);
        }

        @Qualifier("job-execute-vertical-transaction-aware-data-source")
        @Bean(name = "job-execute-vertical-transaction-aware-data-source")
        public TransactionAwareDataSourceProxy
        transactionAwareDataSourceProxy(@Qualifier("job-execute-vertical-data-source") DataSource dataSource) {
            return new TransactionAwareDataSourceProxy(dataSource);
        }
    }


}
