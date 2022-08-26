package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.generated.tables.StepInstanceRollingTask;
import org.jooq.generated.tables.records.StepInstanceRollingTaskRecord;

import java.util.Arrays;
import java.util.List;

/**
 * step_instance_rolling_task DAO
 */
public class StepInstanceRollingTaskRecordDAO extends AbstractExecuteRecordDAO<StepInstanceRollingTaskRecord> {

    private static final StepInstanceRollingTask TABLE = StepInstanceRollingTask.STEP_INSTANCE_ROLLING_TASK;

    private static final List<Field<?>> FIELDS =
        Arrays.asList(
            TABLE.ID,
            TABLE.STEP_INSTANCE_ID,
            TABLE.EXECUTE_COUNT,
            TABLE.BATCH,
            TABLE.START_TIME,
            TABLE.END_TIME,
            TABLE.TOTAL_TIME,
            TABLE.STATUS,
            TABLE.ROW_CREATE_TIME,
            TABLE.ROW_UPDATE_TIME
        );

    public StepInstanceRollingTaskRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
        super(context, archiveConfig);
    }

    @Override
    public List<Field<?>> listFields() {
        return FIELDS;
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
