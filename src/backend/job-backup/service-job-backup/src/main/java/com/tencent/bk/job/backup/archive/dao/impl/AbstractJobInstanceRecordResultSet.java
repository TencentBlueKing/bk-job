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

package com.tencent.bk.job.backup.archive.dao.impl;

import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Condition;
import org.jooq.Record;

import java.util.Collection;
import java.util.List;


abstract class AbstractJobInstanceRecordResultSet<T extends Record> implements RecordResultSet<T> {
    private final AbstractJobInstanceHotRecordDAO<T> jobInstanceHotRecordDAO;
    protected final Collection<Long> jobInstanceIds;
    protected final Long readRowLimit;

    protected List<T> records;
    protected boolean hasNext;

    public AbstractJobInstanceRecordResultSet(AbstractJobInstanceHotRecordDAO<T> jobInstanceHotRecordDAO,
                                              Collection<Long> jobInstanceIds,
                                              Long readRowLimit) {
        this.jobInstanceHotRecordDAO = jobInstanceHotRecordDAO;
        this.jobInstanceIds = jobInstanceIds;
        this.readRowLimit = readRowLimit;
    }

    @Override
    public boolean next() {
        if (!hasNext) {
            return false;
        }

//        records = jobInstanceHotRecordDAO.listRecords(jobInstanceIds,
//            buildQueryConditions(), readRowLimit);

        if (CollectionUtils.isEmpty(records)) {
            hasNext = false;
            return false;
        } else {
            // readRowLimit == null 表示全量查询
            if (readRowLimit == null || records.size() < readRowLimit) {
                hasNext = false;
            } else {
                hasNext = true;
                T last = records.get(records.size() - 1);
                saveQueryCursor(last);
            }
            return true;
        }
    }

    /**
     * 构造查询条件
     *
     * @return 查询条件
     */
    protected abstract List<Condition> buildQueryConditions();

    /**
     * 保存查询游标
     *
     * @param lastRecord 本次查询的最后一条记录
     */
    protected abstract void saveQueryCursor(T lastRecord);

    @Override
    public List<T> get() {
        return records;
    }
}
