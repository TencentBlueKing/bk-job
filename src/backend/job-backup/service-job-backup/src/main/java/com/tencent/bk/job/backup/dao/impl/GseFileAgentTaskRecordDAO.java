package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.execute.model.tables.GseFileAgentTask;
import com.tencent.bk.job.execute.model.tables.records.GseFileAgentTaskRecord;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.jooq.Table;
import org.jooq.TableField;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * gse_file_agent_task DAO
 */
public class GseFileAgentTaskRecordDAO extends AbstractExecuteRecordDAO<GseFileAgentTaskRecord> {

    private static final GseFileAgentTask TABLE = GseFileAgentTask.GSE_FILE_AGENT_TASK;
    private static final List<OrderField<?>> ORDER_FIELDS = new ArrayList<>();

    static {
        ORDER_FIELDS.add(GseFileAgentTask.GSE_FILE_AGENT_TASK.STEP_INSTANCE_ID.asc());
        ORDER_FIELDS.add(GseFileAgentTask.GSE_FILE_AGENT_TASK.EXECUTE_COUNT.asc());
        ORDER_FIELDS.add(GseFileAgentTask.GSE_FILE_AGENT_TASK.BATCH.asc());
        ORDER_FIELDS.add(GseFileAgentTask.GSE_FILE_AGENT_TASK.MODE.asc());
        ORDER_FIELDS.add(GseFileAgentTask.GSE_FILE_AGENT_TASK.HOST_ID.asc());
    }

    public GseFileAgentTaskRecordDAO(DSLContext context) {
        super(context);
    }

    @Override
    public Table<GseFileAgentTaskRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<GseFileAgentTaskRecord, Long> getArchiveIdField() {
        return TABLE.STEP_INSTANCE_ID;
    }

    @Override
    protected Collection<? extends OrderField<?>> getListRecordsOrderFields() {
        return ORDER_FIELDS;
    }
}
