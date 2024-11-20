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
@ConfigurationProperties(prefix = "sharding")
@Getter
@Setter
@ToString
public class ShardingProperties {
    /**
     * 是否启用分库分表
     */
    private boolean enabled;
    /**
     * 数据库实例数量
     */
    private Integer dbNodeCount;
    /**
     * 每个数据库实例上的表分片数量
     */
    private Integer tableNodeCount;

    /**
     * 单 db -> 分库分表迁移配置
     */
    private MigrationProperties migration;

    /**
     * ShardingSphere 分片组件配置
     */
    private ShardingsphereProperties shardingsphere;

    @Getter
    @Setter
    @ToString
    public static class MigrationProperties {
        private boolean enabled;
    }

    @Getter
    @Setter
    @ToString
    public static class ShardingsphereProperties {
        /**
         * 分片数据库名
         */
        private String databaseName;
        /**
         * 数据源集群。 key:集群名称；value:集群配置
         */
        private Map<String, DataSource> dataSources;
        /**
         * 分片规则
         */
        private ShardingRule shardingRule;
        /**
         * 系统级属性配置
         */
        private Map<String, String> props;

        @Getter
        @Setter
        public static class DataSource {
            /**
             * 数据库驱动类名，以数据库连接池自身配置为准
             */
            private String driverClassName;
            /**
             * 数据库 URL 连接，以数据库连接池自身配置为准
             */
            private String jdbcUrl;
            private String username;
            private String password;

            @Override
            public String toString() {
                return new StringJoiner(", ", DataSource.class.getSimpleName() + "[", "]")
                    .add("driverClassName='" + driverClassName + "'")
                    .add("jdbcUrl='" + jdbcUrl + "'")
                    .add("username='" + username + "'")
                    .add("password='******'")
                    .toString();
            }
        }


        /**
         * 数据分片规则配置
         */
        @Getter
        @Setter
        @ToString
        public static class ShardingRule {
            /**
             * 表对应的分片规则。 key: 表名称;value: 分片规则
             */
            private Map<String, TableShardingRule> tables;
            /**
             * 分片算法定义
             */
            private Map<String, ShardingAlgorithm> shardingAlgorithms;
            /**
             * 分布式序列算法配置
             */
            private Map<String, KeyGenerator> keyGenerators;
            /**
             * 默认数据库分片策略
             */
            private ShardingStrategy defaultDatabaseStrategy;
            /**
             * 默认表分片策略
             */
            private ShardingStrategy defaultTableStrategy;
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
        public static class TableShardingRule {
            /**
             * 由数据源名 + 表名组成
             */
            private String actualDataNodes;
            /**
             * 分库策略
             */
            private ShardingStrategy databaseStrategy;
            /**
             * 分表策略，同分库策略
             */
            private ShardingStrategy tableStrategy;
            /**
             * 分布式序列策略
             */
            private KeyGenerateStrategy keyGenerateStrategy;
        }

        /**
         * 分片计算策略
         */
        @Getter
        @Setter
        @ToString
        public static class ShardingStrategy {
            private String type;
            private String shardingColumn;
            private String shardingAlgorithmName;
        }

        /**
         * 分布式序列策略
         */
        @Getter
        @Setter
        @ToString
        public static class KeyGenerateStrategy {
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
        public static class KeyGenerator {
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
        public static class ShardingAlgorithm {
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
}
