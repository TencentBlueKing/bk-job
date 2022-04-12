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

package com.tencent.bk.job.manage.common.consts.globalsetting;

/**
 * @Description
 * @Date 2020/2/28
 * @Version 1.0
 */
public class GlobalSettingKeys {
    //执行历史保存时间
    public static final String KEY_HISTORY_EXPIRE_DAYS = "HISTORY_EXPIRE_DAYS";
    //默认命名规则中文
    public static final String KEY_DEFAULT_NAME_RULES = "DEFAULT_NAME_RULES";
    //默认命名规则英文
    public static final String KEY_DEFAULT_NAME_RULES_EN = "DEFAULT_NAME_RULES_EN";
    //当前命名规则中文
    public static final String KEY_CURRENT_NAME_RULES = "CURRENT_NAME_RULES";
    //当前命名规则英文
    public static final String KEY_CURRENT_NAME_RULES_EN = "CURRENT_NAME_RULES_EN";
    //上传文件的限制配置
    public static final String KEY_FILE_UPLOAD_SETTING = "FILE_UPLOAD_SETTING";
    //Title与Footer配置
    public static final String KEY_TITLE_FOOTER = "TITLE_FOOTER";
    //全局可用消息通知渠道是否已配置
    public static final String KEY_NOTIFY_CHANNEL_CONFIGED = "NOTIFY_CHANNEL_CONFIGED";
    //是否启用文件管理特性
    public static final String KEY_ENABLE_FEATURE_FILE_MANAGE = "ENABLE_FEATURE_FILE_MANAGE";
    //是否上传本地文件到制品库
    public static final String KEY_ENABLE_UPLOAD_TO_ARTIFACTORY = "ENABLE_UPLOAD_TO_ARTIFACTORY";
    //Job自有业务集是否已完全迁移至CMDB
    public static final String KEY_IS_BIZSET_MIGRATED_TO_CMDB = "IS_BIZSET_MIGRATED_TO_CMDB";
}
