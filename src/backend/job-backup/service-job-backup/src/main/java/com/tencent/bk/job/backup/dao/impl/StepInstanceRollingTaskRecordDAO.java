package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.execute.model.tables.StepInstanceRollingTask;
import com.tencent.bk.job.execute.model.tables.records.StepInstanceRollingTaskRecord;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.jooq.Table;
import org.jooq.TableField;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * step_instance_rolling_task DAO
 */
public class StepInstanceRollingTaskRecordDAO extends AbstractExecuteRecordDAO<StepInstanceRollingTaskRecord> {

    private static final StepInstanceRollingTask TABLE = StepInstanceRollingTask.STEP_INSTANCE_ROLLING_TASK;
    private static final List<OrderField<?>> ORDER_FIELDS = new ArrayList<>();

    static {
        ORDER_FIELDS.add(StepInstanceRollingTask.STEP_INSTANCE_ROLLING_TASK.STEP_INSTANCE_ID.asc());
        ORDER_FIELDS.add(StepInstanceRollingTask.STEP_INSTANCE_ROLLING_TASK.EXECUTE_COUNT.asc());
        ORDER_FIELDS.add(StepInstanceRollingTask.STEP_INSTANCE_ROLLING_TASK.BATCH.asc());
    }

    public StepInstanceRollingTaskRecordDAO(DSLContext context) {
        super(context);
    }

    @Override
    public Table<StepInstanceRollingTaskRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<StepInstanceRollingTaskRecord, Long> getArchiveIdField() {
        return TABLE.STEP_INSTANCE_ID;
    }

    @Override
    protected Collection<? extends OrderField<?>> getListRecordsOrderFields() {
        return ORDER_FIELDS;
    }
}
