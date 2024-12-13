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

import com.tencent.bk.job.common.sharding.mysql.algorithm.ShardingStrategyType;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * 分库分表配置
 */
@ConfigurationProperties(prefix = "mysql.sharding")
@Getter
@Setter
@ToString
public class ShardingProperties {
    /**
     * 是否启用分库分表
     */
    private boolean enabled;

    /**
     * 数据库分片配置
     */
    private Map<String, DatabaseProperties> databases;

    @Getter
    @Setter
    public static class DatabaseProperties {
        /**
         * 逻辑数据库名称
         */
        private String logicDatabaseName;
        /**
         * 数据源 driverClassName
         */
        private String dataSourceDriverClassName;
        /**
         * 数据库用户名
         */
        private String username;
        /**
         * 数据库密码
         */
        private String password;
        /**
         * 数据源分组
         */
        private Map<String, DataSourceGroupProperties> dataSourceGroups;
        /**
         * 分片规则
         */
        private ShardingRuleProperties shardingRule;
        /**
         * 系统级属性配置
         */
        private Map<String, String> props;

        @Override
        public String toString() {
            return new StringJoiner(", ", DatabaseProperties.class.getSimpleName() + "[", "]")
                .add("username='" + username + "'")
                .add("password='******'")
                .add("dataSourceGroups=" + dataSourceGroups)
                .add("shardingRule=" + shardingRule)
                .add("props=" + props)
                .toString();
        }
    }

    @Getter
    @Setter
    @ToString
    public static class DataSourceGroupProperties {
        private List<DataSourceProperties> dataSources;
    }

    @Getter
    @Setter
    public static class DataSourceProperties {
        /**
         * 数据源索引位置
         */
        private int index;
        /**
         * 数据库 URL 连接，以数据库连接池自身配置为准
         */
        private String jdbcUrl;
    }


    /**
     * 数据分片规则配置
     */
    @Getter
    @Setter
    @ToString
    public static class ShardingRuleProperties {
        /**
         * 表对应的分片规则。 key: 表名称;value: 分片规则
         */
        private Map<String, TableShardingRuleProperties> tables;
        /**
         * 分片算法定义
         */
        private Map<String, ShardingAlgorithmProperties> shardingAlgorithms;
        /**
         * 分布式序列算法配置
         */
        private Map<String, KeyGeneratorProperties> keyGenerators;
        /**
         * 默认数据库分片策略
         */
        private ShardingStrategyProperties defaultDatabaseStrategy;
        /**
         * 默认表分片策略
         */
        private ShardingStrategyProperties defaultTableStrategy;
        /**
         * 默认分片列名称
         */
        private String defaultShardingColumn;
        /**
         * 默认的分布式序列策略
         */
        private String defaultKeyGenerateStrategy;

        private List<String> bindingTables;
    }


    @Getter
    @Setter
    @ToString
    public static class TableShardingRuleProperties {
        /**
         * 分片数据节点表达式
         */
        private String dataNodes;
        /**
         * 分库策略
         */
        private ShardingStrategyProperties databaseStrategy;
        /**
         * 分表策略，同分库策略
         */
        private ShardingStrategyProperties tableStrategy;
        /**
         * 分布式序列策略
         */
        private KeyGenerateStrategyProperties keyGenerateStrategy;
    }

    /**
     * 分片计算策略
     */
    @Getter
    @Setter
    @ToString
    public static class ShardingStrategyProperties {
        /**
         * 分片策略类型
         *
         * @see ShardingStrategyType
         */
        private String type;
        /**
         * 分片键（单列，standard 类型的分片算法使用）
         */
        private String shardingColumn;
        /**
         * 分片键（多列，complex 类型的分片算法使用）
         */
        private String shardingColumns;
        /**
         * 算法名
         */
        private String shardingAlgorithmName;
    }

    /**
     * 分布式序列策略
     */
    @Getter
    @Setter
    @ToString
    public static class KeyGenerateStrategyProperties {
        /**
         * 自增列名称，缺省表示不使用自增主键生成器
         */
        private String column;
        /**
         * 分布式序列算法名称
         */
        private String keyGeneratorName;
    }

    /**
     * 分布式序列生成配置
     */
    @Getter
    @Setter
    @ToString
    public static class KeyGeneratorProperties {
        /**
         * 分布式序列算法类型
         */
        private String type;
        /**
         * 分布式序列算法属性配置
         */
        private Map<String, String> props;
    }

    /**
     * 分片算法配置
     */
    @Getter
    @Setter
    @ToString
    public static class ShardingAlgorithmProperties {
        /**
         * 分片算法类型
         */
        private String type;
        /**
         * 分片算法属性配置
         */
        private Map<String, String> props;
    }
}
