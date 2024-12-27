package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.execute.model.tables.TaskInstanceHost;
import com.tencent.bk.job.execute.model.tables.records.TaskInstanceHostRecord;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;

public class TaskInstanceHostRecordDAO extends AbstractExecuteRecordDAO<TaskInstanceHostRecord> {

    private static final TaskInstanceHost TABLE = TaskInstanceHost.TASK_INSTANCE_HOST;

    public TaskInstanceHostRecordDAO(DSLContext context) {
        super(context);
    }

    @Override
    public Table<TaskInstanceHostRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<TaskInstanceHostRecord, Long> getArchiveIdField() {
        return TABLE.TASK_INSTANCE_ID;
    }
}
