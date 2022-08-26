package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.generated.tables.StepInstanceConfirm;
import org.jooq.generated.tables.records.StepInstanceConfirmRecord;

/**
 * step_instance_confirm DAO
 */
public class StepInstanceConfirmRecordDAO extends AbstractExecuteRecordDAO<StepInstanceConfirmRecord> {


    private static final StepInstanceConfirm TABLE = StepInstanceConfirm.STEP_INSTANCE_CONFIRM;

    public StepInstanceConfirmRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
        super(context, archiveConfig);
    }

    @Override
    public Table<StepInstanceConfirmRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<StepInstanceConfirmRecord, Long> getArchiveIdField() {
        return TABLE.STEP_INSTANCE_ID;
    }
}
