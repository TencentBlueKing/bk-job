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

package com.tencent.bk.job.backup.archive.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tencent.bk.job.common.annotation.PersistenceObject;
import lombok.Data;
import lombok.ToString;
import net.minidev.json.annotate.JsonIgnore;

import java.util.HashMap;
import java.util.Map;

/**
 * 作业实例归档任务执行详情;通过 json 反序列化存储到 MySQL 中
 */
@Data
@ToString
@PersistenceObject
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ArchiveTaskExecutionDetail {
    /**
     * 归档任务耗时（毫秒）
     */
    private long costTime;
    /**
     * 已归档的记录数量（主表）
     */
    private long archivedRecordSize;
    /**
     * 执行错误信息
     */
    private String errorMsg;

    private Map<String, ArchiveTableDetail> tables = new HashMap<>();

    public void accumulateTableBackup(String tableName, long backupRows, long costTime) {
        ArchiveTableDetail archiveTableDetail = getOrInitArchiveTableDetail(tableName);
        archiveTableDetail.accumulateBackup(backupRows, costTime);
    }

    public void accumulateTableDelete(String tableName, long deleteRows, long costTime) {
        ArchiveTableDetail archiveTableDetail = getOrInitArchiveTableDetail(tableName);
        archiveTableDetail.accumulateDelete(deleteRows, costTime);
    }

    @JsonIgnore
    private ArchiveTableDetail getOrInitArchiveTableDetail(String tableName) {
        ArchiveTableDetail archiveTableDetail = tables.get(tableName);
        if (archiveTableDetail == null) {
            archiveTableDetail = new ArchiveTableDetail();
            tables.put(tableName, archiveTableDetail);
        }
        return archiveTableDetail;
    }

}
