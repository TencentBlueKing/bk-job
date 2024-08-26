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

package com.tencent.bk.job.backup.archive.dao;

import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.Table;

import java.util.Collection;
import java.util.List;

/**
 * DB 表操作 DAO
 *
 * @param <T> 表记录
 */
public interface RecordDAO<T extends Record> {

    /**
     * 获取表
     *
     * @return 表
     */
    Table<T> getTable();


    /**
     * 根据条件查询表记录
     *
     * @param conditions 查询条件
     * @param limit          获取的记录数量
     * @return 表记录
     */
    List<T> listRecords(List<Condition> conditions, Long readRowLimit);

    /**
     * 根据起始/结束ID删除表记录
     *
     * @param jobInstanceIds       作业实例 ID 列表
     * @param deleteRowLimit 批量删除每批次limit
     * @return 删除的记录数量
     */
    int deleteRecords(Collection<Long> jobInstanceIds, long deleteRowLimit);
}
