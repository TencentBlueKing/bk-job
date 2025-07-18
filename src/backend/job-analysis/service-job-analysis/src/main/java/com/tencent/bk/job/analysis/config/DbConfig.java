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

package com.tencent.bk.job.analysis.config;

import com.tencent.bk.job.common.mysql.util.JooqConfigurationUtil;
import org.jooq.ConnectionProvider;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.jooq.impl.DefaultExecuteListenerProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

@Configuration(value = "jobAnalysisDbConfig")
@EnableTransactionManagement
public class DbConfig {
    @Qualifier("job-analysis-data-source")
    @Bean(name = "job-analysis-data-source")
    @ConfigurationProperties(prefix = "spring.datasource.job-analysis")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    @Qualifier("jobAnalysisTransactionManager")
    @Bean(name = "jobAnalysisTransactionManager")
    @DependsOn("job-analysis-data-source")
    public DataSourceTransactionManager transactionManager(
        @Qualifier("job-analysis-data-source") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Qualifier("job-analysis-jdbc-template")
    @Bean(name = "job-analysis-jdbc-template")
    public JdbcTemplate jdbcTemplate(
        @Qualifier("job-analysis-data-source") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Qualifier("job-analysis-dsl-context")
    @Bean(name = "job-analysis-dsl-context")
    public DSLContext dslContext(@Qualifier("job-analysis-jooq-conf") org.jooq.Configuration configuration) {
        return new DefaultDSLContext(configuration);
    }

    @Qualifier("job-analysis-jooq-conf")
    @Bean(name = "job-analysis-jooq-conf")
    public org.jooq.Configuration
    jooqConf(@Qualifier("job-analysis-conn-provider") ConnectionProvider connectionProvider,
             DefaultExecuteListenerProvider jooqExecuteListenerProvider
    ) {
        return JooqConfigurationUtil.getConfiguration(connectionProvider, jooqExecuteListenerProvider);
    }

    @Qualifier("job-analysis-conn-provider")
    @Bean(name = "job-analysis-conn-provider")
    public ConnectionProvider connectionProvider(
        @Qualifier("jobAnalysisTransactionAwareDataSource") DataSource dataSource) {
        return new DataSourceConnectionProvider(dataSource);
    }

    @Qualifier("jobAnalysisTransactionAwareDataSource")
    @Bean(name = "jobAnalysisTransactionAwareDataSource")
    public TransactionAwareDataSourceProxy
    transactionAwareDataSourceProxy(@Qualifier("job-analysis-data-source") DataSource dataSource) {
        return new TransactionAwareDataSourceProxy(dataSource);
    }

}
