package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.execute.model.tables.GseScriptExecuteObjTask;
import com.tencent.bk.job.execute.model.tables.records.GseScriptExecuteObjTaskRecord;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.jooq.Table;
import org.jooq.TableField;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * gse_script_execute_obj_task DAO
 */
public class GseScriptExecuteObjTaskRecordDAO extends AbstractExecuteRecordDAO<GseScriptExecuteObjTaskRecord> {

    private static final GseScriptExecuteObjTask TABLE = GseScriptExecuteObjTask.GSE_SCRIPT_EXECUTE_OBJ_TASK;
    private static final List<OrderField<?>> ORDER_FIELDS = new ArrayList<>();

    static {
        ORDER_FIELDS.add(GseScriptExecuteObjTask.GSE_SCRIPT_EXECUTE_OBJ_TASK.STEP_INSTANCE_ID.asc());
        ORDER_FIELDS.add(GseScriptExecuteObjTask.GSE_SCRIPT_EXECUTE_OBJ_TASK.EXECUTE_COUNT.asc());
        ORDER_FIELDS.add(GseScriptExecuteObjTask.GSE_SCRIPT_EXECUTE_OBJ_TASK.BATCH.asc());
        ORDER_FIELDS.add(GseScriptExecuteObjTask.GSE_SCRIPT_EXECUTE_OBJ_TASK.EXECUTE_OBJ_ID.asc());
    }

    public GseScriptExecuteObjTaskRecordDAO(DSLContext context) {
        super(context);
    }

    @Override
    public Table<GseScriptExecuteObjTaskRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<GseScriptExecuteObjTaskRecord, Long> getArchiveIdField() {
        return TABLE.TASK_INSTANCE_ID;
    }

    @Override
    protected Collection<? extends OrderField<?>> getListRecordsOrderFields() {
        return ORDER_FIELDS;
    }
}
