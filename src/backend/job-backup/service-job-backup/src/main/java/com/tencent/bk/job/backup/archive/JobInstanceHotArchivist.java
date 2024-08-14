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

import org.apache.commons.collections4.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class JobInstanceHotArchivist {
    /**
     * 对一个批次数据进行备份
     *
     * @param start 数据起始记录ID
     * @param stop  数据终止记录ID
     * @return 备份结果
     */
    private AbstractJobInstanceArchivist.BackupResult backupRecords(List<Long> jobInstanceIds) throws IOException {
        if (lastBackupId >= stop) {
            // 说明数据已经备份过，跳过
            log.info("[{}] Record is already backup, skip. lastBackId: {}", tableName, lastBackupId);
            return new AbstractJobInstanceArchivist.BackupResult(0L, 0L, 0L, 0L);
        }
        long startId = start;
        if (lastBackupId > start) {
            // 从上次备份结束的 ID 位置开始
            startId = lastBackupId;
        }

        List<T> recordList = new ArrayList<>(readRowLimit);
        long backupRows = 0;
        long readRows = 0;
        long readCost = 0;
        long writeCost = 0;
        long offset = 0L;
        List<T> records;
        do {
            // 选取start<recordId<=stop的数据
            long readStartTime = System.currentTimeMillis();
            records = listRecord(jobInstanceIds, offset, (long) readRowLimit);
            readRows += records.size();
            long readCostTime = System.currentTimeMillis() - readStartTime;
            readCost += readCostTime;
            log.info(
                "Read {}({}-{}) rows from {}, start={}, stop={}, cost={} ms",
                records.size(), offset, offset + readRowLimit, tableName, startId, stop, readCostTime
            );

            if (CollectionUtils.isNotEmpty(records)) {
                recordList.addAll(records);
            }

            if (recordList.size() >= batchInsertRowSize) {
                long writeStartTime = System.currentTimeMillis();
                int insertRows = insertAndReset(recordList);
                writeCost += (System.currentTimeMillis() - writeStartTime);
                backupRows += insertRows;
            }
            offset += readRowLimit;

        } while (records.size() == readRowLimit);

        if (CollectionUtils.isNotEmpty(recordList)) {
            // 处理没有达到批量插入阈值的最后一个批次的数据
            int insertRows = insertAndReset(recordList);
            backupRows += insertRows;
        }

        updateArchiveProgress(stop);

        return new AbstractJobInstanceArchivist.BackupResult(readRows, backupRows, readCost, writeCost);
    }
}
