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
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Condition;
import org.jooq.Record;

import java.util.Collection;
import java.util.List;

/**
 * 作业实例数据查询结果集
 *
 * @param <T>
 */
abstract class AbstractJobInstanceRecordResultSet<T extends Record> implements RecordResultSet<T> {
    private final AbstractJobInstanceHotRecordDAO<T> jobInstanceHotRecordDAO;
    private final Collection<Long> jobInstanceIds;
    private final long readRowLimit;
    /**
     * 当前查询的记录列表
     */
    private List<T> records;
    /**
     * 当前查询的最后一条记录
     */
    private T lastRecord;
    private boolean hasNext = true;


    public AbstractJobInstanceRecordResultSet(AbstractJobInstanceHotRecordDAO<T> jobInstanceHotRecordDAO,
                                              Collection<Long> jobInstanceIds,
                                              long readRowLimit) {
        this.jobInstanceHotRecordDAO = jobInstanceHotRecordDAO;
        this.jobInstanceIds = jobInstanceIds;
        this.readRowLimit = readRowLimit;
    }

    @Override
    public boolean next() {
        if (!hasNext) {
            return false;
        }
        if (lastRecord == null) {
            // 首次查询
            records = jobInstanceHotRecordDAO.listRecords(jobInstanceIds, readRowLimit);
        } else {
            // 非首次查询，需要加入数据查询偏移条件
            List<Condition> offsetConditions = buildOffsetQueryConditions(lastRecord);
            if (CollectionUtils.isEmpty(offsetConditions)) {
                // 如果构造的数据查询偏移条件为空，不再继续查询
                hasNext = false;
                records = null;
                lastRecord = null;
                return false;
            }
            List<Condition> conditions = jobInstanceHotRecordDAO.buildBasicConditions(jobInstanceIds);
            conditions.addAll(offsetConditions);
            records = jobInstanceHotRecordDAO.listRecordsByConditions(conditions, readRowLimit);
        }

        if (CollectionUtils.isEmpty(records)) {
            hasNext = false;
            lastRecord = null;
            return false;
        } else {
            lastRecord = records.get(records.size() - 1);
            hasNext = records.size() >= readRowLimit;
            return true;
        }
    }

    /**
     * 构造数据查询偏移条件
     *
     * @param lastRecord 当前最后一条记录
     * @return 数据查询偏移条件
     */
    protected abstract List<Condition> buildOffsetQueryConditions(T lastRecord);

    @Override
    public List<T> getRecords() {
        return records;
    }
}
