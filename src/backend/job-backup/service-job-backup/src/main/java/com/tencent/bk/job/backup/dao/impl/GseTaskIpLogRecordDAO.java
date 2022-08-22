package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.generated.tables.GseTaskIpLog;
import org.jooq.generated.tables.records.GseTaskIpLogRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GseTaskIpLogRecordDAO extends AbstractExecuteRecordDAO<GseTaskIpLogRecord> {

    private static final GseTaskIpLog TABLE = GseTaskIpLog.GSE_TASK_IP_LOG;

    private static final List<Field<?>> FIELDS =
        Arrays.asList(
            TABLE.STEP_INSTANCE_ID,
            TABLE.EXECUTE_COUNT,
            TABLE.IP,
            TABLE.STATUS,
            TABLE.START_TIME,
            TABLE.END_TIME,
            TABLE.TOTAL_TIME,
            TABLE.ERROR_CODE,
            TABLE.EXIT_CODE,
            TABLE.TAG,
            TABLE.LOG_OFFSET,
            TABLE.DISPLAY_IP,
            TABLE.IS_TARGET,
            TABLE.ROW_CREATE_TIME,
            TABLE.ROW_UPDATE_TIME,
            TABLE.IS_SOURCE
        );

    public GseTaskIpLogRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
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
    protected Table<GseTaskIpLogRecord> getTable() {
        return TABLE;
    }
}
