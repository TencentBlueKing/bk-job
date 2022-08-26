package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.generated.tables.StepInstanceVariable;
import org.jooq.generated.tables.records.StepInstanceVariableRecord;

import java.util.Arrays;
import java.util.List;

/**
 * step_instance_variable DAO
 */
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
    public Table<StepInstanceVariableRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<StepInstanceVariableRecord, Long> getArchiveIdField() {
        return TABLE.STEP_INSTANCE_ID;
    }
}
