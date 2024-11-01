package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.common.mysql.dynamic.ds.DSLContextProvider;
import com.tencent.bk.job.execute.model.tables.TaskInstanceVariable;
import com.tencent.bk.job.execute.model.tables.records.TaskInstanceVariableRecord;
import org.jooq.Table;
import org.jooq.TableField;

/**
 * task_instance_variable DAO
 */
public class TaskInstanceVariableRecordDAO extends AbstractExecuteRecordDAO<TaskInstanceVariableRecord> {

    private static final TaskInstanceVariable TABLE = TaskInstanceVariable.TASK_INSTANCE_VARIABLE;

    public TaskInstanceVariableRecordDAO(DSLContextProvider dslContextProvider) {
        super(dslContextProvider, TABLE.getName());
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
