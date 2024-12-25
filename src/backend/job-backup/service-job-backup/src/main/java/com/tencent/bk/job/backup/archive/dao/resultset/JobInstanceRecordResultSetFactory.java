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

package com.tencent.bk.job.backup.archive.dao.resultset;

import com.tencent.bk.job.backup.archive.dao.impl.AbstractJobInstanceHotRecordDAO;
import org.jooq.Condition;
import org.jooq.Record;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class JobInstanceRecordResultSetFactory {

    /**
     * 创建一个多次分批查询
     *
     * @param jobInstanceHotRecordDAO dao
     * @param jobInstanceIds          作业实例 ID 列表
     * @param readRowLimit            单次查询最大读取行数限制
     * @param offsetConditionBuilder  查询偏移条件构造
     * @param <T>                     记录
     * @return ResultSet
     */
    public static <T extends Record> DefaultJobInstanceRecordResultSet<T> createMultiQueryResultSet(
        AbstractJobInstanceHotRecordDAO<T> jobInstanceHotRecordDAO,
        Collection<Long> jobInstanceIds,
        long readRowLimit,
        Function<T, List<Condition>> offsetConditionBuilder) {
        return new DefaultJobInstanceRecordResultSet<>(
            jobInstanceHotRecordDAO,
            jobInstanceIds,
            readRowLimit,
            offsetConditionBuilder
        );
    }

    /**
     * 创建一个单次全量查询
     *
     * @param jobInstanceHotRecordDAO dao
     * @param jobInstanceIds          作业实例 ID 列表
     * @param readRowLimit            单次查询最大读取行数限制
     * @param <T>                     记录
     * @return ResultSet
     */
    public static <T extends Record> DefaultJobInstanceRecordResultSet<T> createOneQueryResultSet(
        AbstractJobInstanceHotRecordDAO<T> jobInstanceHotRecordDAO,
        Collection<Long> jobInstanceIds,
        long readRowLimit) {
        return new DefaultJobInstanceRecordResultSet<>(
            jobInstanceHotRecordDAO,
            jobInstanceIds,
            readRowLimit,
            lastRecord -> null
        );
    }

    public static class DefaultJobInstanceRecordResultSet<T extends Record>
        extends AbstractJobInstanceRecordResultSet<T> {

        Function<T, List<Condition>> offsetConditionBuilder;

        public DefaultJobInstanceRecordResultSet(
            AbstractJobInstanceHotRecordDAO<T> jobInstanceHotRecordDAO,
            Collection<Long> jobInstanceIds,
            long readRowLimit,
            Function<T, List<Condition>> offsetConditionBuilder) {
            super(jobInstanceHotRecordDAO, jobInstanceIds, readRowLimit);
            this.offsetConditionBuilder = offsetConditionBuilder;
        }

        @Override
        protected List<Condition> buildOffsetQueryConditions(T lastRecord) {
            return offsetConditionBuilder.apply(lastRecord);
        }
    }


}
