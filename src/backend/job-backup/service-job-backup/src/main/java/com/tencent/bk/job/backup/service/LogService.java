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

package com.tencent.bk.job.backup.service;

import com.tencent.bk.job.backup.constant.LogEntityTypeEnum;
import com.tencent.bk.job.backup.model.dto.LogEntityDTO;

import java.util.List;

/**
 * @since 29/7/2020 11:10
 */
public interface LogService {

    /**
     * 记录导出日志
     *
     * @param appId   业务 ID
     * @param jobId   导出任务 ID
     * @param message 日志消息
     */
    void addExportLog(Long appId, String jobId, String message);

    /**
     * 记录普通导入日志
     *
     * @param appId   业务 ID
     * @param jobId   导入任务 ID
     * @param message 日志消息
     */
    void addImportLog(Long appId, String jobId, String message);

    /**
     * 带类型记录导入日志
     *
     * @param appId   业务 ID
     * @param jobId   导入任务 ID
     * @param message 日志消息
     * @param type    日志类型
     */
    void addImportLog(Long appId, String jobId, String message, LogEntityTypeEnum type);

    /**
     * 带类型和附加信息记录导入日志
     *
     * @param appId      业务 ID
     * @param jobId      导入任务 ID
     * @param message    日志消息
     * @param type       日志类型
     * @param templateId 关联的作业模版 ID
     * @param planId     关联的执行方案 ID
     */
    void addImportLog(Long appId, String jobId, String message, LogEntityTypeEnum type, long templateId, long planId);

    /**
     * 按任务 ID 批量拉取导出日志
     *
     * @param appId 业务 ID
     * @param jobId 导出任务 ID
     * @return 导出日志列表
     */
    List<LogEntityDTO> getExportLogById(Long appId, String jobId);

    /**
     * 按任务 ID 批量拉取导入日志
     *
     * @param appId 业务 ID
     * @param jobId 导入任务 ID
     * @return 导入日志列表
     */
    List<LogEntityDTO> getImportLogById(Long appId, String jobId);

}
