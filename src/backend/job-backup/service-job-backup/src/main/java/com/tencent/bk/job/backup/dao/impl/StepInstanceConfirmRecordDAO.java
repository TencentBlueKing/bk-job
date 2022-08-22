package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.generated.tables.StepInstanceConfirm;
import org.jooq.generated.tables.records.StepInstanceConfirmRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class StepInstanceConfirmRecordDAO extends AbstractExecuteRecordDAO<StepInstanceConfirmRecord> {


    private static final StepInstanceConfirm TABLE = StepInstanceConfirm.STEP_INSTANCE_CONFIRM;

    private static final List<Field<?>> FIELDS =
        Arrays.asList(
            TABLE.STEP_INSTANCE_ID,
            TABLE.CONFIRM_MESSAGE,
            TABLE.CONFIRM_USERS,
            TABLE.CONFIRM_ROLES,
            TABLE.NOTIFY_CHANNELS,
            TABLE.ROW_CREATE_TIME,
            TABLE.ROW_UPDATE_TIME,
            TABLE.CONFIRM_REASON
        );

    public StepInstanceConfirmRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
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
    protected Table<StepInstanceConfirmRecord> getTable() {
        return TABLE;
    }
}
