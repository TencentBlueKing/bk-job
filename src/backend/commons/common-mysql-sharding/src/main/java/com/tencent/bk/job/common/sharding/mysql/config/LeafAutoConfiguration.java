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

import com.tencent.bk.job.common.sharding.mysql.JooqLeafIdAllocator;
import org.jooq.ConnectionProvider;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;

/**
 * 美团 leaf 分布式 ID 组件自动装配
 */
@Configuration(value = "leafDbConfig")
@ConditionalOnProperty(value = "leaf.enabled", havingValue = "true")
@EnableConfigurationProperties({LeafProperties.class})
public class LeafAutoConfiguration {
    @Qualifier("leaf-data-source")
    @Bean(name = "leaf-data-source")
    @ConfigurationProperties(prefix = "spring.datasource.leaf")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    @Qualifier("leafTransactionManager")
    @Bean(name = "leafTransactionManager")
    @DependsOn("leaf-data-source")
    public DataSourceTransactionManager transactionManager(
        @Qualifier("leaf-data-source") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    @Qualifier("leaf-jdbc-template")
    @Bean(name = "leaf-jdbc-template")
    public JdbcTemplate jdbcTemplate(
        @Qualifier("leaf-data-source") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Qualifier("leaf-dsl-context")
    @Bean(name = "leaf-dsl-context")
    public DSLContext dslContext(@Qualifier("leaf-jooq-conf") org.jooq.Configuration configuration) {
        return new DefaultDSLContext(configuration);
    }

    @Qualifier("leaf-jooq-conf")
    @Bean(name = "leaf-jooq-conf")
    public org.jooq.Configuration
    jooqConf(@Qualifier("leaf-conn-provider") ConnectionProvider connectionProvider) {
        return new DefaultConfiguration().derive(connectionProvider).derive(SQLDialect.MYSQL);
    }

    @Qualifier("leaf-conn-provider")
    @Bean(name = "leaf-conn-provider")
    public ConnectionProvider connectionProvider(
        @Qualifier("leafTransactionAwareDataSource") DataSource dataSource) {
        return new DataSourceConnectionProvider(dataSource);
    }

    @Qualifier("leafTransactionAwareDataSource")
    @Bean(name = "leafTransactionAwareDataSource")
    public TransactionAwareDataSourceProxy
    transactionAwareDataSourceProxy(@Qualifier("leaf-data-source") DataSource dataSource) {
        return new TransactionAwareDataSourceProxy(dataSource);
    }

    @Bean("jooqLeafIdAllocator")
    public JooqLeafIdAllocator jooqLeafIdAllocator(@Qualifier("leaf-dsl-context") DSLContext dslContext) {
        return new JooqLeafIdAllocator(dslContext);
    }
}
