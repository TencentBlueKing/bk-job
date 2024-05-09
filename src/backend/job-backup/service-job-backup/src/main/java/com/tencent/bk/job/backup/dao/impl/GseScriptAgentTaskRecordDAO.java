package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.execute.model.tables.GseFileAgentTask;
import com.tencent.bk.job.execute.model.tables.GseScriptAgentTask;
import com.tencent.bk.job.execute.model.tables.records.GseScriptAgentTaskRecord;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.jooq.Table;
import org.jooq.TableField;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * gse_script_agent_task DAO
 */
public class GseScriptAgentTaskRecordDAO extends AbstractExecuteRecordDAO<GseScriptAgentTaskRecord> {

    private static final GseScriptAgentTask TABLE = GseScriptAgentTask.GSE_SCRIPT_AGENT_TASK;
    private static final List<OrderField<?>> ORDER_FIELDS = new ArrayList<>();

    static {
        ORDER_FIELDS.add(GseScriptAgentTask.GSE_SCRIPT_AGENT_TASK.STEP_INSTANCE_ID.asc());
        ORDER_FIELDS.add(GseScriptAgentTask.GSE_SCRIPT_AGENT_TASK.EXECUTE_COUNT.asc());
        ORDER_FIELDS.add(GseScriptAgentTask.GSE_SCRIPT_AGENT_TASK.BATCH.asc());
        ORDER_FIELDS.add(GseScriptAgentTask.GSE_SCRIPT_AGENT_TASK.HOST_ID.asc());
    }

    public GseScriptAgentTaskRecordDAO(DSLContext context) {
        super(context);
    }

    @Override
    public Table<GseScriptAgentTaskRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<GseScriptAgentTaskRecord, Long> getArchiveIdField() {
        return TABLE.STEP_INSTANCE_ID;
    }

    @Override
    protected Collection<? extends OrderField<?>> getListRecordsOrderFields() {
        return ORDER_FIELDS;
    }
}
