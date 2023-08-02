package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveDBProperties;
import com.tencent.bk.job.execute.model.tables.GseFileAgentTask;
import com.tencent.bk.job.execute.model.tables.records.GseFileAgentTaskRecord;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;

/**
 * gse_file_agent_task DAO
 */
public class GseFileAgentTaskRecordDAO extends AbstractExecuteRecordDAO<GseFileAgentTaskRecord> {

    private static final GseFileAgentTask TABLE = GseFileAgentTask.GSE_FILE_AGENT_TASK;

    public GseFileAgentTaskRecordDAO(DSLContext context, ArchiveDBProperties archiveDBProperties) {
        super(context, archiveDBProperties);
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
