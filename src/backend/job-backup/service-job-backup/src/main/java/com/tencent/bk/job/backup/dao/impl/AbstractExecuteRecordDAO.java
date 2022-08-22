package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import com.tencent.bk.job.backup.dao.ExecuteRecordDAO;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.Table;

import java.util.List;

import static org.jooq.impl.DSL.min;

public abstract class AbstractExecuteRecordDAO<T extends Record> implements ExecuteRecordDAO<T> {

    protected final DSLContext context;
    protected final ArchiveConfig archiveConfig;

    public AbstractExecuteRecordDAO(DSLContext context, ArchiveConfig archiveConfig) {
        this.context = context;
        this.archiveConfig = archiveConfig;
    }

    @Override
    public List<T> listRecords(Long start, Long end) {
        Result<Record> result = query(getTable(), listFields(), buildConditions(start, end));
        return result.into(getTable());
    }

    @Override
    public int deleteRecords(Long start, Long end) {
        return deleteWithLimit(getTable(), buildConditions(start, end));
    }

    public abstract Table<T> getTable();

    protected abstract List<Condition> buildConditions(Long start, Long end);

    private int deleteWithLimit(Table<? extends Record> table, List<Condition> conditions) {
        int totalDeleteRows = 0;
        int maxLimitedDeleteRows = archiveConfig.getDeleteLimitRowCount();
        while (true) {
            int deletedRows = context.delete(table).where(conditions).limit(maxLimitedDeleteRows).execute();
            totalDeleteRows += deletedRows;
            if (deletedRows < maxLimitedDeleteRows) {
                break;
            }
        }
        return totalDeleteRows;
    }

    private Result<Record> query(Table<?> table, List<Field<?>> fields, List<Condition> conditions) {
        return context.select(fields)
            .from(table)
            .where(conditions)
            .fetch();
    }

    @Override
    public Long getFirstInstanceId() {
        Record1<Long> firstInstanceIdRecord = context.select(min(getIdField())).from(getTable()).fetchOne();
        if (firstInstanceIdRecord != null && firstInstanceIdRecord.get(0) != null) {
            return (Long) firstInstanceIdRecord.get(0);
        }
        return 0L;
    }
}
