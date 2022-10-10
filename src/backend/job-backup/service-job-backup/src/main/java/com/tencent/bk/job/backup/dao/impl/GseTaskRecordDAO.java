package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.generated.tables.GseTask;
import org.jooq.generated.tables.records.GseTaskRecord;

public class GseTaskRecordDAO extends AbstractExecuteRecordDAO<GseTaskRecord> {

    private static final GseTask TABLE = GseTask.GSE_TASK;

    public GseTaskRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
        super(context, archiveConfig);
    }

    @Override
    public Table<GseTaskRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<GseTaskRecord, Long> getArchiveIdField() {
        return TABLE.STEP_INSTANCE_ID;
    }
}
