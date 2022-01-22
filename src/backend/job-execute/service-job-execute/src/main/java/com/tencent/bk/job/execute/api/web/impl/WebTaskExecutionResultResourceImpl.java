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

package com.tencent.bk.job.execute.api.web.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.Order;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.common.util.CustomCollectionUtils;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.execute.api.web.WebTaskExecutionResultResource;
import com.tencent.bk.job.execute.client.ServiceNotificationResourceClient;
import com.tencent.bk.job.execute.common.constants.FileDistModeEnum;
import com.tencent.bk.job.execute.common.constants.FileDistStatusEnum;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTotalTimeTypeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.engine.consts.IpStatus;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.model.AgentTaskExecutionDTO;
import com.tencent.bk.job.execute.model.ExecutionResultGroupDTO;
import com.tencent.bk.job.execute.model.FileIpLogContent;
import com.tencent.bk.job.execute.model.ScriptIpLogContent;
import com.tencent.bk.job.execute.model.StepExecutionDTO;
import com.tencent.bk.job.execute.model.StepExecutionDetailDTO;
import com.tencent.bk.job.execute.model.StepExecutionRecordDTO;
import com.tencent.bk.job.execute.model.StepExecutionResultQuery;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.StepInstanceVariableValuesDTO;
import com.tencent.bk.job.execute.model.TaskExecuteResultDTO;
import com.tencent.bk.job.execute.model.TaskExecutionDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceQuery;
import com.tencent.bk.job.execute.model.VariableValueDTO;
import com.tencent.bk.job.execute.model.converter.TaskInstanceConverter;
import com.tencent.bk.job.execute.model.web.vo.AgentTaskExecutionVO;
import com.tencent.bk.job.execute.model.web.vo.ExecuteVariableVO;
import com.tencent.bk.job.execute.model.web.vo.ExecutionResultGroupVO;
import com.tencent.bk.job.execute.model.web.vo.FileDistributionDetailVO;
import com.tencent.bk.job.execute.model.web.vo.IpFileLogContentVO;
import com.tencent.bk.job.execute.model.web.vo.IpScriptLogContentVO;
import com.tencent.bk.job.execute.model.web.vo.StepExecutionDetailVO;
import com.tencent.bk.job.execute.model.web.vo.StepExecutionRecordVO;
import com.tencent.bk.job.execute.model.web.vo.StepExecutionVO;
import com.tencent.bk.job.execute.model.web.vo.TaskExecuteResultVO;
import com.tencent.bk.job.execute.model.web.vo.TaskExecutionVO;
import com.tencent.bk.job.execute.model.web.vo.TaskInstanceVO;
import com.tencent.bk.job.execute.service.ExecuteAuthService;
import com.tencent.bk.job.execute.service.GseTaskService;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.StepInstanceVariableValueService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceVariableService;
import com.tencent.bk.job.execute.service.TaskResultService;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.model.inner.ServiceAppRoleDTO;
import com.tencent.bk.job.manage.model.inner.ServiceNotifyChannelDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tencent.bk.job.execute.constants.Consts.MAX_SEARCH_TASK_HISTORY_RANGE_MILLS;

@RestController
@Slf4j
public class WebTaskExecutionResultResourceImpl
    implements WebTaskExecutionResultResource {
    private final TaskResultService taskResultService;
    private final MessageI18nService i18nService;
    private final LogService logService;
    private final StepInstanceVariableValueService stepInstanceVariableValueService;
    private final TaskInstanceService taskInstanceService;
    private final TaskInstanceVariableService taskInstanceVariableService;
    private final ServiceNotificationResourceClient notifyResource;
    private final ExecuteAuthService executeAuthService;
    private final WebAuthService webAuthService;
    private final GseTaskService gseTaskService;

    private LoadingCache<String, Map<String, String>> roleCache = CacheBuilder.newBuilder()
        .maximumSize(10).expireAfterWrite(10, TimeUnit.MINUTES).
            build(new CacheLoader<String, Map<String, String>>() {
                      @Override
                      public Map<String, String> load(String lang) {
                          InternalResponse<List<ServiceAppRoleDTO>> resp = notifyResource.getNotifyRoles(lang);
                          log.info("Get notify roles, resp={}", resp);
                          if (!resp.isSuccess() || resp.getData() == null) {
                              return new HashMap<>();
                          } else {
                              List<ServiceAppRoleDTO> appRoles = resp.getData();
                              Map<String, String> codeNameMap = new HashMap<>();
                              if (appRoles != null) {
                                  appRoles.forEach(role -> codeNameMap.put(role.getCode(), role.getName()));
                              }
                              return codeNameMap;
                          }
                      }
                  }
            );
    private LoadingCache<String, Map<String, String>> channelCache = CacheBuilder.newBuilder()
        .maximumSize(10).expireAfterWrite(10, TimeUnit.MINUTES).
            build(new CacheLoader<String, Map<String, String>>() {
                      @Override
                      public Map<String, String> load(String lang) {
                          InternalResponse<List<ServiceNotifyChannelDTO>> resp = notifyResource.getNotifyChannels(lang);
                          log.info("Get notify channels, resp={}", resp);
                          if (!resp.isSuccess() || resp.getData() == null) {
                              return new HashMap<>();
                          } else {
                              List<ServiceNotifyChannelDTO> channels = resp.getData();
                              Map<String, String> typeNameMap = new HashMap<>();
                              if (channels != null) {
                                  channels.forEach(channel -> typeNameMap.put(channel.getType(), channel.getName()));
                              }
                              return typeNameMap;
                          }
                      }
                  }
            );

    @Autowired
    public WebTaskExecutionResultResourceImpl(TaskResultService taskResultService,
                                              MessageI18nService i18nService,
                                              LogService logService,
                                              StepInstanceVariableValueService stepInstanceVariableValueService,
                                              TaskInstanceService taskInstanceService,
                                              TaskInstanceVariableService taskInstanceVariableService,
                                              ServiceNotificationResourceClient notifyResource,
                                              ExecuteAuthService executeAuthService,
                                              WebAuthService webAuthService,
                                              GseTaskService gseTaskService) {
        this.taskResultService = taskResultService;
        this.i18nService = i18nService;
        this.logService = logService;
        this.stepInstanceVariableValueService = stepInstanceVariableValueService;
        this.taskInstanceService = taskInstanceService;
        this.taskInstanceVariableService = taskInstanceVariableService;
        this.notifyResource = notifyResource;
        this.executeAuthService = executeAuthService;
        this.webAuthService = webAuthService;
        this.gseTaskService = gseTaskService;
    }

    @Override
    public Response<PageData<TaskInstanceVO>> getTaskHistoryList(String username,
                                                                 Long appId,
                                                                 String taskName,
                                                                 Long taskInstanceId,
                                                                 Integer status,
                                                                 String operator,
                                                                 Integer taskType,
                                                                 String startTime,
                                                                 String endTime,
                                                                 Integer timeRange,
                                                                 TaskTotalTimeTypeEnum totalTimeType,
                                                                 Integer start,
                                                                 Integer pageSize,
                                                                 Long cronTaskId,
                                                                 String startupModes,
                                                                 String ip) {
        TaskInstanceQuery taskQuery = new TaskInstanceQuery();
        taskQuery.setTaskInstanceId(taskInstanceId);
        taskQuery.setAppId(appId);
        taskQuery.setTaskName(taskName);
        taskQuery.setCronTaskId(cronTaskId);

        ValidateResult validateResult = validateAndSetQueryTimeRange(taskQuery, startTime, endTime, timeRange);
        if (!validateResult.isPass()) {
            return Response.buildValidateFailResp(validateResult);
        }

        setTotalTimeCondition(taskQuery, totalTimeType);
        taskQuery.setOperator(operator);
        setStartupModeCondition(taskQuery, startupModes);
        if (taskType != null) {
            taskQuery.setTaskType(TaskTypeEnum.valueOf(taskType));
        }
        if (status != null) {
            taskQuery.setStatus(RunStatusEnum.valueOf(status));
        }
        taskQuery.setIp(ip);
        BaseSearchCondition baseSearchCondition = new BaseSearchCondition();
        baseSearchCondition.setStart(start);
        baseSearchCondition.setLength(pageSize);


        PageData<TaskInstanceDTO> pageData = taskResultService.listPageTaskInstance(taskQuery, baseSearchCondition);
        if (pageData == null) {
            return Response.buildSuccessResp(PageData.emptyPageData(start, pageSize));
        }

        PageData<TaskInstanceVO> pageDataVO = new PageData<>();
        pageDataVO.setTotal(pageData.getTotal());
        pageDataVO.setStart(pageData.getStart());
        pageDataVO.setPageSize(pageData.getPageSize());

        List<TaskInstanceVO> taskInstanceVOS = new ArrayList<>();
        if (pageData.getData() != null) {
            pageData.getData().forEach(taskInstanceDTO -> taskInstanceVOS.add(TaskInstanceConverter
                .convertToTaskInstanceVO(taskInstanceDTO, i18nService)));
        }
        pageDataVO.setData(taskInstanceVOS);
        batchSetPermissionsForTaskInstance(username, appId, taskInstanceVOS);
        return Response.buildSuccessResp(pageDataVO);
    }

    private ValidateResult validateAndSetQueryTimeRange(TaskInstanceQuery taskInstanceQuery, String startTime,
                                                        String endTime, Integer timeRange) {
        Long start = null;
        Long end = null;
        if (timeRange != null) {
            if (timeRange < 1) {
                log.warn("Param timeRange should greater than 0");
                return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM);
            }
            if (timeRange > 30) {
                log.warn("Param timeRange should less then 30");
                return ValidateResult.fail(ErrorCode.TASK_INSTANCE_QUERY_TIME_SPAN_MORE_THAN_30_DAYS);
            }
            // 当天结束时间
            long todayMaxMills = LocalDateTime.of(LocalDate.now(), LocalTime.MAX).getSecond() * 1000L;
            start = todayMaxMills - 30 * 24 * 3600 * 1000L;
        } else {
            if (StringUtils.isNotBlank(startTime)) {
                start = DateUtils.convertUnixTimestampFromDateTimeStr(startTime, "yyyy-MM-dd HH:mm:ss",
                    ChronoUnit.MILLIS, ZoneId.systemDefault());
            }
            if (StringUtils.isNotBlank(endTime)) {
                end = DateUtils.convertUnixTimestampFromDateTimeStr(endTime, "yyyy-MM-dd HH:mm:ss",
                    ChronoUnit.MILLIS, ZoneId.systemDefault());
            }

            if (start == null) {
                log.info("StartTime should not be empty!");
                return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM);
            }
            if (end == null) {
                end = System.currentTimeMillis();
            }
            if (end - start > MAX_SEARCH_TASK_HISTORY_RANGE_MILLS) {
                log.info("Query task instance history time span must be less than 30 days");
                return ValidateResult.fail(ErrorCode.TASK_INSTANCE_QUERY_TIME_SPAN_MORE_THAN_30_DAYS);
            }
        }

        taskInstanceQuery.setStartTime(start);
        taskInstanceQuery.setEndTime(end);
        return ValidateResult.pass();
    }

    private void setTotalTimeCondition(TaskInstanceQuery taskQuery, TaskTotalTimeTypeEnum totalTimeType) {
        if (TaskTotalTimeTypeEnum.LESS_THAN_ONE_MINUTE == totalTimeType) {
            taskQuery.setMinTotalTimeMills(null);
            taskQuery.setMaxTotalTimeMills(60000L);
        } else if (TaskTotalTimeTypeEnum.ONE_MINUTE_TO_TEN_MINUTES == totalTimeType) {
            taskQuery.setMinTotalTimeMills(60000L);
            taskQuery.setMaxTotalTimeMills(600000L);
        } else if (TaskTotalTimeTypeEnum.MORE_THAN_TEN_MINUTES == totalTimeType) {
            taskQuery.setMinTotalTimeMills(600000L);
            taskQuery.setMaxTotalTimeMills(null);
        }
    }

    private void setStartupModeCondition(TaskInstanceQuery taskQuery, String startupModes) {
        if (StringUtils.isNotEmpty(startupModes)) {
            String[] startupModeArray = startupModes.split(",");
            if (startupModeArray.length != 0) {
                List<TaskStartupModeEnum> startupModeList = new ArrayList<>();
                for (String startupModeValue : startupModeArray) {
                    startupModeList.add(TaskStartupModeEnum.getStartupMode(Integer.parseInt(startupModeValue)));
                }
                taskQuery.setStartupModes(startupModeList);
            }
        }
    }

    private void batchSetPermissionsForTaskInstance(String username, long appId, List<TaskInstanceVO> taskInstances) {
        if (CustomCollectionUtils.isEmptyCollection(taskInstances)) {
            return;
        }
        boolean hasViewAllPermission = executeAuthService.authViewAllTaskInstance(username, appId).isPass();
        if (hasViewAllPermission) {
            taskInstances.forEach(taskInstance -> {
                taskInstance.setCanView(true);
                taskInstance.setCanExecute(true);
            });
        } else {
            taskInstances.forEach(taskInstance -> {
                taskInstance.setCanView(taskInstance.getOperator().equals(username));
                taskInstance.setCanExecute(taskInstance.getOperator().equals(username));
            });
        }
    }


    @Override
    public Response<TaskExecuteResultVO> getTaskExecutionResult(String username, Long appId,
                                                                Long taskInstanceId) {
        TaskExecuteResultDTO taskExecuteResult = taskResultService.getTaskExecutionResult(username, appId,
            taskInstanceId);
        TaskExecuteResultVO taskExecuteResultVO = convertToTaskExecuteResultVO(taskExecuteResult);
        return Response.buildSuccessResp(taskExecuteResultVO);
    }

    private TaskExecuteResultVO convertToTaskExecuteResultVO(TaskExecuteResultDTO taskExecuteResultDTO) {
        TaskExecuteResultVO taskExecuteResultVO = new TaskExecuteResultVO();
        taskExecuteResultVO.setFinished(taskExecuteResultDTO.isFinished());

        TaskExecutionDTO taskExecutionDTO = taskExecuteResultDTO.getTaskInstanceExecutionResult();
        TaskExecutionVO taskExecutionVO = new TaskExecutionVO();
        taskExecutionVO.setName(taskExecutionDTO.getName());
        taskExecutionVO.setType(taskExecutionDTO.getType());
        taskExecutionVO.setStatus(taskExecutionDTO.getStatus());
        taskExecutionVO.setStatusDesc(i18nService.getI18n(Objects.requireNonNull(
            RunStatusEnum.valueOf(taskExecutionDTO.getStatus())).getI18nKey()));
        taskExecutionVO.setTaskInstanceId(taskExecutionDTO.getTaskInstanceId());
        taskExecutionVO.setTaskId(taskExecutionDTO.getTaskId());
        taskExecutionVO.setTemplateId(taskExecutionDTO.getTaskTemplateId());
        taskExecutionVO.setDebugTask(taskExecutionDTO.isDebugTask());
        taskExecutionVO.setStartTime(taskExecutionDTO.getStartTime());
        taskExecutionVO.setEndTime(taskExecutionDTO.getEndTime());
        taskExecutionVO.setTotalTime(taskExecutionDTO.getTotalTime());
        taskExecuteResultVO.setTaskExecution(taskExecutionVO);

        List<StepExecutionVO> stepExecutionVOS = new ArrayList<>();
        for (StepExecutionDTO stepExecutionDTO : taskExecuteResultDTO.getStepInstanceExecutionResults()) {
            StepExecutionVO stepExecutionVO = new StepExecutionVO();
            stepExecutionVO.setName(stepExecutionDTO.getName());
            stepExecutionVO.setRetryCount(stepExecutionDTO.getExecuteCount());
            stepExecutionVO.setStepInstanceId(stepExecutionDTO.getStepInstanceId());
            stepExecutionVO.setStartTime(stepExecutionDTO.getStartTime());
            stepExecutionVO.setEndTime(stepExecutionDTO.getEndTime());
            stepExecutionVO.setStatus(stepExecutionDTO.getStatus());
            RunStatusEnum runStatus = RunStatusEnum.valueOf(stepExecutionDTO.getStatus());
            if (runStatus != null) {
                stepExecutionVO.setStatusDesc(i18nService.getI18n(runStatus.getI18nKey()));
            }
            stepExecutionVO.setTotalTime(stepExecutionDTO.getTotalTime());
            stepExecutionVO.setType(stepExecutionDTO.getType());
            stepExecutionVO.setCurrentStepRunning(taskExecuteResultDTO.getTaskInstanceExecutionResult()
                .getCurrentStepInstanceId() == stepExecutionDTO.getStepInstanceId());
            stepExecutionVO.setIsLastStep(stepExecutionDTO.isLastStep());

            fillConfirmStepDetail(stepExecutionVO, stepExecutionDTO);
            stepExecutionVOS.add(stepExecutionVO);
        }
        taskExecuteResultVO.setStepExecution(stepExecutionVOS);

        return taskExecuteResultVO;
    }

    private void fillConfirmStepDetail(StepExecutionVO stepExecutionVO, StepExecutionDTO stepExecutionDTO) {
        // 处理人工确认步骤，需要包含步骤详细信息
        if (TaskStepTypeEnum.APPROVAL.getValue() == stepExecutionDTO.getType()) {
            stepExecutionVO.setConfirmMessage(stepExecutionDTO.getConfirmMessage());
            stepExecutionVO.setConfirmReason(stepExecutionDTO.getConfirmReason());
            stepExecutionVO.setOperator(stepExecutionDTO.getOperator());
            stepExecutionVO.setUserList(stepExecutionDTO.getConfirmUsers());
            if (stepExecutionDTO.getConfirmRoles() != null && !stepExecutionDTO.getConfirmRoles().isEmpty()) {
                List<String> roleNames = new ArrayList<>();
                Map<String, String> roleCodeAndName = null;
                try {
                    roleCodeAndName = roleCache.get(JobContextUtil.getUserLang());
                } catch (Exception e) {
                    log.warn("Get role from cache fail", e);
                    roleCodeAndName = new HashMap<>();
                }
                for (String roleCode : stepExecutionDTO.getConfirmRoles()) {
                    String roleName = roleCodeAndName.get(roleCode);
                    if (StringUtils.isNotEmpty(roleName)) {
                        roleNames.add(roleName);
                    } else {
                        log.warn("Invalid role, roleCodeAndName:{}, roleCode:{}", roleCodeAndName, roleCode);
                    }
                }
                stepExecutionVO.setRoleNameList(roleNames);
            }
            if (stepExecutionDTO.getConfirmNotifyChannels() != null
                && !stepExecutionDTO.getConfirmNotifyChannels().isEmpty()) {
                Map<String, String> channelTypeAndName;
                try {
                    channelTypeAndName = channelCache.get(JobContextUtil.getUserLang());
                } catch (Exception e) {
                    log.warn("Get channel from cache fail", e);
                    channelTypeAndName = new HashMap<>();
                }
                List<String> channelNameList = new ArrayList<>();
                for (String channelType : stepExecutionDTO.getConfirmNotifyChannels()) {
                    String channelName = channelTypeAndName.get(channelType);
                    if (StringUtils.isEmpty(channelName)) {
                        log.warn("Invalid channel, channelTypeAndName:{}, channelType:{}", channelTypeAndName,
                            channelType);
                    } else {
                        channelNameList.add(channelName);
                    }
                }
                stepExecutionVO.setNotifyChannelNameList(channelNameList);
            }
        }
    }

    @Override
    public Response<StepExecutionDetailVO> getStepExecutionResult(String username,
                                                                  Long appId,
                                                                  Long stepInstanceId,
                                                                  Integer executeCount,
                                                                  Integer batch,
                                                                  Integer resultType,
                                                                  String tag,
                                                                  Integer maxIpsPerResultGroup,
                                                                  String keyword,
                                                                  String searchIp,
                                                                  String orderField,
                                                                  Integer order) {
        StepExecutionResultQuery query = new StepExecutionResultQuery();
        query.setStepInstanceId(stepInstanceId);
        query.setExecuteCount(executeCount);
        query.setBatch(batch);
        query.setResultType(resultType);
        query.setTag(tag);
        query.setLogKeyword(keyword);
        query.setSearchIp(searchIp);
        query.setMaxAgentTasksForResultGroup(maxIpsPerResultGroup);
        query.setOrderField(orderField);
        query.setOrder(Order.valueOf(order));

        StepExecutionDetailDTO executionResult = taskResultService.getStepExecutionResult(username,
            appId, query);
        return Response.buildSuccessResp(convertToStepInstanceExecutionDetailVO(executionResult));
    }

    @Override
    public Response<StepExecutionDetailVO> getFastTaskStepExecutionResult(String username,
                                                                          Long appId,
                                                                          Long taskInstanceId,
                                                                          Integer batch,
                                                                          Integer resultType,
                                                                          String tag,
                                                                          Integer maxIpsPerResultGroup,
                                                                          String orderField,
                                                                          Integer order) {
        StepExecutionResultQuery query = new StepExecutionResultQuery();
        query.setResultType(resultType);
        query.setTag(tag);
        query.setMaxAgentTasksForResultGroup(maxIpsPerResultGroup);
        query.setOrderField(orderField);
        query.setOrder(Order.valueOf(order));
        StepExecutionDetailDTO executionResult = taskResultService.getFastTaskStepExecutionResult(username,
            appId, taskInstanceId, query);
        return Response.buildSuccessResp(convertToStepInstanceExecutionDetailVO(executionResult));
    }

    private StepExecutionDetailVO convertToStepInstanceExecutionDetailVO(StepExecutionDetailDTO executionDetail) {
        StepExecutionDetailVO stepExecutionDetailVO = new StepExecutionDetailVO();
        stepExecutionDetailVO.setFinished(executionDetail.isFinished());
        stepExecutionDetailVO.setName(executionDetail.getName());
        stepExecutionDetailVO.setStepInstanceId(executionDetail.getStepInstanceId());
        stepExecutionDetailVO.setRetryCount(executionDetail.getExecuteCount());
        stepExecutionDetailVO.setStatus(executionDetail.getStatus());
        RunStatusEnum runStatusEnum = RunStatusEnum.valueOf(executionDetail.getStatus());
        if (runStatusEnum != null) {
            stepExecutionDetailVO.setStatusDesc(i18nService.getI18n(runStatusEnum.getI18nKey()));
        }
        stepExecutionDetailVO.setStartTime(executionDetail.getStartTime());
        stepExecutionDetailVO.setEndTime(executionDetail.getEndTime());
        stepExecutionDetailVO.setTotalTime(executionDetail.getTotalTime());
        stepExecutionDetailVO.setGseTaskId(executionDetail.getGseTaskId());
        stepExecutionDetailVO.setIsLastStep(executionDetail.isLastStep());
        stepExecutionDetailVO.setType(executionDetail.getStepType());

        List<ExecutionResultGroupVO> resultGroupVOS = new ArrayList<>();
        for (ExecutionResultGroupDTO resultGroup : executionDetail.getResultGroups()) {
            ExecutionResultGroupVO executionResultGroupVO = new ExecutionResultGroupVO();
            executionResultGroupVO.setResultType(resultGroup.getResultType());
            executionResultGroupVO.setResultTypeDesc(
                i18nService.getI18n(IpStatus.valueOf(resultGroup.getResultType()).getI18nKey()));
            executionResultGroupVO.setTag(resultGroup.getTag());
            executionResultGroupVO.setAgentTaskSize(resultGroup.getAgentTaskSize());

            List<AgentTaskExecutionVO> agentTaskExecutionVOS = new ArrayList<>();
            if (resultGroup.getAgentTaskExecutionDetail() != null) {
                for (AgentTaskExecutionDTO agentTaskExecution : resultGroup.getAgentTaskExecutionDetail()) {
                    AgentTaskExecutionVO agentTaskVO = new AgentTaskExecutionVO();
                    agentTaskVO.setIp(agentTaskExecution.getCloudIp());
                    agentTaskVO.setDisplayIp(agentTaskExecution.getDisplayIp());
                    agentTaskVO.setEndTime(agentTaskExecution.getEndTime());
                    agentTaskVO.setStartTime(agentTaskExecution.getStartTime());
                    agentTaskVO.setStatus(agentTaskExecution.getStatus());
                    agentTaskVO.setStatusDesc(
                        i18nService.getI18n(IpStatus.valueOf(agentTaskExecution.getStatus()).getI18nKey()));
                    agentTaskVO.setErrorCode(agentTaskExecution.getErrorCode());
                    agentTaskVO.setExitCode(agentTaskExecution.getExitCode());
                    agentTaskVO.setTag(agentTaskExecution.getTag());
                    agentTaskVO.setTotalTime(agentTaskExecution.getTotalTime());
                    agentTaskVO.setCloudAreaId(agentTaskExecution.getCloudAreaId());
                    agentTaskVO.setCloudAreaName(agentTaskExecution.getCloudAreaName());
                    agentTaskVO.setRetryCount(agentTaskExecution.getExecuteCount());
                    agentTaskExecutionVOS.add(agentTaskVO);
                }
            }
            executionResultGroupVO.setAgentTaskExecutionDetail(agentTaskExecutionVOS);

            resultGroupVOS.add(executionResultGroupVO);
        }
        stepExecutionDetailVO.setResultGroups(resultGroupVOS);

        return stepExecutionDetailVO;
    }

    @Override
    public Response<IpScriptLogContentVO> getScriptLogContentByIp(String username,
                                                                  Long appId,
                                                                  Long stepInstanceId,
                                                                  Integer executeCount,
                                                                  String ip) {
        if (stepInstanceId == null || executeCount == null || ip == null) {
            log.warn("Get ip log content, param is illegal!");
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        if (!IpUtils.checkCloudAreaIdAndIpStr(ip)) {
            log.warn("Get ip log content, param ip is illegal! ip={}", ip);
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }

        StepInstanceBaseDTO stepInstance = taskInstanceService.getBaseStepInstance(stepInstanceId);
        if (stepInstance == null) {
            return Response.buildCommonFailResp(ErrorCode.STEP_INSTANCE_NOT_EXIST);
        }
        AuthResult authResult = authViewStepInstance(username, appId, stepInstance);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        ScriptIpLogContent scriptIpLogContent = logService.getScriptIpLogContent(stepInstanceId, executeCount,
            IpDTO.fromCloudAreaIdAndIpStr(ip));
        IpScriptLogContentVO ipScriptLogContentVO = new IpScriptLogContentVO();
        ipScriptLogContentVO.setDisplayIp(ip);
        if (scriptIpLogContent != null) {
            ipScriptLogContentVO.setLogContent(scriptIpLogContent.getContent());
            ipScriptLogContentVO.setFinished(scriptIpLogContent.isFinished());
        }
        return Response.buildSuccessResp(ipScriptLogContentVO);
    }

    private AuthResult authViewStepInstance(String username, Long appId, StepInstanceBaseDTO stepInstance) {
        String operator = stepInstance.getOperator();
        if (username.equals(operator)) {
            return AuthResult.pass();
        }
        AuthResult authResult = executeAuthService.authViewTaskInstance(username, appId,
            stepInstance.getTaskInstanceId());
        if (!authResult.isPass()) {
            authResult.setApplyUrl(webAuthService.getApplyUrl(authResult.getRequiredActionResources()));
        }
        return authResult;
    }


    @Override
    public Response<List<ExecuteVariableVO>> getStepVariableByIp(String username, Long appId,
                                                                 Long stepInstanceId, String ip) {
        StepInstanceDTO stepInstance = taskInstanceService.getStepInstanceDetail(stepInstanceId);
        if (stepInstance == null) {
            return Response.buildSuccessResp(Collections.emptyList());
        }
        if (!stepInstance.getExecuteType().equals(StepExecuteTypeEnum.EXECUTE_SCRIPT.getValue())
            || !stepInstance.getScriptType().equals(ScriptTypeEnum.SHELL.getValue())) {
            return Response.buildSuccessResp(Collections.emptyList());
        }

        AuthResult authResult = authViewStepInstance(username, appId, stepInstance);
        if (!authResult.isPass()) {
            throw new PermissionDeniedException(authResult);
        }

        List<TaskVariableDTO> taskVars =
            taskInstanceVariableService.getByTaskInstanceId(stepInstance.getTaskInstanceId());
        if (taskVars == null || taskVars.isEmpty()) {
            return Response.buildSuccessResp(Collections.emptyList());
        }
        List<ExecuteVariableVO> taskVariableVOS = new ArrayList<>();
        List<String> changeableVarNames = new ArrayList<>();
        List<String> namespaceVarNames = new ArrayList<>();
        Map<String, TaskVariableDTO> taskVariablesMap = new HashMap<>();
        for (TaskVariableDTO taskVar : taskVars) {
            taskVariablesMap.put(taskVar.getName(), taskVar);
            if (taskVar.getType() == TaskVariableTypeEnum.HOST_LIST.getType()) {
                continue;
            }
            if (!taskVar.isChangeable()) {
                taskVariableVOS.add(convertToTaskVariableVO(taskVar));
            } else {
                changeableVarNames.add(taskVar.getName());
                if (taskVar.getType() == TaskVariableTypeEnum.NAMESPACE.getType()) {
                    namespaceVarNames.add(taskVar.getName());
                }
            }
        }

        if (!changeableVarNames.isEmpty()) {
            StepInstanceVariableValuesDTO inputStepInstanceValues = stepInstanceVariableValueService
                .computeInputStepInstanceVariableValues(stepInstance.getTaskInstanceId(), stepInstanceId, taskVars);
            if (inputStepInstanceValues == null) {
                taskVars.stream().filter(var -> !var.getType().equals(TaskVariableTypeEnum.HOST_LIST.getType()) &&
                    var.isChangeable()).forEach(var -> taskVariableVOS.add(convertToTaskVariableVO(var)));
                return Response.buildSuccessResp(taskVariableVOS);
            }

            namespaceVarNames.forEach(paramName -> {
                ExecuteVariableVO vo = new ExecuteVariableVO();
                vo.setName(paramName);
                String paramValue = (inputStepInstanceValues.getNamespaceParamsMap() != null
                    && inputStepInstanceValues.getNamespaceParamsMap().get(ip) != null
                    && inputStepInstanceValues.getNamespaceParamsMap().get(ip).get(paramName) != null)
                    ? inputStepInstanceValues.getNamespaceParamsMap().get(ip).get(paramName).getValue()
                    : taskVariablesMap.get(paramName).getValue();
                vo.setValue(paramValue);
                vo.setChangeable(1);
                vo.setType(TaskVariableTypeEnum.NAMESPACE.getType());
                taskVariableVOS.add(vo);
            });

            List<VariableValueDTO> globalVars = inputStepInstanceValues.getGlobalParams();
            if (globalVars != null) {
                for (VariableValueDTO varValue : globalVars) {
                    if (varValue.getType().equals(TaskVariableTypeEnum.HOST_LIST.getType())) {
                        // 过滤掉主机变量
                        continue;
                    }
                    TaskVariableDTO taskVariable = taskVariablesMap.get(varValue.getName());
                    if (taskVariable == null || !taskVariable.isChangeable()) {
                        // 过滤掉常量
                        continue;
                    }

                    ExecuteVariableVO vo = new ExecuteVariableVO();
                    vo.setName(varValue.getName());
                    vo.setValue(varValue.getValue());
                    vo.setChangeable(1);
                    vo.setType(taskVariable.getType());
                    taskVariableVOS.add(vo);
                }
            }
        }
        return Response.buildSuccessResp(taskVariableVOS);
    }

    private ExecuteVariableVO convertToTaskVariableVO(TaskVariableDTO taskVariable) {
        ExecuteVariableVO vo = new ExecuteVariableVO();
        vo.setName(taskVariable.getName());
        if (taskVariable.getType().equals(TaskVariableTypeEnum.CIPHER.getType())) {
            vo.setValue("******");
        } else {
            vo.setValue(taskVariable.getValue());
        }
        vo.setChangeable(taskVariable.isChangeable() ? 1 : 0);
        TaskVariableTypeEnum varType = TaskVariableTypeEnum.valOf(taskVariable.getType());
        if (varType != null) {
            vo.setType(varType.getType());
        }
        return vo;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Response<IpFileLogContentVO> getFileLogContentByIp(String username, Long appId, Long stepInstanceId,
                                                              Integer executeCount,
                                                              String ip, String mode) {

        if (stepInstanceId == null || executeCount == null || ip == null) {
            log.warn("Get ip log content, param is illegal!");
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
        if (!IpUtils.checkCloudAreaIdAndIpStr(ip)) {
            log.warn("Get ip log content, param ip is illegal! ip={}", ip);
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }

        IpFileLogContentVO result = new IpFileLogContentVO();
        List<FileDistributionDetailVO> fileDistDetailVOS = new ArrayList<>();
        result.setFileDistributionDetails(fileDistDetailVOS);

        if ("download".equals(mode)) {
            FileIpLogContent downloadLog = logService.getFileIpLogContent(stepInstanceId, executeCount,
                IpDTO.fromCloudAreaIdAndIpStr(ip), FileDistModeEnum.DOWNLOAD.getValue());
            // downloadLog为null说明步骤还未下发至GSE就被终止
            if (downloadLog != null && CollectionUtils.isNotEmpty(downloadLog.getFileTaskLogs())) {
                downloadLog.getFileTaskLogs().forEach(fileLog -> {
                    if (fileLog.getMode().equals(FileDistModeEnum.UPLOAD.getValue())) {
                        return;
                    }
                    fileDistDetailVOS.add(convertToFileDistributionDetailVO(fileLog));
                });
                result.setFinished(downloadLog.isFinished());
            }
            Collections.sort(fileDistDetailVOS);
        } else {
            List<ServiceFileTaskLogDTO> fileTaskLogs = logService.batchGetFileSourceIpLogContent(stepInstanceId,
                executeCount);
            if (CollectionUtils.isNotEmpty(fileTaskLogs)) {
                fileTaskLogs.forEach(fileTaskLog -> {
                    if (fileTaskLog.getMode().equals(FileDistModeEnum.DOWNLOAD.getValue())) {
                        return;
                    }
                    fileDistDetailVOS.add(convertToFileDistributionDetailVO(fileTaskLog));
                });
                Collections.sort(fileDistDetailVOS);
                result.setFinished(fileTaskLogs.stream().noneMatch(fileLog ->
                    (fileLog.getStatus().equals(FileDistStatusEnum.DOWNLOADING.getValue())
                        || fileLog.getStatus().equals(FileDistStatusEnum.UPLOADING.getValue())
                        || fileLog.getStatus().equals(FileDistStatusEnum.WAITING.getValue()))
                        || fileLog.getStatus().equals(FileDistStatusEnum.PULLING.getValue())));
            }
        }
        boolean includingLogContent = !removeFileLogContentIfResultIsLarge(fileDistDetailVOS);
        result.setIncludingLogContent(includingLogContent);
        return Response.buildSuccessResp(result);
    }

    private boolean removeFileLogContentIfResultIsLarge(List<FileDistributionDetailVO> fileDistDetailVOS) {
        // 超过128K
        boolean removeFileLogContent = calculateFileLogContentLength(fileDistDetailVOS) > 131072L;
        if (removeFileLogContent) {
            fileDistDetailVOS.forEach(fileDistributionDetailVO -> {
                fileDistributionDetailVO.setLogContent(null);
            });
        }
        return removeFileLogContent;
    }

    private long calculateFileLogContentLength(List<FileDistributionDetailVO> fileDistDetailVOS) {
        long length = 0;
        for (FileDistributionDetailVO fileDistributionDetailVO : fileDistDetailVOS) {
            length += (StringUtils.isEmpty(fileDistributionDetailVO.getLogContent()) ?
                0 : fileDistributionDetailVO.getLogContent().getBytes(StandardCharsets.UTF_8).length);
        }
        return length;
    }

    private FileDistributionDetailVO convertToFileDistributionDetailVO(ServiceFileTaskLogDTO fileLog) {
        FileDistributionDetailVO fileDistDetailVO = new FileDistributionDetailVO();
        fileDistDetailVO.setTaskId(fileLog.getTaskId());
        fileDistDetailVO.setMode(fileLog.getMode());
        if (FileDistModeEnum.UPLOAD.getValue().equals(fileLog.getMode())) {
            fileDistDetailVO.setSrcIp(fileLog.getDisplaySrcIp());
            fileDistDetailVO.setFileName(fileLog.getDisplaySrcFile());
        } else {
            fileDistDetailVO.setSrcIp(fileLog.getDisplaySrcIp());
            fileDistDetailVO.setDestIp(fileLog.getDestIp());
            fileDistDetailVO.setFileName(fileLog.getDestFile());
        }
        fileDistDetailVO.setFileSize(fileLog.getSize());
        fileDistDetailVO.setProgress(fileLog.getProcess());
        fileDistDetailVO.setSpeed(fileLog.getSpeed());
        FileDistStatusEnum fileDistStatus = FileDistStatusEnum.getFileDistStatus(fileLog.getStatus());
        fileDistDetailVO.setStatusDesc(fileDistStatus != null ? fileDistStatus.getName() : "");
        fileDistDetailVO.setStatus(fileLog.getStatus());
        fileDistDetailVO.setLogContent(fileLog.getContent());
        return fileDistDetailVO;
    }

    @Override
    public Response<List<FileDistributionDetailVO>> getFileLogContentByFileTaskIds(String username, Long appId,
                                                                                   Long stepInstanceId,
                                                                                   Integer executeCount,
                                                                                   List<String> taskIds) {

        List<ServiceFileTaskLogDTO> fileTaskLogs = logService.getFileLogContentByTaskIds(stepInstanceId, executeCount
            , taskIds);
        if (CollectionUtils.isEmpty(fileTaskLogs)) {
            return Response.buildSuccessResp(null);
        }
        List<FileDistributionDetailVO> fileDistDetailVOS = new ArrayList<>();
        fileTaskLogs.forEach(fileLog -> {
            fileDistDetailVOS.add(convertToFileDistributionDetailVO(fileLog));
        });
        return Response.buildSuccessResp(fileDistDetailVOS);
    }

    @Override
    public Response<List<HostDTO>> getHostsByResultType(String username, Long appId, Long stepInstanceId,
                                                        Integer executeCount, Integer resultType,
                                                        String tag, String keyword) {
        List<IpDTO> hosts = taskResultService.getHostsByResultType(username, appId, stepInstanceId, executeCount,
            resultType, tag, keyword);
        return Response.buildSuccessResp(hosts.stream().map(IpDTO::toHost)
            .collect(Collectors.toList()));
    }

    @Override
    public Response<List<StepExecutionRecordVO>> listStepExecutionHistory(String username, Long appId,
                                                                          Long stepInstanceId) {
        List<StepExecutionRecordDTO> stepExecutionRecords = taskResultService.listStepExecutionHistory(username,
            appId, stepInstanceId);

        return Response.buildSuccessResp(stepExecutionRecords.stream().map(stepExecutionRecord -> {
            StepExecutionRecordVO vo = new StepExecutionRecordVO();
            vo.setStepInstanceId(stepInstanceId);
            vo.setRetryCount(stepExecutionRecord.getRetryCount());
            vo.setCreateTime(stepExecutionRecord.getCreateTime());
            return vo;
        }).collect(Collectors.toList()));
    }
}
