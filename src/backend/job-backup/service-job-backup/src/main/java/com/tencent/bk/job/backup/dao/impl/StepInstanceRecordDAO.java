package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.generated.tables.StepInstance;
import org.jooq.generated.tables.records.StepInstanceRecord;

import static org.jooq.impl.DSL.max;

/**
 * step_instance DAO
 */
public class StepInstanceRecordDAO extends AbstractExecuteRecordDAO<StepInstanceRecord> {

    private static final StepInstance TABLE = StepInstance.STEP_INSTANCE;

    public StepInstanceRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
        super(context, archiveConfig);
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
