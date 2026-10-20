/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 */

package com.tencent.bk.job.consts;

/**
 * OP API路径常量。
 */
public final class ApiPaths {

    public static final String ISSUE_STATUS = "/api/issueStatus";

    /**
     * 兼容期旧路径，调用方迁移到 {@link #ISSUE_STATUS} 后删除
     */
    public static final String LEGACY_ISSUE_STATUS = "/issueStatus";

    private ApiPaths() {
    }
}
