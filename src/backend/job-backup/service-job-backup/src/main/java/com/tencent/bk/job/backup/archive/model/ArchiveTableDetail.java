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

import lombok.Data;

/**
 * 归档表执行性情
 */
@Data
public class ArchiveTableDetail {
    /**
     * 写入归档冷 DB 的记录行数
     */
    private long backupRows;
    /**
     * 从热 DB 删除的记录行数
     */
    private long deleteRows;
    /**
     * 备份到冷 DB 耗时(毫秒)
     */
    private long backupCostTime;
    /**
     * 删除热数据耗时(毫秒)
     */
    private long deleteCostTime;
    /**
     * 总耗时(毫秒)
     */
    private long costTime;

    /**
     * 累加统计数据 - 备份
     *
     * @param backupRows 已备份行数
     * @param costTime   耗时
     */
    public void accumulateBackup(long backupRows, long costTime) {
        this.backupRows += backupRows;
        this.backupCostTime += costTime;
        this.costTime += costTime;
    }

    /**
     * 累加统计数据 - 删除
     *
     * @param deleteRows 已删除行数
     * @param costTime   耗时
     */
    public void accumulateDelete(long deleteRows, long costTime) {
        this.deleteRows += deleteRows;
        this.deleteCostTime += costTime;
        this.costTime += costTime;
    }

}
