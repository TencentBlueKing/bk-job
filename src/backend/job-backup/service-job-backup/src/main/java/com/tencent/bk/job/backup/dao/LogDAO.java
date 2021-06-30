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

package com.tencent.bk.job.backup.dao;

import com.tencent.bk.job.backup.constant.LogTypeEnum;
import com.tencent.bk.job.backup.model.dto.LogEntityDTO;

import java.util.List;

/**
 * @since 29/7/2020 12:18
 */
public interface LogDAO {

    /**
     * 插入日志
     *
     * @param logEntity 日志信息
     * @param type      日志类型
     */
    void insertLogEntity(LogEntityDTO logEntity, LogTypeEnum type);

    /**
     * 根据作业 ID 拉取日志列表
     *
     * @param appId 业务 ID
     * @param jobId 作业 ID
     * @param type  日志类型
     * @return 日志逆袭列表
     */
    List<LogEntityDTO> listLogById(Long appId, String jobId, LogTypeEnum type);
}
