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

import com.tencent.bk.job.common.i18n.locale.LocaleUtils;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.dto.ApplicationInfoDTO;
import com.tencent.bk.job.common.model.dto.UserRoleInfoDTO;
import com.tencent.bk.job.common.util.JobContextUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.client.ServiceNotificationResourceClient;
import com.tencent.bk.job.execute.client.ServiceUserResourceClient;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.config.JobExecuteConfig;
import com.tencent.bk.job.execute.model.NotifyDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.model.TaskNotifyDTO;
import com.tencent.bk.job.execute.service.ApplicationService;
import com.tencent.bk.job.execute.service.NotifyService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.manage.common.consts.notify.ExecuteStatusEnum;
import com.tencent.bk.job.manage.common.consts.notify.NotifyConsts;
import com.tencent.bk.job.manage.common.consts.notify.ResourceTypeEnum;
import com.tencent.bk.job.manage.common.consts.notify.TriggerTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.model.inner.ServiceNotificationMessage;
import com.tencent.bk.job.manage.model.inner.ServiceNotificationTriggerDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTemplateNotificationDTO;
import com.tencent.bk.job.manage.model.inner.ServiceTriggerTemplateNotificationDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
public class NotifyServiceImpl implements NotifyService {
    private static final Properties templatePropsEN = new Properties();
    private static final Properties templatePropsZH = new Properties();

    static {
        loadNotifyTemplate();
    }

    private final JobExecuteConfig jobExecuteConfig;
    private final ServiceNotificationResourceClient notificationResourceClient;
    private final ServiceUserResourceClient userResourceClient;
    private final ApplicationService applicationService;
    private final TaskInstanceService taskInstanceService;
    private final MessageI18nService i18nService;

    @Autowired
    public NotifyServiceImpl(JobExecuteConfig jobExecuteConfig,
                             ServiceNotificationResourceClient notificationResourceClient,
                             ServiceUserResourceClient userResourceClient, ApplicationService applicationService,
                             TaskInstanceService taskInstanceService, MessageI18nService i18nService) {
        this.jobExecuteConfig = jobExecuteConfig;
        this.notificationResourceClient = notificationResourceClient;
        this.userResourceClient = userResourceClient;
        this.applicationService = applicationService;
        this.taskInstanceService = taskInstanceService;
        this.i18nService = i18nService;
    }

    private static void loadNotifyTemplate() {
        log.info("Load notification template!");
        try (InputStream inputStream =
                 new ClassPathResource("notification-template/notification_zh.properties").getInputStream();
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            templatePropsZH.load(reader);
        } catch (IOException e) {
            log.error("Fail to load notification template for ZH", e);
        }
        try (InputStream inputStream =
                 new ClassPathResource("notification-template/notification_en.properties").getInputStream();
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            templatePropsEN.load(reader);
        } catch (IOException e) {
            log.error("Fail to load notification template for EN", e);
        }
        log.info("Load notification template successfully!");
    }

    private static String formatCostTime(Long costInMills) {
        if (costInMills == null) {
            return "";
        }
        if (costInMills < 60_000) {
            String cost = "";
            if (costInMills < 1_000) {
                DecimalFormat df = new DecimalFormat("0.000");
                cost = df.format(costInMills / 1000.0d);
            } else {
                DecimalFormat df = new DecimalFormat("#.000");
                cost = df.format(costInMills / 1000.0d);
            }
            if (isChineseLocale()) {
                return cost + " 秒";
            } else {
                return cost + " seconds";
            }
        }

        if (costInMills > 60_000L && costInMills < 3_600_000L) {
            long minutes = costInMills / 60_000;
            long seconds = (costInMills - minutes * 60_000) / 1000;
            if (isChineseLocale()) {
                return minutes + "分 " + seconds + "秒";
            } else {
                return minutes + " minutes " + seconds + " seconds";
            }
        } else {
            long hours = costInMills / 3_600_000;
            long left = costInMills - hours * 3600_000;
            long minutes = left / 60_000;
            long seconds = (left - minutes * 60_000) / 1000;
            if (isChineseLocale()) {
                return hours + "小时 " + minutes + "分 " + seconds + "秒";
            } else {
                return hours + " hours " + minutes + " minutes " + seconds + " seconds";
            }
        }
    }

    private static boolean isChineseLocale() {
//        Locale locale = LocaleContextHolder.getLocale();
//        log.info("locale={}", locale);
//        return (locale == null || locale.equals(Locale.SIMPLIFIED_CHINESE) || locale.equals(Locale.CHINA));
        return true;
    }

    @Override
    public void notifyTaskFail(TaskNotifyDTO taskNotifyDTO) {
        notifyCommon(taskNotifyDTO, ExecuteStatusEnum.FAIL);
    }

    private ServiceNotificationTriggerDTO buildQueryTrigger(TaskNotifyDTO taskNotifyDTO) {
        ServiceNotificationTriggerDTO trigger = new ServiceNotificationTriggerDTO();
        trigger.setAppId(taskNotifyDTO.getAppId());
        trigger.setTriggerUser(taskNotifyDTO.getOperator());
        Integer startupMode = taskNotifyDTO.getStartupMode();
        if (startupMode.equals(TaskStartupModeEnum.NORMAL.getValue())) {
            trigger.setTriggerType(TriggerTypeEnum.PAGE_EXECUTE.getType());
        } else if (startupMode.equals(TaskStartupModeEnum.API.getValue())) {
            trigger.setTriggerType(TriggerTypeEnum.API_INVOKE.getType());
        } else if (startupMode.equals(TaskStartupModeEnum.CRON.getValue())) {
            trigger.setTriggerType(TriggerTypeEnum.TIMER_TASK.getType());
        } else {
            log.warn("Invalid startup mode!");
        }
        trigger.setResourceExecuteStatus(taskNotifyDTO.getResourceExecuteStatus());
        trigger.setResourceType(taskNotifyDTO.getResourceType());
        trigger.setResourceId(taskNotifyDTO.getResourceId());
        return trigger;
    }

    private String buildContentTemplateKey(String executeStatusKey, String channelKey) {
        return "task." + executeStatusKey + ".msg.content.template." + channelKey;
    }

    private String buildTitleTemplateKey(String executeStatusKey, String channelKey) {
        return "task." + executeStatusKey + ".msg.title.template." + channelKey;
    }

    private String getExecuteStatusKey(ExecuteStatusEnum executeStatus) {
        String executeStatusKey = "";
        if (executeStatus == ExecuteStatusEnum.FAIL) {
            executeStatusKey = "fail";
        } else if (executeStatus == ExecuteStatusEnum.SUCCESS) {
            executeStatusKey = "success";
        } else if (executeStatus == ExecuteStatusEnum.READY) {
            executeStatusKey = "waiting";
        }
        return executeStatusKey;
    }

    private String getChannelKey(String channel) {
        String channelKey = "";
        if (StringUtils.isEmpty(channel)) {
            return "common";
        }
        if (channel.equalsIgnoreCase("weixin")) {
            channelKey = "weixin";
        } else if (channel.equalsIgnoreCase("work-weixin")) {
            channelKey = "weixin";
        } else if (channel.equalsIgnoreCase("sms")) {
            channelKey = "sms";
        } else if (channel.equalsIgnoreCase("mail")) {
            channelKey = "mail";
        } else {
            channelKey = "common";
        }
        return channelKey;
    }

    private String replaceTemplatePlaceHolder(String template, Map<String, String> replacements) {
        String result = template;
        for (Map.Entry<String, String> replacement : replacements.entrySet()) {
            result = result.replaceAll("\\{" + replacement.getKey() + "}", replacement.getValue());
        }
        return result;
    }

    private String getTemplate(String key) {
        if (isChineseLocale()) {
            return templatePropsZH.getProperty(key);
        } else {
            return templatePropsEN.getProperty(key);
        }
    }

    private String buildJobExecuteDetailUrl(Long taskInstanceId) {
        return jobExecuteConfig.getJobWebUrl() + "/api_execute/" + taskInstanceId;
    }

    @Override
    public void notifyTaskSuccess(TaskNotifyDTO taskNotifyDTO) {
        notifyCommon(taskNotifyDTO, ExecuteStatusEnum.SUCCESS);
    }

    private String getTemplateCodeByExecuteStatus(ExecuteStatusEnum executeStatus) {
        if (executeStatus == ExecuteStatusEnum.SUCCESS) {
            return NotifyConsts.NOTIFY_TEMPLATE_CODE_EXECUTE_SUCCESS;
        } else if (executeStatus == ExecuteStatusEnum.FAIL) {
            return NotifyConsts.NOTIFY_TEMPLATE_CODE_EXECUTE_FAILURE;
        } else if (executeStatus == ExecuteStatusEnum.READY) {
            return NotifyConsts.NOTIFY_TEMPLATE_CODE_CONFIRMATION;
        } else {
            log.error("Not supported executeStatus:{}", executeStatus.name());
            return null;
        }
    }

    private void notifyCommon(TaskNotifyDTO taskNotifyDTO, ExecuteStatusEnum executeStatus) {
        ServiceNotificationTriggerDTO trigger = buildQueryTrigger(taskNotifyDTO);
        ServiceTriggerTemplateNotificationDTO triggerTemplateNotificationDTO =
            new ServiceTriggerTemplateNotificationDTO();
        triggerTemplateNotificationDTO.setTriggerDTO(trigger);
        triggerTemplateNotificationDTO.setTemplateCode(getTemplateCodeByExecuteStatus(executeStatus));
        triggerTemplateNotificationDTO.setVariablesMap(getTemplateVariablesMap(taskNotifyDTO, executeStatus));
        notificationResourceClient.triggerTemplateNotification(triggerTemplateNotificationDTO);
    }

    private Map<String, String> getTemplateVariablesMap(TaskNotifyDTO taskNotifyDTO, ExecuteStatusEnum executeStatus) {
        Map<String, String> variablesMap = new HashMap<>();
        variablesMap.put("task.id", taskNotifyDTO.getTaskInstanceId().toString());
        variablesMap.put("task.name", taskNotifyDTO.getTaskInstanceName());
        variablesMap.put("task.app.name", applicationService.getAppById(taskNotifyDTO.getAppId()).getName());
        variablesMap.put("task.app.id", String.valueOf(taskNotifyDTO.getAppId()));
        String detailUrl = buildJobExecuteDetailUrl(taskNotifyDTO.getTaskInstanceId());
        variablesMap.put("task.detail.url", detailUrl);
        variablesMap.put("task.url", detailUrl);
        TaskInstanceDTO taskInstanceDTO = taskInstanceService.getTaskInstance(taskNotifyDTO.getTaskInstanceId());
        variablesMap.put("task.start_time", DateUtils.formatUnixTimestampWithZone(taskInstanceDTO.getStartTime(),
            ChronoUnit.MILLIS));
        List<Long> stepIdList = taskInstanceService.getTaskStepIdList(taskInstanceDTO.getId());
        variablesMap.put("task.step.total_seq_cnt", "" + stepIdList.size());
        Long currentStepId = taskInstanceDTO.getCurrentStepId();
        variablesMap.put("task.step.current_seq_id", "" + (stepIdList.indexOf(currentStepId) + 1));
        StepInstanceDTO stepInstanceDTO = taskInstanceService.getStepInstanceDetail(currentStepId);
        if (executeStatus == ExecuteStatusEnum.FAIL || executeStatus == ExecuteStatusEnum.SUCCESS) {
            if (stepInstanceDTO.getTotalTime() != null) {
                variablesMap.put("task.step.duration", "" + stepInstanceDTO.getTotalTime() / 1000.0);
            }
            if (taskInstanceDTO.getTotalTime() != null) {
                variablesMap.put("task.total_duration", "" + taskInstanceDTO.getTotalTime() / 1000.0);
            }
            variablesMap.put("task.step.failed_cnt", "" + stepInstanceDTO.getFailIPNum());
            variablesMap.put("task.step.success_cnt", "" + stepInstanceDTO.getSuccessIPNum());
        }
        // 国际化处理
        Long appId = taskNotifyDTO.getAppId();
        ApplicationInfoDTO applicationInfoDTO = applicationService.getAppById(appId);
        String userLang = JobContextUtil.getUserLang();
        if (userLang == null) {
            String appLang = applicationInfoDTO.getLanguage();
            if ("1".equals(appLang)) {
                userLang = LocaleUtils.LANG_ZH_CN;
            } else if ("2".equals(appLang)) {
                userLang = LocaleUtils.LANG_EN_US;
            } else {
                log.warn("appLang=null, use zh_CN, appId={}", appId);
                userLang = LocaleUtils.LANG_ZH_CN;
            }
        }
        Locale locale;
        if (userLang.equals(LocaleUtils.LANG_ZH_CN) || userLang.equals(LocaleUtils.LANG_ZH)) {
            locale = Locale.CHINA;
        } else {
            locale = Locale.ENGLISH;
        }
        if (taskNotifyDTO.getResourceType() == ResourceTypeEnum.SCRIPT.getType()) {
            variablesMap.put("task.type", i18nService.getI18n("task.type.name.fast_execute_script", locale));
        } else if (taskNotifyDTO.getResourceType() == ResourceTypeEnum.FILE.getType()) {
            variablesMap.put("task.type", i18nService.getI18n("task.type.name.fast_push_file", locale));
        } else if (taskNotifyDTO.getResourceType() == ResourceTypeEnum.JOB.getType()) {
            variablesMap.put("task.type", i18nService.getI18n("task.type.name.job", locale));
        } else {
            variablesMap.put("task.type", "Unknown");
            log.error("Not supported resourceType:{}", taskNotifyDTO.getResourceType());
        }
        variablesMap.put("task.operator", taskNotifyDTO.getOperator());
        variablesMap.put("current.date", DateUtils.formatLocalDateTime(LocalDateTime.now(), "yyyy-MM-dd"));
        variablesMap.put("task.step.name", stepInstanceDTO.getName());
        if (stepInstanceDTO.getStepType().equals(TaskStepTypeEnum.SCRIPT.getType())) {
            variablesMap.put("task.step.type", i18nService.getI18n("task.step.type.name.script"));
        } else if (stepInstanceDTO.getStepType().equals(TaskStepTypeEnum.FILE.getType())) {
            variablesMap.put("task.step.type", i18nService.getI18n("task.step.type.name.file"));
        } else if (stepInstanceDTO.getStepType().equals(TaskStepTypeEnum.APPROVAL.getType())) {
            variablesMap.put("task.step.type", i18nService.getI18n("task.step.type.name.manual_confirm"));
        }
        if (executeStatus == ExecuteStatusEnum.SUCCESS) {
            variablesMap.put("task.cost", formatCostTime(taskNotifyDTO.getCost()));
        } else if (executeStatus == ExecuteStatusEnum.READY) {
            variablesMap.put("confirm.message", String.valueOf(taskNotifyDTO.getConfirmMessage()));
            variablesMap.put("task.step.confirm_info", String.valueOf(taskNotifyDTO.getConfirmMessage()));
            Set<String> receiverSet = new HashSet<>(taskNotifyDTO.getNotifyDTO().getReceiverUsers());
            receiverSet.addAll(userResourceClient.getUsersByRoles(
                appId,
                taskNotifyDTO.getOperator(),
                taskNotifyDTO.getResourceType(),
                taskNotifyDTO.getResourceId(),
                new HashSet<>(taskNotifyDTO.getNotifyDTO().getReceiverRoles())).getData()
            );
            variablesMap.put("task.step.confirmer", String.join(",", receiverSet));
        }
        return variablesMap;
    }

    private Map<String, ServiceNotificationMessage> buildNotificationMessage(ExecuteStatusEnum executeStatus,
                                                                             Collection<String> notifyChannels,
                                                                             Map<String, String> placeholders) {
        Map<String, ServiceNotificationMessage> messageMap = new HashMap<>();
        for (String notifyChannel : notifyChannels) {
            String executeStatusKey = getExecuteStatusKey(executeStatus);
            String channelKey = getChannelKey(notifyChannel);
            String contentTemplateKey = buildContentTemplateKey(executeStatusKey, channelKey);
            String titleTemplateKey = buildTitleTemplateKey(executeStatusKey, channelKey);
            String title = getTemplate(titleTemplateKey);
            String contentTemplate = getTemplate(contentTemplateKey);

            if (StringUtils.isBlank(contentTemplate)) {
                title = getTemplate(buildTitleTemplateKey(executeStatusKey, "common"));
                contentTemplate = getTemplate(buildContentTemplateKey(executeStatusKey, "common"));
            }

            String content = replaceTemplatePlaceHolder(contentTemplate, placeholders);

            ServiceNotificationMessage notificationMessage = new ServiceNotificationMessage(title, content);
            messageMap.put(notifyChannel, notificationMessage);
        }

        return messageMap;
    }

    @Override
    public void notifyTaskConfirm(TaskNotifyDTO taskNotifyDTO) {
        if (taskNotifyDTO.getNotifyDTO() != null) {
            NotifyDTO notifyDTO = taskNotifyDTO.getNotifyDTO();
            ServiceTemplateNotificationDTO serviceTemplateNotificationDTO = new ServiceTemplateNotificationDTO();
            serviceTemplateNotificationDTO.setAppId(taskNotifyDTO.getAppId());
            serviceTemplateNotificationDTO.setActiveChannels(notifyDTO.getChannels());
            serviceTemplateNotificationDTO.setTriggerUser(taskNotifyDTO.getOperator());
            serviceTemplateNotificationDTO.setResourceType(ResourceTypeEnum.JOB.getType());
            serviceTemplateNotificationDTO.setResourceId(taskNotifyDTO.getResourceId());
            UserRoleInfoDTO userRoleInfoDTO = new UserRoleInfoDTO();
            userRoleInfoDTO.setUserList(notifyDTO.getReceiverUsers());
            userRoleInfoDTO.setRoleList(notifyDTO.getReceiverRoles());
            serviceTemplateNotificationDTO.setReceiverInfo(userRoleInfoDTO);
            serviceTemplateNotificationDTO.setTemplateCode(NotifyConsts.NOTIFY_TEMPLATE_CODE_CONFIRMATION);
            Map<String, String> variablesMap = getTemplateVariablesMap(taskNotifyDTO, ExecuteStatusEnum.READY);
            serviceTemplateNotificationDTO.setVariablesMap(variablesMap);
            notificationResourceClient.sendTemplateNotification(serviceTemplateNotificationDTO);
        }
    }
}
