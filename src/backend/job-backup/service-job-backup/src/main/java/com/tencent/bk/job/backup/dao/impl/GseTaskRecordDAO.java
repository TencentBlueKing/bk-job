package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.execute.model.tables.GseTask;
import com.tencent.bk.job.execute.model.tables.records.GseTaskRecord;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.jooq.Table;
import org.jooq.TableField;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GseTaskRecordDAO extends AbstractExecuteRecordDAO<GseTaskRecord> {

    private static final GseTask TABLE = GseTask.GSE_TASK;
    private static final List<OrderField<?>> ORDER_FIELDS = new ArrayList<>();

    static {
        ORDER_FIELDS.add(GseTask.GSE_TASK.STEP_INSTANCE_ID.asc());
        ORDER_FIELDS.add(GseTask.GSE_TASK.EXECUTE_COUNT.asc());
        ORDER_FIELDS.add(GseTask.GSE_TASK.BATCH.asc());
    }

    public GseTaskRecordDAO(DSLContext context) {
        super(context);
    }

    @Override
    public Table<GseTaskRecord> getTable() {
        return TABLE;
    }

    @Override
    public TableField<GseTaskRecord, Long> getArchiveIdField() {
        return TABLE.STEP_INSTANCE_ID;
    }

    @Override
    protected Collection<? extends OrderField<?>> getListRecordsOrderFields() {
        return ORDER_FIELDS;
    }
}
