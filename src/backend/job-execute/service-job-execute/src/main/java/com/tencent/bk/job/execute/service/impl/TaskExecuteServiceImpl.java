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
import com.tencent.bk.job.common.gse.constants.AgentAliveStatusEnum;
import com.tencent.bk.job.common.gse.constants.DefaultBeanNames;
import com.tencent.bk.job.common.gse.service.AgentStateClient;
import com.tencent.bk.job.common.gse.service.model.HostAgentStateQuery;
import com.tencent.bk.job.common.gse.util.AgentUtils;
import com.tencent.bk.job.common.gse.v2.model.resp.AgentState;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.iam.constant.ResourceTypeId;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.service.feature.strategy.JobInstanceAttrToggleStrategy;
import com.tencent.bk.job.common.util.ArrayUtil;
import com.tencent.bk.job.common.util.DataSizeConverter;
import com.tencent.bk.job.common.util.ListUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.util.feature.FeatureExecutionContext;
import com.tencent.bk.job.common.util.feature.FeatureIdConstants;
import com.tencent.bk.job.common.util.feature.FeatureToggle;
import com.tencent.bk.job.common.util.feature.ToggleStrategyContextParams;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.audit.ExecuteJobAuditEventBuilder;
import com.tencent.bk.job.execute.auth.ExecuteAuthService;
import com.tencent.bk.job.execute.common.cache.WhiteHostCache;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
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
import com.tencent.bk.job.execute.engine.util.TimeoutUtils;
import com.tencent.bk.job.execute.model.AccountDTO;
import com.tencent.bk.job.execute.model.DynamicServerGroupDTO;
import com.tencent.bk.job.execute.model.DynamicServerTopoNodeDTO;
import com.tencent.bk.job.execute.model.FastTaskDTO;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.OperationLogDTO;
import com.tencent.bk.job.execute.model.RollingConfigDTO;
import com.tencent.bk.job.execute.model.ServersDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.StepOperationDTO;
import com.tencent.bk.job.execute.model.TaskExecuteParam;
import com.tencent.bk.job.execute.model.TaskInfo;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.AccountService;
import com.tencent.bk.job.execute.service.DangerousScriptCheckService;
import com.tencent.bk.job.execute.service.HostService;
import com.tencent.bk.job.execute.service.RollingConfigService;
import com.tencent.bk.job.execute.service.ScriptService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskExecuteService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceVariableService;
import com.tencent.bk.job.execute.service.TaskOperationLogService;
import com.tencent.bk.job.execute.service.TaskPlanService;
import com.tencent.bk.job.execute.util.LoggerFactory;
import com.tencent.bk.job.manage.api.inner.ServiceTaskTemplateResource;
import com.tencent.bk.job.manage.api.inner.ServiceUserResource;
import com.tencent.bk.job.manage.common.consts.JobResourceStatusEnum;
import com.tencent.bk.job.manage.common.consts.notify.JobRoleEnum;
import com.tencent.bk.job.manage.common.consts.notify.ResourceTypeEnum;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.common.consts.whiteip.ActionScopeEnum;
import com.tencent.bk.job.manage.model.inner.ServiceAccountDTO;
import com.tencent.bk.job.manage.model.inner.ServiceHostInfoDTO;
import com.tencent.bk.job.manage.model.inner.ServiceListAppHostResultDTO;
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
import org.springframework.beans.factory.annotation.Qualifier;
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
    private final AgentStateClient preferV2AgentStateClient;
    private final TaskOperationLogService taskOperationLogService;
    private final TaskInstanceService taskInstanceService;
    private final StepInstanceService stepInstanceService;
    private final HostService hostService;
    private final ServiceUserResource userResource;
    private final ExecuteAuthService executeAuthService;
    private final DangerousScriptCheckService dangerousScriptCheckService;
    private final RollingConfigService rollingConfigService;
    private final JobExecuteConfig jobExecuteConfig;
    private final TaskEvictPolicyExecutor taskEvictPolicyExecutor;
    private final AppScopeMappingService appScopeMappingService;
    private final WhiteHostCache whiteHostCache;
    private final ServiceTaskTemplateResource taskTemplateResource;

    private static final Logger TASK_MONITOR_LOGGER = LoggerFactory.TASK_MONITOR_LOGGER;

    @Autowired
    public TaskExecuteServiceImpl(AccountService accountService,
                                  TaskInstanceService taskInstanceService,
                                  TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher,
                                  TaskPlanService taskPlanService,
                                  TaskInstanceVariableService taskInstanceVariableService,
                                  @Qualifier(DefaultBeanNames.PREFER_V2_AGENT_STATE_CLIENT)
                                      AgentStateClient preferV2AgentStateClient,
                                  TaskOperationLogService taskOperationLogService,
                                  ScriptService scriptService,
                                  StepInstanceService stepInstanceService,
                                  HostService hostService,
                                  ServiceUserResource userResource,
                                  ExecuteAuthService executeAuthService,
                                  DangerousScriptCheckService dangerousScriptCheckService,
                                  JobExecuteConfig jobExecuteConfig,
                                  TaskEvictPolicyExecutor taskEvictPolicyExecutor,
                                  RollingConfigService rollingConfigService,
                                  AppScopeMappingService appScopeMappingService,
                                  WhiteHostCache whiteHostCache,
                                  ServiceTaskTemplateResource taskTemplateResource) {
        this.accountService = accountService;
        this.taskInstanceService = taskInstanceService;
        this.taskExecuteMQEventDispatcher = taskExecuteMQEventDispatcher;
        this.taskPlanService = taskPlanService;
        this.taskInstanceVariableService = taskInstanceVariableService;
        this.preferV2AgentStateClient = preferV2AgentStateClient;
        this.taskOperationLogService = taskOperationLogService;
        this.scriptService = scriptService;
        this.stepInstanceService = stepInstanceService;
        this.hostService = hostService;
        this.userResource = userResource;
        this.executeAuthService = executeAuthService;
        this.dangerousScriptCheckService = dangerousScriptCheckService;
        this.rollingConfigService = rollingConfigService;
        this.jobExecuteConfig = jobExecuteConfig;
        this.taskEvictPolicyExecutor = taskEvictPolicyExecutor;
        this.appScopeMappingService = appScopeMappingService;
        this.whiteHostCache = whiteHostCache;
        this.taskTemplateResource = taskTemplateResource;
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
        TaskInstanceDTO taskInstance = fastTask.getTaskInstance();
        StepInstanceDTO stepInstance = fastTask.getStepInstance();

        StopWatch watch = new StopWatch("executeFastTask");
        // 检查任务是否应当被驱逐
        checkTaskEvict(taskInstance);
        standardizeStepDynamicGroupId(Collections.singletonList(stepInstance));
        adjustStepTimeout(stepInstance);
        try {
            // 设置账号信息
            watch.start("checkAndSetAccountInfo");
            checkAndSetAccountInfo(stepInstance, taskInstance.getAppId());
            watch.stop();

            // 获取主机
            watch.start("acquireAndSetHosts");
            ServiceListAppHostResultDTO hosts =
                acquireAndSetHosts(taskInstance, Collections.singletonList(stepInstance), null);
            watch.stop();

            // 获取主机白名单
            watch.start("getHostAllowedActions");
            Map<Long, List<String>> hostAllowActions = getHostAllowedActions(taskInstance.getAppId(),
                ListUtil.union(hosts.getValidHosts(), hosts.getNotInAppHosts()));
            watch.stop();

            //检查主机
            watch.start("checkHosts");
            checkHosts(Collections.singletonList(stepInstance), hosts, hostAllowActions);
            watch.stop();

            // 检查步骤约束
            watch.start("checkStepInstanceConstraint");
            checkStepInstanceConstraint(taskInstance, Collections.singletonList(stepInstance));
            watch.stop();

            // 鉴权
            watch.start("authFastExecute");
            authFastExecute(taskInstance, stepInstance, hostAllowActions);
            watch.stop();

            // 保存作业
            saveTaskInstance(watch, fastTask, taskInstance, stepInstance);

            // 启动作业
            watch.start("startJob");
            startTask(taskInstance.getId());
            watch.stop();

            // 审计
            Set<HostDTO> allHosts = extractHosts(Collections.singletonList(stepInstance), null);
            taskInstance.setAllHosts(allHosts);
            auditFastJobExecute(taskInstance);

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

    private void saveTaskInstance(StopWatch watch,
                                  FastTaskDTO fastTask,
                                  TaskInstanceDTO taskInstance,
                                  StepInstanceDTO stepInstance) {
        // 保存作业、步骤实例
        watch.start("saveInstance");
        Long taskInstanceId = taskInstanceService.addTaskInstance(taskInstance);
        taskInstance.setId(taskInstanceId);
        stepInstance.setTaskInstanceId(taskInstanceId);
        stepInstance.setStepNum(1);
        stepInstance.setStepOrder(1);
        long stepInstanceId = taskInstanceService.addStepInstance(stepInstance);
        stepInstance.setId(stepInstanceId);
        taskInstance.setStepInstances(Collections.singletonList(stepInstance));
        watch.stop();

        // 保存作业实例与主机的关系，优化根据主机检索作业执行历史的效率
        watch.start("saveTaskInstanceHosts");
        saveTaskInstanceHosts(taskInstanceId, Collections.singletonList(stepInstance));
        watch.stop();

        // 保存滚动配置
        if (fastTask.isRollingEnabled()) {
            watch.start("saveRollingConfig");
            RollingConfigDTO rollingConfig = rollingConfigService.saveRollingConfigForFastJob(fastTask);
            long rollingConfigId = rollingConfig.getId();
            stepInstanceService.updateStepRollingConfigId(stepInstanceId, rollingConfigId);
            watch.stop();
        }

        // 记录操作日志
        watch.start("saveOperationLog");
        taskOperationLogService.saveOperationLog(buildTaskOperationLog(taskInstance, taskInstance.getOperator(),
            UserOperationEnum.START));
        watch.stop();
    }

    /**
     * @param appId 业务ID
     * @param hosts 主机列表
     * @return key=hostId, value: 允许的操作列表
     */
    private Map<Long, List<String>> getHostAllowedActions(long appId, Collection<HostDTO> hosts) {
        Map<Long, List<String>> hostAllowActionsMap = new HashMap<>();
        if (CollectionUtils.isEmpty(hosts)) {
            return hostAllowActionsMap;
        }
        for (HostDTO host : hosts) {
            List<String> allowActions = whiteHostCache.getHostAllowedAction(appId, host.getHostId());
            if (CollectionUtils.isNotEmpty(allowActions)) {
                hostAllowActionsMap.put(host.getHostId(), allowActions);
            }
        }
        return hostAllowActionsMap;
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

    private void saveTaskInstanceHosts(long taskInstanceId,
                                       List<StepInstanceDTO> stepInstanceList) {
        Set<HostDTO> stepHosts = extractHosts(stepInstanceList, null);
        saveTaskInstanceHosts(taskInstanceId, stepHosts);
    }

    private void saveTaskInstanceHosts(long taskInstanceId, Collection<HostDTO> hosts) {
        if (CollectionUtils.isEmpty(hosts)) {
            return;
        }
        taskInstanceService.saveTaskInstanceHosts(taskInstanceId, hosts);
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
        if (stepInstance.getExecuteType().equals(EXECUTE_SQL.getValue())) {
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
        if (stepInstance.getExecuteType().equals(EXECUTE_SCRIPT.getValue())
            || stepInstance.getExecuteType().equals(EXECUTE_SQL.getValue())) {
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
                stepInstance.setScriptType(script.getType());
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
        ScriptTypeEnum scriptType = ScriptTypeEnum.valueOf(stepInstance.getScriptType());
        String content = stepInstance.getScriptContent();
        List<ServiceScriptCheckResultItemDTO> checkResultItems = dangerousScriptCheckService.check(scriptType, content);
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
        if (StepExecuteTypeEnum.EXECUTE_SCRIPT.getValue().equals(stepInstance.getExecuteType())) {
            accountId = stepInstance.getAccountId();
        } else if (StepExecuteTypeEnum.EXECUTE_SQL.getValue().equals(stepInstance.getExecuteType())) {
            accountId = stepInstance.getDbAccountId();
        }
        if (accountId == null) {
            return AuthResult.fail();
        }

        AuthResult accountAuthResult = executeAuthService.authAccountExecutable(username, new AppResourceScope(appId)
            , accountId);

        AuthResult serverAuthResult;
        ServersDTO servers = stepInstance.getTargetServers().clone();
        filterServerDoNotRequireAuth(ActionScopeEnum.SCRIPT_EXECUTE, servers, whiteHostAllowActions);
        if (servers.isEmpty()) {
            // 如果主机为空，无需对主机进行鉴权
            return accountAuthResult;
        }
        ScriptSourceEnum scriptSource = ScriptSourceEnum.getScriptSourceEnum(stepInstance.getScriptSource());
        if (scriptSource == ScriptSourceEnum.CUSTOM) {
            // 快速执行脚本鉴权

            serverAuthResult = executeAuthService.authFastExecuteScript(
                username, new AppResourceScope(appId), servers);
        } else if (scriptSource == ScriptSourceEnum.QUOTED_APP) {

            serverAuthResult = executeAuthService.authExecuteAppScript(
                username, new AppResourceScope(appId), stepInstance.getScriptId(),
                stepInstance.getScriptName(), servers);
        } else if (scriptSource == ScriptSourceEnum.QUOTED_PUBLIC) {

            serverAuthResult = executeAuthService.authExecutePublicScript(
                username, new AppResourceScope(appId), stepInstance.getScriptId()
                , stepInstance.getScriptName(), servers);
        } else {
            serverAuthResult = AuthResult.fail();
        }

        return accountAuthResult.mergeAuthResult(serverAuthResult);
    }

    /*
     * 过滤掉主机白名单的机器
     */
    private void filterServerDoNotRequireAuth(ActionScopeEnum action,
                                              ServersDTO servers,
                                              Map<Long, List<String>> whiteHostAllowActions) {
        if (whiteHostAllowActions == null || whiteHostAllowActions.isEmpty()) {
            return;
        }
        if (CollectionUtils.isNotEmpty(servers.getStaticIpList())) {
            servers.setStaticIpList(servers.getStaticIpList().stream()
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

        ServersDTO servers = stepInstance.getTargetServers().clone();
        stepInstance.getFileSourceList().stream()
            .filter(fileSource -> !fileSource.isLocalUpload()
                && fileSource.getFileType() != TaskFileTypeEnum.BASE64_FILE.getType()
                && fileSource.getServers() != null)
            .forEach(fileSource -> servers.merge(fileSource.getServers()));
        filterServerDoNotRequireAuth(ActionScopeEnum.FILE_DISTRIBUTION, servers, whiteHostAllowActions);
        if (servers.isEmpty()) {
            // 如果主机为空，无需对主机进行权限
            return accountAuthResult;
        }

        AuthResult serverAuthResult = executeAuthService.authFastPushFile(
            username, new AppResourceScope(appId), servers);

        return accountAuthResult.mergeAuthResult(serverAuthResult);
    }

    private ServiceListAppHostResultDTO acquireAndSetHosts(TaskInstanceDTO taskInstance,
                                                           List<StepInstanceDTO> stepInstances,
                                                           Collection<TaskVariableDTO> variables) {
        StopWatch watch = new StopWatch("AcquireAndSetHosts");
        try {
            long appId = taskInstance.getAppId();

            // 提取动态分组/topo节点
            Set<DynamicServerGroupDTO> groups = new HashSet<>();
            Set<DynamicServerTopoNodeDTO> topoNodes = new HashSet<>();
            stepInstances.forEach(stepInstance -> extractDynamicGroupsAndTopoNodes(stepInstance, groups, topoNodes));
            if (CollectionUtils.isNotEmpty(variables)) {
                variables.forEach(variable -> {
                    if (TaskVariableTypeEnum.HOST_LIST.getType() == variable.getType()) {
                        extractDynamicGroupsAndTopoNodes(variable.getTargetServers(), groups, topoNodes);
                    }
                });
            }

            // 获取动态分组的主机并设置
            fillDynamicGroupHosts(watch, appId, groups, stepInstances, variables);

            // 获取topo节点的主机并设置
            fillTopoNodeHosts(watch, appId, topoNodes, stepInstances, variables);

            // 提取作业包含的主机列表
            watch.start("extractHosts");
            Set<HostDTO> queryHosts = extractHosts(stepInstances, variables);
            watch.stop();

            if (CollectionUtils.isEmpty(queryHosts)) {
                return ServiceListAppHostResultDTO.EMPTY;
            }

            watch.start("batchGetAppHosts");
            ServiceListAppHostResultDTO queryHostsResult = hostService.batchGetAppHosts(appId, queryHosts,
                needRefreshHostBkAgentId(taskInstance));
            watch.stop();

            if (CollectionUtils.isNotEmpty(queryHostsResult.getNotExistHosts())) {
                // 如果主机在cmdb不存在，直接报错
                throwHostInvalidException(queryHostsResult.getNotExistHosts());
            }

            watch.start("fillTaskInstanceHostDetail");
            fillTaskInstanceHostDetail(taskInstance, stepInstances, variables, queryHostsResult);
            watch.stop();

            return queryHostsResult;
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
            if (watch.getTotalTimeMillis() > 1000) {
                log.warn("AcquireAndSetHosts slow, watch: {}", watch.prettyPrint());
            }
        }

    }

    private void fillDynamicGroupHosts(StopWatch watch,
                                       long appId,
                                       Set<DynamicServerGroupDTO> groups,
                                       List<StepInstanceDTO> stepInstances,
                                       Collection<TaskVariableDTO> variables) {
        // 获取动态分组的主机并设置
        if (CollectionUtils.isNotEmpty(groups)) {
            watch.start("fillDynamicGroupHosts");
            Map<DynamicServerGroupDTO, List<HostDTO>> dynamicGroupHosts =
                hostService.batchGetAndGroupHostsByDynamicGroup(appId, groups);
            stepInstances.forEach(stepInstance -> {
                setHostsForDynamicGroup(stepInstance.getTargetServers(), dynamicGroupHosts);
                if (stepInstance.getExecuteType() == TaskStepTypeEnum.FILE.getValue()) {
                    List<FileSourceDTO> fileSources = stepInstance.getFileSourceList();
                    for (FileSourceDTO fileSource : fileSources) {
                        ServersDTO servers = fileSource.getServers();
                        if (servers != null && !fileSource.isLocalUpload()) {
                            // 服务器文件的处理
                            setHostsForDynamicGroup(servers, dynamicGroupHosts);
                        }
                    }
                }
            });
            if (CollectionUtils.isNotEmpty(variables)) {
                variables.forEach(variable -> {
                    if (TaskVariableTypeEnum.HOST_LIST.getType() == variable.getType()) {
                        setHostsForDynamicGroup(variable.getTargetServers(), dynamicGroupHosts);
                    }
                });
            }
            watch.stop();
        }
    }

    private void fillTopoNodeHosts(StopWatch watch,
                                   long appId,
                                   Set<DynamicServerTopoNodeDTO> topoNodes,
                                   List<StepInstanceDTO> stepInstances,
                                   Collection<TaskVariableDTO> variables) {
        if (CollectionUtils.isNotEmpty(topoNodes)) {
            watch.start("fillTopoNodeHosts");
            Map<DynamicServerTopoNodeDTO, List<HostDTO>> topoNodeHosts =
                hostService.getAndGroupHostsByTopoNodes(appId, topoNodes);
            stepInstances.forEach(stepInstance -> {
                setHostsForTopoNode(stepInstance.getTargetServers(), topoNodeHosts);
                if (stepInstance.getExecuteType() == TaskStepTypeEnum.FILE.getValue()) {
                    List<FileSourceDTO> fileSources = stepInstance.getFileSourceList();
                    for (FileSourceDTO fileSource : fileSources) {
                        ServersDTO servers = fileSource.getServers();
                        if (servers != null && !fileSource.isLocalUpload()) {
                            // 服务器文件的处理
                            setHostsForTopoNode(servers, topoNodeHosts);
                        }
                    }
                }
            });
            if (CollectionUtils.isNotEmpty(variables)) {
                variables.forEach(variable -> {
                    if (TaskVariableTypeEnum.HOST_LIST.getType() == variable.getType()) {
                        setHostsForTopoNode(variable.getTargetServers(), topoNodeHosts);
                    }
                });
            }
            watch.stop();
        }
    }

    private boolean needRefreshHostBkAgentId(TaskInstanceDTO taskInstance) {
        /*
         * tmp: GSE Agent v1/v2 兼容期间特殊逻辑, 对于节点管理安装Agent插件的请求需要实时获取bk_agent_id。等后续只对接GSE V2 之后，
         * 此处代码可删除。 https://github.com/Tencent/bk-job/issues/1542
         */
        return taskInstance.getStartupMode() == TaskStartupModeEnum.API.getValue()
            && StringUtils.isNotEmpty(taskInstance.getAppCode())
            && (StringUtils.equals(taskInstance.getAppCode(), "bkc-nodeman")
            || StringUtils.equals(taskInstance.getAppCode(), "bk_nodeman"));
    }

    private void checkHosts(List<StepInstanceDTO> stepInstances,
                            ServiceListAppHostResultDTO hosts,
                            Map<Long, List<String>> hostAllowActions) {
        // 检查步骤引用的主机不为空
        stepInstances.forEach(this::checkStepInstanceHostNonEmpty);

        // 判断主机是否可以被当前作业使用
        checkHostAccessible(stepInstances, hosts.getNotInAppHosts(), hostAllowActions);
    }

    /**
     * 判断主机是否可以被当前作业使用
     *
     * @param stepInstanceList      作业步骤列表
     * @param notInAppHosts         作业中包含的不在当前业务下的主机
     * @param whileHostAllowActions 主机白名单
     */
    private void checkHostAccessible(List<StepInstanceDTO> stepInstanceList,
                                     List<HostDTO> notInAppHosts,
                                     Map<Long, List<String>> whileHostAllowActions) {
        if (CollectionUtils.isEmpty(notInAppHosts)) {
            return;
        }
        log.info("Contains hosts not in app, check white host config. notInAppHosts: {}, whileHostAllowActions: {}",
            notInAppHosts, whileHostAllowActions);
        Map<Long, HostDTO> notInAppHostMap = notInAppHosts.stream()
            .collect(Collectors.toMap(HostDTO::getHostId, host -> host));

        // 非法的主机
        Set<HostDTO> invalidHosts = new HashSet<>();
        for (StepInstanceDTO stepInstance : stepInstanceList) {
            if (!isStepContainsHostProps(stepInstance)) {
                continue;
            }
            TaskStepTypeEnum stepType = stepInstance.getStepType();
            // 检查目标主机
            stepInstance.getTargetServers().getIpList().forEach(host -> {
                if (isHostUnAccessible(stepType, host, notInAppHostMap, whileHostAllowActions)) {
                    invalidHosts.add(host);
                }
            });
            // 如果是文件分发任务，检查文件源
            checkFileSourceHostAccessible(invalidHosts, stepInstance, stepType, notInAppHostMap,
                whileHostAllowActions);
        }

        if (CollectionUtils.isNotEmpty(invalidHosts)) {
            // 检查是否在白名单配置
            log.warn("Found hosts not in target app!");
            throwHostInvalidException(invalidHosts);
        }
    }

    private void checkFileSourceHostAccessible(Set<HostDTO> invalidHosts,
                                               StepInstanceDTO stepInstance,
                                               TaskStepTypeEnum stepType,
                                               Map<Long, HostDTO> notInAppHostMap,
                                               Map<Long, List<String>> whileHostAllowActions) {
        if (!stepInstance.isFileStep()) {
            return;
        }
        List<FileSourceDTO> fileSourceList = stepInstance.getFileSourceList();
        if (CollectionUtils.isEmpty(fileSourceList)) {
            return;
        }
        for (FileSourceDTO fileSource : fileSourceList) {
            // 远程文件分发需要校验文件源主机;其他类型不需要
            if (fileSource.getFileType().equals(TaskFileTypeEnum.SERVER.getType())) {
                ServersDTO servers = fileSource.getServers();
                if (servers == null || CollectionUtils.isEmpty(servers.getIpList())) {
                    continue;
                }
                servers.getIpList().forEach(host -> {
                    if (isHostUnAccessible(stepType, host, notInAppHostMap, whileHostAllowActions)) {
                        invalidHosts.add(host);
                    }
                });
            }
        }
    }

    private boolean isHostUnAccessible(TaskStepTypeEnum stepType,
                                       HostDTO host,
                                       Map<Long, HostDTO> notInAppHostMap,
                                       Map<Long, List<String>> whileHostAllowActions) {
        long hostId = host.getHostId();
        if (!notInAppHostMap.containsKey(host.getHostId())) {
            // 主机在当前业务下，可以使用
            return false;
        }
        // 如果主机不在当前业务下，需要判断主机白名单
        if (whileHostAllowActions == null || whileHostAllowActions.isEmpty()) {
            return true;
        }
        List<String> allowActions = whileHostAllowActions.get(hostId);
        String actionScope = (stepType == TaskStepTypeEnum.SCRIPT ?
            ActionScopeEnum.SCRIPT_EXECUTE.name() :
            (stepType == TaskStepTypeEnum.FILE ? ActionScopeEnum.FILE_DISTRIBUTION.name() : ""));
        return CollectionUtils.isEmpty(allowActions) || !allowActions.contains(actionScope);
    }


    private boolean isUsingGseV2(TaskInstanceDTO taskInstance, Collection<HostDTO> taskInstanceHosts) {
        // 初始化Job任务灰度对接 GSE2.0 上下文
        FeatureExecutionContext featureExecutionContext =
            FeatureExecutionContext.builder()
                .addContextParam(ToggleStrategyContextParams.CTX_PARAM_RESOURCE_SCOPE,
                    appScopeMappingService.getScopeByAppId(taskInstance.getAppId()))
                .addContextParam(JobInstanceAttrToggleStrategy.CTX_PARAM_IS_ANY_GSE_V2_AGENT_AVAILABLE,
                    () -> taskInstanceHosts.stream().anyMatch(host -> AgentUtils.isGseV2AgentId(host.getAgentId())))
                .addContextParam(JobInstanceAttrToggleStrategy.CTX_PARAM_IS_ALL_GSE_V2_AGENT_AVAILABLE,
                    () -> taskInstanceHosts.stream().allMatch(
                        host -> AgentUtils.isGseV2AgentId(host.getAgentId())))
                .addContextParam(JobInstanceAttrToggleStrategy.CTX_PARAM_STARTUP_MODE,
                    () -> TaskStartupModeEnum.getStartupMode(taskInstance.getStartupMode()).getName())
                .addContextParam(JobInstanceAttrToggleStrategy.CTX_PARAM_OPERATOR, taskInstance::getOperator);

        boolean isUsingGseV2 = FeatureToggle.checkFeature(
            FeatureIdConstants.FEATURE_GSE_V2,
            featureExecutionContext
        );
        log.info("Use gse version {}", isUsingGseV2 ? "v2" : "v1");
        return isUsingGseV2;
    }

    private void extractDynamicGroupsAndTopoNodes(StepInstanceDTO stepInstance,
                                                  Set<DynamicServerGroupDTO> groups,
                                                  Set<DynamicServerTopoNodeDTO> topoNodes) {
        extractDynamicGroupsAndTopoNodes(stepInstance.getTargetServers(), groups, topoNodes);
        if (stepInstance.getExecuteType() == TaskStepTypeEnum.FILE.getValue()) {
            List<FileSourceDTO> fileSources = stepInstance.getFileSourceList();
            for (FileSourceDTO fileSource : fileSources) {
                ServersDTO servers = fileSource.getServers();
                if (servers != null && !fileSource.isLocalUpload()) {
                    // 服务器文件的处理
                    extractDynamicGroupsAndTopoNodes(servers, groups, topoNodes);
                }
            }
        }
    }

    private void extractDynamicGroupsAndTopoNodes(ServersDTO servers,
                                                  Set<DynamicServerGroupDTO> groups,
                                                  Set<DynamicServerTopoNodeDTO> topoNodes) {
        if (servers == null) {
            return;
        }
        if (CollectionUtils.isNotEmpty(servers.getDynamicServerGroups())) {
            groups.addAll(servers.getDynamicServerGroups());
        }
        if (CollectionUtils.isNotEmpty(servers.getTopoNodes())) {
            topoNodes.addAll(servers.getTopoNodes());
        }
    }

    private void setHostsForDynamicGroup(ServersDTO servers,
                                         Map<DynamicServerGroupDTO, List<HostDTO>> groups) {
        if (servers != null && CollectionUtils.isNotEmpty(servers.getDynamicServerGroups())) {
            servers.getDynamicServerGroups().forEach(group -> group.setIpList(groups.get(group)));
        }
    }

    private void setHostsForTopoNode(ServersDTO servers,
                                     Map<DynamicServerTopoNodeDTO, List<HostDTO>> topoNodes) {
        if (servers != null && CollectionUtils.isNotEmpty(servers.getTopoNodes())) {
            servers.getTopoNodes().forEach(topoNode -> topoNode.setIpList(topoNodes.get(topoNode)));
        }
    }

    private void checkStepInstanceHostNonEmpty(StepInstanceDTO stepInstance) {
        if (!isStepContainsHostProps(stepInstance)) {
            return;
        }
        ServersDTO targetServers = stepInstance.getTargetServers();
        if (targetServers == null || CollectionUtils.isEmpty(targetServers.getIpList())) {
            log.warn("Empty target server! stepInstanceName: {}", stepInstance.getName());
            throw new FailedPreconditionException(ErrorCode.STEP_TARGET_HOST_EMPTY,
                new String[]{stepInstance.getName()});
        }
        if (stepInstance.isFileStep()) {
            List<FileSourceDTO> fileSourceList = stepInstance.getFileSourceList();
            for (FileSourceDTO fileSource : fileSourceList) {
                // 远程文件分发需要判断文件源主机是否为空
                if (TaskFileTypeEnum.SERVER.getType() == fileSource.getFileType()) {
                    ServersDTO servers = fileSource.getServers();
                    if (servers != null && CollectionUtils.isEmpty(servers.getIpList())) {
                        log.warn("Empty file source server, stepInstanceName: {}", stepInstance.getName());
                        throw new FailedPreconditionException(ErrorCode.STEP_SOURCE_HOST_EMPTY,
                            new String[]{stepInstance.getName()});
                    }
                }
            }
        }
    }

    private boolean isStepContainsHostProps(StepInstanceBaseDTO stepInstance) {
        // 判断步骤是否包含主机信息
        return !stepInstance.getExecuteType().equals(MANUAL_CONFIRM.getValue());
    }

    private void fillTaskInstanceHostDetail(TaskInstanceDTO taskInstance,
                                            List<StepInstanceDTO> stepInstanceList,
                                            Collection<TaskVariableDTO> variables,
                                            ServiceListAppHostResultDTO hosts) {

        fillHostAgent(taskInstance, hosts);

        Map<String, HostDTO> hostMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(hosts.getValidHosts())) {
            hosts.getValidHosts().forEach(host -> {
                hostMap.put("hostId:" + host.getHostId(), host);
                hostMap.put("hostIp:" + host.toCloudIp(), host);
            });
        }
        if (CollectionUtils.isNotEmpty(hosts.getNotInAppHosts())) {
            hosts.getNotInAppHosts().forEach(host -> {
                hostMap.put("hostId:" + host.getHostId(), host);
                hostMap.put("hostIp:" + host.toCloudIp(), host);
            });
        }

        for (StepInstanceDTO stepInstance : stepInstanceList) {
            if (!isStepContainsHostProps(stepInstance)) {
                continue;
            }
            // 目标主机设置主机详情
            fillTargetHostDetail(stepInstance, hostMap);
            // 文件源设置主机详情
            fillFileSourceHostDetail(stepInstance, hostMap);
        }

        if (CollectionUtils.isNotEmpty(variables)) {
            variables.forEach(variable -> {
                if (variable.getType() == TaskVariableTypeEnum.HOST_LIST.getType()) {
                    fillServersDetail(variable.getTargetServers(), hostMap);
                }
            });
        }
    }

    private void fillHostAgent(TaskInstanceDTO taskInstance, ServiceListAppHostResultDTO hosts) {
        boolean isUsingGseV2 = isUsingGseV2(taskInstance,
            ListUtil.union(hosts.getValidHosts(), hosts.getNotInAppHosts()));
        /*
         * 后续下发任务给GSE会根据agentId路由请求到GSE1.0/2.0。如果要使用GSE2.0，那么直接使用原始bk_agent_id;如果要使用GSE1.0,
         * 按照{云区域ID:ip}的方式构造agent_id
         */
        Set<HostDTO> invalidAgentIdHosts = new HashSet<>();

        if (CollectionUtils.isNotEmpty(hosts.getValidHosts())) {
            hosts.getValidHosts().forEach(host -> setHostAgentId(isUsingGseV2, host, invalidAgentIdHosts));
        }

        if (CollectionUtils.isNotEmpty(hosts.getNotInAppHosts())) {
            hosts.getNotInAppHosts().forEach(host -> setHostAgentId(isUsingGseV2, host, invalidAgentIdHosts));
        }

        if (CollectionUtils.isNotEmpty(invalidAgentIdHosts)) {
            // 如果存在主机没有agentID，不影响影响整个任务的执行。所以这里仅输出日志，不拦截整个任务的执行。后续执行代码会处理`主机没有agentId`的情况
            log.warn("Contains invalid agent id host, appId: {}, isUsingGseV2: {}, invalidHosts: {}",
                taskInstance.getAppId(), isUsingGseV2, invalidAgentIdHosts);
        }

        setAgentStatus(hosts.getValidHosts());
        setAgentStatus(hosts.getNotInAppHosts());
    }

    private void setHostAgentId(boolean isUsingGseV2, HostDTO host, Set<HostDTO> invalidAgentIdHosts) {
        // 如果对接GSE1.0,使用云区域+ipv4构造agentId
        if (!isUsingGseV2) {
            host.setAgentId(host.toCloudIp());
        }
        if (StringUtils.isBlank(host.getAgentId())) {
            invalidAgentIdHosts.add(host);
        }
    }

    private void fillTargetHostDetail(StepInstanceDTO stepInstance, Map<String, HostDTO> hostMap) {
        fillServersDetail(stepInstance.getTargetServers(), hostMap);
    }

    private void fillFileSourceHostDetail(StepInstanceDTO stepInstance, Map<String, HostDTO> hostMap) {
        if (stepInstance.getExecuteType().equals(SEND_FILE.getValue())) {
            List<FileSourceDTO> fileSourceList = stepInstance.getFileSourceList();
            if (fileSourceList != null) {
                for (FileSourceDTO fileSource : fileSourceList) {
                    fillServersDetail(fileSource.getServers(), hostMap);
                }
            }
        }
    }

    private void fillServersDetail(ServersDTO servers, Map<String, HostDTO> hostMap) {
        if (servers != null) {
            fillHostsDetail(servers.getStaticIpList(), hostMap);
            if (CollectionUtils.isNotEmpty(servers.getDynamicServerGroups())) {
                servers.getDynamicServerGroups().forEach(group -> fillHostsDetail(group.getIpList(), hostMap));
            }
            if (CollectionUtils.isNotEmpty(servers.getTopoNodes())) {
                servers.getTopoNodes().forEach(topoNode -> fillHostsDetail(topoNode.getIpList(), hostMap));
            }
            servers.setIpList(servers.extractHosts());
        }
    }

    private void fillHostsDetail(Collection<HostDTO> hosts, Map<String, HostDTO> hostMap) {
        if (CollectionUtils.isNotEmpty(hosts)) {
            hosts.forEach(host -> {
                HostDTO hostDetail;
                if (host.getHostId() != null) {
                    hostDetail = hostMap.get("hostId:" + host.getHostId());
                } else {
                    hostDetail = hostMap.get("hostIp:" + host.toCloudIp());
                }
                host.updateByHost(hostDetail);
            });
        }
    }

    private Set<HostDTO> extractHosts(List<StepInstanceDTO> stepInstanceList,
                                      Collection<TaskVariableDTO> variables) {
        Set<HostDTO> hosts = new HashSet<>();
        for (StepInstanceDTO stepInstance : stepInstanceList) {
            if (!isStepContainsHostProps(stepInstance)) {
                continue;
            }
            if (stepInstance.getTargetServers() != null) {
                hosts.addAll(stepInstance.getTargetServers().extractHosts());
            }
            if (stepInstance.getExecuteType().equals(SEND_FILE.getValue())) {
                List<FileSourceDTO> fileSourceList = stepInstance.getFileSourceList();
                if (fileSourceList != null) {
                    for (FileSourceDTO fileSource : fileSourceList) {
                        if (fileSource.getServers() != null) {
                            hosts.addAll(fileSource.getServers().extractHosts());
                        }
                    }
                }
            }
        }
        if (CollectionUtils.isNotEmpty(variables)) {
            variables.stream()
                .filter(variable -> variable.getType() == TaskVariableTypeEnum.HOST_LIST.getType()
                    && variable.getTargetServers() != null)
                .forEach(variable -> hosts.addAll(variable.getTargetServers().extractHosts()));
        }
        return hosts;
    }


    private void throwHostInvalidException(Collection<HostDTO> invalidHosts) {
        String hostListStr = StringUtils.join(invalidHosts.stream()
            .map(this::printHostIdOrIp).collect(Collectors.toList()), ",");
        log.warn("The following hosts are invalid, hosts={}", invalidHosts);
        throw new FailedPreconditionException(ErrorCode.HOST_INVALID, new Object[]{invalidHosts.size(), hostListStr});
    }

    private String printHostIdOrIp(HostDTO host) {
        if (StringUtils.isNotBlank(host.getPrimaryIp())) {
            // 优先使用ip，可读性更好
            return "(ip:" + host.getPrimaryIp() + ")";
        } else {
            return "(host_id:" + host.getHostId() + ")";
        }
    }

    private void checkStepInstanceConstraint(TaskInstanceDTO taskInstance, List<StepInstanceDTO> stepInstanceList) {
        String appCode = taskInstance.getAppCode();
        Long appId = taskInstance.getAppId();
        String taskName = taskInstance.getName();

        for (StepInstanceDTO stepInstance : stepInstanceList) {
            String operator = stepInstance.getOperator();
            if (stepInstance.isFileStep()) {
                int targetServerSize = stepInstance.getTargetServerTotalCount();
                int totalSourceFileSize = 0;
                for (FileSourceDTO fileSource : stepInstance.getFileSourceList()) {
                    int sourceServerSize = 1;
                    Integer fileType = fileSource.getFileType();
                    if (fileType == TaskFileTypeEnum.SERVER.getType() && fileSource.getServers() != null) {
                        sourceServerSize = CollectionUtils.size(fileSource.getServers().getIpList());
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
                int targetServerSize = stepInstance.getTargetServerTotalCount();
                if (targetServerSize > 10000) {
                    TASK_MONITOR_LOGGER.info("LargeTask|type:script|taskName:{}|appCode:{}|appId:{}|operator:{}"
                            + "|targetServerSize:{}",
                        taskName, appCode, appId, operator, targetServerSize);
                }
                if (targetServerSize > jobExecuteConfig.getScriptTaskMaxTargetServer()) {
                    log.info("Reject large task|type:file|taskName:{}|appCode:{}|appId:{}|operator" +
                            ":{}|targetServerSize:{}|maxAllowedSize:{}", taskName, appCode, appId, operator,
                        targetServerSize, jobExecuteConfig.getScriptTaskMaxTargetServer());
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
            StepInstanceDTO originStepInstance = taskInstanceService.getStepInstanceByTaskInstanceId(taskInstanceId);
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
            ServiceTaskPlanDTO jobPlan = taskInfo.getJobPlan();
            TaskInstanceDTO taskInstance = taskInfo.getTaskInstance();
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

            // 获取主机列表
            watch.start("acquireAndSetHosts");
            ServiceListAppHostResultDTO hosts =
                acquireAndSetHosts(taskInstance, stepInstanceList, finalVariableValueMap.values());
            watch.stop();

            // 获取主机白名单
            watch.start("getHostAllowedActions");
            Map<Long, List<String>> hostAllowActions = getHostAllowedActions(taskInstance.getAppId(),
                ListUtil.union(hosts.getValidHosts(), hosts.getNotInAppHosts()));
            watch.stop();

            //检查主机
            watch.start("checkHosts");
            checkHosts(stepInstanceList, hosts, hostAllowActions);
            watch.stop();

            // 检查步骤约束
            watch.start("checkStepInstanceConstraint");
            checkStepInstanceConstraint(taskInstance, stepInstanceList);
            watch.stop();

            if (!executeParam.isSkipAuth()) {
                watch.start("auth-execute-job");
                authExecuteJobPlan(executeParam.getOperator(), executeParam.getAppId(), jobPlan, stepInstanceList,
                    hostAllowActions);
                watch.stop();
            }

            watch.start("saveInstance");
            // 这里保存的stepInstanceList已经是完成变量解析之后的步骤信息了
            saveTaskInstance(taskInstance, stepInstanceList, finalVariableValueMap);
            watch.stop();

            Set<HostDTO> allHosts = extractHosts(taskInstance.getStepInstances(), null);
            taskInstance.setAllHosts(allHosts);

            // 保存作业实例与主机的关系，优化根据主机检索作业执行历史的效率
            watch.start("saveTaskInstanceHosts");
            saveTaskInstanceHosts(taskInstance.getId(), allHosts);
            watch.stop();

            watch.start("saveOperationLog");
            taskOperationLogService.saveOperationLog(buildTaskOperationLog(taskInstance, taskInstance.getOperator(),
                UserOperationEnum.START));
            watch.stop();

            // 启动作业
            watch.start("startJob");
            startTask(taskInstance.getId());
            watch.stop();

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
            if (stepInstance.getTargetServers() != null
                && CollectionUtils.isNotEmpty(stepInstance.getTargetServers().getDynamicServerGroups())) {
                standardizeServerDynamicGroupId(stepInstance.getTargetServers());
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

    private void standardizeServerDynamicGroupId(ServersDTO servers) {
        if (servers != null && CollectionUtils.isNotEmpty(servers.getDynamicServerGroups())) {
            servers.getDynamicServerGroups().forEach(this::standardizeDynamicGroupId);
        }
    }

    private void standardizeTaskVarDynamicGroupId(Collection<TaskVariableDTO> variables) {
        if (CollectionUtils.isNotEmpty(variables)) {
            variables.stream().filter(variable -> variable.getTargetServers() != null)
                .forEach(variable -> standardizeServerDynamicGroupId(variable.getTargetServers()));
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
        log.info("Create task instance for task, appId={}, planId={}, operator={}, attributes={}", appId, planId,
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
        ServersDTO authServers = new ServersDTO();
        Set<Long> accountIds = new HashSet<>();
        for (StepInstanceDTO stepInstance : stepInstanceList) {
            if (!stepInstance.isScriptStep() && !stepInstance.isFileStep()) {
                continue;
            }
            Pair<ServersDTO, Set<Long>> needAuthHostsAndAccounts =
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

    private Pair<ServersDTO, Set<Long>> extractNeedAuthHostsAndAccounts(StepInstanceDTO stepInstance,
                                                                        Map<Long, List<String>> whiteHostAllowActions) {
        ServersDTO authServers = new ServersDTO();
        Set<Long> accountIds = new HashSet<>();
        accountIds.add(stepInstance.getAccountId());
        if (stepInstance.isFileStep()) {
            ServersDTO stepTargetServers = stepInstance.getTargetServers().clone();
            filterServerDoNotRequireAuth(ActionScopeEnum.FILE_DISTRIBUTION, stepTargetServers,
                whiteHostAllowActions);
            authServers.merge(stepTargetServers);
            if (!CollectionUtils.isEmpty(stepInstance.getFileSourceList())) {
                stepInstance.getFileSourceList().stream().filter(fileSource -> !fileSource.isLocalUpload())
                    .forEach(fileSource -> {
                            ServersDTO stepFileSourceServers = fileSource.getServers().clone();
                            filterServerDoNotRequireAuth(ActionScopeEnum.FILE_DISTRIBUTION, stepFileSourceServers,
                                whiteHostAllowActions);
                            authServers.merge(stepFileSourceServers);
                            if (fileSource.getAccountId() != null) {
                                accountIds.add(fileSource.getAccountId());
                            }
                        }
                    );
            }
        } else if (stepInstance.isScriptStep()) {
            ServersDTO stepTargetServers = stepInstance.getTargetServers().clone();
            filterServerDoNotRequireAuth(ActionScopeEnum.SCRIPT_EXECUTE, stepTargetServers, whiteHostAllowActions);
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
                ScriptTypeEnum scriptType = ScriptTypeEnum.valueOf(step.getScriptStepInfo().getType());
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

    private TaskStepTypeEnum getTaskStepTypeFromExecuteType(StepExecuteTypeEnum executeType) throws ServiceException {
        TaskStepTypeEnum stepType;
        if (executeType == EXECUTE_SCRIPT || executeType == EXECUTE_SQL) {
            stepType = TaskStepTypeEnum.SCRIPT;
        } else if (executeType == SEND_FILE) {
            stepType = TaskStepTypeEnum.FILE;
        } else if (executeType == MANUAL_CONFIRM) {
            stepType = TaskStepTypeEnum.APPROVAL;
        } else {
            throw new InternalException(ErrorCode.INTERNAL_ERROR);
        }
        return stepType;
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
        stepInstance.setExecuteType(stepType.getValue());
        stepInstance.setStatus(RunStatusEnum.BLANK);
        stepInstance.setOperator(operator);
        stepInstance.setAppId(appId);
        stepInstance.setCreateTime(DateUtils.currentTimeMillis());
        stepInstance.setExecuteCount(0);
        return stepInstance;
    }

    public TaskInstanceDTO createTaskInstanceForRedo(Long appId, Long taskInstanceId, String operator,
                                                     List<TaskVariableDTO> executeVariableValues) throws ServiceException {
        log.info("Create task instance for redo, appId={}, taskInstanceId={}, operator={}, attributes={}", appId,
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
            StepExecuteTypeEnum executeType = StepExecuteTypeEnum.valueOf(originStepInstance.getExecuteType());
            StepInstanceDTO stepInstance = createCommonStepInstanceDTO(appId, operator,
                originStepInstance.getStepId(), originStepInstance.getName(), executeType);
            TaskStepTypeEnum stepType = getTaskStepTypeFromExecuteType(executeType);
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

        // 获取主机列表
        ServiceListAppHostResultDTO hosts = acquireAndSetHosts(taskInstance, stepInstanceList,
            finalVariableValueMap.values());

        // 获取主机白名单
        Map<Long, List<String>> hostAllowActions = getHostAllowedActions(taskInstance.getAppId(),
            ListUtil.union(hosts.getValidHosts(), hosts.getNotInAppHosts()));

        // 检查主机合法性
        checkHosts(stepInstanceList, hosts, hostAllowActions);

        // 检查步骤约束
        checkStepInstanceConstraint(taskInstance, stepInstanceList);

        authRedoJob(operator, appId, originTaskInstance, hostAllowActions);

        saveTaskInstance(taskInstance, stepInstanceList, finalVariableValueMap);

        saveTaskInstanceHosts(taskInstance.getId(), taskInstance.getStepInstances());

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

    private void saveTaskInstance(TaskInstanceDTO taskInstance, List<StepInstanceDTO> stepInstances,
                                  Map<String, TaskVariableDTO> taskVariablesMap) throws ServiceException {
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
            long stepInstanceId = taskInstanceService.addStepInstance(stepInstance);
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
                commonVariable.setTargetServers(convertToServersDTO(variable.getDefaultTargetValue()));
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
            to.setTargetServers(from.getTargetServers());
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
        stepInstance.setScriptType(scriptStepInfo.getType());
        stepInstance.setScriptSource(scriptStepInfo.getScriptSource());

        ServiceAccountDTO accountInfo = scriptStepInfo.getAccount();
        if (accountInfo == null) {
            log.warn("Account is null! step_id:{}", step.getId());
            throw new NotFoundException(ErrorCode.ACCOUNT_NOT_EXIST);
        }

        ScriptTypeEnum scriptType = ScriptTypeEnum.valueOf(scriptStepInfo.getType());
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
        }

        ServiceTaskTargetDTO target = scriptStepInfo.getExecuteTarget();
        ServersDTO targetServers = buildFinalTargetServers(target, variableValueMap);
        stepInstance.setTargetServers(targetServers);

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
                ServersDTO targetServers = buildFinalTargetServers(target, variableValueMap);
                fileSource.setServers(targetServers);
                List<FileDetailDTO> fileList = new ArrayList<>();
                originFile.getFileLocation().forEach(fileLocation -> fileList.add(new FileDetailDTO(fileLocation)));
                fileSource.setFiles(fileList);
            } else if (originFile.getFileType() == TaskFileTypeEnum.FILE_SOURCE.getType()) {
                fileSource.setLocalUpload(false);
                fileSource.setServers(ServersDTO.emptyInstance());
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
        ServersDTO targetServers = buildFinalTargetServers(target, variableValueMap);
        stepInstance.setTargetServers(targetServers);

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

        ScriptTypeEnum scriptType = ScriptTypeEnum.valueOf(originStepInstance.getScriptType());
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

        ServersDTO targetServers = buildFinalTargetServers(originStepInstance.getTargetServers(), variableValueMap);
        stepInstance.setTargetServers(targetServers);
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
                ServersDTO targetServers = buildFinalTargetServers(newFileSource.getServers(), variableValueMap);
                newFileSource.setServers(targetServers);
                fileSourceList.add(newFileSource);
            });
            stepInstance.setFileSourceList(fileSourceList);
        }

        ServersDTO targetServers = buildFinalTargetServers(originStepInstance.getTargetServers(), variableValueMap);
        stepInstance.setTargetServers(targetServers);
    }

    private ServersDTO buildFinalTargetServers(@NotNull ServiceTaskTargetDTO target,
                                               @NotNull Map<String, TaskVariableDTO> variableValueMap)
        throws ServiceException {
        // 如果目标服务器使用主机变量，那么需要解析主机变量
        if (StringUtils.isNotBlank(target.getVariable())) {
            return getServerValueFromVariable(target.getVariable(), variableValueMap);
        } else {
            return convertToServersDTO(target);
        }
    }

    private ServersDTO buildFinalTargetServers(ServersDTO target, Map<String, TaskVariableDTO> variableValueMap)
        throws ServiceException {
        // 如果目标服务器使用主机变量，那么需要解析主机变量
        if (target != null && StringUtils.isNotBlank(target.getVariable())) {
            return getServerValueFromVariable(target.getVariable(), variableValueMap);
        } else {
            return target;
        }
    }

    private ServersDTO getServerValueFromVariable(@NotNull String hostVariableName,
                                                  @NotNull Map<String, TaskVariableDTO> variableValueMap)
        throws ServiceException {
        TaskVariableDTO serverVariable = variableValueMap.get(hostVariableName);
        if (serverVariable == null) {
            log.warn("Target server variable is not exist.variable={}", hostVariableName);
            throw new FailedPreconditionException(ErrorCode.TASK_INSTANCE_RELATED_HOST_VAR_NOT_EXIST,
                new String[]{hostVariableName});
        }

        if (serverVariable.getTargetServers() == null) {
            return null;
        }
        ServersDTO targetServers = serverVariable.getTargetServers().clone();
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


    private ServersDTO convertToServersDTO(ServiceTaskTargetDTO taskTarget) {
        if (taskTarget == null) {
            return null;
        }
        ServersDTO servers = new ServersDTO();
        ServiceTaskHostNodeDTO targetServers = taskTarget.getTargetServer();
        List<ServiceHostInfoDTO> hostList = targetServers.getHostList();
        if (hostList != null && !hostList.isEmpty()) {
            List<HostDTO> staticIpList = new ArrayList<>();
            for (ServiceHostInfoDTO hostInfo : hostList) {
                staticIpList.add(
                    HostDTO.fromHostIdOrCloudIp(hostInfo.getHostId(), hostInfo.getCloudAreaId(), hostInfo.getIp()));
            }
            servers.setStaticIpList(staticIpList);
        }

        List<String> groupIdList = targetServers.getDynamicGroupId();
        if (groupIdList != null && !groupIdList.isEmpty()) {
            List<DynamicServerGroupDTO> groups = new ArrayList<>();
            for (String groupId : groupIdList) {
                groups.add(new DynamicServerGroupDTO(groupId));
            }
            servers.setDynamicServerGroups(groups);
        }

        List<ServiceTaskNodeInfoDTO> topoNodeIdList = targetServers.getNodeInfoList();
        if (topoNodeIdList != null && !topoNodeIdList.isEmpty()) {
            List<DynamicServerTopoNodeDTO> topoNodes = new ArrayList<>();
            for (ServiceTaskNodeInfoDTO topoNodeId : topoNodeIdList) {
                topoNodes.add(new DynamicServerTopoNodeDTO(topoNodeId.getId(), topoNodeId.getType()));
            }
            servers.setTopoNodes(topoNodes);
        }
        return servers;
    }

    private void setAgentStatus(List<HostDTO> hosts) {
        if (CollectionUtils.isEmpty(hosts)) {
            return;
        }
        long start = System.currentTimeMillis();

        List<HostAgentStateQuery> hostAgentStateQueryList = new ArrayList<>(hosts.size());
        Map<HostDTO, HostAgentStateQuery> hostAgentStateQueryMap = new HashMap<>(hosts.size());
        hosts.forEach(hostDTO -> {
            HostAgentStateQuery hostAgentStateQuery = HostAgentStateQuery.from(hostDTO);
            hostAgentStateQueryList.add(hostAgentStateQuery);
            hostAgentStateQueryMap.put(hostDTO, hostAgentStateQuery);
        });

        // 此处用于记录下发任务时的Agent状态快照数据，因此使用最终真实下发任务的agentId获取状态
        Map<String, AgentState> agentStateMap = preferV2AgentStateClient.batchGetAgentState(hostAgentStateQueryList);

        for (HostDTO host : hosts) {
            HostAgentStateQuery hostAgentStateQuery = hostAgentStateQueryMap.get(host);
            String effectiveAgentId = preferV2AgentStateClient.getEffectiveAgentId(hostAgentStateQuery);
            if (StringUtils.isEmpty(effectiveAgentId)) {
                host.setAlive(AgentAliveStatusEnum.NOT_ALIVE.getStatusValue());
                continue;
            }
            AgentState agentState = agentStateMap.get(effectiveAgentId);
            if (agentState != null) {
                host.setAlive(AgentAliveStatusEnum.fromAgentState(agentState).getStatusValue());
            } else {
                host.setAlive(AgentAliveStatusEnum.NOT_ALIVE.getStatusValue());
            }
        }

        long cost = System.currentTimeMillis() - start;
        if (cost > 1000) {
            log.warn("SetAgentStatus slow, hostSize: {}, cost:{} ms", hosts.size(), cost);
        }
    }

    @Override
    public Integer doStepOperation(Long appId, String operator,
                                   StepOperationDTO stepOperation) throws ServiceException {
        long stepInstanceId = stepOperation.getStepInstanceId();
        StepOperationEnum operation = stepOperation.getOperation();
        log.info("Operate step, appId:{}, stepInstanceId:{}, operator:{}, operation:{}", appId, stepInstanceId,
            operator, operation.getValue());
        StepInstanceDTO stepInstance = taskInstanceService.getStepInstanceDetail(stepInstanceId);
        if (stepInstance == null) {
            log.warn("Step instance {} is not exist", stepInstanceId);
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }
        if (!stepInstance.getAppId().equals(appId)) {
            log.warn("Step instance {} is not in app:{}", stepInstance, appId);
            throw new NotFoundException(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }
        int executeCount = stepInstance.getExecuteCount();
        switch (operation) {
            case CONFIRM_CONTINUE:
                confirmContinue(stepInstance, operator, stepOperation.getConfirmReason());
                break;
            case RETRY_FAIL_IP:
                retryStepFail(stepInstance, operator);
                executeCount++;
                break;
            case IGNORE_ERROR:
                ignoreError(stepInstance, operator);
                break;
            case RETRY_ALL_IP:
                retryStepAll(stepInstance, operator);
                executeCount++;
                break;
            case CONFIRM_TERMINATE:
                confirmTerminate(stepInstance, operator, stepOperation.getConfirmReason());
                break;
            case CONFIRM_RESTART:
                confirmRestart(stepInstance, operator);
                break;
            case NEXT_STEP:
                nextStep(stepInstance, operator);
                break;
            case SKIP:
                skipStep(stepInstance, operator);
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
        taskExecuteMQEventDispatcher.dispatchStepEvent(StepEvent.startStep(stepInstance.getId(),
            stepInstance.getBatch() + 1));
    }

    private void confirmTerminate(StepInstanceDTO stepInstance, String operator, String reason) {
        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(stepInstance.getTaskInstanceId());
        // 只有人工确认等待中的任务，可以进行“终止流程”操作
        if (RunStatusEnum.WAITING_USER != stepInstance.getStatus()) {
            log.warn("Unsupported operation, stepInstanceId: {}, operation: {}, stepStatus: {}",
                stepInstance.getId(), StepOperationEnum.CONFIRM_TERMINATE.name(), stepInstance.getStatus().name());
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        if (!stepInstance.getExecuteType().equals(MANUAL_CONFIRM.getValue())) {
            log.warn("StepInstance:{} is not confirm step, Unsupported Operation:{}", stepInstance.getId(),
                "confirm-terminate");
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        checkConfirmUser(taskInstance, stepInstance, operator);

        OperationLogDTO operationLog = buildCommonStepOperationLog(stepInstance, operator,
            UserOperationEnum.CONFIRM_TERMINATE);
        operationLog.getDetail().setConfirmReason(reason);
        taskOperationLogService.saveOperationLog(operationLog);

        taskInstanceService.updateConfirmReason(stepInstance.getId(), reason);
        taskInstanceService.updateStepOperator(stepInstance.getId(), operator);

        taskExecuteMQEventDispatcher.dispatchStepEvent(StepEvent.confirmStepTerminate(stepInstance.getId()));
    }

    private void confirmRestart(StepInstanceDTO stepInstance, String operator) {
        // 只有“确认终止”状态的任务，可以进行“重新发起确认”操作
        if (RunStatusEnum.CONFIRM_TERMINATED != stepInstance.getStatus()) {
            log.warn("Unsupported operation, stepInstanceId: {}, operation: {}, stepStatus: {}",
                stepInstance.getId(), StepOperationEnum.CONFIRM_RESTART.name(), stepInstance.getStatus().name());
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        if (!stepInstance.getExecuteType().equals(MANUAL_CONFIRM.getValue())) {
            log.warn("StepInstance:{} is not confirm step, Unsupported Operation:{}", stepInstance.getId(),
                "confirm-restart");
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        taskOperationLogService.saveOperationLog(buildCommonStepOperationLog(stepInstance, operator,
            UserOperationEnum.CONFIRM_RESTART));
        taskExecuteMQEventDispatcher.dispatchStepEvent(StepEvent.confirmStepRestart(stepInstance.getId()));
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
        if (!stepInstance.getExecuteType().equals(MANUAL_CONFIRM.getValue())) {
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

        taskInstanceService.updateConfirmReason(stepInstance.getId(), reason);
        taskInstanceService.updateStepOperator(stepInstance.getId(), operator);

        // 需要同步设置任务状态为RUNNING，保证客户端可以在操作完之后立马获取到运行状态，开启同步刷新
        taskInstanceService.updateTaskStatus(taskInstance.getId(), RunStatusEnum.RUNNING.getValue());

        taskExecuteMQEventDispatcher.dispatchStepEvent(StepEvent.confirmStepContinue(stepInstance.getId()));
    }

    private void nextStep(StepInstanceDTO stepInstance, String operator) {
        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(stepInstance.getTaskInstanceId());
        // 只有"终止成功"状态的任务，可以直接进入下一步
        if (RunStatusEnum.STOP_SUCCESS != stepInstance.getStatus()) {
            log.warn("Unsupported operation, stepInstanceId: {}, operation: {}, stepStatus: {}",
                stepInstance.getId(), StepOperationEnum.NEXT_STEP.name(), stepInstance.getStatus().name());
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        taskOperationLogService.saveOperationLog(buildCommonStepOperationLog(stepInstance, operator,
            UserOperationEnum.NEXT_STEP));

        // 需要同步设置任务状态为RUNNING，保证客户端可以在操作完之后立马获取到运行状态，开启同步刷新
        taskInstanceService.updateTaskStatus(taskInstance.getId(), RunStatusEnum.RUNNING.getValue());
        taskExecuteMQEventDispatcher.dispatchStepEvent(StepEvent.nextStep(stepInstance.getId()));
    }

    private void retryStepFail(StepInstanceDTO stepInstance, String operator) {
        if (!isStepRetryable(stepInstance.getStatus())) {
            log.warn("Unsupported operation, stepInstanceId: {}, operation: {}, stepStatus: {}",
                stepInstance.getId(), StepOperationEnum.RETRY_FAIL_IP.name(), stepInstance.getStatus().name());
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        // 需要同步设置任务状态为RUNNING，保证客户端可以在操作完之后立马获取到运行状态，开启同步刷新
        taskInstanceService.updateTaskStatus(stepInstance.getTaskInstanceId(), RunStatusEnum.RUNNING.getValue());
        taskInstanceService.addStepInstanceExecuteCount(stepInstance.getId());
        taskExecuteMQEventDispatcher.dispatchStepEvent(StepEvent.retryStepFail(stepInstance.getId()));
        OperationLogDTO operationLog = buildCommonStepOperationLog(stepInstance, operator,
            UserOperationEnum.RETRY_STEP_FAIL);
        taskOperationLogService.saveOperationLog(operationLog);
    }

    private void retryStepAll(StepInstanceDTO stepInstance, String operator) {
        if (!isStepRetryable(stepInstance.getStatus())) {
            log.warn("Unsupported operation, stepInstanceId: {}, operation: {}, stepStatus: {}",
                stepInstance.getId(), StepOperationEnum.RETRY_ALL_IP.name(), stepInstance.getStatus().name());
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        // 需要同步设置任务状态为RUNNING，保证客户端可以在操作完之后立马获取到运行状态，开启同步刷新
        taskInstanceService.updateTaskStatus(stepInstance.getTaskInstanceId(), RunStatusEnum.RUNNING.getValue());
        taskInstanceService.addStepInstanceExecuteCount(stepInstance.getId());
        taskExecuteMQEventDispatcher.dispatchStepEvent(StepEvent.retryStepAll(stepInstance.getId()));
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

    private void ignoreError(StepInstanceDTO stepInstance, String operator) {
        // 只有“执行失败”的作业可以忽略错误进入下一步
        if (stepInstance.getStatus() != RunStatusEnum.FAIL &&
            stepInstance.getStatus() != RunStatusEnum.ABNORMAL_STATE) {
            log.warn("Unsupported operation, stepInstanceId: {}, operation: {}, stepStatus: {}",
                stepInstance.getId(), StepOperationEnum.IGNORE_ERROR.name(), stepInstance.getStatus().name());
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        // 需要同步设置任务状态为RUNNING，保证客户端可以在操作完之后立马获取到运行状态，开启同步刷新
        taskInstanceService.updateTaskStatus(stepInstance.getTaskInstanceId(), RunStatusEnum.RUNNING.getValue());
        taskExecuteMQEventDispatcher.dispatchStepEvent(StepEvent.ignoreError(stepInstance.getId()));
        OperationLogDTO operationLog = buildCommonStepOperationLog(stepInstance, operator,
            UserOperationEnum.IGNORE_ERROR);
        taskOperationLogService.saveOperationLog(operationLog);
    }

    private void skipStep(StepInstanceDTO stepInstance, String operator) {
        // 只有“强制终止中”的作业可以跳过
        if (stepInstance.getStatus() != RunStatusEnum.STOPPING) {
            log.warn("Unsupported operation, stepInstanceId: {}, operation: {}, stepStatus: {}",
                stepInstance.getId(), StepOperationEnum.SKIP.name(), stepInstance.getStatus().name());
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        taskExecuteMQEventDispatcher.dispatchStepEvent(StepEvent.skipStep(stepInstance.getId()));
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
    public void terminateJob(String username, Long appId, Long taskInstanceId) throws ServiceException {
        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(taskInstanceId);
        if (taskInstance == null || !taskInstance.getAppId().equals(appId)) {
            log.warn("Task instance is not exist, appId:{}, taskInstanceId:{}", appId, taskInstance);
            throw new NotFoundException(ErrorCode.TASK_INSTANCE_NOT_EXIST);
        }
        if (RunStatusEnum.RUNNING != taskInstance.getStatus()
            && RunStatusEnum.WAITING_USER != taskInstance.getStatus()) {
            log.warn("TaskInstance:{} status is not running/waiting, should not terminate it!", taskInstance.getId());
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        taskExecuteMQEventDispatcher.dispatchJobEvent(JobEvent.stopJob(taskInstanceId));
        OperationLogDTO operationLog = buildTaskOperationLog(taskInstance, username, UserOperationEnum.TERMINATE_JOB);
        taskOperationLogService.saveOperationLog(operationLog);
    }

    @Override
    public void doTaskOperation(Long appId, String operator, long taskInstanceId,
                                TaskOperationEnum operation) throws ServiceException {
        log.info("Operate task instance, appId:{}, taskInstanceId:{}, operator:{}, operation:{}", appId,
            taskInstanceId, operator, operation.getValue());
        if (operation == TaskOperationEnum.TERMINATE_JOB) {
            terminateJob(operator, appId, taskInstanceId);
        } else {
            log.warn("Undefined task operation!");
        }
    }

    @Override
    public void authExecuteJobPlan(TaskExecuteParam executeParam) throws ServiceException {
        StopWatch watch = new StopWatch("authJobPlan");
        TaskInfo taskInfo = buildTaskInfoFromExecuteParam(executeParam, watch);

        watch.start("acquireAndSetHosts");
        ServiceListAppHostResultDTO hosts =
            acquireAndSetHosts(taskInfo.getTaskInstance(), taskInfo.getStepInstances(),
                taskInfo.getVariables() != null ? taskInfo.getVariables().values() : null);
        watch.stop();

        // 获取主机白名单
        watch.start("getHostAllowedActions");
        Map<Long, List<String>> hostAllowActions = getHostAllowedActions(taskInfo.getTaskInstance().getAppId(),
            ListUtil.union(hosts.getValidHosts(), hosts.getNotInAppHosts()));
        watch.stop();

        // 检查主机合法性
        watch.start("checkHost");
        checkHosts(taskInfo.getStepInstances(), hosts, hostAllowActions);
        watch.stop();

        watch.start("auth-execute-job");
        authExecuteJobPlan(executeParam.getOperator(), executeParam.getAppId(), taskInfo.getJobPlan(),
            taskInfo.getStepInstances(), hostAllowActions);
        watch.stop();

        if (watch.getTotalTimeMillis() > 500) {
            log.warn("authJobPlan is slow, watcher: {}", watch.prettyPrint());
        }
    }
}
