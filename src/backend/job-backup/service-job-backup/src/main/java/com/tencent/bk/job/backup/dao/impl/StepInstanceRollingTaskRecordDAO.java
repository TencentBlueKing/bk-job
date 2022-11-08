package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.generated.tables.StepInstanceRollingTask;
import org.jooq.generated.tables.records.StepInstanceRollingTaskRecord;

/**
 * step_instance_rolling_task DAO
 */
public class StepInstanceRollingTaskRecordDAO extends AbstractExecuteRecordDAO<StepInstanceRollingTaskRecord> {

    private static final StepInstanceRollingTask TABLE = StepInstanceRollingTask.STEP_INSTANCE_ROLLING_TASK;

    public StepInstanceRollingTaskRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
        super(context, archiveConfig);
    }

    @Override
    public Table<StepInstanceRollingTaskRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<StepInstanceRollingTaskRecord, Long> getArchiveIdField() {
        return TABLE.STEP_INSTANCE_ID;
    }
}
