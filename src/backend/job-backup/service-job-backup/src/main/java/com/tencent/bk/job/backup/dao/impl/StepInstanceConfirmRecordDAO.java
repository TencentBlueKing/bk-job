package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.generated.tables.StepInstanceConfirm;
import org.jooq.generated.tables.records.StepInstanceConfirmRecord;

import java.util.Arrays;
import java.util.List;

/**
 * step_instance_confirm DAO
 */
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
    public Table<StepInstanceConfirmRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<StepInstanceConfirmRecord, Long> getArchiveIdField() {
        return TABLE.STEP_INSTANCE_ID;
    }
}
