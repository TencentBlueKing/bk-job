/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package com.tencent.bk.job.crontab.dao.impl;

import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.crontab.constant.ExecuteStatusEnum;
import com.tencent.bk.job.crontab.dao.CronJobHistoryDAO;
import com.tencent.bk.job.crontab.model.dto.CronJobHistoryDTO;
import com.tencent.bk.job.crontab.util.DbRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.jooq.generated.tables.CronJobHistory;
import org.jooq.generated.tables.records.CronJobHistoryRecord;
import org.jooq.types.UByte;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * @since 16/2/2020 20:55
 */
@Slf4j
@Repository
public class CronJobHistoryDAOImpl implements CronJobHistoryDAO {
    private static final CronJobHistory TABLE = CronJobHistory.CRON_JOB_HISTORY;
    private DSLContext context;

    public CronJobHistoryDAOImpl(@Qualifier("job-crontab-dsl-context") DSLContext context) {
        this.context = context;
    }

    @Override
    public CronJobHistoryDTO getCronJobHistory(long appId, long cronJobId, long scheduledFireTime) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.eq(ULong.valueOf(appId)));
        conditions.add(TABLE.CRON_JOB_ID.eq(ULong.valueOf(cronJobId)));
        conditions.add(TABLE.SCHEDULED_TIME.eq(ULong.valueOf(scheduledFireTime)));
        Record10<ULong, ULong, ULong, UByte, ULong, ULong, ULong, String, ULong, String> record =
            context.select(TABLE.ID, TABLE.APP_ID, TABLE.CRON_JOB_ID, TABLE.STATUS, TABLE.SCHEDULED_TIME,
                TABLE.START_TIME, TABLE.FINISH_TIME, TABLE.EXECUTOR, TABLE.ERROR_CODE, TABLE.ERROR_MESSAGE).from(TABLE).where(conditions).fetchOne();
        return DbRecordMapper.convertRecordToCronJobHistory(record);
    }

    @Override
    public long insertCronJobHistory(long appId, long cronJobId, long scheduledFireTime) {
        CronJobHistoryRecord cronJobHistoryRecord = context.insertInto(TABLE)
            .columns(TABLE.APP_ID, TABLE.CRON_JOB_ID, TABLE.STATUS, TABLE.SCHEDULED_TIME, TABLE.START_TIME)
            .values(ULong.valueOf(appId), ULong.valueOf(cronJobId), UByte.valueOf(ExecuteStatusEnum.DEFAULT.getValue()),
                ULong.valueOf(scheduledFireTime), ULong.valueOf(System.currentTimeMillis()))
            .returning(TABLE.ID).fetchOne();
        if (cronJobHistoryRecord != null) {
            return cronJobHistoryRecord.get(TABLE.ID).longValue();
        } else {
            return 0;
        }
    }

    @Override
    public boolean updateStatusByIdAndTime(long appId, long cronJobId, long scheduledFireTime, int status) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.eq(ULong.valueOf(appId)));
        conditions.add(TABLE.CRON_JOB_ID.eq(ULong.valueOf(cronJobId)));
        conditions.add(TABLE.SCHEDULED_TIME.eq(ULong.valueOf(scheduledFireTime)));
        return 1 == context.update(TABLE).set(TABLE.STATUS, UByte.valueOf(status)).where(conditions).limit(1).execute();
    }

    @Override
    public boolean fillErrorInfo(long historyId, long errorCode, String errorMsg) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.eq(ULong.valueOf(historyId)));
        return 1 == context.update(TABLE).set(TABLE.ERROR_CODE, ULong.valueOf(errorCode)).set(TABLE.ERROR_MESSAGE,
            errorMsg).where(conditions).limit(1).execute();
    }

    @Override
    public boolean fillExecutor(long historyId, String executor) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.eq(ULong.valueOf(historyId)));
        return 1 == context.update(TABLE).set(TABLE.EXECUTOR, executor).where(conditions).limit(1).execute();
    }

    @Override
    public PageData<CronJobHistoryDTO> listPageHistoryByCondition(CronJobHistoryDTO historyCondition,
                                                                  BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = buildConditionList(historyCondition, baseSearchCondition);
        long historyCount = getPageCronJobHistoryCount(conditions);
        int start = baseSearchCondition.getStartOrDefault(0);
        int length = baseSearchCondition.getLengthOrDefault(10);
        Result<Record10<ULong, ULong, ULong, UByte, ULong, ULong, ULong, String, ULong, String>> result =
            context.select(TABLE.ID, TABLE.APP_ID, TABLE.CRON_JOB_ID, TABLE.STATUS, TABLE.SCHEDULED_TIME,
            TABLE.START_TIME, TABLE.FINISH_TIME, TABLE.EXECUTOR, TABLE.ERROR_CODE, TABLE.ERROR_MESSAGE)
            .from(TABLE).where(conditions).orderBy(TABLE.START_TIME.desc()).limit(start, length).fetch();

        List<CronJobHistoryDTO> cronJobHistoryList = new ArrayList<>();

        if (result != null && result.size() > 0) {
            result.map(record -> cronJobHistoryList.add(DbRecordMapper.convertRecordToCronJobHistory(record)));
        }
        PageData<CronJobHistoryDTO> historyPageData = new PageData<>();
        historyPageData.setTotal(historyCount);
        historyPageData.setStart(start);
        historyPageData.setPageSize(length);
        historyPageData.setData(cronJobHistoryList);
        return historyPageData;
    }

    @Override
    public List<CronJobHistoryDTO> listLatestCronJobHistory(Long appId, Long cronJobId, Long latestTimeInSeconds,
                                                            ExecuteStatusEnum status, Integer limit) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(appId)));
        conditions.add(TABLE.CRON_JOB_ID.equal(ULong.valueOf(cronJobId)));
        if (latestTimeInSeconds != null && latestTimeInSeconds > 0) {
            long fromTimeInMillSecond = Instant.now().minusMillis(1000 * latestTimeInSeconds).toEpochMilli();
            conditions.add(TABLE.SCHEDULED_TIME.greaterOrEqual(ULong.valueOf(fromTimeInMillSecond)));
        }
        if (status != null) {
            conditions.add(TABLE.STATUS.equal(UByte.valueOf(status.getValue())));
        }
        SelectSeekStep1<Record10<ULong, ULong, ULong, UByte, ULong, ULong, ULong, String, ULong, String>, ULong> selectStep = context.select(TABLE.ID, TABLE.APP_ID, TABLE.CRON_JOB_ID, TABLE.STATUS, TABLE.SCHEDULED_TIME,
            TABLE.START_TIME, TABLE.FINISH_TIME, TABLE.EXECUTOR, TABLE.ERROR_CODE, TABLE.ERROR_MESSAGE).from(TABLE).where(conditions).orderBy(TABLE.SCHEDULED_TIME.desc());
        Result<Record10<ULong, ULong, ULong, UByte, ULong, ULong, ULong, String, ULong, String>> result;
        if (limit != null && limit > 0) {
            result = selectStep.limit(0, limit.intValue()).fetch();
        } else {
            result = selectStep.fetch();
        }
        List<CronJobHistoryDTO> cronJobHistoryList = new ArrayList<>();
        if (result != null && result.size() > 0) {
            result.map(record -> cronJobHistoryList.add(DbRecordMapper.convertRecordToCronJobHistory(record)));
        }
        return cronJobHistoryList;
    }

    @Override
    public int cleanHistory(long cleanBefore, boolean cleanAll) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.SCHEDULED_TIME.lessOrEqual(ULong.valueOf(cleanBefore)));
        if (!cleanAll) {
            conditions.add(TABLE.STATUS.in(UByte.valueOf(ExecuteStatusEnum.RUNNING.getValue()),
                UByte.valueOf(ExecuteStatusEnum.SUCCESS.getValue())));
        }
        return context.deleteFrom(TABLE).where(conditions).execute();
    }

    private List<Condition> buildConditionList(CronJobHistoryDTO historyCondition,
                                               BaseSearchCondition baseSearchCondition) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(historyCondition.getAppId())));
        conditions.add(TABLE.CRON_JOB_ID.equal(ULong.valueOf(historyCondition.getCronJobId())));
        if (historyCondition.getStatus() != null) {
            conditions.add(TABLE.STATUS.equal(UByte.valueOf(historyCondition.getStatus().getValue())));
        }
        return conditions;
    }

    private long getPageCronJobHistoryCount(List<Condition> conditions) {
        return context.selectCount().from(TABLE).where(conditions).fetchOne(0, Long.class);
    }
}
