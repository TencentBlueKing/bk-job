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

import com.tencent.bk.job.common.sharding.mysql.config.MySQLProperties;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.BeanCreationException;

@Slf4j
public class StaticDSLContextProvider implements DSLContextProvider {


    private final DSLContext dslContext;

    public StaticDSLContextProvider(MySQLProperties mySQLProperties,
                                    DSLContext standardDSLContext,
                                    DSLContext verticalShardingDSLContext) {
        if (mySQLProperties == null) {
            this.dslContext = standardDSLContext;
            return;
        }

        String primaryDsName = mySQLProperties.getPrimaryDataSourceName();
        // 使用标准数据源（不分片）
        if (primaryDsName.equals("job-execute")) {
            if (standardDSLContext == null) {
                log.error("StandardDSLContext empty");
                throw new BeanCreationException("StandardDSLContextEmpty");
            }
            this.dslContext = standardDSLContext;
        } else if (primaryDsName.equals("job-execute-vertical")) {
            // 使用垂直分片数据源
            if (verticalShardingDSLContext == null) {
                log.error("VerticalShardingDSLContext empty");
                throw new BeanCreationException("VerticalShardingDSLContext");
            }
            this.dslContext = verticalShardingDSLContext;
        } else {
            throw new BeanCreationException("Unsupported job execute primary ds name");
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
