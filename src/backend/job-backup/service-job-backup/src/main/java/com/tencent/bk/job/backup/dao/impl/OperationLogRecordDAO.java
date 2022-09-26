package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.generated.tables.OperationLog;
import org.jooq.generated.tables.records.OperationLogRecord;

/**
 * operation_log DAO
 */
public class OperationLogRecordDAO extends AbstractExecuteRecordDAO<OperationLogRecord> {

    private static final OperationLog TABLE = OperationLog.OPERATION_LOG;

    public OperationLogRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
        super(context, archiveConfig);
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
