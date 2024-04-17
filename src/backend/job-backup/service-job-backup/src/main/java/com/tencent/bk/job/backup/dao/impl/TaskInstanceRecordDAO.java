package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.execute.model.tables.TaskInstance;
import com.tencent.bk.job.execute.model.tables.records.TaskInstanceRecord;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Table;
import org.jooq.TableField;

import static org.jooq.impl.DSL.max;

/**
 * task_instance DAO
 */
public class TaskInstanceRecordDAO extends AbstractExecuteRecordDAO<TaskInstanceRecord> {

    private static final TaskInstance TABLE = TaskInstance.TASK_INSTANCE;

    public TaskInstanceRecordDAO(DSLContext context) {
        super(context);
    }

    @Override
    public Table<TaskInstanceRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<TaskInstanceRecord, Long> getArchiveIdField() {
        return TABLE.ID;
    }

    public Long getMaxId(Long endTime) {
        Record1<Long> record =
            context.select(max(TABLE.ID))
                .from(TABLE)
                .where(TABLE.CREATE_TIME.lessOrEqual(endTime))
                .fetchOne();
        if (record != null) {
            Long maxId = (Long) record.get(0);
            if (maxId != null) {
                return maxId;
            }
        }
        return 0L;
    }


}
