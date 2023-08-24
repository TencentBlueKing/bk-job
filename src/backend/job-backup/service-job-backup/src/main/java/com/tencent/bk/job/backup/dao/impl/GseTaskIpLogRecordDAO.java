package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveDBProperties;
import com.tencent.bk.job.execute.model.tables.GseTaskIpLog;
import com.tencent.bk.job.execute.model.tables.records.GseTaskIpLogRecord;
import org.jooq.DSLContext;
import org.jooq.Table;
import org.jooq.TableField;

/**
 * gse_task_ip_log DAO
 */
public class GseTaskIpLogRecordDAO extends AbstractExecuteRecordDAO<GseTaskIpLogRecord> {

    private static final GseTaskIpLog TABLE = GseTaskIpLog.GSE_TASK_IP_LOG;

    public GseTaskIpLogRecordDAO(DSLContext context, ArchiveDBProperties archiveDBProperties) {
        super(context, archiveDBProperties);
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
