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

import com.tencent.bk.job.common.mysql.dynamic.ds.DSLContextProvider;
import com.tencent.bk.job.common.mysql.dynamic.ds.DataSourceMode;
import com.tencent.bk.job.common.mysql.dynamic.ds.DbOperationEnum;
import com.tencent.bk.job.common.mysql.dynamic.ds.MySQLOperation;
import com.tencent.bk.job.common.mysql.dynamic.ds.StandaloneDSLContextProvider;
import com.tencent.bk.job.common.mysql.dynamic.ds.VerticalShardingDSLContextProvider;
import com.tencent.bk.job.common.util.toggle.prop.PropToggleStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 基于属性动态控制的数据源，可以根据属性值切换到不同的数据源
 */
@Slf4j
public class PropBasedDynamicDataSource extends AbstractPropBasedDynamicComponent<DSLContextProvider> {

    public PropBasedDynamicDataSource(
        StandaloneDSLContextProvider standaloneDSLContextProvider,
        VerticalShardingDSLContextProvider verticalShardingDSLContextProvider,
        RedisTemplate<String, Object> redisTemplate,
        PropToggleStore propToggleStore) {
        super(redisTemplate, propToggleStore, "execute_mysql");

        Map<String, DSLContextProvider> candidateDSLContextProviders = new HashMap<>();
        if (standaloneDSLContextProvider != null) {
            candidateDSLContextProviders.put(DataSourceMode.Constants.STANDALONE, standaloneDSLContextProvider);
        }
        if (verticalShardingDSLContextProvider != null) {
            candidateDSLContextProviders.put(DataSourceMode.Constants.VERTICAL_SHARDING,
                verticalShardingDSLContextProvider);
        }
        super.initCandidateComponents(candidateDSLContextProviders);
    }

    /**
     * 获取当前 DSLContextProvider。
     * 如果处于db 数据源切换中，会阻塞当前线程直到切换完成
     *
     * @return DSLContextProvider
     */
    public DSLContextProvider getCurrent(MySQLOperation op) {
        if (op.op() == DbOperationEnum.WRITE) {
            return getCurrent(true);
        } else {
            return getCurrent(false);
        }
    }

    @Override
    protected DSLContextProvider getComponentByProp(Map<String, DSLContextProvider> candidateComponents,
                                                    String propValue) {
        if (!DataSourceMode.checkValid(propValue)) {
            log.error("Invalid target datasource mode : {}, skip migration", propValue);
            return null;
        }
        return candidateComponents.get(propValue);
    }
}
