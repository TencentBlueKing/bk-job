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

    /**
     * 获取作业实例ID范围内的步骤实例ID最大值
     *
     * @param taskInstanceId 作业实例ID
     * @return 步骤实例ID 最大值
     */
    public Long getMaxId(Long taskInstanceId) {
        Record1<Long> record =
            context.select(max(TABLE.ID))
                .from(TABLE)
                .where(TABLE.TASK_INSTANCE_ID.lessOrEqual(taskInstanceId))
                .fetchOne();
        if (record != null) {
            Long maxId = (Long) record.get(0);
            if (maxId != null) {
                return maxId;
            }
        }
        return 0L;
    }

    @Override
    public TableField<StepInstanceRecord, Long> getArchiveIdField() {
        return TABLE.ID;
    }
}
