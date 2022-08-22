package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.generated.tables.StepInstanceVariable;
import org.jooq.generated.tables.records.StepInstanceVariableRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StepInstanceVariableRecordDAO extends AbstractExecuteRecordDAO<StepInstanceVariableRecord> {

    private static final StepInstanceVariable TABLE =
        StepInstanceVariable.STEP_INSTANCE_VARIABLE;
    private static final List<Field<?>> FIELDS =
        Arrays.asList(
            TABLE.TASK_INSTANCE_ID,
            TABLE.STEP_INSTANCE_ID,
            TABLE.TYPE,
            TABLE.PARAM_VALUES,
            TABLE.ROW_CREATE_TIME,
            TABLE.ROW_UPDATE_TIME
        );

    public StepInstanceVariableRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
        super(context, archiveConfig);
    }

    @Override
    public List<Field<?>> listFields() {
        return FIELDS;
    }

    @Override
    protected final List<Condition> buildConditions(Long start, Long end) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.STEP_INSTANCE_ID.greaterThan(start));
        conditions.add(TABLE.STEP_INSTANCE_ID.lessOrEqual(end));
        return conditions;
    }

    @Override
    protected Table<StepInstanceVariableRecord> getTable() {
        return TABLE;
    }
}
