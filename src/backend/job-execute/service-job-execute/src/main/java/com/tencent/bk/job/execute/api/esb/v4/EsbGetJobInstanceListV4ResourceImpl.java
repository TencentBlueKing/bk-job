/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

package com.tencent.bk.job.execute.api.esb.v4;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.metrics.EsbApiTimed;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.esb.util.EsbDTOAppScopeMappingHelper;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.metrics.CommonMetricNames;
import com.tencent.bk.job.common.model.DeepPaginationCondition;
import com.tencent.bk.job.common.model.ValidateResult;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.constants.Consts;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceQuery;
import com.tencent.bk.job.execute.model.esb.v4.resp.V4GetJobInstanceListResult;
import com.tencent.bk.job.execute.model.esb.v4.resp.V4JobInstanceDTO;
import com.tencent.bk.job.execute.service.TaskResultService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
@Slf4j
public class EsbGetJobInstanceListV4ResourceImpl implements EsbGetJobInstanceListV4Resource {

    private final TaskResultService taskResultService;
    private final AppScopeMappingService appScopeMappingService;

    public EsbGetJobInstanceListV4ResourceImpl(TaskResultService taskResultService,
                                               AppScopeMappingService appScopeMappingService) {
        this.taskResultService = taskResultService;
        this.appScopeMappingService = appScopeMappingService;
    }

    @Override
    @EsbApiTimed(value = CommonMetricNames.ESB_API, extraTags = {"api_name", "v4_get_job_instance_list"})
    public EsbV4Response<V4GetJobInstanceListResult> getJobInstanceList(String username,
                                                                        String appCode,
                                                                        String scopeType,
                                                                        String scopeId,
                                                                        Long createTimeStart,
                                                                        Long createTimeEnd,
                                                                        Long jobInstanceId,
                                                                        String operator,
                                                                        String taskName,
                                                                        Integer startupMode,
                                                                        Integer taskType,
                                                                        Integer taskStatus,
                                                                        String ip,
                                                                        Long cronId,
                                                                        Long cursorJobInstanceId,
                                                                        Integer length) {
        GetJobInstanceListReqValidationTarget validationTarget = new GetJobInstanceListReqValidationTarget();
        validationTarget.setCreateTimeStart(createTimeStart);
        validationTarget.setCreateTimeEnd(createTimeEnd);
        validationTarget.setJobInstanceId(jobInstanceId);
        validationTarget.setTaskType(taskType);
        validationTarget.setTaskStatus(taskStatus);
        validationTarget.setStartupMode(startupMode);
        ValidateResult validateResult = checkRequest(validationTarget);

        if (!validateResult.isPass()) {
            log.warn("Get job instance ip log request is illegal!");
            throw new InvalidParamException(validateResult);
        }

        Long appId = appScopeMappingService.getAppIdByScope(scopeType, scopeId);
        TaskInstanceQuery query = new TaskInstanceQuery();
        query.setAppId(appId);
        query.setTaskInstanceId(jobInstanceId);
        query.setOperator(operator);
        query.setTaskName(taskName);
        if (taskStatus != null) {
            query.setStatus(RunStatusEnum.valueOf(taskStatus));
        }
        if (startupMode != null) {
            query.setStartupModes(Collections.singletonList(TaskStartupModeEnum.getStartupMode(startupMode)));
        }
        if (taskType != null) {
            query.setTaskType(TaskTypeEnum.valueOf(taskType));
        }
        query.setIp(ip);
        query.setCronTaskId(cronId);
        query.setStartTime(createTimeStart);
        query.setEndTime(createTimeEnd);
        query.setIp(ip);

        DeepPaginationCondition condition = new DeepPaginationCondition();
        if (cursorJobInstanceId != null) {
            condition.setStartId(cursorJobInstanceId);
        }
        if (length != null) {
            condition.setLength(length);
        }

        List<TaskInstanceDTO> jobInstanceList = taskResultService.listJobInstanceStartingFromId(query, condition);
        V4GetJobInstanceListResult result = buildFinalResult(jobInstanceList);
        return EsbV4Response.success(result);
    }

    private ValidateResult checkRequest(GetJobInstanceListReqValidationTarget validationTarget) {
        if (validationTarget.getJobInstanceId() != null) {
            if (validationTarget.getJobInstanceId() < 1) {
                log.warn("jobInstanceId is illegal, jobInstanceId={}", validationTarget.getJobInstanceId());
                return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "job_instance_id");
            } else {
                // 若是查询条件中有 job_instance_id 则其他条件不需要再判断
                return ValidateResult.pass();
            }
        }

        if (validationTarget.getCreateTimeStart() == null || validationTarget.getCreateTimeStart() < 1) {
            log.warn("createTimeStart is empty or illegal, createTimeStart={}", validationTarget.getCreateTimeStart());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "create_time_start");
        }
        if (validationTarget.getCursorJobInstanceId() != null && validationTarget.getCursorJobInstanceId() < 1) {
            log.warn("cursorJobInstanceId is illegal, cursorJobInstanceId={}",
                validationTarget.getCursorJobInstanceId());
            return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "cursor_job_instance_id");
        }
        if (validationTarget.getCreateTimeEnd() == null || validationTarget.getCreateTimeEnd() < 1) {
            log.warn("createTimeEnd is empty or illegal, createTimeEnd={}", validationTarget.getCreateTimeEnd());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME, "create_time_end");
        }
        long period = validationTarget.getCreateTimeEnd() - validationTarget.getCreateTimeStart();
        if (period <= 0) {
            log.warn("CreateStartTime is greater or equal to createTimeEnd, getCreateTimeStart={}, createTimeEnd={}",
                validationTarget.getCreateTimeStart(), validationTarget.getCreateTimeEnd());
            return ValidateResult.fail(ErrorCode.MISSING_OR_ILLEGAL_PARAM_WITH_PARAM_NAME,
                "create_time_start|create_time_end");
        } else if (period > Consts.MAX_SEARCH_TASK_HISTORY_RANGE_MILLS) {
            log.warn("Search time range greater than {} days!", Consts.MAX_SEARCH_TASK_HISTORY_RANGE_MILLS);
            return ValidateResult.fail(
                ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME,
                "create_time_start|create_time_end");
        }
        if (validationTarget.getLength() != null
            && (validationTarget.getLength() < 1 || validationTarget.getLength() > 1000)) {
            log.warn("length is illegal, length={}", validationTarget.getLength());
            return ValidateResult.fail(
                ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME_AND_REASON,
                new String[]{"length", "must be between 1 and 1000"}
            );
        }
        if (validationTarget.getTaskType() != null && TaskTypeEnum.valueOf(validationTarget.getTaskType()) == null) {
            log.warn("Param type is illegal!");
            return ValidateResult.fail(ErrorCode.ILLEGAL_PARAM_WITH_PARAM_NAME, "type");
        }
        return ValidateResult.pass();
    }

    private V4JobInstanceDTO convertTaskInstanceToV4DTO(TaskInstanceDTO taskInstanceDTO) {
        V4JobInstanceDTO v4JobInstanceDTO = new V4JobInstanceDTO();
        EsbDTOAppScopeMappingHelper.fillEsbAppScopeDTOByAppId(taskInstanceDTO.getAppId(), v4JobInstanceDTO);
        v4JobInstanceDTO.setId(taskInstanceDTO.getId());
        v4JobInstanceDTO.setName(taskInstanceDTO.getName());
        v4JobInstanceDTO.setTemplateId(taskInstanceDTO.getTaskTemplateId());
        v4JobInstanceDTO.setTaskId(taskInstanceDTO.getPlanId());
        v4JobInstanceDTO.setCreateTime(taskInstanceDTO.getCreateTime());
        v4JobInstanceDTO.setStartTime(taskInstanceDTO.getStartTime());
        v4JobInstanceDTO.setEndTime(taskInstanceDTO.getEndTime());
        v4JobInstanceDTO.setStartupMode(taskInstanceDTO.getStartupMode());
        v4JobInstanceDTO.setOperator(taskInstanceDTO.getOperator());
        v4JobInstanceDTO.setStatus(taskInstanceDTO.getStatus().getValue());
        v4JobInstanceDTO.setTotalTime(taskInstanceDTO.getTotalTime());
        v4JobInstanceDTO.setType(taskInstanceDTO.getType());

        return v4JobInstanceDTO;
    }

    private V4GetJobInstanceListResult buildFinalResult(List<TaskInstanceDTO> taskInstanceList) {
        ArrayList<V4JobInstanceDTO> jobInstanceResultList = new ArrayList<>();
        Long cursor = null;
        for (TaskInstanceDTO taskInstanceDTO : taskInstanceList) {
            jobInstanceResultList.add(convertTaskInstanceToV4DTO(taskInstanceDTO));
            // 由于是倒着差，所以下次查询的游标应该是本次结果集中最小的id
            cursor = cursor == null ? taskInstanceDTO.getId() : Math.min(cursor, taskInstanceDTO.getId());
        }
        V4GetJobInstanceListResult result = new V4GetJobInstanceListResult();
        result.setList(jobInstanceResultList);
        result.setNewJobInstanceIdCursor(cursor);
        return result;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    class GetJobInstanceListReqValidationTarget {

        private Long createTimeStart;

        private Long createTimeEnd;

        private Long jobInstanceId;

        private Integer startupMode;

        private Integer taskType;

        private Integer taskStatus;

        private Integer cursorJobInstanceId;

        private Integer length;
    }
}
