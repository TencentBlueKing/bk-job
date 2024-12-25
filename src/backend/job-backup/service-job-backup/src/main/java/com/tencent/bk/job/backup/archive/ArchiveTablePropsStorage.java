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

package com.tencent.bk.job.backup.archive;

import com.tencent.bk.job.backup.archive.model.ArchiveTableProps;
import com.tencent.bk.job.backup.config.ArchiveProperties;
import com.tencent.bk.job.common.util.json.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 归档配置存储
 */
@Slf4j
public class ArchiveTablePropsStorage {
    private final ArchiveProperties archiveProperties;
    private final Map<String, ArchiveTableProps> tablePropsMap = new HashMap<>();

    public ArchiveTablePropsStorage(ArchiveProperties archiveProperties) {
        this.archiveProperties = archiveProperties;
        if (archiveProperties.getTableConfigs() != null && !archiveProperties.getTableConfigs().isEmpty()) {
            archiveProperties.getTableConfigs().forEach(
                (tableName, tableConfig) ->
                    tablePropsMap.put(tableName, new ArchiveTableProps(
                        tableConfig.getReadRowLimit(),
                        tableConfig.getBatchInsertRowSize(),
                        tableConfig.getDeleteRowLimit()
                    )));
        }
        log.info("Init archive table properties, tableProps: {}", JsonUtils.toJson(tablePropsMap));
    }

    /**
     * 从 热 DB 表中读取归档数据，每次读取的记录数量限制
     *
     * @param tableName 表名
     */
    public int getReadRowLimit(String tableName) {
        ArchiveTableProps props = tablePropsMap.get(tableName);
        if (props == null || props.getReadRowLimit() == null) {
            return archiveProperties.getReadRowLimit();
        }
        return props.getReadRowLimit();
    }

    /**
     * 写入归档数据到冷 DB，单批次最小行数
     *
     * @param tableName 表名
     */
    public int getBatchInsertRowSize(String tableName) {
        ArchiveTableProps props = tablePropsMap.get(tableName);
        if (props == null || props.getBatchInsertRowSize() == null) {
            return archiveProperties.getBatchInsertRowSize();
        }
        return props.getBatchInsertRowSize();
    }

    /**
     * 从热 DB 删除数据，每次删除的最大行数
     *
     * @param tableName 表名
     */
    public int getDeleteLimitRowCount(String tableName) {
        ArchiveTableProps props = tablePropsMap.get(tableName);
        if (props == null || props.getDeleteLimitRowCount() == null) {
            return archiveProperties.getDeleteRowLimit();
        }
        return props.getDeleteLimitRowCount();
    }
}
