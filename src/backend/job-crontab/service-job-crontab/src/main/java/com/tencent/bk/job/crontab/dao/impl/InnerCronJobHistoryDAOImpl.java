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

import com.tencent.bk.job.crontab.constant.ExecuteStatusEnum;
import com.tencent.bk.job.crontab.dao.InnerCronJobHistoryDAO;
import com.tencent.bk.job.crontab.model.dto.InnerCronJobHistoryDTO;
import com.tencent.bk.job.crontab.util.DbRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record7;
import org.jooq.generated.tables.InnerCronJobHistory;
import org.jooq.generated.tables.records.InnerCronJobHistoryRecord;
import org.jooq.types.UByte;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 21/2/2020 22:30
 */
@Slf4j
@Repository
public class InnerCronJobHistoryDAOImpl implements InnerCronJobHistoryDAO {

    private static final InnerCronJobHistory TABLE = InnerCronJobHistory.INNER_CRON_JOB_HISTORY;
    private DSLContext context;

    @Autowired
    public InnerCronJobHistoryDAOImpl(@Qualifier("job-crontab-dsl-context") DSLContext context) {
        this.context = context;
    }

    @Override
    public InnerCronJobHistoryDTO getCronJobHistory(String systemId, String jobKey, long scheduledFireTime) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.SYSTEM_ID.eq(systemId));
        conditions.add(TABLE.JOB_KEY.eq(jobKey));
        conditions.add(TABLE.SCHEDULED_TIME.eq(ULong.valueOf(scheduledFireTime)));
        Record7<ULong, String, String, UByte, ULong, ULong, ULong> record =
            context.select(TABLE.ID, TABLE.SYSTEM_ID, TABLE.JOB_KEY, TABLE.STATUS, TABLE.SCHEDULED_TIME,
                TABLE.START_TIME, TABLE.FINISH_TIME).from(TABLE).where(conditions).fetchOne();
        return DbRecordMapper.convertRecordToInnerCronJobHistory(record);
    }

    @Override
    public long insertCronJobHistory(String systemId, String jobKey, long scheduledFireTime) {
        InnerCronJobHistoryRecord innerCronJobHistoryRecord = context.insertInto(TABLE)
            .columns(TABLE.SYSTEM_ID, TABLE.JOB_KEY, TABLE.STATUS, TABLE.SCHEDULED_TIME, TABLE.START_TIME)
            .values(systemId, jobKey, UByte.valueOf(ExecuteStatusEnum.DEFAULT.getValue()),
                ULong.valueOf(scheduledFireTime), ULong.valueOf(System.currentTimeMillis()))
            .returning(TABLE.ID).fetchOne();
        if (innerCronJobHistoryRecord != null) {
            return innerCronJobHistoryRecord.get(TABLE.ID).longValue();
        } else {
            return 0;
        }
    }

    @Override
    public boolean updateStatusByIdAndTime(String systemId, String jobKey, long scheduledFireTime, int status) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.SYSTEM_ID.eq(systemId));
        conditions.add(TABLE.JOB_KEY.eq(jobKey));
        conditions.add(TABLE.SCHEDULED_TIME.eq(ULong.valueOf(scheduledFireTime)));
        if (ExecuteStatusEnum.SUCCESS.getValue() == status) {
            return 1 == context.update(TABLE).set(TABLE.STATUS, UByte.valueOf(status))
                .set(TABLE.FINISH_TIME, ULong.valueOf(System.currentTimeMillis())).where(conditions).limit(1).execute();
        } else {
            return 1 == context.update(TABLE).set(TABLE.STATUS, UByte.valueOf(status)).where(conditions).limit(1)
                .execute();
        }
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
}
