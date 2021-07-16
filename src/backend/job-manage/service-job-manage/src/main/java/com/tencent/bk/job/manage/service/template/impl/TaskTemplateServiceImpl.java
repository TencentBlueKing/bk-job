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

package com.tencent.bk.job.manage.service.template.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.DataConsistencyException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.crontab.model.CronJobVO;
import com.tencent.bk.job.manage.common.consts.JobResourceStatusEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskScriptSourceEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskTemplateStatusEnum;
import com.tencent.bk.job.manage.dao.template.TaskTemplateDAO;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskScriptStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import com.tencent.bk.job.manage.model.web.vo.TagCountVO;
import com.tencent.bk.job.manage.service.AbstractTaskStepService;
import com.tencent.bk.job.manage.service.AbstractTaskVariableService;
import com.tencent.bk.job.manage.service.CronJobService;
import com.tencent.bk.job.manage.service.ScriptService;
import com.tencent.bk.job.manage.service.TagService;
import com.tencent.bk.job.manage.service.TaskFavoriteService;
import com.tencent.bk.job.manage.service.plan.TaskPlanService;
import com.tencent.bk.job.manage.service.template.TaskTemplateService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @since 16/10/2019 19:38
 */
@Slf4j
@Service("TaskTemplateServiceImpl")
public class TaskTemplateServiceImpl implements TaskTemplateService {

    private final TagService tagService;
    private AbstractTaskStepService taskStepService;
    private AbstractTaskVariableService taskVariableService;
    private TaskTemplateDAO taskTemplateDAO;
    private TemplateStatusUpdateService templateStatusUpdateService;
    private TaskFavoriteService taskFavoriteService;
    private CronJobService cronJobService;

    @Autowired
    private TaskPlanService taskPlanService;

    @Autowired
    private ScriptService scriptService;

    @Autowired
    private TaskTemplateService taskTemplateService;

    @Autowired
    public TaskTemplateServiceImpl(
        @Qualifier("TaskTemplateStepServiceImpl") AbstractTaskStepService taskStepService,
        @Qualifier("TaskTemplateVariableServiceImpl") AbstractTaskVariableService taskVariableService,
        TaskTemplateDAO taskTemplateDAO,
        TagService tagService,
        TemplateStatusUpdateService templateStatusUpdateService,
        @Qualifier("TaskTemplateFavoriteServiceImpl") TaskFavoriteService taskFavoriteService,
        CronJobService cronJobService) {
        this.taskStepService = taskStepService;
        this.taskVariableService = taskVariableService;
        this.taskTemplateDAO = taskTemplateDAO;
        this.tagService = tagService;
        this.templateStatusUpdateService = templateStatusUpdateService;
        this.taskFavoriteService = taskFavoriteService;
        this.cronJobService = cronJobService;
    }

    private static void setUpdateFlag(TaskTemplateInfoDTO templateInfo) {
        if (templateInfo != null && CollectionUtils.isNotEmpty(templateInfo.getStepList())) {
            int scriptScript = 0;
            for (TaskStepDTO taskStep : templateInfo.getStepList()) {
                if (taskStep.getScriptStepInfo() != null) {
                    scriptScript |= taskStep.getScriptStepInfo().getStatus();
                    if (scriptScript >= 0b11) {
                        break;
                    }
                }
            }
            templateInfo.setScriptStatus(scriptScript);
        }
    }

    @Override
    public PageData<TaskTemplateInfoDTO> listPageTaskTemplates(
        TaskTemplateInfoDTO templateCondition,
        BaseSearchCondition baseSearchCondition,
        List<Long> favoriteTemplateId
    ) {
        PageData<TaskTemplateInfoDTO> templateInfoPageData =
            taskTemplateDAO.listPageTaskTemplates(templateCondition, baseSearchCondition, favoriteTemplateId);
        templateInfoPageData.getData().forEach(templateInfo -> {
            templateInfo.setStepList(taskStepService.listStepsByParentId(templateInfo.getId()));
            templateInfo.setVariableList(taskVariableService.listVariablesByParentId(templateInfo.getId()));
        });
        return templateInfoPageData;
    }

    @Override
    public PageData<TaskTemplateInfoDTO> listPageTaskTemplatesBasicInfo(
        TaskTemplateInfoDTO templateCondition,
        BaseSearchCondition baseSearchCondition,
        List<Long> favoredTemplateIdList
    ) {
        int start = baseSearchCondition.getStartOrDefault(0);
        int length = baseSearchCondition.getLengthOrDefault(10);
        boolean getAll = baseSearchCondition.isGetAll();

        boolean hasFavored = false;
        List<TaskTemplateInfoDTO> favoredTemplateInfos = null;
        if (CollectionUtils.isNotEmpty(favoredTemplateIdList)) {
            favoredTemplateInfos = taskTemplateDAO.listTaskTemplateByIds(templateCondition.getAppId(),
                favoredTemplateIdList, templateCondition, baseSearchCondition);
            if (CollectionUtils.isNotEmpty(favoredTemplateInfos)) {
                hasFavored = true;
            }
        }

        if (hasFavored && CollectionUtils.isNotEmpty(favoredTemplateInfos) && !getAll) {
            if (favoredTemplateInfos.size() < start) {
                baseSearchCondition.setStart(start - favoredTemplateInfos.size());
                favoredTemplateInfos = null;
            } else {
                favoredTemplateInfos.subList(0, start).clear();
                baseSearchCondition.setStart(0);
                baseSearchCondition.setLength(length - favoredTemplateInfos.size());
            }
        }

        PageData<TaskTemplateInfoDTO> templateInfoPageData =
            taskTemplateDAO.listPageTaskTemplates(templateCondition, baseSearchCondition, favoredTemplateIdList);

        if (hasFavored && CollectionUtils.isNotEmpty(favoredTemplateInfos)) {
            templateInfoPageData.getData().addAll(0, favoredTemplateInfos);
            if (!getAll) {
                if (length < templateInfoPageData.getData().size()) {
                    templateInfoPageData.getData().subList(length, templateInfoPageData.getData().size()).clear();
                }
            }
        }

        if (!getAll) {
            templateInfoPageData.setStart(start);
            templateInfoPageData.setPageSize(length);
        }

        if (CollectionUtils.isNotEmpty(templateInfoPageData.getData())) {
            templateInfoPageData.getData().forEach(TaskTemplateServiceImpl::setUpdateFlag);
        }
        return templateInfoPageData;
    }

    @Override
    public TaskTemplateInfoDTO getTaskTemplateById(Long appId, Long templateId) {
        TaskTemplateInfoDTO templateInfo = taskTemplateDAO.getTaskTemplateById(appId, templateId);
        if (templateInfo != null) {
            templateInfo.setStepList(taskStepService.listStepsByParentId(templateInfo.getId()));
            templateInfo.setVariableList(taskVariableService.listVariablesByParentId(templateInfo.getId()));
            setUpdateFlag(templateInfo);
            return templateInfo;
        } else {
            return null;
        }
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public Long saveTaskTemplate(TaskTemplateInfoDTO taskTemplateInfo) {
        String lockKey = null;
        try {
            if (taskTemplateInfo == null) {
                throw new ServiceException(ErrorCode.MISSING_PARAM);
            }
            boolean isCreate = false;
            if (taskTemplateInfo.getId() == null || taskTemplateInfo.getId() <= 0) {
                isCreate = true;
                lockKey = "save_template:" + taskTemplateInfo.getAppId() + ":" + taskTemplateInfo.getCreator();
            } else {
                lockKey = "save_template:" + taskTemplateInfo.getAppId() + ":" + taskTemplateInfo.getId();
            }
            if (!LockUtils.tryGetDistributedLock(lockKey, JobContextUtil.getRequestId(), 60_000)) {
                throw new ServiceException(ErrorCode.TEMPLATE_LOCK_ACQUIRE_FAILED);
            }
            // 保存新增的标签并获取tagId
            taskTemplateService.processTemplateTag(taskTemplateInfo);

            // 获取引用的非线上脚本
            Map<String, Long> outdatedScriptMap = getOutdatedScriptMap(taskTemplateInfo.getStepList());
            if (MapUtils.isNotEmpty(outdatedScriptMap)) {
                taskTemplateInfo.setScriptStatus(1);
                log.debug("Find outdated script version! Comparing...|{}", outdatedScriptMap);
                if (isCreate) {
                    throw new ServiceException(ErrorCode.SCRIPT_VERSION_ILLEGAL);
                } else {
                    Map<Long, List<Long>> scriptStepVersionIds =
                        taskStepService.batchListScriptStepVersionIdsByTemplateIds(taskTemplateInfo.getAppId(),
                            Collections.singletonList(taskTemplateInfo.getId()));
                    List<Long> currentVersionIds = scriptStepVersionIds.get(taskTemplateInfo.getId());
                    if (CollectionUtils.isNotEmpty(currentVersionIds)) {
                        // 找出缺少的脚本版本Id
                        outdatedScriptMap.values().removeIf(currentVersionIds::contains);
                    }
                    if (MapUtils.isNotEmpty(outdatedScriptMap)) {
                        log.error("Script version outdated!|{}", outdatedScriptMap);
                        throw new ServiceException(ErrorCode.SCRIPT_VERSION_ILLEGAL);
                    }
                }
            } else {
                taskTemplateInfo.setScriptStatus(0);
            }

            // 写作业模板表
            // process template id
            Long templateId;
            if (isCreate) {
                taskTemplateInfo.setCreateTime(DateUtils.currentTimeSeconds());
                templateId = taskTemplateService.insertNewTemplate(taskTemplateInfo);
                taskTemplateInfo.setId(templateId);
            } else {
                boolean bumpVersion = templateHasChange(taskTemplateInfo);
                if (!taskTemplateDAO.updateTaskTemplateById(taskTemplateInfo, bumpVersion)) {
                    throw new ServiceException(ErrorCode.UPDATE_TEMPLATE_FAILED);
                }
                templateId = taskTemplateInfo.getId();
            }

            // 写步骤
            taskTemplateService.processTemplateStep(taskTemplateInfo);

            // 更新作业模板首尾步骤
            // Process first and last step id
            TaskTemplateInfoDTO updateStepIdReq = generateUpdateStepIdReq(taskTemplateInfo);
            taskTemplateDAO.updateTaskTemplateById(updateStepIdReq, false);

            // 写变量
            List<TaskVariableDTO> newVariables = new ArrayList<>();
            Iterator<TaskVariableDTO> variableIterator = taskTemplateInfo.getVariableList().iterator();
            while (variableIterator.hasNext()) {
                TaskVariableDTO taskVariable = variableIterator.next();
                taskVariable.setTemplateId(templateId);
                if (taskVariable.getDelete()) {
                    taskVariableService.deleteVariableById(taskVariable.getTemplateId(), taskVariable.getId());
                    variableIterator.remove();
                } else {
                    if (taskVariable.getId() > 0) {
                        // Update exist variable
                        taskVariableService.updateVariableById(taskVariable);
                    } else {
                        newVariables.add(taskVariable);
                    }
                }
            }
            // Insert new variable
            taskVariableService.batchInsertVariable(newVariables);

            templateStatusUpdateService.offerMessage(templateId);
            return templateId;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unknown exception while insert template!", e);
            throw new ServiceException(ErrorCode.SAVE_TEMPLATE_UNKNOWN_ERROR);
        } finally {
            if (StringUtils.isNotBlank(lockKey)) {
                LockUtils.releaseDistributedLock(lockKey, JobContextUtil.getRequestId());
            }
        }
    }

    private boolean templateHasChange(TaskTemplateInfoDTO taskTemplateInfo) {
        TaskTemplateInfoDTO originTaskTemplateInfo =
            taskTemplateService.getTaskTemplateById(taskTemplateInfo.getAppId(), taskTemplateInfo.getId());
        if (originTaskTemplateInfo == null) {
            throw new DataConsistencyException("taskTemplateId", "Detail");
        }
        if (taskTemplateInfo.getVariableList().size() == originTaskTemplateInfo.getVariableList().size()) {
            int count = 0;
            for (TaskVariableDTO taskVariable : taskTemplateInfo.getVariableList()) {
                TaskVariableDTO originTaskVariable = originTaskTemplateInfo.getVariableList().get(count);
                originTaskVariable.setDefaultValue(taskVariable.getDefaultValue());
                originTaskVariable.setTemplateId(taskVariable.getTemplateId());
                originTaskVariable.setDelete(false);
                if (!taskVariable.equals(originTaskVariable)) {
                    return true;
                }
                count++;
            }
        } else {
            return true;
        }
        if (taskTemplateInfo.getStepList().size() == originTaskTemplateInfo.getStepList().size()) {
            int count = 0;
            for (TaskStepDTO taskStep : taskTemplateInfo.getStepList()) {
                if (taskStep.getId() == null || taskStep.getId() <= 0 || taskStep.getDelete() == 1) {
                    return true;
                }
                TaskStepDTO originTaskStep = originTaskTemplateInfo.getStepList().get(count);
                if (!taskStep.getId().equals(originTaskStep.getId())) {
                    return true;
                }
                if (!taskStep.getType().equals(originTaskStep.getType())) {
                    return true;
                }
                if (!taskStep.getName().equals(originTaskStep.getName())) {
                    return true;
                }
                switch (taskStep.getType()) {
                    case SCRIPT:
                        originTaskStep.getScriptStepInfo().setId(null);
                        originTaskStep.getScriptStepInfo().setTemplateId(taskStep.getScriptStepInfo().getTemplateId());
                        if (!taskStep.getScriptStepInfo().equals(originTaskStep.getScriptStepInfo())) {
                            return true;
                        }
                        break;
                    case FILE:
                        originTaskStep.getFileStepInfo().setId(null);
                        if (!taskStep.getFileStepInfo().equals(originTaskStep.getFileStepInfo())) {
                            return true;
                        }
                        break;
                    case APPROVAL:
                        originTaskStep.getApprovalStepInfo().setId(null);
                        if (!taskStep.getApprovalStepInfo().equals(originTaskStep.getApprovalStepInfo())) {
                            return true;
                        }
                        break;
                    default:
                        return true;
                }
                count++;
            }
        } else {
            return true;
        }
        return false;
    }

    @Override
    public void processTemplateTag(TaskTemplateInfoDTO taskTemplateInfo) {
        try {
            List<TagDTO> tags = taskTemplateInfo.getTags();
            if (tags != null && !tags.isEmpty()) {
                List<TagDTO> newTags = tagService.createNewTagIfNotExist(tags, taskTemplateInfo.getAppId(),
                    taskTemplateInfo.getLastModifyUser());
                taskTemplateInfo.setTags(newTags);
            }
        } catch (Exception e) {
            log.error("Error while process template tag!|{}", taskTemplateInfo, e);
            throw new ServiceException(ErrorCode.PROCESS_TAG_FAILED);
        }
    }

    @Override
    @Transactional
    public Boolean deleteTaskTemplate(Long appId, Long templateId) {
        List<TaskPlanInfoDTO> taskPlanInfoList = taskPlanService.listTaskPlansBasicInfo(appId, templateId);
        if (CollectionUtils.isNotEmpty(taskPlanInfoList)) {
            List<Long> taskPlanIdList =
                taskPlanInfoList.parallelStream().map(TaskPlanInfoDTO::getId).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(taskPlanIdList)) {
                Map<Long, List<CronJobVO>> taskPlanCronJobMap =
                    cronJobService.batchListCronJobByPlanIds(appId, taskPlanIdList);
                if (MapUtils.isNotEmpty(taskPlanCronJobMap)) {
                    for (List<CronJobVO> planCronJobList : taskPlanCronJobMap.values()) {
                        if (CollectionUtils.isNotEmpty(planCronJobList)) {
                            throw new ServiceException(ErrorCode.DELETE_TEMPLATE_FAILED_PLAN_USING_BY_CRON);
                        }
                    }
                }
            }
        }
        taskPlanService.deleteTaskPlanByTemplate(appId, templateId);
        return taskTemplateDAO.deleteTaskTemplateById(appId, templateId);
    }

    @Override
    public TagCountVO getTagTemplateCount(Long appId) {
        Map<Long, Long> templateTagCount = taskTemplateDAO.getTemplateTagCount(appId);
        TagCountVO tagCount = new TagCountVO();
        tagCount.setTagCount(templateTagCount);
        tagCount.setTotal(taskTemplateDAO.getAllTemplateCount(appId));
        tagCount.setUnclassified(taskTemplateDAO.getUnclassifiedTemplateCount(appId));
        tagCount.setNeedUpdate(taskTemplateDAO.getNeedUpdateTemplateCount(appId));
        return tagCount;
    }

    @Override
    public Boolean saveTaskTemplateBasicInfo(TaskTemplateInfoDTO taskTemplateInfo) {
        processTemplateTag(taskTemplateInfo);
        if (!taskTemplateDAO.updateTaskTemplateById(taskTemplateInfo, false)) {
            throw new ServiceException(ErrorCode.UPDATE_TEMPLATE_FAILED);
        }
        return true;
    }

    @Override
    public Map<Long, List<Long>> listTemplateScriptVersionInfo(Long appId, List<Long> templateIdList) {
        return taskStepService.batchListScriptStepVersionIdsByTemplateIds(appId, templateIdList);
    }

    @Override
    public TaskTemplateInfoDTO getTaskTemplateBasicInfoById(Long appId, Long templateId) {
        return taskTemplateDAO.getTaskTemplateById(appId, templateId);
    }

    @Override
    public TaskTemplateInfoDTO getTaskTemplateBasicInfoById(Long templateId) {
        return taskTemplateDAO.getTaskTemplateById(templateId);
    }

    @Override
    public TaskTemplateInfoDTO getDeletedTaskTemplateBasicInfoById(Long templateId) {
        return taskTemplateDAO.getDeletedTaskTemplateById(templateId);
    }

    @Override
    public List<TaskTemplateInfoDTO> listTaskTemplateBasicInfoByIds(Long appId, List<Long> templateIdList) {
        return taskTemplateDAO.listTaskTemplateByIds(appId, templateIdList, null, null);
    }

    @Override
    public boolean checkTemplateName(Long appId, Long templateId, String name) {
        if (templateId == null || templateId < 0) {
            return false;
        }
        name = name.trim();
        if (StringUtils.isEmpty(name)) {
            return false;
        }
        return taskTemplateDAO.checkTemplateName(appId, templateId, name);
    }

    @Override
    public boolean updateScriptStatus(Long appId, String scriptId, Long scriptVersionId, JobResourceStatusEnum status) {
        try {
            return templateStatusUpdateService.offerMessage(scriptId, scriptVersionId, status);
        } catch (InterruptedException e) {
            log.error("Offer message failed! Maybe queue full!", e);
        }
        return false;
    }

    @Override
    public List<TaskTemplateInfoDTO> getFavoredTemplateBasicInfo(Long appId, String username) {
        List<Long> favoredTemplateList = taskFavoriteService.listFavorites(appId, username);
        TaskTemplateInfoDTO taskTemplateInfoDTO = new TaskTemplateInfoDTO();
        taskTemplateInfoDTO.setAppId(appId);
        return taskTemplateDAO.listTaskTemplateByIds(appId, favoredTemplateList, taskTemplateInfoDTO,
            new BaseSearchCondition());
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public Long saveTaskTemplateForMigration(
        TaskTemplateInfoDTO taskTemplateInfo,
        Long createTime,
        Long lastModifyTime,
        String lastModifyUser
    ) {
        try {
            if (taskTemplateInfo == null) {
                throw new ServiceException(ErrorCode.MISSING_PARAM);
            }
            TaskTemplateInfoDTO taskTemplateByName =
                taskTemplateDAO.getTaskTemplateByName(taskTemplateInfo.getAppId(), taskTemplateInfo.getName());
            if (taskTemplateByName != null) {
                throw new ServiceException(ErrorCode.TEMPLATE_NAME_EXIST);
            }
            taskTemplateService.processTemplateTag(taskTemplateInfo);

            if (createTime != null && createTime > 0) {
                taskTemplateInfo.setCreateTime(createTime);
            } else {
                taskTemplateInfo.setCreateTime(DateUtils.currentTimeSeconds());
            }

            if (lastModifyTime != null && lastModifyTime > 0) {
                taskTemplateInfo.setLastModifyTime(lastModifyTime);
            }
            if (StringUtils.isNotBlank(lastModifyUser)) {
                taskTemplateInfo.setLastModifyUser(lastModifyUser);
            }

            if (taskTemplateInfo.getFirstStepId() == null) {
                taskTemplateInfo.setFirstStepId(0L);
            }
            if (taskTemplateInfo.getLastStepId() == null) {
                taskTemplateInfo.setLastStepId(0L);
            }

            // process template id
            Long templateId;
            if (taskTemplateInfo.getId() == null || taskTemplateInfo.getId() <= 0) {
                templateId = taskTemplateService.insertNewTemplate(taskTemplateInfo);
                taskTemplateInfo.setId(templateId);
            } else {
                taskTemplateInfo.setStatus(TaskTemplateStatusEnum.NEW);
                if (taskTemplateService.checkTemplateId(taskTemplateInfo.getId())) {
                    if (taskTemplateDAO.insertTaskTemplateWithId(taskTemplateInfo)) {
                        templateId = taskTemplateInfo.getId();
                    } else {
                        throw new ServiceException(ErrorCode.INSERT_TEMPLATE_FAILED);
                    }
                } else {
                    throw new ServiceException(ErrorCode.TEMPLATE_ID_EXIST);
                }
            }

            taskTemplateService.processTemplateStep(taskTemplateInfo);

            // Process first and last step id
            TaskTemplateInfoDTO updateStepIdReq = generateUpdateStepIdReq(taskTemplateInfo);
            taskTemplateDAO.updateTaskTemplateById(updateStepIdReq, true);

            // Insert new variable
            List<TaskVariableDTO> variableList = taskTemplateInfo.getVariableList();
            variableList.forEach(taskVariableDTO -> {
                taskVariableDTO.setTemplateId(templateId);
                if (taskVariableDTO.getId() <= 0) {
                    taskVariableDTO.setId(null);
                }
            });
            taskVariableService.batchInsertVariableWithId(variableList);

            return templateId;
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unknown exception while insert template!", e);
            throw new ServiceException(ErrorCode.SAVE_TEMPLATE_UNKNOWN_ERROR);
        }
    }

    private TaskTemplateInfoDTO generateUpdateStepIdReq(TaskTemplateInfoDTO taskTemplateInfo) {
        TaskTemplateInfoDTO updateStepIdReq = new TaskTemplateInfoDTO();
        updateStepIdReq.setId(taskTemplateInfo.getId());
        updateStepIdReq.setAppId(taskTemplateInfo.getAppId());
        updateStepIdReq.setLastModifyUser(taskTemplateInfo.getLastModifyUser());
        updateStepIdReq.setLastModifyTime(taskTemplateInfo.getLastModifyTime());
        updateStepIdReq.setFirstStepId(taskTemplateInfo.getFirstStepId());
        updateStepIdReq.setLastStepId(taskTemplateInfo.getLastStepId());
        return updateStepIdReq;
    }

    @Override
    public void processTemplateStep(TaskTemplateInfoDTO taskTemplateInfo) {
        // Save step
        Long firstStepId = 0L;
        Long lastStepId = 0L;
        TaskStepDTO previousStep = null;

        Iterator<TaskStepDTO> stepIterator = taskTemplateInfo.getStepList().iterator();
        while (stepIterator.hasNext()) {
            TaskStepDTO taskStep = stepIterator.next();
            taskStep.setTemplateId(taskTemplateInfo.getId());
            if (taskStep.getDelete() == 1) {
                taskStepService.deleteStepById(taskStep.getTemplateId(), taskStep.getId());
                stepIterator.remove();
            } else {
                // Update previous step info on current step
                if (previousStep == null) {
                    taskStep.setPreviousStepId(0L);
                } else {
                    taskStep.setPreviousStepId(previousStep.getId());
                }

                // Insert 0 first and wait for update
                taskStep.setNextStepId(0L);

                // new step, insert to get id
                if (taskStep.getId() == null || taskStep.getId() <= 0) {
                    taskStep.setId(taskStepService.insertStep(taskStep));
                }

                // update previous step to point to current step
                if (previousStep != null) {
                    previousStep.setNextStepId(taskStep.getId());
                    taskStepService.updateStepById(previousStep);
                }

                // make previous current
                previousStep = taskStep;
                if (firstStepId == 0) {
                    firstStepId = taskStep.getId();
                }
                lastStepId = taskStep.getId();
            }
        }

        // update the final step
        if (previousStep != null) {
            previousStep.setNextStepId(0L);
            taskStepService.updateStepById(previousStep);
        }

        taskTemplateInfo.setFirstStepId(firstStepId);
        taskTemplateInfo.setLastStepId(lastStepId);
    }

    @Override
    public Long insertNewTemplate(TaskTemplateInfoDTO taskTemplateInfo) {
        taskTemplateInfo.setFirstStepId(0L);
        taskTemplateInfo.setLastStepId(0L);
        Long templateId = taskTemplateDAO.insertTaskTemplate(taskTemplateInfo);
        if (templateId == null) {
            throw new ServiceException(ErrorCode.INSERT_TEMPLATE_FAILED);
        }
        return templateId;
    }

    @Override
    public String getTemplateName(long templateId) {
        return taskTemplateDAO.getTemplateName(templateId);
    }

    @Override
    public TaskTemplateInfoDTO getTemplateById(long templateId) {
        return taskTemplateDAO.getTemplateById(templateId);
    }

    @Override
    public boolean checkTemplateId(Long templateId) {
        if (templateId == null || templateId < 0) {
            return false;
        }
        if (templateId == 0) {
            return true;
        }
        return taskTemplateDAO.checkTemplateId(templateId);
    }

    @Override
    public boolean isExistAnyAppTemplate(Long appId) {
        return taskTemplateDAO.isExistAnyAppTemplate(appId);
    }

    @Override
    public Integer countTemplates(Long appId) {
        return taskTemplateDAO.countTemplates(appId);
    }

    @Override
    public Integer countTemplateSteps(
        Long appId,
        TaskStepTypeEnum taskStepType,
        TaskScriptSourceEnum scriptSource,
        TaskFileTypeEnum fileType
    ) {
        if (TaskStepTypeEnum.SCRIPT == taskStepType) {
            return taskStepService.countScriptSteps(appId, scriptSource);
        } else if (TaskStepTypeEnum.FILE == taskStepType) {
            return taskStepService.countFileSteps(appId, fileType);
        } else if (TaskStepTypeEnum.APPROVAL == taskStepType) {
            return taskStepService.countApprovalSteps(appId);
        } else {
            throw new ServiceException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, new String[]{taskStepType.name()});
        }
    }

    @Override
    public Integer countByTag(Long appId, Long tagId) {
        return taskTemplateDAO.countByTag(appId, tagId);
    }

    @Override
    public Integer countCiteScriptSteps(Long appId, List<String> scriptIdList) {
        return taskStepService.countScriptStepsByScriptIds(appId, scriptIdList);
    }

    @Override
    public Set<String> listLocalFiles() {
        List<Long> templateIdList = taskTemplateDAO.listAllTemplateId();
        if (CollectionUtils.isEmpty(templateIdList)) {
            return new HashSet<>();
        }
        List<Long> stepIdList = taskStepService.listStepIdByParentId(templateIdList);
        if (CollectionUtils.isEmpty(stepIdList)) {
            return new HashSet<>();
        }
        List<String> localFileList = taskStepService.listLocalFileByStepId(stepIdList);
        if (CollectionUtils.isEmpty(localFileList)) {
            return new HashSet<>();
        }
        return new HashSet<>(localFileList);
    }

    private Map<String, Long> getOutdatedScriptMap(List<TaskStepDTO> stepList) {
        Map<String, Long> outdatedScriptMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(stepList)) {
            Map<String, Long> scriptVersionMap = new HashMap<>(stepList.size());
            for (TaskStepDTO taskStep : stepList) {
                if (taskStep.getDelete() == 1) {
                    continue;
                }
                if (taskStep.getType() == TaskStepTypeEnum.SCRIPT) {
                    TaskScriptStepDTO scriptStepInfo = taskStep.getScriptStepInfo();
                    if (scriptStepInfo.getScriptSource() == TaskScriptSourceEnum.CITING
                        || scriptStepInfo.getScriptSource() == TaskScriptSourceEnum.PUBLIC) {
                        scriptVersionMap.put(scriptStepInfo.getScriptId(), scriptStepInfo.getScriptVersionId());
                    }
                }
            }
            if (MapUtils.isNotEmpty(scriptVersionMap)) {
                Map<String, ScriptDTO> scriptInfoMap = null;
                try {
                    scriptInfoMap = scriptService
                        .batchGetOnlineScriptVersionByScriptIds(new ArrayList<>(scriptVersionMap.keySet()));
                } catch (ServiceException e) {
                    log.error("Error while getting online script version!", e);
                    return outdatedScriptMap;
                }
                if (MapUtils.isNotEmpty(scriptInfoMap)) {
                    for (Map.Entry<String, ScriptDTO> scriptInfoEntry : scriptInfoMap.entrySet()) {
                        Long submitVersion = scriptVersionMap.get(scriptInfoEntry.getKey());
                        if (submitVersion != null) {
                            if (!submitVersion.equals(scriptInfoEntry.getValue().getScriptVersionId())) {
                                outdatedScriptMap.put(scriptInfoEntry.getKey(), submitVersion);
                            }
                        }
                    }
                } else {
                    outdatedScriptMap = scriptVersionMap;
                }
            }
        }
        return outdatedScriptMap;
    }

    public void setScriptService(ScriptService scriptService) {
        this.scriptService = scriptService;
    }
}
