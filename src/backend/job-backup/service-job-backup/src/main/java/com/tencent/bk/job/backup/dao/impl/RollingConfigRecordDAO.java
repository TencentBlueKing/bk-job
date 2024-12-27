package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.execute.model.tables.RollingConfig;
import com.tencent.bk.job.execute.model.tables.records.RollingConfigRecord;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;

/**
 * rolling_config DAO
 */
public class RollingConfigRecordDAO extends AbstractExecuteRecordDAO<RollingConfigRecord> {

    private static final RollingConfig TABLE = RollingConfig.ROLLING_CONFIG;

    public RollingConfigRecordDAO(DSLContext context) {
        super(context);
    }

    @Override
    public Table<RollingConfigRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<RollingConfigRecord, Long> getArchiveIdField() {
        return TABLE.TASK_INSTANCE_ID;
    }
}
