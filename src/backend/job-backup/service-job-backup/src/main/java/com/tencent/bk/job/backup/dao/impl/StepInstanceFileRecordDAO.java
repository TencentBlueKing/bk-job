package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.generated.tables.StepInstanceFile;
import org.jooq.generated.tables.records.StepInstanceFileRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StepInstanceFileRecordDAO extends AbstractExecuteRecordDAO<StepInstanceFileRecord> {

    private static final StepInstanceFile TABLE = StepInstanceFile.STEP_INSTANCE_FILE;

    private static final List<Field<?>> FIELDS =
        Arrays.asList(
            TABLE.STEP_INSTANCE_ID,
            TABLE.FILE_SOURCE,
            TABLE.RESOLVED_FILE_SOURCE,
            TABLE.FILE_TARGET_PATH,
            TABLE.RESOLVED_FILE_TARGET_PATH,
            TABLE.FILE_UPLOAD_SPEED_LIMIT,
            TABLE.FILE_DOWNLOAD_SPEED_LIMIT,
            TABLE.FILE_DUPLICATE_HANDLE,
            TABLE.NOT_EXIST_PATH_HANDLER,
            TABLE.EXECUTION_TIMEOUT,
            TABLE.SYSTEM_ACCOUNT_ID,
            TABLE.SYSTEM_ACCOUNT,
            TABLE.ROW_CREATE_TIME,
            TABLE.ROW_UPDATE_TIME
        );

    public StepInstanceFileRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
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
    protected Table<StepInstanceFileRecord> getTable() {
        return TABLE;
    }
}
