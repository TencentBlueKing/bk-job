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

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.backup.constant.BackupJobStatusEnum;
import com.tencent.bk.job.backup.constant.SecretHandlerEnum;
import com.tencent.bk.job.backup.crypto.ExportJobPasswordCryptoService;
import com.tencent.bk.job.backup.dao.ExportJobDAO;
import com.tencent.bk.job.backup.model.dto.BackupTemplateInfoDTO;
import com.tencent.bk.job.backup.model.dto.ExportJobInfoDTO;
import com.tencent.bk.job.backup.model.tables.ExportJob;
import com.tencent.bk.job.backup.model.tables.records.ExportJobRecord;
import com.tencent.bk.job.common.util.json.JsonMapper;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record13;
import org.jooq.Result;
import org.jooq.UpdateSetMoreStep;
import org.jooq.types.UByte;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @since 28/7/2020 12:26
 */
@Slf4j
@Repository
public class ExportJobDAOImpl implements ExportJobDAO {
    private static final ExportJob TABLE = ExportJob.EXPORT_JOB;

    private final DSLContext context;
    private final ExportJobPasswordCryptoService exportJobPasswordCryptoService;

    @Autowired
    public ExportJobDAOImpl(@Qualifier("job-backup-dsl-context") DSLContext context,
                            ExportJobPasswordCryptoService exportJobPasswordCryptoService) {
        this.context = context;
        this.exportJobPasswordCryptoService = exportJobPasswordCryptoService;
    }

    @Override
    public String insertExportJob(ExportJobInfoDTO exportJobInfo) {
        int affectedNum = context.insertInto(TABLE)
            .columns(
                TABLE.ID,
                TABLE.APP_ID,
                TABLE.CREATOR,
                TABLE.CREATE_TIME,
                TABLE.UPDATE_TIME,
                TABLE.STATUS,
                TABLE.PASSWORD,
                TABLE.PACKAGE_NAME,
                TABLE.SECRET_HANDLER,
                TABLE.EXPIRE_TIME,
                TABLE.TEMPLATE_PLAN_INFO,
                TABLE.FILE_NAME,
                TABLE.LOCALE
            ).values(
                exportJobInfo.getId(),
                ULong.valueOf(exportJobInfo.getAppId()),
                exportJobInfo.getCreator(),
                ULong.valueOf(exportJobInfo.getCreateTime()),
                ULong.valueOf(exportJobInfo.getUpdateTime()),
                UByte.valueOf(exportJobInfo.getStatus().getStatus()),
                exportJobPasswordCryptoService.encryptExportJobPassword(exportJobInfo.getPassword()),
                exportJobInfo.getPackageName(),
                UByte.valueOf(exportJobInfo.getSecretHandler().getType()),
                ULong.valueOf(exportJobInfo.getExpireTime()),
                JsonMapper.nonEmptyMapper().toJson(exportJobInfo.getTemplateInfo()),
                exportJobInfo.getFileName(),
                LocaleContextHolder.getLocale().toLanguageTag()
            ).execute();
        if (1 == affectedNum) {
            return exportJobInfo.getId();
        } else {
            return null;
        }
    }

    @Override
    public ExportJobInfoDTO getExportJobById(Long appId, String jobId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.equal(jobId));
        if (appId > 0) {
            conditions.add(TABLE.APP_ID.equal(ULong.valueOf(appId)));
        }
        Record13<String, ULong, String, ULong, ULong, UByte, String, String, UByte, ULong, String,
            String, String> record =
            context.select(
                TABLE.ID,
                TABLE.APP_ID,
                TABLE.CREATOR,
                TABLE.CREATE_TIME,
                TABLE.UPDATE_TIME,
                TABLE.STATUS,
                TABLE.PASSWORD,
                TABLE.PACKAGE_NAME,
                TABLE.SECRET_HANDLER,
                TABLE.EXPIRE_TIME,
                TABLE.TEMPLATE_PLAN_INFO,
                TABLE.FILE_NAME,
                TABLE.LOCALE
            ).from(TABLE).where(conditions).fetchOne();
        return convertRecordToExportJobInfo(record);
    }

    @Override
    public List<ExportJobInfoDTO> getExportJobByUser(Long appId, String username) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.CREATOR.equal(username));
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(appId)));
        conditions.add(TABLE.STATUS.in(
            UByte.valueOf(BackupJobStatusEnum.SUBMIT.getStatus()),
            UByte.valueOf(BackupJobStatusEnum.PROCESSING.getStatus()),
            UByte.valueOf(BackupJobStatusEnum.ALL_SUCCESS.getStatus())
        ));

        Result<
            Record13<String, ULong, String, ULong, ULong, UByte, String, String, UByte, ULong, String, String,
                String>> result =
            context.select(
                TABLE.ID,
                TABLE.APP_ID,
                TABLE.CREATOR,
                TABLE.CREATE_TIME,
                TABLE.UPDATE_TIME,
                TABLE.STATUS,
                TABLE.PASSWORD,
                TABLE.PACKAGE_NAME,
                TABLE.SECRET_HANDLER,
                TABLE.EXPIRE_TIME,
                TABLE.TEMPLATE_PLAN_INFO,
                TABLE.FILE_NAME,
                TABLE.LOCALE
            ).from(TABLE).where(conditions).fetch();
        List<ExportJobInfoDTO> exportJobInfoList = new ArrayList<>();
        result.forEach(record -> exportJobInfoList.add(convertRecordToExportJobInfo(record)));
        return exportJobInfoList;
    }

    @Override
    public boolean updateExportJob(ExportJobInfoDTO exportInfo) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.ID.equal(exportInfo.getId()));
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(exportInfo.getAppId())));
        conditions.add(TABLE.CREATOR.equal(exportInfo.getCreator()));

        UpdateSetMoreStep<ExportJobRecord> updateStep =
            context.update(TABLE).set(TABLE.UPDATE_TIME, ULong.valueOf(System.currentTimeMillis()));

        updateStep = updateStep.set(
            TABLE.PASSWORD,
            exportJobPasswordCryptoService.encryptExportJobPassword(exportInfo.getPassword())
        );

        if (exportInfo.getStatus() != null) {
            updateStep = updateStep.set(TABLE.STATUS, UByte.valueOf(exportInfo.getStatus().getStatus()));
        }
        if (exportInfo.getFileName() != null) {
            updateStep = updateStep.set(TABLE.FILE_NAME, exportInfo.getFileName());
        }

        return updateStep.where(conditions).execute() == 1;
    }

    @Override
    public List<ExportJobInfoDTO> listOldExportJob() {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.CREATE_TIME.lessOrEqual(ULong.valueOf(System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000)));
        conditions.add(TABLE.IS_CLEANED.equal(UByte.valueOf(0)));

        Result<
            Record13<String, ULong, String, ULong, ULong, UByte, String, String, UByte, ULong, String, String,
                String>> result =
            context.select(
                TABLE.ID,
                TABLE.APP_ID,
                TABLE.CREATOR,
                TABLE.CREATE_TIME,
                TABLE.UPDATE_TIME,
                TABLE.STATUS,
                TABLE.PASSWORD,
                TABLE.PACKAGE_NAME,
                TABLE.SECRET_HANDLER,
                TABLE.EXPIRE_TIME,
                TABLE.TEMPLATE_PLAN_INFO,
                TABLE.FILE_NAME,
                TABLE.LOCALE
            ).from(TABLE).where(conditions).fetch();
        List<ExportJobInfoDTO> exportJobInfoList = new ArrayList<>();
        result.forEach(record -> exportJobInfoList.add(convertRecordToExportJobInfo(record)));
        return exportJobInfoList;
    }

    @Override
    public boolean setCleanMark(Long appId, String jobId) {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(TABLE.APP_ID.equal(ULong.valueOf(appId)));
        conditions.add(TABLE.ID.equal(jobId));
        return 1 == context.update(TABLE).set(TABLE.IS_CLEANED, UByte.valueOf(1)).where(conditions).execute();
    }

    public ExportJobInfoDTO convertRecordToExportJobInfo(
        Record13<String, ULong, String, ULong, ULong, UByte, String, String, UByte, ULong, String, String,
            String> record) {
        if (record == null) {
            return null;
        }
        ExportJob table = ExportJob.EXPORT_JOB;
        ExportJobInfoDTO exportJobInfo = new ExportJobInfoDTO();
        exportJobInfo.setId(record.get(table.ID));
        exportJobInfo.setAppId(record.get(table.APP_ID).longValue());
        exportJobInfo.setCreator(record.get(table.CREATOR));
        exportJobInfo.setCreateTime(record.get(table.CREATE_TIME).longValue());
        exportJobInfo.setUpdateTime(record.get(table.UPDATE_TIME).longValue());
        exportJobInfo.setStatus(BackupJobStatusEnum.valueOf(record.get(table.STATUS).intValue()));

        // 导出密码解密
        String encryptedPassword = record.get(table.PASSWORD);
        exportJobInfo.setPassword(exportJobPasswordCryptoService.decryptExportJobPassword(encryptedPassword));

        exportJobInfo.setPackageName(record.get(table.PACKAGE_NAME));
        exportJobInfo.setSecretHandler(SecretHandlerEnum.valueOf(record.get(table.SECRET_HANDLER).intValue()));
        exportJobInfo.setExpireTime(record.get(table.EXPIRE_TIME).longValue());
        exportJobInfo.setTemplateInfo(JsonUtils.fromJson(record.get(table.TEMPLATE_PLAN_INFO),
            new TypeReference<List<BackupTemplateInfoDTO>>() {
            }));
        exportJobInfo.setFileName(record.get(table.FILE_NAME));
        exportJobInfo.setLocale(Locale.forLanguageTag(record.get(table.LOCALE)));
        return exportJobInfo;
    }
}
