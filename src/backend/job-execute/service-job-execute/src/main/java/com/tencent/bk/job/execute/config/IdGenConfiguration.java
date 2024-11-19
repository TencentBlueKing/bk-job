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

package com.tencent.bk.job.execute.config;

import com.tencent.bk.job.common.mysql.dynamic.id.IdGenType;
import com.tencent.bk.job.common.util.toggle.prop.PropToggleStore;
import com.tencent.bk.job.execute.dao.common.AutoIncrementIdGen;
import com.tencent.bk.job.execute.dao.common.PropBasedDynamicIdGen;
import com.tencent.bk.job.execute.dao.common.SegmentIdGen;
import com.tencent.devops.leaf.service.SegmentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.AllNestedConditions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration(value = "jobExecuteIdGenConfiguration")
@Slf4j
public class IdGenConfiguration {

    @Conditional(AutoIncrementIdGenCondition.class)
    @Bean
    public AutoIncrementIdGen autoIncrementIdGen() {
        log.info("Init AutoIncrementIdGen");
        return new AutoIncrementIdGen();
    }

    static class AutoIncrementIdGenCondition extends AllNestedConditions {
        public AutoIncrementIdGenCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(value = "idGen.type", havingValue = IdGenType.Constants.AUTO_INCREMENT)
        static class IdGenTypeCondition {

        }

        @ConditionalOnProperty(value = "idGen.migration.enabled", havingValue = "false", matchIfMissing = true)
        static class MigrationDisable {

        }
    }

    @Conditional(SegmentIdGenCondition.class)
    @Bean
    public SegmentIdGen segmentIdGen(SegmentService segmentService) {
        log.info("Init SegmentIdGen");
        return new SegmentIdGen(segmentService);
    }

    static class SegmentIdGenCondition extends AllNestedConditions {
        public SegmentIdGenCondition() {
            super(ConfigurationPhase.REGISTER_BEAN);
        }

        @ConditionalOnProperty(value = "idGen.type", havingValue = IdGenType.Constants.LEAF_SEGMENT)
        static class IdGenTypeCondition {

        }

        @ConditionalOnProperty(value = "idGen.migration.enabled", havingValue = "false", matchIfMissing = true)
        static class MigrationDisable {

        }
    }

    @ConditionalOnProperty(value = "idGen.migration.enabled", havingValue = "true")
    @Bean
    public PropBasedDynamicIdGen propBasedDynamicIdGen(
        SegmentService segmentService,
        @Qualifier("jsonRedisTemplate") RedisTemplate<String, Object> redisTemplate,
        PropToggleStore propToggleStore
    ) {
        log.info("Init PropBasedDynamicIdGen");
        AutoIncrementIdGen autoIncrementIdGen = new AutoIncrementIdGen();
        SegmentIdGen segmentIdGen = new SegmentIdGen(segmentService);
        return new PropBasedDynamicIdGen(autoIncrementIdGen, segmentIdGen, redisTemplate, propToggleStore);
    }


}
