package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.generated.tables.TaskInstanceVariable;
import org.jooq.generated.tables.records.TaskInstanceVariableRecord;

/**
 * task_instance_variable DAO
 */
public class TaskInstanceVariableRecordDAO extends AbstractExecuteRecordDAO<TaskInstanceVariableRecord> {

    private static final TaskInstanceVariable TABLE = TaskInstanceVariable.TASK_INSTANCE_VARIABLE;

    public TaskInstanceVariableRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
        super(context, archiveConfig);
    }

    @Override
    public Table<TaskInstanceVariableRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<TaskInstanceVariableRecord, Long> getArchiveIdField() {
        return TABLE.TASK_INSTANCE_ID;
    }
}
