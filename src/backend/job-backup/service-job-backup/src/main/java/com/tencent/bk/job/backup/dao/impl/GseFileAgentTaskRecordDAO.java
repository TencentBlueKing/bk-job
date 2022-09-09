package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.generated.tables.GseFileAgentTask;
import org.jooq.generated.tables.records.GseFileAgentTaskRecord;

/**
 * gse_file_agent_task DAO
 */
public class GseFileAgentTaskRecordDAO extends AbstractExecuteRecordDAO<GseFileAgentTaskRecord> {

    private static final GseFileAgentTask TABLE = GseFileAgentTask.GSE_FILE_AGENT_TASK;

    public GseFileAgentTaskRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
        super(context, archiveConfig);
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
