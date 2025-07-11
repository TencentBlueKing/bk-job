/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

import com.tencent.bk.job.execute.common.constants.FileDistStatusEnum;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.engine.model.JobFile;
import com.tencent.bk.job.execute.model.AtomicFileTaskLog;
import com.tencent.bk.job.execute.model.ExecuteObjectCompositeKey;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.FileExecuteObjectLogContent;
import com.tencent.bk.job.execute.model.ScriptExecuteObjectLogContent;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;
import com.tencent.bk.job.logsvr.model.service.ServiceExecuteObjectLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceExecuteObjectScriptLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;

import java.util.List;
import java.util.Map;

/**
 * 日志服务
 */
public interface LogService {

    /**
     * 构造job系统日志
     *
     * @param stepInstance         步骤实例
     * @param executeObject        执行对象
     * @param content              日志原始内容
     * @param offset               日志偏移 - 字节
     * @param logTimeInMillSeconds 日志时间
     * @return 系统日志
     */
    ServiceExecuteObjectScriptLogDTO buildSystemScriptLog(StepInstanceBaseDTO stepInstance,
                                                          ExecuteObject executeObject,
                                                          String content,
                                                          int offset,
                                                          Long logTimeInMillSeconds);

    /**
     * 构造脚本日志
     *
     * @param stepInstance     步骤实例
     * @param executeObject    执行对象
     * @param content          日志原始内容
     * @param contentSizeBytes 日志内容大小(单位byte)
     * @param offset           日志偏移 - 字节
     * @return 系统日志
     */
    ServiceExecuteObjectScriptLogDTO buildScriptLog(StepInstanceBaseDTO stepInstance,
                                                    ExecuteObject executeObject,
                                                    String content,
                                                    int contentSizeBytes,
                                                    int offset);

    /**
     * 写脚本执行日志
     *
     * @param jobCreateTime  任务创建时间
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次;非滚动步骤传入null
     * @param scriptLogs     脚本日志
     */
    void batchWriteScriptLog(long jobCreateTime,
                             long stepInstanceId,
                             int executeCount,
                             Integer batch,
                             List<ServiceExecuteObjectScriptLogDTO> scriptLogs);


    /**
     * 根据执行对象获取脚本执行日志
     *
     * @param stepInstance 步骤实例
     * @param executeCount 执行次数
     * @param batch        滚动执行批次;非滚动步骤传入null
     * @param key          执行对象复合 KEY
     * @return 日志内容
     */
    ScriptExecuteObjectLogContent getScriptExecuteObjectLogContent(StepInstanceBaseDTO stepInstance,
                                                                   int executeCount,
                                                                   Integer batch,
                                                                   ExecuteObjectCompositeKey key);

    /**
     * 根据执行对象任务获取脚本执行日志
     *
     * @param stepInstance      步骤实例
     * @param executeCount      执行次数
     * @param batch             滚动执行批次;非滚动步骤传入null
     * @param executeObjectTask 执行对象任务
     * @return 日志内容
     */
    ScriptExecuteObjectLogContent getScriptExecuteObjectLogContent(StepInstanceBaseDTO stepInstance,
                                                                   int executeCount,
                                                                   Integer batch,
                                                                   ExecuteObjectTask executeObjectTask);

    /**
     * 批量获取脚本执行日志
     *
     * @param jobCreateDateStr           作业创建时间
     * @param stepInstance               步骤实例
     * @param executeCount               执行次数
     * @param batch                      滚动执行批次;非滚动步骤传入null
     * @param executeObjectCompositeKeys 执行对象复合 KEY 列表,最大支持1000个
     * @return 日志内容
     */
    List<ScriptExecuteObjectLogContent> batchGetScriptExecuteObjectLogContent(
        String jobCreateDateStr,
        StepInstanceBaseDTO stepInstance,
        int executeCount,
        Integer batch,
        List<ExecuteObjectCompositeKey> executeObjectCompositeKeys
    );

    /**
     * 根据执行对象获取脚本执行日志
     *
     * @param stepInstance              步骤实例
     * @param executeCount              执行次数
     * @param batch                     滚动执行批次;非滚动步骤传入null
     * @param executeObjectCompositeKey 执行对象复合 KEY
     * @param mode                      文件传输模式
     * @return 日志内容
     */
    FileExecuteObjectLogContent getFileExecuteObjectLogContent(
        StepInstanceBaseDTO stepInstance,
        int executeCount,
        Integer batch,
        ExecuteObjectCompositeKey executeObjectCompositeKey,
        Integer mode
    );

    /**
     * 根据执行对象任务获取脚本执行日志
     *
     * @param stepInstance      步骤实例
     * @param executeCount      执行次数
     * @param batch             滚动执行批次;非滚动步骤传入null
     * @param executeObjectTask 执行对象任务
     * @return 日志内容
     */
    FileExecuteObjectLogContent getFileExecuteObjectLogContent(
        StepInstanceBaseDTO stepInstance,
        int executeCount,
        Integer batch,
        ExecuteObjectTask executeObjectTask
    );

    /**
     * 根据文件任务ID批量获取文件任务执行日志
     *
     * @param stepInstanceId 步骤实例 ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次;非滚动步骤传入null
     * @param taskIds        文件任务ID列表
     * @return 日志内容
     */
    List<AtomicFileTaskLog> getAtomicFileTaskLogByTaskIds(long stepInstanceId,
                                                          int executeCount,
                                                          Integer batch,
                                                          List<String> taskIds);

    /**
     * 获取文件任务文件源日志
     *
     * @param stepInstanceId 步骤实例 ID
     * @param executeCount   执行次数
     * @param batch          滚动执行批次;非滚动步骤传入null
     * @return 日志内容
     */
    List<FileExecuteObjectLogContent> batchGetFileSourceExecuteObjectLogContent(long stepInstanceId,
                                                                                int executeCount,
                                                                                Integer batch);

    /**
     * 获取文件任务文件日志
     *
     * @param stepInstanceId             步骤实例 ID
     * @param executeCount               执行次数
     * @param batch                      滚动执行批次;非滚动步骤传入null
     * @param mode                       文件分发任务模式;传入 null 该过滤条件不生效
     * @param executeObjectCompositeKeys 执行对象复合 KEY 列表;传入 null 该过滤条件不生效
     * @return 日志内容
     */
    List<FileExecuteObjectLogContent> batchGetFileExecuteObjectLogContent(
        long stepInstanceId,
        int executeCount,
        Integer batch,
        FileTaskModeEnum mode,
        List<ExecuteObjectCompositeKey> executeObjectCompositeKeys);

    /**
     * 根据日志关键字获取对应的执行对象KEY
     *
     * @param stepInstance 步骤实例
     * @param executeCount 执行次数
     * @param batch        滚动执行批次;非滚动步骤传入null
     * @param keyword      关键字
     * @return 执行对象列表¬
     */
    List<ExecuteObjectCompositeKey> getExecuteObjectsCompositeKeysByContentKeyword(StepInstanceBaseDTO stepInstance,
                                                                                   int executeCount,
                                                                                   Integer batch,
                                                                                   String keyword);

    /**
     * 写文件日志日志 - 指定时间
     *
     * @param jobCreateTime        任务创建时间
     * @param hostFileLogs         主机执行日志
     * @param logTimeInMillSeconds 日志时间
     */
    void writeFileLogsWithTimestamp(long jobCreateTime,
                                    List<ServiceExecuteObjectLogDTO> hostFileLogs,
                                    Long logTimeInMillSeconds);


    /**
     * 写文件日志日志
     *
     * @param jobCreateTime     任务创建时间
     * @param executeObjectLogs 文件任务执行日志
     */
    void writeFileLogs(long jobCreateTime, List<ServiceExecuteObjectLogDTO> executeObjectLogs);

    /**
     * 构造上传文件任务日志
     *
     * @param stepInstance 步骤实例
     * @param srcFile      源文件
     * @param status       任务状态
     * @param size         源文件大小
     * @param speed        上传速度
     * @param process      进度
     * @param content      日志内容
     */
    ServiceFileTaskLogDTO buildUploadServiceFileTaskLogDTO(StepInstanceDTO stepInstance,
                                                           JobFile srcFile,
                                                           FileDistStatusEnum status,
                                                           String size,
                                                           String speed,
                                                           String process,
                                                           String content);

    /**
     * 构造上传文件任务日志
     *
     * @param stepInstance       步骤实例
     * @param fileType           源文件类型
     * @param srcFilePath        源文件路径
     * @param displaySrcFilePath 展示给用户的源文件路径
     * @param executeObject      上传源执行对象
     * @param status             任务状态
     * @param size               源文件大小
     * @param speed              上传速度
     * @param process            进度
     * @param content            日志内容
     */
    ServiceFileTaskLogDTO buildUploadServiceFileTaskLogDTO(StepInstanceDTO stepInstance,
                                                           TaskFileTypeEnum fileType,
                                                           String srcFilePath,
                                                           String displaySrcFilePath,
                                                           ExecuteObject executeObject,
                                                           FileDistStatusEnum status,
                                                           String size,
                                                           String speed,
                                                           String process,
                                                           String content);

    /**
     * 构造下载文件任务日志
     *
     * @param stepInstance        步骤实例
     * @param srcFile             源文件
     * @param targetExecuteObject 目标执行对象
     * @param targetPath          目标路径
     * @param status              任务状态
     * @param size                源文件大小
     * @param speed               下载速度
     * @param process             进度
     * @param content             日志内容
     */
    ServiceFileTaskLogDTO buildDownloadServiceFileTaskLogDTO(StepInstanceDTO stepInstance,
                                                             JobFile srcFile,
                                                             ExecuteObject targetExecuteObject,
                                                             String targetPath,
                                                             FileDistStatusEnum status,
                                                             String size,
                                                             String speed,
                                                             String process,
                                                             String content);

    /**
     * 增加文件任务日志，并且按照执行对象的维度对文件任务日志进行分组
     *
     * @param stepInstance      步骤实例
     * @param executeObjectLogs 执行对象日志列表
     * @param executeObject     文件任务对应的执行对象
     * @param fileTaskLog       单个文件任务日志
     */
    void addFileTaskLog(StepInstanceBaseDTO stepInstance,
                        Map<ExecuteObjectCompositeKey, ServiceExecuteObjectLogDTO> executeObjectLogs,
                        ExecuteObject executeObject,
                        ServiceFileTaskLogDTO fileTaskLog);

}
