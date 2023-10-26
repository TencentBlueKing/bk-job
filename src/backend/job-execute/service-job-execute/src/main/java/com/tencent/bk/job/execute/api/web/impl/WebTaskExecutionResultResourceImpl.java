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
import com.tencent.bk.audit.annotations.AuditEntry;
import com.tencent.bk.job.common.constant.Bool;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.Order;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.gse.constants.FileDistModeEnum;
import com.tencent.bk.job.common.gse.util.AgentUtils;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.constant.ActionId;
import com.tencent.bk.job.common.model.BaseSearchCondition;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.PageData;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.AppResourceScope;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.CustomCollectionUtils;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.util.ip.IpUtils;
import com.tencent.bk.job.execute.api.web.WebTaskExecutionResultResource;
import com.tencent.bk.job.execute.auth.ExecuteAuthService;
import com.tencent.bk.job.execute.common.constants.FileDistStatusEnum;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTotalTimeTypeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.engine.consts.AgentTaskStatusEnum;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import com.tencent.bk.job.execute.model.AgentTaskDetailDTO;
import com.tencent.bk.job.execute.model.AgentTaskResultGroupDTO;
import com.tencent.bk.job.execute.model.FileIpLogContent;
import com.tencent.bk.job.execute.model.ScriptHostLogContent;
import com.tencent.bk.job.execute.model.StepExecutionDTO;
import com.tencent.bk.job.execute.model.StepExecutionDetailDTO;
import com.tencent.bk.job.execute.model.StepExecutionRecordDTO;
import com.tencent.bk.job.execute.model.StepExecutionResultQuery;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.StepInstanceRollingTaskDTO;
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
import com.tencent.bk.job.execute.model.web.vo.RollingStepBatchTaskVO;
import com.tencent.bk.job.execute.model.web.vo.StepExecutionDetailVO;
import com.tencent.bk.job.execute.model.web.vo.StepExecutionRecordVO;
import com.tencent.bk.job.execute.model.web.vo.StepExecutionVO;
import com.tencent.bk.job.execute.model.web.vo.TaskExecuteResultVO;
import com.tencent.bk.job.execute.model.web.vo.TaskExecutionVO;
import com.tencent.bk.job.execute.model.web.vo.TaskInstanceVO;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.StepInstanceVariableValueService;
import com.tencent.bk.job.execute.service.TaskInstanceAccessProcessor;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceVariableService;
import com.tencent.bk.job.execute.service.TaskResultService;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
import com.tencent.bk.job.manage.api.inner.ServiceNotificationResource;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.model.inner.ServiceAppRoleDTO;
import com.tencent.bk.job.manage.model.inner.ServiceNotifyChannelDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.tencent.bk.job.execute.constants.Consts.MAX_SEARCH_TASK_HISTORY_RANGE_MILLS;

@RestController
@Slf4j
public class WebTaskExecutionResultResourceImpl implements WebTaskExecutionResultResource {
    private final TaskResultService taskResultService;
    private final MessageI18nService i18nService;
    private final LogService logService;
    private final StepInstanceVariableValueService stepInstanceVariableValueService;
    private final TaskInstanceService taskInstanceService;
    private final TaskInstanceVariableService taskInstanceVariableService;
    private final ServiceNotificationResource notifyResource;
    private final ExecuteAuthService executeAuthService;
    private final TaskInstanceAccessProcessor taskInstanceAccessProcessor;


    private final LoadingCache<String, Map<String, String>> roleCache = CacheBuilder.newBuilder()
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
    private final LoadingCache<String, Map<String, String>> channelCache = CacheBuilder.newBuilder()
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
                                              ServiceNotificationResource notifyResource,
                                              ExecuteAuthService executeAuthService,
                                              TaskInstanceAccessProcessor taskInstanceAccessProcessor) {
        this.taskResultService = taskResultService;
        this.i18nService = i18nService;
        this.logService = logService;
        this.stepInstanceVariableValueService = stepInstanceVariableValueService;
        this.taskInstanceService = taskInstanceService;
        this.taskInstanceVariableService = taskInstanceVariableService;
        this.notifyResource = notifyResource;
        this.executeAuthService = executeAuthService;
        this.taskInstanceAccessProcessor = taskInstanceAccessProcessor;
    }

    @Override
    public Response<PageData<TaskInstanceVO>> getTaskHistoryList(String username,
                                                                 AppResourceScope appResourceScope,
                                                                 String scopeType,
                                                                 String scopeId,
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
                                                                 Boolean countPageTotal,
                                                                 Long cronTaskId,
                                                                 String startupModes,
                                                                 String ip) {
        TaskInstanceQuery taskQuery = buildListTaskInstanceQuery(appResourceScope, taskName, taskInstanceId,
            status, operator, taskType, startTime, endTime, timeRange, totalTimeType, cronTaskId, startupModes, ip);
        BaseSearchCondition baseSearchCondition = BaseSearchCondition.pageCondition(start, pageSize,
            countPageTotal == null ? true : countPageTotal);

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
                .convertToTaskInstanceVO(taskInstanceDTO)));
        }
        pageDataVO.setData(taskInstanceVOS);
        batchSetPermissionsForTaskInstance(username, appResourceScope, taskInstanceVOS);
        return Response.buildSuccessResp(pageDataVO);
    }

    private TaskInstanceQuery buildListTaskInstanceQuery(AppResourceScope appResourceScope,
                                                         String taskName,
                                                         Long taskInstanceId,
                                                         Integer status,
                                                         String operator,
                                                         Integer taskType,
                                                         String startTime,
                                                         String endTime,
                                                         Integer timeRange,
                                                         TaskTotalTimeTypeEnum totalTimeType,
                                                         Long cronTaskId,
                                                         String startupModes,
                                                         String ip) {
        TaskInstanceQuery taskQuery = new TaskInstanceQuery();
        taskQuery.setTaskInstanceId(taskInstanceId);
        taskQuery.setAppId(appResourceScope.getAppId());
        taskQuery.setTaskName(taskName);
        taskQuery.setCronTaskId(cronTaskId);

        validateAndSetQueryTimeRange(taskQuery, startTime, endTime, timeRange);

        setTotalTimeCondition(taskQuery, totalTimeType);
        taskQuery.setOperator(operator);
        setStartupModeCondition(taskQuery, startupModes);
        if (taskType != null) {
            taskQuery.setTaskType(TaskTypeEnum.valueOf(taskType));
        }
        if (status != null) {
            taskQuery.setStatus(RunStatusEnum.valueOf(status));
        }
        if (StringUtils.isNotEmpty(ip)) {
            if (IpUtils.checkIpv4(ip)) {
                taskQuery.setIp(ip);
            } else if (IpUtils.checkIpv6(ip)) {
                taskQuery.setIpv6(ip);
            } else {
                log.warn("Invalid ip {}", ip);
            }
        }
        taskQuery.setIp(ip);

        return taskQuery;
    }

    private void validateAndSetQueryTimeRange(TaskInstanceQuery taskInstanceQuery,
                                              String startTime,
                                              String endTime,
                                              Integer timeRange) {
        Long start = null;
        Long end = null;
        if (timeRange != null) {
            if (timeRange < 1) {
                log.warn("Param timeRange should greater than 0");
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
            }
            if (timeRange > 30) {
                log.warn("Param timeRange should less then 30");
                throw new FailedPreconditionException(ErrorCode.TASK_INSTANCE_QUERY_TIME_SPAN_MORE_THAN_30_DAYS);
            }
            // 当天结束时间 - 往前的天数
            start = DateUtils.getUTCCurrentDayEndTimestamp() - timeRange * 24 * 3600 * 1000L;
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
                throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
            }
            if (end == null) {
                end = System.currentTimeMillis();
            }
            if (end - start > MAX_SEARCH_TASK_HISTORY_RANGE_MILLS) {
                log.info("Query task instance history time span must be less than 30 days");
                throw new FailedPreconditionException(ErrorCode.TASK_INSTANCE_QUERY_TIME_SPAN_MORE_THAN_30_DAYS);
            }
        }

        taskInstanceQuery.setStartTime(start);
        taskInstanceQuery.setEndTime(end);
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

    private void batchSetPermissionsForTaskInstance(String username, AppResourceScope appResourceScope,
                                                    List<TaskInstanceVO> taskInstances) {
        if (CustomCollectionUtils.isEmptyCollection(taskInstances)) {
            return;
        }
        boolean hasViewAllPermission = executeAuthService.authViewAllTaskInstance(
            username, appResourceScope).isPass();
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
    @AuditEntry(actionId = ActionId.VIEW_HISTORY)
    public Response<TaskExecuteResultVO> getTaskExecutionResult(String username,
                                                                AppResourceScope appResourceScope,
                                                                String scopeType,
                                                                String scopeId,
                                                                Long taskInstanceId) {
        TaskExecuteResultDTO taskExecuteResult = taskResultService.getTaskExecutionResult(username,
            appResourceScope.getAppId(), taskInstanceId);
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
        taskExecutionVO.setStatusDesc(i18nService.getI18n(
            RunStatusEnum.valueOf(taskExecutionDTO.getStatus()).getI18nKey()));
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
            stepExecutionVO.setStatusDesc(i18nService.getI18n(runStatus.getI18nKey()));
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
    @AuditEntry(actionId = ActionId.VIEW_HISTORY)
    public Response<StepExecutionDetailVO> getStepExecutionResult(String username,
                                                                  AppResourceScope appResourceScope,
                                                                  String scopeType,
                                                                  String scopeId,
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
        StepExecutionResultQuery query = StepExecutionResultQuery.builder()
            .stepInstanceId(stepInstanceId)
            .executeCount(executeCount)
            .batch(batch == null ? null : (batch == 0 ? null : batch))
            .filterByLatestBatch(batch == null)
            .status(resultType)
            .tag(tag)
            .logKeyword(keyword)
            .searchIp(searchIp)
            .maxAgentTasksForResultGroup(maxIpsPerResultGroup)
            .orderField(orderField)
            .order(Order.valueOf(order))
            .build();

        StepExecutionDetailDTO executionResult = taskResultService.getStepExecutionResult(username,
            appResourceScope.getAppId(), query);
        return Response.buildSuccessResp(convertToStepInstanceExecutionDetailVO(executionResult));
    }

    @Override
    @AuditEntry(actionId = ActionId.VIEW_HISTORY)
    public Response<StepExecutionDetailVO> getFastTaskStepExecutionResult(String username,
                                                                          AppResourceScope appResourceScope,
                                                                          String scopeType,
                                                                          String scopeId,
                                                                          Long taskInstanceId,
                                                                          Integer batch,
                                                                          Integer resultType,
                                                                          String tag,
                                                                          Integer maxIpsPerResultGroup,
                                                                          String orderField,
                                                                          Integer order) {
        StepExecutionResultQuery query = StepExecutionResultQuery.builder()
            .batch(batch)
            .status(resultType)
            .tag(tag)
            .maxAgentTasksForResultGroup(maxIpsPerResultGroup)
            .orderField(orderField)
            .order(Order.valueOf(order))
            .build();
        StepExecutionDetailDTO executionResult = taskResultService.getFastTaskStepExecutionResult(username,
            appResourceScope.getAppId(), taskInstanceId, query);
        return Response.buildSuccessResp(convertToStepInstanceExecutionDetailVO(executionResult));
    }

    private StepExecutionDetailVO convertToStepInstanceExecutionDetailVO(StepExecutionDetailDTO executionDetail) {
        StepExecutionDetailVO stepExecutionDetailVO = new StepExecutionDetailVO();
        stepExecutionDetailVO.setFinished(executionDetail.isFinished());
        stepExecutionDetailVO.setName(executionDetail.getName());
        stepExecutionDetailVO.setStepInstanceId(executionDetail.getStepInstanceId());
        stepExecutionDetailVO.setRetryCount(executionDetail.getExecuteCount());
        stepExecutionDetailVO.setStatus(executionDetail.getStatus().getValue());
        stepExecutionDetailVO.setStatusDesc(
            i18nService.getI18n(executionDetail.getStatus().getI18nKey()));
        stepExecutionDetailVO.setStartTime(executionDetail.getStartTime());
        stepExecutionDetailVO.setEndTime(executionDetail.getEndTime());
        stepExecutionDetailVO.setTotalTime(executionDetail.getTotalTime());
        stepExecutionDetailVO.setIsLastStep(executionDetail.isLastStep());
        stepExecutionDetailVO.setType(executionDetail.getStepType().getValue());
        stepExecutionDetailVO.setRunMode(executionDetail.getRunMode().getValue());

        List<ExecutionResultGroupVO> resultGroupVOS = new ArrayList<>();
        for (AgentTaskResultGroupDTO resultGroup : executionDetail.getResultGroups()) {
            ExecutionResultGroupVO executionResultGroupVO = new ExecutionResultGroupVO();
            executionResultGroupVO.setResultType(resultGroup.getStatus());
            executionResultGroupVO.setResultTypeDesc(
                i18nService.getI18n(AgentTaskStatusEnum.valueOf(resultGroup.getStatus()).getI18nKey()));
            executionResultGroupVO.setTag(resultGroup.getTag());
            executionResultGroupVO.setAgentTaskSize(resultGroup.getTotalAgentTasks());

            List<AgentTaskExecutionVO> agentTaskExecutionVOS = new ArrayList<>();
            if (resultGroup.getAgentTasks() != null) {
                for (AgentTaskDetailDTO agentTask : resultGroup.getAgentTasks()) {
                    AgentTaskExecutionVO agentTaskVO = new AgentTaskExecutionVO();
                    agentTaskVO.setHostId(agentTask.getHostId());
                    agentTaskVO.setAgentId(AgentUtils.displayAsRealAgentId(agentTask.getAgentId()));
                    agentTaskVO.setIp(agentTask.getCloudIp());
                    agentTaskVO.setIpv4(agentTask.getIp());
                    agentTaskVO.setIpv6(agentTask.getIpv6());
                    agentTaskVO.setEndTime(agentTask.getEndTime());
                    agentTaskVO.setStartTime(agentTask.getStartTime());
                    agentTaskVO.setStatus(agentTask.getStatus().getValue());
                    agentTaskVO.setStatusDesc(i18nService.getI18n(agentTask.getStatus().getI18nKey()));
                    agentTaskVO.setErrorCode(agentTask.getErrorCode());
                    agentTaskVO.setExitCode(agentTask.getExitCode());
                    agentTaskVO.setTag(agentTask.getTag());
                    agentTaskVO.setTotalTime(agentTask.getTotalTime());
                    agentTaskVO.setCloudAreaId(agentTask.getBkCloudId());
                    agentTaskVO.setCloudAreaName(agentTask.getBkCloudName());
                    agentTaskVO.setRetryCount(agentTask.getExecuteCount());
                    agentTaskVO.setBatch(agentTask.getBatch());
                    agentTaskExecutionVOS.add(agentTaskVO);
                }
            }
            executionResultGroupVO.setAgentTaskExecutionDetail(agentTaskExecutionVOS);

            resultGroupVOS.add(executionResultGroupVO);
        }
        stepExecutionDetailVO.setResultGroups(resultGroupVOS);

        if (CollectionUtils.isNotEmpty(executionDetail.getRollingTasks())) {
            stepExecutionDetailVO.setRollingTasks(toRollingStepBatchTaskVOs(executionDetail.getLatestBatch(),
                executionDetail.getRollingTasks()));
        }

        return stepExecutionDetailVO;
    }

    private List<RollingStepBatchTaskVO> toRollingStepBatchTaskVOs(Integer latestBatch,
                                                                   List<StepInstanceRollingTaskDTO> stepInstanceRollingTasks) {
        return stepInstanceRollingTasks.stream().map(stepInstanceRollingTask -> {
            RollingStepBatchTaskVO vo = new RollingStepBatchTaskVO();
            vo.setBatch(stepInstanceRollingTask.getBatch());
            vo.setStatus(stepInstanceRollingTask.getStatus().getValue());
            vo.setLatestBatch(latestBatch.equals(stepInstanceRollingTask.getBatch()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @AuditEntry(actionId = ActionId.VIEW_HISTORY)
    public Response<IpScriptLogContentVO> getScriptLogContentByHost(String username,
                                                                    AppResourceScope appResourceScope,
                                                                    String scopeType,
                                                                    String scopeId,
                                                                    Long stepInstanceId,
                                                                    Integer executeCount,
                                                                    Long hostId,
                                                                    Integer batch) {
        auditAndAuthViewStepInstance(username, appResourceScope, stepInstanceId);

        ScriptHostLogContent scriptHostLogContent = logService.getScriptHostLogContent(stepInstanceId, executeCount,
            batch, HostDTO.fromHostId(hostId));
        IpScriptLogContentVO ipScriptLogContentVO = new IpScriptLogContentVO();
        if (scriptHostLogContent != null) {
            ipScriptLogContentVO.setDisplayIp(scriptHostLogContent.getCloudIp());
            ipScriptLogContentVO.setLogContent(scriptHostLogContent.getContent());
            ipScriptLogContentVO.setFinished(scriptHostLogContent.isFinished());
        }
        return Response.buildSuccessResp(ipScriptLogContentVO);
    }

    @Override
    @AuditEntry(actionId = ActionId.VIEW_HISTORY)
    public Response<List<ExecuteVariableVO>> getStepVariableByHost(String username,
                                                                   AppResourceScope appResourceScope,
                                                                   String scopeType,
                                                                   String scopeId,
                                                                   Long stepInstanceId,
                                                                   Long hostId,
                                                                   String ip) {
        StepInstanceDTO stepInstance = taskInstanceService.getStepInstanceDetail(
            appResourceScope.getAppId(), stepInstanceId);
        if (!stepInstance.getExecuteType().equals(StepExecuteTypeEnum.EXECUTE_SCRIPT.getValue())
            || !stepInstance.getScriptType().equals(ScriptTypeEnum.SHELL.getValue())) {
            return Response.buildSuccessResp(Collections.emptyList());
        }

        taskInstanceAccessProcessor.processBeforeAccess(username,
            appResourceScope.getAppId(), stepInstance.getTaskInstanceId());

        List<ExecuteVariableVO> taskVariableVOS = getStepVariableByHost(stepInstance, hostId, ip);
        return Response.buildSuccessResp(taskVariableVOS);
    }

    private List<ExecuteVariableVO> getStepVariableByHost(StepInstanceBaseDTO stepInstance,
                                                          Long hostId,
                                                          String ip) {
        List<TaskVariableDTO> taskVars =
            taskInstanceVariableService.getByTaskInstanceId(stepInstance.getTaskInstanceId());
        if (taskVars == null || taskVars.isEmpty()) {
            return Collections.emptyList();
        }
        List<ExecuteVariableVO> taskVariableVOS = new ArrayList<>();
        List<TaskVariableDTO> changeableVars = new ArrayList<>();
        for (TaskVariableDTO taskVar : taskVars) {
            // 主机变量无需返回
            if (taskVar.getType() == TaskVariableTypeEnum.HOST_LIST.getType()) {
                continue;
            }
            if (!taskVar.isChangeable()) {
                taskVariableVOS.add(convertToTaskVariableVO(taskVar));
            } else {
                changeableVars.add(taskVar);
            }
        }

        if (CollectionUtils.isNotEmpty(changeableVars)) {
            appendStepChangeableVars(taskVariableVOS, stepInstance, changeableVars, hostId, ip);
        }

        return taskVariableVOS;
    }

    private void appendStepChangeableVars(List<ExecuteVariableVO> taskVariableVOS,
                                          StepInstanceBaseDTO stepInstance,
                                          List<TaskVariableDTO> changeableVars,
                                          Long hostId,
                                          String ip) {
        long stepInstanceId = stepInstance.getId();

        StepInstanceVariableValuesDTO inputStepInstanceValues = stepInstanceVariableValueService
            .computeInputStepInstanceVariableValues(stepInstance.getTaskInstanceId(), stepInstanceId, changeableVars);
        if (inputStepInstanceValues == null) {
            changeableVars.stream().filter(var -> !var.getType().equals(TaskVariableTypeEnum.HOST_LIST.getType()) &&
                var.isChangeable()).forEach(var -> taskVariableVOS.add(convertToTaskVariableVO(var)));
            return;
        }

        Map<String, TaskVariableDTO> taskVariablesMap = new HashMap<>();
        for (TaskVariableDTO taskVar : changeableVars) {
            taskVariablesMap.put(taskVar.getName(), taskVar);
        }

        // 命名空间变量
        appendNamespaceVars(taskVariableVOS, changeableVars, inputStepInstanceValues, taskVariablesMap, hostId, ip);

        // 全局变量(除命名空间变量)
        appendGlobalChangeableVars(taskVariableVOS, inputStepInstanceValues, taskVariablesMap);
    }

    private void appendNamespaceVars(List<ExecuteVariableVO> taskVariableVOS,
                                     List<TaskVariableDTO> changeableVars,
                                     StepInstanceVariableValuesDTO inputStepInstanceValues,
                                     Map<String, TaskVariableDTO> taskVariablesMap,
                                     Long hostId,
                                     String ip) {
        Map<String, VariableValueDTO> hostVariables = new HashMap<>();
        if (inputStepInstanceValues.getNamespaceParamsMap() != null
            && !inputStepInstanceValues.getNamespaceParamsMap().isEmpty()) {
            // 命名空间变量的数据，之前的版本不包含hostId,只包含ip；需要兼容hostId/ip查询
            boolean isFilterByHostId = inputStepInstanceValues.getNamespaceParams().get(0).getHostId() != null;
            // Map<varName,varValue>
            if (isFilterByHostId) {
                inputStepInstanceValues.getNamespaceParamsMap()
                    .forEach((host, hostVars) -> {
                        if (host.getHostId() != null && host.getHostId().equals(hostId)) {
                            hostVars.forEach(hostVariables::put);
                        }
                    });
            } else {
                // 兼容历史数据，命名空间变量只有ip的场景
                inputStepInstanceValues.getNamespaceParamsMap()
                    .forEach((host, hostVars) -> {
                        if (host.toCloudIp() != null && host.toCloudIp().equals(ip)) {
                            hostVars.forEach(hostVariables::put);
                        }
                    });
            }
        }
        changeableVars
            .stream()
            .filter(var -> var.getType().equals(TaskVariableTypeEnum.NAMESPACE.getType()))
            .forEach(var -> {
                ExecuteVariableVO vo = new ExecuteVariableVO();
                String varName = var.getName();
                vo.setName(varName);
                String varValue = hostVariables.get(varName) != null
                    ? hostVariables.get(varName).getValue() : taskVariablesMap.get(varName).getValue();
                vo.setValue(varValue);
                vo.setChangeable(Bool.TRUE.intValue());
                vo.setType(TaskVariableTypeEnum.NAMESPACE.getType());
                taskVariableVOS.add(vo);
            });
    }

    private void appendGlobalChangeableVars(List<ExecuteVariableVO> taskVariableVOS,
                                            StepInstanceVariableValuesDTO inputStepInstanceValues,
                                            Map<String, TaskVariableDTO> taskVariablesMap) {
        List<VariableValueDTO> globalVars = inputStepInstanceValues.getGlobalParams();
        if (CollectionUtils.isNotEmpty(globalVars)) {
            for (VariableValueDTO varValue : globalVars) {
                TaskVariableDTO taskVariable = taskVariablesMap.get(varValue.getName());
                if (taskVariable == null || !taskVariable.isChangeable()) {
                    // 过滤掉常量
                    continue;
                }

                ExecuteVariableVO vo = new ExecuteVariableVO();
                vo.setName(varValue.getName());
                vo.setValue(varValue.getValue());
                vo.setChangeable(Bool.TRUE.intValue());
                vo.setType(taskVariable.getType());
                taskVariableVOS.add(vo);
            }
        }
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
    @AuditEntry(actionId = ActionId.VIEW_HISTORY)
    public Response<IpFileLogContentVO> getFileLogContentByHost(String username,
                                                                AppResourceScope appResourceScope,
                                                                String scopeType,
                                                                String scopeId,
                                                                Long stepInstanceId,
                                                                Integer executeCount,
                                                                String ip,
                                                                Long hostId,
                                                                String mode,
                                                                Integer batch) {

        auditAndAuthViewStepInstance(username, appResourceScope, stepInstanceId);

        IpFileLogContentVO result = new IpFileLogContentVO();
        List<FileDistributionDetailVO> fileDistDetailVOS = new ArrayList<>();
        result.setFileDistributionDetails(fileDistDetailVOS);

        if ("download".equals(mode)) {
            FileIpLogContent downloadLog = logService.getFileIpLogContent(stepInstanceId, executeCount, batch,
                HostDTO.fromHostIdOrCloudIp(hostId, ip), FileDistModeEnum.DOWNLOAD.getValue());
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
                executeCount, batch);
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
            fileDistDetailVO.setFileName(fileLog.getDisplaySrcFile());
        } else {
            fileDistDetailVO.setDestIp(IpUtils.extractIp(fileLog.getDestIp()));
            fileDistDetailVO.setDestIpv6(IpUtils.extractIp(fileLog.getDestIpv6()));
            fileDistDetailVO.setFileName(fileLog.getDestFile());
        }
        boolean hideSrcIp = fileLog.getSrcFileType() != null
            && TaskFileTypeEnum.valueOf(fileLog.getSrcFileType()) != TaskFileTypeEnum.SERVER;
        fileDistDetailVO.setSrcIp(hideSrcIp ? "--" : IpUtils.extractIp(fileLog.getSrcIp()));
        fileDistDetailVO.setSrcIpv6(hideSrcIp ? "--" : IpUtils.extractIp(fileLog.getSrcIpv6()));
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
    @AuditEntry(actionId = ActionId.VIEW_HISTORY)
    public Response<List<FileDistributionDetailVO>> getFileLogContentByFileTaskIds(String username,
                                                                                   AppResourceScope appResourceScope,
                                                                                   String scopeType,
                                                                                   String scopeId,
                                                                                   Long stepInstanceId,
                                                                                   Integer executeCount,
                                                                                   Integer batch,
                                                                                   List<String> taskIds) {
        auditAndAuthViewStepInstance(username, appResourceScope, stepInstanceId);

        List<ServiceFileTaskLogDTO> fileTaskLogs = logService.getFileLogContentByTaskIds(stepInstanceId, executeCount,
            batch, taskIds);
        if (CollectionUtils.isEmpty(fileTaskLogs)) {
            return Response.buildSuccessResp(null);
        }
        List<FileDistributionDetailVO> fileDistDetailVOS = new ArrayList<>();
        fileTaskLogs.forEach(fileLog -> {
            fileDistDetailVOS.add(convertToFileDistributionDetailVO(fileLog));
        });
        return Response.buildSuccessResp(fileDistDetailVOS);
    }

    private void auditAndAuthViewStepInstance(String username, AppResourceScope appResourceScope, Long stepInstanceId) {
        StepInstanceBaseDTO stepInstance = taskInstanceService.getBaseStepInstance(
            appResourceScope.getAppId(), stepInstanceId);
        taskInstanceAccessProcessor.processBeforeAccess(username,
            appResourceScope.getAppId(), stepInstance.getTaskInstanceId());
    }

    @Override
    @AuditEntry(actionId = ActionId.VIEW_HISTORY)
    public Response<List<HostDTO>> getHostsByResultType(String username,
                                                        AppResourceScope appResourceScope,
                                                        String scopeType,
                                                        String scopeId,
                                                        Long stepInstanceId,
                                                        Integer executeCount,
                                                        Integer batch,
                                                        Integer resultType,
                                                        String tag,
                                                        String keyword) {
        List<HostDTO> hosts = taskResultService.getHostsByResultType(username, appResourceScope.getAppId(),
            stepInstanceId, executeCount, batch, resultType, tag, keyword);
        return Response.buildSuccessResp(hosts);
    }

    @Override
    @AuditEntry(actionId = ActionId.VIEW_HISTORY)
    public Response<List<StepExecutionRecordVO>> listStepExecutionHistory(String username,
                                                                          AppResourceScope appResourceScope,
                                                                          String scopeType,
                                                                          String scopeId,
                                                                          Long stepInstanceId,
                                                                          Integer batch) {

        List<StepExecutionRecordDTO> stepExecutionRecords = taskResultService.listStepExecutionHistory(username,
            appResourceScope.getAppId(), stepInstanceId, batch);

        return Response.buildSuccessResp(stepExecutionRecords.stream().map(stepExecutionRecord -> {
            StepExecutionRecordVO vo = new StepExecutionRecordVO();
            vo.setStepInstanceId(stepInstanceId);
            vo.setRetryCount(stepExecutionRecord.getRetryCount());
            vo.setCreateTime(stepExecutionRecord.getCreateTime());
            return vo;
        }).collect(Collectors.toList()));
    }
}
