package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.config.ArchiveDBProperties;
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
    protected final ArchiveDBProperties archiveDBProperties;

    public AbstractExecuteRecordDAO(DSLContext context, ArchiveDBProperties archiveDBProperties) {
        this.context = context;
        this.archiveDBProperties = archiveDBProperties;
    }

    @Override
    public List<T> listRecords(Long start, Long end, Long offset, Long limit) {
        Result<Record> result = query(getTable(), buildConditions(start, end), offset, limit);
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
        int maxLimitedDeleteRows = 5000;
        while (true) {
            int deletedRows = context.delete(table).where(conditions).limit(maxLimitedDeleteRows).execute();
            totalDeleteRows += deletedRows;
            if (deletedRows < maxLimitedDeleteRows) {
                break;
            }
        }
        return totalDeleteRows;
    }

    private Result<Record> query(Table<?> table, List<Condition> conditions, Long offset, Long limit) {
        return context.select()
            .from(table)
            .where(conditions)
            .limit(offset, limit)
            .fetch();
    }

    @Override
    public Long getMinArchiveId() {
        Record1<Long> firstArchiveIdRecord = context.select(min(getArchiveIdField())).from(getTable()).fetchOne();
        if (firstArchiveIdRecord != null && firstArchiveIdRecord.get(0) != null) {
            return (Long) firstArchiveIdRecord.get(0);
        }
        return null;
    }

    public abstract Table<T> getTable();
}
