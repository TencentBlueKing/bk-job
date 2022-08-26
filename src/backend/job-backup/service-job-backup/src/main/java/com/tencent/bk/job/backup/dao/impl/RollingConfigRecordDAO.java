package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.generated.tables.RollingConfig;
import org.jooq.generated.tables.records.RollingConfigRecord;

/**
 * rolling_config DAO
 */
public class RollingConfigRecordDAO extends AbstractExecuteRecordDAO<RollingConfigRecord> {

    private static final RollingConfig TABLE = RollingConfig.ROLLING_CONFIG;

    public RollingConfigRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
        super(context, archiveConfig);
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
