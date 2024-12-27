package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.dao.ExecuteRecordDAO;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.OrderField;
import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.Table;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jooq.impl.DSL.min;

public abstract class AbstractExecuteRecordDAO<T extends Record> implements ExecuteRecordDAO<T> {

    protected final DSLContext context;

    public AbstractExecuteRecordDAO(DSLContext context) {
        this.context = context;
    }

    @Override
    public List<T> listRecords(Long start, Long end, Long offset, Long limit) {
        Result<Record> result = query(getTable(), buildConditions(start, end), offset, limit);
        return result.into(getTable());
    }

    @Override
    public int deleteRecords(Long start, Long end, long maxLimitedDeleteRows) {
        return deleteWithLimit(getTable(), buildConditions(start, end), maxLimitedDeleteRows);
    }

    private List<Condition> buildConditions(Long start, Long end) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(getArchiveIdField().greaterThan(start));
        conditions.add(getArchiveIdField().lessOrEqual(end));
        return conditions;
    }

    private int deleteWithLimit(Table<? extends Record> table, List<Condition> conditions, long maxLimitedDeleteRows) {
        int totalDeleteRows = 0;
        while (true) {
            int deletedRows = context.delete(table).where(conditions).limit(maxLimitedDeleteRows).execute();
            totalDeleteRows += deletedRows;
            if (deletedRows < maxLimitedDeleteRows) {
                break;
            }
        }
        return totalDeleteRows;
    }

    private Result<Record> query(Table<?> table,
                                 List<Condition> conditions,
                                 Long offset,
                                 Long limit) {
        SelectConditionStep<Record> selectConditionStep = context.select()
            .from(table)
            .where(conditions);

        if (CollectionUtils.isNotEmpty(getListRecordsOrderFields())) {
            return selectConditionStep.orderBy(getListRecordsOrderFields()).limit(offset, limit).fetch();
        } else {
            return selectConditionStep.limit(offset, limit).fetch();
        }
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

    protected Collection<? extends OrderField<?>> getListRecordsOrderFields() {
        return null;
    }
}
