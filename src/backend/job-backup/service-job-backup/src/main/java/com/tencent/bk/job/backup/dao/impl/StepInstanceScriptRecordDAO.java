package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.generated.tables.StepInstanceScript;
import org.jooq.generated.tables.records.StepInstanceScriptRecord;

/**
 * step_instance_script DAO
 */
public class StepInstanceScriptRecordDAO extends AbstractExecuteRecordDAO<StepInstanceScriptRecord> {

    private static final StepInstanceScript TABLE = StepInstanceScript.STEP_INSTANCE_SCRIPT;

    public StepInstanceScriptRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
        super(context, archiveConfig);
    }

    @Override
    public Table<StepInstanceScriptRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<StepInstanceScriptRecord, Long> getArchiveIdField() {
        return TABLE.STEP_INSTANCE_ID;
    }
}
