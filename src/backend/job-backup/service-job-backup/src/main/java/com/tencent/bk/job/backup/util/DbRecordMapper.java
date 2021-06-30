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

package com.tencent.bk.job.backup.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.backup.constant.BackupJobStatusEnum;
import com.tencent.bk.job.backup.constant.DuplicateIdHandlerEnum;
import com.tencent.bk.job.backup.constant.LogEntityTypeEnum;
import com.tencent.bk.job.backup.constant.SecretHandlerEnum;
import com.tencent.bk.job.backup.model.dto.*;
import com.tencent.bk.job.backup.model.tables.ExportJob;
import com.tencent.bk.job.backup.model.tables.ImportJob;
import com.tencent.bk.job.common.util.json.JsonUtils;
import org.jooq.Record13;
import org.jooq.Record9;
import org.jooq.types.UByte;
import org.jooq.types.ULong;

import java.util.List;
import java.util.Locale;

/**
 * @since 28/7/2020 22:38
 */
public class DbRecordMapper {

    public static ExportJobInfoDTO convertRecordToExportJobInfo(
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
        exportJobInfo.setPassword(record.get(table.PASSWORD));
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

    public static ImportJobInfoDTO convertRecordToImportJobInfo(
        Record13<String, ULong, String, ULong, ULong, UByte,
            String, String, String, String, UByte, String, String> record) {
        if (record == null) {
            return null;
        }
        ImportJob table = ImportJob.IMPORT_JOB;
        ImportJobInfoDTO importJobInfo = new ImportJobInfoDTO();

        importJobInfo.setId(record.get(table.ID));
        importJobInfo.setAppId(record.get(table.APP_ID).longValue());
        importJobInfo.setCreator(record.get(table.CREATOR));
        importJobInfo.setCreateTime(record.get(table.CREATE_TIME).longValue());
        importJobInfo.setUpdateTime(record.get(table.UPDATE_TIME).longValue());
        importJobInfo.setStatus(BackupJobStatusEnum.valueOf(record.get(table.STATUS).intValue()));
        importJobInfo.setExportId(record.get(table.EXPORT_ID));
        importJobInfo.setFileName(record.get(table.FILE_NAME));
        importJobInfo.setTemplateInfo(JsonUtils.fromJson(record.get(table.TEMPLATE_PLAN_INFO),
            new TypeReference<List<BackupTemplateInfoDTO>>() {
            }));
        importJobInfo.setDuplicateSuffix(record.get(table.DUPLICATE_SUFFIX));
        importJobInfo
            .setDuplicateIdHandler(DuplicateIdHandlerEnum.valueOf(record.get(table.DUPLICATE_ID_HANDLER).intValue()));
        if (record.get(table.ID_NAME_INFO) != null) {
            importJobInfo.setIdNameInfo(
                JsonUtils.fromJson(record.get(table.ID_NAME_INFO), new TypeReference<IdNameInfoDTO>() {
                }));
        }
        importJobInfo.setLocale(Locale.forLanguageTag(record.get(table.LOCALE)));
        return importJobInfo;
    }

    public static LogEntityDTO
    convertRecordToLogEntity(Record9<ULong, String, UByte, ULong, String, ULong, ULong, String, String> record) {
        if (record == null) {
            return null;
        }
        LogEntityDTO logEntityDTO = new LogEntityDTO();
        logEntityDTO.setAppId(DbUtils.convertJooqLongValue((ULong) record.get(0)));
        logEntityDTO.setJobId((String) record.get(1));
        logEntityDTO.setType(LogEntityTypeEnum.valueOf(((UByte) record.get(2)).intValue()));
        logEntityDTO.setTimestamp(DbUtils.convertJooqLongValue((ULong) record.get(3)));
        logEntityDTO.setContent((String) record.get(4));
        logEntityDTO.setTemplateId(DbUtils.convertJooqLongValue((ULong) record.get(5)));
        logEntityDTO.setPlanId(DbUtils.convertJooqLongValue((ULong) record.get(6)));
        logEntityDTO.setLinkText(DbUtils.convertString(record.get(7)));
        logEntityDTO.setLinkUrl(DbUtils.convertString(record.get(8)));
        return logEntityDTO;
    }
}
