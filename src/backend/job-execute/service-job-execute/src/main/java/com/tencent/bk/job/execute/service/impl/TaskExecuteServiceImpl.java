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

package com.tencent.bk.job.execute.service.impl;

import com.tencent.bk.audit.context.ActionAuditContext;
import com.tencent.bk.audit.context.AuditContext;
import com.tencent.bk.audit.utils.AuditInstanceUtils;
import com.tencent.bk.job.common.audit.JobAuditAttributeNames;
import com.tencent.bk.job.common.audit.JobAuditExtendDataKeys;
import com.tencent.bk.job.common.audit.constants.EventContentConstants;
import com.tencent.bk.job.common.constant.AccountCategoryEnum;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.exception.AbortedException;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.exception.ResourceExhaustedException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.util.ArrayUtil;
import com.tencent.bk.job.common.util.DataSizeConverter;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.audit.ExecuteJobAuditEventBuilder;
import com.tencent.bk.job.execute.auth.ExecuteAuthService;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.common.context.JobExecuteContext;
import com.tencent.bk.job.execute.common.context.JobExecuteContextThreadLocalRepo;
import com.tencent.bk.job.execute.common.context.JobInstanceContext;
import com.tencent.bk.job.execute.common.converter.StepTypeExecuteTypeConverter;
import com.tencent.bk.job.execute.common.exception.RunningJobQuotaLimitExceedException;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.constants.ScriptSourceEnum;
import com.tencent.bk.job.execute.constants.StepOperationEnum;
import com.tencent.bk.job.execute.constants.TaskOperationEnum;
import com.tencent.bk.job.execute.constants.UserOperationEnum;
import com.tencent.bk.job.execute.engine.evict.TaskEvictPolicyExecutor;
import com.tencent.bk.job.execute.engine.listener.event.JobEvent;
import com.tencent.bk.job.execute.engine.listener.event.StepEvent;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.engine.quota.limit.ResourceQuotaCheckResultEnum;
import com.tencent.bk.job.execute.engine.quota.limit.RunningJobResourceQuotaManager;
import com.tencent.bk.job.execute.engine.util.TimeoutUtils;
import com.tencent.bk.job.execute.model.AccountDTO;
import com.tencent.bk.job.execute.model.DynamicServerGroupDTO;
import com.tencent.bk.job.execute.model.DynamicServerTopoNodeDTO;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.FastTaskDTO;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.OperationLogDTO;
import com.tencent.bk.job.execute.model.RollingConfigDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.StepOperationDTO;
import com.tencent.bk.job.execute.model.TaskExecuteParam;
import com.tencent.bk.job.execute.model.TaskInfo;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceExecuteObjects;
import com.tencent.bk.job.execute.service.AccountService;
import com.tencent.bk.job.execute.service.DangerousScriptCheckService;
import com.tencent.bk.job.execute.service.RollingConfigService;
import com.tencent.bk.job.execute.service.ScriptService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskExecuteService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceVariableService;
import com.tencent.bk.job.execute.service.TaskOperationLogService;
import com.tencent.bk.job.execute.service.TaskPlanService;
import com.tencent.bk.job.execute.util.LoggerFactory;
import com.tencent.bk.job.manage.GlobalAppScopeMappingService;
import com.tencent.bk.job.manage.api.common.constants.JobResourceStatusEnum;
import com.tencent.bk.job.manage.api.common.constants.notify.JobRoleEnum;
import com.tencent.bk.job.manage.api.common.constants.notify.ResourceTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.whiteip.ActionScopeEnum;
import com.tencent.bk.job.manage.api.inner.ServiceTaskTemplateResource;
import com.tencent.bk.job.manage.api.inner.ServiceUserResource;
import com.tencent.bk.job.manage.model.inner.ServiceAccountDTO;
import com.tencent.bk.job.manage.model.inner.ServiceHostInfoDTO;
import com.tencent.bk.job.manage.model.inner.ServiceScriptCheckResultItemDTO;
import com.tencent.bk.job.manage.model.inner.ServiceScriptDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskApprovalStepDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskFileInfoDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskFileStepDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskHostNodeDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskNodeInfoDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskPlanDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskScriptStepDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskStepDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskTargetDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTaskVariableDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum.EXECUTE_SCRIPT;
import static com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum.EXECUTE_SQL;
import static com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum.MANUAL_CONFIRM;
import static com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum.SEND_FILE;

@Service
@Slf4j
public class TaskExecuteServiceImpl implements TaskExecuteService {
    private final AccountService accountService;
    private final ScriptService scriptService;
    private final TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;
    private final TaskPlanService taskPlanService;
    private final TaskInstanceVariableService taskInstanceVariableService;
    private final TaskOperationLogService taskOperationLogService;
    private final TaskInstanceService taskInstanceService;
    private final StepInstanceService stepInstanceService;
    private final ServiceUserResource userResource;
    private final ExecuteAuthService executeAuthService;
    private final DangerousScriptCheckService dangerousScriptCheckService;
    private final RollingConfigService rollingConfigService;
    private final JobExecuteConfig jobExecuteConfig;
    private final TaskEvictPolicyExecutor taskEvictPolicyExecutor;
    private final ServiceTaskTemplateResource taskTemplateResource;
    private final TaskInstanceExecuteObjectProcessor taskInstanceExecuteObjectProcessor;

    private final RunningJobResourceQuotaManager runningJobResourceQuotaManager;

    private static final Logger TASK_MONITOR_LOGGER = LoggerFactory.TASK_MONITOR_LOGGER;

    @Autowired
    public TaskExecuteServiceImpl(AccountService accountService,
                                  TaskInstanceService taskInstanceService,
                                  TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher,
                                  TaskPlanService taskPlanService,
                                  TaskInstanceVariableService taskInstanceVariableService,
                                  TaskOperationLogService taskOperationLogService,
                                  ScriptService scriptService,
                                  StepInstanceService stepInstanceService,
                                  ServiceUserResource userResource,
                                  ExecuteAuthService executeAuthService,
                                  DangerousScriptCheckService dangerousScriptCheckService,
                                  JobExecuteConfig jobExecuteConfig,
                                  TaskEvictPolicyExecutor taskEvictPolicyExecutor,
                                  RollingConfigService rollingConfigService,
                                  ServiceTaskTemplateResource taskTemplateResource,
                                  TaskInstanceExecuteObjectProcessor taskInstanceExecuteObjectProcessor,
                                  RunningJobResourceQuotaManager runningJobResourceQuotaManager) {
        this.accountService = accountService;
        this.taskInstanceService = taskInstanceService;
        this.taskExecuteMQEventDispatcher = taskExecuteMQEventDispatcher;
        this.taskPlanService = taskPlanService;
        this.taskInstanceVariableService = taskInstanceVariableService;
        this.taskOperationLogService = taskOperationLogService;
        this.scriptService = scriptService;
        this.stepInstanceService = stepInstanceService;
        this.userResource = userResource;
        this.executeAuthService = executeAuthService;
        this.dangerousScriptCheckService = dangerousScriptCheckService;
        this.rollingConfigService = rollingConfigService;
        this.jobExecuteConfig = jobExecuteConfig;
        this.taskEvictPolicyExecutor = taskEvictPolicyExecutor;
        this.taskTemplateResource = taskTemplateResource;
        this.taskInstanceExecuteObjectProcessor = taskInstanceExecuteObjectProcessor;
        this.runningJobResourceQuotaManager = runningJobResourceQuotaManager;
    }

    @Override
    public TaskInstanceDTO executeFastTask(FastTaskDTO fastTask) {
        // 设置脚本信息
        checkAndSetScript(fastTask.getTaskInstance(), fastTask.getStepInstance());

        StepInstanceDTO stepInstance = fastTask.getStepInstance();

        ActionAuditContext actionAuditContext;
        if (stepInstance.isFileStep()) {
            actionAuditContext = ActionAuditContext.builder(ActionId.QUICK_TRANSFER_FILE)
                .setEventBuilder(ExecuteJobAuditEventBuilder.class)
                .setContent(EventContentConstants.QUICK_TRANSFER_FILE)
                .build();
        } else if (stepInstance.isScriptStep()) {
            ScriptSourceEnum scriptSource = ScriptSourceEnum.getScriptSourceEnum(stepInstance.getScriptSource());
            if (scriptSource == ScriptSourceEnum.CUSTOM) {
                actionAuditContext = ActionAuditContext.builder(ActionId.QUICK_EXECUTE_SCRIPT)
                    .setEventBuilder(ExecuteJobAuditEventBuilder.class)
                    .setContent(EventContentConstants.QUICK_EXECUTE_SCRIPT)
                    .build();
            } else if (scriptSource == ScriptSourceEnum.QUOTED_APP) {
                actionAuditContext = ActionAuditContext.builder(ActionId.EXECUTE_SCRIPT)
                    .setEventBuilder(ExecuteJobAuditEventBuilder.class)
                    .setContent(EventContentConstants.EXECUTE_SCRIPT)
                    .build();
            } else if (scriptSource == ScriptSourceEnum.QUOTED_PUBLIC) {
                actionAuditContext = ActionAuditContext.builder(ActionId.EXECUTE_PUBLIC_SCRIPT)
                    .setEventBuilder(ExecuteJobAuditEventBuilder.class)
                    .setContent(EventContentConstants.EXECUTE_PUBLIC_SCRIPT)
                    .build();
            } else {
                actionAuditContext = ActionAuditContext.INVALID;
            }
        } else {
            actionAuditContext = ActionAuditContext.INVALID;
        }

        AuditContext.current().updateActionId(actionAuditContext.getActionId());

        return actionAuditContext.wrapActionCallable(() -> executeFastTaskInternal(fastTask)).call();
    }

    private TaskInstanceDTO executeFastTaskInternal(FastTaskDTO fastTask) {
        log.info("Begin to execute fast task: {}", fastTask);

        long appId = fastTask.getTaskInstance().getAppId();
        TaskInstanceDTO taskInstance = fastTask.getTaskInstance();
        StepInstanceDTO stepInstance = fastTask.getStepInstance();

        StopWatch watch = new StopWatch("executeFastTask");
        try {
            watch.start("checkRunningJobQuoteLimit");
            // 检查正在执行的作业配额限制
            checkRunningJobQuotaLimit(appId, taskInstance.getAppCode());
            watch.stop();

            // 检查任务是否应当被驱逐
            checkTaskEvict(taskInstance);

            standardizeStepDynamicGroupId(Collections.singletonList(stepInstance));
            adjustStepTimeout(stepInstance);

            // 设置账号信息
            watch.start("checkAndSetAccountInfo");
            checkAndSetAccountInfo(stepInstance, appId);
            watch.stop();

            // 处理执行对象
            watch.start("processExecuteObjects");
            TaskInstanceExecuteObjects taskInstanceExecuteObjects =
                taskInstanceExecuteObjectProcessor.processExecuteObjects(taskInstance,
                    Collections.singletonList(stepInstance), null);
            watch.stop();

            // 检查步骤
            watch.start("checkStepInstance");
            checkStepInstance(taskInstance, Collections.singletonList(stepInstance));
            watch.stop();

            // 鉴权
            watch.start("authFastExecute");
            authFastExecute(taskInstance, stepInstance, taskInstanceExecuteObjects.getWhiteHostAllowActions());
            watch.stop();

            // 保存作业
            saveTaskInstance(watch, fastTask, taskInstance, stepInstance);

            // 启动作业
            watch.start("startJob");
            startTask(taskInstance.getId());
            watch.stop();

            // 审计
            Set<HostDTO> allHosts = taskInstanceExecuteObjectProcessor.extractHosts(
                Collections.singletonList(stepInstance), null);
            taskInstance.setAllHosts(allHosts);
            auditFastJobExecute(taskInstance);

            // 日志记录容器执行对象的作业，用于统计、分析
            logContainerExecuteObjectJob(taskInstance, taskInstanceExecuteObjects);

            return taskInstance;
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
            if (watch.getTotalTimeMillis() > 1000) {
                log.warn("CreateTaskInstanceFast is slow, statistics: {}", watch.prettyPrint());
            }
        }
    }

    /*
     * 对于包含容器执行对象的作业，输出日志用于统计、分析（后续版本删除）
     */
    private void logContainerExecuteObjectJob(TaskInstanceDTO taskInstance,
                                              TaskInstanceExecuteObjects taskInstanceExecuteObjects) {
        if (taskInstanceExecuteObjects.isContainsAnyContainer()) {
            log.info("ContainerJobRecord -> resourceScope|{}|appCode|{}|jobInstanceId|{}|name|{}",
                GlobalAppScopeMappingService.get().getScopeByAppId(taskInstance.getAppId()).toResourceScopeUniqueId(),
                StringUtils.isNotEmpty(taskInstance.getAppCode()) ? taskInstance.getAppCode() : "None",
                taskInstance.getId(),
                taskInstance.getName()
            );
        }
    }

    /*
     * 检查正在执行的作业配额限制，防止单个业务占用所有的执行引擎调度资源
     */
    private void checkRunningJobQuotaLimit(Long appId, String appCode) {
        ResourceScope resourceScope = GlobalAppScopeMappingService.get().getScopeByAppId(appId);
        ResourceQuotaCheckResultEnum checkResult = runningJobResourceQuotaManager.checkResourceQuotaLimit(
            appCode, resourceScope);
        switch (checkResult) {
            case NO_LIMIT:
                break;
            case RESOURCE_SCOPE_LIMIT:
                log.warn("ResourceQuotaLimit-runningJob exceed resource scope quota limit, resourceScope: {}",
                    resourceScope.toResourceScopeUniqueId());
                throw new RunningJobQuotaLimitExceedException(ErrorCode.RUNNING_JOB_EXCEED_RESOURCE_SCOPE_QUOTA_LIMIT);
            case APP_LIMIT:
                log.warn("ResourceQuotaLimit-runningJob exceed app quota limit, appCode: {}", appCode);
                throw new RunningJobQuotaLimitExceedException(ErrorCode.RUNNING_JOB_EXCEED_APP_QUOTA_LIMIT);
            case SYSTEM_LIMIT:
                log.warn("ResourceQuotaLimit-runningJob exceed system quota limit, resourceScope: {}, appCode: {}",
                    resourceScope.toResourceScopeUniqueId(), appCode);
                throw new RunningJobQuotaLimitExceedException(ErrorCode.RUNNING_JOB_EXCEED_SYSTEM_QUOTA_LIMIT);
        }
    }

    private void saveTaskInstance(StopWatch watch,
                                  FastTaskDTO fastTask,
                                  TaskInstanceDTO taskInstance,
                                  StepInstanceDTO stepInstance) {
        // 保存作业、步骤实例
        watch.start("saveInstance");
        long taskInstanceId = taskInstanceService.addTaskInstance(taskInstance);
        taskInstance.setId(taskInstanceId);

        // 添加作业执行上下文，用于全局共享、传播上下文信息
        addJobInstanceContext(taskInstance);

        stepInstance.setTaskInstanceId(taskInstanceId);
        stepInstance.setStepNum(1);
        stepInstance.setStepOrder(1);
        long stepInstanceId = stepInstanceService.addStepInstance(stepInstance);
        stepInstance.setId(stepInstanceId);
        taskInstance.setStepInstances(Collections.singletonList(stepInstance));
        watch.stop();

        // 保存作业实例与主机的关系，优化根据主机检索作业执行历史的效率
        watch.start("saveTaskInstanceHosts");
        saveTaskInstanceHosts(taskInstance.getAppId(), taskInstanceId, Collections.singletonList(stepInstance));
        watch.stop();

        // 保存滚动配置
        if (fastTask.isRollingEnabled()) {
            watch.start("saveRollingConfig");
            RollingConfigDTO rollingConfig = rollingConfigService.saveRollingConfigForFastJob(fastTask);
            long rollingConfigId = rollingConfig.getId();
            stepInstanceService.updateStepRollingConfigId(taskInstanceId, stepInstanceId, rollingConfigId);
            watch.stop();
        }

        // 记录操作日志
        watch.start("saveOperationLog");
        taskOperationLogService.saveOperationLog(buildTaskOperationLog(taskInstance, taskInstance.getOperator(),
            UserOperationEnum.START));
        watch.stop();

        // 记录到当前正在运行的任务存储中，用于配额限制
        watch.start("addRunningJobResourceQuota");
        runningJobResourceQuotaManager.addJob(
            taskInstance.getAppCode(),
            GlobalAppScopeMappingService.get().getScopeByAppId(taskInstance.getAppId()),
            taskInstanceId
        );
        watch.stop();
    }

    private void addJobInstanceContext(TaskInstanceDTO taskInstance) {
        JobExecuteContext jobExecuteContext = JobExecuteContextThreadLocalRepo.get();
        if (jobExecuteContext != null) {
            JobInstanceContext jobInstanceContext = new JobInstanceContext(taskInstance.getId());
            jobExecuteContext.setJobInstanceContext(jobInstanceContext);
            JobExecuteContextThreadLocalRepo.set(jobExecuteContext);
        }
    }

    private void auditFastJobExecute(TaskInstanceDTO taskInstance) {
        addFastJobExecuteAuditInstance(taskInstance);
        addJobInstanceInfoToExtendData(taskInstance);
    }

    private void addFastJobExecuteAuditInstance(TaskInstanceDTO taskInstance) {
        setHostAuditInstances(taskInstance.getAllHosts());
        // 快速执行任务，只有单个步骤
        StepInstanceDTO stepInstance = taskInstance.getStepInstances().get(0);
        if (stepInstance.getScriptVersionId() != null) {
            ActionAuditContext.current().addAttribute(JobAuditAttributeNames.SCRIPT_VERSION_ID,
                stepInstance.getScriptVersionId());
        }
        if (StringUtils.isNotBlank(stepInstance.getScriptName())) {
            ActionAuditContext.current().addAttribute(JobAuditAttributeNames.SCRIPT_NAME,
                stepInstance.getScriptName());
        }
    }

    private void setHostAuditInstances(Collection<HostDTO> hosts) {
        ActionAuditContext.current()
            .setInstanceIdList(
                AuditInstanceUtils.mapInstanceList(hosts, host -> String.valueOf(host.getHostId()))
            )
            .setInstanceNameList(
                AuditInstanceUtils.mapInstanceList(hosts, HostDTO::getPrimaryIp)
            );
    }

    private void auditJobPlanExecute(TaskInstanceDTO taskInstance) {
        addExecuteJobPlanAuditInstance(taskInstance);
        addJobInstanceInfoToExtendData(taskInstance);
    }

    private void addExecuteJobPlanAuditInstance(TaskInstanceDTO taskInstance) {
        setHostAuditInstances(taskInstance.getAllHosts());
        ActionAuditContext.current()
            .addAttribute(JobAuditAttributeNames.PLAN_ID, taskInstance.getPlanId())
            .addAttribute(JobAuditAttributeNames.PLAN_NAME, taskInstance.getPlan().getName());
    }

    private void addJobInstanceInfoToExtendData(TaskInstanceDTO taskInstance) {
        ActionAuditContext.current()
            .addExtendData(JobAuditExtendDataKeys.JOB_INSTANCE_ID, taskInstance.getId());
    }

    private void saveTaskInstanceHosts(long appId,
                                       long taskInstanceId,
                                       List<StepInstanceDTO> stepInstanceList) {
        Set<HostDTO> stepHosts = taskInstanceExecuteObjectProcessor.extractHosts(stepInstanceList, null);
        saveTaskInstanceHosts(appId, taskInstanceId, stepHosts);
    }

    private void saveTaskInstanceHosts(long appId, long taskInstanceId, Collection<HostDTO> hosts) {
        if (CollectionUtils.isEmpty(hosts)) {
            return;
        }
        taskInstanceService.saveTaskInstanceHosts(appId, taskInstanceId, hosts);
    }

    private void checkTaskEvict(TaskInstanceDTO taskInstance) {
        if (taskEvictPolicyExecutor.shouldEvictTask(taskInstance)) {
            throw new ResourceExhaustedException(ErrorCode.TASK_ABANDONED);
        }
    }

    /**
     * 调整任务超时时间
     *
     * @param stepInstance 步骤
     */
    private void adjustStepTimeout(StepInstanceDTO stepInstance) {
        stepInstance.setTimeout(TimeoutUtils.adjustTaskTimeout(stepInstance.getTimeout()));
    }

    private void checkAndSetAccountInfo(StepInstanceDTO stepInstance,
                                        Long appId) throws ServiceException {
        if (stepInstance.getExecuteType() == EXECUTE_SQL) {
            checkAndSetDbAccountInfo(stepInstance);
        } else {
            checkAndSetOsAccountInfo(stepInstance, appId);
        }
    }

    private void checkAndSetOsAccountInfo(StepInstanceDTO stepInstance, Long appId) {
        //设置系统账号信息
        Long systemAccountId = stepInstance.getAccountId();
        if (systemAccountId != null && systemAccountId > 0) {
            AccountDTO systemAccount = accountService.getAccountPreferCache(systemAccountId, null, null, null);
            if (systemAccount == null) {
                log.warn("System account is not exist, accountId={}", systemAccountId);
                throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST, ArrayUtil.toArray("ID=" + systemAccountId));
            }
            stepInstance.setAccount(systemAccount.getAccount());
            stepInstance.setAccountAlias(systemAccount.getAlias());
        } else if (StringUtils.isNotBlank(stepInstance.getAccountAlias())) {
            //兼容老版本API调用，用户直接传account的场景
            String accountAlias = stepInstance.getAccountAlias();
            AccountDTO systemAccount = accountService.getSystemAccountByAlias(accountAlias, appId);
            if (systemAccount == null) {
                log.warn("System account is not exist, appId={}, accountAlias={}", appId, accountAlias);
                throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST, ArrayUtil.toArray(accountAlias));
            }
            stepInstance.setAccount(systemAccount.getAccount());
            stepInstance.setAccountId(systemAccount.getId());
        } else {
            log.warn("Account is empty!");
            throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST);
        }
        if (stepInstance.isFileStep() && CollectionUtils.isNotEmpty(stepInstance.getFileSourceList())) {
            stepInstance.getFileSourceList().forEach(fileSource -> {
                Long fileSourceAccountId = fileSource.getAccountId();
                String fileSourceAccountAlias = fileSource.getAccountAlias();
                if (fileSourceAccountId != null && fileSourceAccountId > 0) {
                    AccountDTO systemAccount = accountService.getAccountPreferCache(systemAccountId, null, null, null);
                    if (systemAccount == null) {
                        log.warn("System account is not exist, accountId={}", systemAccountId);
                        throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST,
                            ArrayUtil.toArray("ID=" + systemAccountId));
                    }
                    fileSource.setAccountAlias(systemAccount.getAlias());
                    fileSource.setAccount(systemAccount.getAccount());
                } else if (StringUtils.isNotEmpty(fileSourceAccountAlias)) {
                    AccountDTO systemAccount = accountService.getAccountPreferCache(null,
                        AccountCategoryEnum.SYSTEM, fileSourceAccountAlias, appId);
                    if (systemAccount == null) {
                        log.warn("System account is not exist, accountId={}", systemAccountId);
                        throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST,
                            ArrayUtil.toArray("ID=" + systemAccountId));
                    }
                    fileSource.setAccountId(systemAccount.getId());
                    fileSource.setAccount(systemAccount.getAccount());
                }
            });
        }
    }

    private void checkAndSetDbAccountInfo(StepInstanceDTO stepInstance) {
        // 设置DB账号信息
        Long dbAccountId = stepInstance.getDbAccountId();
        if (dbAccountId != null && dbAccountId > 0) {
            AccountDTO dbAccount = accountService.getAccountById(dbAccountId);
            if (dbAccount == null) {
                log.warn("DB account is not exist, accountId={}", dbAccountId);
                throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST);
            }
            stepInstance.setDbAccount(dbAccount.getAccount());
            stepInstance.setDbType(dbAccount.getType().getType());
            stepInstance.setDbPort(dbAccount.getDbPort());
            stepInstance.setDbPass(dbAccount.getDbPassword());

            long systemAccountId = dbAccount.getDbSystemAccountId();
            AccountDTO systemAccount = accountService.getAccountById(systemAccountId);
            if (systemAccount == null) {
                log.warn("DB account dependency system account is not exist, systemAccountId={}", systemAccountId);
                throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST);
            }
            stepInstance.setAccountId(systemAccount.getId());
            stepInstance.setAccount(systemAccount.getAccount());
            stepInstance.setAccountAlias(systemAccount.getAlias());
        } else {
            log.warn("DB account is requested");
            throw new NotFoundException(ErrorCode.ILLEGAL_PARAM);
        }
    }

    private void checkAndSetScript(TaskInstanceDTO taskInstance, StepInstanceDTO stepInstance) {
        long appId = taskInstance.getAppId();
        ServiceScriptDTO script = null;
        if (stepInstance.isScriptStep()) {
            boolean isScriptSpecifiedById = false;
            Long scriptVersionId = stepInstance.getScriptVersionId();
            if (scriptVersionId != null && scriptVersionId > 0) {
                script = scriptService.getScriptByScriptVersionId(stepInstance.getOperator(), appId, scriptVersionId);
                isScriptSpecifiedById = true;
            } else if (StringUtils.isNotEmpty(stepInstance.getScriptId())) {
                script = scriptService.getOnlineScriptVersion(stepInstance.getScriptId());
                isScriptSpecifiedById = true;
            }
            if (isScriptSpecifiedById) {
                checkScriptExist(appId, stepInstance, script);
                checkScriptStatusExecutable(script);

                if (StringUtils.isEmpty(stepInstance.getScriptId())) {
                    stepInstance.setScriptId(script.getId());
                }
                if (stepInstance.getScriptVersionId() == null || stepInstance.getScriptVersionId() < 1) {
                    stepInstance.setScriptVersionId(script.getScriptVersionId());
                }
                stepInstance.setScriptContent(script.getContent());
                stepInstance.setScriptName(script.getName());
                stepInstance.setScriptType(ScriptTypeEnum.valOf(script.getType()));
                if (script.isPublicScript()) {
                    stepInstance.setScriptSource(ScriptSourceEnum.QUOTED_PUBLIC.getValue());
                } else {
                    stepInstance.setScriptSource(ScriptSourceEnum.QUOTED_APP.getValue());
                }
            } else {
                stepInstance.setScriptSource(ScriptSourceEnum.CUSTOM.getValue());
            }
        }
        // 检查高危脚本
        checkScriptMatchDangerousRule(taskInstance, stepInstance);
    }

    private void checkScriptExist(long appId, StepInstanceDTO stepInstance, ServiceScriptDTO script) {
        if (script == null) {
            log.warn("Script is not exist, appId={}, scriptId: {}, scriptVersionId={}", appId,
                stepInstance.getScriptId(), stepInstance.getScriptVersionId());
            throw new NotFoundException(ErrorCode.SCRIPT_NOT_EXIST);
        }
        if (!script.isPublicScript() && !script.getAppId().equals(appId)) {
            log.warn("Script is not exist in app, appId={}, scriptId: {}, scriptVersionId={}",
                stepInstance.getAppId(), stepInstance.getScriptId(), stepInstance.getScriptVersionId());
            throw new NotFoundException(ErrorCode.SCRIPT_NOT_EXIST);
        }
    }

    private void checkScriptStatusExecutable(ServiceScriptDTO script) {
        JobResourceStatusEnum scriptStatus = JobResourceStatusEnum.getJobResourceStatus(script.getStatus());
        // 只有"已上线"和"已下线"的脚本状态可以执行
        if (JobResourceStatusEnum.ONLINE != scriptStatus && JobResourceStatusEnum.OFFLINE != scriptStatus) {
            log.warn("Script status is {}, should not execute! ScriptId: {}, scriptVersionId={}",
                scriptStatus, script.getId(), script.getScriptVersionId());
            throw new FailedPreconditionException(ErrorCode.SCRIPT_NOT_EXECUTABLE_STATUS,
                new String[]{
                    "{" + scriptStatus.getStatusI18nKey() + "}"
                });
        }
    }

    private void checkScriptMatchDangerousRule(TaskInstanceDTO taskInstance, StepInstanceDTO stepInstance) {
        if (!stepInstance.isScriptStep()) {
            return;
        }

        String content = stepInstance.getScriptContent();
        List<ServiceScriptCheckResultItemDTO> checkResultItems =
            dangerousScriptCheckService.check(stepInstance.getScriptType(), content);
        if (CollectionUtils.isNotEmpty(checkResultItems)) {
            String checkResultSummary =
                dangerousScriptCheckService.summaryDangerousScriptCheckResult(stepInstance.getName(), checkResultItems);
            if (StringUtils.isNotBlank(checkResultSummary)) {
                log.info("Script match dangerous rule, checkResult: {}", checkResultItems);
                dangerousScriptCheckService.saveDangerousRecord(taskInstance, stepInstance, checkResultItems);
                if (dangerousScriptCheckService.shouldIntercept(checkResultItems)) {
                    throw new AbortedException(ErrorCode.DANGEROUS_SCRIPT_FORBIDDEN_EXECUTION,
                        ArrayUtil.toArray(checkResultSummary));
                }
            }
        }
    }

    private void batchCheckScriptMatchDangerousRule(TaskInstanceDTO taskInstance,
                                                    List<StepInstanceDTO> stepInstanceList) {
        stepInstanceList.forEach(stepInstance -> checkScriptMatchDangerousRule(taskInstance, stepInstance));
    }

    private void authFastExecute(TaskInstanceDTO taskInstance,
                                 StepInstanceDTO stepInstance,
                                 Map<Long, List<String>> whiteHostAllowActions) {
        AuthResult authResult;
        if (stepInstance.isScriptStep()) {
            // 鉴权脚本任务
            authResult = authExecuteScript(taskInstance, stepInstance, whiteHostAllowActions);
        } else {
            // 鉴权文件任务
            authResult = authFileTransfer(taskInstance, stepInstance, whiteHostAllowActions);
        }

        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
    }

    private AuthResult authExecuteScript(TaskInstanceDTO taskInstance,
                                         StepInstanceDTO stepInstance,
                                         Map<Long, List<String>> whiteHostAllowActions) {
        Long appId = taskInstance.getAppId();
        String username = taskInstance.getOperator();
        Long accountId = null;
        if (StepExecuteTypeEnum.EXECUTE_SCRIPT == stepInstance.getExecuteType()) {
            accountId = stepInstance.getAccountId();
        } else if (StepExecuteTypeEnum.EXECUTE_SQL == stepInstance.getExecuteType()) {
            accountId = stepInstance.getDbAccountId();
        }
        if (accountId == null) {
            return AuthResult.fail();
        }

        AuthResult accountAuthResult = executeAuthService.authAccountExecutable(username,
            new AppResourceScope(appId), accountId);

        AuthResult serverAuthResult;
        ExecuteTargetDTO executeObjects = stepInstance.getTargetExecuteObjects().clone();
        filterHostsDoNotRequireAuth(ActionScopeEnum.SCRIPT_EXECUTE, executeObjects, whiteHostAllowActions);
        if (executeObjects.isEmpty()) {
            // 如果执行对象为空，无需进一步鉴权
            return accountAuthResult;
        }
        ScriptSourceEnum scriptSource = ScriptSourceEnum.getScriptSourceEnum(stepInstance.getScriptSource());
        if (scriptSource == ScriptSourceEnum.CUSTOM) {
            // 快速执行脚本鉴权
            serverAuthResult = executeAuthService.authFastExecuteScript(
                username, new AppResourceScope(appId), executeObjects);
        } else if (scriptSource == ScriptSourceEnum.QUOTED_APP) {
            serverAuthResult = executeAuthService.authExecuteAppScript(
                username, new AppResourceScope(appId), stepInstance.getScriptId(),
                stepInstance.getScriptName(), executeObjects);
        } else if (scriptSource == ScriptSourceEnum.QUOTED_PUBLIC) {
            serverAuthResult = executeAuthService.authExecutePublicScript(
                username, new AppResourceScope(appId), stepInstance.getScriptId(),
                stepInstance.getScriptName(), executeObjects);
        } else {
            serverAuthResult = AuthResult.fail();
        }

        return accountAuthResult.mergeAuthResult(serverAuthResult);
    }

    /**
     * 过滤掉主机白名单的机器
     */
    private void filterHostsDoNotRequireAuth(ActionScopeEnum action,
                                             ExecuteTargetDTO executeTarget,
                                             Map<Long, List<String>> whiteHostAllowActions) {
        if (whiteHostAllowActions == null || whiteHostAllowActions.isEmpty()) {
            return;
        }
        if (CollectionUtils.isNotEmpty(executeTarget.getStaticIpList())) {
            executeTarget.setStaticIpList(executeTarget.getStaticIpList().stream()
                .filter(host -> {
                    List<String> allowedActions = whiteHostAllowActions.get(host.getHostId());
                    boolean skipAuth = allowedActions != null && allowedActions.contains(action.name());
                    if (skipAuth) {
                        log.info("Host: {} is white host, skip auth!", host.toStringBasic());
                    }
                    return !skipAuth;
                })
                .collect(Collectors.toList()));
        }
    }

    private AuthResult authFileTransfer(TaskInstanceDTO taskInstance,
                                        StepInstanceDTO stepInstance,
                                        Map<Long, List<String>> whiteHostAllowActions) {
        String username = taskInstance.getOperator();
        Long appId = taskInstance.getAppId();

        Set<Long> accounts = new HashSet<>();
        accounts.add(stepInstance.getAccountId());
        stepInstance.getFileSourceList().stream()
            .filter(fileSource -> !fileSource.isLocalUpload() && fileSource.getAccountId() != null)
            .forEach(fileSource -> accounts.add(fileSource.getAccountId()));

        AuthResult accountAuthResult = executeAuthService.batchAuthAccountExecutable(
            username, new AppResourceScope(appId), accounts);

        ExecuteTargetDTO executeTarget = stepInstance.getTargetExecuteObjects().clone();
        stepInstance.getFileSourceList().stream()
            .filter(fileSource -> !fileSource.isLocalUpload()
                && fileSource.getFileType() != TaskFileTypeEnum.BASE64_FILE.getType()
                && fileSource.getServers() != null)
            .forEach(fileSource -> executeTarget.merge(fileSource.getServers()));
        filterHostsDoNotRequireAuth(ActionScopeEnum.FILE_DISTRIBUTION, executeTarget, whiteHostAllowActions);
        if (executeTarget.isEmpty()) {
            // 如果主机为空，无需对主机进行权限
            return accountAuthResult;
        }

        AuthResult serverAuthResult = executeAuthService.authFastPushFile(
            username, new AppResourceScope(appId), executeTarget);

        return accountAuthResult.mergeAuthResult(serverAuthResult);
    }

    private void checkStepInstanceExecuteTargetNonEmpty(StepInstanceDTO stepInstance) {
        if (!stepInstance.isStepContainsExecuteObject()) {
            return;
        }
        ExecuteTargetDTO targetExecuteObjects = stepInstance.getTargetExecuteObjects();
        if (targetExecuteObjects == null
            || CollectionUtils.isEmpty(targetExecuteObjects.getExecuteObjectsCompatibly())) {
            log.warn("Empty target execute object! stepInstanceName: {}", stepInstance.getName());
            throw new FailedPreconditionException(ErrorCode.STEP_TARGET_EXECUTE_OBJECT_EMPTY,
                new String[]{stepInstance.getName()});
        }
        if (stepInstance.isFileStep()) {
            List<FileSourceDTO> fileSourceList = stepInstance.getFileSourceList();
            for (FileSourceDTO fileSource : fileSourceList) {
                // 远程文件分发需要判断文件源主机是否为空
                if (TaskFileTypeEnum.SERVER.getType() == fileSource.getFileType()) {
                    ExecuteTargetDTO executeTarget = fileSource.getServers();
                    if (executeTarget == null
                        || CollectionUtils.isEmpty(executeTarget.getExecuteObjectsCompatibly())) {
                        log.warn("Empty file source server, stepInstanceName: {}", stepInstance.getName());
                        throw new FailedPreconditionException(ErrorCode.STEP_SOURCE_EXECUTE_OBJECT_EMPTY,
                            new String[]{stepInstance.getName()});
                    }
                }
            }
        }
    }

    private void checkStepInstance(TaskInstanceDTO taskInstance, List<StepInstanceDTO> stepInstanceList) {
        // 检查步骤引用的执行对象不为空
        stepInstanceList.forEach(this::checkStepInstanceExecuteTargetNonEmpty);
        // 检查步骤的GSE原子任务上限
        checkStepInstanceAtomicTasksLimit(taskInstance, stepInstanceList);
    }

    private void checkStepInstanceAtomicTasksLimit(TaskInstanceDTO taskInstance,
                                                   List<StepInstanceDTO> stepInstanceList) {
        String appCode = taskInstance.getAppCode();
        Long appId = taskInstance.getAppId();
        String taskName = taskInstance.getName();

        for (StepInstanceDTO stepInstance : stepInstanceList) {
            String operator = stepInstance.getOperator();
            if (stepInstance.isFileStep()) {
                int targetServerSize = stepInstance.getTargetExecuteObjectCount();
                int totalSourceFileSize = 0;
                for (FileSourceDTO fileSource : stepInstance.getFileSourceList()) {
                    int sourceServerSize = 1;
                    Integer fileType = fileSource.getFileType();
                    if (fileType == TaskFileTypeEnum.SERVER.getType() && fileSource.getServers() != null) {
                        sourceServerSize = CollectionUtils.size(fileSource.getServers().getExecuteObjectsCompatibly());
                    }
                    int sourceFileSize = CollectionUtils.size(fileSource.getFiles());
                    totalSourceFileSize += sourceServerSize * sourceFileSize;
                }
                int totalFileTaskSize = totalSourceFileSize * targetServerSize;
                if (totalFileTaskSize > 10000) {
                    TASK_MONITOR_LOGGER.info("LargeTask|type:file|taskName:{}|appCode:{}|appId:{}|operator:{}"
                            + "|fileTaskSize:{}",
                        taskName, appCode, appId, operator, totalFileTaskSize);
                }
                if (totalFileTaskSize > jobExecuteConfig.getFileTasksMax()) {
                    log.info("Reject large task|type:file|taskName:{}|appCode:{}|appId:{}|operator" +
                            ":{}|totalFileTaskSize:{}|maxAllowedSize:{}",
                        taskName, appCode, appId, operator, totalFileTaskSize, jobExecuteConfig.getFileTasksMax());
                    throw new AbortedException(ErrorCode.FILE_TASKS_EXCEEDS_LIMIT,
                        new Integer[]{jobExecuteConfig.getFileTasksMax()});
                }
            } else if (stepInstance.isScriptStep()) {
                int targetExecuteObjectSize = stepInstance.getTargetExecuteObjectCount();
                if (targetExecuteObjectSize > 10000) {
                    TASK_MONITOR_LOGGER.info("LargeTask|type:script|taskName:{}|appCode:{}|appId:{}|operator:{}"
                            + "|targetExecuteObjectSize:{}",
                        taskName, appCode, appId, operator, targetExecuteObjectSize);
                }
                if (targetExecuteObjectSize > jobExecuteConfig.getScriptTaskMaxTargetServer()) {
                    log.info("Reject large task|type:file|taskName:{}|appCode:{}|appId:{}|operator" +
                            ":{}|targetExecuteObjectSize:{}|maxAllowedSize:{}", taskName, appCode, appId, operator,
                        targetExecuteObjectSize, jobExecuteConfig.getScriptTaskMaxTargetServer());
                    throw new ResourceExhaustedException(ErrorCode.SCRIPT_TASK_TARGET_SERVER_EXCEEDS_LIMIT,
                        new Integer[]{jobExecuteConfig.getScriptTaskMaxTargetServer()});
                }
            }
        }
    }

    @Override
    public TaskInstanceDTO redoFastTask(FastTaskDTO fastTask) {
        TaskInstanceDTO taskInstance = fastTask.getTaskInstance();
        StepInstanceDTO stepInstance = fastTask.getStepInstance();
        long taskInstanceId = taskInstance.getId();
        if (StringUtils.isNotEmpty(stepInstance.getScriptParam()) && stepInstance.getScriptParam().equals("******")) {
            // 重做快速任务，如果是敏感参数，并且用户未修改脚本参数值(******为与前端的约定，表示用户未修改脚本参数值)，需要从原始任务取值
            StepInstanceDTO originStepInstance = stepInstanceService.getStepInstanceByTaskInstanceId(taskInstanceId);
            if (originStepInstance == null) {
                log.error("Rode task is not exist, taskInstanceId: {}", taskInstanceId);
                throw new NotFoundException(ErrorCode.TASK_INSTANCE_NOT_EXIST);
            }
            stepInstance.setScriptParam(originStepInstance.getScriptParam());
            stepInstance.setSecureParam(originStepInstance.isSecureParam());
        }
        return executeFastTask(fastTask);
    }

    @Override
    public void startTask(long taskInstanceId) {
        log.info("Start task, taskInstanceId={}", taskInstanceId);
        taskExecuteMQEventDispatcher.dispatchJobEvent(JobEvent.startJob(taskInstanceId));
    }

    @Override
    public TaskInstanceDTO executeJobPlan(TaskExecuteParam executeParam) {
        StopWatch watch = new StopWatch("createTaskInstanceForTask");
        TaskInfo taskInfo = buildTaskInfoFromExecuteParam(executeParam, watch);
        ServiceTaskPlanDTO plan = taskInfo.getJobPlan();
        ActionAuditContext actionAuditContext;

        if (plan.isDebugTask()) {
            // 作业模版调试
            actionAuditContext = ActionAuditContext.builder(ActionId.DEBUG_JOB_TEMPLATE)
                .setContent(EventContentConstants.DEBUG_JOB_TEMPLATE)
                .setResourceType(ResourceTypeId.TEMPLATE)
                .setInstanceId(String.valueOf(plan.getTaskTemplateId()))
                .setInstanceName(taskTemplateResource.getTemplateNameById(plan.getTaskTemplateId()).getData())
                .build();
        } else {
            // 执行方案执行
            actionAuditContext = ActionAuditContext.builder(ActionId.LAUNCH_JOB_PLAN)
                .setEventBuilder(ExecuteJobAuditEventBuilder.class)
                .setContent(EventContentConstants.LAUNCH_JOB_PLAN)
                .build();
        }

        AuditContext.current().updateActionId(actionAuditContext.getActionId());

        return actionAuditContext.wrapActionCallable(() -> {
            TaskInstanceDTO taskInstance = executeJobPlanInternal(watch, executeParam, taskInfo);
            if (!plan.isDebugTask()) {
                auditJobPlanExecute(taskInstance);
            } else {
                addJobInstanceInfoToExtendData(taskInstance);
            }
            return taskInstance;
        }).call();
    }

    private TaskInstanceDTO executeJobPlanInternal(StopWatch watch, TaskExecuteParam executeParam, TaskInfo taskInfo) {
        try {
            // 检查正在执行的作业配额限制
            TaskInstanceDTO taskInstance = taskInfo.getTaskInstance();
            watch.start("checkRunningJobQuoteLimit");
            checkRunningJobQuotaLimit(taskInstance.getAppId(), taskInstance.getAppCode());
            watch.stop();

            ServiceTaskPlanDTO jobPlan = taskInfo.getJobPlan();
            taskInstance.setPlan(jobPlan);
            List<StepInstanceDTO> stepInstanceList = taskInfo.getStepInstances();
            taskInstance.setStepInstances(stepInstanceList);
            Map<String, TaskVariableDTO> finalVariableValueMap = taskInfo.getVariables();


            // 调整超时时间
            stepInstanceList.forEach(this::adjustStepTimeout);

            // 检查高危脚本
            watch.start("checkDangerousScript");
            batchCheckScriptMatchDangerousRule(taskInstance, stepInstanceList);
            watch.stop();

            // 处理执行对象
            watch.start("processExecuteObjects");
            TaskInstanceExecuteObjects taskInstanceExecuteObjects =
                taskInstanceExecuteObjectProcessor.processExecuteObjects(taskInstance, stepInstanceList,
                    finalVariableValueMap.values());
            watch.stop();

            // 检查步骤
            watch.start("checkStepInstance");
            checkStepInstance(taskInstance, stepInstanceList);
            watch.stop();

            if (!executeParam.isSkipAuth()) {
                watch.start("auth-execute-job");
                authExecuteJobPlan(executeParam.getOperator(), executeParam.getAppId(), jobPlan, stepInstanceList,
                    taskInstanceExecuteObjects.getWhiteHostAllowActions());
                watch.stop();
            }

            watch.start("saveInstance");
            // 这里保存的stepInstanceList已经是完成变量解析之后的步骤信息了
            saveTaskInstance(taskInstance, stepInstanceList, finalVariableValueMap);
            watch.stop();

            Set<HostDTO> allHosts = taskInstanceExecuteObjectProcessor.extractHosts(
                taskInstance.getStepInstances(), null);
            taskInstance.setAllHosts(allHosts);

            // 保存作业实例与主机的关系，优化根据主机检索作业执行历史的效率
            watch.start("saveTaskInstanceHosts");
            saveTaskInstanceHosts(taskInstance.getAppId(), taskInstance.getId(), allHosts);
            watch.stop();

            watch.start("saveOperationLog");
            taskOperationLogService.saveOperationLog(buildTaskOperationLog(taskInstance, taskInstance.getOperator(),
                UserOperationEnum.START));
            watch.stop();

            // 启动作业
            watch.start("startJob");
            startTask(taskInstance.getId());
            watch.stop();

            // 日志记录容器执行对象的作业，用于统计、分析
            logContainerExecuteObjectJob(taskInstance, taskInstanceExecuteObjects);

            return taskInstance;
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
            if (watch.getTotalTimeMillis() > 1000) {
                log.warn("createTaskInstanceForTask is slow, statistics: {}", watch.prettyPrint());
            }
        }
    }

    private void standardizeStepDynamicGroupId(List<StepInstanceDTO> stepInstanceList) {
        for (StepInstanceDTO stepInstance : stepInstanceList) {
            if (stepInstance.getTargetExecuteObjects() != null
                && CollectionUtils.isNotEmpty(stepInstance.getTargetExecuteObjects().getDynamicServerGroups())) {
                standardizeServerDynamicGroupId(stepInstance.getTargetExecuteObjects());
            }
            if (CollectionUtils.isNotEmpty(stepInstance.getFileSourceList())) {
                for (FileSourceDTO fileSource : stepInstance.getFileSourceList()) {
                    if (fileSource.getServers() != null &&
                        CollectionUtils.isNotEmpty(fileSource.getServers().getDynamicServerGroups())) {
                        standardizeServerDynamicGroupId(fileSource.getServers());
                    }
                }
            }
        }
    }

    private void standardizeServerDynamicGroupId(ExecuteTargetDTO executeTarget) {
        if (executeTarget != null && CollectionUtils.isNotEmpty(executeTarget.getDynamicServerGroups())) {
            executeTarget.getDynamicServerGroups().forEach(this::standardizeDynamicGroupId);
        }
    }

    private void standardizeTaskVarDynamicGroupId(Collection<TaskVariableDTO> variables) {
        if (CollectionUtils.isNotEmpty(variables)) {
            variables.stream().filter(variable -> variable.getExecuteTarget() != null)
                .forEach(variable -> standardizeServerDynamicGroupId(variable.getExecuteTarget()));
        }
    }

    private void standardizeDynamicGroupId(DynamicServerGroupDTO dynamicGroup) {
        // 移除动态分组ID中多余的bizId(历史问题)
        // bizId:groupId
        String[] bizIdAndGroupId = dynamicGroup.getGroupId().split(":");
        if (bizIdAndGroupId.length == 2) {
            log.info("Found invalid dynamicGroupId, try to transform to standard format! dynamicGroupId: {}",
                dynamicGroup.getGroupId());
            dynamicGroup.setGroupId(bizIdAndGroupId[1]);
        }
    }

    private TaskInfo buildTaskInfoFromExecuteParam(TaskExecuteParam executeParam, StopWatch watch) {
        Long appId = executeParam.getAppId();
        Long planId = executeParam.getPlanId();
        String operator = executeParam.getOperator();
        log.info("Create task instance for task, appId={}, planId={}, operator={}, variables={}", appId, planId,
            operator, executeParam.getExecuteVariableValues());
        watch.start("getPlan");
        ServiceTaskPlanDTO taskPlan = taskPlanService.getPlanById(appId, planId);
        if (taskPlan == null) {
            log.warn("Create task instance for task, task plan is not exist.appId={}, planId={}", appId, planId);
            throw new NotFoundException(ErrorCode.EXECUTE_TASK_PLAN_NOT_EXIST);
        }
        watch.stop();

        watch.start("buildInstance");
        TaskInstanceDTO taskInstance = buildTaskInstanceForTask(executeParam, taskPlan);

        List<TaskVariableDTO> planDefaultVariables = convertToCommonVariables(taskPlan.getVariableList());
        Map<String, TaskVariableDTO> finalVariableValueMap = buildFinalTaskVariableValues(planDefaultVariables
            , executeParam.getExecuteVariableValues());
        standardizeTaskVarDynamicGroupId(finalVariableValueMap.values());
        log.info("Final variable={}", JsonUtils.toJson(finalVariableValueMap));

        if (taskPlan.getStepList() == null || taskPlan.getStepList().isEmpty()) {
            log.warn("Task plan step is empty! planId={}", planId);
            throw new InternalException(ErrorCode.EXECUTE_TASK_PLAN_ILLEGAL);
        }

        List<StepInstanceDTO> stepInstanceList = new ArrayList<>();
        for (ServiceTaskStepDTO step : taskPlan.getStepList()) {
            StepExecuteTypeEnum executeType = getExecuteTypeFromTaskStepType(step);
            StepInstanceDTO stepInstance = createCommonStepInstanceDTO(appId, operator, step.getId(),
                step.getName(), executeType);
            TaskStepTypeEnum stepType = TaskStepTypeEnum.valueOf(step.getType());
            switch (stepType) {
                case SCRIPT:
                    // 解析全局变量，放到targetServers中，进一步解析节点、动态分组对应的主机统一放到ipList中
                    parseScriptStepInstanceFromPlanStep(stepInstance, step, finalVariableValueMap);
                    break;
                case FILE:
                    parseFileStepInstanceFromPlanStep(stepInstance, step, finalVariableValueMap);
                    break;
                case APPROVAL:
                    parseManualConfirmStepInstance(stepInstance, step.getApprovalStepInfo());
                    break;
            }
            stepInstanceList.add(stepInstance);
        }
        standardizeStepDynamicGroupId(stepInstanceList);
        watch.stop();
        return new TaskInfo(taskInstance, stepInstanceList, finalVariableValueMap, taskPlan);
    }

    private void authExecuteJobPlan(String username, long appId, ServiceTaskPlanDTO plan,
                                    List<StepInstanceDTO> stepInstanceList,
                                    Map<Long, List<String>> whiteHostAllowActions) throws PermissionDeniedException {
        boolean needAuth = stepInstanceList.stream()
            .anyMatch(stepInstance -> stepInstance.isScriptStep() || stepInstance.isFileStep());
        if (!needAuth) {
            return;
        }

        boolean isDebugTask = plan.isDebugTask();
        ExecuteTargetDTO authServers = new ExecuteTargetDTO();
        Set<Long> accountIds = new HashSet<>();
        for (StepInstanceDTO stepInstance : stepInstanceList) {
            if (!stepInstance.isScriptStep() && !stepInstance.isFileStep()) {
                continue;
            }
            Pair<ExecuteTargetDTO, Set<Long>> needAuthHostsAndAccounts =
                extractNeedAuthHostsAndAccounts(stepInstance, whiteHostAllowActions);
            authServers = needAuthHostsAndAccounts.getLeft();
            accountIds = needAuthHostsAndAccounts.getRight();
        }

        // 账号使用鉴权
        AuthResult accountAuthResult = executeAuthService.batchAuthAccountExecutable(
            username, new AppResourceScope(appId), accountIds);

        AuthResult authResult;
        if (authServers.isEmpty()) {
            log.info("Required auth servers is empty, authServers: {}", authServers);
            // 主机为空，无需对主机鉴权
            authResult = accountAuthResult;
        } else {
            AuthResult serverAuthResult;
            if (isDebugTask) {
                // 鉴权模板调试
                serverAuthResult = executeAuthService.authDebugTemplate(
                    username, new AppResourceScope(appId), plan.getTaskTemplateId(), authServers);
            } else {
                // 鉴权执行方案的执行
                serverAuthResult = executeAuthService.authExecutePlan(
                    username, new AppResourceScope(appId), plan.getTaskTemplateId(),
                    plan.getId(), plan.getName(), authServers);
            }
            authResult = accountAuthResult.mergeAuthResult(serverAuthResult);
        }

        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
    }

    private Pair<ExecuteTargetDTO, Set<Long>> extractNeedAuthHostsAndAccounts
        (
            StepInstanceDTO stepInstance,
            Map<Long, List<String>> whiteHostAllowActions
        ) {

        ExecuteTargetDTO authServers = new ExecuteTargetDTO();
        Set<Long> accountIds = new HashSet<>();
        accountIds.add(stepInstance.getAccountId());
        if (stepInstance.isFileStep()) {
            ExecuteTargetDTO stepTargetServers = stepInstance.getTargetExecuteObjects().clone();
            filterHostsDoNotRequireAuth(ActionScopeEnum.FILE_DISTRIBUTION, stepTargetServers,
                whiteHostAllowActions);
            authServers.merge(stepTargetServers);
            if (!CollectionUtils.isEmpty(stepInstance.getFileSourceList())) {
                stepInstance.getFileSourceList().stream()
                    .filter(fileSource -> !fileSource.isLocalUpload())
                    .forEach(fileSource -> {
                            if (fileSource.getServers() == null) {
                                return;
                            }
                            ExecuteTargetDTO stepFileSourceServers = fileSource.getServers().clone();
                            filterHostsDoNotRequireAuth(ActionScopeEnum.FILE_DISTRIBUTION, stepFileSourceServers,
                                whiteHostAllowActions);
                            authServers.merge(stepFileSourceServers);
                            if (fileSource.getAccountId() != null) {
                                accountIds.add(fileSource.getAccountId());
                            }
                        }
                    );
            }
        } else if (stepInstance.isScriptStep()) {
            ExecuteTargetDTO stepTargetServers = stepInstance.getTargetExecuteObjects().clone();
            filterHostsDoNotRequireAuth(ActionScopeEnum.SCRIPT_EXECUTE, stepTargetServers, whiteHostAllowActions);
            authServers.merge(stepTargetServers);
        }
        return Pair.of(authServers, accountIds);
    }

    private void authRedoJob(String username, long appId, TaskInstanceDTO taskInstance,
                             Map<Long, List<String>> whiteHostAllowActions) {
        Integer taskType = taskInstance.getType();
        if (taskType.equals(TaskTypeEnum.NORMAL.getValue())
            && taskInstance.getPlanId() != null
            && taskInstance.getPlanId() > 0) {
            // 作业鉴权
            ServiceTaskPlanDTO serviceTaskPlanDTO = taskPlanService.getPlanById(appId, taskInstance.getPlanId());
            if (serviceTaskPlanDTO == null) {
                log.warn("auth redo task instance for task, task plan is not exist.appId={}, planId={}", appId,
                    taskInstance.getPlanId());
                throw new NotFoundException(ErrorCode.TASK_PLAN_NOT_EXIST);
            }
            authExecuteJobPlan(username, appId, serviceTaskPlanDTO, taskInstance.getStepInstances(),
                whiteHostAllowActions);
        } else if (taskType.equals(TaskTypeEnum.SCRIPT.getValue())) {
            // 快速执行脚本鉴权
            StepInstanceDTO scriptStepInstance = taskInstance.getStepInstances().get(0);
            Long scriptVersionId = scriptStepInstance.getScriptVersionId();
            if (scriptVersionId != null && scriptVersionId > 0) {
                ServiceScriptDTO script = scriptService.getScriptByScriptVersionId(scriptVersionId);
                if (script != null) {
                    scriptStepInstance.setScriptName(script.getName());
                }
            }
            authFastExecute(taskInstance, scriptStepInstance, whiteHostAllowActions);
        } else if (taskType.equals(TaskTypeEnum.FILE.getValue())) {
            // 快速分发文件鉴权
            StepInstanceDTO fileStepInstance = taskInstance.getStepInstances().get(0);
            authFastExecute(taskInstance, fileStepInstance, whiteHostAllowActions);
        } else {
            log.warn("Auth fail because of invalid task type!");
            throw new PermissionDeniedException(AuthResult.fail());
        }
    }

    private StepExecuteTypeEnum getExecuteTypeFromTaskStepType(ServiceTaskStepDTO step) throws ServiceException {
        StepExecuteTypeEnum executeType = null;
        TaskStepTypeEnum stepType = TaskStepTypeEnum.valueOf(step.getType());
        switch (stepType) {
            case SCRIPT:
                ScriptTypeEnum scriptType = ScriptTypeEnum.valOf(step.getScriptStepInfo().getType());
                if (scriptType == ScriptTypeEnum.SQL) {
                    executeType = EXECUTE_SQL;
                } else {
                    executeType = EXECUTE_SCRIPT;
                }
                break;
            case FILE:
                executeType = SEND_FILE;
                break;
            case APPROVAL:
                executeType = MANUAL_CONFIRM;
                break;
        }
        return executeType;
    }

    private TaskInstanceDTO buildTaskInstanceForTask(TaskExecuteParam executeParam, ServiceTaskPlanDTO taskPlan) {
        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setAppId(executeParam.getAppId());
        taskInstance.setType(TaskTypeEnum.NORMAL.getValue());
        taskInstance.setStartupMode(executeParam.getStartupMode().getValue());
        if (executeParam.getStartupMode() == TaskStartupModeEnum.CRON) {
            taskInstance.setCronTaskId(executeParam.getCronTaskId());
        } else {
            taskInstance.setCronTaskId(-1L);
        }
        taskInstance.setStatus(RunStatusEnum.BLANK);
        taskInstance.setCreateTime(DateUtils.currentTimeMillis());
        taskInstance.setOperator(executeParam.getOperator());
        String taskName = StringUtils.isBlank(executeParam.getTaskName()) ? taskPlan.getName() :
            executeParam.getTaskName();
        taskInstance.setName(taskName);
        taskInstance.setPlanId(taskPlan.getId());
        taskInstance.setTaskTemplateId(taskPlan.getTaskTemplateId());
        taskInstance.setCurrentStepInstanceId(-1L);
        taskInstance.setDebugTask(taskPlan.isDebugTask());
        taskInstance.setCallbackUrl(executeParam.getCallbackUrl());
        taskInstance.setAppCode(executeParam.getAppCode());
        return taskInstance;
    }

    private StepInstanceDTO createCommonStepInstanceDTO(long appId, String operator, long stepId, String stepName,
                                                        StepExecuteTypeEnum stepType) {
        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setStepId(stepId);
        stepInstance.setName(stepName);
        stepInstance.setExecuteType(stepType);
        stepInstance.setStatus(RunStatusEnum.BLANK);
        stepInstance.setOperator(operator);
        stepInstance.setAppId(appId);
        stepInstance.setCreateTime(DateUtils.currentTimeMillis());
        stepInstance.setExecuteCount(0);
        return stepInstance;
    }

    public TaskInstanceDTO redoJob(Long appId,
                                   Long taskInstanceId,
                                   String operator,
                                   List<TaskVariableDTO> executeVariableValues) {
        log.info("Create task instance for redo, appId={}, taskInstanceId={}, operator={}, variables={}", appId,
            taskInstanceId, operator, executeVariableValues);
        TaskInstanceDTO originTaskInstance = taskInstanceService.getTaskInstanceDetail(taskInstanceId);
        if (originTaskInstance == null) {
            log.warn("Create task instance for redo, task instance is not exist.appId={}, planId={}", appId,
                taskInstanceId);
            throw new NotFoundException(ErrorCode.TASK_INSTANCE_NOT_EXIST);
        }

        TaskInstanceDTO taskInstance = createTaskInstanceForRedo(originTaskInstance, operator);

        Map<String, TaskVariableDTO> finalVariableValueMap = buildFinalTaskVariableValues(
            originTaskInstance.getVariables(), executeVariableValues);
        log.info("Final variable={}", finalVariableValueMap);

        if (originTaskInstance.getStepInstances() == null || originTaskInstance.getStepInstances().isEmpty()) {
            log.warn("Task instance step is empty! taskInstanceId={}", taskInstanceId);
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }

        List<StepInstanceDTO> stepInstanceList = new ArrayList<>();
        for (StepInstanceDTO originStepInstance : originTaskInstance.getStepInstances()) {
            StepExecuteTypeEnum executeType = originStepInstance.getExecuteType();
            StepInstanceDTO stepInstance = createCommonStepInstanceDTO(appId, operator,
                originStepInstance.getStepId(), originStepInstance.getName(), executeType);
            TaskStepTypeEnum stepType = StepTypeExecuteTypeConverter.convertToStepType(executeType);
            switch (stepType) {
                case SCRIPT:
                    parseScriptStepInstanceFromStepInstance(stepInstance, originStepInstance, finalVariableValueMap);
                    break;
                case FILE:
                    parseFileStepInstanceFromStepInstance(stepInstance, originStepInstance, finalVariableValueMap);
                    break;
                case APPROVAL:
                    parseManualConfirmStepInstance(stepInstance, originStepInstance);
                    break;
            }
            stepInstanceList.add(stepInstance);
        }

        // 检查高危脚本
        batchCheckScriptMatchDangerousRule(taskInstance, stepInstanceList);

        // 处理执行对象
        TaskInstanceExecuteObjects taskInstanceExecuteObjects =
            taskInstanceExecuteObjectProcessor.processExecuteObjects(taskInstance, stepInstanceList,
                finalVariableValueMap.values());

        // 检查步骤
        checkStepInstance(taskInstance, stepInstanceList);

        authRedoJob(operator, appId, originTaskInstance, taskInstanceExecuteObjects.getWhiteHostAllowActions());

        saveTaskInstance(taskInstance, stepInstanceList, finalVariableValueMap);

        saveTaskInstanceHosts(taskInstance.getAppId(), taskInstance.getId(), taskInstance.getStepInstances());

        taskOperationLogService.saveOperationLog(buildTaskOperationLog(taskInstance, taskInstance.getOperator(),
            UserOperationEnum.START));

        // 启动作业
        startTask(taskInstance.getId());

        return taskInstance;
    }

    private TaskInstanceDTO createTaskInstanceForRedo(TaskInstanceDTO originTaskInstance, String operator) {
        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setAppId(originTaskInstance.getAppId());
        taskInstance.setType(originTaskInstance.getType());
        taskInstance.setStartupMode(TaskStartupModeEnum.WEB.getValue());
        taskInstance.setCronTaskId(-1L);
        taskInstance.setStatus(RunStatusEnum.BLANK);
        taskInstance.setCreateTime(DateUtils.currentTimeMillis());
        taskInstance.setOperator(operator);
        taskInstance.setName(originTaskInstance.getName());
        taskInstance.setPlanId(originTaskInstance.getPlanId());
        taskInstance.setTaskTemplateId(originTaskInstance.getTaskTemplateId());
        taskInstance.setCurrentStepInstanceId(-1L);
        taskInstance.setDebugTask(false);
        return taskInstance;
    }

    private void saveTaskInstance(TaskInstanceDTO taskInstance,
                                  List<StepInstanceDTO> stepInstances,
                                  Map<String, TaskVariableDTO> taskVariablesMap) {
        // 保存TaskInstance
        long newTaskInstanceId = taskInstanceService.addTaskInstance(taskInstance);
        taskInstance.setId(newTaskInstanceId);
        stepInstances.forEach(stepInstanceDTO -> stepInstanceDTO.setTaskInstanceId(newTaskInstanceId));
        int stepNum = stepInstances.size();
        int stepOrder = 1;
        for (StepInstanceDTO stepInstance : stepInstances) {
            stepInstance.setStepNum(stepNum);
            stepInstance.setStepOrder(stepOrder++);
            // 保存StepInstance
            long stepInstanceId = stepInstanceService.addStepInstance(stepInstance);
            stepInstance.setId(stepInstanceId);
        }
        taskInstance.setStepInstances(stepInstances);

        if (taskVariablesMap != null && !taskVariablesMap.isEmpty()) {
            List<TaskVariableDTO> taskVariables = new ArrayList<>();
            for (TaskVariableDTO taskVariable : taskVariablesMap.values()) {
                taskVariable.setTaskInstanceId(newTaskInstanceId);
                taskVariables.add(taskVariable);
            }
            taskInstanceVariableService.saveTaskInstanceVariables(taskVariables);
            taskInstance.setVariables(taskVariables);
        }

        // 记录到当前正在运行的任务存储中，用于配额限制
        runningJobResourceQuotaManager.addJob(
            taskInstance.getAppCode(),
            GlobalAppScopeMappingService.get().getScopeByAppId(taskInstance.getAppId()),
            taskInstance.getId()
        );
        log.info("Save taskInstance successfully! taskInstanceId: {}", taskInstance.getId());
    }

    private List<TaskVariableDTO> convertToCommonVariables(List<ServiceTaskVariableDTO> variables) {
        List<TaskVariableDTO> commonVariables = new ArrayList<>();
        if (variables == null) {
            return commonVariables;
        }
        variables.forEach(variable -> {
            TaskVariableDTO commonVariable = new TaskVariableDTO();
            commonVariable.setId(variable.getId());
            commonVariable.setName(variable.getName());
            commonVariable.setValue(variable.getDefaultValue());
            if (variable.getType().equals(TaskVariableTypeEnum.HOST_LIST.getType())) {
                commonVariable.setExecuteTarget(convertToServersDTO(variable.getDefaultTargetValue()));
            }
            if (variable.getType().equals(TaskVariableTypeEnum.NAMESPACE.getType())) {
                commonVariable.setChangeable(true);
            } else {
                commonVariable.setChangeable(variable.isChangeable());
            }
            commonVariable.setRequired(variable.isRequired());
            commonVariable.setType(variable.getType());
            commonVariables.add(commonVariable);
        });
        return commonVariables;
    }

    /*
     * 计算作业引用的全局变量的值
     */
    private Map<String, TaskVariableDTO> buildFinalTaskVariableValues(
        List<TaskVariableDTO> defaultVariableValues,
        List<TaskVariableDTO> executeVariableValues
    ) throws ServiceException {
        Map<String, TaskVariableDTO> finalVariableValueMap = new HashMap<>();
        if (defaultVariableValues != null && !defaultVariableValues.isEmpty()) {
            // 执行时候设置的全局变量值;需要同时支持根据id/name匹配
            Map<Long, TaskVariableDTO> idKeyExecuteVariableValueMap = new HashMap<>();
            Map<String, TaskVariableDTO> nameKeyExecuteVariableValueMap = new HashMap<>();
            if (executeVariableValues != null) {
                for (TaskVariableDTO taskVariableDTO : executeVariableValues) {
                    if (taskVariableDTO.getId() != null) {
                        idKeyExecuteVariableValueMap.put(taskVariableDTO.getId(), taskVariableDTO);
                    } else if (StringUtils.isNotBlank(taskVariableDTO.getName())) {
                        nameKeyExecuteVariableValueMap.put(taskVariableDTO.getName(), taskVariableDTO);
                    }
                }
            }

            for (TaskVariableDTO defaultTaskVariable : defaultVariableValues) {
                // 最终使用的全局变量
                TaskVariableDTO finalTaskVariable = new TaskVariableDTO();
                finalTaskVariable.setId(defaultTaskVariable.getId());
                finalTaskVariable.setName(defaultTaskVariable.getName());
                finalTaskVariable.setType(defaultTaskVariable.getType());
                finalTaskVariable.setChangeable(defaultTaskVariable.isChangeable());
                finalTaskVariable.setRequired(defaultTaskVariable.isRequired());

                // 如果执行时赋予了变量值，优先使用执行时赋予的变量值
                if (idKeyExecuteVariableValueMap.containsKey(defaultTaskVariable.getId())) {
                    TaskVariableDTO executeVariableValue =
                        idKeyExecuteVariableValueMap.get(defaultTaskVariable.getId());
                    grantValueForVariable(finalTaskVariable, executeVariableValue);
                } else if (nameKeyExecuteVariableValueMap.containsKey(defaultTaskVariable.getName())) {
                    TaskVariableDTO executeVariableValue =
                        nameKeyExecuteVariableValueMap.get(defaultTaskVariable.getName());
                    grantValueForVariable(finalTaskVariable, executeVariableValue);
                } else {
                    // 否则，使用变量的默认值
                    grantValueForVariable(finalTaskVariable, defaultTaskVariable);
                }
                finalVariableValueMap.put(finalTaskVariable.getName(), finalTaskVariable);
            }
        }
        return finalVariableValueMap;
    }

    private void grantValueForVariable(TaskVariableDTO to, TaskVariableDTO from) {
        if (TaskVariableTypeEnum.HOST_LIST.getType() == to.getType()) {
            to.setExecuteTarget(from.getExecuteTarget());
        } else {
            to.setValue(from.getValue());
        }
    }

    private void parseScriptStepInstanceFromPlanStep(StepInstanceDTO stepInstance, ServiceTaskStepDTO step,
                                                     Map<String, TaskVariableDTO> variableValueMap) {
        ServiceTaskScriptStepDTO scriptStepInfo = step.getScriptStepInfo();
        Long scriptVersionId = step.getScriptStepInfo().getScriptVersionId();
        String scriptId = step.getScriptStepInfo().getScriptId();
        if (scriptVersionId != null) {
            if (step.getScriptStepInfo().getScriptStatus().equals(JobResourceStatusEnum.DISABLED.getValue())) {
                log.warn("Script is disabled, should not execute! appId={}, scriptVersionId={}",
                    stepInstance.getAppId(), scriptVersionId);
                throw new FailedPreconditionException(ErrorCode.SCRIPT_DISABLED_SHOULD_NOT_EXECUTE);
            }
            stepInstance.setScriptVersionId(scriptVersionId);
            stepInstance.setScriptId(scriptId);
        }
        stepInstance.setScriptContent(scriptStepInfo.getContent());
        stepInstance.setScriptParam(scriptStepInfo.getScriptParam());
        if (scriptStepInfo.getScriptTimeout() != null) {
            stepInstance.setTimeout(scriptStepInfo.getScriptTimeout().intValue());
        } else {
            stepInstance.setTimeout(1000);
        }
        stepInstance.setSecureParam(scriptStepInfo.getSecureParam() != null && scriptStepInfo.getSecureParam());
        stepInstance.setScriptType(ScriptTypeEnum.valOf(scriptStepInfo.getType()));
        stepInstance.setScriptSource(scriptStepInfo.getScriptSource());

        ServiceAccountDTO accountInfo = scriptStepInfo.getAccount();
        if (accountInfo == null) {
            log.warn("Account is null! step_id:{}", step.getId());
            throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST);
        }

        ScriptTypeEnum scriptType = ScriptTypeEnum.valOf(scriptStepInfo.getType());
        stepInstance.setAccountId(accountInfo.getId());
        if (scriptType == ScriptTypeEnum.SQL) {
            stepInstance.setAccountId(accountInfo.getDbSystemAccount().getId());
            stepInstance.setAccount(accountInfo.getDbSystemAccount().getAccount());
            stepInstance.setDbAccountId(accountInfo.getId());
            stepInstance.setDbAccount(accountInfo.getAccount());
            stepInstance.setDbType(accountInfo.getType());
            stepInstance.setDbPort(accountInfo.getDbPort());
            stepInstance.setDbPass(accountInfo.getDbPassword());
        } else {
            stepInstance.setAccountId(accountInfo.getId());
            stepInstance.setAccount(accountInfo.getAccount());
            stepInstance.setWindowsInterpreter(scriptStepInfo.getWindowsInterpreter());
        }

        ServiceTaskTargetDTO target = scriptStepInfo.getExecuteTarget();
        ExecuteTargetDTO targetServers = buildFinalTargetServers(target, variableValueMap);
        stepInstance.setTargetExecuteObjects(targetServers);

        stepInstance.setIgnoreError(scriptStepInfo.getIgnoreError());
    }

    private void parseFileStepInstanceFromPlanStep(StepInstanceDTO stepInstance, ServiceTaskStepDTO step,
                                                   Map<String, TaskVariableDTO> variableValueMap) {
        ServiceTaskFileStepDTO fileStepInfo = step.getFileStepInfo();

        ServiceAccountDTO accountInfo = step.getFileStepInfo().getAccount();
        if (accountInfo == null) {
            log.warn("Account is null! step_id:{}", step.getId());
            throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST);
        }
        stepInstance.setAccountId(fileStepInfo.getAccount().getId());
        stepInstance.setAccount(fileStepInfo.getAccount().getAccount());
        stepInstance.setFileTargetPath(fileStepInfo.getDestinationFileLocation());

        List<ServiceTaskFileInfoDTO> originFileList = fileStepInfo.getOriginFileList();
        List<FileSourceDTO> fileSources = new ArrayList<>();
        for (ServiceTaskFileInfoDTO originFile : originFileList) {
            FileSourceDTO fileSource = new FileSourceDTO();
            fileSource.setFileType(originFile.getFileType());
            boolean isLocalUploadFile = originFile.getFileType() == TaskFileTypeEnum.LOCAL.getType();
            fileSource.setLocalUpload(isLocalUploadFile);
            if (originFile.getFileType() == TaskFileTypeEnum.LOCAL.getType()) {
                // 本地文件分发默认使用root账户
                fileSource.setLocalUpload(true);
                fileSource.setAccount("root");
                if (originFile.getFileLocation() != null && !originFile.getFileLocation().isEmpty()) {
                    List<FileDetailDTO> fileList = new ArrayList<>();
                    fileList.add(new FileDetailDTO(true, originFile.getFileLocation().get(0),
                        originFile.getFileHash(), originFile.getFileSize()));
                    fileSource.setFiles(fileList);
                }
            } else if (originFile.getFileType() == TaskFileTypeEnum.SERVER.getType()) {
                fileSource.setLocalUpload(false);
                ServiceAccountDTO accountDTO = originFile.getAccount();
                if (accountDTO != null) {
                    fileSource.setAccount(accountDTO.getAccount());
                    fileSource.setAccountId(accountDTO.getId());
                }
                ServiceTaskTargetDTO target = originFile.getExecuteTarget();
                ExecuteTargetDTO targetServers = buildFinalTargetServers(target, variableValueMap);
                fileSource.setServers(targetServers);
                List<FileDetailDTO> fileList = new ArrayList<>();
                originFile.getFileLocation().forEach(fileLocation -> fileList.add(new FileDetailDTO(fileLocation)));
                fileSource.setFiles(fileList);
            } else if (originFile.getFileType() == TaskFileTypeEnum.FILE_SOURCE.getType()) {
                fileSource.setLocalUpload(false);
                // 文件源文件只需要fileSourceId与文件路径
                List<FileDetailDTO> fileList = new ArrayList<>();
                originFile.getFileLocation().forEach(fileLocation -> fileList.add(new FileDetailDTO(fileLocation)));
                fileSource.setFiles(fileList);
                fileSource.setFileSourceId(originFile.getFileSourceId());
            }
            fileSources.add(fileSource);
        }
        stepInstance.setFileSourceList(fileSources);

        ServiceTaskTargetDTO target = fileStepInfo.getExecuteTarget();
        ExecuteTargetDTO targetServers = buildFinalTargetServers(target, variableValueMap);
        stepInstance.setTargetExecuteObjects(targetServers);

        if (fileStepInfo.getDownloadSpeedLimit() != null) {
            // MB->KB
            stepInstance.setFileDownloadSpeedLimit(
                DataSizeConverter.convertMBToKB(fileStepInfo.getDownloadSpeedLimit()));
        }
        if (fileStepInfo.getUploadSpeedLimit() != null) {
            // MB->KB
            stepInstance.setFileUploadSpeedLimit(
                DataSizeConverter.convertMBToKB(fileStepInfo.getUploadSpeedLimit()));
        }
        stepInstance.setTimeout(fileStepInfo.getTimeout());

        stepInstance.setIgnoreError(fileStepInfo.getIgnoreError());
        stepInstance.setNotExistPathHandler(fileStepInfo.getNotExistPathHandler());

    }

    private void parseScriptStepInstanceFromStepInstance(StepInstanceDTO stepInstance,
                                                         StepInstanceDTO originStepInstance,
                                                         Map<String, TaskVariableDTO> variableValueMap) {
        stepInstance.setScriptContent(originStepInstance.getScriptContent());
        stepInstance.setScriptParam(originStepInstance.getScriptParam());
        stepInstance.setSecureParam(originStepInstance.isSecureParam());
        stepInstance.setIgnoreError(originStepInstance.isIgnoreError());
        if (originStepInstance.getTimeout() != null) {
            stepInstance.setTimeout(originStepInstance.getTimeout());
        } else {
            stepInstance.setTimeout(1000);
        }
        stepInstance.setScriptType(originStepInstance.getScriptType());
        stepInstance.setScriptSource(originStepInstance.getScriptSource());

        ScriptTypeEnum scriptType = originStepInstance.getScriptType();
        stepInstance.setAccountId(originStepInstance.getAccountId());
        if (scriptType == ScriptTypeEnum.SQL) {
            stepInstance.setAccountId(originStepInstance.getAccountId());
            stepInstance.setAccount(originStepInstance.getAccount());
            stepInstance.setDbAccountId(originStepInstance.getDbAccountId());
            stepInstance.setDbAccount(originStepInstance.getDbAccount());
            stepInstance.setDbType(originStepInstance.getDbType());
            stepInstance.setDbPort(originStepInstance.getDbPort());
            stepInstance.setDbPass(originStepInstance.getDbPass());
        } else {
            stepInstance.setAccountId(originStepInstance.getAccountId());
            stepInstance.setAccount(originStepInstance.getAccount());
        }

        ExecuteTargetDTO targetServers = buildFinalTargetServers(originStepInstance.getTargetExecuteObjects(),
            variableValueMap);
        stepInstance.setTargetExecuteObjects(targetServers);
    }

    private void parseFileStepInstanceFromStepInstance(StepInstanceDTO stepInstance,
                                                       StepInstanceDTO originStepInstance,
                                                       Map<String, TaskVariableDTO> variableValueMap) {
        stepInstance.setAccountId(originStepInstance.getAccountId());
        stepInstance.setAccount(originStepInstance.getAccount());
        stepInstance.setFileTargetPath(originStepInstance.getFileTargetPath());
        stepInstance.setIgnoreError(originStepInstance.isIgnoreError());
        stepInstance.setTimeout(originStepInstance.getTimeout());
        stepInstance.setFileUploadSpeedLimit(originStepInstance.getFileUploadSpeedLimit());
        stepInstance.setFileDownloadSpeedLimit(originStepInstance.getFileDownloadSpeedLimit());

        if (originStepInstance.getFileSourceList() != null) {
            List<FileSourceDTO> fileSourceList = new ArrayList<>();
            originStepInstance.getFileSourceList().forEach(fileSourceDTO -> {
                FileSourceDTO newFileSource = fileSourceDTO.clone();
                // 重新解析源文件服务器信息
                ExecuteTargetDTO targetServers = buildFinalTargetServers(newFileSource.getServers(), variableValueMap);
                newFileSource.setServers(targetServers);
                fileSourceList.add(newFileSource);
            });
            stepInstance.setFileSourceList(fileSourceList);
        }

        ExecuteTargetDTO targetServers = buildFinalTargetServers(originStepInstance.getTargetExecuteObjects(),
            variableValueMap);
        stepInstance.setTargetExecuteObjects(targetServers);
    }

    private ExecuteTargetDTO buildFinalTargetServers(@NotNull ServiceTaskTargetDTO target,
                                                     @NotNull Map<String, TaskVariableDTO> variableValueMap)
        throws ServiceException {
        // 如果目标服务器使用主机变量，那么需要解析主机变量
        if (StringUtils.isNotBlank(target.getVariable())) {
            return getServerValueFromVariable(target.getVariable(), variableValueMap);
        } else {
            return convertToServersDTO(target);
        }
    }

    private ExecuteTargetDTO buildFinalTargetServers(ExecuteTargetDTO target,
                                                     Map<String, TaskVariableDTO> variableValueMap)
        throws ServiceException {
        // 如果目标服务器使用主机变量，那么需要解析主机变量
        if (target != null && StringUtils.isNotBlank(target.getVariable())) {
            return getServerValueFromVariable(target.getVariable(), variableValueMap);
        } else {
            return target;
        }
    }

    private ExecuteTargetDTO getServerValueFromVariable(@NotNull String hostVariableName,
                                                        @NotNull Map<String, TaskVariableDTO> variableValueMap)
        throws ServiceException {
        TaskVariableDTO serverVariable = variableValueMap.get(hostVariableName);
        if (serverVariable == null) {
            log.warn("Target server variable is not exist.variable={}", hostVariableName);
            throw new FailedPreconditionException(ErrorCode.TASK_INSTANCE_RELATED_HOST_VAR_NOT_EXIST,
                new String[]{hostVariableName});
        }

        if (serverVariable.getExecuteTarget() == null) {
            return null;
        }
        ExecuteTargetDTO targetServers = serverVariable.getExecuteTarget().clone();
        targetServers.setVariable(hostVariableName);
        return targetServers;
    }

    private void parseManualConfirmStepInstance(
        StepInstanceDTO stepInstance,
        ServiceTaskApprovalStepDTO approvalStep) {
        stepInstance.setConfirmMessage(approvalStep.getApprovalMessage());
        stepInstance.setConfirmUsers(approvalStep.getApprovalUser().getUserList());
        stepInstance.setConfirmRoles(approvalStep.getApprovalUser().getRoleList());
        stepInstance.setNotifyChannels(approvalStep.getChannels());
    }

    private void parseManualConfirmStepInstance(StepInstanceDTO stepInstance, StepInstanceDTO originStepInstance) {
        stepInstance.setConfirmMessage(originStepInstance.getConfirmMessage());
        stepInstance.setConfirmUsers(originStepInstance.getConfirmUsers());
        stepInstance.setConfirmRoles(originStepInstance.getConfirmRoles());
        stepInstance.setNotifyChannels(originStepInstance.getNotifyChannels());
    }


    private ExecuteTargetDTO convertToServersDTO(ServiceTaskTargetDTO taskTarget) {
        if (taskTarget == null) {
            return null;
        }
        ExecuteTargetDTO executeTarget = new ExecuteTargetDTO();
        ServiceTaskHostNodeDTO targetServers = taskTarget.getTargetServer();
        List<ServiceHostInfoDTO> hostList = targetServers.getHostList();
        if (hostList != null && !hostList.isEmpty()) {
            List<HostDTO> staticIpList = new ArrayList<>();
            for (ServiceHostInfoDTO hostInfo : hostList) {
                staticIpList.add(
                    new HostDTO(hostInfo.getHostId(), hostInfo.getCloudAreaId(), hostInfo.getIp()));
            }
            executeTarget.setStaticIpList(staticIpList);
        }

        List<String> groupIdList = targetServers.getDynamicGroupId();
        if (groupIdList != null && !groupIdList.isEmpty()) {
            List<DynamicServerGroupDTO> groups = new ArrayList<>();
            for (String groupId : groupIdList) {
                groups.add(new DynamicServerGroupDTO(groupId));
            }
            executeTarget.setDynamicServerGroups(groups);
        }

        List<ServiceTaskNodeInfoDTO> topoNodeIdList = targetServers.getNodeInfoList();
        if (topoNodeIdList != null && !topoNodeIdList.isEmpty()) {
            List<DynamicServerTopoNodeDTO> topoNodes = new ArrayList<>();
            for (ServiceTaskNodeInfoDTO topoNodeId : topoNodeIdList) {
                topoNodes.add(new DynamicServerTopoNodeDTO(topoNodeId.getId(), topoNodeId.getType()));
            }
            executeTarget.setTopoNodes(topoNodes);
        }
        return executeTarget;
    }

    @Override
    public Integer doStepOperation(Long appId,
                                   String operator,
                                   StepOperationDTO stepOperation) {
        long stepInstanceId = stepOperation.getStepInstanceId();
        StepOperationEnum operation = stepOperation.getOperation();

        log.info("Operate step, appId:{}, stepInstanceId:{}, operator:{}, operation:{}", appId, stepInstanceId,
            operator, operation.getValue());

        TaskInstanceDTO taskInstance = queryTaskInstanceAndCheckExist(appId, stepOperation.getTaskInstanceId());
        addJobInstanceContext(taskInstance);

        StepInstanceDTO stepInstance = queryStepInstanceAndCheckExist(
            appId, stepOperation.getTaskInstanceId(), stepInstanceId);

        int executeCount = stepInstance.getExecuteCount();
        switch (operation) {
            case CONFIRM_CONTINUE:
                confirmContinue(stepInstance, operator, stepOperation.getConfirmReason());
                break;
            case RETRY_FAIL_IP:
                retryStepFail(taskInstance, stepInstance, operator);
                executeCount++;
                break;
            case IGNORE_ERROR:
                ignoreError(taskInstance, stepInstance, operator);
                break;
            case RETRY_ALL_IP:
                retryStepAll(taskInstance, stepInstance, operator);
                executeCount++;
                break;
            case CONFIRM_TERMINATE:
                confirmTerminate(stepInstance, operator, stepOperation.getConfirmReason());
                break;
            case CONFIRM_RESTART:
                confirmRestart(stepInstance, operator);
                break;
            case NEXT_STEP:
                nextStep(taskInstance, stepInstance, operator);
                break;
            case SKIP:
                skipStep(taskInstance, stepInstance, operator);
                break;
            case ROLLING_CONTINUE:
                continueRolling(stepInstance);
                break;
            default:
                log.warn("Undefined step operation!");
                break;
        }
        return executeCount;
    }

    private TaskInstanceDTO queryTaskInstanceAndCheckExist(long appId, long taskInstanceId) {
        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(taskInstanceId);
        if (taskInstance == null || !taskInstance.getAppId().equals(appId)) {
            log.warn("Task instance is not exist, appId:{}, taskInstanceId:{}", appId, taskInstance);
            throw new NotFoundException(ErrorCode.TASK_INSTANCE_NOT_EXIST);
        }
        return taskInstance;
    }

    private StepInstanceDTO queryStepInstanceAndCheckExist(long appId, long taskInstanceId, long stepInstanceId) {
        StepInstanceDTO stepInstance = stepInstanceService.getStepInstanceDetail(
            taskInstanceId, stepInstanceId);
        if (stepInstance == null) {
            log.warn("Step instance {} is not exist", stepInstanceId);
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }
        if (!stepInstance.getAppId().equals(appId)) {
            log.warn("Step instance {} is not in app:{}", stepInstance, appId);
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }
        return stepInstance;
    }

    private void continueRolling(StepInstanceDTO stepInstance) {
        // 只有“等待用户”的滚动步骤可以继续滚动
        if (stepInstance.getStatus() != RunStatusEnum.WAITING_USER) {
            log.warn("Unsupported operation, stepInstanceId: {}, operation: {}, stepStatus: {}",
                stepInstance.getId(), StepOperationEnum.ROLLING_CONTINUE.name(), stepInstance.getStatus().name());
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        if (!(stepInstance.isRollingStep())) {
            log.warn("StepInstance:{} is not rolling step, Unsupported Operation:{}", stepInstance.getId(),
                "rolling-continue");
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }

        // 需要同步设置任务状态为RUNNING，保证客户端可以在操作完之后立马获取到运行状态，开启同步刷新
        taskInstanceService.updateTaskStatus(stepInstance.getTaskInstanceId(), RunStatusEnum.RUNNING.getValue());
        taskExecuteMQEventDispatcher.dispatchStepEvent(StepEvent.startStep(stepInstance.getTaskInstanceId(),
            stepInstance.getId(), stepInstance.getBatch() + 1));
    }

    private void confirmTerminate(StepInstanceDTO stepInstance, String operator, String reason) {
        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(stepInstance.getTaskInstanceId());
        // 只有人工确认等待中的任务，可以进行“终止流程”操作
        if (RunStatusEnum.WAITING_USER != stepInstance.getStatus()) {
            log.warn("Unsupported operation, stepInstanceId: {}, operation: {}, stepStatus: {}",
                stepInstance.getId(), StepOperationEnum.CONFIRM_TERMINATE.name(), stepInstance.getStatus().name());
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        if (stepInstance.getExecuteType() != MANUAL_CONFIRM) {
            log.warn("StepInstance:{} is not confirm step, Unsupported Operation:{}", stepInstance.getId(),
                "confirm-terminate");
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        checkConfirmUser(taskInstance, stepInstance, operator);

        OperationLogDTO operationLog = buildCommonStepOperationLog(stepInstance, operator,
            UserOperationEnum.CONFIRM_TERMINATE);
        operationLog.getDetail().setConfirmReason(reason);
        taskOperationLogService.saveOperationLog(operationLog);

        stepInstanceService.updateConfirmReason(stepInstance.getTaskInstanceId(), stepInstance.getId(), reason);
        stepInstanceService.updateStepOperator(stepInstance.getTaskInstanceId(), stepInstance.getId(), operator);

        taskExecuteMQEventDispatcher.dispatchStepEvent(
            StepEvent.confirmStepTerminate(stepInstance.getTaskInstanceId(), stepInstance.getId()));
    }

    private void confirmRestart(StepInstanceDTO stepInstance, String operator) {
        // 只有“确认终止”状态的任务，可以进行“重新发起确认”操作
        if (RunStatusEnum.CONFIRM_TERMINATED != stepInstance.getStatus()) {
            log.warn("Unsupported operation, stepInstanceId: {}, operation: {}, stepStatus: {}",
                stepInstance.getId(), StepOperationEnum.CONFIRM_RESTART.name(), stepInstance.getStatus().name());
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        if (stepInstance.getExecuteType() != MANUAL_CONFIRM) {
            log.warn("StepInstance:{} is not confirm step, Unsupported Operation:{}", stepInstance.getId(),
                "confirm-restart");
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        taskOperationLogService.saveOperationLog(buildCommonStepOperationLog(stepInstance, operator,
            UserOperationEnum.CONFIRM_RESTART));
        taskExecuteMQEventDispatcher.dispatchStepEvent(StepEvent.confirmStepRestart(
            stepInstance.getTaskInstanceId(), stepInstance.getId()));
    }

    private void checkConfirmUser(TaskInstanceDTO taskInstance, StepInstanceDTO stepInstance,
                                  String operator) throws ServiceException {
        // 人工确认步骤，需要判断操作者
        // 判断指定确认人
        if (CollectionUtils.isNotEmpty(stepInstance.getConfirmUsers())
            && stepInstance.getConfirmUsers().contains(operator)) {
            return;
        }

        // 判断确认角色
        if (stepInstance.getConfirmRoles() != null && !stepInstance.getConfirmRoles().isEmpty()) {
            if (stepInstance.getConfirmRoles().contains(JobRoleEnum.JOB_RESOURCE_TRIGGER_USER.name())
                && taskInstance.getOperator().equals(operator)) {
                return;
            }

            Set<String> confirmCmdbRoleUsers = getCmdbRoleUsers(taskInstance.getAppId(), operator,
                String.valueOf(taskInstance.getPlanId()), stepInstance.getConfirmRoles());
            if (CollectionUtils.isEmpty(confirmCmdbRoleUsers) || !confirmCmdbRoleUsers.contains(operator)) {
                log.warn("Confirm user is invalid, allowed confirmUsers: {}, confirmCmdbRoleUsers : {}, " +
                        "taskTrigger: {}, operator: {}",
                    stepInstance.getConfirmUsers(), confirmCmdbRoleUsers, taskInstance.getOperator(), operator);
                throw new FailedPreconditionException(ErrorCode.NOT_IN_CONFIRM_USER_LIST);
            }
        }

    }

    private Set<String> getCmdbRoleUsers(Long appId,
                                         String operator,
                                         String resourceId,
                                         Collection<String> allRoles) {
        Set<String> cmdbRoles = getCmdbRoles(allRoles);
        Set<String> confirmRoleUsers = new HashSet<>();
        if (CollectionUtils.isNotEmpty(cmdbRoles)) {
            InternalResponse<Set<String>> resp = userResource.getUsersByRoles(appId, operator,
                ResourceTypeEnum.JOB.getType(), resourceId, cmdbRoles);
            if (resp.isSuccess() && resp.getData() != null) {
                confirmRoleUsers.addAll(resp.getData());
            }
        }
        return confirmRoleUsers;
    }

    private Set<String> getCmdbRoles(Collection<String> allRoles) {
        // 不属于cmdb的角色，需要移除
        return allRoles.stream()
            .filter(role -> !JobRoleEnum.isJobRole(role))
            .collect(Collectors.toSet());
    }

    private void confirmContinue(StepInstanceDTO stepInstance, String operator, String reason) {
        // 只有"人工确认等待"，可以进行"确认继续"操作
        if (stepInstance.getExecuteType() != MANUAL_CONFIRM) {
            log.warn("StepInstance:{} is not confirm-step, Unsupported Operation:{}", stepInstance.getId(),
                "confirm-continue");
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        if (RunStatusEnum.WAITING_USER != stepInstance.getStatus()) {
            log.warn("Unsupported operation, stepInstanceId: {}, operation: {}, stepStatus: {}",
                stepInstance.getId(), StepOperationEnum.CONFIRM_CONTINUE.name(), stepInstance.getStatus().name());
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        // 人工确认继续，需要判断操作者
        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(stepInstance.getTaskInstanceId());
        checkConfirmUser(taskInstance, stepInstance, operator);

        OperationLogDTO operationLog = buildCommonStepOperationLog(stepInstance, operator,
            UserOperationEnum.CONFIRM_CONTINUE);
        operationLog.getDetail().setConfirmReason(reason);
        taskOperationLogService.saveOperationLog(operationLog);

        stepInstanceService.updateConfirmReason(stepInstance.getTaskInstanceId(), stepInstance.getId(), reason);
        stepInstanceService.updateStepOperator(stepInstance.getTaskInstanceId(), stepInstance.getId(), operator);

        // 需要同步设置任务状态为RUNNING，保证客户端可以在操作完之后立马获取到运行状态，开启同步刷新
        taskInstanceService.updateTaskStatus(taskInstance.getId(), RunStatusEnum.RUNNING.getValue());

        taskExecuteMQEventDispatcher.dispatchStepEvent(
            StepEvent.confirmStepContinue(stepInstance.getTaskInstanceId(), stepInstance.getId()));
    }

    private void nextStep(TaskInstanceDTO taskInstance, StepInstanceDTO stepInstance, String operator) {
        // 只有"终止成功"状态的任务，可以直接进入下一步
        if (RunStatusEnum.STOP_SUCCESS != stepInstance.getStatus()) {
            log.warn("Unsupported operation, stepInstanceId: {}, operation: {}, stepStatus: {}",
                stepInstance.getId(), StepOperationEnum.NEXT_STEP.name(), stepInstance.getStatus().name());
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        runningJobResourceQuotaManager.addJob(
            taskInstance.getAppCode(),
            GlobalAppScopeMappingService.get().getScopeByAppId(taskInstance.getAppId()),
            taskInstance.getId()
        );
        taskOperationLogService.saveOperationLog(buildCommonStepOperationLog(stepInstance, operator,
            UserOperationEnum.NEXT_STEP));

        // 需要同步设置任务状态为RUNNING，保证客户端可以在操作完之后立马获取到运行状态，开启同步刷新
        taskInstanceService.updateTaskStatus(taskInstance.getId(), RunStatusEnum.RUNNING.getValue());
        taskExecuteMQEventDispatcher.dispatchStepEvent(
            StepEvent.nextStep(stepInstance.getTaskInstanceId(), stepInstance.getId()));
    }

    private void retryStepFail(TaskInstanceDTO taskInstance, StepInstanceDTO stepInstance, String operator) {
        if (!isStepRetryable(stepInstance.getStatus())) {
            log.warn("Unsupported operation, stepInstanceId: {}, operation: {}, stepStatus: {}",
                stepInstance.getId(), StepOperationEnum.RETRY_FAIL_IP.name(), stepInstance.getStatus().name());
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        runningJobResourceQuotaManager.addJob(
            taskInstance.getAppCode(),
            GlobalAppScopeMappingService.get().getScopeByAppId(taskInstance.getAppId()),
            taskInstance.getId()
        );
        // 需要同步设置任务状态为RUNNING，保证客户端可以在操作完之后立马获取到运行状态，开启同步刷新
        taskInstanceService.updateTaskStatus(stepInstance.getTaskInstanceId(), RunStatusEnum.RUNNING.getValue());
        stepInstanceService.addStepInstanceExecuteCount(
            stepInstance.getTaskInstanceId(), stepInstance.getId());
        taskExecuteMQEventDispatcher.dispatchStepEvent(StepEvent.retryStepFail(stepInstance.getTaskInstanceId(),
            stepInstance.getId()));
        OperationLogDTO operationLog = buildCommonStepOperationLog(stepInstance, operator,
            UserOperationEnum.RETRY_STEP_FAIL);
        taskOperationLogService.saveOperationLog(operationLog);
    }

    private void retryStepAll(TaskInstanceDTO taskInstance, StepInstanceDTO stepInstance, String operator) {
        if (!isStepRetryable(stepInstance.getStatus())) {
            log.warn("Unsupported operation, stepInstanceId: {}, operation: {}, stepStatus: {}",
                stepInstance.getId(), StepOperationEnum.RETRY_ALL_IP.name(), stepInstance.getStatus().name());
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        runningJobResourceQuotaManager.addJob(
            taskInstance.getAppCode(),
            GlobalAppScopeMappingService.get().getScopeByAppId(taskInstance.getAppId()),
            taskInstance.getId()
        );
        // 需要同步设置任务状态为RUNNING，保证客户端可以在操作完之后立马获取到运行状态，开启同步刷新
        taskInstanceService.updateTaskStatus(stepInstance.getTaskInstanceId(), RunStatusEnum.RUNNING.getValue());
        stepInstanceService.addStepInstanceExecuteCount(stepInstance.getTaskInstanceId(), stepInstance.getId());
        taskExecuteMQEventDispatcher.dispatchStepEvent(
            StepEvent.retryStepAll(stepInstance.getTaskInstanceId(), stepInstance.getId()));
        OperationLogDTO operationLog = buildCommonStepOperationLog(stepInstance, operator,
            UserOperationEnum.RETRY_STEP_ALL);
        taskOperationLogService.saveOperationLog(operationLog);
    }

    private boolean isStepRetryable(RunStatusEnum stepStatus) {
        // 只有“执行失败”,"状态异常","终止成功"的作业可以重试
        return stepStatus == RunStatusEnum.FAIL
            || stepStatus == RunStatusEnum.ABNORMAL_STATE
            || stepStatus == RunStatusEnum.STOP_SUCCESS;
    }

    private void ignoreError(TaskInstanceDTO taskInstance, StepInstanceDTO stepInstance, String operator) {
        // 只有“执行失败”的作业可以忽略错误进入下一步
        if (stepInstance.getStatus() != RunStatusEnum.FAIL &&
            stepInstance.getStatus() != RunStatusEnum.ABNORMAL_STATE) {
            log.warn("Unsupported operation, stepInstanceId: {}, operation: {}, stepStatus: {}",
                stepInstance.getId(), StepOperationEnum.IGNORE_ERROR.name(), stepInstance.getStatus().name());
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        runningJobResourceQuotaManager.addJob(
            taskInstance.getAppCode(),
            GlobalAppScopeMappingService.get().getScopeByAppId(taskInstance.getAppId()),
            taskInstance.getId()
        );
        // 需要同步设置任务状态为RUNNING，保证客户端可以在操作完之后立马获取到运行状态，开启同步刷新
        taskInstanceService.updateTaskStatus(stepInstance.getTaskInstanceId(), RunStatusEnum.RUNNING.getValue());
        taskExecuteMQEventDispatcher.dispatchStepEvent(
            StepEvent.ignoreError(stepInstance.getTaskInstanceId(), stepInstance.getId()));
        OperationLogDTO operationLog = buildCommonStepOperationLog(stepInstance, operator,
            UserOperationEnum.IGNORE_ERROR);
        taskOperationLogService.saveOperationLog(operationLog);
    }

    private void skipStep(TaskInstanceDTO taskInstance, StepInstanceDTO stepInstance, String operator) {
        // 只有“强制终止中”的作业可以跳过
        if (stepInstance.getStatus() != RunStatusEnum.STOPPING) {
            log.warn("Unsupported operation, stepInstanceId: {}, operation: {}, stepStatus: {}",
                stepInstance.getId(), StepOperationEnum.SKIP.name(), stepInstance.getStatus().name());
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        runningJobResourceQuotaManager.addJob(
            taskInstance.getAppCode(),
            GlobalAppScopeMappingService.get().getScopeByAppId(taskInstance.getAppId()),
            taskInstance.getId()
        );
        taskExecuteMQEventDispatcher.dispatchStepEvent(
            StepEvent.skipStep(stepInstance.getTaskInstanceId(), stepInstance.getId()));
        OperationLogDTO operationLog = buildCommonStepOperationLog(stepInstance, operator, UserOperationEnum.SKIP_STEP);
        taskOperationLogService.saveOperationLog(operationLog);
    }

    private OperationLogDTO buildCommonStepOperationLog(StepInstanceDTO stepInstance, String operator,
                                                        UserOperationEnum operation) {
        OperationLogDTO operationLog = new OperationLogDTO();
        operationLog.setTaskInstanceId(stepInstance.getTaskInstanceId());
        operationLog.setOperator(operator);
        operationLog.setCreateTime(DateUtils.currentTimeMillis());
        operationLog.setOperationEnum(operation);
        OperationLogDTO.OperationDetail taskDetail = new OperationLogDTO.OperationDetail();
        taskDetail.setTaskInstanceId(stepInstance.getTaskInstanceId());
        taskDetail.setStepInstanceId(stepInstance.getId());
        taskDetail.setStepName(stepInstance.getName());
        taskDetail.setExecuteCount(stepInstance.getExecuteCount());
        taskDetail.setBatch(stepInstance.getBatch());
        operationLog.setDetail(taskDetail);
        return operationLog;
    }

    private OperationLogDTO buildTaskOperationLog(TaskInstanceDTO taskInstance, String operator,
                                                  UserOperationEnum operation) {
        OperationLogDTO operationLog = new OperationLogDTO();
        operationLog.setTaskInstanceId(taskInstance.getId());
        operationLog.setOperator(operator);
        operationLog.setCreateTime(DateUtils.currentTimeMillis());
        operationLog.setOperationEnum(operation);
        OperationLogDTO.OperationDetail taskDetail = new OperationLogDTO.OperationDetail();
        taskDetail.setTaskInstanceId(taskInstance.getId());
        taskDetail.setStepName("--");
        taskDetail.setStartupMode(taskInstance.getStartupMode());
        taskDetail.setAppCode(taskInstance.getAppCode());
        operationLog.setDetail(taskDetail);
        return operationLog;
    }

    @Override
    public void terminateJob(String username, Long appId, Long taskInstanceId) {
        TaskInstanceDTO taskInstance = queryTaskInstanceAndCheckExist(appId, taskInstanceId);
        terminateJob(username, taskInstance);
    }

    private void terminateJob(String operator, TaskInstanceDTO taskInstance) {
        addJobInstanceContext(taskInstance);
        if (RunStatusEnum.RUNNING != taskInstance.getStatus()
            && RunStatusEnum.WAITING_USER != taskInstance.getStatus()) {
            log.warn("TaskInstance:{} status is not running/waiting, should not terminate it!", taskInstance.getId());
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        taskExecuteMQEventDispatcher.dispatchJobEvent(JobEvent.stopJob(taskInstance.getId()));
        OperationLogDTO operationLog = buildTaskOperationLog(taskInstance, operator, UserOperationEnum.TERMINATE_JOB);
        taskOperationLogService.saveOperationLog(operationLog);
    }

    @Override
    public void doTaskOperation(Long appId,
                                String operator,
                                long taskInstanceId,
                                TaskOperationEnum operation) {
        log.info("Operate task instance, appId:{}, taskInstanceId:{}, operator:{}, operation:{}", appId,
            taskInstanceId, operator, operation.getValue());
        TaskInstanceDTO taskInstance = queryTaskInstanceAndCheckExist(appId, taskInstanceId);
        if (operation == TaskOperationEnum.TERMINATE_JOB) {
            terminateJob(operator, taskInstance);
        } else {
            log.warn("Undefined task operation!");
        }
    }

    @Override
    public void authExecuteJobPlan(TaskExecuteParam executeParam) throws ServiceException {
        StopWatch watch = new StopWatch("authJobPlan");
        TaskInfo taskInfo = buildTaskInfoFromExecuteParam(executeParam, watch);
        TaskInstanceDTO taskInstance = taskInfo.getTaskInstance();
        List<StepInstanceDTO> stepInstanceList = taskInfo.getStepInstances();

        TaskInstanceExecuteObjects taskInstanceExecuteObjects =
            taskInstanceExecuteObjectProcessor.processExecuteObjects(taskInstance, stepInstanceList,
                taskInfo.getVariables().values());

        watch.start("auth-execute-job");
        authExecuteJobPlan(executeParam.getOperator(), executeParam.getAppId(), taskInfo.getJobPlan(),
            taskInfo.getStepInstances(), taskInstanceExecuteObjects.getWhiteHostAllowActions());
        watch.stop();

        if (watch.getTotalTimeMillis() > 500) {
            log.warn("authJobPlan is slow, watcher: {}", watch.prettyPrint());
        }
    }
}
