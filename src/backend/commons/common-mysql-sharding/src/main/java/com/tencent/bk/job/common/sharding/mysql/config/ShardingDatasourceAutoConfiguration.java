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
import com.tencent.bk.job.common.sharding.mysql.ShardingConfigParseException;
import com.zaxxer.hikari.HikariDataSource;
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
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.ShardingStrategyConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ShardingsphereProperties.class})
@ConditionalOnProperty(value = "shardingsphere.enabled", havingValue = "true")
@Import(LeafDbConfig.class)
public class ShardingDatasourceAutoConfiguration {

    @Bean("jooqLeafIdAllocator")
    public JooqLeafIdAllocator jooqLeafIdAllocator(@Qualifier("leaf-dsl-context") DSLContext dslContext) {
        return new JooqLeafIdAllocator(dslContext);
    }

    @Bean("shardingDataSource")
    public DataSource shardingDataSource(ShardingsphereProperties shardingsphereProperties) throws SQLException {
        // 指定逻辑 Database 名称
        String databaseName = shardingsphereProperties.getDatabaseName();
        // 构建运行模式
        ModeConfiguration modeConfig = createModeConfiguration();
        // 构建真实数据源
        Map<String, DataSource> dataSourceMap = createDataSources(shardingsphereProperties.getDataSources());
        // 构建具体规则
        List<RuleConfiguration> ruleConfigs = new ArrayList<>();
        ruleConfigs.add(createShardingRuleConfiguration(shardingsphereProperties.getShardingRule()));
        return ShardingSphereDataSourceFactory.createDataSource(databaseName, modeConfig,
            dataSourceMap, ruleConfigs, null);
    }

    private ModeConfiguration createModeConfiguration() {
        return new ModeConfiguration("Standalone",
            new StandalonePersistRepositoryConfiguration("JDBC", new Properties()));
    }

    private Map<String, DataSource> createDataSources(
        Map<String, ShardingsphereProperties.DataSource> dataSourcePropMap) {

        Map<String, DataSource> dataSourceMap = new HashMap<>();

        dataSourcePropMap.forEach((name, dataSourceProp) -> {
            HikariDataSource dataSource = new HikariDataSource();
            dataSource.setDataSourceClassName(dataSourceProp.getDataSourceClassName());
            dataSource.setDriverClassName(dataSourceProp.getDriverClassName());
            dataSource.setJdbcUrl(dataSourceProp.getJdbcUrl());
            dataSource.setUsername(dataSourceProp.getUsername());
            dataSource.setPassword(dataSourceProp.getPassword());
            dataSourceMap.put(name, dataSource);
        });

        return dataSourceMap;
    }

    private ShardingRuleConfiguration createShardingRuleConfiguration(
        ShardingsphereProperties.ShardingRule shardingRuleProps) {

        ShardingRuleConfiguration shardingRuleConfiguration = new ShardingRuleConfiguration();
        shardingRuleConfiguration.getTables()
            .addAll(createShardingTableRuleConfigurations(shardingRuleProps.getTables()));

        if (CollectionUtils.isNotEmpty(shardingRuleProps.getBindingTables())) {
            shardingRuleProps.getBindingTables().forEach(bindingTables -> {
                String ruleName = "binding_rule_" +
                    Arrays.stream(bindingTables.split(",")).map(String::trim).collect(Collectors.joining("_"));
                shardingRuleConfiguration.getBindingTableGroups().add(
                    new ShardingTableReferenceRuleConfiguration(ruleName, bindingTables));
            });

        }

        if (shardingRuleProps.getDefaultDatabaseStrategy() != null) {
            shardingRuleConfiguration.setDefaultDatabaseShardingStrategy(
                createShardingStrategyConfiguration(shardingRuleProps.getDefaultDatabaseStrategy()));
        }

        if (shardingRuleProps.getDefaultTableStrategy() != null) {
            shardingRuleConfiguration.setDefaultTableShardingStrategy(
                createShardingStrategyConfiguration(shardingRuleProps.getDefaultTableStrategy()));
        }

        if (MapUtils.isNotEmpty(shardingRuleProps.getShardingAlgorithms())) {
            shardingRuleProps.getShardingAlgorithms().forEach((name, algorithm) -> {
                Properties props = toProperties(algorithm.getProps());
                shardingRuleConfiguration.getShardingAlgorithms()
                    .put(name, new AlgorithmConfiguration(algorithm.getType(), props));
            });
        }

        if (MapUtils.isNotEmpty(shardingRuleProps.getKeyGenerators())) {
            shardingRuleProps.getKeyGenerators().forEach((name, generator) -> {
                Properties props = toProperties(generator.getProps());
                shardingRuleConfiguration.getKeyGenerators()
                    .put(name, new AlgorithmConfiguration(generator.getType(), props));
            });
        }

        return shardingRuleConfiguration;
    }

    private Properties toProperties(Map<String, String> propsMap) {
        Properties props = new Properties();
        props.putAll(propsMap);
        return props;
    }


    private List<ShardingTableRuleConfiguration> createShardingTableRuleConfigurations(
        Map<String, ShardingsphereProperties.TableShardingRule> tables) {

        List<ShardingTableRuleConfiguration> configurations = new ArrayList<>(tables.size());

        tables.forEach((tableName, tableShardingRule) ->
            configurations.add(createShardingTableRuleConfiguration(tableName, tableShardingRule)));

        return configurations;
    }

    private ShardingTableRuleConfiguration createShardingTableRuleConfiguration(
        String tableName,
        ShardingsphereProperties.TableShardingRule tableShardingRule) {

        ShardingTableRuleConfiguration configuration = new ShardingTableRuleConfiguration(
            tableName, tableShardingRule.getActualDataNodes());
        configuration.setKeyGenerateStrategy(
            new KeyGenerateStrategyConfiguration(
                tableShardingRule.getKeyGenerateStrategy().getColumn(),
                tableShardingRule.getKeyGenerateStrategy().getKeyGeneratorName()
            )
        );
        if (tableShardingRule.getDatabaseStrategy() != null) {
            configuration.setDatabaseShardingStrategy(
                createShardingStrategyConfiguration(tableShardingRule.getDatabaseStrategy()));
        }
        if (tableShardingRule.getTableStrategy() != null) {
            configuration.setTableShardingStrategy(
                createShardingStrategyConfiguration(tableShardingRule.getTableStrategy()));
        }

        return configuration;
    }

    private ShardingStrategyConfiguration createShardingStrategyConfiguration(
        ShardingsphereProperties.ShardingStrategy shardingStrategyProps) {
        if (isStandardShardingAlgorithm(shardingStrategyProps.getShardingAlgorithmName())) {
            return new StandardShardingStrategyConfiguration(
                shardingStrategyProps.getShardingColumn(),
                shardingStrategyProps.getShardingAlgorithmName()
            );
        } else {
            throw new ShardingConfigParseException("Not support sharding algorithm");
        }
    }

    private boolean isStandardShardingAlgorithm(String shardingAlgorithmName) {
        return shardingAlgorithmName.equalsIgnoreCase("standard");
    }
}
