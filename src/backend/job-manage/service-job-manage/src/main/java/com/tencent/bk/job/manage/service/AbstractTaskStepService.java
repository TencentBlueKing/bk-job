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

package com.tencent.bk.job.manage.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.common.consts.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskScriptSourceEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskTypeEnum;
import com.tencent.bk.job.manage.dao.TaskApprovalStepDAO;
import com.tencent.bk.job.manage.dao.TaskFileInfoDAO;
import com.tencent.bk.job.manage.dao.TaskFileStepDAO;
import com.tencent.bk.job.manage.dao.TaskScriptStepDAO;
import com.tencent.bk.job.manage.dao.TaskStepDAO;
import com.tencent.bk.job.manage.model.dto.task.TaskApprovalStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskFileInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskFileStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskScriptStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @since 16/10/2019 19:40
 */
public abstract class AbstractTaskStepService {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    protected TaskStepDAO taskStepDAO;
    protected TaskScriptStepDAO taskScriptStepDAO;
    protected TaskFileStepDAO taskFileStepDAO;
    protected TaskApprovalStepDAO taskApprovalStepDAO;
    protected TaskFileInfoDAO taskFileInfoDAO;
    protected TaskTypeEnum taskType;

    /**
     * 根据父资源 ID 拉取步骤信息列表
     *
     * @param parentId 父资源 ID，使用时应为作业模版 ID 或执行方案 ID
     * @return 步骤信息列表
     */
    public List<TaskStepDTO> listStepsByParentId(Long parentId) {
        // List step basic info
        List<TaskStepDTO> taskStepList = taskStepDAO.listStepsByParentId(parentId);
        List<Long> scriptStepIdList = new ArrayList<>();
        List<Long> fileStepIdList = new ArrayList<>();
        List<Long> approvalStepIdList = new ArrayList<>();

        taskStepList.forEach(taskStep -> {
            switch (taskStep.getType()) {
                case SCRIPT:
                    scriptStepIdList.add(taskStep.getId());
                    break;
                case FILE:
                    fileStepIdList.add(taskStep.getId());
                    break;
                case APPROVAL:
                    approvalStepIdList.add(taskStep.getId());
                    break;
                default:
                    break;
            }
        });

        // Fill detail info by type
        Map<Long, TaskScriptStepDTO> scriptStepMap = taskScriptStepDAO.listScriptStepByIds(scriptStepIdList);
        Map<Long, TaskFileStepDTO> fileStepMap = taskFileStepDAO.listFileStepsByIds(fileStepIdList);
        Map<Long, List<TaskFileInfoDTO>> fileInfoListMap = taskFileInfoDAO.listFileInfosByStepIds(fileStepIdList);
        Map<Long, TaskApprovalStepDTO> approvalStepMap = taskApprovalStepDAO.listApprovalsByIds(approvalStepIdList);

        fileStepMap.forEach((stepId, fileStep) -> fileStep.setOriginFileList(fileInfoListMap.get(stepId)));

        taskStepList.forEach(taskStep -> {
            switch (taskStep.getType()) {
                case SCRIPT:
                    taskStep.setScriptStepInfo(scriptStepMap.get(taskStep.getId()));
                    break;
                case FILE:
                    taskStep.setFileStepInfo(fileStepMap.get(taskStep.getId()));
                    break;
                case APPROVAL:
                    taskStep.setApprovalStepInfo(approvalStepMap.get(taskStep.getId()));
                    break;
                default:
                    log.error("Unrecognized step type!|{}", taskStep.getType());
                    break;
            }
        });

        return taskStepList;
    }

    /**
     * 新增步骤
     *
     * @param taskStep 步骤信息
     * @return 新增的步骤 ID
     * @throws ServiceException 新增步骤异常
     */
    @Transactional(rollbackFor = ServiceException.class)
    public long insertStep(TaskStepDTO taskStep) throws ServiceException {
        try {
            Long stepId = taskStepDAO.insertStep(taskStep);
            if (stepId > 0) {
                taskStep.setId(stepId);
                switch (taskStep.getType()) {
                    case SCRIPT:
                        TaskScriptStepDTO scriptStepInfo = taskStep.getScriptStepInfo();
                        switch (taskType) {
                            case TEMPLATE:
                                scriptStepInfo.setTemplateId(taskStep.getTemplateId());
                                break;
                            case PLAN:
                                scriptStepInfo.setPlanId(taskStep.getPlanId());
                                break;
                            default:
                                throw new InvalidParamException(ErrorCode.WRONG_TASK_TYPE);
                        }
                        scriptStepInfo.setStepId(stepId);
                        Long scriptStepId = taskScriptStepDAO.insertScriptStep(scriptStepInfo);
                        if (scriptStepId > 0) {
                            taskStep.setScriptStepId(scriptStepId);
                            scriptStepInfo.setId(scriptStepId);
                            taskStepDAO.updateStepById(taskStep);
                        } else {
                            throw new InternalException(ErrorCode.CREATE_STEP_FAILED);
                        }
                        break;
                    case FILE:
                        TaskFileStepDTO fileStepInfo = taskStep.getFileStepInfo();
                        fileStepInfo.setStepId(stepId);
                        Long fileStepId = taskFileStepDAO.insertFileStep(fileStepInfo);
                        if (fileStepId > 0) {
                            List<TaskFileInfoDTO> originFileList = fileStepInfo.getOriginFileList();
                            originFileList.forEach(fileInfo -> fileInfo.setStepId(stepId));
                            taskFileInfoDAO.batchInsertFileInfo(originFileList);

                            taskStep.setFileStepId(fileStepId);
                            fileStepInfo.setId(fileStepId);
                            taskStepDAO.updateStepById(taskStep);
                        } else {
                            throw new InternalException(ErrorCode.CREATE_STEP_FAILED);
                        }
                        break;
                    case APPROVAL:
                        TaskApprovalStepDTO approvalStepInfo = taskStep.getApprovalStepInfo();
                        approvalStepInfo.setStepId(stepId);
                        Long approvalStepId = taskApprovalStepDAO.insertApproval(approvalStepInfo);
                        if (approvalStepId > 0) {
                            taskStep.setApprovalStepId(approvalStepId);
                            approvalStepInfo.setId(approvalStepId);
                            taskStepDAO.updateStepById(taskStep);
                        } else {
                            throw new InternalException(ErrorCode.CREATE_STEP_FAILED);
                        }
                        break;
                    default:
                        log.error("Unrecognized step type!|{}", taskStep.getType());
                        break;
                }
                return stepId;
            } else {
                throw new InternalException(ErrorCode.CREATE_STEP_FAILED);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("unknown error while insert step!", e);
            throw new InternalException(ErrorCode.CREATE_STEP_FAILED);
        }
    }

    /**
     * 根据步骤 ID 更新步骤
     *
     * @param taskStep 步骤信息
     * @return 是否更新成功
     * @throws ServiceException 更新异常
     */
    @Transactional(rollbackFor = ServiceException.class)
    public boolean updateStepById(TaskStepDTO taskStep) throws ServiceException {
        try {
            if (taskStepDAO.updateStepById(taskStep)) {
                // plan step does not need update!
                if (taskStep.getTemplateStepId() != null && taskStep.getTemplateStepId() > 0) {
                    return true;
                }
                switch (taskStep.getType()) {
                    case SCRIPT:
                        if (taskStep.getScriptStepInfo().getStepId() > 0) {
                            if (taskScriptStepDAO.updateScriptStepById(taskStep.getScriptStepInfo())) {
                                return true;
                            } else {
                                throw new InternalException(ErrorCode.UPDATE_STEP_FAILED);
                            }
                        } else {
                            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
                        }
                    case FILE:
                        if (taskStep.getFileStepInfo().getStepId() > 0) {
                            if (taskFileStepDAO.updateFileStepById(taskStep.getFileStepInfo())) {
                                List<TaskFileInfoDTO> originTaskFileInfo =
                                    taskFileInfoDAO.listFileInfoByStepId(taskStep.getFileStepInfo().getStepId());
                                List<Long> originTaskFileInfoIds = originTaskFileInfo.parallelStream()
                                    .map(TaskFileInfoDTO::getId).collect(Collectors.toList());
                                List<TaskFileInfoDTO> newFileInfo = new ArrayList<>();
                                for (TaskFileInfoDTO fileInfo : taskStep.getFileStepInfo().getOriginFileList()) {
                                    if (fileInfo.getId() > 0) {
                                        if (originTaskFileInfo.contains(fileInfo)) {
                                            log.debug("Find exact same info, skip...");
                                            originTaskFileInfoIds.remove(fileInfo.getId());
                                            continue;
                                        }
                                        if (!taskFileInfoDAO.updateFileInfoById(fileInfo)) {
                                            throw new InternalException(ErrorCode.UPDATE_FILE_INFO_FAILED);
                                        }
                                        originTaskFileInfoIds.remove(fileInfo.getId());
                                    } else {
                                        newFileInfo.add(fileInfo);
                                    }
                                }
                                taskFileInfoDAO.batchInsertFileInfo(newFileInfo);
                                if (CollectionUtils.isNotEmpty(originTaskFileInfoIds)) {
                                    originTaskFileInfoIds.forEach(fileInfoId -> taskFileInfoDAO
                                        .deleteFileInfoById(taskStep.getFileStepInfo().getStepId(), fileInfoId));
                                }
                                return true;
                            } else {
                                throw new InternalException(ErrorCode.UPDATE_STEP_FAILED);
                            }
                        } else {
                            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
                        }
                    case APPROVAL:
                        if (taskStep.getApprovalStepInfo().getStepId() > 0) {
                            if (taskApprovalStepDAO.updateApprovalById(taskStep.getApprovalStepInfo())) {
                                return true;
                            } else {
                                throw new InternalException(ErrorCode.UPDATE_STEP_FAILED);
                            }
                        } else {
                            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
                        }
                    default:
                        log.error("Unrecognized step type!|{}", taskStep.getType());
                        break;
                }
                return true;
            } else {
                throw new InternalException(ErrorCode.UPDATE_STEP_FAILED);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("unknown error while update step!", e);
            throw new InternalException(ErrorCode.UPDATE_STEP_FAILED);
        }
    }

    /**
     * 根据步骤 ID 删除步骤
     *
     * @param parentId 父资源 ID，使用时应为作业模版 ID 或执行方案 ID
     * @param id       步骤 ID
     * @return 是否删除成功
     * @throws ServiceException 删除异常
     */
    @Transactional(rollbackFor = ServiceException.class)
    public boolean deleteStepById(Long parentId, Long id) throws ServiceException {
        try {
            TaskStepDTO taskStep = taskStepDAO.getStepById(parentId, id);
            if (taskStep == null) {
                return false;
            }
            if (taskStepDAO.deleteStepById(parentId, id)) {
                switch (taskStep.getType()) {
                    case SCRIPT:
                        if (taskScriptStepDAO.deleteScriptStepById(id)) {
                            return true;
                        } else {
                            throw new InternalException(ErrorCode.DELETE_STEP_FAILED);
                        }
                    case FILE:
                        if (taskFileStepDAO.deleteFileStepById(id)) {
                            taskFileInfoDAO.deleteFileInfosByStepId(id);
                            return true;
                        }
                        throw new InternalException(ErrorCode.DELETE_STEP_FAILED);
                    case APPROVAL:
                        if (taskApprovalStepDAO.deleteApprovalById(id)) {
                            return true;
                        } else {
                            throw new InternalException(ErrorCode.DELETE_STEP_FAILED);
                        }
                    default:
                        log.error("Unrecognized step type!|{}", taskStep.getType());
                        return true;
                }
            } else {
                throw new InternalException(ErrorCode.DELETE_STEP_FAILED);
            }
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("unknown error while delete step!", e);
            throw new InternalException(ErrorCode.DELETE_STEP_FAILED);
        }
    }

    /**
     * 根据作业模版 ID 列表批量拉取脚本版本 ID
     *
     * @param appId          业务 ID
     * @param templateIdList 作业模版 ID 列表
     * @return 作业模版 ID 与引用的脚本版本 ID 对应关系表
     */
    public Map<Long, List<Long>> batchListScriptStepVersionIdsByTemplateIds(Long appId, List<Long> templateIdList) {
        Map<Long, List<Long>> templateScriptVersionMap = new ConcurrentHashMap<>();
        List<TaskScriptStepDTO> taskScriptStepList = taskScriptStepDAO.batchListScriptStepIdByParentIds(templateIdList);
        Set<Long> uniqScriptVersionSet = new HashSet<>();
        if (CollectionUtils.isNotEmpty(taskScriptStepList)) {
            taskScriptStepList.forEach(taskScriptStepDTO -> {
                templateScriptVersionMap.computeIfAbsent(taskScriptStepDTO.getTemplateId(), k -> new ArrayList<>());
                templateScriptVersionMap.get(taskScriptStepDTO.getTemplateId())
                    .add(taskScriptStepDTO.getScriptVersionId());
                uniqScriptVersionSet.add(taskScriptStepDTO.getScriptVersionId());
            });
        }
        templateScriptVersionMap.put(0L, uniqScriptVersionSet.parallelStream().collect(Collectors.toList()));
        return templateScriptVersionMap;
    }

    public Integer countApprovalSteps(Long appId) {
        return taskApprovalStepDAO.countApprovalSteps(appId);
    }

    public Integer countScriptSteps(Long appId, TaskScriptSourceEnum scriptSource) {
        return taskScriptStepDAO.countScriptSteps(appId, scriptSource);
    }

    public Integer countFileSteps(Long appId, TaskFileTypeEnum fileType) {
        return taskFileStepDAO.countFileSteps(appId, fileType);
    }

    public Integer countScriptStepsByScriptIds(Long appId, List<String> scriptIdList) {
        return taskScriptStepDAO.countScriptStepsByScriptIds(appId, scriptIdList);
    }

    public List<Long> listStepIdByParentId(List<Long> parentIdList) {
        return taskStepDAO.listStepIdByParentId(parentIdList);
    }

    public List<String> listLocalFileByStepId(List<Long> stepIdList) {
        return getFileFromLocation(taskFileInfoDAO.listLocalFileByStepId(stepIdList));
    }

    protected List<String> getFileFromLocation(List<String> fileLocationList) {
        if (CollectionUtils.isEmpty(fileLocationList)) {
            return new ArrayList<>();
        }
        List<String> fileList = new ArrayList<>();
        for (String fileLocation : fileLocationList) {
            if (StringUtils.isNotBlank(fileLocation)) {
                List<String> localFileList = JsonUtils.fromJson(fileLocation, new TypeReference<List<String>>() {
                });
                if (CollectionUtils.isNotEmpty(localFileList)) {
                    fileList.addAll(localFileList);
                }
            }
        }
        return fileList;
    }
}
