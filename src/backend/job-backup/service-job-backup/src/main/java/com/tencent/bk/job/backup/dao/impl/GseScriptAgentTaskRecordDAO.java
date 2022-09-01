package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.generated.tables.GseScriptAgentTask;
import org.jooq.generated.tables.records.GseScriptAgentTaskRecord;

/**
 * gse_script_agent_task DAO
 */
public class GseScriptAgentTaskRecordDAO extends AbstractExecuteRecordDAO<GseScriptAgentTaskRecord> {

    private static final GseScriptAgentTask TABLE = GseScriptAgentTask.GSE_SCRIPT_AGENT_TASK;

    public GseScriptAgentTaskRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
        super(context, archiveConfig);
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
