package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveDBProperties;
import com.tencent.bk.job.execute.model.tables.GseScriptAgentTask;
import com.tencent.bk.job.execute.model.tables.records.GseScriptAgentTaskRecord;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;

/**
 * gse_script_agent_task DAO
 */
public class GseScriptAgentTaskRecordDAO extends AbstractExecuteRecordDAO<GseScriptAgentTaskRecord> {

    private static final GseScriptAgentTask TABLE = GseScriptAgentTask.GSE_SCRIPT_AGENT_TASK;

    public GseScriptAgentTaskRecordDAO(DSLContext context, ArchiveDBProperties archiveDBProperties) {
        super(context, archiveDBProperties);
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
