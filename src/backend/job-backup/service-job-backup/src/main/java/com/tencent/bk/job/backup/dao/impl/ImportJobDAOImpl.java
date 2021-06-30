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

import com.tencent.bk.job.backup.constant.BackupJobStatusEnum;
import com.tencent.bk.job.backup.dao.ImportJobDAO;
import com.tencent.bk.job.backup.model.dto.ImportJobInfoDTO;
import com.tencent.bk.job.backup.model.tables.ImportJob;
import com.tencent.bk.job.backup.model.tables.records.ImportJobRecord;
import com.tencent.bk.job.backup.util.DbRecordMapper;
import com.tencent.bk.job.common.util.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.*;
import org.jooq.types.UByte;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 28/7/2020 12:26
 */
@Slf4j
@Repository
public class ImportJobDAOImpl implements ImportJobDAO {
    private static final ImportJob TABLE = ImportJob.IMPORT_JOB;

    private final DSLContext context;

    @Autowired
    public ImportJobDAOImpl(@Qualifier("job-backup-dsl-context") DSLContext context) {
        this.context = context;
    }

    @Override
    public String insertImportJob(ImportJobInfoDTO importJobInfo) {
        if (1 == context.insertInto(TABLE)
            .columns(TABLE.ID, TABLE.APP_ID, TABLE.CREATOR, TABLE.CREATE_TIME, TABLE.UPDATE_TIME, TABLE.STATUS,
                TABLE.EXPORT_ID, TABLE.FILE_NAME, TABLE.TEMPLATE_PLAN_INFO, TABLE.DUPLICATE_SUFFIX,
                TABLE.DUPLICATE_ID_HANDLER, TABLE.ID_NAME_INFO, TABLE.LOCALE)
            .values(importJobInfo.getId(), ULong.valueOf(importJobInfo.getAppId()), importJobInfo.getCreator(),
                ULong.valueOf(importJobInfo.getCreateTime()), ULong.valueOf(importJobInfo.getUpdateTime()),
                UByte.valueOf(importJobInfo.getStatus().getStatus()), importJobInfo.getExportId(),
                importJobInfo.getFileName(), JsonMapper.nonEmptyMapper().toJson(importJobInfo.getTemplateInfo()),
                importJobInfo.getDuplicateSuffix(), UByte.valueOf(importJobInfo.getDuplicateIdHandler().getType()),
                JsonMapper.nonEmptyMapper().toJson(importJobInfo.getIdNameInfo()),
                LocaleContextHolder.getLocale().toLanguageTag())
            .execute()) {
            return importJobInfo.getId();
        } else {
            return null;
        }
    }

    @Override
    public ImportJobInfoDTO getImportJobById(Long appId, String jobId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.equal(jobId));
        if (appId > 0) {
            conditions.add(TABLE.APP_ID.equal(ULong.valueOf(appId)));
        }

        Record13<String, ULong, String, ULong, ULong, UByte, String, String, String, String, UByte, String, String> record =
            context.select(TABLE.ID, TABLE.APP_ID, TABLE.CREATOR, TABLE.CREATE_TIME, TABLE.UPDATE_TIME,
                TABLE.STATUS, TABLE.EXPORT_ID, TABLE.FILE_NAME, TABLE.TEMPLATE_PLAN_INFO, TABLE.DUPLICATE_SUFFIX,
                TABLE.DUPLICATE_ID_HANDLER, TABLE.ID_NAME_INFO, TABLE.LOCALE).from(TABLE).where(conditions).fetchOne();
        return DbRecordMapper.convertRecordToImportJobInfo(record);
    }

    @Override
    public List<ImportJobInfoDTO> getImportJobByUser(Long appId, String username) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.CREATOR.equal(username));
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(appId)));
        conditions.add(TABLE.STATUS.in(UByte.valueOf(BackupJobStatusEnum.SUBMIT.getStatus()),
            UByte.valueOf(BackupJobStatusEnum.PROCESSING.getStatus())));

        Result<
            Record13<String, ULong, String, ULong, ULong, UByte, String, String, String, String, UByte,
                String, String>> result =
            context
                .select(TABLE.ID, TABLE.APP_ID, TABLE.CREATOR, TABLE.CREATE_TIME, TABLE.UPDATE_TIME,
                    TABLE.STATUS, TABLE.EXPORT_ID, TABLE.FILE_NAME, TABLE.TEMPLATE_PLAN_INFO,
                    TABLE.DUPLICATE_SUFFIX, TABLE.DUPLICATE_ID_HANDLER, TABLE.ID_NAME_INFO, TABLE.LOCALE)
                .from(TABLE).where(conditions).fetch();
        List<ImportJobInfoDTO> importJobInfoList = new ArrayList<>();
        if (result != null) {
            result.forEach(record -> importJobInfoList.add(DbRecordMapper.convertRecordToImportJobInfo(record)));
        }
        return importJobInfoList;
    }

    @Override
    public boolean updateImportJobById(ImportJobInfoDTO importJobInfo) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.equal(importJobInfo.getId()));
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(importJobInfo.getAppId())));
        conditions.add(TABLE.CREATOR.equal(importJobInfo.getCreator()));
        conditions.add(TABLE.STATUS.notIn(UByte.valueOf(BackupJobStatusEnum.FAILED.getStatus()),
            UByte.valueOf(BackupJobStatusEnum.CANCEL.getStatus())));

        UpdateSetMoreStep<ImportJobRecord> updateStep =
            context.update(TABLE).set(TABLE.UPDATE_TIME, ULong.valueOf(System.currentTimeMillis()));

        if (importJobInfo.getStatus() != null) {
            updateStep = updateStep.set(TABLE.STATUS, UByte.valueOf(importJobInfo.getStatus().getStatus()));
        }
        if (importJobInfo.getExportId() != null) {
            updateStep = updateStep.set(TABLE.EXPORT_ID, importJobInfo.getExportId());
        }
        if (importJobInfo.getFileName() != null) {
            updateStep = updateStep.set(TABLE.FILE_NAME, importJobInfo.getFileName());
        }
        if (CollectionUtils.isNotEmpty(importJobInfo.getTemplateInfo())) {
            updateStep = updateStep.set(TABLE.TEMPLATE_PLAN_INFO,
                JsonMapper.nonEmptyMapper().toJson(importJobInfo.getTemplateInfo()));
        }
        if (importJobInfo.getIdNameInfo() != null) {
            updateStep =
                updateStep.set(TABLE.ID_NAME_INFO, JsonMapper.nonEmptyMapper().toJson(importJobInfo.getIdNameInfo()));
        }
        if (importJobInfo.getDuplicateSuffix() != null) {
            updateStep = updateStep.set(TABLE.DUPLICATE_SUFFIX, importJobInfo.getDuplicateSuffix());
        }
        if (importJobInfo.getDuplicateIdHandler() != null) {
            updateStep = updateStep.set(TABLE.DUPLICATE_ID_HANDLER,
                UByte.valueOf(importJobInfo.getDuplicateIdHandler().getType()));
        }

        return updateStep.where(conditions).execute() == 1;
    }

    @Override
    public List<ImportJobInfoDTO> listOldImportJob() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.CREATE_TIME.lessOrEqual(ULong.valueOf(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000)));
        conditions.add(TABLE.IS_CLEANED.equal(UByte.valueOf(0)));

        Result<
            Record13<String, ULong, String, ULong, ULong, UByte, String, String, String, String, UByte, String,
                String>> result =
            context.select(TABLE.ID, TABLE.APP_ID, TABLE.CREATOR, TABLE.CREATE_TIME, TABLE.UPDATE_TIME,
                TABLE.STATUS, TABLE.EXPORT_ID, TABLE.FILE_NAME, TABLE.TEMPLATE_PLAN_INFO, TABLE.DUPLICATE_SUFFIX,
                TABLE.DUPLICATE_ID_HANDLER, TABLE.ID_NAME_INFO, TABLE.LOCALE).from(TABLE).where(conditions).fetch();
        List<ImportJobInfoDTO> importJobInfoList = new ArrayList<>();
        if (result != null) {
            result.forEach(record -> importJobInfoList.add(DbRecordMapper.convertRecordToImportJobInfo(record)));
        }
        return importJobInfoList;
    }

    @Override
    public boolean setCleanMark(Long appId, String jobId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(appId)));
        conditions.add(TABLE.ID.equal(jobId));
        return 1 == context.update(TABLE).set(TABLE.IS_CLEANED, UByte.valueOf(1)).where(conditions).execute();
    }
}
