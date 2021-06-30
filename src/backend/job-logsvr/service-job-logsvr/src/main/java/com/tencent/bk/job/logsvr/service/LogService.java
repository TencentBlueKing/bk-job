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

package com.tencent.bk.job.logsvr.service;

import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.logsvr.consts.LogTypeEnum;
import com.tencent.bk.job.logsvr.model.FileLogQuery;
import com.tencent.bk.job.logsvr.model.FileTaskLog;
import com.tencent.bk.job.logsvr.model.ScriptLogQuery;
import com.tencent.bk.job.logsvr.model.TaskIpLog;

import java.util.List;

/**
 * 作业执行日志服务
 */
public interface LogService {
    /**
     * 保存执行日志
     *
     * @param taskIpLog 执行日志
     */
    void saveLog(TaskIpLog taskIpLog);

    /**
     * 保存执行日志
     *
     * @param logType    日志类型
     * @param taskIpLogs 执行日志
     */
    void saveLogs(LogTypeEnum logType, List<TaskIpLog> taskIpLogs);

    /**
     * 获取ip对应的脚本任务日志
     *
     * @param query 获取执行日志请求
     * @return 执行日志
     */
    TaskIpLog getScriptLogByIp(ScriptLogQuery query);

    /**
     * 批量获取脚本执行日志
     *
     * @param query 获取执行日志请求
     * @return 日志内容
     * @throws ServiceException 异常
     */
    List<TaskIpLog> batchGetScriptLogByIps(ScriptLogQuery query) throws ServiceException;

    /**
     * 获取ip对应的文件任务日志
     *
     * @param query 获取执行日志请求
     * @return 执行日志
     */
    TaskIpLog getFileLogByIp(FileLogQuery query);

    /**
     * 根据分发模式获取
     *
     * @param query 获取执行日志请求
     * @return
     */
    List<FileTaskLog> getFileLogs(FileLogQuery query);

    /**
     * 根据文件任务ID获取日志
     *
     * @param jobCreateDate  创建时间
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param taskIds        任务ID列表
     * @return
     */
    List<FileTaskLog> getFileLogsByTaskIds(String jobCreateDate, long stepInstanceId, int executeCount,
                                           List<String> taskIds);

    /**
     * 删除步骤日志
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param jobCreateDate  任务创建时间,yyyy_MM_dd
     */
    long deleteStepContent(Long stepInstanceId, Integer executeCount, String jobCreateDate);

    /**
     * 返回日志内容包含关键字的任务对应的主机ip
     *
     * @param stepInstanceId 步骤ID
     * @param executeCount   执行次数
     * @param jobCreateDate  创建时间
     * @param keyword        查询关键字
     * @return ip
     */
    List<IpDTO> getIpsByKeyword(long stepInstanceId, Integer executeCount, String jobCreateDate, String keyword);

}
