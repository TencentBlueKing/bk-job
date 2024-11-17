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

import org.jooq.Record;
import org.jooq.Table;
import org.jooq.TableField;

import java.util.List;

/**
 * job-execute 微服务的表数据DAO
 *
 * @param <T> 表记录
 */
public interface ExecuteRecordDAO<T extends Record> {

    /**
     * 获取表
     *
     * @return 表
     */
    Table<T> getTable();

    /**
     * 获取用于查询归档记录的ID字段
     *
     * @return ID字段
     */
    TableField<T, Long> getArchiveIdField();

    /**
     * 根据起始/结束ID获取表记录
     *
     * @param start  起始ID(exclude)
     * @param end    结束ID(include)
     * @param offset 记录偏移数
     * @param limit  获取的记录数量
     * @return 表记录
     */
    List<T> listRecords(Long start, Long end, Long offset, Long limit);

    /**
     * 根据起始/结束ID删除表记录
     *
     * @param start                起始ID(exclude)
     * @param end                  结束ID(include)
     * @param maxLimitedDeleteRows 批量删除每批次limit
     * @return 删除的记录数量
     */
    int deleteRecords(Long start, Long end, long maxLimitedDeleteRows);

    /**
     * 获取表中最小归档ID
     *
     * @return id值。如果表中没有数据，那么返回 null
     */
    Long getMinArchiveId();
}
