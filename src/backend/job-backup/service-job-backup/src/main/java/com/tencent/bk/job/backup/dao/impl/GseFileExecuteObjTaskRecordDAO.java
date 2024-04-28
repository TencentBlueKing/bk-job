package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.execute.model.tables.GseFileExecuteObjTask;
import com.tencent.bk.job.execute.model.tables.records.GseFileExecuteObjTaskRecord;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;

/**
 * gse_file_execute_obj_task DAO
 */
public class GseFileExecuteObjTaskRecordDAO extends AbstractExecuteRecordDAO<GseFileExecuteObjTaskRecord> {

    private static final GseFileExecuteObjTask TABLE = GseFileExecuteObjTask.GSE_FILE_EXECUTE_OBJ_TASK;

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
}
