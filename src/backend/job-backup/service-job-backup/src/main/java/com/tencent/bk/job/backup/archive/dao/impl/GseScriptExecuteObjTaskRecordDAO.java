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

import com.tencent.bk.job.common.mysql.dynamic.ds.DSLContextProvider;
import com.tencent.bk.job.execute.model.tables.GseScriptExecuteObjTask;
import com.tencent.bk.job.execute.model.tables.records.GseScriptExecuteObjTaskRecord;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Condition;
import org.jooq.OrderField;
import org.jooq.Table;
import org.jooq.TableField;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * gse_script_execute_obj_task DAO
 */
public class GseScriptExecuteObjTaskRecordDAO extends AbstractJobInstanceHotRecordDAO<GseScriptExecuteObjTaskRecord> {

    private static final GseScriptExecuteObjTask TABLE = GseScriptExecuteObjTask.GSE_SCRIPT_EXECUTE_OBJ_TASK;
    private static final List<OrderField<?>> ORDER_FIELDS = new ArrayList<>();

    static {
        ORDER_FIELDS.add(GseScriptExecuteObjTask.GSE_SCRIPT_EXECUTE_OBJ_TASK.TASK_INSTANCE_ID.asc());
        ORDER_FIELDS.add(GseScriptExecuteObjTask.GSE_SCRIPT_EXECUTE_OBJ_TASK.ID.asc());
    }

    public GseScriptExecuteObjTaskRecordDAO(DSLContextProvider dslContextProvider) {
        super(dslContextProvider, TABLE.getName());
    }

    @Override
    public Table<GseScriptExecuteObjTaskRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<GseScriptExecuteObjTaskRecord, Long> getJobInstanceIdField() {
        return TABLE.TASK_INSTANCE_ID;
    }

    @Override
    protected Collection<? extends OrderField<?>> getListRecordsOrderFields() {
        return ORDER_FIELDS;
    }

    private class TableRecordResultSet implements RecordResultSet<GseScriptExecuteObjTaskRecord> {

        private final Collection<Long> jobInstanceIds;
        private final Long readRowLimit;
        private List<GseScriptExecuteObjTaskRecord> records;
        private boolean hasNext;
        private Long currentTaskInstanceId;
        private Long currentId;

        public TableRecordResultSet(Collection<Long> jobInstanceIds, Long readRowLimit) {
            this.jobInstanceIds = jobInstanceIds;
            this.readRowLimit = readRowLimit;
        }

        @Override
        public boolean next() {
            if (!hasNext) {
                return false;
            }

            records = query(TABLE,
                buildConditions(jobInstanceIds, currentTaskInstanceId, currentId), readRowLimit);
            if (CollectionUtils.isEmpty(records)) {
                hasNext = false;
                return false;
            } else {
                // readRowLimit == null 表示全量查询
                if (readRowLimit == null || records.size() < readRowLimit) {
                    hasNext = false;
                } else {
                    hasNext = true;
                    GseScriptExecuteObjTaskRecord last = records.get(records.size() - 1);
                    currentTaskInstanceId = last.get(TABLE.TASK_INSTANCE_ID);
                    currentId = last.get(TABLE.ID);
                }
                return true;
            }
        }

        private List<Condition> buildConditions(Collection<Long> jobInstanceIds,
                                                Long fromTaskInstanceId,
                                                Long fromId) {
            List<Condition> conditions = new ArrayList<>();
            conditions.add(TABLE.TASK_INSTANCE_ID.in(jobInstanceIds));
            if (fromTaskInstanceId != null) {
                conditions.add(TABLE.TASK_INSTANCE_ID.ge(fromTaskInstanceId));
            }
            if (fromId != null) {
                conditions.add(TABLE.ID.gt(fromId));
            }
            return conditions;
        }

        @Override
        public List<GseScriptExecuteObjTaskRecord> get() {
            return records;
        }
    }


}
