package com.tencent.bk.job.common.sharding.mysql.config;

import com.tencent.bk.job.common.sharding.mysql.DataNodesParser;
import com.tencent.bk.job.common.sharding.mysql.DataSourceGroupShardingNode;
import com.tencent.bk.job.common.sharding.mysql.ShardingConfigParseException;
import com.tencent.bk.job.common.sharding.mysql.algorithm.ShardingStrategyType;
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
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.ComplexShardingStrategyConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.HintShardingStrategyConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.ShardingStrategyConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
public class ShardingDataSourceFactory {

    /**
     * 创建分库分表数据源
     *
     * @param databaseProperties database 配置
     */
    public static DataSource createDataSource(
        ShardingProperties.DatabaseProperties databaseProperties) throws SQLException {

        // 指定逻辑 Database 名称
        String databaseName = databaseProperties.getLogicDatabaseName();
        log.info("Init sharding datasource start, logicDatabaseName: {}", databaseName);
        // 构建运行模式
        ModeConfiguration modeConfig = createModeConfiguration();
        // 构建真实数据源
        Map<String, DataSource> dataSourceMap = createDataSources(databaseProperties);
        // 构建具体规则
        List<RuleConfiguration> ruleConfigs = new ArrayList<>();
        ruleConfigs.add(createShardingRuleConfiguration(databaseProperties.getShardingRule()));
        // 构建系统级属性配置
        Properties globalProps = createShardingGlobalProps(databaseProperties.getProps());
        DataSource dataSource = ShardingSphereDataSourceFactory.createDataSource(databaseName, modeConfig,
            dataSourceMap, ruleConfigs, globalProps);
        log.info("Init sharding datasource successfully, logicDatabaseName: {}", databaseName);
        return dataSource;
    }


    private static ModeConfiguration createModeConfiguration() {
        log.info("Load sharding mode, type: Standalone");
        return new ModeConfiguration("Standalone",
            new StandalonePersistRepositoryConfiguration("JDBC", new Properties()));
    }

    private static Map<String, DataSource> createDataSources(
        ShardingProperties.DatabaseProperties databaseProperties) {

        Map<String, ShardingProperties.DataSourceGroupProperties> dataSourceGroups =
            databaseProperties.getDataSourceGroups();
        Map<String, DataSource> dataSourceMap = new HashMap<>();
        dataSourceGroups.forEach((dataSourceGroupName, dataSourceGroup) ->
            dataSourceGroup.getDataSources().forEach(dataSourceProperties -> {
                log.info("Create sharding data source, dataSourceGroupName: {}, dataSourceProp: {}",
                    dataSourceGroupName, dataSourceProperties);
                HikariDataSource dataSource = new HikariDataSource();
                dataSource.setDriverClassName(databaseProperties.getDataSourceDriverClassName());
                dataSource.setJdbcUrl(dataSourceProperties.getJdbcUrl());
                dataSource.setUsername(databaseProperties.getUsername());
                dataSource.setPassword(databaseProperties.getPassword());
                dataSourceMap.put(buildDataSourceName(dataSourceGroupName,
                    dataSourceProperties.getIndex()), dataSource);
            }));
        return dataSourceMap;
    }

    private static String buildDataSourceName(String dataSourceGroupName, int index) {
        return dataSourceGroupName + "_" + index;
    }

    private static ShardingRuleConfiguration createShardingRuleConfiguration(
        ShardingProperties.ShardingRuleProperties shardingRuleProps) {
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

    private static Properties toProperties(Map<String, String> propsMap) {
        Properties props = new Properties();
        props.putAll(propsMap);
        return props;
    }


    private static List<ShardingTableRuleConfiguration> createShardingTableRuleConfigurations(
        Map<String, ShardingProperties.TableShardingRuleProperties> tables) {

        List<ShardingTableRuleConfiguration> configurations = new ArrayList<>(tables.size());

        tables.forEach((tableName, tableShardingRule) ->
            configurations.add(createShardingTableRuleConfiguration(tableName, tableShardingRule)));

        return configurations;
    }

    private static ShardingTableRuleConfiguration createShardingTableRuleConfiguration(
        String tableName,
        ShardingProperties.TableShardingRuleProperties tableShardingRule) {
        log.info("[{}] Create sharding table rule configuration, tableShardingRule: {}",
            tableName, tableShardingRule);

        ShardingTableRuleConfiguration configuration = new ShardingTableRuleConfiguration(
            tableName, buildShardingSphereActualDataNodes(tableName, tableShardingRule.getDataNodes()));
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

    private static String buildShardingSphereActualDataNodes(String tableName, String dataNodes) {
        List<DataSourceGroupShardingNode> dataSourceGroupShardingNodes = DataNodesParser.parseDataNodes(dataNodes);
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (DataSourceGroupShardingNode dataSourceGroupShardingNode : dataSourceGroupShardingNodes) {
            String dataSourceGroupName = dataSourceGroupShardingNode.getDataSourceGroupName();
            // 构造 ShardingSphere actualDataNodes inline 表达式, 比如 ds_${0..2}.task_instance_${0..31}
            if (!first) {
                sb.append(",");
            } else {
                first = false;
            }
            sb.append(dataSourceGroupName)
                .append("_${0..")
                .append(dataSourceGroupShardingNode.getDbNodeCount() - 1)
                .append("}")
                .append(".")
                .append(tableName)
                .append("_${0..")
                .append(dataSourceGroupShardingNode.getTbNodeCount() - 1)
                .append("}");
        }
        String actualDataNodes = sb.toString();
        log.info("Parse actualDataNodes, tableName: {}, dataNodes: {}, actualDataNodes: {}",
            tableName, dataNodes, actualDataNodes);
        return actualDataNodes;
    }

    private static ShardingStrategyConfiguration createShardingStrategyConfiguration(
        ShardingProperties.ShardingStrategyProperties shardingStrategyProps) {
        String strategyTypeValue = shardingStrategyProps.getType().trim().toLowerCase();
        ShardingStrategyType shardingStrategyType = ShardingStrategyType.valOf(strategyTypeValue);

        switch (shardingStrategyType) {
            case STANDARD:
                return new StandardShardingStrategyConfiguration(
                    shardingStrategyProps.getShardingColumn(),
                    shardingStrategyProps.getShardingAlgorithmName()
                );
            case HINT:
                return new HintShardingStrategyConfiguration(
                    shardingStrategyProps.getShardingAlgorithmName()
                );
            case COMPLEX:
                return new ComplexShardingStrategyConfiguration(
                    shardingStrategyProps.getShardingColumns(),
                    shardingStrategyProps.getShardingAlgorithmName()
                );
            default:
                throw new ShardingConfigParseException("Not support sharding strategy type");
        }
    }


    private static Properties createShardingGlobalProps(Map<String, String> globalPropsMap) {
        // 配置 shardingsphere 系统级配置
        if (globalPropsMap == null || globalPropsMap.isEmpty()) {
            return null;
        }
        return toProperties(globalPropsMap);
    }
}
