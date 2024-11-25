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
@ConditionalOnProperty(value = "sharding.enabled", havingValue = "true")
@Slf4j
public class ShardingDatasourceAutoConfiguration {

    @Bean("jobShardingDataSource")
    public DataSource shardingDataSource(ShardingProperties shardingProperties) throws SQLException {
        log.info("Init sharding datasource start ...");
        ShardingProperties.ShardingsphereProperties shardingsphereProperties = shardingProperties.getShardingsphere();
        // 指定逻辑 Database 名称
        String databaseName = shardingsphereProperties.getDatabaseName();
        // 构建运行模式
        ModeConfiguration modeConfig = createModeConfiguration();
        // 构建真实数据源
        Map<String, DataSource> dataSourceMap = createDataSources(shardingsphereProperties.getDataSources());
        // 构建具体规则
        List<RuleConfiguration> ruleConfigs = new ArrayList<>();
        ruleConfigs.add(createShardingRuleConfiguration(shardingsphereProperties.getShardingRule()));
        // 构建系统级属性配置
        Properties globalProps = createShardingGlobalProps(shardingsphereProperties.getProps());
        DataSource dataSource = ShardingSphereDataSourceFactory.createDataSource(databaseName, modeConfig,
            dataSourceMap, ruleConfigs, globalProps);
        log.info("Init sharding datasource successfully");
        return dataSource;
    }

    private ModeConfiguration createModeConfiguration() {
        log.info("Load sharding mode, type: Standalone");
        return new ModeConfiguration("Standalone",
            new StandalonePersistRepositoryConfiguration("JDBC", new Properties()));
    }

    private Map<String, DataSource> createDataSources(
        Map<String, ShardingProperties.ShardingsphereProperties.DataSource> dataSourcePropMap) {
        log.info("Init datasourceMap start ...");

        Map<String, DataSource> dataSourceMap = new HashMap<>();

        dataSourcePropMap.forEach((name, dataSourceProp) -> {
            log.info("Create sharding datasource, name : {}, dataSourceProp: {}", name, dataSourceProp);
            HikariDataSource dataSource = new HikariDataSource();
            dataSource.setDriverClassName(dataSourceProp.getDriverClassName());
            dataSource.setJdbcUrl(dataSourceProp.getJdbcUrl());
            dataSource.setUsername(dataSourceProp.getUsername());
            dataSource.setPassword(dataSourceProp.getPassword());
            dataSourceMap.put(name, dataSource);
        });
        log.info("Init datasourceMap successfully");
        return dataSourceMap;
    }

    private ShardingRuleConfiguration createShardingRuleConfiguration(
        ShardingProperties.ShardingsphereProperties.ShardingRule shardingRuleProps) {
        log.info("Init sharding rule configuration start ...");

        ShardingRuleConfiguration shardingRuleConfiguration = new ShardingRuleConfiguration();

        // 分片表规则列表
        shardingRuleConfiguration.getTables()
            .addAll(createShardingTableRuleConfigurations(shardingRuleProps.getTables()));

        // 绑定表规则列表
        if (CollectionUtils.isNotEmpty(shardingRuleProps.getBindingTables())) {
            shardingRuleProps.getBindingTables().forEach(bindingTables -> {
                String ruleName = "binding_rule_" +
                    Arrays.stream(bindingTables.split(",")).map(String::trim).collect(Collectors.joining("_"));
                log.info("Create binding table group, ruleName: {}, bindingTables: {}", ruleName, bindingTables);
                shardingRuleConfiguration.getBindingTableGroups().add(
                    new ShardingTableReferenceRuleConfiguration(ruleName, bindingTables));
            });
        }

        // 默认分库策略
        if (shardingRuleProps.getDefaultDatabaseStrategy() != null) {
            log.info("Create default database sharding strategy, strategy: {}",
                shardingRuleProps.getDefaultDatabaseStrategy());

            shardingRuleConfiguration.setDefaultDatabaseShardingStrategy(
                createShardingStrategyConfiguration(shardingRuleProps.getDefaultDatabaseStrategy()));
        }

        // 默认分表策略
        if (shardingRuleProps.getDefaultTableStrategy() != null) {
            log.info("Create default table sharding strategy, strategy: {}",
                shardingRuleProps.getDefaultTableStrategy());
            shardingRuleConfiguration.setDefaultTableShardingStrategy(
                createShardingStrategyConfiguration(shardingRuleProps.getDefaultTableStrategy()));
        }

        // 分片算法配置
        if (MapUtils.isNotEmpty(shardingRuleProps.getShardingAlgorithms())) {
            shardingRuleProps.getShardingAlgorithms().forEach((name, algorithm) -> {
                log.info("Create sharding algorithms, name: {}, type: {}, props: {}",
                    name, algorithm.getType(), algorithm.getProps());
                Properties props = toProperties(algorithm.getProps());
                shardingRuleConfiguration.getShardingAlgorithms()
                    .put(name, new AlgorithmConfiguration(algorithm.getType(), props));
            });
        }

        // 自增列生成算法配置
        if (MapUtils.isNotEmpty(shardingRuleProps.getKeyGenerators())) {
            shardingRuleProps.getKeyGenerators().forEach((name, generator) -> {
                log.info("Create key generator, name: {}, type: {}", name, generator.getType());
                Properties props = toProperties(generator.getProps());
                shardingRuleConfiguration.getKeyGenerators()
                    .put(name, new AlgorithmConfiguration(generator.getType(), props));
            });
        }

        log.info("Init sharding rule configuration successfully");

        return shardingRuleConfiguration;
    }

    private Properties toProperties(Map<String, String> propsMap) {
        Properties props = new Properties();
        props.putAll(propsMap);
        return props;
    }


    private List<ShardingTableRuleConfiguration> createShardingTableRuleConfigurations(
        Map<String, ShardingProperties.ShardingsphereProperties.TableShardingRule> tables) {

        List<ShardingTableRuleConfiguration> configurations = new ArrayList<>(tables.size());

        tables.forEach((tableName, tableShardingRule) ->
            configurations.add(createShardingTableRuleConfiguration(tableName, tableShardingRule)));

        return configurations;
    }

    private ShardingTableRuleConfiguration createShardingTableRuleConfiguration(
        String tableName,
        ShardingProperties.ShardingsphereProperties.TableShardingRule tableShardingRule) {
        log.info("[{}] Create sharding table rule configuration, tableShardingRule: {}",
            tableName, tableShardingRule);

        ShardingTableRuleConfiguration configuration = new ShardingTableRuleConfiguration(
            tableName, tableShardingRule.getActualDataNodes());
        if (tableShardingRule.getKeyGenerateStrategy() != null) {
            log.info("[{}] Create key generate strategy configuration, column: {}, keyGeneratorName: {}",
                tableName,
                tableShardingRule.getKeyGenerateStrategy().getColumn(),
                tableShardingRule.getKeyGenerateStrategy().getKeyGeneratorName());
            configuration.setKeyGenerateStrategy(
                new KeyGenerateStrategyConfiguration(
                    tableShardingRule.getKeyGenerateStrategy().getColumn(),
                    tableShardingRule.getKeyGenerateStrategy().getKeyGeneratorName()
                )
            );
        }
        if (tableShardingRule.getDatabaseStrategy() != null) {
            log.info("[{}] Create database sharding strategy, strategy: {}",
                tableName, tableShardingRule.getDatabaseStrategy());
            configuration.setDatabaseShardingStrategy(
                createShardingStrategyConfiguration(tableShardingRule.getDatabaseStrategy()));
        }
        if (tableShardingRule.getTableStrategy() != null) {
            log.info("[{}] Create table sharding strategy, strategy: {}",
                tableName, tableShardingRule.getTableStrategy());
            configuration.setTableShardingStrategy(
                createShardingStrategyConfiguration(tableShardingRule.getTableStrategy()));
        }

        return configuration;
    }

    private ShardingStrategyConfiguration createShardingStrategyConfiguration(
        ShardingProperties.ShardingsphereProperties.ShardingStrategy shardingStrategyProps) {
        String shardingStrategyType = shardingStrategyProps.getType().trim().toLowerCase();
        switch (shardingStrategyType) {
            case "standard":
                return new StandardShardingStrategyConfiguration(
                    shardingStrategyProps.getShardingColumn(),
                    shardingStrategyProps.getShardingAlgorithmName()
                );
            case "hint":
                return new HintShardingStrategyConfiguration(
                    shardingStrategyProps.getShardingAlgorithmName()
                );
            default:
                throw new ShardingConfigParseException("Not support sharding algorithm");
        }
    }


    private Properties createShardingGlobalProps(Map<String, String> globalPropsMap) {
        // 配置 shardingsphere 系统级配置
        if (globalPropsMap == null || globalPropsMap.isEmpty()) {
            return null;
        }
        return toProperties(globalPropsMap);
    }

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
