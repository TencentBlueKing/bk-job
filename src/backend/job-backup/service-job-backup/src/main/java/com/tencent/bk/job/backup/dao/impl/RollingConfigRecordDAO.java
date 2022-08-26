package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.generated.tables.RollingConfig;
import org.jooq.generated.tables.records.RollingConfigRecord;

import java.util.Arrays;
import java.util.List;

/**
 * rolling_config DAO
 */
public class RollingConfigRecordDAO extends AbstractExecuteRecordDAO<RollingConfigRecord> {

    private static final RollingConfig TABLE = RollingConfig.ROLLING_CONFIG;

    private static final List<Field<?>> FIELDS =
        Arrays.asList(
            TABLE.ID,
            TABLE.TASK_INSTANCE_ID,
            TABLE.CONFIG_NAME,
            TABLE.CONFIG,
            TABLE.ROW_CREATE_TIME,
            TABLE.ROW_UPDATE_TIME
        );

    public RollingConfigRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
        super(context, archiveConfig);
    }

    @Override
    public List<Field<?>> listFields() {
        return FIELDS;
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
