package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.generated.tables.GseTaskLog;
import org.jooq.generated.tables.records.GseTaskLogRecord;

/**
 * gse_task_log DAO
 */
public class GseTaskLogRecordDAO extends AbstractExecuteRecordDAO<GseTaskLogRecord> {

    private static final GseTaskLog TABLE = GseTaskLog.GSE_TASK_LOG;

    public GseTaskLogRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
        super(context, archiveConfig);
    }

    @Override
    public Table<GseTaskLogRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<GseTaskLogRecord, Long> getArchiveIdField() {
        return TABLE.STEP_INSTANCE_ID;
    }
}
