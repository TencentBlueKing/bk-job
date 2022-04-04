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
import com.tencent.bk.job.analysis.service.HostService;
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
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.util.Counter;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.common.consts.task.TaskStepTypeEnum;
import com.tencent.bk.job.common.model.ServiceApplicationDTO;
import com.tencent.bk.job.manage.model.inner.ServiceHostInfoDTO;
import com.tencent.bk.job.manage.model.inner.ServiceHostStatusDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskHostNodeDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskNodeInfoDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskPlanDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskStepDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskTemplateDTO;
import com.tencent.bk.job.manage.model.web.request.ipchooser.AppTopologyTreeNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Description 寻找执行方案中是否有无效IP/Agent异常/Agent未安装的情况
 * @Date 2020/3/6
 * @Version 1.0
 */
@Component
@AnalysisTask("TaskPlanTargetChecker")
@Slf4j
public class TaskPlanTargetChecker extends BaseAnalysisTask {

    private final TaskPlanService taskPlanService;
    private final TaskTemplateService templateService;
    private final HostService hostService;

    @Autowired
    public TaskPlanTargetChecker(
        DSLContext dslContext,
        AnalysisTaskDAO analysisTaskDAO,
        AnalysisTaskInstanceDAO analysisTaskInstanceDAO,
        TaskTemplateService templateService,
        TaskPlanService taskPlanService,
        ApplicationService applicationService,
        HostService hostService
    ) {
        super(dslContext, analysisTaskDAO, analysisTaskInstanceDAO, applicationService);
        this.templateService = templateService;
        this.taskPlanService = taskPlanService;
        this.hostService = hostService;
    }

    @Override
    public void run() {
        try {
            log.info("Task " + getTaskCode() + " start");
            List<ServiceApplicationDTO> appInfoList = getAppInfoList();
            Counter appCounter = new Counter();
            AnalysisTaskDTO analysisTask = getAnalysisTask();
            for (ServiceApplicationDTO applicationInfoDTO : appInfoList) {
                if (applicationInfoDTO.getAppType() != AppTypeEnum.NORMAL.getValue()) {
                    continue;
                }
                Long appId = applicationInfoDTO.getId();
                appCounter.addOne();
                log.info(
                    "begin to analysis app:" + appCounter.getValue()
                        + "/" + appInfoList.size()
                        + "," + appId
                        + "," + applicationInfoDTO.getName()
                );
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
                    List<AbnormalTargetPlanInfo> abnormalTargetPlanInfoList = new ArrayList<>();
                    //业务运维身份信息
                    String username = applicationInfoDTO.getMaintainers();
                    if (username.contains(";")) {
                        username = username.split(";")[0];
                    }
                    //1.拿到所有作业模板
                    ServiceTaskTemplateDTO taskTemplateCondition = new ServiceTaskTemplateDTO();
                    taskTemplateCondition.setAppId(appId);
                    BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
                    baseSearchCondition.setStart(0);
                    baseSearchCondition.setLength(Integer.MAX_VALUE);
                    PageData<ServiceTaskTemplateDTO> taskTemplateInfoDTOPageData =
                        templateService.listPageTaskTemplates(appId, baseSearchCondition);
                    List<ServiceTaskTemplateDTO> taskTemplateInfoDTOList = taskTemplateInfoDTOPageData.getData();
                    //2.遍历所有作业模板
                    String finalUsername = username;
                    Counter templateCounter = new Counter();
                    for (ServiceTaskTemplateDTO taskTemplateBasicInfoDTO : taskTemplateInfoDTOList) {
                        try {
                            templateCounter.addOne();
                            log.info("beigin to analysis taskTemplate:"
                                + templateCounter.getValue()
                                + "/" + taskTemplateInfoDTOList.size()
                                + "," + taskTemplateBasicInfoDTO.getId()
                                + "," + taskTemplateBasicInfoDTO.getName()
                            );
                            List<Long> taskPlanIdList =
                                taskPlanService.listTaskPlanIds(taskTemplateBasicInfoDTO.getId());
                            if (taskPlanIdList == null || taskPlanIdList.isEmpty()) {
                                log.info("Cannot find taskPlanIdList by templateId {}",
                                    taskTemplateBasicInfoDTO.getId());
                                continue;
                            }
                            //遍历作业模板中的执行方案
                            for (Long taskPlanId : taskPlanIdList) {
                                try {
                                    //获得执行计划详情
                                    ServiceTaskPlanDTO taskPlanInfoDTO =
                                        taskPlanService.getTaskPlanById(appId, taskPlanId);
                                    if (taskPlanInfoDTO == null) {
                                        log.error("Cannot find taskPlanInfoDTO by id {}", taskPlanId);
                                        continue;
                                    }
                                    //检查步骤
                                    for (ServiceTaskStepDTO taskStepDTO : taskPlanInfoDTO.getStepList()) {
                                        ServiceTaskHostNodeDTO taskHostNodeDTO = null;
                                        if (taskStepDTO.getType() == TaskStepTypeEnum.SCRIPT.getType()) {
                                            taskHostNodeDTO =
                                                taskStepDTO.getScriptStepInfo().getExecuteTarget().getTargetServer();
                                        } else if (taskStepDTO.getType() == TaskStepTypeEnum.FILE.getType()) {
                                            taskHostNodeDTO = taskStepDTO
                                                .getFileStepInfo()
                                                .getExecuteTarget()
                                                .getTargetServer();
                                        }
                                        if (taskHostNodeDTO != null) {
                                            findAgentAbnormalInTaskHostNodeVO(
                                                abnormalTargetPlanInfoList,
                                                taskHostNodeDTO,
                                                finalUsername,
                                                appId,
                                                "" + taskPlanInfoDTO.getTaskTemplateId()
                                                    + "," + taskPlanInfoDTO.getId(),
                                                taskPlanInfoDTO.getName(),
                                                "${job.analysis.analysistask.result.AbnormalTargetPlanInfo.stepName"
                                                    + ".scope.Step}",
                                                taskStepDTO.getName(),
                                                "${job.analysis.analysistask.result.AbnormalTargetPlanInfo"
                                                    + ".description.AbnormalTarget}"
                                            );
                                        }
                                    }
                                } catch (Throwable t) {
                                    log.warn(
                                        String.format("Task of app(id=%d) template(id=%d) taskplan(id=%d) failed"
                                                + " because of exception",
                                            appId,
                                            taskTemplateBasicInfoDTO.getId(),
                                            taskPlanId),
                                        t);
                                }
                            }
                        } catch (Throwable t) {
                            log.warn(String.format("Task of app(id=%d) template(id=%d) failed because of exception",
                                appId, taskTemplateBasicInfoDTO.getId()), t);
                        }
                    }
                    //结果入库
                    analysisTaskInstanceDTO.setResultData(
                        JsonUtils.toJson(
                            new AnalysisTaskResultData<>(
                                (long) abnormalTargetPlanInfoList.size(),
                                abnormalTargetPlanInfoList)
                        )
                    );
                    analysisTaskInstanceDTO.setStatus(AnalysisTaskStatusEnum.SUCCESS.getValue());
                    updateAnalysisTaskInstanceById(analysisTaskInstanceDTO);
                    log.debug(String.format("%d found agentAbnormalItems are recorded:%s",
                        abnormalTargetPlanInfoList.size(), JsonUtils.toJson(abnormalTargetPlanInfoList)));
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

    private AbnormalTargetPlanInfo getAbnormalTargetPlanInfo(AnalysisResourceEnum analysisResourceEnum,
                                                             String locationContent, String instanceName,
                                                             String scope, String scopeName, String description) {
        return new AbnormalTargetPlanInfo(
            AnalysisResourceEnum.TASK_PLAN
            , new AnalysisTaskResultItemLocation(
            "${job.analysis.analysistask.result.ItemLocation.description"
                + ".CommaSeparatedTplIdAndPlanId}",
            locationContent)
            , instanceName
            , scope + ":" + scopeName
            , description
        );
    }

    private void findAgentAbnormalInTaskHostNodeVO(List<AbnormalTargetPlanInfo> abnormalTargetPlanInfoList,
                                                   ServiceTaskHostNodeDTO serviceTaskHostNodeDTO, String username,
                                                   Long appId, String locationContent, String instanceName,
                                                   String scope, String scopeName, String description) {
        List<ServiceHostStatusDTO> hostStatusDTOList = new ArrayList<>();
        //（1）目标节点
        List<ServiceTaskNodeInfoDTO> targetNodeVOList = serviceTaskHostNodeDTO.getNodeInfoList();
        if (targetNodeVOList != null && !targetNodeVOList.isEmpty()) {
            hostStatusDTOList.addAll(hostService.getHostStatusByNode(username, appId,
                targetNodeVOList.stream().map(it -> new AppTopologyTreeNode(
                    it.getType(),
                    "",
                    it.getId(),
                    "",
                    null
                )).collect(Collectors.toList())));
            //找到目标节点对应的所有主机
            for (ServiceHostStatusDTO serviceHostStatusDTO : hostStatusDTOList) {
                if (serviceHostStatusDTO.getAlive() != 1) {
                    abnormalTargetPlanInfoList.add(getAbnormalTargetPlanInfo(AnalysisResourceEnum.TASK_PLAN,
                        locationContent, instanceName, scope, scopeName, description));
                    break;
                }
            }
        }
        //（2）动态分组
        //找到动态分组对应的所有主机
        List<String> dynamicGroupIdList = serviceTaskHostNodeDTO.getDynamicGroupId();
        if (dynamicGroupIdList != null && !dynamicGroupIdList.isEmpty()) {
            hostStatusDTOList.addAll(hostService.getHostStatusByDynamicGroup(username, appId,
                dynamicGroupIdList));
        }
        //找到目标节点对应的所有主机//主机异常
        for (ServiceHostStatusDTO hostStatusDTO : hostStatusDTOList) {
            if (hostStatusDTO.getAlive() != 1) {
                abnormalTargetPlanInfoList.add(getAbnormalTargetPlanInfo(AnalysisResourceEnum.TASK_PLAN,
                    locationContent, instanceName, scope, scopeName, description));
                break;
            }
        }
        //（3）主机
        List<ServiceHostInfoDTO> serviceHostInfoDTOList = serviceTaskHostNodeDTO.getHostList();
        if (serviceHostInfoDTOList == null || serviceHostInfoDTOList.isEmpty()) return;
        List<String> ipList = serviceHostInfoDTOList.parallelStream().map(serviceHostInfoDTO ->
            serviceHostInfoDTO.getCloudAreaId() + ":" + serviceHostInfoDTO.getIp()
        ).collect(Collectors.toList());
        List<ServiceHostStatusDTO> hostStatusDTOListByIp =
            hostService.getHostStatusByIp(username, appId, ipList);
        Map<String, ServiceHostStatusDTO> map = new HashMap<>();
        hostStatusDTOListByIp.forEach(serviceHostStatusDTO -> {
            map.put(serviceHostStatusDTO.getIp(), serviceHostStatusDTO);
        });
        if (!ipList.isEmpty()) {
            for (String ip : ipList) {
                if (!map.containsKey(ip)) {
                    //IP无效
                    abnormalTargetPlanInfoList.add(getAbnormalTargetPlanInfo(AnalysisResourceEnum.TASK_PLAN,
                        locationContent, instanceName, scope, scopeName, description));
                    break;
                } else {
                    if (map.get(ip).getAlive() != 1) {
                        //主机异常
                        abnormalTargetPlanInfoList.add(getAbnormalTargetPlanInfo(AnalysisResourceEnum.TASK_PLAN,
                            locationContent, instanceName, scope, scopeName, description));
                        break;
                    }
                }
            }
        }
    }

    @Override
    public AnalysisTaskResultVO generateResultVO(String descriptionTpl, String itemTpl, String data) {
        AnalysisTaskResultData<AbnormalTargetPlanInfo> resultData = JsonUtils.fromJson(data,
            new TypeReference<AnalysisTaskResultData<AbnormalTargetPlanInfo>>() {
            });
        String description = descriptionTpl;
        List<AnalysisTaskResultItem> contents = new ArrayList<>();
        if (resultData.getCount() == 0) {
            return null;
        } else if (resultData.getCount() == 1) {
            AbnormalTargetPlanInfo abnormalTargetPlanInfo = resultData.getData().get(0);
            description = description.replace("${planName}", abnormalTargetPlanInfo.getPlanName());
            description = description.replace("${stepName}", abnormalTargetPlanInfo.getStepName());
            description = description.replace("${description}", abnormalTargetPlanInfo.getDescription());
            contents.add(new AnalysisTaskResultItem(abnormalTargetPlanInfo.analysisResourceType.name(),
                abnormalTargetPlanInfo.getLocation(), description));
        } else {
            description = descriptionTpl.replace("${itemsCount}", "" + resultData.getData().size());
            resultData.getData().forEach(it -> contents.add(new AnalysisTaskResultItem(
                it.getAnalysisResourceType().name(),
                it.getLocation(),
                it.getPlanName() + ":" + it.getStepName() + ":" + it.getDescription()
            )));
        }
        return new AnalysisTaskResultVO(description, contents);
    }

    /**
     * 执行目标异常的执行方案信息
     */
    @NoArgsConstructor
    @AllArgsConstructor
    @Data
    private static class AbnormalTargetPlanInfo {
        private AnalysisResourceEnum analysisResourceType;
        private AnalysisTaskResultItemLocation location;
        private String planName;
        private String stepName;
        private String description;
    }
}
