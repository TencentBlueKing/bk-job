package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.generated.tables.FileSourceTaskLog;
import org.jooq.generated.tables.records.FileSourceTaskLogRecord;

/**
 * file_source_task DAO
 */
public class FileSourceTaskRecordDAO extends AbstractExecuteRecordDAO<FileSourceTaskLogRecord> {

    private static final FileSourceTaskLog TABLE = FileSourceTaskLog.FILE_SOURCE_TASK_LOG;

    public FileSourceTaskRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
        super(context, archiveConfig);
    }

    @Override
    public Table<FileSourceTaskLogRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<FileSourceTaskLogRecord, Long> getArchiveIdField() {
        return TABLE.STEP_INSTANCE_ID;
    }
}
