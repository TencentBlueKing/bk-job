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

package com.tencent.bk.job.manage.common.consts;

/**
 * JOB资源状态
 *
 * @date 2019/09/19
 */

public enum JobResourceStatusEnum {
    /**
     * 未上线-草稿
     */
    DRAFT(0),
    /**
     * 已上线
     */
    ONLINE(1),
    /**
     * 已下线
     */
    OFFLINE(2),
    /**
     * 禁用
     */
    DISABLED(3);

    private int status;

    JobResourceStatusEnum(int status) {
        this.status = status;
    }

    public static JobResourceStatusEnum getJobResourceStatus(Integer status) {
        if (status == null) {
            return null;
        }
        if (status == ONLINE.getValue()) {
            return ONLINE;
        } else if (status == DRAFT.getValue()) {
            return DRAFT;
        } else if (status == DISABLED.getValue()) {
            return DISABLED;
        } else if (status == OFFLINE.getValue()) {
            return OFFLINE;
        } else {
            return null;
        }
    }

    public int getValue() {
        return status;
    }

    public String getStatusI18nKey() {
        if (this == DRAFT) {
            return "job.resource.status.draft";
        } else if (this == ONLINE) {
            return "job.resource.status.online";
        } else if (this == DISABLED) {
            return "job.resource.status.disabled";
        } else if (this == OFFLINE) {
            return "job.resource.status.offline";
        } else {
            return "";
        }
    }
}
