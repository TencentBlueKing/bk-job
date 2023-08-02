package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveDBProperties;
import com.tencent.bk.job.execute.model.tables.TaskInstanceVariable;
import com.tencent.bk.job.execute.model.tables.records.TaskInstanceVariableRecord;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;

/**
 * task_instance_variable DAO
 */
public class TaskInstanceVariableRecordDAO extends AbstractExecuteRecordDAO<TaskInstanceVariableRecord> {

    private static final TaskInstanceVariable TABLE = TaskInstanceVariable.TASK_INSTANCE_VARIABLE;

    public TaskInstanceVariableRecordDAO(DSLContext context, ArchiveDBProperties archiveDBProperties) {
        super(context, archiveDBProperties);
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
