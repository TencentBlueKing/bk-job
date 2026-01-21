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

package com.tencent.bk.job.file_gateway.service.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.mysql.JobTransactional;
import com.tencent.bk.job.common.util.ArrayUtil;
import com.tencent.bk.job.common.util.file.FileSizeUtil;
import com.tencent.bk.job.common.util.file.PathUtil;
import com.tencent.bk.job.execute.common.constants.FileDistStatusEnum;
import com.tencent.bk.job.file_gateway.consts.TaskStatusEnum;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceBatchTaskDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceTaskDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.FileTaskDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.FileWorkerDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.NoTenantFileSourceDAO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceBatchTaskDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceTaskDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileTaskDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileTaskProgressDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileWorkerDTO;
import com.tencent.bk.job.file_gateway.model.resp.inner.FileLogPieceDTO;
import com.tencent.bk.job.file_gateway.model.resp.inner.ThirdFileSourceTaskLogDTO;
import com.tencent.bk.job.file_gateway.service.FileSourceTaskUpdateService;
import com.tencent.bk.job.file_gateway.service.context.TaskContext;
import com.tencent.bk.job.file_gateway.service.context.impl.DefaultTaskContext;
import com.tencent.bk.job.file_gateway.service.listener.FileTaskStatusChangeListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class FileSourceTaskUpdateServiceImpl implements FileSourceTaskUpdateService {

    public static final String PREFIX_REDIS_TASK_LOG = "job:file-gateway:taskLog:";

    private final FileSourceBatchTaskDAO fileSourceBatchTaskDAO;
    private final FileSourceTaskDAO fileSourceTaskDAO;
    private final FileTaskDAO fileTaskDAO;
    private final FileWorkerDAO fileworkerDAO;
    private final NoTenantFileSourceDAO noTenantFileSourceDAO;
    private final RedisTemplate<String, Object> redisTemplate;
    private final List<FileTaskStatusChangeListener> fileTaskStatusChangeListenerList = new ArrayList<>();

    @Autowired
    public FileSourceTaskUpdateServiceImpl(FileSourceBatchTaskDAO fileSourceBatchTaskDAO,
                                           FileSourceTaskDAO fileSourceTaskDAO,
                                           FileTaskDAO fileTaskDAO,
                                           FileWorkerDAO fileworkerDAO,
                                           NoTenantFileSourceDAO noTenantFileSourceDAO,
                                           @Qualifier("jsonRedisTemplate") RedisTemplate<String, Object> redisTemplate,
                                           FileTaskStatusChangeListener fileTaskStatusChangeListener) {
        this.fileSourceBatchTaskDAO = fileSourceBatchTaskDAO;
        this.fileSourceTaskDAO = fileSourceTaskDAO;
        this.fileTaskDAO = fileTaskDAO;
        this.fileworkerDAO = fileworkerDAO;
        this.noTenantFileSourceDAO = noTenantFileSourceDAO;
        this.redisTemplate = redisTemplate;
        addFileTaskStatusChangeListener(fileTaskStatusChangeListener);
    }

    public void addFileTaskStatusChangeListener(FileTaskStatusChangeListener listener) {
        this.fileTaskStatusChangeListenerList.add(listener);
    }

    @Override
    @JobTransactional(transactionManager = "jobFileGatewayTransactionManager")
    public String updateFileSourceTask(String batchTaskId,
                                       String fileSourceTaskId,
                                       Long fileTaskId,
                                       FileTaskProgressDTO fileTaskProgressDTO) {
        // 开启事务后立即加排它锁，保证读取到其他事务已提交的数据
        FileSourceBatchTaskDTO fileSourceBatchTaskDTO =
            fileSourceBatchTaskDAO.getBatchTaskByIdForUpdate(batchTaskId);
        // 查出加锁后的最新数据
        FileSourceTaskDTO fileSourceTaskDTO = fileSourceTaskDAO.getFileSourceTaskByIdForUpdate(fileSourceTaskId);
        FileTaskDTO fileTaskDTO = fileTaskDAO.getFileTaskByIdForUpdate(fileTaskId);
        if (log.isDebugEnabled()) {
            log.debug("fileTaskDTO={}", fileTaskDTO);
        }
        fileTaskDTO.setDownloadPath(fileTaskProgressDTO.getDownloadPath());

        String filePath = fileTaskProgressDTO.getFilePath();
        TaskStatusEnum previousStatus = TaskStatusEnum.valueOf(fileTaskDTO.getStatus());
        TaskStatusEnum status = fileTaskProgressDTO.getStatus();
        Integer progress = fileTaskProgressDTO.getProgress();
        Long fileSize = fileTaskProgressDTO.getFileSize();
        if (fileSourceBatchTaskDTO == null) {
            log.error("Cannot find fileSourceBatchTaskDTO by batchTaskId {} fileSourceTaskId {} filePath {}",
                fileSourceTaskDTO.getBatchTaskId(),
                fileSourceTaskId,
                filePath
            );
            return null;
        }
        FileWorkerDTO fileWorkerDTO = fileworkerDAO.getFileWorkerById(fileSourceTaskDTO.getFileWorkerId());
        int affectedRowNum = -1;
        if (status == TaskStatusEnum.RUNNING) {
            // 已处于结束态的任务不再接受状态更新
            if (!fileTaskDTO.isDone()) {
                fileTaskDTO.setProgress(progress);
                fileTaskDTO.setFileSize(fileSize);
                fileTaskDTO.setStatus(TaskStatusEnum.RUNNING.getStatus());
                affectedRowNum = fileTaskDAO.updateFileTask(fileTaskDTO);
                logUpdatedTaskStatus(fileSourceTaskId, filePath, progress, status);
            } else {
                log.info("fileTask {} already done, do not update to running", fileSourceTaskId);
            }
        } else if (status == TaskStatusEnum.SUCCESS) {
            fileTaskDTO.setProgress(100);
            fileTaskDTO.setStatus(TaskStatusEnum.SUCCESS.getStatus());
            affectedRowNum = fileTaskDAO.updateFileTask(fileTaskDTO);
            logUpdatedTaskStatus(fileSourceTaskId, filePath, progress, status);
        } else if (status == TaskStatusEnum.FAILED) {
            fileTaskDTO.setProgress(progress);
            fileTaskDTO.setStatus(TaskStatusEnum.FAILED.getStatus());
            affectedRowNum = fileTaskDAO.updateFileTask(fileTaskDTO);
            logUpdatedTaskStatus(fileSourceTaskId, filePath, progress, status);
        } else if (status == TaskStatusEnum.STOPPED) {
            fileTaskDTO.setProgress(progress);
            fileTaskDTO.setStatus(TaskStatusEnum.STOPPED.getStatus());
            affectedRowNum = fileTaskDAO.updateFileTask(fileTaskDTO);
            logUpdatedTaskStatus(fileSourceTaskId, filePath, progress, status);
        } else {
            log.warn("fileTask {} unknown status:{}", fileSourceTaskId, status);
        }
        if (affectedRowNum != -1) {
            log.info("{} updated, affectedRowNum={}", fileTaskDTO, affectedRowNum);
        }
        // 通知关注者
        if (status != previousStatus) {
            notifyFileTaskStatusChangeListeners(fileTaskDTO, fileSourceTaskDTO, fileWorkerDTO, previousStatus, status);
        }
        // 进度上报
        writeLog(fileSourceTaskDTO, fileWorkerDTO, fileTaskProgressDTO);
        return fileSourceTaskId;
    }

    private void notifyFileTaskStatusChangeListeners(FileTaskDTO fileTaskDTO,
                                                     FileSourceTaskDTO fileSourceTaskDTO,
                                                     FileWorkerDTO fileWorkerDTO,
                                                     TaskStatusEnum previousStatus,
                                                     TaskStatusEnum currentStatus) {
        TaskContext context = new DefaultTaskContext(fileTaskDTO, fileSourceTaskDTO, fileWorkerDTO);
        if (!fileTaskStatusChangeListenerList.isEmpty()) {
            boolean stop;
            for (FileTaskStatusChangeListener listener : fileTaskStatusChangeListenerList) {
                stop = listener.onStatusChange(context, previousStatus, currentStatus);
                if (stop) break;
            }
        }
    }

    private void logUpdatedTaskStatus(String taskId, String filePath, Integer progress, TaskStatusEnum status) {
        log.info("updated fileTask:{},{},{},{}", taskId, filePath, progress, status.name());
    }

    private void writeLog(FileSourceTaskDTO fileSourceTaskDTO,
                          FileWorkerDTO fileWorkerDTO,
                          FileTaskProgressDTO fileTaskProgressDTO) {
        String taskId = fileSourceTaskDTO.getId();
        String fileSizeStr = FileSizeUtil.getFileSizeStr(fileTaskProgressDTO.getFileSize());
        ThirdFileSourceTaskLogDTO thirdFileSourceTaskLog = new ThirdFileSourceTaskLogDTO();
        String sourceCloudIp = fileWorkerDTO.getCloudIp();
        thirdFileSourceTaskLog.setIp(sourceCloudIp);
        // 追加文件源名称
        // 日志定位坐标：（文件源，文件路径），需要区分不同文件源下相同文件路径的日志
        FileSourceDTO fileSourceDTO = noTenantFileSourceDAO.getFileSourceById(fileSourceTaskDTO.getFileSourceId());
        if (fileSourceDTO == null) {
            throw new NotFoundException(ErrorCode.FILE_SOURCE_NOT_EXIST,
                ArrayUtil.toArray("fileSourceId:" + fileSourceTaskDTO.getFileSourceId()));
        }
        String filePathWithSourceAlias = PathUtil.joinFilePath(
            fileSourceDTO.getAlias(),
            fileTaskProgressDTO.getFilePath()
        );
        List<FileLogPieceDTO> fileLogPieceList = new ArrayList<>();
        FileLogPieceDTO fileLogPiece = new FileLogPieceDTO();
        fileLogPiece.setContent(buildFileLogContent(fileTaskProgressDTO, filePathWithSourceAlias, fileSizeStr));
        fileLogPiece.setDisplaySrcFile(filePathWithSourceAlias);
        fileLogPiece.setProcess(buildProcessStr(fileTaskProgressDTO));
        fileLogPiece.setSize(fileSizeStr);
        fileLogPiece.setSrcIp(sourceCloudIp);
        fileLogPiece.setStatus(FileDistStatusEnum.PULLING.getValue());
        fileLogPiece.setStatusDesc(FileDistStatusEnum.PULLING.getName());
        fileLogPieceList.add(fileLogPiece);
        thirdFileSourceTaskLog.setFileTaskLogs(fileLogPieceList);
        // 写入Redis
        redisTemplate.opsForList().rightPush(PREFIX_REDIS_TASK_LOG + taskId, thirdFileSourceTaskLog);
        // 一小时后过期
        redisTemplate.expireAt(PREFIX_REDIS_TASK_LOG + taskId, new Date(System.currentTimeMillis() + 3600 * 1000));
    }

    @SuppressWarnings("StringBufferReplaceableByString")
    private String buildFileLogContent(FileTaskProgressDTO fileTaskProgressDTO,
                                       String filePathWithSourceAlias,
                                       String fileSizeStr) {
        StringBuilder sb = new StringBuilder();
        sb.append("FileName: ");
        sb.append(filePathWithSourceAlias);
        sb.append(" FileSize: ");
        sb.append(fileSizeStr);
        sb.append(" ");
        sb.append("Speed: ");
        sb.append(fileTaskProgressDTO.getSpeed());
        sb.append(" Progress: ");
        sb.append(fileTaskProgressDTO.getProgress());
        sb.append("% Detail: ");
        sb.append(fileTaskProgressDTO.getContent());
        return sb.toString();
    }

    private String buildProcessStr(FileTaskProgressDTO fileTaskProgressDTO) {
        return "" + fileTaskProgressDTO.getProgress() + "%";
    }

}
