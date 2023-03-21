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
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.common.constants.FileDistStatusEnum;
import com.tencent.bk.job.execute.engine.model.JobFile;
import com.tencent.bk.job.execute.model.FileIpLogContent;
import com.tencent.bk.job.execute.model.ScriptHostLogContent;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceHostLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceHostLogsDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceScriptLogDTO;

import java.util.List;

/**
 * 日志服务
 */
public interface LogService {

    /**
     * 构造job系统日志
     *
     * @param host                 主机
     * @param content              日志原始内容
     * @param offset               日志偏移 - 字节
     * @param logTimeInMillSeconds 日志时间
     * @return 系统日志
     */
    ServiceScriptLogDTO buildSystemScriptLog(HostDTO host,
                                             String content,
                                             int offset,
                                             Long logTimeInMillSeconds);

    /**
     * 写脚本执行日志
     *
     * @param jobCreateTime  任务创建时间
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次;非滚动步骤传入null
     * @param scriptLogs     脚本日志
     */
    void batchWriteScriptLog(long jobCreateTime, long stepInstanceId, int executeCount, Integer batch,
                             List<ServiceScriptLogDTO> scriptLogs);


    /**
     * 获取脚本执行日志
     *
     * @param stepInstanceId 步骤实例 ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次;非滚动步骤传入null
     * @param host           主机
     * @return 日志内容
     */
    ScriptHostLogContent getScriptHostLogContent(long stepInstanceId, int executeCount, Integer batch, HostDTO host);

    /**
     * 批量获取脚本执行日志
     *
     * @param jobCreateDateStr 作业创建时间
     * @param stepInstanceId   步骤实例 ID
     * @param executeCount     执行次数
     * @param batch            滚动执行批次;非滚动步骤传入null
     * @param hosts            主机列表,最大支持1000个
     * @return 日志内容
     */
    List<ScriptHostLogContent> batchGetScriptHostLogContent(String jobCreateDateStr, long stepInstanceId,
                                                            int executeCount, Integer batch, List<HostDTO> hosts);

    /**
     * 获取脚本执行日志
     *
     * @param stepInstanceId 步骤实例 ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次;非滚动步骤传入null
     * @param host           主机
     * @param mode           文件传输模式
     * @return 日志内容
     * @throws ServiceException 异常
     */
    FileIpLogContent getFileIpLogContent(long stepInstanceId, int executeCount, Integer batch, HostDTO host,
                                         Integer mode);

    /**
     * 获取脚本执行日志
     *
     * @param stepInstanceId 步骤实例 ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次;非滚动步骤传入null
     * @param taskIds        文件任务ID列表
     * @return 日志内容
     */
    List<ServiceFileTaskLogDTO> getFileLogContentByTaskIds(long stepInstanceId, int executeCount, Integer batch,
                                                           List<String> taskIds);

    /**
     * 获取文件任务文件源日志
     *
     * @param stepInstanceId 步骤实例 ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次;非滚动步骤传入null
     * @return 日志内容
     */
    List<ServiceFileTaskLogDTO> batchGetFileSourceIpLogContent(long stepInstanceId,
                                                               int executeCount,
                                                               Integer batch);

    /**
     * 获取文件任务文件源日志
     *
     * @param stepInstanceId 步骤实例 ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次;非滚动步骤传入null
     * @param hosts          主机列表
     * @return 日志内容
     */
    ServiceHostLogsDTO batchGetFileIpLogContent(long stepInstanceId,
                                                int executeCount,
                                                Integer batch,
                                                List<HostDTO> hosts);

    /**
     * 根据关键字获取对应的ip
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次;非滚动步骤传入null
     * @param keyword        关键字
     * @return ips
     */
    List<HostDTO> getIpsByContentKeyword(long stepInstanceId, int executeCount, Integer batch, String keyword);

    /**
     * 写文件日志日志 - 指定时间
     *
     * @param jobCreateTime        任务创建时间
     * @param hostFileLogs         主机执行日志
     * @param logTimeInMillSeconds 日志时间
     */
    void writeFileLogsWithTimestamp(long jobCreateTime,
                                    List<ServiceHostLogDTO> hostFileLogs,
                                    Long logTimeInMillSeconds);


    /**
     * 写文件日志日志
     *
     * @param jobCreateTime 任务创建时间
     * @param hostFileLogs  文件任务执行日志
     */
    void writeFileLogs(long jobCreateTime, List<ServiceHostLogDTO> hostFileLogs);

    /**
     * 构造上传文件任务日志
     *
     * @param srcFile 源文件
     * @param status  任务状态
     * @param size    源文件大小
     * @param speed   上传速度
     * @param process 进度
     * @param content 日志内容
     */
    ServiceFileTaskLogDTO buildUploadServiceFileTaskLogDTO(JobFile srcFile,
                                                           FileDistStatusEnum status,
                                                           String size,
                                                           String speed,
                                                           String process,
                                                           String content);

    /**
     * 构造下载文件任务日志
     *
     * @param srcFile    源文件
     * @param targetHost 目标主机
     * @param targetPath 目标路径
     * @param status     任务状态
     * @param size       源文件大小
     * @param speed      下载速度
     * @param process    进度
     * @param content    日志内容
     */
    ServiceFileTaskLogDTO buildDownloadServiceFileTaskLogDTO(JobFile srcFile,
                                                             HostDTO targetHost,
                                                             String targetPath,
                                                             FileDistStatusEnum status,
                                                             String size,
                                                             String speed,
                                                             String process,
                                                             String content);

}
