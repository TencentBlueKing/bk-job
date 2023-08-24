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

import com.tencent.bk.job.common.service.constants.DeployModeEnum;
import org.jooq.ConnectionProvider;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration(value = "executeDbArchiveConfig")
@ConditionalOnProperty(value = "job.backup.archive.execute.enabled", havingValue = "true")
public class ExecuteDbArchiveConfig {

    /**
     * job-execute DB 配置
     */
    @Configuration("executeDbConfig")
    @ConditionalOnProperty(value = "deploy.mode", havingValue = DeployModeEnum.Constants.STANDARD,
        matchIfMissing = true)
    static class JobExecuteDbConfig {
        @Qualifier("job-execute-source")
        @Bean(name = "job-execute-source")
        @ConfigurationProperties(prefix = "spring.datasource.job-execute")
        public DataSource executeDataSource() {
            return DataSourceBuilder.create().build();
        }

        @Qualifier("job-execute-dsl-context")
        @Bean(name = "job-execute-dsl-context")
        public DSLContext executeDslContext(@Qualifier("job-execute-jooq-conf") org.jooq.Configuration configuration) {
            return new DefaultDSLContext(configuration);
        }

        @Qualifier("job-execute-jooq-conf")
        @Bean(name = "job-execute-jooq-conf")
        public org.jooq.Configuration
        executeJooqConf(@Qualifier("job-execute-conn-provider") ConnectionProvider connectionProvider) {
            return new DefaultConfiguration().derive(connectionProvider).derive(SQLDialect.MYSQL);
        }

        @Qualifier("job-execute-conn-provider")
        @Bean(name = "job-execute-conn-provider")
        public ConnectionProvider executeConnectionProvider(@Qualifier("job-execute-source") DataSource dataSource) {
            return new DataSourceConnectionProvider(dataSource);
        }
    }

    /**
     * 归档 DB 配置
     */
    @Configuration("executeArchiveDbConfig")
    @ConditionalOnProperty(value = "job.backup.archive.execute.backup.enabled", havingValue = "true")
    static class JobExecuteArchiveDbConfig {
        @Qualifier("job-execute-archive-source")
        @Bean(name = "job-execute-archive-source")
        @ConfigurationProperties(prefix = "spring.datasource.job-execute-archive")
        @ConditionalOnProperty("spring.datasource.job-execute-archive.jdbc-url")
        public DataSource executeArchiveDataSource() {
            return DataSourceBuilder.create().build();
        }

        @Qualifier("job-execute-archive-dsl-context")
        @Bean(name = "job-execute-archive-dsl-context")
        @ConditionalOnProperty("spring.datasource.job-execute-archive.jdbc-url")
        public DSLContext executeArchiveDslContext(
            @Qualifier("job-execute-archive-jooq-conf") org.jooq.Configuration configuration) {
            return new DefaultDSLContext(configuration);
        }

        @Qualifier("job-execute-archive-jooq-conf")
        @Bean(name = "job-execute-archive-jooq-conf")
        @ConditionalOnProperty("spring.datasource.job-execute-archive.jdbc-url")
        public org.jooq.Configuration
        executeArchiveJooqConf(@Qualifier("job-execute-archive-conn-provider") ConnectionProvider connectionProvider) {
            return new DefaultConfiguration().derive(connectionProvider).derive(SQLDialect.MYSQL);
        }

        @Qualifier("job-execute-archive-conn-provider")
        @Bean(name = "job-execute-archive-conn-provider")
        @ConditionalOnProperty("spring.datasource.job-execute-archive.jdbc-url")
        public ConnectionProvider executeArchiveConnectionProvider(
            @Qualifier("job-execute-archive-source") DataSource dataSource) {
            return new DataSourceConnectionProvider(dataSource);
        }
    }


}
