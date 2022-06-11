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
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.logsvr.consts.LogTypeEnum;
import com.tencent.bk.job.logsvr.model.FileLogQuery;
import com.tencent.bk.job.logsvr.model.FileTaskLogDoc;
import com.tencent.bk.job.logsvr.model.ScriptLogQuery;
import com.tencent.bk.job.logsvr.model.TaskHostLog;

import java.util.List;

/**
 * 作业执行日志服务
 */
public interface LogService {
    /**
     * 保存执行日志
     *
     * @param taskHostLog 执行日志
     */
    void saveLog(TaskHostLog taskHostLog);

    /**
     * 保存执行日志
     *
     * @param logType      日志类型
     * @param taskHostLogs 执行日志
     */
    void saveLogs(LogTypeEnum logType, List<TaskHostLog> taskHostLogs);

    /**
     * 批量获取脚本执行日志
     *
     * @param scriptLogQuery 获取执行日志请求
     * @return 日志内容
     * @throws ServiceException 异常
     */
    List<TaskHostLog> listScriptLogs(ScriptLogQuery scriptLogQuery);

    /**
     * 查询文件任务执行日志
     *
     * @param query 查询文件任务执行日志
     */
    List<FileTaskLogDoc> listFileLogs(FileLogQuery query);

    /**
     * 根据文件任务ID获取日志
     *
     * @param jobCreateDate  创建时间
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次
     * @param taskIds        任务ID列表
     * @return 文件任务执行日志
     */
    List<FileTaskLogDoc> getFileLogsByTaskIds(String jobCreateDate,
                                              long stepInstanceId,
                                              int executeCount,
                                              Integer batch,
                                              List<String> taskIds);


    /**
     * 返回日志内容包含关键字的任务对应的主机ip
     *
     * @param jobCreateDate  创建时间
     * @param stepInstanceId 步骤ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次
     * @param keyword        查询关键字
     * @return ip
     */
    List<HostDTO> getIpsByKeyword(String jobCreateDate,
                                  long stepInstanceId,
                                  int executeCount,
                                  Integer batch,
                                  String keyword);

}
