package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.generated.tables.StepInstanceVariable;
import org.jooq.generated.tables.records.StepInstanceVariableRecord;

/**
 * step_instance_variable DAO
 */
public class StepInstanceVariableRecordDAO extends AbstractExecuteRecordDAO<StepInstanceVariableRecord> {

    private static final StepInstanceVariable TABLE = StepInstanceVariable.STEP_INSTANCE_VARIABLE;

    public StepInstanceVariableRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
        super(context, archiveConfig);
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
