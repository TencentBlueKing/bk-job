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

package com.tencent.bk.job.execute.service;

import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.execute.model.FileIpLogContent;
import com.tencent.bk.job.execute.model.ScriptIpLogContent;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceIpLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceIpLogsDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceScriptLogDTO;

import java.util.List;
import java.util.Map;

/**
 * 日志服务
 */
public interface LogService {

    /**
     * 写job系统日志(非用户脚本输出)
     *
     * @param jobCreateTime        任务创建时间
     * @param stepInstanceId       步骤实例ID
     * @param executeCount         执行次数
     * @param cloudAreaIdAndIp     云区域ID:IP
     * @param content              日志内容
     * @param offset               日志偏移 - 字节
     * @param logTimeInMillSeconds 日志时间
     * @throws ServiceException 写入失败，返回ServiceException
     */
    void writeJobSystemScriptLog(long jobCreateTime, long stepInstanceId, int executeCount,
                                 String cloudAreaIdAndIp, String content, int offset,
                                 Long logTimeInMillSeconds) throws ServiceException;

    /**
     * 批量写job系统日志(非用户脚本输出)
     *
     * @param jobCreateTime        任务创建时间
     * @param stepInstanceId       步骤实例ID
     * @param executeCount         执行次数
     * @param ipsAndOffset         主机列表以及对应的日志偏移
     * @param content              日志内容
     * @param logTimeInMillSeconds 日志时间
     * @throws ServiceException 写入失败，返回ServiceException
     */
    void batchWriteJobSystemScriptLog(long jobCreateTime, long stepInstanceId, int executeCount,
                                      Map<String, Integer> ipsAndOffset, String content,
                                      Long logTimeInMillSeconds) throws ServiceException;

    /**
     * 构造job系统日志
     *
     * @param cloudIp              云区域ID:IP
     * @param content              日志原始内容
     * @param offset               日志偏移 - 字节
     * @param logTimeInMillSeconds 日志时间
     * @return 系统日志
     */
    ServiceScriptLogDTO buildSystemScriptLog(String cloudIp, String content, int offset,
                                             Long logTimeInMillSeconds);

    /**
     * 写脚本执行日志
     *
     * @param jobCreateDate  任务创建时间
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param scriptLog      脚本日志
     * @throws ServiceException 写入失败，返回ServiceException
     */
    void writeScriptLog(String jobCreateDate, long stepInstanceId, int executeCount,
                        ServiceScriptLogDTO scriptLog) throws ServiceException;

    /**
     * 写脚本执行日志
     *
     * @param jobCreateDate  任务创建时间
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param scriptLogs     脚本日志
     * @throws ServiceException 写入失败，返回ServiceException
     */
    void batchWriteScriptLog(String jobCreateDate, long stepInstanceId, int executeCount,
                             List<ServiceScriptLogDTO> scriptLogs) throws ServiceException;

    /**
     * 删除步骤实例对应的日志
     *
     * @param jobCreateDate  任务创建时间
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @throws ServiceException 删除失败，返回ServiceException
     */
    void deleteStepLog(String jobCreateDate, long stepInstanceId, int executeCount) throws ServiceException;

    /**
     * 获取脚本执行日志
     *
     * @param stepInstanceId 步骤实例 ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次;非滚动步骤传入null
     * @param ip             主机ip
     * @return 日志内容
     * @throws ServiceException 异常
     */
    ScriptIpLogContent getScriptIpLogContent(long stepInstanceId, int executeCount, Integer batch, IpDTO ip)
        throws ServiceException;

    /**
     * 批量获取脚本执行日志
     *
     * @param jobCreateDateStr 作业创建时间
     * @param stepInstanceId   步骤实例 ID
     * @param executeCount     执行次数
     * @param ips              主机列表,最大支持1000个
     * @return 日志内容
     * @throws ServiceException 异常
     */
    List<ScriptIpLogContent> batchGetScriptIpLogContent(String jobCreateDateStr, long stepInstanceId, int executeCount,
                                                        List<IpDTO> ips) throws ServiceException;

    /**
     * 获取脚本执行日志
     *
     * @param stepInstanceId 步骤实例 ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次;非滚动步骤传入null
     * @param ip             主机ip
     * @return 日志内容
     * @throws ServiceException 异常
     * @parma mode           文件传输模式
     */
    FileIpLogContent getFileIpLogContent(long stepInstanceId, int executeCount, Integer batch, IpDTO ip,
                                         Integer mode) throws ServiceException;

    /**
     * 获取脚本执行日志
     *
     * @param stepInstanceId 步骤实例 ID
     * @param executeCount   执行次数
     * @param taskIds        文件任务ID列表
     * @return 日志内容
     * @throws ServiceException 异常
     */
    List<ServiceFileTaskLogDTO> getFileLogContentByTaskIds(long stepInstanceId, int executeCount,
                                                           List<String> taskIds) throws ServiceException;

    /**
     * 获取文件任务文件源日志
     *
     * @param stepInstanceId 步骤实例 ID
     * @param executeCount   执行次数
     * @return 日志内容
     * @throws ServiceException
     */
    List<ServiceFileTaskLogDTO> batchGetFileSourceIpLogContent(long stepInstanceId,
                                                               int executeCount) throws ServiceException;

    /**
     * 获取文件任务文件源日志
     *
     * @param stepInstanceId 步骤实例 ID
     * @param executeCount   执行次数
     * @param ips            服务器列表
     * @return 日志内容
     * @throws ServiceException
     */
    ServiceIpLogsDTO batchGetFileIpLogContent(long stepInstanceId,
                                              int executeCount, List<IpDTO> ips) throws ServiceException;

    /**
     * 根据关键字获取对应的ip
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param keyword        关键字
     * @return ips
     * @throws ServiceException 查询失败返回ServcieException
     */
    List<IpDTO> getIpsByContentKeyword(long stepInstanceId, int executeCount, String keyword) throws ServiceException;

    /**
     * 写日志
     *
     * @param jobCreateTime        任务创建时间
     * @param stepInstanceId       步骤实例ID
     * @param executeCount         执行次数
     * @param cloudAreaIdAndIp     云区域ID:IP
     * @param executionLog         文件任务执行日志
     * @param logTimeInMillSeconds 日志时间
     * @throws ServiceException 写入失败，返回ServiceException
     */
    void writeFileLogWithTimestamp(long jobCreateTime, long stepInstanceId, int executeCount,
                                   String cloudAreaIdAndIp, ServiceIpLogDTO executionLog,
                                   Long logTimeInMillSeconds) throws ServiceException;

    /**
     * 写文件日志日志
     *
     * @param jobCreateTime 任务创建时间
     * @param fileLogs      文件任务执行日志
     */
    void writeFileLogs(long jobCreateTime, List<ServiceIpLogDTO> fileLogs);

}
