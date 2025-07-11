/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.bk.job.backup.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@NoArgsConstructor
public class ArchiveSummary {
    private boolean enabled;
    private String tableName;
    private String archiveEndDate;
    private boolean skip;
    private boolean success;

    private String archiveMode;

    /**
     * 备份数据-查询原数据耗时（单位毫秒)
     */
    private Long backupReadCost;
    /**
     * 备份数据-写入数据到归档 db 耗时（单位毫秒)
     */
    private Long backupWriteCost;
    /**
     * 删除热数据耗时（单位毫秒)
     */
    private Long deleteCost;
    /**
     * 归档总耗时（单位毫秒)
     */
    private Long archiveCost;

    private Long archiveIdStart;
    private Long archiveIdEnd;
    private Long needArchiveRecordSize;

    private Long lastBackupId;
    private Long backupRecordSize;

    private Long lastDeletedId;
    private Long deleteRecordSize;

    /**
     * 归档详细说明信息
     */
    private String message;

    public ArchiveSummary(String tableName) {
        this.tableName = tableName;
    }
}
