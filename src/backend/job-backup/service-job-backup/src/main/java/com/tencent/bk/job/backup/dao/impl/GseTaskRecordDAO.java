package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveDBProperties;
import com.tencent.bk.job.execute.model.tables.GseTask;
import com.tencent.bk.job.execute.model.tables.records.GseTaskRecord;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;

public class GseTaskRecordDAO extends AbstractExecuteRecordDAO<GseTaskRecord> {

    private static final GseTask TABLE = GseTask.GSE_TASK;

    public GseTaskRecordDAO(DSLContext context, ArchiveDBProperties archiveDBProperties) {
        super(context, archiveDBProperties);
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
