package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.generated.tables.TaskInstanceHost;
import org.jooq.generated.tables.records.TaskInstanceHostRecord;

public class TaskInstanceHostRecordDAO extends AbstractExecuteRecordDAO<TaskInstanceHostRecord> {

    private static final TaskInstanceHost TABLE = TaskInstanceHost.TASK_INSTANCE_HOST;

    public TaskInstanceHostRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
        super(context, archiveConfig);
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
