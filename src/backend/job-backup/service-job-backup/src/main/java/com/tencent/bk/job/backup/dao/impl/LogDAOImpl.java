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

package com.tencent.bk.job.backup.dao.impl;

import com.tencent.bk.job.backup.constant.LogTypeEnum;
import com.tencent.bk.job.backup.dao.LogDAO;
import com.tencent.bk.job.backup.model.dto.LogEntityDTO;
import com.tencent.bk.job.backup.model.tables.ExportLog;
import com.tencent.bk.job.backup.model.tables.ImportLog;
import com.tencent.bk.job.backup.util.DbRecordMapper;
import com.tencent.bk.job.backup.util.DbUtils;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record9;
import org.jooq.Result;
import org.jooq.types.UByte;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 29/7/2020 12:18
 */
@Slf4j
@Repository
public class LogDAOImpl implements LogDAO {
    private static final ExportLog EXPORT_TABLE = ExportLog.EXPORT_LOG;
    private static final ImportLog IMPORT_TABLE = ImportLog.IMPORT_LOG;
    private final DSLContext context;

    @Autowired
    public LogDAOImpl(@Qualifier("job-backup-dsl-context") DSLContext context) {
        this.context = context;
    }

    @Override
    public void insertLogEntity(LogEntityDTO logEntity, LogTypeEnum type) {
        switch (type) {
            case EXPORT:
                context.insertInto(EXPORT_TABLE)
                    .columns(EXPORT_TABLE.APP_ID, EXPORT_TABLE.JOB_ID, EXPORT_TABLE.TYPE, EXPORT_TABLE.TIMESTAMP,
                        EXPORT_TABLE.CONTENT, EXPORT_TABLE.TEMPLATE_ID, EXPORT_TABLE.PLAN_ID, EXPORT_TABLE.LINK_TEXT,
                        EXPORT_TABLE.LINK_URL)
                    .values(ULong.valueOf(logEntity.getAppId()), logEntity.getJobId(),
                        UByte.valueOf(logEntity.getType().getType()), ULong.valueOf(logEntity.getTimestamp()),
                        logEntity.getContent(), DbUtils.getJooqLongValue(logEntity.getTemplateId()),
                        DbUtils.getJooqLongValue(logEntity.getPlanId()), logEntity.getLinkText(),
                        logEntity.getLinkUrl())
                    .execute();
                break;
            case IMPORT:
                context.insertInto(IMPORT_TABLE)
                    .columns(IMPORT_TABLE.APP_ID, IMPORT_TABLE.JOB_ID, IMPORT_TABLE.TYPE, IMPORT_TABLE.TIMESTAMP,
                        IMPORT_TABLE.CONTENT, IMPORT_TABLE.TEMPLATE_ID, IMPORT_TABLE.PLAN_ID, IMPORT_TABLE.LINK_TEXT,
                        IMPORT_TABLE.LINK_URL)
                    .values(ULong.valueOf(logEntity.getAppId()), logEntity.getJobId(),
                        UByte.valueOf(logEntity.getType().getType()), ULong.valueOf(logEntity.getTimestamp()),
                        logEntity.getContent(), DbUtils.getJooqLongValue(logEntity.getTemplateId()),
                        DbUtils.getJooqLongValue(logEntity.getPlanId()), logEntity.getLinkText(),
                        logEntity.getLinkUrl())
                    .execute();
                break;
            default:
                return;
        }
    }

    @Override
    public List<LogEntityDTO> listLogById(Long appId, String jobId, LogTypeEnum type) {
        List<LogEntityDTO> logList = new ArrayList<>();

        List<Condition> conditions = new ArrayList<>();
        Result<Record9<ULong, String, UByte, ULong, String, ULong, ULong, String, String>> result = null;

        switch (type) {
            case EXPORT:
                conditions.add(EXPORT_TABLE.APP_ID.equal(ULong.valueOf(appId)));
                conditions.add(EXPORT_TABLE.JOB_ID.equal(jobId));
                result = context.select(EXPORT_TABLE.APP_ID, EXPORT_TABLE.JOB_ID, EXPORT_TABLE.TYPE,
                    EXPORT_TABLE.TIMESTAMP, EXPORT_TABLE.CONTENT, EXPORT_TABLE.TEMPLATE_ID, EXPORT_TABLE.PLAN_ID,
                    EXPORT_TABLE.LINK_TEXT, EXPORT_TABLE.LINK_URL).from(EXPORT_TABLE).where(conditions).fetch();
                break;
            case IMPORT:
                conditions.add(IMPORT_TABLE.APP_ID.equal(ULong.valueOf(appId)));
                conditions.add(IMPORT_TABLE.JOB_ID.equal(jobId));
                result = context.select(IMPORT_TABLE.APP_ID, IMPORT_TABLE.JOB_ID, IMPORT_TABLE.TYPE,
                    IMPORT_TABLE.TIMESTAMP, IMPORT_TABLE.CONTENT, IMPORT_TABLE.TEMPLATE_ID, IMPORT_TABLE.PLAN_ID,
                    IMPORT_TABLE.LINK_TEXT, IMPORT_TABLE.LINK_URL).from(IMPORT_TABLE).where(conditions).fetch();
                break;
            default:
                return logList;
        }

        if (result != null) {
            result.forEach(record -> logList.add(DbRecordMapper.convertRecordToLogEntity(record)));
        }
        return logList;
    }
}
