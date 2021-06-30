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

import org.jooq.ConnectionProvider;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;

/**
 * @date 2019/09/19
 */
@Configuration
@EnableTransactionManagement
public class BackupDbConfig {
    @Qualifier("job-backup-data-source")
    @Primary
    @Bean(name = "job-backup-data-source")
    @ConfigurationProperties(prefix = "spring.datasource.job-backup")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    @Qualifier("transactionManager")
    @Bean(name = "transactionManager")
    @DependsOn("job-backup-data-source")
    @Primary
    public DataSourceTransactionManager transactionManager(@Qualifier("job-backup-data-source") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Qualifier("job-backup-jdbc-template")
    @Bean(name = "job-backup-jdbc-template")
    public JdbcTemplate jdbcTemplate(@Qualifier("job-backup-data-source") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Qualifier("job-backup-dsl-context")
    @Bean(name = "job-backup-dsl-context")
    public DSLContext dslContext(@Qualifier("job-backup-jooq-conf") org.jooq.Configuration configuration) {
        return new DefaultDSLContext(configuration);
    }

    @Qualifier("job-backup-jooq-conf")
    @Bean(name = "job-backup-jooq-conf")
    public org.jooq.Configuration
    jooqConf(@Qualifier("job-backup-conn-provider") ConnectionProvider connectionProvider) {
        return new DefaultConfiguration().derive(connectionProvider).derive(SQLDialect.MYSQL);
    }

    @Qualifier("job-backup-conn-provider")
    @Bean(name = "job-backup-conn-provider")
    public ConnectionProvider connectionProvider(@Qualifier("job-backup-data-source") DataSource dataSource) {
        return new DataSourceConnectionProvider(dataSource);
    }

}
