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

package com.tencent.bk.job.upgrader.task.param;

public class ParamNameConsts {
    // 需要用户手动输入的参数
    // 集群中任意一个job-manage实例对应的IP加端口地址，格式：ip:port，例如：127.0.0.1:10505
    public static final String INPUT_PARAM_JOB_MANAGE_SERVER_ADDRESS = "job.manage.server.address";

    // 自动从配置文件读取的参数
    public static final String CONFIG_PROPERTY_APP_CODE = "app.code";
    public static final String CONFIG_PROPERTY_APP_SECRET = "app.secret";
    public static final String CONFIG_PROPERTY_IAM_BASE_URL = "iam.base-url";
    public static final String CONFIG_PROPERTY_ESB_SERVICE_URL = "esb.service.url";
    public static final String CONFIG_PROPERTY_JOB_SECURITY_PUBLIC_KEY_BASE64 = "job.security.public-key-base64";
    public static final String CONFIG_PROPERTY_JOB_SECURITY_PRIVATE_KEY_BASE64 = "job.security.private-key-base64";
    public static final String CONFIG_PROPERTY_JOB_ENCRYPT_PASSWORD = "job.encrypt.password";
    public static final String CONFIG_PROPERTY_ARTIFACTORY_BASE_URL = "artifactory.base-url";
    public static final String CONFIG_PROPERTY_ARTIFACTORY_ADMIN_USERNAME = "artifactory.admin.username";
    public static final String CONFIG_PROPERTY_ARTIFACTORY_ADMIN_PASSWORD = "artifactory.admin.password";
    public static final String CONFIG_PROPERTY_ARTIFACTORY_JOB_USERNAME = "artifactory.job.username";
    public static final String CONFIG_PROPERTY_ARTIFACTORY_JOB_PASSWORD = "artifactory.job.password";
    public static final String CONFIG_PROPERTY_ARTIFACTORY_JOB_PROJECT = "artifactory.job.project";
    public static final String CONFIG_PROPERTY_LOCAL_FILE_ARTIFACTORY_REPO = "local-file.artifactory.repo";
    public static final String CONFIG_PROPERTY_BACKUP_ARTIFACTORY_REPO = "job.backup.artifactory.repo";
    public static final String CONFIG_PROPERTY_LOG_EXPORT_ARTIFACTORY_REPO = "log-export.artifactory.repo";
    public static final String CONFIG_PROPERTY_JOB_STORAGE_ROOT_PATH = "job.storage.root-path";
    public static final String CONFIG_PROPERTY_ENABLE_MIGRATE_LOCAL_UPLOAD_FILE = "job.migrate.local-upload-file.enable";
    public static final String CONFIG_PROPERTY_ENABLE_MIGRATE_BACKUP_FILE = "job.migrate.backup-file.enable";
    public static final String CONFIG_PROPERTY_ENABLE_MIGRATE_LOG_EXPORT_FILE = "job.migrate.log-export-file.enable";
    public static final String CONFIG_PROPERTY_MIGRATE_UPLOAD_CONCURRENCY = "job.migrate.upload-concurrency";
    public static final String CONFIG_PROPERTY_CMDB_DEFAULT_SUPPLIER_ACCOUNT = "cmdb.default.supplier.account";
}
