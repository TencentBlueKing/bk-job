package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Table;
import org.jooq.generated.tables.StepInstance;
import org.jooq.generated.tables.records.StepInstanceRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.jooq.impl.DSL.max;

public class StepInstanceRecordDAO extends AbstractExecuteRecordDAO<StepInstanceRecord> {

    private static final StepInstance TABLE = StepInstance.STEP_INSTANCE;

    private static final List<Field<?>> FIELDS =
        Arrays.asList(
            TABLE.ID,
            TABLE.STEP_ID,
            TABLE.TASK_INSTANCE_ID,
            TABLE.APP_ID,
            TABLE.NAME,
            TABLE.TYPE,
            TABLE.OPERATOR,
            TABLE.STATUS,
            TABLE.EXECUTE_COUNT,
            TABLE.TARGET_SERVERS,
            TABLE.ABNORMAL_AGENT_IP_LIST,
            TABLE.START_TIME,
            TABLE.END_TIME,
            TABLE.TOTAL_TIME,
            TABLE.TOTAL_IP_NUM,
            TABLE.ABNORMAL_AGENT_NUM,
            TABLE.RUN_IP_NUM,
            TABLE.FAIL_IP_NUM,
            TABLE.SUCCESS_IP_NUM,
            TABLE.CREATE_TIME,
            TABLE.IGNORE_ERROR,
            TABLE.ROW_CREATE_TIME,
            TABLE.ROW_UPDATE_TIME,
            TABLE.STEP_NUM,
            TABLE.STEP_ORDER
        );

    public StepInstanceRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
        super(context, archiveConfig);
    }

    @Override
    public List<Field<?>> listFields() {
        return FIELDS;
    }

    @Override
    protected final List<Condition> buildConditions(Long start, Long end) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.greaterThan(start));
        conditions.add(TABLE.ID.lessOrEqual(end));
        return conditions;
    }

    @Override
    protected Table<StepInstanceRecord> getTable() {
        return TABLE;
    }

    public Long getMaxNeedArchiveStepInstanceId(Long taskInstanceId) {
        Record1<Long> maxNeedStepInstanceIdRecord =
            context.select(max(TABLE.ID))
                .from(TABLE)
                .where(TABLE.TASK_INSTANCE_ID.lessOrEqual(taskInstanceId))
                .fetchOne();
        if (maxNeedStepInstanceIdRecord != null) {
            Long maxNeedStepInstanceId = (Long) maxNeedStepInstanceIdRecord.get(0);
            if (maxNeedStepInstanceId != null) {
                return maxNeedStepInstanceId;
            }
        }
        return 0L;
    }

}
