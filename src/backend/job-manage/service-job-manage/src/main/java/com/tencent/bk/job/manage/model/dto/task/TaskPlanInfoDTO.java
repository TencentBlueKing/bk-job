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

package com.tencent.bk.job.manage.model.dto.task;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.manage.model.esb.v3.response.EsbPlanInfoV3DTO;
import com.tencent.bk.job.manage.model.web.request.TaskPlanCreateUpdateReq;
import com.tencent.bk.job.manage.model.web.vo.task.TaskPlanVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @since 15/11/2019 15:49
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class TaskPlanInfoDTO {

    /**
     * 执行方案 ID
     */
    private Long id;

    /**
     * 业务 ID
     */
    private Long appId;

    /**
     * 模版 ID
     */
    private Long templateId;

    /**
     * 模板名称
     */
    private String templateName;

    /**
     * 执行方案名称
     */
    private String name;

    /**
     * 执行方案创建人
     */
    private String creator;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 更新用户
     */
    private String lastModifyUser;

    /**
     * 更新时间
     */
    private Long lastModifyTime;

    /**
     * 首个步骤 ID
     */
    private Long firstStepId;

    /**
     * 末尾步骤 ID
     */
    private Long lastStepId;

    /**
     * 是否需要更新
     */
    private Boolean needUpdate;

    /**
     * 是否关联定时任务
     */
    private Boolean hasCronJob;

    /**
     * 执行方案包含的步骤列表
     */
    private List<TaskStepDTO> stepList;

    /**
     * 执行方案包含的变量列表
     */
    private List<TaskVariableDTO> variableList;

    /**
     * 启用的步骤列表
     */
    private List<Long> enableStepList = new ArrayList<>();

    /**
     * 是否是调试方案
     */
    private Boolean debug = false;

    /**
     * 执行方案版本
     */
    private String version;

    /**
     * 作业模版版本
     */
    private String templateVersion;

    /**
     * 关联的定时任务个数
     */
    private Long cronJobCount;

    public static TaskPlanVO toVO(TaskPlanInfoDTO planInfo) {
        TaskPlanVO planVO = new TaskPlanVO();
        planVO.setId(planInfo.getId());
        planVO.setAppId(planInfo.getAppId());
        planVO.setTemplateId(planInfo.getTemplateId());
        planVO.setName(planInfo.getName());
        planVO.setCreator(planInfo.getCreator());
        planVO.setTemplateName(planInfo.getTemplateName());
        planVO.setCreateTime(planInfo.getCreateTime());
        planVO.setLastModifyUser(planInfo.getLastModifyUser());
        planVO.setLastModifyTime(planInfo.getLastModifyTime());
        planVO.setNeedUpdate(planInfo.getNeedUpdate());
        planVO.setHasCronJob(planInfo.getHasCronJob());
        planVO.setCronJobCount(planInfo.getCronJobCount());
        planVO.setVersion(planInfo.getVersion());
        planVO.setTemplateVersion(planInfo.getTemplateVersion());

        if (planInfo.getVariableList() != null) {
            planVO.setVariableList(
                planInfo.getVariableList().stream().map(TaskVariableDTO::toVO).collect(Collectors.toList()));
        }

        if (planInfo.getStepList() != null) {
            planVO.setStepList(planInfo.getStepList().stream().map(TaskStepDTO::toVO).collect(Collectors.toList()));
        }

        return planVO;
    }

    public static TaskPlanInfoDTO fromReq(String username, Long appId, TaskPlanCreateUpdateReq planCreateUpdateReq) {
        if (planCreateUpdateReq == null) {
            return null;
        }
        TaskPlanInfoDTO planInfo = new TaskPlanInfoDTO();
        planInfo.setAppId(appId);
        planInfo.setTemplateId(planCreateUpdateReq.getTemplateId());
        planInfo.setLastModifyUser(username);
        planInfo.setLastModifyTime(DateUtils.currentTimeSeconds());

        if (planCreateUpdateReq.getId() == null || planCreateUpdateReq.getId() <= 0) {
            planInfo.setCreator(username);
        } else {
            planInfo.setId(planCreateUpdateReq.getId());
        }
        planInfo.setName(planCreateUpdateReq.getName());
        if (CollectionUtils.isNotEmpty(planCreateUpdateReq.getEnableSteps())) {
            planInfo.setEnableStepList(planCreateUpdateReq.getEnableSteps());
        } else {
            planInfo.setEnableStepList(Collections.emptyList());
        }
        if (CollectionUtils.isNotEmpty(planCreateUpdateReq.getVariables())) {
            planInfo.setVariableList(
                planCreateUpdateReq.getVariables().stream().map(TaskVariableDTO::fromVO).collect(Collectors.toList()));
        } else {
            planInfo.setVariableList(Collections.emptyList());
        }
        planInfo.setDebug(false);

        return planInfo;
    }

    public static void buildPlanInfo(TaskPlanInfoDTO planInfo, TaskTemplateInfoDTO templateInfo) {
        if (templateInfo == null) {
            throw new NotFoundException(ErrorCode.TEMPLATE_NOT_EXIST);
        }
        if (planInfo == null) {
            throw new NotFoundException(ErrorCode.TASK_PLAN_NOT_EXIST);
        }
        planInfo.setAppId(templateInfo.getAppId());
        planInfo.setTemplateId(templateInfo.getId());
        planInfo.setFirstStepId(templateInfo.getFirstStepId());
        planInfo.setLastStepId(templateInfo.getLastStepId());
        planInfo.setNeedUpdate(false);
        if (planInfo.getDebug()) {
            planInfo.setDebug(true);
        } else {
            planInfo.setDebug(false);
        }

        planInfo.setStepList(templateInfo.getStepList());
        planInfo.getStepList().forEach(taskStep -> {
            taskStep.setTemplateStepId(taskStep.getId());
            taskStep.setId(null);
            if (planInfo.getEnableStepList().contains(taskStep.getTemplateStepId())) {
                taskStep.setEnable(1);
            } else {
                taskStep.setEnable(0);
            }
        });

        Map<Long, String> variableDefaultValueMap = new ConcurrentHashMap<>();
        if (CollectionUtils.isNotEmpty(planInfo.getVariableList())) {
            planInfo.getVariableList().parallelStream().forEach(taskVariableDTO -> {
                if (taskVariableDTO.getDefaultValue() == null) {
                    // No default value in request, skip
                    return;
                } else if (taskVariableDTO.getType().getMask() != null
                    && taskVariableDTO.getType().getMask().equals(taskVariableDTO.getDefaultValue())) {
                    // Type have mask and value equals mask, skip
                    return;
                }
                // Has default value in request and do not equal mask, keep
                variableDefaultValueMap.put(taskVariableDTO.getId(), taskVariableDTO.getDefaultValue());
            });
        }

        planInfo.setVariableList(templateInfo.getVariableList());
        planInfo.getVariableList().forEach(taskVariable -> {
            taskVariable.setTemplateId(null);
            if (variableDefaultValueMap.containsKey(taskVariable.getId())) {
                taskVariable.setDefaultValue(variableDefaultValueMap.get(taskVariable.getId()));
            }
        });
        planInfo.setVersion(templateInfo.getVersion());
    }

    public static TaskPlanInfoDTO fromVO(String username, Long appId, TaskPlanVO planInfo) {
        if (planInfo == null) {
            return null;
        }

        TaskPlanInfoDTO taskPlanInfoDTO = new TaskPlanInfoDTO();
        taskPlanInfoDTO.setId(planInfo.getId());
        taskPlanInfoDTO.setAppId(appId);
        taskPlanInfoDTO.setTemplateId(planInfo.getTemplateId());
        taskPlanInfoDTO.setName(planInfo.getName());
        taskPlanInfoDTO.setCreator(username);
        taskPlanInfoDTO.setCreateTime(DateUtils.currentTimeSeconds());
        taskPlanInfoDTO.setLastModifyUser(username);
        taskPlanInfoDTO.setLastModifyTime(DateUtils.currentTimeSeconds());
        taskPlanInfoDTO
            .setStepList(planInfo.getStepList().stream().map(TaskStepDTO::fromVO).collect(Collectors.toList()));

        taskPlanInfoDTO.setFirstStepId(taskPlanInfoDTO.getStepList().get(0).getTemplateStepId());
        taskPlanInfoDTO.setLastStepId(
            taskPlanInfoDTO.getStepList().get(taskPlanInfoDTO.getStepList().size() - 1).getTemplateStepId());

        if (CollectionUtils.isNotEmpty(planInfo.getVariableList())) {
            taskPlanInfoDTO.setVariableList(
                planInfo.getVariableList().parallelStream().map(TaskVariableDTO::fromVO).collect(Collectors.toList()));
        }
        taskPlanInfoDTO.setDebug(false);
        taskPlanInfoDTO.setVersion(planInfo.getVersion());

        return taskPlanInfoDTO;
    }

    public static EsbPlanInfoV3DTO toEsbPlanInfoV3(TaskPlanInfoDTO taskPlanInfo) {
        if (taskPlanInfo == null) {
            return null;
        }
        EsbPlanInfoV3DTO esbPlanInfo = new EsbPlanInfoV3DTO();
        esbPlanInfo.setAppId(taskPlanInfo.getAppId());
        esbPlanInfo.setId(taskPlanInfo.getId());
        esbPlanInfo.setTemplateId(taskPlanInfo.getTemplateId());
        esbPlanInfo.setName(taskPlanInfo.getName());
        esbPlanInfo.setCreator(taskPlanInfo.getCreator());
        esbPlanInfo.setCreateTime(taskPlanInfo.getCreateTime());
        esbPlanInfo.setLastModifyUser(taskPlanInfo.getLastModifyUser());
        esbPlanInfo.setLastModifyTime(taskPlanInfo.getLastModifyTime());
        if (CollectionUtils.isNotEmpty(taskPlanInfo.getVariableList())) {
            esbPlanInfo.setGlobalVarList(taskPlanInfo.getVariableList().parallelStream()
                .map(TaskVariableDTO::toEsbGlobalVarV3).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(taskPlanInfo.getStepList())) {
            esbPlanInfo.setStepList(taskPlanInfo.getStepList().parallelStream()
                .map(TaskStepDTO::toEsbStepV3).collect(Collectors.toList()));
        }
        return esbPlanInfo;
    }

    public boolean validate() {
        if (templateId == null || templateId <= 0) {
            return false;
        }
        if (appId == null || appId <= 0) {
            return false;
        }
        if (StringUtils.isBlank(name)) {
            return false;
        }
        if (CollectionUtils.isEmpty(stepList)) {
            return false;
        }
        return true;
    }
}
