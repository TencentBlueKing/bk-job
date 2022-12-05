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

package com.tencent.bk.job.k8s;

public class Consts {
    // k8s命名空间
    public static final String KEY_KUBERNETES_NAMESPACE = "KUBERNETES_NAMESPACE";
    // 服务启动依赖关系定义字符串
    public static final String KEY_STARTUP_DEPENDENCIES_STR = "BK_JOB_STARTUP_DEPENDENCIES_STR";
    // 当前服务名称
    public static final String KEY_CURRENT_SERVICE_NAME = "BK_JOB_CURRENT_SERVICE_NAME";
    // 所有依赖服务达到Ready状态时必须要有的公共标签
    public static final String KEY_EXPECT_POD_LABELS_COMMON = "BK_JOB_EXPECT_POD_LABELS_COMMON";
    // 为每个依赖服务单独定义的达到Ready状态时必须要有的标签
    public static final String KEY_EXPECT_POD_LABELS_SERVICE = "BK_JOB_EXPECT_POD_LABELS_SERVICE";
}
