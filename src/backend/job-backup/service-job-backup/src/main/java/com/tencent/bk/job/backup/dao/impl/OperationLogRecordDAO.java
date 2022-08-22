package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.generated.tables.OperationLog;
import org.jooq.generated.tables.records.OperationLogRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OperationLogRecordDAO extends AbstractExecuteRecordDAO<OperationLogRecord> {

    private static final OperationLog TABLE = OperationLog.OPERATION_LOG;

    private static final List<Field<?>> FIELDS =
        Arrays.asList(
            TABLE.ID,
            TABLE.TASK_INSTANCE_ID,
            TABLE.OP_CODE,
            TABLE.OPERATOR,
            TABLE.DETAIL,
            TABLE.CREATE_TIME,
            TABLE.ROW_CREATE_TIME,
            TABLE.ROW_UPDATE_TIME
        );

    public OperationLogRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
        super(context, archiveConfig);
    }

    @Override
    public List<Field<?>> listFields() {
        return FIELDS;
    }

    @Override
    protected final List<Condition> buildConditions(Long start, Long end) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.TASK_INSTANCE_ID.greaterThan(start));
        conditions.add(TABLE.TASK_INSTANCE_ID.lessOrEqual(end));
        return conditions;
    }

    @Override
    protected Table<OperationLogRecord> getTable() {
        return TABLE;
    }
}
