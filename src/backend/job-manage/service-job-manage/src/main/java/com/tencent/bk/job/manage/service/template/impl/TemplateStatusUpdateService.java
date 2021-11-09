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

package com.tencent.bk.job.manage.service.template.impl;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.manage.common.consts.JobResourceStatusEnum;
import com.tencent.bk.job.manage.dao.template.TaskTemplateDAO;
import com.tencent.bk.job.manage.model.dto.ScriptStatusUpdateMessageDTO;
import com.tencent.bk.job.manage.service.AbstractTaskStepService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record1;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.generated.tables.ScriptVersion;
import org.jooq.generated.tables.TaskTemplateStepScript;
import org.jooq.types.UByte;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TemplateStatusUpdateService {
    private final TaskTemplateDAO taskTemplateDAO;
    private static final TaskTemplateStepScript STEP_SCRIPT_TABLE = TaskTemplateStepScript.TASK_TEMPLATE_STEP_SCRIPT;
    private static final ScriptVersion SCRIPT_VERSION_TABLE = ScriptVersion.SCRIPT_VERSION;
    private static final LinkedBlockingQueue<ScriptStatusUpdateMessageDTO> UPDATE_MESSAGE_QUEUE =
        new LinkedBlockingQueue<>();
    private DSLContext context;
    @Autowired
    private TemplateStatusUpdateService templateStatusUpdateService;

    private AbstractTaskStepService taskStepService;

    @Autowired
    public TemplateStatusUpdateService(
        @Qualifier("job-manage-dsl-context") DSLContext context,
        @Qualifier("TaskTemplateStepServiceImpl") AbstractTaskStepService taskStepService,
        TaskTemplateDAO taskTemplateDAO
    ) {
        this.context = context;
        this.taskStepService = taskStepService;
        this.taskTemplateDAO = taskTemplateDAO;
        TemplateStatusUpdateThread templateStatusUpdateThread = new TemplateStatusUpdateThread();
        templateStatusUpdateThread.start();
    }

    public boolean offerMessage(long templateId) throws InterruptedException {
        return offerMessage(templateId, 10L, TimeUnit.SECONDS);
    }

    public boolean offerMessage(long templateId, long timeout, TimeUnit timeUnit) throws InterruptedException {
        ScriptStatusUpdateMessageDTO scriptStatusUpdateMessage = new ScriptStatusUpdateMessageDTO();
        scriptStatusUpdateMessage.setTemplateId(templateId);
        return UPDATE_MESSAGE_QUEUE.offer(scriptStatusUpdateMessage, timeout, timeUnit);
    }

    public boolean offerMessage(String scriptId, long scriptVersionId, JobResourceStatusEnum status)
        throws InterruptedException {
        return offerMessage(scriptId, scriptVersionId, status, 10L, TimeUnit.SECONDS);
    }

    public boolean offerMessage(String scriptId, long scriptVersionId, JobResourceStatusEnum status, long timeout,
                                TimeUnit timeUnit) throws InterruptedException {
        ScriptStatusUpdateMessageDTO scriptStatusUpdateMessage = new ScriptStatusUpdateMessageDTO();
        scriptStatusUpdateMessage.setScriptId(scriptId);
        scriptStatusUpdateMessage.setScriptVersionId(scriptVersionId);
        scriptStatusUpdateMessage.setStatus(status);
        return UPDATE_MESSAGE_QUEUE.offer(scriptStatusUpdateMessage, timeout, timeUnit);
    }

    void doUpdateStatus(String uuid, ScriptStatusUpdateMessageDTO scriptStatusUpdateMessage) {
        log.debug("{}|Start process script status update message...|{}", uuid, scriptStatusUpdateMessage);
        if (scriptStatusUpdateMessage.getTemplateId() != null && scriptStatusUpdateMessage.getTemplateId() > 0) {
            log.debug("Processing template status...|{}", scriptStatusUpdateMessage);
            templateStatusUpdateService.processTemplateStatus(ULong.valueOf(scriptStatusUpdateMessage.getTemplateId()));
        } else if (StringUtils.isNotBlank(scriptStatusUpdateMessage.getScriptId())
            && scriptStatusUpdateMessage.getScriptVersionId() != null
            && scriptStatusUpdateMessage.getScriptVersionId() > 0 && scriptStatusUpdateMessage.getStatus() != null) {

            List<Condition> conditions = new ArrayList<>();
            conditions.add(STEP_SCRIPT_TABLE.SCRIPT_ID.eq(scriptStatusUpdateMessage.getScriptId()));
            conditions.add(
                STEP_SCRIPT_TABLE.SCRIPT_VERSION_ID.eq(ULong.valueOf(scriptStatusUpdateMessage.getScriptVersionId())));

            Result<Record1<ULong>> records =
                context.select(STEP_SCRIPT_TABLE.TEMPLATE_ID).from(STEP_SCRIPT_TABLE).where(conditions).fetch();
            Set<ULong> templateIdSet;
            if (records != null && records.size() > 0) {
                templateIdSet = records.intoSet(STEP_SCRIPT_TABLE.TEMPLATE_ID);
                if (CollectionUtils.isNotEmpty(templateIdSet)) {
                    log.debug("Processing template status...|{}|{}", scriptStatusUpdateMessage, templateIdSet);
                    templateIdSet.forEach(templateId -> {
                        try {
                            templateStatusUpdateService.processTemplateStatus(templateId);
                        } catch (Exception e) {
                            log.error("Error while processing template status!|{}", templateId, e);
                        }
                    });
                } else {
                    log.error("Error while process template status!|Convert records failed!|{}|{}",
                        scriptStatusUpdateMessage, records);
                }
                log.debug("Process template status finished.|{}", templateIdSet);
            } else {
                log.warn("Error while process template status!|No template to update!|{}", scriptStatusUpdateMessage);
            }
        } else {
            log.error("Error while process template status!|Param error!|{}", scriptStatusUpdateMessage);
            throw new InvalidParamException(ErrorCode.ILLEGAL_PARAM);
        }
    }

    @Transactional(rollbackFor = {Exception.class, Error.class})
    public void processTemplateStatus(ULong templateId) {
        Result<Record2<ULong, ULong>> records =
            context.select(STEP_SCRIPT_TABLE.ID, STEP_SCRIPT_TABLE.SCRIPT_VERSION_ID).from(STEP_SCRIPT_TABLE)
                .where(STEP_SCRIPT_TABLE.TEMPLATE_ID.eq(templateId)).fetch();
        if (records != null && records.size() > 0) {
            Map<ULong, ULong> templateScriptVersionMap =
                records.intoMap(STEP_SCRIPT_TABLE.ID, STEP_SCRIPT_TABLE.SCRIPT_VERSION_ID);
            if (MapUtils.isNotEmpty(templateScriptVersionMap)) {
                Result<Record2<ULong, UByte>> scriptVersionStatusInfo =
                    context.select(SCRIPT_VERSION_TABLE.ID, SCRIPT_VERSION_TABLE.STATUS).from(SCRIPT_VERSION_TABLE)
                        .where(SCRIPT_VERSION_TABLE.ID.in(templateScriptVersionMap.values())).fetch();

                if (scriptVersionStatusInfo != null && scriptVersionStatusInfo.size() > 0) {

                    Set<ULong> offlineVersions = new HashSet<>();
                    Set<ULong> disabledVersions = new HashSet<>();

                    scriptVersionStatusInfo.forEach(record -> {
                        switch (record.get(SCRIPT_VERSION_TABLE.STATUS).intValue()) {
                            case 2:
                                // JobResourceStatusEnum.Offline
                                offlineVersions.add(record.get(SCRIPT_VERSION_TABLE.ID));
                                break;
                            case 3:
                                // JobResourceStatusEnum.Disabled
                                disabledVersions.add(record.get(SCRIPT_VERSION_TABLE.ID));
                                break;
                            default:
                                break;
                        }
                    });

                    int scriptStatus = 0;

                    if (offlineVersions.size() > 0) {
                        scriptStatus |= 0b1;
                    }
                    if (disabledVersions.size() > 0) {
                        scriptStatus |= 0b10;
                    }

                    templateStatusUpdateService.updateTemplateStatus(templateId, scriptStatus);

                    processScriptStepStatus(templateScriptVersionMap, offlineVersions, disabledVersions);

                } else {
                    templateStatusUpdateService.updateTemplateStatus(templateId, 0);
                    log.warn("Getting script version info failed!|{}|{}", templateId, templateScriptVersionMap);
                }
            } else {
                templateStatusUpdateService.updateTemplateStatus(templateId, 0);
                log.error("Error while process template status!|Convert records failed!|{}|{}", templateId, records);
            }
        } else {
            templateStatusUpdateService.updateTemplateStatus(templateId, 0);
            log.warn("Error while process template status!|Fetch template step info failed!|{}", templateId);
        }
    }

    private void processScriptStepStatus(Map<ULong, ULong> templateScriptVersionMap, Set<ULong> offlineVersions,
                                         Set<ULong> disabledVersions) {
        for (Map.Entry<ULong, ULong> scriptStepInfo : templateScriptVersionMap.entrySet()) {
            if (offlineVersions.contains(scriptStepInfo.getValue())) {
                context.update(STEP_SCRIPT_TABLE).set(STEP_SCRIPT_TABLE.STATUS, UByte.valueOf(0b1))
                    .where(STEP_SCRIPT_TABLE.ID.equal(scriptStepInfo.getKey())).limit(1).execute();
            } else if (disabledVersions.contains(scriptStepInfo.getValue())) {
                context.update(STEP_SCRIPT_TABLE).set(STEP_SCRIPT_TABLE.STATUS, UByte.valueOf(0b10))
                    .where(STEP_SCRIPT_TABLE.ID.equal(scriptStepInfo.getKey())).limit(1).execute();
            } else {
                context.update(STEP_SCRIPT_TABLE).set(STEP_SCRIPT_TABLE.STATUS, UByte.valueOf(0))
                    .where(STEP_SCRIPT_TABLE.ID.equal(scriptStepInfo.getKey())).limit(1).execute();
            }
        }
    }

    @Transactional(rollbackFor = {Exception.class, Error.class})
    public void updateTemplateStatus(ULong templateId, int scriptStatus) {
        taskTemplateDAO.updateTemplateStatus(templateId, scriptStatus);
    }

    class TemplateStatusUpdateThread extends Thread {
        @Override
        public void run() {
            this.setName("Template-Status-Update-Thread");
            while (true) {
                String uuid = UUID.randomUUID().toString();
                log.debug("{}|Waiting for script update message...", uuid);
                try {
                    doUpdateStatus(uuid, UPDATE_MESSAGE_QUEUE.take());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    log.error("{}|Error while updating template status!", uuid);
                }
                log.debug("{}|Script update message processed.", uuid);
            }
        }
    }
}
