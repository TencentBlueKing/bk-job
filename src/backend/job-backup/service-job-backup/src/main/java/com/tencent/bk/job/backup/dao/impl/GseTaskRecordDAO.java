package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.generated.tables.GseTask;
import org.jooq.generated.tables.records.GseTaskRecord;

import java.util.Arrays;
import java.util.List;

public class GseTaskRecordDAO extends AbstractExecuteRecordDAO<GseTaskRecord> {

    private static final GseTask TABLE = GseTask.GSE_TASK;

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
            TABLE.GSE_TASK_ID,
            TABLE.ROW_CREATE_TIME,
            TABLE.ROW_UPDATE_TIME
        );

    public GseTaskRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
        super(context, archiveConfig);
    }

    @Override
    public List<Field<?>> listFields() {
        return FIELDS;
    }

    @Override
    public Table<GseTaskRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<GseTaskRecord, Long> getArchiveIdField() {
        return TABLE.STEP_INSTANCE_ID;
    }
}
