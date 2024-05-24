package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.execute.model.tables.GseFileExecuteObjTask;
import com.tencent.bk.job.execute.model.tables.records.GseFileExecuteObjTaskRecord;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.jooq.Table;
import org.jooq.TableField;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * gse_file_execute_obj_task DAO
 */
public class GseFileExecuteObjTaskRecordDAO extends AbstractExecuteRecordDAO<GseFileExecuteObjTaskRecord> {

    private static final GseFileExecuteObjTask TABLE = GseFileExecuteObjTask.GSE_FILE_EXECUTE_OBJ_TASK;
    private static final List<OrderField<?>> ORDER_FIELDS = new ArrayList<>();

    static {
        ORDER_FIELDS.add(GseFileExecuteObjTask.GSE_FILE_EXECUTE_OBJ_TASK.STEP_INSTANCE_ID.asc());
        ORDER_FIELDS.add(GseFileExecuteObjTask.GSE_FILE_EXECUTE_OBJ_TASK.EXECUTE_COUNT.asc());
        ORDER_FIELDS.add(GseFileExecuteObjTask.GSE_FILE_EXECUTE_OBJ_TASK.BATCH.asc());
        ORDER_FIELDS.add(GseFileExecuteObjTask.GSE_FILE_EXECUTE_OBJ_TASK.MODE.asc());
        ORDER_FIELDS.add(GseFileExecuteObjTask.GSE_FILE_EXECUTE_OBJ_TASK.EXECUTE_OBJ_ID.asc());
    }

    public GseFileExecuteObjTaskRecordDAO(DSLContext context) {
        super(context);
    }

    @Override
    public Table<GseFileExecuteObjTaskRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<GseFileExecuteObjTaskRecord, Long> getArchiveIdField() {
        return TABLE.TASK_INSTANCE_ID;
    }

    @Override
    protected Collection<? extends OrderField<?>> getListRecordsOrderFields() {
        return ORDER_FIELDS;
    }
}
