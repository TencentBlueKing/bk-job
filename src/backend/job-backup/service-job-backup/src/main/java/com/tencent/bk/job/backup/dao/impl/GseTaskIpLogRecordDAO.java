package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.generated.tables.GseTaskIpLog;
import org.jooq.generated.tables.records.GseTaskIpLogRecord;

/**
 * gse_task_ip_log DAO
 */
public class GseTaskIpLogRecordDAO extends AbstractExecuteRecordDAO<GseTaskIpLogRecord> {

    private static final GseTaskIpLog TABLE = GseTaskIpLog.GSE_TASK_IP_LOG;

    public GseTaskIpLogRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
        super(context, archiveConfig);
    }

    @Override
    public Table<GseTaskIpLogRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<GseTaskIpLogRecord, Long> getArchiveIdField() {
        return TABLE.STEP_INSTANCE_ID;
    }
}
