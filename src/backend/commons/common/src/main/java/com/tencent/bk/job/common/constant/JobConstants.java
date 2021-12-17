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

package com.tencent.bk.job.common.constant;

/**
 * Job 全局通用常量
 */
public class JobConstants {
    /**
     * 公共资源对应的业务ID(比如公共脚本)
     */
    public static final long PUBLIC_APP_ID = 0L;
    /**
     * 执行结果分组标签最大长度
     */
    public static final int RESULT_GROUP_TAG_MAX_LENGTH = 256;
    /**
     * 文件存储后端：本地
     */
    public static final String FILE_STORAGE_BACKEND_LOCAL = "local";
    /**
     * 文件存储后端：制品库
     */
    public static final String FILE_STORAGE_BACKEND_ARTIFACTORY = "artifactory";
    /**
     * PROFILE-Kubernetes
     */
    public static final String PROFILE_KUBERNETES = "kubernetes";
    /**
     * 作业默认超时时间，单位秒
     */
    public static final int DEFAULT_JOB_TIMEOUT_SECONDS = 7200;
    /**
     * 作业最大超时时间，单位秒
     */
    public static final int MAX_JOB_TIMEOUT_SECONDS = 86400;
}
