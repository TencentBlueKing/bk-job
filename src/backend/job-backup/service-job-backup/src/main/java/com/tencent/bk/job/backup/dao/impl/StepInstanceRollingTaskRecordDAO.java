package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveDBProperties;
import com.tencent.bk.job.execute.model.tables.StepInstanceRollingTask;
import com.tencent.bk.job.execute.model.tables.records.StepInstanceRollingTaskRecord;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;

/**
 * step_instance_rolling_task DAO
 */
public class StepInstanceRollingTaskRecordDAO extends AbstractExecuteRecordDAO<StepInstanceRollingTaskRecord> {

    private static final StepInstanceRollingTask TABLE = StepInstanceRollingTask.STEP_INSTANCE_ROLLING_TASK;

    public StepInstanceRollingTaskRecordDAO(DSLContext context, ArchiveDBProperties archiveDBProperties) {
        super(context, archiveDBProperties);
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
