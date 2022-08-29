package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.generated.tables.TaskInstance;
import org.jooq.generated.tables.records.TaskInstanceRecord;

import static org.jooq.impl.DSL.max;

/**
 * task_instance DAO
 */
public class TaskInstanceRecordDAO extends AbstractExecuteRecordDAO<TaskInstanceRecord> {

    private static final TaskInstance TABLE = TaskInstance.TASK_INSTANCE;

    public TaskInstanceRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
        super(context, archiveConfig);
    }

    @Override
    public Table<TaskInstanceRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<TaskInstanceRecord, Long> getArchiveIdField() {
        return TABLE.ID;
    }

    public Long getMaxNeedArchiveTaskInstanceId(Long endTime) {
        Record1<Long> maxNeedTaskInstanceIdRecord =
            context.select(max(TABLE.ID))
                .from(TABLE)
                .where(TABLE.CREATE_TIME.lessOrEqual(endTime))
                .fetchOne();
        if (maxNeedTaskInstanceIdRecord != null) {
            Long maxNeedTaskInstanceId = (Long) maxNeedTaskInstanceIdRecord.get(0);
            if (maxNeedTaskInstanceId != null) {
                return maxNeedTaskInstanceId;
            }
        }
        return 0L;
    }


}
