package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.common.mysql.dynamic.ds.DSLContextProvider;
import com.tencent.bk.job.execute.model.tables.StepInstanceConfirm;
import com.tencent.bk.job.execute.model.tables.records.StepInstanceConfirmRecord;
import org.jooq.Table;
import org.jooq.TableField;

/**
 * step_instance_confirm DAO
 */
public class StepInstanceConfirmRecordDAO extends AbstractExecuteRecordDAO<StepInstanceConfirmRecord> {


    private static final StepInstanceConfirm TABLE = StepInstanceConfirm.STEP_INSTANCE_CONFIRM;

    public StepInstanceConfirmRecordDAO(DSLContextProvider dslContextProvider) {
        super(dslContextProvider, TABLE.getName());
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
