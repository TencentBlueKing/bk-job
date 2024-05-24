package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.execute.model.tables.StepInstanceVariable;
import com.tencent.bk.job.execute.model.tables.records.StepInstanceVariableRecord;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.jooq.Table;
import org.jooq.TableField;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * step_instance_variable DAO
 */
public class StepInstanceVariableRecordDAO extends AbstractExecuteRecordDAO<StepInstanceVariableRecord> {

    private static final StepInstanceVariable TABLE = StepInstanceVariable.STEP_INSTANCE_VARIABLE;
    private static final List<OrderField<?>> ORDER_FIELDS = new ArrayList<>();

    static {
        ORDER_FIELDS.add(StepInstanceVariable.STEP_INSTANCE_VARIABLE.STEP_INSTANCE_ID.asc());
        ORDER_FIELDS.add(StepInstanceVariable.STEP_INSTANCE_VARIABLE.EXECUTE_COUNT.asc());
    }

    public StepInstanceVariableRecordDAO(DSLContext context) {
        super(context);
    }

    @Override
    public Table<StepInstanceVariableRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<StepInstanceVariableRecord, Long> getArchiveIdField() {
        return TABLE.STEP_INSTANCE_ID;
    }

    @Override
    protected Collection<? extends OrderField<?>> getListRecordsOrderFields() {
        return ORDER_FIELDS;
    }
}
