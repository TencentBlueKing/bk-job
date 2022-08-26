package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.generated.tables.GseFileAgentTask;
import org.jooq.generated.tables.records.GseFileAgentTaskRecord;

import java.util.Arrays;
import java.util.List;

/**
 * gse_file_agent_task DAO
 */
public class GseFileAgentTaskRecordDAO extends AbstractExecuteRecordDAO<GseFileAgentTaskRecord> {

    private static final GseFileAgentTask TABLE = GseFileAgentTask.GSE_FILE_AGENT_TASK;

    private static final List<Field<?>> FIELDS =
        Arrays.asList(
            TABLE.ID,
            TABLE.STEP_INSTANCE_ID,
            TABLE.EXECUTE_COUNT,
            TABLE.ACTUAL_EXECUTE_COUNT,
            TABLE.BATCH,
            TABLE.MODE,
            TABLE.HOST_ID,
            TABLE.AGENT_ID,
            TABLE.GSE_TASK_ID,
            TABLE.START_TIME,
            TABLE.END_TIME,
            TABLE.TOTAL_TIME,
            TABLE.STATUS,
            TABLE.ERROR_CODE,
            TABLE.ROW_CREATE_TIME,
            TABLE.ROW_UPDATE_TIME
        );

    public GseFileAgentTaskRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
        super(context, archiveConfig);
    }

    @Override
    public List<Field<?>> listFields() {
        return FIELDS;
    }

    @Override
    public Table<GseFileAgentTaskRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<GseFileAgentTaskRecord, Long> getArchiveIdField() {
        return TABLE.STEP_INSTANCE_ID;
    }
}
