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

package com.tencent.bk.job.analysis.task.analysis.task.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.analysis.dao.AnalysisTaskDAO;
import com.tencent.bk.job.analysis.dao.AnalysisTaskInstanceDAO;
import com.tencent.bk.job.analysis.model.dto.AnalysisTaskDTO;
import com.tencent.bk.job.analysis.model.dto.AnalysisTaskInstanceDTO;
import com.tencent.bk.job.analysis.model.inner.AnalysisTaskResultItemLocation;
import com.tencent.bk.job.analysis.service.ApplicationService;
import com.tencent.bk.job.analysis.service.ScriptService;
import com.tencent.bk.job.analysis.service.TaskPlanService;
import com.tencent.bk.job.analysis.service.TaskTemplateService;
import com.tencent.bk.job.analysis.task.analysis.AnalysisTaskStatusEnum;
import com.tencent.bk.job.analysis.task.analysis.BaseAnalysisTask;
import com.tencent.bk.job.analysis.task.analysis.anotation.AnalysisTask;
import com.tencent.bk.job.analysis.task.analysis.enums.AnalysisResourceEnum;
import com.tencent.bk.job.analysis.task.analysis.task.pojo.AnalysisTaskResultData;
import com.tencent.bk.job.analysis.task.analysis.task.pojo.AnalysisTaskResultItem;
import com.tencent.bk.job.analysis.task.analysis.task.pojo.AnalysisTaskResultVO;
import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.util.Counter;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.common.consts.JobResourceStatusEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskScriptSourceEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.model.inner.resp.ServiceApplicationDTO;
import com.tencent.bk.job.manage.model.inner.ServiceScriptDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskPlanDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskStepDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskTemplateDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description 寻找在作业模板/执行方案中使用的禁用脚本
 * @Date 2020/3/6
 * @Version 1.0
 */
@Component
@AnalysisTask("ForbiddenScriptFinder")
@Slf4j
public class ForbiddenScriptFinder extends BaseAnalysisTask {

    private final TaskPlanService taskPlanService;
    private final TaskTemplateService templateService;
    private final ScriptService scriptService;

    @Autowired
    public ForbiddenScriptFinder(
        DSLContext dslContext, AnalysisTaskDAO analysisTaskDAO,
        AnalysisTaskInstanceDAO analysisTaskInstanceDAO,
        ApplicationService applicationService,
        TaskPlanService taskPlanService,
        TaskTemplateService templateService,
        ScriptService scriptService
    ) {
        super(dslContext, analysisTaskDAO, analysisTaskInstanceDAO, applicationService);
        this.taskPlanService = taskPlanService;
        this.templateService = templateService;
        this.scriptService = scriptService;
    }

    private void findForbiddenScriptFromTaskTemplateStep(
        ServiceTaskTemplateDTO taskTemplateInfoDTO,
        ServiceTaskStepDTO taskStepDTO,
        List<BadTplPlanInfo> badTplPlanInfoList
    ) {
        Long appId = taskTemplateInfoDTO.getAppId();
        if (taskStepDTO.getType() == TaskStepTypeEnum.SCRIPT.getType()) {
            if (taskStepDTO.getScriptStepInfo().getScriptSource()
                == TaskScriptSourceEnum.CITING.getType()) {
                ServiceScriptDTO scriptVersion = null;
                try {
                    scriptVersion =
                        scriptService.getScriptVersion(taskTemplateInfoDTO.getCreator(), appId,
                            taskStepDTO.getScriptStepInfo().getScriptVersionId());
                } catch (ServiceException e) {
                    log.debug("script not exist by scriptVersionId {}"
                        , taskStepDTO.getScriptStepInfo().getScriptVersionId());
                    if (e.getErrorCode() == ErrorCode.SCRIPT_NOT_EXIST) {
                        badTplPlanInfoList.add(new BadTplPlanInfo(
                            AnalysisResourceEnum.TASK_PLAN
                            , new AnalysisTaskResultItemLocation(
                            "${job.analysis.analysistask.result.ItemLocation"
                                + ".description.TemplateId}",
                            "" + taskTemplateInfoDTO.getId()),
                            "${job.analysis.analysistask.result.BadTplPlanInfo"
                                + ".typeName.Template}",
                            taskTemplateInfoDTO.getName(),
                            taskStepDTO.getName()
                        ));
                    } else {
                        log.debug("script not exist by scriptVersionId {}, error"
                            , taskStepDTO.getScriptStepInfo().getScriptVersionId(), e);
                        throw e;
                    }
                }
                if (scriptVersion == null
                    || scriptVersion.getStatus() == JobResourceStatusEnum.DISABLED.getValue()) {
                    badTplPlanInfoList.add(new BadTplPlanInfo(AnalysisResourceEnum.TEMPLATE,
                        new AnalysisTaskResultItemLocation(
                            "${job.analysis.analysistask.result"
                                + ".ItemLocation.description.TemplateId}",
                            "" + taskTemplateInfoDTO.getId()),
                        "${job.analysis.analysistask"
                            + ".result.BadTplPlanInfo.typeName.Template}",
                        taskTemplateInfoDTO.getName(),
                        taskStepDTO.getName())
                    );
                }
            }
        }
    }

    private void findForbiddenScriptFromTaskPlanStep(
        ServiceTaskTemplateDTO taskTemplateInfoDTO,
        ServiceTaskPlanDTO taskPlanInfoDTO,
        ServiceTaskStepDTO taskStepDTO,
        List<BadTplPlanInfo> badTplPlanInfoList
    ) {
        Long appId = taskTemplateInfoDTO.getAppId();
        if (taskStepDTO.getType() == TaskStepTypeEnum.SCRIPT.getType()) {
            if (taskStepDTO.getScriptStepInfo().getScriptSource()
                == TaskScriptSourceEnum.CITING.getType()) {
                ServiceScriptDTO scriptVersion = null;
                try {
                    scriptVersion = scriptService.getScriptVersion(
                        taskTemplateInfoDTO.getCreator(),
                        appId,
                        taskStepDTO.getScriptStepInfo().getScriptVersionId()
                    );
                } catch (ServiceException e) {
                    log.debug("script not exist by scriptVersionId {}"
                        , taskStepDTO.getScriptStepInfo().getScriptVersionId());
                    if (e.getErrorCode() == ErrorCode.SCRIPT_NOT_EXIST) {
                        badTplPlanInfoList.add(new BadTplPlanInfo(
                            AnalysisResourceEnum.TASK_PLAN
                            , new AnalysisTaskResultItemLocation(
                            "${job.analysis.analysistask.result.ItemLocation"
                                + ".description.CommaSeparatedTplIdAndPlanId}",
                            "" + taskPlanInfoDTO.getTaskTemplateId()
                                + "," + taskPlanInfoDTO.getId()),
                            "${job.analysis.analysistask.result.BadTplPlanInfo"
                                + ".typeName.TaskPlan}",
                            taskPlanInfoDTO.getName(),
                            taskStepDTO.getName()
                        ));
                    } else {
                        log.debug("script not exist by scriptVersionId {}, error"
                            , taskStepDTO.getScriptStepInfo().getScriptVersionId(), e);
                        throw e;
                    }
                }
                if (scriptVersion == null || scriptVersion.getStatus()
                    == JobResourceStatusEnum.DISABLED.getValue()) {
                    badTplPlanInfoList.add(new BadTplPlanInfo(
                        AnalysisResourceEnum.TASK_PLAN,
                        new AnalysisTaskResultItemLocation(
                            "${job.analysis.analysistask.result.ItemLocation" +
                                ".description"
                                + ".CommaSeparatedTplIdAndPlanId}",
                            "" + taskPlanInfoDTO.getTaskTemplateId()
                                + "," + taskPlanInfoDTO.getId()),
                        "${job.analysis.analysistask.result.BadTplPlanInfo.typeName"
                            + ".TaskPlan}",
                        taskPlanInfoDTO.getName(),
                        taskStepDTO.getName()));
                }
            }
        }
    }

    private void findForbiddenScriptFromTemplatePlans(
        ServiceTaskTemplateDTO taskTemplateInfoDTO,
        List<BadTplPlanInfo> badTplPlanInfoList
    ) {
        Long appId = taskTemplateInfoDTO.getAppId();
        try {
            List<Long> taskPlanIdList =
                taskPlanService.listTaskPlanIds(taskTemplateInfoDTO.getId());
            if (taskPlanIdList == null) {
                log.error("Cannot find taskPlanIdList by templateId {}",
                    taskTemplateInfoDTO.getId());
                return;
            }
            //遍历作业模板中的执行方案
            for (Long taskPlanId : taskPlanIdList) {
                try {
                    //获得执行计划详情
                    ServiceTaskPlanDTO taskPlanInfoDTO =
                        taskPlanService.getTaskPlanById(appId, taskPlanId);
                    if (taskPlanInfoDTO == null) {
                        log.warn("Cannot find taskPlanInfoDTO by id {}", taskPlanId);
                        continue;
                    }
                    taskPlanInfoDTO.getStepList().forEach(taskStepDTO ->
                    {
                        findForbiddenScriptFromTaskPlanStep(
                            taskTemplateInfoDTO,
                            taskPlanInfoDTO,
                            taskStepDTO,
                            badTplPlanInfoList
                        );
                    });
                } catch (Throwable t) {
                    log.warn(String.format("Task of app(id=%d) template(id=%d) taskplan(id=%d) failed"
                        + " because of exception", appId, taskTemplateInfoDTO.getId(), taskPlanId), t);
                }
            }
            //遍历作业模板本身
            taskTemplateInfoDTO.getStepList().forEach(taskStepDTO -> {
                findForbiddenScriptFromTaskTemplateStep(
                    taskTemplateInfoDTO,
                    taskStepDTO,
                    badTplPlanInfoList
                );
            });
        } catch (Throwable t) {
            log.warn(String.format("Task of app(id=%d) template(id=%d) failed because of exception",
                appId, taskTemplateInfoDTO.getId()), t);
        }
    }

    @Override
    public void run() {
        try {
            log.info("Task " + getTaskCode() + " start");
            Counter appCounter = new Counter();
            List<ServiceApplicationDTO> appInfoList = getAppInfoList();
            AnalysisTaskDTO analysisTask = getAnalysisTask();
            for (ServiceApplicationDTO applicationInfoDTO : appInfoList) {
                if (applicationInfoDTO.getAppType() != AppTypeEnum.NORMAL.getValue()) {
                    continue;
                }
                Long appId = applicationInfoDTO.getId();
                appCounter.addOne();
                log.info("beigin to analysis app:" + appCounter.getValue() + "/"
                    + appInfoList.size() + "," + appId + "," + applicationInfoDTO.getName());
                //初始化
                val analysisTaskInstanceDTO = new AnalysisTaskInstanceDTO(
                    null,
                    appId,
                    analysisTask.getId(),
                    AnalysisTaskStatusEnum.RUNNING.getValue(),
                    "",
                    analysisTask.getPriority(),
                    analysisTask.isActive(),
                    analysisTask.getCreator(),
                    System.currentTimeMillis(),
                    analysisTask.getCreator(),
                    System.currentTimeMillis()
                );
                try {
                    Long id = insertAnalysisTaskInstance(analysisTaskInstanceDTO);
                    log.info("taskId:" + id);
                    analysisTaskInstanceDTO.setId(id);
                    List<BadTplPlanInfo> badTplPlanInfoList = new ArrayList<>();
                    BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
                    baseSearchCondition.setStart(0);
                    baseSearchCondition.setLength(Integer.MAX_VALUE);

                    ServiceTaskTemplateDTO templateCondition = new ServiceTaskTemplateDTO();
                    templateCondition.setAppId(appId);
                    PageData<ServiceTaskTemplateDTO> taskTemplateInfoDTOPageData =
                        templateService.listPageTaskTemplates(appId, baseSearchCondition);
                    List<ServiceTaskTemplateDTO> taskTemplateInfoDTOList = taskTemplateInfoDTOPageData.getData();
                    for (ServiceTaskTemplateDTO taskTemplateInfoDTO : taskTemplateInfoDTOList) {
                        findForbiddenScriptFromTemplatePlans(taskTemplateInfoDTO, badTplPlanInfoList);
                    }
                    //结果入库
                    analysisTaskInstanceDTO.setResultData(
                        JsonUtils.toJson(new AnalysisTaskResultData<>((long) badTplPlanInfoList.size(),
                            badTplPlanInfoList)));
                    analysisTaskInstanceDTO.setStatus(AnalysisTaskStatusEnum.SUCCESS.getValue());
                    updateAnalysisTaskInstanceById(analysisTaskInstanceDTO);
                    log.debug(String.format("%d badTemplates are recorded:%s", badTplPlanInfoList.size(),
                        JsonUtils.toJson(badTplPlanInfoList)));
                } catch (Throwable t) {
                    analysisTaskInstanceDTO.setStatus(AnalysisTaskStatusEnum.FAIL.getValue());
                    updateAnalysisTaskInstanceById(analysisTaskInstanceDTO);
                    log.warn(String.format("Task of app(id=%d) failed because of exception", appId), t);
                } finally {
                    log.info(String.format("Task:%s of app(id=%d) end", getTaskCode(), appId));
                }
            }
        } catch (Throwable t) {
            log.warn("Task failed because of exception", t);
        } finally {
            log.info("Task " + getTaskCode() + " end");
        }
    }

    @Override
    public AnalysisTaskResultVO generateResultVO(String descriptionTpl, String itemTpl, String data) {
        AnalysisTaskResultData<BadTplPlanInfo> resultData = JsonUtils.fromJson(data,
            new TypeReference<AnalysisTaskResultData<BadTplPlanInfo>>() {
            });
        String description = descriptionTpl;
        List<AnalysisTaskResultItem> contents = new ArrayList<>();
        if (resultData.getCount() == 0) {
            return null;
        } else if (resultData.getCount() == 1) {
            BadTplPlanInfo badTplPlanInfo = resultData.getData().get(0);
            description = description.replace("${typeName}", resultData.getData().get(0).getTypeName());
            description = description.replace("${instanceName}", resultData.getData().get(0).getInstanceName());
            description = description.replace("${stepName}", resultData.getData().get(0).getStepName());
            contents.add(new AnalysisTaskResultItem(badTplPlanInfo.analysisResourceType.name(),
                badTplPlanInfo.getLocation(), description));
        } else {
            description = descriptionTpl.replace("${itemsCount}", "" + resultData.getData().size());
            resultData.getData().forEach(it -> contents.add(new AnalysisTaskResultItem(it.analysisResourceType.name()
                , it.getLocation(), it.getTypeName() + ":" + it.getInstanceName() + ":" + it.getStepName())));
        }
        return new AnalysisTaskResultVO(description, contents);
    }

    /**
     * 使用了禁用状态脚本版本的作业模板/执行方案信息
     */
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    private static class BadTplPlanInfo {
        private AnalysisResourceEnum analysisResourceType;
        private AnalysisTaskResultItemLocation location;
        private String typeName;
        private String instanceName;
        private String stepName;
    }
}
