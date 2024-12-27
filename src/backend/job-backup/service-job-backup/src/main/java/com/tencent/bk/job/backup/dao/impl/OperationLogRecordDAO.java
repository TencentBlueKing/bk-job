package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.execute.model.tables.OperationLog;
import com.tencent.bk.job.execute.model.tables.records.OperationLogRecord;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;

/**
 * operation_log DAO
 */
public class OperationLogRecordDAO extends AbstractExecuteRecordDAO<OperationLogRecord> {

    private static final OperationLog TABLE = OperationLog.OPERATION_LOG;

    public OperationLogRecordDAO(DSLContext context) {
        super(context);
    }

    @Override
    public Table<OperationLogRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<OperationLogRecord, Long> getArchiveIdField() {
        return TABLE.TASK_INSTANCE_ID;
    }
}
