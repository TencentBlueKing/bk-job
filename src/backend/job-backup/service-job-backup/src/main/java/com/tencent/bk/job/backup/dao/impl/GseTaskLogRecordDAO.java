package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveDBProperties;
import com.tencent.bk.job.execute.model.tables.GseTaskLog;
import com.tencent.bk.job.execute.model.tables.records.GseTaskLogRecord;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;

/**
 * gse_task_log DAO
 */
public class GseTaskLogRecordDAO extends AbstractExecuteRecordDAO<GseTaskLogRecord> {

    private static final GseTaskLog TABLE = GseTaskLog.GSE_TASK_LOG;

    public GseTaskLogRecordDAO(DSLContext context, ArchiveDBProperties archiveDBProperties) {
        super(context, archiveDBProperties);
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
