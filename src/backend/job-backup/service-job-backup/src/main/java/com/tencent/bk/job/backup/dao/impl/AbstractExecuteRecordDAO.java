package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveConfig;
import com.tencent.bk.job.backup.dao.ExecuteRecordDAO;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.Table;

import java.util.ArrayList;
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
        Result<Record> result = query(getTable(), buildConditions(start, end));
        return result.into(getTable());
    }

    @Override
    public int deleteRecords(Long start, Long end) {
        return deleteWithLimit(getTable(), buildConditions(start, end));
    }

    private List<Condition> buildConditions(Long start, Long end) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(getArchiveIdField().greaterThan(start));
        conditions.add(getArchiveIdField().lessOrEqual(end));
        return conditions;
    }

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

    private Result<Record> query(Table<?> table, List<Condition> conditions) {
        return context.select()
            .from(table)
            .where(conditions)
            .fetch();
    }

    @Override
    public Long getFirstArchiveId() {
        Record1<Long> firstArchiveIdRecord = context.select(min(getArchiveIdField())).from(getTable()).fetchOne();
        if (firstArchiveIdRecord != null && firstArchiveIdRecord.get(0) != null) {
            return (Long) firstArchiveIdRecord.get(0);
        }
        return 0L;
    }

    public abstract Table<T> getTable();
}
