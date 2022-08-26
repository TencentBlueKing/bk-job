package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.generated.tables.GseScriptAgentTask;
import org.jooq.generated.tables.records.GseScriptAgentTaskRecord;

import java.util.Arrays;
import java.util.List;

/**
 * gse_script_agent_task DAO
 */
public class GseScriptAgentTaskRecordDAO extends AbstractExecuteRecordDAO<GseScriptAgentTaskRecord> {

    private static final GseScriptAgentTask TABLE = GseScriptAgentTask.GSE_SCRIPT_AGENT_TASK;

    private static final List<Field<?>> FIELDS =
        Arrays.asList(
            TABLE.ID,
            TABLE.STEP_INSTANCE_ID,
            TABLE.EXECUTE_COUNT,
            TABLE.ACTUAL_EXECUTE_COUNT,
            TABLE.BATCH,
            TABLE.HOST_ID,
            TABLE.AGENT_ID,
            TABLE.GSE_TASK_ID,
            TABLE.START_TIME,
            TABLE.END_TIME,
            TABLE.TOTAL_TIME,
            TABLE.STATUS,
            TABLE.ERROR_CODE,
            TABLE.EXIT_CODE,
            TABLE.TAG,
            TABLE.LOG_OFFSET,
            TABLE.ROW_CREATE_TIME,
            TABLE.ROW_UPDATE_TIME
        );

    public GseScriptAgentTaskRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
        super(context, archiveConfig);
    }

    @Override
    public List<Field<?>> listFields() {
        return FIELDS;
    }

    @Override
    public Table<GseScriptAgentTaskRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<GseScriptAgentTaskRecord, Long> getArchiveIdField() {
        return TABLE.STEP_INSTANCE_ID;
    }
}
