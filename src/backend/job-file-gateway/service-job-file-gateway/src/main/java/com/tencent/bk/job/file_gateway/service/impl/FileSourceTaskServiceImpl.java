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

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.http.HttpReq;
import com.tencent.bk.job.common.util.http.JobHttpClient;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.file_gateway.consts.TaskCommandEnum;
import com.tencent.bk.job.file_gateway.consts.TaskStatusEnum;
import com.tencent.bk.job.file_gateway.dao.filesource.FileSourceTaskDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.FileTaskDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.FileWorkerDAO;
import com.tencent.bk.job.file_gateway.dao.filesource.NoTenantFileSourceDAO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileSourceTaskDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileTaskDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileTaskProgressDTO;
import com.tencent.bk.job.file_gateway.model.dto.FileWorkerDTO;
import com.tencent.bk.job.file_gateway.model.resp.inner.FileSourceTaskStatusDTO;
import com.tencent.bk.job.file_gateway.model.resp.inner.TaskInfoDTO;
import com.tencent.bk.job.file_gateway.model.resp.inner.ThirdFileSourceTaskLogDTO;
import com.tencent.bk.job.file_gateway.service.FileSourceTaskService;
import com.tencent.bk.job.file_gateway.service.FileSourceTaskUpdateService;
import com.tencent.bk.job.file_gateway.service.dispatch.DispatchService;
import com.tencent.bk.job.file_gateway.service.remote.FileSourceTaskReqGenService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FileSourceTaskServiceImpl implements FileSourceTaskService {

    public static final String PREFIX_REDIS_TASK_LOG = "job:file-gateway:taskLog:";

    private final FileSourceTaskUpdateService fileSourceTaskUpdateService;
    private final FileSourceTaskDAO fileSourceTaskDAO;
    private final FileTaskDAO fileTaskDAO;
    private final FileWorkerDAO fileworkerDAO;
    private final NoTenantFileSourceDAO noTenantFileSourceDAO;
    private final DispatchService dispatchService;
    private final FileSourceTaskReqGenService fileSourceTaskReqGenService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final JobHttpClient jobHttpClient;

    @Autowired
    public FileSourceTaskServiceImpl(FileSourceTaskUpdateService fileSourceTaskUpdateService,
                                     FileSourceTaskDAO fileSourceTaskDAO,
                                     FileTaskDAO fileTaskDAO,
                                     FileWorkerDAO fileworkerDAO,
                                     NoTenantFileSourceDAO noTenantFileSourceDAO,
                                     DispatchService dispatchService,
                                     FileSourceTaskReqGenService fileSourceTaskReqGenService,
                                     @Qualifier("jsonRedisTemplate") RedisTemplate<String, Object> redisTemplate,
                                     JobHttpClient jobHttpClient) {
        this.fileSourceTaskUpdateService = fileSourceTaskUpdateService;
        this.fileSourceTaskDAO = fileSourceTaskDAO;
        this.fileTaskDAO = fileTaskDAO;
        this.fileworkerDAO = fileworkerDAO;
        this.noTenantFileSourceDAO = noTenantFileSourceDAO;
        this.dispatchService = dispatchService;
        this.fileSourceTaskReqGenService = fileSourceTaskReqGenService;
        this.redisTemplate = redisTemplate;
        this.jobHttpClient = jobHttpClient;
    }

    @Override
    public TaskInfoDTO startFileSourceDownloadTask(String username,
                                                   Long appId,
                                                   Long stepInstanceId,
                                                   Integer executeCount,
                                                   String batchTaskId,
                                                   Integer fileSourceId,
                                                   List<String> filePathList) {
        return startFileSourceDownloadTaskWithId(
            username,
            appId,
            stepInstanceId,
            executeCount,
            batchTaskId,
            fileSourceId,
            filePathList,
            null
        );
    }

    public TaskInfoDTO startFileSourceDownloadTaskWithId(String username,
                                                         Long appId,
                                                         Long stepInstanceId,
                                                         Integer executeCount,
                                                         String batchTaskId,
                                                         Integer fileSourceId,
                                                         List<String> filePathList,
                                                         String fileSourceTaskId) {
        log.info(
            "startFileSourceDownloadTaskWithId, input=({},{},{},{},{},{},{})",
            username,
            appId,
            stepInstanceId,
            executeCount,
            batchTaskId,
            fileSourceId,
            filePathList
        );
        FileSourceDTO fileSourceDTO = noTenantFileSourceDAO.getFileSourceById(fileSourceId);
        if (fileSourceDTO == null) {
            throw new RuntimeException("FileSource not exist, fileSourceId=" + fileSourceId.toString());
        }
        FileWorkerDTO fileWorkerDTO = dispatchService.findBestFileWorker(
            fileSourceDTO,
            "DownloadTask(appId=" + appId + ")"
        );
        if (fileWorkerDTO == null) {
            throw new RuntimeException(String.format("Cannot match fileWorker for FileSourceTask,appId=%d," +
                    "stepInstanceId=%d,fileSourceId=%d,filePathList=%s", appId, stepInstanceId, fileSourceId,
                filePathList.toString()));
        }
        FileSourceTaskDTO fileSourceTaskDTO = saveFileSourceTask(
            username,
            appId,
            stepInstanceId,
            executeCount,
            batchTaskId,
            fileSourceId,
            filePathList,
            fileSourceTaskId,
            fileWorkerDTO.getId()
        );
        fileSourceTaskId = fileSourceTaskDTO.getId();
        try {
            // 分发文件任务
            HttpReq req = fileSourceTaskReqGenService.genDownloadFilesReq(appId, fileWorkerDTO, fileSourceDTO,
                fileSourceTaskDTO);
            postHttpReq(req);
        } catch (Exception e) {
            String msg = MessageFormatter.format(
                "Fail to dispatch FileSourceTask={}",
                JsonUtils.toJson(fileSourceTaskDTO)
            ).getMessage();
            log.error(msg, e);
            // 清理DB中的任务数据便于外层重试
            clearSavedFileSourceTask(fileSourceTaskId);
            throw new InternalException(
                e,
                ErrorCode.FAIL_TO_REQUEST_FILE_WORKER_START_FILE_SOURCE_DOWNLOAD_TASK,
                new String[]{e.getMessage()}
            );
        }
        return new TaskInfoDTO(
            fileSourceTaskId,
            fileSourceDTO.getAlias(),
            fileSourceDTO.getPublicFlag(),
            fileWorkerDTO.getId(),
            fileWorkerDTO.getAccessHost(),
            fileWorkerDTO.getCloudAreaId(),
            fileWorkerDTO.getInnerIpProtocol(),
            fileWorkerDTO.getInnerIp()
        );
    }

    private FileSourceTaskDTO saveFileSourceTask(String username,
                                                 Long appId,
                                                 Long stepInstanceId,
                                                 Integer executeCount,
                                                 String batchTaskId,
                                                 Integer fileSourceId,
                                                 List<String> filePathList,
                                                 String fileSourceTaskId,
                                                 Long fileWorkerId) {
        FileSourceTaskDTO fileSourceTaskDTO = new FileSourceTaskDTO();
        fileSourceTaskDTO.setId(fileSourceTaskId);
        fileSourceTaskDTO.setBatchTaskId(batchTaskId);
        fileSourceTaskDTO.setAppId(appId);
        fileSourceTaskDTO.setCreator(username);
        fileSourceTaskDTO.setCreateTime(System.currentTimeMillis());
        fileSourceTaskDTO.setStepInstanceId(stepInstanceId);
        fileSourceTaskDTO.setExecuteCount(executeCount);
        fileSourceTaskDTO.setFileSourceId(fileSourceId);
        fileSourceTaskDTO.setFileWorkerId(fileWorkerId);
        fileSourceTaskDTO.setStatus(TaskStatusEnum.INIT.getStatus());
        List<FileTaskDTO> fileTaskDTOList = new ArrayList<>();
        for (String filePath : filePathList) {
            FileTaskDTO fileTaskDTO = new FileTaskDTO();
            fileTaskDTO.setId(null);
            fileTaskDTO.setFileSourceTaskId(null);
            fileTaskDTO.setCreateTime(System.currentTimeMillis());
            fileTaskDTO.setProgress(0);
            fileTaskDTO.setFilePath(filePath);
            fileTaskDTO.setDownloadPath(null);
            fileTaskDTO.setStatus(TaskStatusEnum.INIT.getStatus());
            fileTaskDTO.setErrorMsg("");
            fileTaskDTOList.add(fileTaskDTO);
        }
        fileSourceTaskDTO.setFileTaskList(fileTaskDTOList);
        fileSourceTaskId = fileSourceTaskDAO.insertFileSourceTask(fileSourceTaskDTO);
        fileSourceTaskDTO.setId(fileSourceTaskId);
        return fileSourceTaskDTO;
    }

    private void clearSavedFileSourceTask(String fileSourceTaskId) {
        // 1.删除子任务
        int deletedTaskNum = fileTaskDAO.deleteFileTaskByFileSourceTaskId(fileSourceTaskId);
        // 2.删除FileSourceTask任务
        int deletedFileSourceTaskNum = deleteFileSourceTaskById(fileSourceTaskId);
        log.info(
            "{} fileTask {} fileSourceTask deleted, fileSourceTaskId={}",
            deletedTaskNum,
            deletedFileSourceTaskNum,
            fileSourceTaskId
        );
    }

    @Override
    public String updateFileSourceTask(FileTaskProgressDTO fileTaskProgressDTO) {
        String fileSourceTaskId = fileTaskProgressDTO.getFileSourceTaskId();
        String filePath = fileTaskProgressDTO.getFilePath();
        FileTaskDTO fileTaskDTO = fileTaskDAO.getOneFileTask(
            fileSourceTaskId,
            filePath
        );
        if (fileTaskDTO == null) {
            log.error(
                "Cannot find fileTaskDTO by taskId {} filePath {}",
                fileTaskProgressDTO.getFileSourceTaskId(),
                fileTaskProgressDTO.getFilePath()
            );
            return null;
        }
        FileSourceTaskDTO fileSourceTaskDTO = fileSourceTaskDAO.getFileSourceTaskById(fileSourceTaskId);
        if (fileSourceTaskDTO == null) {
            log.error("Cannot find fileSourceTaskDTO by taskId {} filePath {}", fileSourceTaskId, filePath);
            return null;
        }
        String batchTaskId = fileSourceTaskDTO.getBatchTaskId();
        Long fileTaskId = fileTaskDTO.getId();
        return fileSourceTaskUpdateService.updateFileSourceTask(
            batchTaskId,
            fileSourceTaskId,
            fileTaskId,
            fileTaskProgressDTO
        );
    }

    @Override
    public FileSourceTaskStatusDTO getFileSourceTaskStatusAndLogs(String taskId, Long logStart, Long logLength) {
        FileSourceTaskDTO fileSourceTaskDTO = fileSourceTaskDAO.getFileSourceTaskById(taskId);
        if (fileSourceTaskDTO == null) {
            throw new RuntimeException("FileSourceTask not exist, fileTaskId=" + taskId);
        }
        FileWorkerDTO fileWorkerDTO = fileworkerDAO.getFileWorkerById(fileSourceTaskDTO.getFileWorkerId());
        FileSourceTaskStatusDTO fileSourceTaskStatusDTO = new FileSourceTaskStatusDTO();
        fileSourceTaskStatusDTO.setTaskId(taskId);
        fileSourceTaskStatusDTO.setStatus(fileSourceTaskDTO.getStatus());
        StringBuilder messageBuilder = new StringBuilder();
        List<FileTaskDTO> fileTaskDTOList = fileSourceTaskDTO.getFileTaskList();
        fileTaskDTOList.forEach(fileTaskDTO -> {
            messageBuilder.append(fileTaskDTO.getFilePath());
            messageBuilder.append("|");
            messageBuilder.append(fileTaskDTO.getProgress());
            messageBuilder.append("|");
            messageBuilder.append(fileTaskDTO.getErrorMsg());
            messageBuilder.append("\n");
        });
        fileSourceTaskStatusDTO.setMessage(messageBuilder.toString());
        fileSourceTaskStatusDTO.setCloudId(fileWorkerDTO.getCloudAreaId());
        fileSourceTaskStatusDTO.setIpProtocol(fileWorkerDTO.getInnerIpProtocol());
        fileSourceTaskStatusDTO.setIp(fileWorkerDTO.getInnerIp());
        fileSourceTaskStatusDTO.setFileCleared(fileSourceTaskDTO.getFileCleared());
        Map<String, String> filePathMap = new HashMap<>();
        fileTaskDTOList.forEach(fileTaskDTO -> {
            String downloadPath = fileTaskDTO.getDownloadPath();
            if (downloadPath != null) {
                filePathMap.put(fileTaskDTO.getFilePath(), downloadPath);
            } else {
                filePathMap.put(fileTaskDTO.getFilePath(), "no start yet");
            }
        });
        fileSourceTaskStatusDTO.setFilePathMap(filePathMap);
        Long logSize = redisTemplate.opsForList().size(PREFIX_REDIS_TASK_LOG + taskId);
        log.debug("logSize={}", logSize);
        if (logSize == null || logSize == 0) {
            // 没有日志
            fileSourceTaskStatusDTO.setLogList(Collections.emptyList());
            fileSourceTaskStatusDTO.setLogEnd(true);
            return fileSourceTaskStatusDTO;
        }
        long logEnd = logStart + logLength;
        if (logLength < 0) {
            logEnd = logSize;
        }
        log.info("taskId={},logStart={},logEnd={},logSize={}", taskId, logStart, logEnd, logSize);
        List<ThirdFileSourceTaskLogDTO> logDTOList = null;
        List<Object> logObjList = redisTemplate.opsForList().range(
            PREFIX_REDIS_TASK_LOG + taskId,
            logStart,
            logEnd
        );
        if (logObjList != null) {
            logDTOList = logObjList.stream().map(obj -> (ThirdFileSourceTaskLogDTO) obj).collect(Collectors.toList());
        }
        log.debug("logDTOList={}", logDTOList);
        fileSourceTaskStatusDTO.setLogList(logDTOList);
        fileSourceTaskStatusDTO.setLogEnd(logSize <= logEnd);
        return fileSourceTaskStatusDTO;
    }

    @Override
    public Integer stopTasks(List<String> taskIdList) {
        return stopTasksWithCommand(taskIdList, TaskCommandEnum.STOP_AND_REPORT);
    }

    @Override
    public Integer recallTasks(List<String> taskIdList) {
        return stopTasksWithCommand(taskIdList, TaskCommandEnum.STOP_QUIETLY);
    }

    private void addTaskIdToWorkerTaskMap(Map<FileWorkerDTO, List<String>> workerTaskMap,
                                          FileWorkerDTO fileWorkerDTO,
                                          String taskId) {
        List<String> workerTaskIdList;
        if (workerTaskMap.containsKey(fileWorkerDTO)) {
            workerTaskIdList = workerTaskMap.get(fileWorkerDTO);
        } else {
            workerTaskIdList = new ArrayList<>();
            workerTaskMap.put(fileWorkerDTO, workerTaskIdList);
        }
        workerTaskIdList.add(taskId);
    }

    private String postHttpReq(HttpReq req) {
        return jobHttpClient.post(req);
    }

    private Integer stopTasksWithCommand(List<String> taskIdList, TaskCommandEnum command) {
        // 将taskId按照FileWorker分组
        Map<FileWorkerDTO, List<String>> workerTaskMap = new HashMap<>();
        for (String taskId : taskIdList) {
            FileSourceTaskDTO fileSourceTaskDTO = fileSourceTaskDAO.getFileSourceTaskById(taskId);
            if (fileSourceTaskDTO == null || fileSourceTaskDTO.isDone() || fileSourceTaskDTO.getFileCleared()) {
                // 任务已达终止态/已被清理
                continue;
            }
            FileWorkerDTO fileWorkerDTO = fileworkerDAO.getFileWorkerById(fileSourceTaskDTO.getFileWorkerId());
            if (fileWorkerDTO == null || fileWorkerDTO.getOnlineStatus().intValue() == 0) {
                // FileWorker已丢失/下线
                continue;
            }
            addTaskIdToWorkerTaskMap(workerTaskMap, fileWorkerDTO, taskId);
        }
        int allCount = 0;
        // 逐个Worker停止任务
        for (Map.Entry<FileWorkerDTO, List<String>> entry : workerTaskMap.entrySet()) {
            FileWorkerDTO worker = entry.getKey();
            List<String> taskIds = entry.getValue();
            try {
                HttpReq req = fileSourceTaskReqGenService.genStopTasksReq(worker, taskIds, command);
                String respStr = postHttpReq(req);
                Integer count = parseInteger(respStr);
                allCount += count;
            } catch (Exception e) {
                if (e instanceof ServiceException) {
                    throw (ServiceException) e;
                } else {
                    throw new InternalException(e, ErrorCode.FAIL_TO_REQUEST_FILE_WORKER_STOP_TASKS,
                        new String[]{e.getMessage()});
                }
            }
        }
        return allCount;
    }

    @Override
    public Integer clearTaskFiles(List<String> taskIdList) {
        List<String> targetTaskIdList = new ArrayList<>();
        // 将taskId按照FileWorker分组
        Map<FileWorkerDTO, List<String>> workerTaskMap = new HashMap<>();
        for (String taskId : taskIdList) {
            FileSourceTaskDTO fileSourceTaskDTO = fileSourceTaskDAO.getFileSourceTaskById(taskId);
            if (fileSourceTaskDTO == null || fileSourceTaskDTO.getFileCleared()) {
                // 任务已被清理
                continue;
            }
            FileWorkerDTO fileWorkerDTO = fileworkerDAO.getFileWorkerById(fileSourceTaskDTO.getFileWorkerId());
            if (fileWorkerDTO == null || fileWorkerDTO.getOnlineStatus().intValue() == 0) {
                // FileWorker已丢失/下线
                continue;
            }
            addTaskIdToWorkerTaskMap(workerTaskMap, fileWorkerDTO, taskId);
            targetTaskIdList.add(taskId);
        }
        int affectedRowNum = fileSourceTaskDAO.updateFileClearStatus(targetTaskIdList, true);
        log.info("{}/{} taskFile set clear status true", affectedRowNum, targetTaskIdList.size());
        int allCount = 0;
        // 逐个Worker清理文件
        for (Map.Entry<FileWorkerDTO, List<String>> entry : workerTaskMap.entrySet()) {
            FileWorkerDTO worker = entry.getKey();
            List<String> taskIds = entry.getValue();
            try {
                HttpReq req = fileSourceTaskReqGenService.genClearTaskFilesReq(worker, taskIds);
                String respStr = postHttpReq(req);
                Integer count = parseInteger(respStr);
                allCount += count;
            } catch (Exception e) {
                if (e instanceof ServiceException) {
                    throw (ServiceException) e;
                } else {
                    throw new InternalException(e, ErrorCode.FAIL_TO_REQUEST_FILE_WORKER_CLEAR_TASK_FILES,
                        new String[]{e.getMessage()});
                }
            }
        }
        return allCount;
    }

    @Override
    public FileSourceTaskDTO getFileSourceTaskById(String id) {
        return fileSourceTaskDAO.getFileSourceTaskById(id);
    }

    @Override
    public Integer deleteFileSourceTaskById(String id) {
        return fileSourceTaskDAO.deleteById(id);
    }

    private Integer parseInteger(String respStr) {
        Response<Integer> resp;
        try {
            resp = JsonUtils.fromJson(respStr, new TypeReference<Response<Integer>>() {
            });
        } catch (Throwable t) {
            String msg = String.format("Fail to parse resp:%s", respStr);
            log.error(msg, t);
            throw new RuntimeException(msg, t);
        }
        if (resp == null) {
            String msg = "Resp parsed is null";
            log.error(msg);
            throw new RuntimeException(msg);
        } else {
            if (resp.isSuccess()) {
                return resp.getData();
            } else {
                String msg = String.format("Resp is not success, resp:%s", resp);
                log.error(msg);
                throw new RuntimeException(msg);
            }
        }
    }
}
