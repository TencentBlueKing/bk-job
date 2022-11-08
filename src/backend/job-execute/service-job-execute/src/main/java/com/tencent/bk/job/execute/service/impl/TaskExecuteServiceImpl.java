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

import brave.Tracing;
import com.google.common.collect.Lists;
import com.tencent.bk.job.common.cc.model.CcInstanceDTO;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.exception.AbortedException;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.exception.ResourceExhaustedException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.gse.constants.AgentStatusEnum;
import com.tencent.bk.job.common.gse.service.QueryAgentStatusClient;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.common.trace.executors.TraceableExecutorService;
import com.tencent.bk.job.common.util.ArrayUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.auth.ExecuteAuthService;
import com.tencent.bk.job.execute.client.ServiceUserResourceClient;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.constants.ScriptSourceEnum;
import com.tencent.bk.job.execute.constants.StepOperationEnum;
import com.tencent.bk.job.execute.constants.TaskOperationEnum;
import com.tencent.bk.job.execute.constants.UserOperationEnum;
import com.tencent.bk.job.execute.engine.TaskExecuteControlMsgSender;
import com.tencent.bk.job.execute.engine.evict.TaskEvictPolicyExecutor;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.engine.util.TimeoutUtils;
import com.tencent.bk.job.execute.model.AccountDTO;
import com.tencent.bk.job.execute.model.DynamicServerGroupDTO;
import com.tencent.bk.job.execute.model.DynamicServerTopoNodeDTO;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.OperationLogDTO;
import com.tencent.bk.job.execute.model.ServersDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.StepOperationDTO;
import com.tencent.bk.job.execute.model.TaskExecuteParam;
import com.tencent.bk.job.execute.model.TaskInfo;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.AccountService;
import com.tencent.bk.job.execute.service.DangerousScriptCheckService;
import com.tencent.bk.job.execute.service.HostService;
import com.tencent.bk.job.execute.service.ScriptService;
import com.tencent.bk.job.execute.service.TaskExecuteService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceVariableService;
import com.tencent.bk.job.execute.service.TaskOperationLogService;
import com.tencent.bk.job.execute.service.TaskPlanService;
import com.tencent.bk.job.execute.util.LoggerFactory;
import com.tencent.bk.job.manage.common.consts.JobResourceStatusEnum;
import com.tencent.bk.job.manage.common.consts.account.AccountCategoryEnum;
import com.tencent.bk.job.manage.common.consts.notify.JobRoleEnum;
import com.tencent.bk.job.manage.common.consts.notify.ResourceTypeEnum;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.common.consts.whiteip.ActionScopeEnum;
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
import org.apache.commons.lang3.tuple.ImmutablePair;
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
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
    private final TaskExecuteControlMsgSender controlMsgSender;
    private final TaskPlanService taskPlanService;
    private final TaskInstanceVariableService taskInstanceVariableService;
    private final QueryAgentStatusClient queryAgentStatusClient;
    private final TaskOperationLogService taskOperationLogService;
    private final TaskInstanceService taskInstanceService;
    private final HostService hostService;
    private final ServiceUserResourceClient userResource;
    private final ExecuteAuthService executeAuthService;
    private final ExecutorService GET_HOSTS_BY_TOPO_EXECUTOR;
    private final DangerousScriptCheckService dangerousScriptCheckService;
    private final JobExecuteConfig jobExecuteConfig;
    private final TaskEvictPolicyExecutor taskEvictPolicyExecutor;

    private static final Logger TASK_MONITOR_LOGGER = LoggerFactory.TASK_MONITOR_LOGGER;

    @Autowired
    public TaskExecuteServiceImpl(AccountService accountService,
                                  TaskInstanceService taskInstanceService,
                                  TaskExecuteControlMsgSender controlMsgSender,
                                  TaskPlanService taskPlanService,
                                  TaskInstanceVariableService taskInstanceVariableService,
                                  QueryAgentStatusClient queryAgentStatusClient,
                                  TaskOperationLogService taskOperationLogService,
                                  ScriptService scriptService,
                                  HostService hostService,
                                  ServiceUserResourceClient userResource,
                                  ExecuteAuthService executeAuthService,
                                  Tracing tracing,
                                  DangerousScriptCheckService dangerousScriptCheckService,
                                  JobExecuteConfig jobExecuteConfig,
                                  TaskEvictPolicyExecutor taskEvictPolicyExecutor) {
        this.accountService = accountService;
        this.taskInstanceService = taskInstanceService;
        this.controlMsgSender = controlMsgSender;
        this.taskPlanService = taskPlanService;
        this.taskInstanceVariableService = taskInstanceVariableService;
        this.queryAgentStatusClient = queryAgentStatusClient;
        this.taskOperationLogService = taskOperationLogService;
        this.scriptService = scriptService;
        this.hostService = hostService;
        this.userResource = userResource;
        this.executeAuthService = executeAuthService;
        this.GET_HOSTS_BY_TOPO_EXECUTOR = new TraceableExecutorService(new ThreadPoolExecutor(50,
            100, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>()), tracing);
        this.dangerousScriptCheckService = dangerousScriptCheckService;
        this.jobExecuteConfig = jobExecuteConfig;
        this.taskEvictPolicyExecutor = taskEvictPolicyExecutor;
    }

    private static List<IpDTO> getHostsContainsNotAllowedAction(Map<IpDTO, Set<String>> hostBindActions,
                                                                Map<IpDTO, List<String>> hostAllowedActions) {
        List<IpDTO> invalidHosts = new ArrayList<>();
        for (Map.Entry<IpDTO, Set<String>> binding : hostBindActions.entrySet()) {
            IpDTO host = binding.getKey();
            if (!hostAllowedActions.containsKey(host)
                || !hostAllowedActions.get(host).containsAll(binding.getValue())) {
                invalidHosts.add(host);
            }
        }
        return invalidHosts;
    }

    @Override
    public Long createTaskInstanceFast(TaskInstanceDTO taskInstance,
                                       StepInstanceDTO stepInstance) throws ServiceException {
        log.info("Begin to create task instance and step instance for fast-execution-task, taskInstance: {}, " +
            "stepInstance: {}", taskInstance, stepInstance);
        StopWatch watch = new StopWatch("createTaskInstanceFast");
        // 检查任务是否应当被驱逐
        checkTaskEvict(taskInstance);
        standardizeStepDynamicGroupId(Collections.singletonList(stepInstance));
        adjustStepTimeout(stepInstance);
        try {
            // 设置脚本信息
            watch.start("checkAndSetScriptInfoIfScriptTask");
            checkAndSetScriptInfoForFast(taskInstance, stepInstance);
            watch.stop();
            // 设置账号信息
            watch.start("setAccountInfo");
            checkAndSetAccountInfo(stepInstance, taskInstance.getAppId());
            watch.stop();
            // 获取ip列表
            watch.start("setServerInfoFastJob");
            setServerInfoFastJob(stepInstance);
            watch.stop();
            //检查ip
            watch.start("checkHosts");
            checkHosts(stepInstance, shouldIgnoreInvalidHost(taskInstance));
            watch.stop();

            // 检查步骤约束
            watch.start("checkStepInstanceConstraint");
            checkStepInstanceConstraint(taskInstance, Collections.singletonList(stepInstance));
            watch.stop();

            watch.start("authFastExecute");
            authFastExecute(taskInstance, stepInstance);
            watch.stop();

            watch.start("saveInstance");
            Long taskInstanceId = taskInstanceService.addTaskInstance(taskInstance);
            taskInstance.setId(taskInstanceId);

            stepInstance.setTaskInstanceId(taskInstanceId);
            stepInstance.setStepNum(1);
            stepInstance.setStepOrder(1);
            long stepInstanceId = taskInstanceService.addStepInstance(stepInstance);
            stepInstance.setId(stepInstanceId);
            watch.stop();

            watch.start("saveOperationLog");
            taskOperationLogService.saveOperationLog(buildTaskOperationLog(taskInstance, taskInstance.getOperator(),
                UserOperationEnum.START));
            watch.stop();
            return taskInstanceId;
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
            if (watch.getTotalTimeMillis() > 1000) {
                log.warn("CreateTaskInstanceFast is slow, statistics: {}", watch.prettyPrint());
            }
        }
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

    private ServiceScriptDTO checkAndSetScriptInfoForFast(
        TaskInstanceDTO taskInstance,
        StepInstanceDTO stepInstance) throws ServiceException {
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
        return script;
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
                new String[] {
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

    private void authFastExecute(TaskInstanceDTO taskInstance, StepInstanceDTO stepInstance) {
        AuthResult authResult;
        if (stepInstance.isScriptStep()) {
            // 鉴权脚本任务
            authResult = authExecuteScript(taskInstance, stepInstance);
        } else {
            // 鉴权文件任务
            authResult = authFileTransfer(taskInstance, stepInstance);
        }

        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }
    }

    private AuthResult authExecuteScript(TaskInstanceDTO taskInstance, StepInstanceDTO stepInstance) {
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
        filterServerDoNotRequireAuth(appId, servers, ActionScopeEnum.SCRIPT_EXECUTE);
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
     * 过滤掉白名单的机器/允许忽略的非法主机、topo、动态分组
     */
    private void filterServerDoNotRequireAuth(long appId, ServersDTO servers, ActionScopeEnum action) {

        if (CollectionUtils.isNotEmpty(servers.getStaticIpList())) {
            servers.setStaticIpList(servers.getStaticIpList().stream()
                .filter(host -> {
                    boolean isWhiteIp = hostService.isMatchWhiteIpRule(appId, host, action.name());
                    if (isWhiteIp) {
                        log.info("Host: {} is white ip, skip auth!", host.convertToStrIp());
                    }
                    return !isWhiteIp;
                })
                .filter(host -> {
                    boolean isValidIp =
                        servers.getInvalidIpList() == null || !servers.getInvalidIpList().contains(host);
                    if (!isValidIp) {
                        log.info("Host: {} is invalid ip, skip auth!", host.convertToStrIp());
                    }
                    return isValidIp;
                })
                .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(servers.getDynamicServerGroups()) &&
            CollectionUtils.isNotEmpty(servers.getInvalidDynamicServerGroups())) {
            servers.setDynamicServerGroups(servers.getDynamicServerGroups().stream()
                .filter(group -> !servers.getInvalidDynamicServerGroups().contains(group))
                .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(servers.getTopoNodes()) &&
            CollectionUtils.isNotEmpty(servers.getInvalidTopoNodes())) {
            servers.setTopoNodes(servers.getTopoNodes().stream()
                .filter(topoNode -> !servers.getInvalidTopoNodes().contains(topoNode))
                .collect(Collectors.toList()));
        }
    }

    private AuthResult authFileTransfer(TaskInstanceDTO taskInstance, StepInstanceDTO stepInstance) {
        String username = taskInstance.getOperator();
        Long appId = taskInstance.getAppId();

        Set<Long> accounts = new HashSet<>();
        accounts.add(stepInstance.getAccountId());
        stepInstance.getFileSourceList().stream()
            .filter(fileSource -> !fileSource.isLocalUpload() && fileSource.getAccountId() != null)
            .forEach(fileSource -> {
                accounts.add(fileSource.getAccountId());
            });

        AuthResult accountAuthResult = executeAuthService.batchAuthAccountExecutable(
            username, new AppResourceScope(appId), accounts);

        ServersDTO servers = stepInstance.getTargetServers().clone();
        stepInstance.getFileSourceList().stream()
            .filter(fileSource -> !fileSource.isLocalUpload()
                && fileSource.getFileType() != TaskFileTypeEnum.BASE64_FILE.getType()
                && fileSource.getServers() != null)
            .forEach(fileSource -> {
                servers.merge(fileSource.getServers());
            });
        filterServerDoNotRequireAuth(appId, servers, ActionScopeEnum.FILE_DISTRIBUTION);
        if (servers.isEmpty()) {
            // 如果主机为空，无需对主机进行权限
            return accountAuthResult;
        }

        AuthResult serverAuthResult = executeAuthService.authFastPushFile(
            username, new AppResourceScope(appId), servers);

        return accountAuthResult.mergeAuthResult(serverAuthResult);
    }

    private void setServerInfoFastJob(StepInstanceDTO stepInstance) {
        ServersDTO targetServers = stepInstance.getTargetServers();
        acquireStaticIp(stepInstance.getAppId(), targetServers);
        stepInstance.setIpList(convertToIpListStr(targetServers.getIpList()));
        stepInstance.setTargetServers(targetServers);
        setAgentStatus(targetServers.getIpList());

        if (stepInstance.getExecuteType() == TaskStepTypeEnum.FILE.getValue()) {
            List<FileSourceDTO> fileSources = stepInstance.getFileSourceList();
            for (FileSourceDTO fileSource : fileSources) {
                ServersDTO servers = fileSource.getServers();
                if (servers != null && !fileSource.isLocalUpload()) {
                    // 服务器文件的处理
                    acquireStaticIp(stepInstance.getAppId(), servers);
                    setAgentStatus(servers.getIpList());
                }
            }
        }
    }

    private String convertToIpListStr(Collection<IpDTO> ips) {
        return StringUtils.join(ips.stream().map(ipDTO ->
            ipDTO.getCloudAreaId() + ":" + ipDTO.getIp()).collect(Collectors.toList()), ",");
    }

    /**
     * 检查主机的合法性
     *
     * @param stepInstanceList  步骤列表
     * @param ignoreInvalidHost 是否忽略不合法主机
     * @throws ServiceException 如果包含不合法的主机，抛出异常
     */
    private void checkHosts(List<StepInstanceDTO> stepInstanceList, boolean ignoreInvalidHost)
        throws ServiceException {
        long appId = stepInstanceList.get(0).getAppId();

        Set<IpDTO> checkHosts = new HashSet<>();
        addNeedCheckHosts(stepInstanceList, checkHosts);
        if (checkHosts.isEmpty()) {
            return;
        }

        // 检查是否在当前业务下
        Collection<IpDTO> unavailableHosts = checkHostsNotInApp(appId, checkHosts);
        if (unavailableHosts.isEmpty()) {
            return;
        }

        // 检查是否在白名单配置
        List<IpDTO> invalidHosts = checkHostsNotAllowedInWhiteIpConfig(appId, stepInstanceList, unavailableHosts);
        if (!invalidHosts.isEmpty()) {
            log.warn("Contains invalid host, invalidHost: {}", JsonUtils.toJson(invalidHosts));
            // 如果不允许忽略非法主机，或者全部主机都非法，那么直接拒绝
            if (!ignoreInvalidHost || (invalidHosts.size() == checkHosts.size())) {
                throwHostInvalidException(invalidHosts, appId);
            }
            // 包含非法IP，需要继续走完流程，但是不下发任务
            setInvalidHostsForStepInstance(stepInstanceList, invalidHosts);
        }
    }

    private void addNeedCheckHosts(List<StepInstanceDTO> stepInstanceList, Set<IpDTO> checkHosts) {
        for (StepInstanceDTO stepInstance : stepInstanceList) {
            if (stepInstance.getExecuteType().equals(MANUAL_CONFIRM.getValue())) {
                continue;
            }
            checkHosts.addAll(stepInstance.getTargetServers().getIpList());
            if (stepInstance.getExecuteType().equals(SEND_FILE.getValue())) {
                List<FileSourceDTO> fileSourceList = stepInstance.getFileSourceList();
                if (fileSourceList != null) {
                    for (FileSourceDTO fileSource : fileSourceList) {
                        ServersDTO servers = fileSource.getServers();
                        if (servers != null && servers.getIpList() != null) {
                            checkHosts.addAll(servers.getIpList());
                        }
                    }
                }
            }
        }
    }

    private void setInvalidHostsForStepInstance(List<StepInstanceDTO> stepInstanceList, List<IpDTO> invalidHosts) {
        stepInstanceList.forEach(stepInstance -> {
            if (stepInstance.getExecuteType().equals(MANUAL_CONFIRM.getValue())) {
                return;
            }
            if (stepInstance.getExecuteType().equals(SEND_FILE.getValue())) {
                List<FileSourceDTO> fileSourceList = stepInstance.getFileSourceList();
                if (fileSourceList != null) {
                    for (FileSourceDTO fileSource : fileSourceList) {
                        ServersDTO servers = fileSource.getServers();
                        if (servers != null && servers.getIpList() != null) {
                            servers.setInvalidIpList(servers.getIpList().stream()
                                .filter(invalidHosts::contains).collect(Collectors.toList()));
                        }
                    }
                }
            }
            ServersDTO targetServers = stepInstance.getTargetServers();
            targetServers.setInvalidIpList(targetServers.getIpList().stream()
                .filter(invalidHosts::contains).collect(Collectors.toList()));
        });
    }

    private List<IpDTO> checkHostsNotAllowedInWhiteIpConfig(long appId, List<StepInstanceDTO> stepInstanceList,
                                                            Collection<IpDTO> unavailableHosts) {
        Map<IpDTO, List<String>> hostAllowActionsMap = new HashMap<>();
        for (IpDTO host : unavailableHosts) {
            List<String> allowActions = hostService.getHostAllowedAction(appId, host);
            if (allowActions != null && !allowActions.isEmpty()) {
                hostAllowActionsMap.put(host, allowActions);
            }
        }
        if (hostAllowActionsMap.isEmpty()) {
            log.warn("Hosts are not in the white-ip configuration, hosts:{}", unavailableHosts);
            return new ArrayList<>(unavailableHosts);
        }

        log.debug("Host allow actions:{}", hostAllowActionsMap);
        // 如果配置了白名单，那么需要对主机支持的操作进行校验
        Map<IpDTO, Set<String>> hostBindActionsMap = getHostBindActions(stepInstanceList, unavailableHosts);
        log.debug("Host bind actions:{}", hostBindActionsMap);

        return getHostsContainsNotAllowedAction(hostBindActionsMap, hostAllowActionsMap);
    }

    private Map<IpDTO, Set<String>> getHostBindActions(List<StepInstanceDTO> stepInstanceList,
                                                       Collection<IpDTO> unavailableHosts) {
        Map<IpDTO, Set<String>> hostBindActionsMap = new HashMap<>();
        for (IpDTO host : unavailableHosts) {
            for (StepInstanceDTO stepInstance : stepInstanceList) {
                if (stepInstance.getExecuteType().equals(MANUAL_CONFIRM.getValue())) {
                    continue;
                }
                if (stepInstance.getTargetServers().getIpList().contains(host)) {
                    if (stepInstance.getExecuteType().equals(EXECUTE_SCRIPT.getValue()) ||
                        stepInstance.getExecuteType().equals(EXECUTE_SQL.getValue())) {
                        hostBindActionsMap.compute(host, (hostKey, actions) -> {
                            if (actions == null) {
                                actions = new HashSet<>();
                            }
                            actions.add(ActionScopeEnum.SCRIPT_EXECUTE.name());
                            return actions;
                        });
                    } else if (stepInstance.getExecuteType().equals(SEND_FILE.getValue())) {
                        hostBindActionsMap.compute(host, (hostKey, actions) -> {
                            if (actions == null) {
                                actions = new HashSet<>();
                            }
                            actions.add(ActionScopeEnum.FILE_DISTRIBUTION.name());
                            return actions;
                        });
                    }
                }
            }
        }
        return hostBindActionsMap;
    }


    private void checkHosts(StepInstanceDTO stepInstance, boolean ignoreInvalidHost) throws ServiceException {
        long appId = stepInstance.getAppId();
        ServersDTO targetServers = stepInstance.getTargetServers();
        if (targetServers == null || targetServers.getIpList() == null || targetServers.getIpList().isEmpty()) {
            log.warn("Empty target server");
            throw new FailedPreconditionException(ErrorCode.SERVER_EMPTY);
        }

        List<IpDTO> ipList = targetServers.getIpList();
        Collection<IpDTO> notInAppHosts = checkHostsNotInApp(appId, ipList);
        if (notInAppHosts.isEmpty()) {
            return;
        }

        // 检查是否在白名单配置
        List<IpDTO> invalidHosts = checkHostsNotAllowedInWhiteIpConfig(appId, Lists.newArrayList(stepInstance),
            notInAppHosts);
        if (!invalidHosts.isEmpty()) {
            log.warn("Contains invalid host, invalidHost: {}", JsonUtils.toJson(invalidHosts));
            // 如果不允许忽略非法主机，或者全部主机都非法，那么直接拒绝
            if (!ignoreInvalidHost || (invalidHosts.size() == ipList.size())) {
                throwHostInvalidException(invalidHosts, appId);
            }
            // 包含部分非法主机，需要继续走完流程，下发任务到合法的主机
            targetServers.setInvalidIpList(invalidHosts);
        }
    }

    private boolean shouldIgnoreInvalidHost(TaskInstanceDTO taskInstance) {
        // 定时任务才支持自动忽略非法主机
        return (taskInstance.getStartupMode() != null
            && taskInstance.getStartupMode().equals(TaskStartupModeEnum.CRON.getValue()));
    }

    private void throwHostInvalidException(Collection<IpDTO> unavailableHosts, long appId) {
        String ipListStr = StringUtils.join(unavailableHosts.stream().map(IpDTO::getIp).collect(Collectors.toList()),
            ",");
        log.warn("The following hosts are not registered, appId:{}, ips={}", appId, ipListStr);
        throw new FailedPreconditionException(ErrorCode.SERVER_UNREGISTERED, new Object[]{ipListStr});
    }

    private Collection<IpDTO> checkHostsNotInApp(Long appId, Collection<IpDTO> hosts) {
        List<IpDTO> notInAppHosts = hostService.checkAppHosts(appId, hosts);
        if (CollectionUtils.isNotEmpty(notInAppHosts)) {
            log.info("Check host, appId:{}, not in current app hosts:{}", appId, notInAppHosts);
        }
        return notInAppHosts;
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
    public Long createTaskInstanceForFastTaskRedo(TaskInstanceDTO taskInstance,
                                                  StepInstanceDTO stepInstance) throws ServiceException {
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
        return createTaskInstanceFast(taskInstance, stepInstance);
    }

    @Override
    public void startTask(long taskInstanceId) {
        log.info("Start task, taskInstanceId={}", taskInstanceId);
        controlMsgSender.startTask(taskInstanceId);
    }

    @Override
    public TaskInstanceDTO createTaskInstanceForTask(TaskExecuteParam executeParam) throws ServiceException {
        StopWatch watch = new StopWatch("createTaskInstanceForTask");
        try {

            TaskInfo taskInfo = buildTaskInfoFromExecuteParam(executeParam, watch);

            TaskInstanceDTO taskInstance = taskInfo.getTaskInstance();
            List<StepInstanceDTO> stepInstanceList = taskInfo.getStepInstances();
            Map<String, TaskVariableDTO> finalVariableValueMap = taskInfo.getVariables();
            ServiceTaskPlanDTO jobPlan = taskInfo.getJobPlan();

            // 调整超时时间
            stepInstanceList.forEach(this::adjustStepTimeout);

            // 检查高危脚本
            watch.start("checkDangerousScript");
            batchCheckScriptMatchDangerousRule(taskInstance, stepInstanceList);
            watch.stop();

            // 检查主机合法性
            watch.start("checkHost");
            checkHosts(stepInstanceList, shouldIgnoreInvalidHost(taskInstance));
            watch.stop();

            // 检查步骤约束
            watch.start("checkStepInstanceConstraint");
            checkStepInstanceConstraint(taskInstance, stepInstanceList);
            watch.stop();

            if (!executeParam.isSkipAuth()) {
                watch.start("auth-execute-job");
                authExecuteJobPlan(executeParam.getOperator(), executeParam.getAppId(), jobPlan, stepInstanceList);
                watch.stop();
            }

            watch.start("saveInstance");
            // 这里保存的stepInstanceList已经是完成变量解析之后的步骤信息了
            saveTaskInstance(taskInstance, stepInstanceList, finalVariableValueMap);
            watch.stop();

            watch.start("saveOperationLog");
            taskOperationLogService.saveOperationLog(buildTaskOperationLog(taskInstance, taskInstance.getOperator(),
                UserOperationEnum.START));
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
        // 移除动态分组ID中多余的appId(历史问题)
        // appId:groupId
        String[] appIdAndGroupId = dynamicGroup.getGroupId().split(":");
        if (appIdAndGroupId.length == 2) {
            log.info("Found invalid dynamicGroupId, try to transform to standard format! dynamicGroupId: {}",
                dynamicGroup.getGroupId());
            dynamicGroup.setGroupId(appIdAndGroupId[1]);
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
        Map<String, TaskVariableDTO> finalVariableValueMap = buildFinalTaskVariableValues(appId, planDefaultVariables
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
            StepInstanceDTO stepInstance = createCommonStepInstanceDTO(appId, operator, step.getId(), step.getName(),
                executeType);
            TaskStepTypeEnum stepType = TaskStepTypeEnum.valueOf(step.getType());
            if (stepType == null) {
                throw new InternalException(ErrorCode.INTERNAL_ERROR);
            }
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
                                    List<StepInstanceDTO> stepInstanceList) throws PermissionDeniedException {
        boolean isDebugTask = plan.getDebugTask();
        ServersDTO authServers = new ServersDTO();
        Set<Long> accountIds = new HashSet<>();
        for (StepInstanceDTO stepInstance : stepInstanceList) {
            if (!stepInstance.isScriptStep() && !stepInstance.isFileStep()) {
                continue;
            }
            accountIds.add(stepInstance.getAccountId());
            if (stepInstance.isFileStep()) {
                ServersDTO stepTargetServers = stepInstance.getTargetServers().clone();
                filterServerDoNotRequireAuth(appId, stepTargetServers, ActionScopeEnum.FILE_DISTRIBUTION);
                authServers.merge(stepTargetServers);
                if (!CollectionUtils.isEmpty(stepInstance.getFileSourceList())) {
                    stepInstance.getFileSourceList().stream().filter(fileSource -> !fileSource.isLocalUpload())
                        .forEach(fileSource -> {
                                ServersDTO stepFileSourceServers = fileSource.getServers().clone();
                                filterServerDoNotRequireAuth(appId, stepFileSourceServers,
                                    ActionScopeEnum.FILE_DISTRIBUTION);
                                authServers.merge(stepFileSourceServers);
                                if (fileSource.getAccountId() != null) {
                                    accountIds.add(fileSource.getAccountId());
                                }
                            }
                        );
                }
            } else if (stepInstance.isScriptStep()) {
                ServersDTO stepTargetServers = stepInstance.getTargetServers().clone();
                filterServerDoNotRequireAuth(appId, stepTargetServers, ActionScopeEnum.SCRIPT_EXECUTE);
                authServers.merge(stepTargetServers);
            }
        }


        AuthResult accountAuthResult = executeAuthService.batchAuthAccountExecutable(
            username, new AppResourceScope(appId), accountIds);

        AuthResult authResult;
        if (authServers.isEmpty()) {
            log.info("Required auth servers is empty, authServers: {}", authServers);
            // 主机为空，无需对主机鉴权
            authResult = accountAuthResult;
        } else {
            AuthResult serverAuthResult = null;
            if (isDebugTask) {
                // 鉴权调试

                serverAuthResult = executeAuthService.authDebugTemplate(
                    username, new AppResourceScope(appId), plan.getTaskTemplateId(),
                    authServers);
            } else {
                // 鉴权执行方案

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

    private void authRedoJob(String username, long appId, TaskInstanceDTO taskInstance) {
        Integer taskType = taskInstance.getType();
        if (taskType.equals(TaskTypeEnum.NORMAL.getValue())
            && taskInstance.getTaskId() != null
            && taskInstance.getTaskId() > 0) {
            // 作业鉴权
            ServiceTaskPlanDTO serviceTaskPlanDTO = taskPlanService.getPlanById(appId, taskInstance.getTaskId());
            authExecuteJobPlan(username, appId, serviceTaskPlanDTO, taskInstance.getStepInstances());
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
            authFastExecute(taskInstance, scriptStepInstance);
        } else if (taskType.equals(TaskTypeEnum.FILE.getValue())) {
            // 快速分发文件鉴权
            StepInstanceDTO fileStepInstance = taskInstance.getStepInstances().get(0);
            authFastExecute(taskInstance, fileStepInstance);
        } else {
            log.warn("Auth fail because of invalid task type!");
            throw new PermissionDeniedException(AuthResult.fail());
        }
    }

    private StepExecuteTypeEnum getExecuteTypeFromTaskStepType(ServiceTaskStepDTO step) throws ServiceException {
        StepExecuteTypeEnum executeType = null;
        TaskStepTypeEnum stepType = TaskStepTypeEnum.valueOf(step.getType());
        if (stepType == null) {
            throw new InternalException(ErrorCode.INTERNAL_ERROR);
        }
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
        taskInstance.setStatus(RunStatusEnum.BLANK.getValue());
        taskInstance.setCreateTime(DateUtils.currentTimeMillis());
        taskInstance.setOperator(executeParam.getOperator());
        String taskName = StringUtils.isBlank(executeParam.getTaskName()) ? taskPlan.getName() :
            executeParam.getTaskName();
        taskInstance.setName(taskName);
        taskInstance.setTaskId(taskPlan.getId());
        taskInstance.setTaskTemplateId(taskPlan.getTaskTemplateId());
        taskInstance.setCurrentStepId(-1L);
        taskInstance.setDebugTask(taskPlan.getDebugTask());
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
        stepInstance.setStatus(RunStatusEnum.BLANK.getValue());
        stepInstance.setOperator(operator);
        stepInstance.setAppId(appId);
        stepInstance.setCreateTime(DateUtils.currentTimeMillis());
        stepInstance.setExecuteCount(0);
        return stepInstance;
    }

    public TaskInstanceDTO createTaskInstanceForRedo(Long appId, Long taskInstanceId, String operator,
                                                     List<TaskVariableDTO> executeVariableValues)
        throws ServiceException {
        log.info("Create task instance for redo, appId={}, taskInstanceId={}, operator={}, variables={}", appId,
            taskInstanceId, operator, executeVariableValues);
        TaskInstanceDTO originTaskInstance = taskInstanceService.getTaskInstanceDetail(taskInstanceId);
        if (originTaskInstance == null) {
            log.warn("Create task instance for redo, task instance is not exist.appId={}, planId={}", appId,
                taskInstanceId);
            throw new NotFoundException(ErrorCode.TASK_INSTANCE_NOT_EXIST);
        }

        TaskInstanceDTO taskInstance = createTaskInstanceForRedo(originTaskInstance, operator);

        Map<String, TaskVariableDTO> finalVariableValueMap = buildFinalTaskVariableValues(appId,
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

        // 检查主机合法性
        checkHosts(stepInstanceList, shouldIgnoreInvalidHost(taskInstance));

        // 检查步骤约束
        checkStepInstanceConstraint(taskInstance, stepInstanceList);

        authRedoJob(operator, appId, originTaskInstance);

        saveTaskInstance(taskInstance, stepInstanceList, finalVariableValueMap);

        taskOperationLogService.saveOperationLog(buildTaskOperationLog(taskInstance, taskInstance.getOperator(),
            UserOperationEnum.START));
        return taskInstance;
    }

    private TaskInstanceDTO createTaskInstanceForRedo(TaskInstanceDTO originTaskInstance, String operator) {
        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setAppId(originTaskInstance.getAppId());
        taskInstance.setType(originTaskInstance.getType());
        taskInstance.setStartupMode(TaskStartupModeEnum.NORMAL.getValue());
        taskInstance.setCronTaskId(-1L);
        taskInstance.setStatus(RunStatusEnum.BLANK.getValue());
        taskInstance.setCreateTime(DateUtils.currentTimeMillis());
        taskInstance.setOperator(operator);
        taskInstance.setName(originTaskInstance.getName());
        taskInstance.setTaskId(originTaskInstance.getTaskId());
        taskInstance.setTaskTemplateId(originTaskInstance.getTaskTemplateId());
        taskInstance.setCurrentStepId(-1L);
        taskInstance.setDebugTask(false);
        return taskInstance;
    }

    private TaskInstanceDTO saveTaskInstance(TaskInstanceDTO taskInstance, List<StepInstanceDTO> stepInstances,
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
        return taskInstance;
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
        long appId,
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
                    grantValueForVariable(appId, finalTaskVariable, executeVariableValue);
                } else if (nameKeyExecuteVariableValueMap.containsKey(defaultTaskVariable.getName())) {
                    TaskVariableDTO executeVariableValue =
                        nameKeyExecuteVariableValueMap.get(defaultTaskVariable.getName());
                    grantValueForVariable(appId, finalTaskVariable, executeVariableValue);
                } else {
                    // 否则，使用变量的默认值
                    grantValueForVariable(appId, finalTaskVariable, defaultTaskVariable);
                }
                finalVariableValueMap.put(finalTaskVariable.getName(), finalTaskVariable);
            }
        }
        return finalVariableValueMap;
    }

    private void grantValueForVariable(long appId, TaskVariableDTO to, TaskVariableDTO from) {
        if (TaskVariableTypeEnum.HOST_LIST.getType() == to.getType()) {
            ServersDTO targetServers = from.getTargetServers();
            if (targetServers != null) {
                // 动态-> 静态IP
                acquireStaticIp(appId, targetServers);
                if (targetServers.getIpList() != null && !targetServers.getIpList().isEmpty()) {
                    setAgentStatus(targetServers.getIpList());
                }
            }
            to.setTargetServers(targetServers);
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
        ServersDTO targetServers = buildFinalTargetServers(stepInstance.getAppId(), target, variableValueMap);
        stepInstance.setTargetServers(targetServers);
        stepInstance.setIpList(convertToIpListStr(targetServers.getIpList()));

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
                ServersDTO targetServers = buildFinalTargetServers(stepInstance.getAppId(), target, variableValueMap);
                fileSource.setServers(targetServers);
                List<FileDetailDTO> fileList = new ArrayList<>();
                originFile.getFileLocation().forEach(fileLocation -> {
                    fileList.add(new FileDetailDTO(fileLocation));
                });
                fileSource.setFiles(fileList);
            } else if (originFile.getFileType() == TaskFileTypeEnum.FILE_SOURCE.getType()) {
                fileSource.setLocalUpload(false);
                fileSource.setServers(ServersDTO.emptyInstance());
                // 文件源文件只需要fileSourceId与文件路径
                List<FileDetailDTO> fileList = new ArrayList<>();
                originFile.getFileLocation().forEach(fileLocation -> {
                    fileList.add(new FileDetailDTO(fileLocation));
                });
                fileSource.setFiles(fileList);
                fileSource.setFileSourceId(originFile.getFileSourceId());
            }
            fileSources.add(fileSource);
        }
        stepInstance.setFileSourceList(fileSources);

        ServiceTaskTargetDTO target = fileStepInfo.getExecuteTarget();
        ServersDTO targetServers = buildFinalTargetServers(stepInstance.getAppId(), target, variableValueMap);
        stepInstance.setTargetServers(targetServers);
        stepInstance.setIpList(convertToIpListStr(targetServers.getIpList()));

        if (fileStepInfo.getDownloadSpeedLimit() != null) {
            // MB->KB
            stepInstance.setFileDownloadSpeedLimit(fileStepInfo.getDownloadSpeedLimit() << 10);
        }
        if (fileStepInfo.getUploadSpeedLimit() != null) {
            // MB->KB
            stepInstance.setFileUploadSpeedLimit(fileStepInfo.getUploadSpeedLimit() << 10);
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
        stepInstance.setIpList(convertToIpListStr(targetServers.getIpList()));
    }

    private void parseFileStepInstanceFromStepInstance(StepInstanceDTO stepInstance, StepInstanceDTO originStepInstance,
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
        stepInstance.setIpList(convertToIpListStr(targetServers.getIpList()));
    }

    private ServersDTO buildFinalTargetServers(@NotNull long appId, @NotNull ServiceTaskTargetDTO target,
                                               @NotNull Map<String, TaskVariableDTO> variableValueMap)
        throws ServiceException {
        // 如果目标服务器使用主机变量，那么需要解析主机变量
        if (StringUtils.isNotBlank(target.getVariable())) {
            return getServerValueFromVariable(target.getVariable(), variableValueMap);
        } else {
            ServersDTO targetServers = convertToServersDTO(target);
            acquireStaticIp(appId, targetServers);
            if (targetServers.getIpList() == null || targetServers.getIpList().isEmpty()) {
                log.warn("Target server variable host is empty.variable={}", target.getVariable());
                throw new FailedPreconditionException(ErrorCode.TASK_INSTANCE_RELATED_HOST_VAR_SERVER_EMPTY,
                    new String[]{target.getVariable()});
            }
            setAgentStatus(targetServers.getIpList());
            return targetServers;
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
        ServersDTO variableTargetServers = serverVariable.getTargetServers();
        if (variableTargetServers == null || CollectionUtils.isEmpty(variableTargetServers.getIpList())) {
            log.warn("Target server variable host is empty.variable={}", hostVariableName);
            throw new FailedPreconditionException(ErrorCode.TASK_INSTANCE_RELATED_HOST_VAR_SERVER_EMPTY,
                new String[]{hostVariableName});
        }

        ServersDTO targetServers = variableTargetServers.clone();
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
            List<IpDTO> staticIpList = new ArrayList<>();
            for (ServiceHostInfoDTO hostInfo : hostList) {
                staticIpList.add(new IpDTO(hostInfo.getCloudAreaId(), hostInfo.getIp()));
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

    private void acquireStaticIp(long appId, ServersDTO servers) throws ServiceException {
        Set<IpDTO> ipSet = new HashSet<>();
        List<IpDTO> staticIps = servers.getStaticIpList();
        if (staticIps != null) {
            ipSet.addAll(staticIps);
        }
        List<DynamicServerGroupDTO> dynamicServerGroups = servers.getDynamicServerGroups();
        if (dynamicServerGroups != null) {
            for (DynamicServerGroupDTO group : dynamicServerGroups) {
                List<IpDTO> groupIps = hostService.getIpByDynamicGroupId(appId, group.getGroupId());
                if (CollectionUtils.isEmpty(groupIps)) {
                    servers.addInvalidDynamicServerGroup(group);
                } else {
                    ipSet.addAll(groupIps);
                    group.setIpList(groupIps);
                }
            }
        }
        List<DynamicServerTopoNodeDTO> topoNodes = servers.getTopoNodes();
        if (topoNodes != null && !topoNodes.isEmpty()) {
            if (topoNodes.size() < 10) {
                for (DynamicServerTopoNodeDTO topoNode : topoNodes) {
                    List<IpDTO> topoIps = hostService.getIpByTopoNodes(appId,
                        Collections.singletonList(new CcInstanceDTO(topoNode.getNodeType(), topoNode.getTopoNodeId())));
                    if (CollectionUtils.isEmpty(topoIps)) {
                        servers.addInvalidTopoNodeDTO(topoNode);
                    } else {
                        ipSet.addAll(topoIps);
                    }
                }
            } else {
                getTopoHostsConcurrent(appId, topoNodes, servers, ipSet);
            }
        }
        List<IpDTO> ipList = new ArrayList<>(ipSet.size());
        ipList.addAll(ipSet);
        servers.setIpList(ipList);
    }


    private void getTopoHostsConcurrent(long appId, List<DynamicServerTopoNodeDTO> topoNodes, ServersDTO servers,
                                        Set<IpDTO> ipSet) {
        log.info("Get topo hosts concurrent, topoNodes: {}", topoNodes);
        CountDownLatch latch = new CountDownLatch(topoNodes.size());
        List<Future<Pair<DynamicServerTopoNodeDTO, List<IpDTO>>>> futures = new ArrayList<>(topoNodes.size());
        for (DynamicServerTopoNodeDTO topoNode : topoNodes) {
            futures.add(GET_HOSTS_BY_TOPO_EXECUTOR.submit(new GetTopoHostTask(appId, topoNode, latch)));
        }

        try {
            for (Future<Pair<DynamicServerTopoNodeDTO, List<IpDTO>>> future : futures) {
                Pair<DynamicServerTopoNodeDTO, List<IpDTO>> topoAndHosts = future.get();
                if (CollectionUtils.isEmpty(topoAndHosts.getRight())) {
                    servers.addInvalidTopoNodeDTO(topoAndHosts.getLeft());
                } else {
                    ipSet.addAll(topoAndHosts.getRight());
                }
            }
        } catch (InterruptedException | ExecutionException e) {

        }
        try {
            latch.await();
        } catch (InterruptedException e) {

        }
        log.info("Get topo hosts success, servers: {}", servers);
    }

    private void setAgentStatus(List<IpDTO> ips) {
        if (ips == null || ips.isEmpty()) {
            return;
        }
        List<String> ipList = new ArrayList<>(ips.size());
        for (IpDTO ip : ips) {
            String fullIp = ip.convertToStrIp();
            ipList.add(fullIp);
        }
        Map<String, QueryAgentStatusClient.AgentStatus> statusMap = queryAgentStatusClient.batchGetAgentStatus(ipList);
        for (IpDTO ip : ips) {
            String fullIp = ip.convertToStrIp();
            ip.setAlive(statusMap.get(fullIp) == null ?
                AgentStatusEnum.UNKNOWN.getValue() : statusMap.get(fullIp).status);
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
            default:
                log.warn("Undefined step operation!");
                break;
        }
        return executeCount;
    }

    private void confirmTerminate(StepInstanceDTO stepInstance, String operator, String reason) {
        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(stepInstance.getTaskInstanceId());
        // 只有人工确认等待中的任务，可以进行“终止流程”操作
        if (!RunStatusEnum.WAITING.getValue().equals(stepInstance.getStatus())) {
            log.warn("StepInstance:{} status is not waiting, Unsupported Operation:{}", stepInstance.getId(),
                "confirm-terminate");
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        if (!stepInstance.getExecuteType().equals(MANUAL_CONFIRM.getValue())) {
            log.warn("StepInstance:{} is not confirm step, Unsupported Operation:{}", stepInstance.getId(), "confirm" +
                "-terminate");
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        checkConfirmUser(taskInstance, stepInstance, operator);

        OperationLogDTO operationLog = buildCommonStepOperationLog(stepInstance, operator,
            UserOperationEnum.CONFIRM_TERMINATE);
        operationLog.getDetail().setConfirmReason(reason);
        taskOperationLogService.saveOperationLog(operationLog);

        taskInstanceService.updateConfirmReason(stepInstance.getId(), reason);
        taskInstanceService.updateStepOperator(stepInstance.getId(), operator);

        controlMsgSender.confirmStepTerminate(stepInstance.getId());
    }

    private void confirmRestart(StepInstanceDTO stepInstance, String operator) {
        // 只有“确认终止”状态的任务，可以进行“重新发起确认”操作
        if (!RunStatusEnum.CONFIRM_TERMINATED.getValue().equals(stepInstance.getStatus())) {
            log.warn("StepInstance:{} status is not confirm_terminated, Unsupported Operation:{}",
                stepInstance.getId(), "confirm-restart");
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        if (!stepInstance.getExecuteType().equals(MANUAL_CONFIRM.getValue())) {
            log.warn("StepInstance:{} is not confirm step, Unsupported Operation:{}", stepInstance.getId(), "confirm" +
                "-restart");
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        taskOperationLogService.saveOperationLog(buildCommonStepOperationLog(stepInstance, operator,
            UserOperationEnum.CONFIRM_RESTART));
        controlMsgSender.confirmStepRestart(stepInstance.getId());
    }

    private void checkConfirmUser(TaskInstanceDTO taskInstance, StepInstanceDTO stepInstance,
                                  String operator) throws ServiceException {
        // 人工确认步骤，需要判断操作者
        if (CollectionUtils.isNotEmpty(stepInstance.getConfirmUsers())
            && stepInstance.getConfirmUsers().contains(operator)) {
            return;
        }

        Set<String> confirmUsers = new HashSet<>();
        if (stepInstance.getConfirmRoles() != null && !stepInstance.getConfirmRoles().isEmpty()) {
            if (stepInstance.getConfirmRoles().contains(JobRoleEnum.JOB_RESOURCE_TRIGGER_USER.name())) {
                confirmUsers.add(taskInstance.getOperator());
            } else {
                Set<String> roles = new HashSet<>(stepInstance.getConfirmRoles());
                // JOB_RESOURCE_TRIGGER_USER should remove
                roles.remove(JobRoleEnum.JOB_RESOURCE_TRIGGER_USER.name());
                InternalResponse<Set<String>> resp = userResource.getUsersByRoles(stepInstance.getAppId(), operator,
                    ResourceTypeEnum.JOB.getType(), String.valueOf(taskInstance.getTaskId()), roles);
                if (resp.isSuccess() && resp.getData() != null) {
                    confirmUsers.addAll(resp.getData());
                }
            }
        }
        if (confirmUsers.isEmpty() || !confirmUsers.contains(operator)) {
            throw new FailedPreconditionException(ErrorCode.NOT_IN_CONFIRM_USER_LIST);
        }
    }

    private void confirmContinue(StepInstanceDTO stepInstance, String operator, String reason) {
        // 只有"人工确认等待"，可以进行"确认继续"操作
        if (!stepInstance.getExecuteType().equals(MANUAL_CONFIRM.getValue())) {
            log.warn("StepInstance:{} is not confirm-step, Unsupported Operation:{}", stepInstance.getId(), "confirm" +
                "-continue");
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        if (!(RunStatusEnum.WAITING.getValue().equals(stepInstance.getStatus()))) {
            log.warn("StepInstance:{} status is not waiting, Unsupported Operation:{}", stepInstance.getId(),
                "confirm-continue");
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

        controlMsgSender.confirmStepContinue(stepInstance.getId());
    }

    private void nextStep(StepInstanceDTO stepInstance, String operator) {
        TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(stepInstance.getTaskInstanceId());
        // 只有"终止成功"状态的任务，可以直接进入下一步
        if (!RunStatusEnum.STOP_SUCCESS.getValue().equals(stepInstance.getStatus())) {
            log.warn("StepInstance:{} status is not stop-success, Unsupported Operation:{}", stepInstance.getId(),
                "next-step");
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        taskOperationLogService.saveOperationLog(buildCommonStepOperationLog(stepInstance, operator,
            UserOperationEnum.NEXT_STEP));

        // 需要同步设置任务状态为RUNNING，保证客户端可以在操作完之后立马获取到运行状态，开启同步刷新
        taskInstanceService.updateTaskStatus(taskInstance.getId(), RunStatusEnum.RUNNING.getValue());
        controlMsgSender.nextStep(stepInstance.getId());
    }

    private void retryStepFail(StepInstanceDTO stepInstance, String operator) {
        // 只有“执行失败”的作业可以失败重试
        if (!stepInstance.getStatus().equals(RunStatusEnum.FAIL.getValue())) {
            log.warn("StepInstance:{} status is not fail, Unsupported Operation:{}", stepInstance.getId(), "retry" +
                "-fail");
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        taskInstanceService.updateTaskStatus(stepInstance.getTaskInstanceId(), RunStatusEnum.RUNNING.getValue());
        taskInstanceService.updateStepStatus(stepInstance.getId(), RunStatusEnum.RUNNING.getValue());
//        taskInstanceService.addStepExecuteCount(stepInstance.getId());
        controlMsgSender.retryStepFail(stepInstance.getId());
        OperationLogDTO operationLog = buildCommonStepOperationLog(stepInstance, operator,
            UserOperationEnum.RETRY_STEP_FAIL);
        taskOperationLogService.saveOperationLog(operationLog);
    }

    private void retryStepAll(StepInstanceDTO stepInstance, String operator) {
        // 只有“执行失败”,"终止成功"的作业可以全部重试
        if (!(stepInstance.getStatus().equals(RunStatusEnum.FAIL.getValue())
            || stepInstance.getStatus().equals(RunStatusEnum.STOP_SUCCESS.getValue()))) {
            log.warn("StepInstance:{} status is not fail, Unsupported Operation:{}", stepInstance.getId(), "retry-all");
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        taskInstanceService.updateTaskStatus(stepInstance.getTaskInstanceId(), RunStatusEnum.RUNNING.getValue());
        taskInstanceService.updateStepStatus(stepInstance.getId(), RunStatusEnum.RUNNING.getValue());
//        taskInstanceService.addStepExecuteCount(stepInstance.getId());
        controlMsgSender.retryStepAll(stepInstance.getId());
        OperationLogDTO operationLog = buildCommonStepOperationLog(stepInstance, operator,
            UserOperationEnum.RETRY_STEP_ALL);
        taskOperationLogService.saveOperationLog(operationLog);
    }

    private void ignoreError(StepInstanceDTO stepInstance, String operator) {
        // 只有“执行失败”的作业可以忽略错误进入下一步
        if (!stepInstance.getStatus().equals(RunStatusEnum.FAIL.getValue()) &&
            !stepInstance.getStatus().equals(RunStatusEnum.ABNORMAL_STATE.getValue())) {
            log.warn("StepInstance:{} status is {}, Unsupported Operation:{}", stepInstance.getId(),
                stepInstance.getStatus(), "ignore-error");
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        // 需要同步设置任务状态为RUNNING，保证客户端可以在操作完之后立马获取到运行状态，开启同步刷新
        taskInstanceService.updateTaskStatus(stepInstance.getTaskInstanceId(), RunStatusEnum.RUNNING.getValue());
        controlMsgSender.ignoreStepError(stepInstance.getId());
        OperationLogDTO operationLog = buildCommonStepOperationLog(stepInstance, operator,
            UserOperationEnum.IGNORE_ERROR);
        taskOperationLogService.saveOperationLog(operationLog);
    }

    private void skipStep(StepInstanceDTO stepInstance, String operator) {
        // 只有“强制终止中”的作业可以跳过
        if (!stepInstance.getStatus().equals(RunStatusEnum.STOPPING.getValue())) {
            log.warn("StepInstance:{} status is not stopping, Unsupported Operation:{}", stepInstance.getId(), "skip");
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        controlMsgSender.skipStep(stepInstance.getId());
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
        if (!RunStatusEnum.RUNNING.getValue().equals(taskInstance.getStatus())) {
            log.warn("TaskInstance:{} status is not running, should not terminate it!", taskInstance.getId());
            throw new FailedPreconditionException(ErrorCode.UNSUPPORTED_OPERATION);
        }
        if (RunStatusEnum.STOPPING.getValue().equals(taskInstance.getStatus())) {
            log.warn("TaskInstance:{} status is stopping now, should not terminate it!", taskInstance.getId());
            throw new FailedPreconditionException(ErrorCode.TASK_STOPPING_DO_NOT_REPEAT);
        }
        controlMsgSender.stopTask(taskInstanceId);
        OperationLogDTO operationLog = buildTaskOperationLog(taskInstance, username, UserOperationEnum.TERMINATE_JOB);
        taskOperationLogService.saveOperationLog(operationLog);
    }

    @Override
    public void doTaskOperation(Long appId, String operator, long taskInstanceId,
                                TaskOperationEnum operation) throws ServiceException {
        log.info("Operate task instance, appId:{}, taskInstanceId:{}, operator:{}, operation:{}", appId,
            taskInstanceId, operator, operation.getValue());
        switch (operation) {
            case TERMINATE_JOB:
                terminateJob(operator, appId, taskInstanceId);
                break;
            default:
                log.warn("Undefined task operation!");
                break;
        }
    }

    @Override
    public void authExecuteJobPlan(TaskExecuteParam executeParam) throws ServiceException {
        StopWatch watch = new StopWatch("authJobPlan");
        TaskInfo taskInfo = buildTaskInfoFromExecuteParam(executeParam, watch);

        // 检查主机合法性
        watch.start("checkHost");
        checkHosts(taskInfo.getStepInstances(), shouldIgnoreInvalidHost(taskInfo.getTaskInstance()));
        watch.stop();

        watch.start("auth-execute-job");
        authExecuteJobPlan(executeParam.getOperator(), executeParam.getAppId(), taskInfo.getJobPlan(),
            taskInfo.getStepInstances());
        watch.stop();

        if (watch.getTotalTimeMillis() > 500) {
            log.warn("authJobPlan is slow, watcher: {}", watch.prettyPrint());
        }
    }

    private class GetTopoHostTask implements Callable<Pair<DynamicServerTopoNodeDTO, List<IpDTO>>> {
        private final long appId;
        private final DynamicServerTopoNodeDTO topoNode;
        private final CountDownLatch latch;

        private GetTopoHostTask(long appId, DynamicServerTopoNodeDTO topoNode, CountDownLatch latch) {
            this.appId = appId;
            this.topoNode = topoNode;
            this.latch = latch;
        }

        @Override
        public Pair<DynamicServerTopoNodeDTO, List<IpDTO>> call() {
            try {
                List<IpDTO> topoIps = hostService.getIpByTopoNodes(appId,
                    Collections.singletonList(new CcInstanceDTO(topoNode.getNodeType(), topoNode.getTopoNodeId())));
                return new ImmutablePair<>(topoNode, topoIps);
            } catch (Throwable e) {
                log.warn("Get hosts by topo fail", e);
                return new ImmutablePair<>(topoNode, Collections.EMPTY_LIST);
            } finally {
                latch.countDown();
            }
        }
    }
}
