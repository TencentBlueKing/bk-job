package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveDBProperties;
import com.tencent.bk.job.execute.model.tables.GseScriptExecuteObjTask;
import com.tencent.bk.job.execute.model.tables.records.GseScriptExecuteObjTaskRecord;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;

/**
 * gse_script_execute_obj_task DAO
 */
public class GseScriptExecuteObjTaskRecordDAO extends AbstractExecuteRecordDAO<GseScriptExecuteObjTaskRecord> {

    private static final GseScriptExecuteObjTask TABLE = GseScriptExecuteObjTask.GSE_SCRIPT_EXECUTE_OBJ_TASK;

    public GseScriptExecuteObjTaskRecordDAO(DSLContext context, ArchiveDBProperties archiveDBProperties) {
        super(context, archiveDBProperties);
    }

    @Override
    public Table<GseScriptExecuteObjTaskRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<GseScriptExecuteObjTaskRecord, Long> getArchiveIdField() {
        return TABLE.TASK_INSTANCE_ID;
    }
}
