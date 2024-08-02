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

import com.tencent.bk.job.common.sharding.mysql.config.ShardingProperties;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.BeanCreationException;

@Slf4j
public class DefaultDSLContextDynamicProvider implements DSLContextDynamicProvider {


    private final DSLContext dslContext;

    public DefaultDSLContextDynamicProvider(ShardingProperties shardingProperties,
                                            DSLContext noShardingDSLContext,
                                            DSLContext shardingDSLContext) {
        boolean shardingEnabled = shardingProperties != null && shardingProperties.isEnabled();

        if (shardingEnabled) {
            if (shardingDSLContext == null) {
                log.error("ShardingDSLContext empty");
                throw new BeanCreationException("ShardingDSLContextEmpty");
            }
            this.dslContext = shardingDSLContext;
        } else {
            if (noShardingDSLContext == null) {
                log.error("NoShardingDSLContext empty");
                throw new BeanCreationException("NoShardingDSLContextEmpty");
            }
            this.dslContext = noShardingDSLContext;
        }
    }

    @Override
    public DSLContext get() {
        return dslContext;
    }

    @Override
    public void set(DSLContext currentDSLContext) {
        throw new UnsupportedOperationException("Can not set DSLContext");
    }

    @Override
    public void unset() {
        throw new UnsupportedOperationException("Can not unset DSLContext");
    }
}
