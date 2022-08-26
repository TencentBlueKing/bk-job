package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.generated.tables.StepInstanceFile;
import org.jooq.generated.tables.records.StepInstanceFileRecord;

/**
 * step_instance_file DAO
 */
public class StepInstanceFileRecordDAO extends AbstractExecuteRecordDAO<StepInstanceFileRecord> {

    private static final StepInstanceFile TABLE = StepInstanceFile.STEP_INSTANCE_FILE;

    public StepInstanceFileRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
        super(context, archiveConfig);
    }

    @Override
    public Table<StepInstanceFileRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<StepInstanceFileRecord, Long> getArchiveIdField() {
        return TABLE.STEP_INSTANCE_ID;
    }
}
