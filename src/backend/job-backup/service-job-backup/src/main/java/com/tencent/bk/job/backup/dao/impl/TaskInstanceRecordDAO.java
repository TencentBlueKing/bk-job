package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.generated.tables.TaskInstance;
import org.jooq.generated.tables.records.TaskInstanceRecord;

import java.util.Arrays;
import java.util.List;

import static org.jooq.impl.DSL.max;

/**
 * task_instance DAO
 */
public class TaskInstanceRecordDAO extends AbstractExecuteRecordDAO<TaskInstanceRecord> {

    private static final TaskInstance TABLE = TaskInstance.TASK_INSTANCE;

    private static final List<Field<?>> FIELDS =
        Arrays.asList(
            TABLE.ID,
            TABLE.APP_ID,
            TABLE.TASK_ID,
            TABLE.TASK_TEMPLATE_ID,
            TABLE.NAME,
            TABLE.TYPE,
            TABLE.OPERATOR,
            TABLE.STATUS,
            TABLE.CURRENT_STEP_ID,
            TABLE.STARTUP_MODE,
            TABLE.TOTAL_TIME,
            TABLE.CALLBACK_URL,
            TABLE.IS_DEBUG_TASK,
            TABLE.CRON_TASK_ID,
            TABLE.CREATE_TIME,
            TABLE.START_TIME,
            TABLE.END_TIME,
            TABLE.APP_CODE,
            TABLE.ROW_CREATE_TIME,
            TABLE.ROW_UPDATE_TIME
        );

    public TaskInstanceRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
        super(context, archiveConfig);
    }

    @Override
    public List<Field<?>> listFields() {
        return FIELDS;
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
