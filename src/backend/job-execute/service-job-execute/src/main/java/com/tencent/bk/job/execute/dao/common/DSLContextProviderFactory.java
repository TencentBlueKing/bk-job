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

import com.tencent.bk.job.common.mysql.MySQLProperties;
import com.tencent.bk.job.common.mysql.dynamic.ds.DSLContextProvider;
import com.tencent.bk.job.common.mysql.dynamic.ds.DataSourceMode;
import com.tencent.bk.job.common.mysql.dynamic.ds.HorizontalShardingDSLContextProvider;
import com.tencent.bk.job.common.mysql.dynamic.ds.MigrateDynamicDSLContextProvider;
import com.tencent.bk.job.common.mysql.dynamic.ds.StandaloneDSLContextProvider;
import com.tencent.bk.job.common.mysql.dynamic.ds.VerticalShardingDSLContextProvider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DSLContextProviderFactory {

    private DSLContextProvider dslContextProvider;

    public DSLContextProviderFactory(StandaloneDSLContextProvider standaloneDSLContextProvider,
                                     VerticalShardingDSLContextProvider verticalShardingDSLContextProvider,
                                     HorizontalShardingDSLContextProvider horizontalShardingDSLContextProvider,
                                     MigrateDynamicDSLContextProvider migrateDynamicDSLContextProvider,
                                     MySQLProperties mySQLProperties) {
        if (mySQLProperties.getMigration() != null && mySQLProperties.getMigration().isEnabled()) {
            checkDSLContextProvider(migrateDynamicDSLContextProvider);
            this.dslContextProvider = migrateDynamicDSLContextProvider;
            log.info("Use MigrateDynamicDSLContextProvider");
        } else {
            DataSourceMode dataSourceMode = DataSourceMode.valOf(mySQLProperties.getDataSourceMode());
            if (dataSourceMode == DataSourceMode.STANDALONE) {
                checkDSLContextProvider(standaloneDSLContextProvider);
                this.dslContextProvider = standaloneDSLContextProvider;
                log.info("Use StandaloneDSLContextProvider");
            } else if (dataSourceMode == DataSourceMode.VERTICAL_SHARDING) {
                checkDSLContextProvider(verticalShardingDSLContextProvider);
                this.dslContextProvider = verticalShardingDSLContextProvider;
                log.info("Use VerticalShardingDSLContextProvider");
            } else if (dataSourceMode == DataSourceMode.HORIZONTAL_SHARDING) {
                checkDSLContextProvider(horizontalShardingDSLContextProvider);
                this.dslContextProvider = horizontalShardingDSLContextProvider;
                log.info("Use HorizontalShardingDSLContextProvider");
            }
        }
    }

    private void checkDSLContextProvider(DSLContextProvider dslContextProvider) {
        if (dslContextProvider == null) {
            throw new IllegalStateException("DSLContextProvider is empty");
        }
    }

    public DSLContextProvider get() {
        return this.dslContextProvider;
    }
}
