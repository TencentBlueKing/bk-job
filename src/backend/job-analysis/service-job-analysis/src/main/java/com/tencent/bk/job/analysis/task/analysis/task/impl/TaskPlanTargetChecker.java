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
import com.tencent.bk.job.analysis.consts.JobAnalysisConsts;
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
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.Counter;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.common.consts.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.model.inner.ServiceHostInfoDTO;
import com.tencent.bk.job.manage.model.inner.ServiceHostStatusDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskHostNodeDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskNodeInfoDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskPlanDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskStepDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskTemplateDTO;
import com.tencent.bk.job.manage.model.inner.resp.ServiceApplicationDTO;
import com.tencent.bk.job.manage.model.web.request.ipchooser.BizTopoNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.jooq.DSLContext;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;
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
    public AnalysisTaskResultVO renderResultVO(String descriptionTpl, String itemTpl, String data) {
        AnalysisTaskResultData<AbnormalTargetPlanInfo> resultData = JsonUtils.fromJson(data,
            new TypeReference<AnalysisTaskResultData<AbnormalTargetPlanInfo>>() {
            });
        String description = descriptionTpl;
        List<AnalysisTaskResultItem> contents = new ArrayList<>();
        if (resultData.getCount() == 0) {
            return null;
        } else if (resultData.getCount() == 1) {
            AbnormalTargetPlanInfo abnormalPlan = resultData.getData().get(0);
            description = description.replace(JobAnalysisConsts.PLACEHOLDER_PLAN_NAME, abnormalPlan.getPlanName());
            description = description.replace(JobAnalysisConsts.PLACEHOLDER_STEP_NAME, abnormalPlan.getStepName());
            description = description.replace(JobAnalysisConsts.PLACEHOLDER_DESCRIPTION, abnormalPlan.getDescription());
            contents.add(
                new AnalysisTaskResultItem(
                    abnormalPlan.analysisResourceType.name(),
                    abnormalPlan.getLocation(),
                    description
                )
            );
        } else {
            description = descriptionTpl.replace(
                JobAnalysisConsts.PLACEHOLDER_ITEMS_COUNT,
                "" + resultData.getData().size()
            );
            resultData.getData().forEach(it -> contents.add(new AnalysisTaskResultItem(
                it.getAnalysisResourceType().name(),
                it.getLocation(),
                it.getDescWithStepInfo()
            )));
        }
        return new AnalysisTaskResultVO(description, contents);
    }

    @Override
    public void run() {
        try {
            log.info("Task {} start", getTaskCode());
            // 当前仅支持对普通业务进行分析
            List<ServiceApplicationDTO> bizAppList = getAppInfoList().stream()
                .filter(ServiceApplicationDTO::isBiz)
                .collect(Collectors.toList());
            Counter appCounter = new Counter();
            AnalysisTaskDTO analysisTask = getAnalysisTask();
            for (ServiceApplicationDTO application : bizAppList) {
                tryToAnalysisOneApp(application, analysisTask, appCounter, bizAppList.size());
            }
        } catch (Throwable t) {
            log.warn("Task failed because of exception", t);
        } finally {
            log.info("Task {} end", getTaskCode());
        }
    }

    /**
     * 分析单个业务的数据
     *
     * @param application  业务信息
     * @param analysisTask 分析任务
     * @param appCounter   业务计数器
     * @param allAppNum    业务总数
     */
    private void tryToAnalysisOneApp(ServiceApplicationDTO application,
                                     AnalysisTaskDTO analysisTask,
                                     Counter appCounter,
                                     int allAppNum) {
        Long appId = application.getId();
        appCounter.addOne();
        log.info(
            "begin to analysis app:{}/{},{},{}",
            appCounter.getValue(),
            allAppNum,
            appId,
            application.getName()
        );
        //初始化
        val analysisTaskInstanceDTO = initAnalysisTaskInstance(appId, analysisTask);
        try {
            analysisOneApp(application, analysisTaskInstanceDTO);
        } catch (Throwable t) {
            analysisTaskInstanceDTO.setStatus(AnalysisTaskStatusEnum.FAIL.getValue());
            updateAnalysisTaskInstanceById(analysisTaskInstanceDTO);
            FormattingTuple msg = MessageFormatter.format(
                "Task of app(id={}) failed because of exception",
                appId
            );
            log.warn(msg.getMessage(), t);
        } finally {
            log.info("Task:{} of app(id={}) end", getTaskCode(), appId);
        }
    }

    /**
     * 生成初始化分析任务实例
     *
     * @param appId        业务对应的appId
     * @param analysisTask 分析任务
     * @return 初始化分析任务实例
     */
    private AnalysisTaskInstanceDTO initAnalysisTaskInstance(Long appId, AnalysisTaskDTO analysisTask) {
        return new AnalysisTaskInstanceDTO(
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
    }

    /**
     * 分析一个业务的数据
     *
     * @param application          业务信息
     * @param analysisTaskInstance 分析任务实例
     */
    private void analysisOneApp(ServiceApplicationDTO application,
                                AnalysisTaskInstanceDTO analysisTaskInstance) {
        Long appId = application.getId();
        Long id = insertAnalysisTaskInstance(analysisTaskInstance);
        log.info("taskId:{}", id);
        analysisTaskInstance.setId(id);
        List<AbnormalTargetPlanInfo> abnormalPlanList = new ArrayList<>();
        //1.拿到所有作业模板
        ServiceTaskTemplateDTO taskTemplateCondition = new ServiceTaskTemplateDTO();
        taskTemplateCondition.setAppId(appId);
        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(0);
        baseSearchCondition.setLength(Integer.MAX_VALUE);
        PageData<ServiceTaskTemplateDTO> taskTemplateInfoDTOPageData =
            templateService.listPageTaskTemplates(appId, baseSearchCondition);
        List<ServiceTaskTemplateDTO> templateList = taskTemplateInfoDTOPageData.getData();
        //2.遍历所有作业模板
        Counter templateCounter = new Counter();
        for (ServiceTaskTemplateDTO templateBasicInfo : templateList) {
            tryToAnalysisOneTemplate(
                appId,
                templateBasicInfo,
                abnormalPlanList,
                templateCounter,
                templateList.size()
            );
        }
        //结果入库
        analysisTaskInstance.setResultData(
            JsonUtils.toJson(
                new AnalysisTaskResultData<>((long) abnormalPlanList.size(), abnormalPlanList)
            )
        );
        analysisTaskInstance.setStatus(AnalysisTaskStatusEnum.SUCCESS.getValue());
        updateAnalysisTaskInstanceById(analysisTaskInstance);
        log.debug(
            "{} found agentAbnormalItems are recorded:{}",
            abnormalPlanList.size(),
            JsonUtils.toJson(abnormalPlanList)
        );
    }

    /**
     * 尝试分析一个模板的数据，若有异常则捕获、打印，不阻塞下一个模板分析
     *
     * @param appId             业务的appId
     * @param templateBasicInfo 模板信息
     * @param abnormalPlanList  异常的执行计划列表
     * @param templateCounter   模板计数器
     * @param allTemplateNum    当前业务下的所有模板总数
     */
    private void tryToAnalysisOneTemplate(Long appId,
                                          ServiceTaskTemplateDTO templateBasicInfo,
                                          List<AbnormalTargetPlanInfo> abnormalPlanList,
                                          Counter templateCounter,
                                          int allTemplateNum) {
        try {
            analysisOneTemplate(appId, templateBasicInfo, abnormalPlanList, templateCounter, allTemplateNum);
        } catch (Throwable t) {
            FormattingTuple msg = MessageFormatter.format(
                "Task of app(id={}) template(id={}) failed because of exception",
                appId,
                templateBasicInfo.getId()
            );
            log.warn(msg.getMessage(), t);
        }
    }

    /**
     * 分析一个模板的数据
     *
     * @param appId             业务的appId
     * @param templateBasicInfo 模板信息
     * @param abnormalPlanList  异常的执行计划列表
     * @param templateCounter   模板计数器
     * @param allTemplateNum    当前业务下的所有模板总数
     */
    private void analysisOneTemplate(Long appId,
                                     ServiceTaskTemplateDTO templateBasicInfo,
                                     List<AbnormalTargetPlanInfo> abnormalPlanList,
                                     Counter templateCounter,
                                     int allTemplateNum) {
        templateCounter.addOne();
        log.info(
            "begin to analysis taskTemplate:{}/{},{},{}",
            templateCounter.getValue(),
            allTemplateNum,
            templateBasicInfo.getId(),
            templateBasicInfo.getName()
        );
        List<Long> taskPlanIdList =
            taskPlanService.listTaskPlanIds(templateBasicInfo.getId());
        if (taskPlanIdList == null || taskPlanIdList.isEmpty()) {
            log.info("Cannot find taskPlanIdList by templateId {}", templateBasicInfo.getId());
            return;
        }
        //遍历作业模板中的执行方案
        for (Long taskPlanId : taskPlanIdList) {
            tryToAnalysisOnePlan(appId, templateBasicInfo.getId(), taskPlanId, abnormalPlanList);
        }
    }

    /**
     * 尝试分析一个执行方案的数据，若有异常则捕获、打印，不阻塞下一个执行方案分析
     *
     * @param appId            业务的appId
     * @param templateId       模板Id
     * @param taskPlanId       执行方案Id
     * @param abnormalPlanList 异常执行方案列表
     */
    private void tryToAnalysisOnePlan(Long appId,
                                      Long templateId,
                                      Long taskPlanId,
                                      List<AbnormalTargetPlanInfo> abnormalPlanList) {
        try {
            analysisOnePlan(appId, taskPlanId, abnormalPlanList);
        } catch (Throwable t) {
            FormattingTuple msg = MessageFormatter.format(
                "Task of app(id={}) template(id={}) taskPlan(id={}) failed because of exception",
                new Object[]{appId, templateId, taskPlanId}
            );
            log.warn(msg.getMessage(), t);
        }
    }

    /**
     * 分析一个执行方案的数据
     *
     * @param appId            业务的appId
     * @param taskPlanId       执行方案Id
     * @param abnormalPlanList 异常执行方案列表
     */
    private void analysisOnePlan(Long appId,
                                 Long taskPlanId,
                                 List<AbnormalTargetPlanInfo> abnormalPlanList) {
        //获得执行计划详情
        ServiceTaskPlanDTO taskPlanInfoDTO =
            taskPlanService.getTaskPlanById(appId, taskPlanId);
        if (taskPlanInfoDTO == null) {
            log.error("Cannot find taskPlanInfoDTO by id {}", taskPlanId);
            return;
        }
        //检查步骤
        for (ServiceTaskStepDTO taskStepDTO : taskPlanInfoDTO.getStepList()) {
            ServiceTaskHostNodeDTO targetServer = null;
            if (taskStepDTO.getType() == TaskStepTypeEnum.SCRIPT.getValue()) {
                targetServer = taskStepDTO.getScriptStepInfo().getExecuteTarget().getTargetServer();
            } else if (taskStepDTO.getType() == TaskStepTypeEnum.FILE.getValue()) {
                targetServer = taskStepDTO.getFileStepInfo().getExecuteTarget().getTargetServer();
            }
            if (targetServer == null) {
                continue;
            }
            if (existNotAliveHostInTaskHostNode(appId, targetServer)) {
                AbnormalTargetPlanInfo abnormalTargetPlanInfo = buildAbnormalTargetPlanInfo(
                    buildPlanLocationContent(taskPlanInfoDTO),
                    taskPlanInfoDTO.getName(),
                    getI18nKeyAbnormalTargetPlanStep(),
                    taskStepDTO.getName(),
                    getI18nKeyAbnormalTargetPlanDesc()
                );
                abnormalPlanList.add(abnormalTargetPlanInfo);
            }
        }
    }

    /**
     * 任务的执行目标中是否存在Agent异常的主机
     *
     * @param appId        业务的appId
     * @param targetServer 任务的执行目标
     * @return 是否存在Agent异常的主机
     */
    private boolean existNotAliveHostInTaskHostNode(Long appId,
                                                    ServiceTaskHostNodeDTO targetServer) {
        //（1）拓扑节点
        List<ServiceTaskNodeInfoDTO> targetNodeList = targetServer.getNodeInfoList();
        if (existNotAliveHostInTargetNodes(appId, targetNodeList)) {
            return true;
        }
        //（2）动态分组
        //找到动态分组对应的所有主机
        List<String> dynamicGroupIdList = targetServer.getDynamicGroupId();
        if (existNotAliveHostInDynamicGroups(appId, dynamicGroupIdList)) {
            return true;
        }
        //（3）主机
        List<ServiceHostInfoDTO> serviceHostInfoDTOList = targetServer.getHostList();
        return existNotAliveHost(appId, serviceHostInfoDTOList);
    }

    /**
     * 构造一个含有异常执行目标的执行方案实例
     *
     * @param locationContent 定位信息
     * @param instanceName    实例名称
     * @param scope           范畴（步骤等）
     * @param scopeName       范畴实例名称
     * @param description     描述
     * @return 异常执行目标的执行方案实例
     */
    private AbnormalTargetPlanInfo buildAbnormalTargetPlanInfo(String locationContent,
                                                               String instanceName,
                                                               String scope,
                                                               String scopeName,
                                                               String description) {
        return new AbnormalTargetPlanInfo(
            AnalysisResourceEnum.TASK_PLAN,
            new AnalysisTaskResultItemLocation(getI18nKeyCommaSeparatedTplIdAndPlanId(), locationContent),
            instanceName,
            scope + ":" + scopeName,
            description
        );
    }

    private String getI18nKeyCommaSeparatedTplIdAndPlanId() {
        return "${job.analysis.analysistask.result.ItemLocation.description.CommaSeparatedTplIdAndPlanId}";
    }

    /**
     * 判断在节点列表下是否存在Agent异常的主机
     *
     * @param appId          业务的appId
     * @param targetNodeList 拓扑节点列表
     * @return 是否存在Agent异常的主机
     */
    private boolean existNotAliveHostInTargetNodes(Long appId, List<ServiceTaskNodeInfoDTO> targetNodeList) {
        if (CollectionUtils.isEmpty(targetNodeList)) {
            return false;
        }
        List<ServiceHostStatusDTO> hostStatusList =
            hostService.getHostStatusByNode(
                appId,
                targetNodeList.stream().map(it -> new BizTopoNode(
                    it.getType(),
                    "",
                    it.getId(),
                    "",
                    null
                )).collect(Collectors.toList()));
        //找到目标节点对应的所有主机
        for (ServiceHostStatusDTO serviceHostStatusDTO : hostStatusList) {
            if (!serviceHostStatusDTO.isAgentAlive()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断在动态分组下是否存在Agent异常的主机
     *
     * @param appId              业务的appId
     * @param dynamicGroupIdList 动态分组Id列表
     * @return 是否存在Agent异常的主机
     */
    private boolean existNotAliveHostInDynamicGroups(Long appId, List<String> dynamicGroupIdList) {
        if (CollectionUtils.isEmpty(dynamicGroupIdList)) {
            return false;
        }
        List<ServiceHostStatusDTO> hostStatusList = hostService.getHostStatusByDynamicGroup(appId, dynamicGroupIdList);
        for (ServiceHostStatusDTO hostStatusDTO : hostStatusList) {
            if (!hostStatusDTO.isAgentAlive()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断在主机列表中是否存在Agent异常的主机
     *
     * @param appId           业务的appId
     * @param serviceHostList 主机信息列表
     * @return 是否存在Agent异常的主机
     */
    private boolean existNotAliveHost(Long appId, List<ServiceHostInfoDTO> serviceHostList) {
        if (CollectionUtils.isEmpty(serviceHostList)) {
            return false;
        }
        //noinspection deprecation
        List<HostDTO> hostList = serviceHostList.parallelStream().map(serviceHost ->
            // TODO:执行方案数据迁移添加hostId后此处可去除cloudAreaId与ip
            HostDTO.fromHostIdOrCloudIp(
                serviceHost.getHostId(),
                serviceHost.getCloudAreaId(),
                serviceHost.getIp()
            )
        ).collect(Collectors.toList());
        List<ServiceHostStatusDTO> hostStatusDTOListByHost =
            hostService.getHostStatusByHost(appId, hostList);
        Map<Long, ServiceHostStatusDTO> hostIdMap = new HashMap<>();
        hostStatusDTOListByHost.forEach(hostStatusDTO -> hostIdMap.put(hostStatusDTO.getHostId(), hostStatusDTO));
        if (hostList.isEmpty()) {
            return true;
        }
        for (HostDTO host : hostList) {
            if (!isHostAlive(hostIdMap, host)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据hostId判断Agent是否正常
     *
     * @param hostIdMap 主机Id与主机Agent状态信息映射表
     * @param host      主机信息
     * @return Agent是否正常
     */
    private boolean isHostAlive(Map<Long, ServiceHostStatusDTO> hostIdMap, HostDTO host) {
        Long hostId = host.getHostId();
        if (hostId == null) {
            return false;
        }
        if (!hostIdMap.containsKey(hostId)) {
            return false;
        }
        return hostIdMap.get(hostId).getAlive() == 1;
    }

    /**
     * 构造执行方案定位信息：模板Id，执行方案Id
     *
     * @param taskPlan 执行方案实例
     * @return 定位信息字符串
     */
    private String buildPlanLocationContent(ServiceTaskPlanDTO taskPlan) {
        return taskPlan.getTaskTemplateId() + "," + taskPlan.getId();
    }

    /**
     * @return 含有异常执行目标的执行方案步骤I18nKey
     */
    private String getI18nKeyAbnormalTargetPlanStep() {
        return "${job.analysis.analysistask.result.AbnormalTargetPlanInfo.stepName.scope.Step}";
    }

    /**
     * @return 含有异常执行目标的执行方案描述I18nKey
     */
    private String getI18nKeyAbnormalTargetPlanDesc() {
        return "${job.analysis.analysistask.result.AbnormalTargetPlanInfo.description.AbnormalTarget}";
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

        public String getDescWithStepInfo() {
            return getPlanName() + ":" + getStepName() + ":" + getDescription();
        }
    }
}
