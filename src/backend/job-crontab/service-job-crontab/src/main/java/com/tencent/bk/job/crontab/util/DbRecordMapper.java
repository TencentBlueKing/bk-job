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

package com.tencent.bk.job.crontab.util;

import com.tencent.bk.job.crontab.constant.ExecuteStatusEnum;
import com.tencent.bk.job.crontab.model.dto.CronJobHistoryDTO;
import com.tencent.bk.job.crontab.model.dto.InnerCronJobHistoryDTO;
import org.jooq.Record10;
import org.jooq.Record7;
import org.jooq.generated.tables.CronJobHistory;
import org.jooq.generated.tables.InnerCronJobHistory;
import org.jooq.types.UByte;
import org.jooq.types.ULong;

/**
 * @since 16/2/2020 22:17
 */
public class DbRecordMapper {

    public static ULong getJooqLongValue(Long longValue) {
        if (longValue == null) {
            return null;
        } else {
            return ULong.valueOf(longValue);
        }
    }

    public static Long getLongValue(ULong uLongValue) {
        if (uLongValue == null) {
            return null;
        } else {
            return uLongValue.longValue();
        }
    }

    public static CronJobHistoryDTO convertRecordToCronJobHistory(Record10<ULong, ULong, ULong, UByte, ULong, ULong,
        ULong, String, ULong, String> record) {
        if (record == null) {
            return null;
        }
        CronJobHistory table = CronJobHistory.CRON_JOB_HISTORY;
        CronJobHistoryDTO cronJobHistory = new CronJobHistoryDTO();
        cronJobHistory.setId(getLongValue(record.get(table.ID)));
        cronJobHistory.setAppId(getLongValue(record.get(table.APP_ID)));
        cronJobHistory.setCronJobId(getLongValue(record.get(table.CRON_JOB_ID)));
        cronJobHistory.setStatus(ExecuteStatusEnum.valueOf(record.get(table.STATUS).intValue()));
        cronJobHistory.setScheduledTime(getLongValue(record.get(table.SCHEDULED_TIME)));
        cronJobHistory.setStartTime(getLongValue(record.get(table.START_TIME)));
        cronJobHistory.setFinishTime(getLongValue(record.get(table.FINISH_TIME)));
        cronJobHistory.setExecutor(record.get(table.EXECUTOR));
        cronJobHistory.setErrorCode(getLongValue(record.get(table.ERROR_CODE)));
        cronJobHistory.setErrorMsg(record.get(table.ERROR_MESSAGE));
        return cronJobHistory;
    }

    public static InnerCronJobHistoryDTO
    convertRecordToInnerCronJobHistory(Record7<ULong, String, String, UByte, ULong, ULong, ULong> record) {
        if (record == null) {
            return null;
        }
        InnerCronJobHistory table = InnerCronJobHistory.INNER_CRON_JOB_HISTORY;
        InnerCronJobHistoryDTO cronJobHistory = new InnerCronJobHistoryDTO();
        cronJobHistory.setId(getLongValue(record.get(table.ID)));
        cronJobHistory.setSystemId(record.get(table.SYSTEM_ID));
        cronJobHistory.setJobKey(record.get(table.JOB_KEY));
        cronJobHistory.setStatus(ExecuteStatusEnum.valueOf(record.get(table.STATUS).intValue()));
        cronJobHistory.setScheduledTime(getLongValue(record.get(table.SCHEDULED_TIME)));
        cronJobHistory.setStartTime(getLongValue(record.get(table.START_TIME)));
        cronJobHistory.setFinishTime(getLongValue(record.get(table.FINISH_TIME)));
        return cronJobHistory;

    }
}
