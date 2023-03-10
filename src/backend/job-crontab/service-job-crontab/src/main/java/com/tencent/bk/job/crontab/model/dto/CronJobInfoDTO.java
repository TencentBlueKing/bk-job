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

package com.tencent.bk.job.crontab.model.dto;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.util.EsbDTOAppScopeMappingHelper;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.model.dto.ResourceScope;
import com.tencent.bk.job.common.model.dto.UserRoleInfoDTO;
import com.tencent.bk.job.common.service.AppScopeMappingService;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.crontab.model.CronJobCreateUpdateReq;
import com.tencent.bk.job.crontab.model.CronJobVO;
import com.tencent.bk.job.crontab.model.esb.response.EsbCronInfoResponse;
import com.tencent.bk.job.crontab.model.esb.v3.response.EsbCronInfoV3DTO;
import com.tencent.bk.job.crontab.model.inner.ServiceCronJobDTO;
import com.tencent.bk.job.crontab.service.TaskPlanService;
import com.tencent.bk.job.crontab.util.CronExpressionUtil;
import com.tencent.bk.job.manage.common.consts.notify.NotifyConsts;
import com.tencent.bk.job.manage.model.inner.ServiceTaskPlanDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTemplateNotificationDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.quartz.CronExpression;

import java.text.ParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @since 23/12/2019 21:03
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class CronJobInfoDTO {
    /**
     * 定时任务 ID
     */
    private Long id;

    /**
     * 业务 ID
     */
    private Long appId;

    /**
     * 定时任务名称
     */
    private String name;

    /**
     * 创建人
     */
    private String creator;

    /**
     * 作业模版 ID
     */
    private Long taskTemplateId;

    /**
     * 执行方案 ID
     */
    private Long taskPlanId;

    /**
     * 脚本 ID
     */
    private String scriptId;

    /**
     * 脚本版本 ID
     */
    private Long scriptVersionId;

    /**
     * 周期执行表达式
     */
    private String cronExpression;

    /**
     * 单次执行时间
     */
    private Long executeTime;

    /**
     * 变量值列表
     */
    private List<CronJobVariableDTO> variableValue;

    /**
     * 上次执行状态
     */
    private Integer lastExecuteStatus;

    /**
     * 上次执行错误码
     */
    private Long lastExecuteErrorCode;

    /**
     * 上次执行错误次数
     */
    private Integer lastExecuteErrorCount;

    /**
     * 是否启用
     */
    private Boolean enable;

    /**
     * 是否删除
     */
    private Boolean delete;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 最后修改人
     */
    private String lastModifyUser;

    /**
     * 最后修改时间
     */
    private Long lastModifyTime;

    /**
     * 通知提前时间
     */
    private Long notifyOffset = 0L;

    /**
     * 通知接收人列表
     */
    private UserRoleInfoDTO notifyUser = new UserRoleInfoDTO();

    /**
     * 通知渠道列表
     */
    private List<String> notifyChannel = Collections.emptyList();

    /**
     * 周期执行结束时间
     */
    private Long endTime = 0L;

    public static CronJobVO toVO(CronJobInfoDTO cronJobInfo) {
        if (cronJobInfo == null) {
            return null;
        }
        CronJobVO cronJobVO = new CronJobVO();
        cronJobVO.setId(cronJobInfo.getId());
        AppScopeMappingService appScopeMappingService =
            ApplicationContextRegister.getBean(AppScopeMappingService.class);
        ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(cronJobInfo.getAppId());
        cronJobVO.setScopeType(resourceScope.getType().getValue());
        cronJobVO.setScopeId(resourceScope.getId());
        cronJobVO.setName(cronJobInfo.getName());
        cronJobVO.setCreator(cronJobInfo.getCreator());
        cronJobVO.setCreateTime(cronJobInfo.getCreateTime());
        cronJobVO.setTaskTemplateId(cronJobInfo.getTaskTemplateId());
        cronJobVO.setTaskPlanId(cronJobInfo.getTaskPlanId());
        cronJobVO.setScriptId(cronJobInfo.getScriptId());
        cronJobVO.setScriptVersionId(cronJobInfo.getScriptVersionId());
        if (StringUtils.isNotBlank(cronJobInfo.getCronExpression())) {
            cronJobVO.setCronExpression(CronExpressionUtil.fixExpressionForUser(cronJobInfo.getCronExpression()));
        } else {
            cronJobVO.setCronExpression(null);
        }
        cronJobVO.setExecuteTime(cronJobInfo.getExecuteTime());
        if (CollectionUtils.isNotEmpty(cronJobInfo.getVariableValue())) {
            cronJobVO.setVariableValue(cronJobInfo.getVariableValue().parallelStream().map(CronJobVariableDTO::toVO)
                .collect(Collectors.toList()));
        } else {
            cronJobVO.setVariableValue(Collections.emptyList());
        }
        cronJobVO.setLastExecuteStatus(cronJobInfo.getLastExecuteStatus());
        cronJobVO.setLastExecuteErrorCode(cronJobInfo.getLastExecuteErrorCode());
        cronJobVO.setLastExecuteErrorCount(cronJobInfo.getLastExecuteErrorCount());
        cronJobVO.setEnable(cronJobInfo.getEnable());
        cronJobVO.setLastModifyUser(cronJobInfo.getLastModifyUser());
        cronJobVO.setLastModifyTime(cronJobInfo.getLastModifyTime());
        cronJobVO.setNotifyOffset(cronJobInfo.getNotifyOffset() / 60L);
        cronJobVO.setNotifyUser(UserRoleInfoDTO.toVO(cronJobInfo.getNotifyUser()));
        cronJobVO.setNotifyChannel(cronJobInfo.getNotifyChannel());
        cronJobVO.setEndTime(cronJobInfo.getEndTime());
        return cronJobVO;
    }

    public static CronJobVO toBasicVO(CronJobInfoDTO cronJobInfo) {
        if (cronJobInfo == null) {
            return null;
        }
        CronJobVO cronJobVO = new CronJobVO();
        cronJobVO.setId(cronJobInfo.getId());
        AppScopeMappingService appScopeMappingService =
            ApplicationContextRegister.getBean(AppScopeMappingService.class);
        ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(cronJobInfo.getAppId());
        cronJobVO.setScopeType(resourceScope.getType().getValue());
        cronJobVO.setScopeId(resourceScope.getId());
        cronJobVO.setName(cronJobInfo.getName());
        cronJobVO.setTaskTemplateId(cronJobInfo.getTaskTemplateId());
        cronJobVO.setTaskPlanId(cronJobInfo.getTaskPlanId());
        cronJobVO.setEnable(cronJobInfo.getEnable());
        if (CollectionUtils.isNotEmpty(cronJobInfo.getVariableValue())) {
            cronJobVO.setVariableValue(cronJobInfo.getVariableValue().parallelStream().map(CronJobVariableDTO::toVO)
                .collect(Collectors.toList()));
        } else {
            cronJobVO.setVariableValue(Collections.emptyList());
        }
        cronJobVO.setLastExecuteStatus(cronJobInfo.getLastExecuteStatus());
        return cronJobVO;
    }

    public static CronJobInfoDTO fromReq(String username, Long appId, CronJobCreateUpdateReq cronJobCreateUpdateReq) {
        if (cronJobCreateUpdateReq == null) {
            return null;
        }
        CronJobInfoDTO cronJobInfo = new CronJobInfoDTO();
        cronJobInfo.setId(cronJobCreateUpdateReq.getId());
        cronJobInfo.setAppId(appId);
        cronJobInfo.setName(cronJobCreateUpdateReq.getName());
        if (cronJobCreateUpdateReq.getId() == null || cronJobCreateUpdateReq.getId() == 0) {
            cronJobInfo.setCreator(username);
        } else {
            cronJobInfo.setCreator(null);
        }
        cronJobInfo.setTaskTemplateId(cronJobCreateUpdateReq.getTaskTemplateId());
        cronJobInfo.setTaskPlanId(cronJobCreateUpdateReq.getTaskPlanId());
        cronJobInfo.setScriptId(cronJobCreateUpdateReq.getScriptId());
        cronJobInfo.setScriptVersionId(cronJobCreateUpdateReq.getScriptVersionId());
        if (StringUtils.isNotBlank(cronJobCreateUpdateReq.getCronExpression())) {
            cronJobInfo.setCronExpression(cronJobCreateUpdateReq.getCronExpression());
            cronJobInfo.setExecuteTime(null);
        } else {
            cronJobInfo.setCronExpression(null);
            cronJobInfo.setExecuteTime(cronJobCreateUpdateReq.getExecuteTime());
        }
        if (CollectionUtils.isNotEmpty(cronJobCreateUpdateReq.getVariableValue())) {
            cronJobInfo.setVariableValue(cronJobCreateUpdateReq.getVariableValue().parallelStream()
                .map(CronJobVariableDTO::fromVO).collect(Collectors.toList()));
        } else {
            cronJobInfo.setVariableValue(null);
        }
        cronJobInfo.setLastModifyUser(username);
        cronJobInfo.setLastModifyTime(DateUtils.currentTimeSeconds());
        cronJobInfo.setEnable(cronJobCreateUpdateReq.getEnable());
        cronJobInfo.setDelete(false);
        if (cronJobCreateUpdateReq.getNotifyOffset() != null) {
            cronJobInfo.setNotifyOffset(cronJobCreateUpdateReq.getNotifyOffset() * 60L);
        }
        cronJobInfo.setNotifyUser(UserRoleInfoDTO.fromVO(cronJobCreateUpdateReq.getNotifyUser()));
        cronJobInfo.setNotifyChannel(cronJobCreateUpdateReq.getNotifyChannel());
        cronJobInfo.setEndTime(cronJobCreateUpdateReq.getEndTime());
        return cronJobInfo;
    }

    public static EsbCronInfoResponse toEsbCronInfo(CronJobInfoDTO cronJobInfoDTO) {
        if (cronJobInfoDTO == null) {
            return null;
        }
        EsbCronInfoResponse esbCronInfoResponse = new EsbCronInfoResponse();
        esbCronInfoResponse.setId(cronJobInfoDTO.getId());
        EsbDTOAppScopeMappingHelper.fillEsbAppScopeDTOByAppId(cronJobInfoDTO.getAppId(), esbCronInfoResponse);
        esbCronInfoResponse.setPlanId(cronJobInfoDTO.getTaskPlanId());
        esbCronInfoResponse.setName(cronJobInfoDTO.getName());
        esbCronInfoResponse.setStatus(cronJobInfoDTO.getEnable() ? 1 : 2);
        if (StringUtils.isNotBlank(cronJobInfoDTO.getCronExpression())) {
            esbCronInfoResponse.setCronExpression(
                CronExpressionUtil.fixExpressionForUser(cronJobInfoDTO.getCronExpression()));
        }
        esbCronInfoResponse.setCreator(cronJobInfoDTO.getCreator());
        esbCronInfoResponse
            .setCreateTime(DateUtils.formatUnixTimestamp(cronJobInfoDTO.getCreateTime(), ChronoUnit.SECONDS));
        esbCronInfoResponse.setLastModifyUser(cronJobInfoDTO.getLastModifyUser());
        esbCronInfoResponse
            .setLastModifyTime(DateUtils.formatUnixTimestamp(cronJobInfoDTO.getLastModifyTime(), ChronoUnit.SECONDS));
        return esbCronInfoResponse;
    }

    public static EsbCronInfoV3DTO toEsbCronInfoV3Response(CronJobInfoDTO cronJobInfoDTO) {
        if (cronJobInfoDTO == null) {
            return null;
        }
        EsbCronInfoV3DTO esbCronInfoV3DTO = new EsbCronInfoV3DTO();
        esbCronInfoV3DTO.setId(cronJobInfoDTO.getId());

        EsbDTOAppScopeMappingHelper.fillEsbAppScopeDTOByAppId(cronJobInfoDTO.getAppId(),
            esbCronInfoV3DTO);

        esbCronInfoV3DTO.setPlanId(cronJobInfoDTO.getTaskPlanId());
        esbCronInfoV3DTO.setName(cronJobInfoDTO.getName());
        esbCronInfoV3DTO.setStatus(cronJobInfoDTO.getEnable() ? 1 : 2);
        if (StringUtils.isNotBlank(cronJobInfoDTO.getCronExpression())) {
            esbCronInfoV3DTO.setCronExpression(
                CronExpressionUtil.fixExpressionForUser(cronJobInfoDTO.getCronExpression()));
        }
        List<CronJobVariableDTO> variableValue = cronJobInfoDTO.getVariableValue();
        if (variableValue != null) {
            esbCronInfoV3DTO.setGlobalVarList(
                variableValue.stream()
                    .map(CronJobVariableDTO::toEsbGlobalVarV3)
                    .collect(Collectors.toList())
            );
        }
        esbCronInfoV3DTO.setCreator(cronJobInfoDTO.getCreator());
        esbCronInfoV3DTO
            .setCreateTime(cronJobInfoDTO.getCreateTime());
        esbCronInfoV3DTO.setLastModifyUser(cronJobInfoDTO.getLastModifyUser());
        esbCronInfoV3DTO
            .setLastModifyTime(cronJobInfoDTO.getLastModifyTime());
        return esbCronInfoV3DTO;
    }

    private static void addTaskBaseInfoToVarMap(Map<String, String> variableMap, CronJobInfoDTO cronJobInfo) {
        variableMap.put("task.id", cronJobInfo.getId().toString());
        variableMap.put("task.name", cronJobInfo.getName());
        variableMap.put("cron_name", cronJobInfo.getName());
        String notifyTimeStr = String.valueOf(cronJobInfo.getNotifyOffset() / 60);
        variableMap.put("notify_time", notifyTimeStr);
        variableMap.put("task.cron.notify_time", notifyTimeStr);
        variableMap.put("cron_updater", cronJobInfo.getLastModifyUser());
        variableMap.put("task.operator", cronJobInfo.getLastModifyUser());
        variableMap.put("task.type", "定时任务");
        variableMap.put("task.cron.plan_id", cronJobInfo.getTaskPlanId().toString());
        TaskPlanService taskPlanService = ApplicationContextRegister.getBean(TaskPlanService.class);
        ServiceTaskPlanDTO serviceTaskPlanDTO =
            taskPlanService.getPlanBasicInfoById(cronJobInfo.getAppId(), cronJobInfo.getTaskPlanId());
        if (serviceTaskPlanDTO == null) {
            throw new InternalException(ErrorCode.INTERNAL_ERROR);
        }
        variableMap.put("task.cron.plan_name", serviceTaskPlanDTO.getName());
    }

    private static void addTaskUrlToVarMap(Map<String, String> variableMap, CronJobInfoDTO cronJobInfo) {
        AppScopeMappingService appScopeMappingService =
            ApplicationContextRegister.getBean(AppScopeMappingService.class);
        ResourceScope resourceScope = appScopeMappingService.getScopeByAppId(cronJobInfo.getAppId());
        String cronUri = "/" + resourceScope.getType().getValue() + "/" + resourceScope.getId() +
            "/cron/list?cronJobId=" + cronJobInfo.getId() + "&mode=detail";
        variableMap.put("task.url", "{{BASE_HOST}}" + cronUri);
        variableMap.put("cron_uri", cronUri);
    }

    public static ServiceTemplateNotificationDTO buildNotifyInfo(CronJobInfoDTO cronJobInfo) {
        ServiceTemplateNotificationDTO notifyInfo = new ServiceTemplateNotificationDTO();
        notifyInfo.setTriggerUser(cronJobInfo.getLastModifyUser());
        notifyInfo.setAppId(cronJobInfo.getAppId());
        notifyInfo.setReceiverInfo(cronJobInfo.getNotifyUser());
        notifyInfo.setActiveChannels(cronJobInfo.getNotifyChannel());

        Map<String, String> variableMap = new HashMap<>(8);
        // 添加任务基础信息
        addTaskBaseInfoToVarMap(variableMap, cronJobInfo);
        // 添加任务链接
        addTaskUrlToVarMap(variableMap, cronJobInfo);

        long triggerTime;
        if (StringUtils.isNotBlank(cronJobInfo.getCronExpression())) {
            // 结束前通知
            notifyInfo.setTemplateCode(NotifyConsts.NOTIFY_TEMPLATE_CODE_BEFORE_CRON_JOB_END);
            variableMap.put("cron_type", "周期执行");
            String cronRuleStr = cronJobInfo.getCronExpression().substring(2).replace("?", "*");
            variableMap.put("cron_rule", cronRuleStr);
            triggerTime = cronJobInfo.getEndTime() - cronJobInfo.getNotifyOffset();
            variableMap.put("task.cron.repeat_freq", "周期执行");
            variableMap.put("task.cron.time_set", cronRuleStr);
            variableMap.put("task.start_time", null);
        } else {
            // 执行前通知
            notifyInfo.setTemplateCode(NotifyConsts.NOTIFY_TEMPLATE_CODE_BEFORE_CRON_JOB_EXECUTE);
            variableMap.put("cron_type", "单次执行");
            variableMap.put("cron_rule",
                DateUtils.formatUnixTimestampWithZone(cronJobInfo.getExecuteTime(), ChronoUnit.SECONDS));
            triggerTime = cronJobInfo.getExecuteTime() - cronJobInfo.getNotifyOffset();
            String executeTimeStr =
                DateUtils.formatUnixTimestampWithZone(cronJobInfo.getExecuteTime(), ChronoUnit.SECONDS);
            variableMap.put("task.cron.repeat_freq", "单次执行");
            variableMap.put("task.cron.time_set", executeTimeStr);
            variableMap.put("task.start_time", executeTimeStr);
        }
        variableMap.put("triggerTime", DateUtils.formatUnixTimestampWithZone(triggerTime, ChronoUnit.SECONDS));
        notifyInfo.setVariablesMap(variableMap);
        return notifyInfo;
    }

    public static ServiceTemplateNotificationDTO buildFailedNotifyInfo(CronJobInfoDTO cronJobInfo) {
        ServiceTemplateNotificationDTO notifyInfo = new ServiceTemplateNotificationDTO();
        notifyInfo.setTriggerUser(cronJobInfo.getLastModifyUser());
        notifyInfo.setAppId(cronJobInfo.getAppId());

        UserRoleInfoDTO userRoleInfo = new UserRoleInfoDTO();
        userRoleInfo.setUserList(new ArrayList<>());
        userRoleInfo.getUserList().add(cronJobInfo.getLastModifyUser());

        notifyInfo.setReceiverInfo(userRoleInfo);
        notifyInfo.setActiveChannels(new ArrayList<>());
        notifyInfo.getActiveChannels().add("weixin");
        notifyInfo.getActiveChannels().add("rtx");
        notifyInfo.getActiveChannels().add("mail");
        notifyInfo.getActiveChannels().add("sms");

        notifyInfo.setTemplateCode(NotifyConsts.NOTIFY_TEMPLATE_CODE_CRON_EXECUTE_FAILED);

        Map<String, String> variableMap = new HashMap<>(8);
        // 添加任务基础信息
        addTaskBaseInfoToVarMap(variableMap, cronJobInfo);
        // 添加任务链接
        addTaskUrlToVarMap(variableMap, cronJobInfo);

        if (StringUtils.isNotBlank(cronJobInfo.getCronExpression())) {
            // 结束前通知
            variableMap.put("task.cron.type", "周期执行");
            String cronRuleStr = cronJobInfo.getCronExpression().substring(2).replace("?", "*");
            variableMap.put("task.cron.rule", cronRuleStr);
            variableMap.put("task.cron.repeat_freq", "周期执行");
            variableMap.put("task.cron.time_set", cronRuleStr);
            variableMap.put("task.start_time", null);
        } else {
            // 执行前通知
            variableMap.put("task.cron.type", "单次执行");
            variableMap.put("task.cron.rule",
                DateUtils.formatUnixTimestampWithZone(cronJobInfo.getExecuteTime(), ChronoUnit.SECONDS));
            String executeTimeStr =
                DateUtils.formatUnixTimestampWithZone(cronJobInfo.getExecuteTime(), ChronoUnit.SECONDS);
            variableMap.put("task.cron.repeat_freq", "单次执行");
            variableMap.put("task.cron.time_set", executeTimeStr);
            variableMap.put("task.start_time", executeTimeStr);
        }

        variableMap.put("task.cron.trigger_time",
            DateUtils.formatUnixTimestampWithZone(DateUtils.currentTimeSeconds(), ChronoUnit.SECONDS));
        notifyInfo.setVariablesMap(variableMap);
        return notifyInfo;
    }

    public static EsbCronInfoV3DTO toEsbCronInfoV3(CronJobInfoDTO cronJobInfoDTO) {
        if (cronJobInfoDTO == null) {
            return null;
        }
        EsbCronInfoV3DTO esbCronInfoResponse = new EsbCronInfoV3DTO();
        esbCronInfoResponse.setId(cronJobInfoDTO.getId());

        EsbDTOAppScopeMappingHelper.fillEsbAppScopeDTOByAppId(cronJobInfoDTO.getAppId(),
            esbCronInfoResponse);

        esbCronInfoResponse.setPlanId(cronJobInfoDTO.getTaskPlanId());
        esbCronInfoResponse.setName(cronJobInfoDTO.getName());
        esbCronInfoResponse.setStatus(cronJobInfoDTO.getEnable() ? 1 : 0);
        if (StringUtils.isNotBlank(cronJobInfoDTO.getCronExpression())) {
            esbCronInfoResponse.setCronExpression(
                cronJobInfoDTO.getCronExpression().substring(2).replace("?", "*"));
        }
        esbCronInfoResponse.setCreator(cronJobInfoDTO.getCreator());
        esbCronInfoResponse.setCreateTime(cronJobInfoDTO.getCreateTime());
        esbCronInfoResponse.setLastModifyUser(cronJobInfoDTO.getLastModifyUser());
        esbCronInfoResponse.setLastModifyTime(cronJobInfoDTO.getLastModifyTime());
        if (CollectionUtils.isNotEmpty(cronJobInfoDTO.getVariableValue())) {
            esbCronInfoResponse.setGlobalVarList(cronJobInfoDTO.getVariableValue().parallelStream()
                .map(CronJobVariableDTO::toEsbGlobalVarV3).collect(Collectors.toList()));
        }
        return esbCronInfoResponse;
    }

    public boolean validate() {
        if (notifyOffset == null || notifyOffset <= 0) {
            notifyOffset = 0L;
            notifyUser = new UserRoleInfoDTO();
            notifyChannel = Collections.emptyList();
        } else {
            if (!notifyUser.validate()) {
                JobContextUtil.addDebugMessage("Empty notify user or role!");
                // 1.指定了notifyOffset，但是未指定有效的notifyUser
                return false;
            }
            if (CollectionUtils.isEmpty(notifyChannel)) {
                JobContextUtil.addDebugMessage("Empty notify channel!");
                // 2.指定了notifyOffset，但是未指定有效的notifyChannel
                return false;
            }
        }
        if (endTime == null || endTime <= 0) {
            endTime = 0L;
        }
        if (enable == null) {
            enable = false;
        }
        if (taskPlanId != null && taskPlanId > 0) {
            scriptId = null;
            scriptVersionId = null;
        } else if (StringUtils.isNotBlank(scriptId) && scriptVersionId != null && scriptVersionId > 0) {
            taskTemplateId = null;
            taskPlanId = null;
        } else {
            JobContextUtil.addDebugMessage("Missing execute plan/script info!");
            // 3.脚本与执行方案都没指定或无效
            return false;
        }

        return validateCronExpression();
    }

    private boolean validateCronExpression() {
        if (StringUtils.isNotBlank(cronExpression)) {
            try {
                cronExpression = CronExpressionUtil.fixExpressionForQuartz(cronExpression);
                new CronExpression(cronExpression);
            } catch (IllegalArgumentException e) {
                JobContextUtil.addDebugMessage("Invalid cron expression!");
                JobContextUtil.addDebugMessage(e.getMessage());
                // 4.定时任务cron表达式不正确
                return false;
            } catch (ParseException e) {
                JobContextUtil.addDebugMessage("Invalid cron expression!");
                JobContextUtil.addDebugMessage(e.getErrorOffset() + "|" + e.getMessage());
                // 4.定时任务cron表达式不正确
                return false;
            }
            executeTime = null;
            if (endTime > 0) {
                if (endTime - notifyOffset <= DateUtils.currentTimeSeconds()) {
                    JobContextUtil.addDebugMessage("Invalid end time or notify time config!");
                    // 5.定时任务指定了结束时间但结束时间太早导致无法进行结束前通知
                    return false;
                }
            }
        } else if (executeTime != null && executeTime > DateUtils.currentTimeSeconds()) {
            cronExpression = null;
            endTime = 0L;
            if (executeTime - notifyOffset <= DateUtils.currentTimeSeconds()) {
                JobContextUtil.addDebugMessage("Invalid notify time config!");
                // 6.单次执行任务指定了执行前通知但执行时间太早导致无法进行执行前通知
                return false;
            }
        } else {
            // 7.定时执行/单次执行参数均未有效配置
            return false;
        }
        return true;
    }

    public ServiceCronJobDTO toServiceCronJobDTO() {
        ServiceCronJobDTO cronJob = new ServiceCronJobDTO();
        cronJob.setId(id);
        cronJob.setAppId(appId);
        cronJob.setName(name);
        cronJob.setCreator(creator);
        cronJob.setLastModifyTime(lastModifyTime);
        cronJob.setTaskPlanId(taskPlanId);
        cronJob.setScriptId(scriptId);
        cronJob.setScriptVersionId(scriptVersionId);
        cronJob.setCronExpression(cronExpression);
        cronJob.setExecuteTime(executeTime);
        if (CollectionUtils.isNotEmpty(variableValue)) {
            cronJob.setVariableValue(variableValue.stream().map(CronJobVariableDTO::toServiceCronJobVariableDTO)
                .collect(Collectors.toList()));
        } else {
            cronJob.setVariableValue(Collections.emptyList());
        }
        cronJob.setLastExecuteStatus(lastExecuteStatus);
        cronJob.setEnable(enable);
        cronJob.setLastModifyUser(lastModifyUser);
        cronJob.setLastModifyTime(lastModifyTime);
        return cronJob;
    }
}
