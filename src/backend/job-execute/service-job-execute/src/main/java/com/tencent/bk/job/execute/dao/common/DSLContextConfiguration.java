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

package com.tencent.bk.job.execute.dao.common;

import lombok.extern.slf4j.Slf4j;

@Slf4j
//@Configuration(value = "jobExecuteDSLContextConfiguration")
public class DSLContextConfiguration {
//
//    @Bean("defaultDSLContextDynamicProvider")
//    @ConditionalOnProperty(value = "sharding.migration.enabled", havingValue = "false", matchIfMissing = true)
//    public DefaultDSLContextDynamicProvider defaultDSLContextProvider(
//        ObjectProvider<ShardingProperties> shardingPropertiesObjectProvider,
//        @Autowired(required = false)
//        @Qualifier("job-execute-dsl-context")
//        DSLContext noShardingDSLContext,
//        @Autowired(required = false)
//        @Qualifier("job-sharding-dsl-context")
//        DSLContext shardingDSLContext) {
//        return new DefaultDSLContextDynamicProvider(shardingPropertiesObjectProvider.getIfAvailable(),
//            noShardingDSLContext, shardingDSLContext);
//    }
//
//    @Bean("shardingMigrateDSLContextDynamicProvider")
//    @Conditional(ShardingMigrateCondition.class)
//    public ShardingMigrateDSLContextDynamicProvider shardingMigrateDSLContextDynamicProvider() {
//        return new ShardingMigrateDSLContextDynamicProvider();
//    }
//
//
//    @Bean("shardingDbMigrateAspect")
//    @Conditional(ShardingMigrateCondition.class)
//    public ShardingDbMigrateAspect shardingDbMigrateAspect(
//        ShardingMigrateDSLContextDynamicProvider shardingMigrateDSLContextDynamicProvider,
//        @Qualifier("job-execute-dsl-context")
//        DSLContext noShardingDSLContext,
//        @Qualifier("job-sharding-dsl-context")
//        DSLContext shardingDSLContext,
//        PropToggleStore propToggleStore) {
//        log.info("Init ShardingDbMigrateAspect");
//        return new ShardingDbMigrateAspect(shardingMigrateDSLContextDynamicProvider, noShardingDSLContext,
//            shardingDSLContext, propToggleStore);
//    }
//
//    static class ShardingMigrateCondition extends AllNestedConditions {
//        public ShardingMigrateCondition() {
//            super(ConfigurationPhase.REGISTER_BEAN);
//        }
//
//        @ConditionalOnProperty(value = "sharding.enabled", havingValue = "true")
//        static class ShardingEnableCondition {
//
//        }
//
//        @ConditionalOnProperty(value = "sharding.migration.enabled", havingValue = "true")
//        static class ShardingMigrationCondition {
//
//        }
//    }

}
