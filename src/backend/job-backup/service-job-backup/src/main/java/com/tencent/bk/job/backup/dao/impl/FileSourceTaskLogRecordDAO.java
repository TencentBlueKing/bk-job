package com.tencent.bk.job.backup.dao.impl;


import com.tencent.bk.job.execute.model.tables.FileSourceTaskLog;
import com.tencent.bk.job.execute.model.tables.records.FileSourceTaskLogRecord;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.jooq.Table;
import org.jooq.TableField;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * file_source_task_log DAO
 */
public class FileSourceTaskLogRecordDAO extends AbstractExecuteRecordDAO<FileSourceTaskLogRecord> {

    private static final FileSourceTaskLog TABLE = FileSourceTaskLog.FILE_SOURCE_TASK_LOG;
    private static final List<OrderField<?>> ORDER_FIELDS = new ArrayList<>();

    static {
        ORDER_FIELDS.add(FileSourceTaskLog.FILE_SOURCE_TASK_LOG.STEP_INSTANCE_ID.asc());
        ORDER_FIELDS.add(FileSourceTaskLog.FILE_SOURCE_TASK_LOG.EXECUTE_COUNT.asc());
    }

    public FileSourceTaskLogRecordDAO(DSLContext context) {
        super(context);
    }

    @Override
    public Table<FileSourceTaskLogRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<FileSourceTaskLogRecord, Long> getArchiveIdField() {
        return TABLE.STEP_INSTANCE_ID;
    }

    @Override
    protected Collection<? extends OrderField<?>> getListRecordsOrderFields() {
        return ORDER_FIELDS;
    }
}
