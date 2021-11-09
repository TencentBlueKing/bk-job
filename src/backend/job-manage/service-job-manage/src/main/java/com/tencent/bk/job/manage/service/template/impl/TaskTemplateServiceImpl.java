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
import com.tencent.bk.job.common.constant.JobResourceTypeEnum;
import com.tencent.bk.job.common.exception.AbortedException;
import com.tencent.bk.job.common.exception.AlreadyExistsException;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
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
import com.tencent.bk.job.manage.model.dto.ResourceTagDTO;
import com.tencent.bk.job.manage.model.dto.ScriptDTO;
import com.tencent.bk.job.manage.model.dto.TagDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskScriptStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import com.tencent.bk.job.manage.model.query.TaskTemplateQuery;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service("TaskTemplateServiceImpl")
public class TaskTemplateServiceImpl implements TaskTemplateService {

    private final TagService tagService;
    private final AbstractTaskStepService taskStepService;
    private final AbstractTaskVariableService taskVariableService;
    private final TaskTemplateDAO taskTemplateDAO;
    private final TemplateStatusUpdateService templateStatusUpdateService;
    private final TaskFavoriteService taskFavoriteService;
    private final CronJobService cronJobService;

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
    public PageData<TaskTemplateInfoDTO> listPageTaskTemplates(TaskTemplateQuery query) {

        PageData<TaskTemplateInfoDTO> templateInfoPageData =
            taskTemplateDAO.listPageTaskTemplates(query);
        templateInfoPageData.getData().forEach(templateInfo -> {
            templateInfo.setStepList(taskStepService.listStepsByParentId(templateInfo.getId()));
            templateInfo.setVariableList(taskVariableService.listVariablesByParentId(templateInfo.getId()));
        });
        setTags(query.getAppId(), templateInfoPageData.getData());
        return templateInfoPageData;
    }

    private void setTags(Long appId, List<TaskTemplateInfoDTO> templateInfoList) {
        List<String> templateIds = templateInfoList.stream().map(template -> String.valueOf(template.getId()))
            .collect(Collectors.toList());
        List<ResourceTagDTO> resourceTags = tagService.listResourceTagsByResourceTypeAndResourceIds(appId,
            JobResourceTypeEnum.TEMPLATE.getValue(), templateIds);

        Map<Long, List<ResourceTagDTO>> templateTags = new HashMap<>();
        resourceTags.forEach(resourceTag -> {
            Long templateId = Long.parseLong(resourceTag.getResourceId());
            templateTags.computeIfAbsent(templateId, k -> new ArrayList<>());
            templateTags.get(templateId).add(resourceTag);
        });

        templateInfoList.forEach(template -> {
            List<ResourceTagDTO> tags = templateTags.get(template.getId());
            if (CollectionUtils.isNotEmpty(tags)) {
                template.setTags(buildTags(tags));
            }
        });
    }

    private List<TagDTO> buildTags(List<ResourceTagDTO> resourceTags) {
        return resourceTags.stream().map(ResourceTagDTO::getTag).filter(Objects::nonNull)
            .sorted(Comparator.comparing(TagDTO::getName)).collect(Collectors.toList());
    }

    @Override
    public PageData<TaskTemplateInfoDTO> listPageTaskTemplatesBasicInfo(TaskTemplateQuery query,
                                                                        List<Long> favoredTemplateIdList) {
        if (query.isExistIdCondition()) {
            return getTemplateById(query);
        }

        BaseSearchCondition baseSearchCondition = query.getBaseSearchCondition();
        int start = baseSearchCondition.getStartOrDefault(0);
        int length = baseSearchCondition.getLengthOrDefault(10);
        boolean getAll = baseSearchCondition.isGetAll();

        if (query.isExistTagCondition()) {
            List<Long> tagMatchedTemplateIds = queryTemplateIdsByTags(query);
            if (CollectionUtils.isEmpty(tagMatchedTemplateIds)) {
                // none match, return empty page data
                return PageData.emptyPageData(start, length);
            } else {
                query.setIds(tagMatchedTemplateIds);
            }
        }

        boolean existAnyMatchedFavoredTemplate;
        List<TaskTemplateInfoDTO> matchedFavoredTemplates = null;
        if (CollectionUtils.isNotEmpty(favoredTemplateIdList)) {
            matchedFavoredTemplates = queryFavoredTemplates(query, favoredTemplateIdList);
            query.setExcludeTemplateIds(favoredTemplateIdList);
        }
        existAnyMatchedFavoredTemplate = CollectionUtils.isNotEmpty(matchedFavoredTemplates);
        if (existAnyMatchedFavoredTemplate && !getAll) {
            resetPageConditionWhenExistFavoredTemplate(baseSearchCondition, start, length, matchedFavoredTemplates);
        }


        PageData<TaskTemplateInfoDTO> templatePageData = taskTemplateDAO.listPageTaskTemplates(query);

        rebuildTemplatePageData(templatePageData, matchedFavoredTemplates, getAll, existAnyMatchedFavoredTemplate,
            start, length);
        setAdditionalAttributesForTemplates(query.getAppId(), templatePageData);

        return templatePageData;
    }

    private PageData<TaskTemplateInfoDTO> getTemplateById(TaskTemplateQuery query) {
        PageData<TaskTemplateInfoDTO> templatePageData = taskTemplateDAO.listPageTaskTemplates(query);
        setAdditionalAttributesForTemplates(query.getAppId(), templatePageData);
        return templatePageData;
    }

    private void setAdditionalAttributesForTemplates(Long appId, PageData<TaskTemplateInfoDTO> templatePageData) {
        if (CollectionUtils.isNotEmpty(templatePageData.getData())) {
            templatePageData.getData().forEach(TaskTemplateServiceImpl::setUpdateFlag);
        }

        setTags(appId, templatePageData.getData());
    }

    private List<TaskTemplateInfoDTO> queryFavoredTemplates(TaskTemplateQuery query,
                                                            List<Long> favoredTemplateIdList) {
        List<TaskTemplateInfoDTO> matchedFavoredTemplates = null;
        if (CollectionUtils.isNotEmpty(favoredTemplateIdList)) {
            TaskTemplateQuery favoredTemplateQuery = query.clone();
            favoredTemplateQuery.setExcludeTemplateIds(null);
            if (CollectionUtils.isNotEmpty(favoredTemplateQuery.getIds())) {
                // remain common template ids
                favoredTemplateQuery.getIds().retainAll(favoredTemplateIdList);
            } else {
                favoredTemplateQuery.setIds(favoredTemplateIdList);
            }
            if (CollectionUtils.isNotEmpty(favoredTemplateQuery.getIds())) {
                matchedFavoredTemplates = taskTemplateDAO.listTaskTemplates(favoredTemplateQuery);
            }
        }
        return matchedFavoredTemplates;
    }

    private void resetPageConditionWhenExistFavoredTemplate(BaseSearchCondition baseSearchCondition,
                                                            Integer start,
                                                            Integer length,
                                                            List<TaskTemplateInfoDTO> matchedFavoredTemplates) {
        if (matchedFavoredTemplates.size() <= start) {
            baseSearchCondition.setStart(start - matchedFavoredTemplates.size());
        } else {
            baseSearchCondition.setStart(0);
            baseSearchCondition.setLength(Math.max(1, start + length - matchedFavoredTemplates.size()));
        }
    }

    private void rebuildTemplatePageData(PageData<TaskTemplateInfoDTO> templatePageData,
                                         List<TaskTemplateInfoDTO> matchedFavoredTemplates,
                                         boolean getAll,
                                         boolean existAnyMatchedFavoredTemplate,
                                         Integer start,
                                         Integer length) {
        if (existAnyMatchedFavoredTemplate) {
            if (getAll) {
                templatePageData.getData().addAll(0, matchedFavoredTemplates);
            } else {
                putFavoredTemplateInFrontIfExist(templatePageData, matchedFavoredTemplates, start, length);
            }
        }

        if (!getAll) {
            templatePageData.setStart(start);
            templatePageData.setPageSize(length);
            if (existAnyMatchedFavoredTemplate) {
                templatePageData.setTotal(matchedFavoredTemplates.size() + templatePageData.getTotal());
            }
        }
    }

    private void putFavoredTemplateInFrontIfExist(PageData<TaskTemplateInfoDTO> templatePageData,
                                         List<TaskTemplateInfoDTO> matchedFavoredTemplates,
                                         Integer start,
                                         Integer length) {
        // 前置的模板
        if (CollectionUtils.isNotEmpty(matchedFavoredTemplates)) {
            if (matchedFavoredTemplates.size() > start) {
                templatePageData.getData().addAll(0, matchedFavoredTemplates.stream().skip(start).limit(length)
                    .collect(Collectors.toList()));
            }
        }

        // subList
        if (templatePageData.getData().size() > length) {
            List<TaskTemplateInfoDTO> templates = new ArrayList<>(templatePageData.getData().subList(0, length));
            templatePageData.setData(templates);
        }
    }

    private List<Long> queryTemplateIdsByTags(TaskTemplateQuery query) {
        List<Long> matchTemplateIds = new ArrayList<>();
        if (query.isUntaggedTemplate()) {
            // untagged template
            List<Long> taggedTemplateIds = tagService.listAppTaggedResourceIds(query.getAppId(),
                JobResourceTypeEnum.TEMPLATE.getValue()).stream().map(Long::valueOf)
                .collect(Collectors.toList());
            matchTemplateIds.addAll(taskTemplateDAO.listAllAppTemplateId(query.getAppId()));
            matchTemplateIds.removeAll(taggedTemplateIds);
        } else if (CollectionUtils.isNotEmpty(query.getTags())) {
            List<Long> tagIds = query.getTags().stream().distinct().map(TagDTO::getId).collect(Collectors.toList());
            matchTemplateIds = tagService.listResourceIdsWithAllTagIds(JobResourceTypeEnum.TEMPLATE.getValue(),
                tagIds).stream().map(Long::valueOf).collect(Collectors.toList());
        }
        return matchTemplateIds;
    }

    @Override
    public TaskTemplateInfoDTO getTaskTemplateById(Long appId, Long templateId) {
        TaskTemplateInfoDTO templateInfo = taskTemplateDAO.getTaskTemplateById(appId, templateId);
        if (templateInfo != null) {
            setTags(appId, Collections.singletonList(templateInfo));
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
            boolean isCreate = false;
            if (taskTemplateInfo.getId() == null || taskTemplateInfo.getId() <= 0) {
                isCreate = true;
                lockKey = "save_template:" + taskTemplateInfo.getAppId() + ":" + taskTemplateInfo.getCreator();
            } else {
                lockKey = "save_template:" + taskTemplateInfo.getAppId() + ":" + taskTemplateInfo.getId();
            }
            if (!LockUtils.tryGetDistributedLock(lockKey, JobContextUtil.getRequestId(), 60_000)) {
                throw new AbortedException(ErrorCode.TEMPLATE_LOCK_ACQUIRE_FAILED);
            }
            // 保存新增的标签并获取tagId
            taskTemplateService.createNewTagForTemplateIfNotExist(taskTemplateInfo);

            // 获取引用的非线上脚本
            Map<String, Long> outdatedScriptMap = getOutdatedScriptMap(taskTemplateInfo.getStepList());
            if (MapUtils.isNotEmpty(outdatedScriptMap)) {
                taskTemplateInfo.setScriptStatus(1);
                log.debug("Find outdated script version! Comparing...|{}", outdatedScriptMap);
                if (isCreate) {
                    throw new FailedPreconditionException(ErrorCode.SCRIPT_VERSION_ILLEGAL);
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
                        throw new FailedPreconditionException(ErrorCode.SCRIPT_VERSION_ILLEGAL);
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
                    throw new InternalException(ErrorCode.UPDATE_TEMPLATE_FAILED);
                }
                templateId = taskTemplateInfo.getId();
            }

            updateTemplateTags(taskTemplateInfo);

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
            throw new InternalException(ErrorCode.SAVE_TEMPLATE_UNKNOWN_ERROR);
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
            throw new NotFoundException(ErrorCode.TEMPLATE_NOT_EXIST);
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
    public void createNewTagForTemplateIfNotExist(TaskTemplateInfoDTO taskTemplateInfo) {
        List<TagDTO> tags = taskTemplateInfo.getTags();
        if (tags != null && !tags.isEmpty()) {
            List<TagDTO> newTags = tagService.createNewTagIfNotExist(tags, taskTemplateInfo.getAppId(),
                taskTemplateInfo.getLastModifyUser());
            taskTemplateInfo.setTags(newTags);
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
                            throw new FailedPreconditionException(ErrorCode.DELETE_TEMPLATE_FAILED_PLAN_USING_BY_CRON);
                        }
                    }
                }
            }
        }
        taskPlanService.deleteTaskPlanByTemplate(appId, templateId);
        taskTemplateDAO.deleteTaskTemplateById(appId, templateId);
        tagService.batchDeleteResourceTags(appId, JobResourceTypeEnum.TEMPLATE.getValue(), String.valueOf(templateId));
        return true;
    }

    @Override
    public TagCountVO getTagTemplateCount(Long appId) {
        TagCountVO tagCount = new TagCountVO();

        List<String> appTemplateIds = taskTemplateDAO.listAllAppTemplateId(appId)
            .stream().map(String::valueOf).collect(Collectors.toList());
        tagCount.setTotal((long) appTemplateIds.size());

        List<ResourceTagDTO> tags = tagService.listResourceTagsByResourceTypeAndResourceIds(appId,
            JobResourceTypeEnum.TEMPLATE.getValue(), appTemplateIds);
        Map<Long, Long> templateTagCount = tagService.countResourcesByTag(tags);
        tagCount.setTagCount(templateTagCount);

        long taggedTemplateCount = tags.stream()
            .map(tag -> Long.valueOf(tag.getResourceId())).distinct().count();
        tagCount.setUnclassified(appTemplateIds.size() - taggedTemplateCount);

        tagCount.setNeedUpdate(taskTemplateDAO.getNeedUpdateTemplateCount(appId));
        return tagCount;
    }

    @Override
    public Boolean saveTaskTemplateBasicInfo(TaskTemplateInfoDTO taskTemplateInfo) {
        createNewTagForTemplateIfNotExist(taskTemplateInfo);
        updateTemplateTags(taskTemplateInfo);
        if (!taskTemplateDAO.updateTaskTemplateById(taskTemplateInfo, false)) {
            throw new InternalException(ErrorCode.UPDATE_TEMPLATE_FAILED);
        }
        return true;
    }

    private void updateTemplateTags(TaskTemplateInfoDTO taskTemplateInfo) {
        tagService.patchResourceTags(JobResourceTypeEnum.TEMPLATE.getValue(), String.valueOf(taskTemplateInfo.getId()),
            taskTemplateInfo.getTags() == null ? Collections.emptyList() :
                taskTemplateInfo.getTags().stream().map(TagDTO::getId).distinct().collect(Collectors.toList()));
    }

    @Override
    public TaskTemplateInfoDTO getTaskTemplateBasicInfoById(Long appId, Long templateId) {
        TaskTemplateInfoDTO template = taskTemplateDAO.getTaskTemplateById(appId, templateId);
        setTags(appId, Collections.singletonList(template));
        return template;
    }

    @Override
    public TaskTemplateInfoDTO getTaskTemplateBasicInfoById(Long templateId) {
        return taskTemplateDAO.getTaskTemplateById(templateId);
    }

    @Override
    public List<TaskTemplateInfoDTO> listTaskTemplateBasicInfoByIds(Long appId, List<Long> templateIdList) {
        TaskTemplateQuery query = TaskTemplateQuery.builder().appId(appId).ids(templateIdList).build();
        List<TaskTemplateInfoDTO> templates = taskTemplateDAO.listTaskTemplates(query);
        setTags(appId, templates);
        return templates;
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
        TaskTemplateQuery query = TaskTemplateQuery.builder().appId(appId).ids(favoredTemplateList)
            .baseSearchCondition(new BaseSearchCondition()).build();
        List<TaskTemplateInfoDTO> templates = taskTemplateDAO.listTaskTemplates(query);
        setTags(appId, templates);
        return templates;
    }

    @Override
    @Transactional(rollbackFor = Throwable.class)
    public Long saveTaskTemplateForMigration(
        TaskTemplateInfoDTO taskTemplateInfo,
        Long createTime,
        Long lastModifyTime,
        String lastModifyUser
    ) {
        TaskTemplateInfoDTO taskTemplateByName =
            taskTemplateDAO.getTaskTemplateByName(taskTemplateInfo.getAppId(), taskTemplateInfo.getName());
        if (taskTemplateByName != null) {
            throw new NotFoundException(ErrorCode.TEMPLATE_NAME_EXIST);
        }
        taskTemplateService.createNewTagForTemplateIfNotExist(taskTemplateInfo);

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
                if (insertNewTemplateWithTemplateId(taskTemplateInfo)) {
                    templateId = taskTemplateInfo.getId();
                } else {
                    throw new InternalException(ErrorCode.INSERT_TEMPLATE_FAILED);
                }
            } else {
                throw new AlreadyExistsException(ErrorCode.TEMPLATE_ID_EXIST);
            }
        }

        updateTemplateTags(taskTemplateInfo);

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
            throw new InternalException(ErrorCode.INSERT_TEMPLATE_FAILED);
        }
        if (CollectionUtils.isNotEmpty(taskTemplateInfo.getTags())) {
            List<ResourceTagDTO> tags =
                taskTemplateInfo.getTags().stream()
                    .map(tag -> new ResourceTagDTO(JobResourceTypeEnum.TEMPLATE.getValue(),
                        String.valueOf(templateId), tag.getId())).collect(Collectors.toList());
            tagService.batchSaveResourceTags(tags);
        }
        return templateId;
    }

    @Override
    public boolean insertNewTemplateWithTemplateId(TaskTemplateInfoDTO taskTemplateInfo) {
        taskTemplateInfo.setFirstStepId(0L);
        taskTemplateInfo.setLastStepId(0L);
        boolean isSuccess = taskTemplateDAO.insertTaskTemplateWithId(taskTemplateInfo);
        if (!isSuccess) {
            throw new InternalException(ErrorCode.INSERT_TEMPLATE_FAILED);
        }
        if (CollectionUtils.isNotEmpty(taskTemplateInfo.getTags())) {
            List<ResourceTagDTO> tags =
                taskTemplateInfo.getTags().stream()
                    .map(tag -> new ResourceTagDTO(JobResourceTypeEnum.TEMPLATE.getValue(),
                        String.valueOf(taskTemplateInfo.getId()), tag.getId())).collect(Collectors.toList());
            tagService.batchSaveResourceTags(tags);
        }
        return true;
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
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, new String[]{taskStepType.name()});
        }
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
                Map<String, ScriptDTO> scriptInfoMap;
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
