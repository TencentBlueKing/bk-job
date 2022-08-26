package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.generated.tables.StepInstance;
import org.jooq.generated.tables.records.StepInstanceRecord;

import java.util.Arrays;
import java.util.List;

import static org.jooq.impl.DSL.max;

/**
 * step_instance DAO
 */
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
            TABLE.STEP_ORDER,
            TABLE.BATCH,
            TABLE.ROLLING_CONFIG_ID
        );

    public StepInstanceRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
        super(context, archiveConfig);
    }

    @Override
    public List<Field<?>> listFields() {
        return FIELDS;
    }

    @Override
    public Table<StepInstanceRecord> getTable() {
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

    @Override
    public TableField<StepInstanceRecord, Long> getArchiveIdField() {
        return TABLE.ID;
    }
}
