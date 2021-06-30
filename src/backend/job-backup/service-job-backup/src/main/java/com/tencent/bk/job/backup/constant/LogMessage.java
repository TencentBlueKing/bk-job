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

package com.tencent.bk.job.backup.constant;

/**
 * @since 10/10/2020 16:15
 */
public class LogMessage {
    public static final String START_EXPORT = "job.backup.startExport";
    public static final String PROCESS_FINISHED = "job.backup.processFinished";
    public static final String PROCESS_LOCAL_FILE = "job.backup.processLocalFile";
    public static final String NO_LOCAL_FILE = "job.backup.noLocalFile";
    public static final String PROCESS_SCRIPT = "job.backup.processScript";
    public static final String NO_SCRIPT = "job.backup.noScript";
    public static final String PROCESS_CIPHER_TEXT = "job.backup.processCipherText";
    public static final String SAVE_NULL = "job.backup.saveNull";
    public static final String SAVE_REAL = "job.backup.saveReal";
    public static final String NO_CIPHER_TEXT = "job.backup.noCipherText";
    public static final String PROCESS_TEMPLATE_PLAN_FINISHED = "job.backup.processTemplatePlanFinished";
    public static final String START_PACKAGE = "job.backup.startPackage";
    public static final String PACKAGE_FINISHED = "job.backup.packageFinished";
    public static final String START_ENCRYPTING = "job.backup.startEncrypting";
    public static final String ENCRYPTING_FINISHED = "job.backup.encryptingFinished";
    public static final String SKIP_ENCRYPTING = "job.backup.skipEncrypting";
    public static final String EXPORT_FINISHED = "job.backup.exportFinished";
    public static final String FOREVER = "job.backup.forever";
    public static final String DAY = "job.backup.day";

    public static final String TEMPLATE = "job.backup.template";
    public static final String PLAN = "job.backup.plan";
    public static final String PROCESS_UPLOAD_FILE = "job.backup.processUploadFile";
    public static final String DETECT_FILE_TYPE = "job.backup.detectFileType";
    public static final String CORRECT_FILE_TYPE = "job.backup.correctFileType";
    public static final String WRONG_FILE_TYPE = "job.backup.wrongFileType";
    public static final String EXTRACT_FILE_DATA = "job.backup.extractFileData";
    public static final String EXTRACT_SUCCESS = "job.backup.extractSuccess";
    public static final String FILE_ENCRYPTED = "job.backup.fileEncrypted";
    public static final String CORRECT_PASSWORD = "job.backup.correctPassword";
    public static final String WRONG_PASSWORD = "job.backup.wrongPassword";
    public static final String EXTRACT_FAILED = "job.backup.extractFailed";
    public static final String START_IMPORT = "job.backup.startImport";
    public static final String IMPORT_SETTING = "job.backup.importSetting";
    public static final String ID_AUTO_INCREMENT = "job.backup.idAutoIncrement";
    public static final String ID_KEEP_ON_DUPLICATE_INCREMENT = "job.backup.idKeepOnDuplicateIncrement";
    public static final String ID_KEEP_ON_DUPLICATE_SKIP = "job.backup.idKeepOnDuplicateSkip";
    public static final String NAME_DUPLICATE_SUFFIX = "job.backup.nameDuplicateSuffix";

    public static final String ID_AUTO_INCREMENT_SUFFIX = "job.backup.idAutoIncrementSuffix";
    public static final String ID_DUPLICATE_INCREMENT_SUFFIX = "job.backup.idDuplicateIncrementSuffix";
    public static final String ID_DUPLICATE_SKIP_SUFFIX = "job.backup.idDuplicateSkipSuffix";
    public static final String ID_KEEP_SUFFIX = "job.backup.idKeepSuffix";

    public static final String START_IMPORT_TEMPLATE = "job.backup.startImportTemplate";
    public static final String TEMPLATE_NAME_CHANGE = "job.backup.templateNameChange";
    public static final String IMPORT_TEMPLATE_SUCCESS = "job.backup.importTemplateSuccess";

    public static final String START_IMPORT_PLAN = "job.backup.startImportPlan";
    public static final String IMPORT_PLAN_SUCCESS = "job.backup.importPlanSuccess";

    public static final String IMPORT_FAILED = "job.backup.importFailed";

    public static final String IMPORT_FINISHED = "job.backup.importFinished";
}
