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

package com.tencent.bk.job.manage.common.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tencent.bk.job.common.constant.AppTypeEnum;
import com.tencent.bk.job.common.constant.DuplicateHandlerEnum;
import com.tencent.bk.job.common.constant.NotExistPathHandlerEnum;
import com.tencent.bk.job.common.model.dto.ApplicationHostInfoDTO;
import com.tencent.bk.job.common.model.dto.ApplicationInfoDTO;
import com.tencent.bk.job.common.model.dto.UserRoleInfoDTO;
import com.tencent.bk.job.common.util.StringUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskApprovalTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskFileTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskScriptSourceEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskStepTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskTemplateStatusEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskTypeEnum;
import com.tencent.bk.job.manage.model.dto.task.TaskApprovalStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskFileInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskFileStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskPlanInfoDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskScriptStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskStepDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTemplateInfoDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Record11;
import org.jooq.Record12;
import org.jooq.Record13;
import org.jooq.Record15;
import org.jooq.Record6;
import org.jooq.Record9;
import org.jooq.generated.tables.Application;
import org.jooq.generated.tables.Host;
import org.jooq.generated.tables.TaskPlan;
import org.jooq.generated.tables.TaskTemplate;
import org.jooq.generated.tables.TaskTemplateStepFileList;
import org.jooq.types.UByte;
import org.jooq.types.ULong;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 5/10/2019 09:38
 */
@Slf4j
public class DbRecordMapper {

    private static final TaskTemplateStepFileList TABLE_TASK_TEMPLATE_STEP_FILE_LIST =
        TaskTemplateStepFileList.TASK_TEMPLATE_STEP_FILE_LIST;



    public static TaskApprovalStepDTO
    convertRecordToTaskApprovalStep(Record6<ULong, ULong, UByte, String, String, String> record) {
        if (record == null) {
            return null;
        }
        TaskApprovalStepDTO taskApprovalStep = new TaskApprovalStepDTO();
        taskApprovalStep.setId(((ULong) record.get(0)).longValue());
        taskApprovalStep.setStepId(((ULong) record.get(1)).longValue());
        taskApprovalStep.setApprovalType(TaskApprovalTypeEnum.valueOf(((UByte) record.get(2)).intValue()));
        taskApprovalStep
            .setApprovalUser(JsonUtils.fromJson((String) record.get(3), new TypeReference<UserRoleInfoDTO>() {
            }));
        taskApprovalStep.setApprovalMessage((String) record.get(4));
        taskApprovalStep
            .setNotifyChannel(JsonUtils.fromJson((String) record.get(5), new TypeReference<List<String>>() {
            }));
        return taskApprovalStep;
    }

    public static TaskStepDTO convertRecordToTaskStep(
        Record11<ULong, ULong, String, UByte, ULong, ULong, ULong, ULong, ULong, ULong, UByte> record,
        TaskTypeEnum taskType) {
        if (record == null) {
            return null;
        }
        TaskStepDTO taskStep = new TaskStepDTO();
        taskStep.setId(((ULong) record.get(0)).longValue());
        switch (taskType) {
            case TEMPLATE:
                taskStep.setTemplateId(((ULong) record.get(1)).longValue());
                taskStep.setDelete(((UByte) record.get(10)).intValue());
                taskStep.setTemplateStepId(null);
                break;
            case PLAN:
                taskStep.setPlanId(((ULong) record.get(1)).longValue());
                taskStep.setEnable(((UByte) record.get(10)).intValue());
                taskStep.setTemplateStepId(((ULong) record.get(6)).longValue());
                break;
            default:
                return null;
        }
        taskStep.setName((String) record.get(2));
        taskStep.setType(TaskStepTypeEnum.valueOf(((UByte) record.get(3)).intValue()));
        taskStep.setPreviousStepId(((ULong) record.get(4)).longValue());
        taskStep.setNextStepId(((ULong) record.get(5)).longValue());
        if (taskStep.getType() == null) {
            return null;
        }

        switch (taskStep.getType()) {
            case SCRIPT:
                taskStep.setScriptStepId(JooqDataTypeUtil.getLongFromULong((ULong) record.get(7)));
                break;
            case FILE:
                taskStep.setFileStepId(JooqDataTypeUtil.getLongFromULong((ULong) record.get(8)));
                break;
            case APPROVAL:
                taskStep.setApprovalStepId(JooqDataTypeUtil.getLongFromULong((ULong) record.get(9)));
                break;
            default:
                return null;
        }
        return taskStep;
    }

    public static TaskScriptStepDTO convertRecordToTaskScriptStep(Record15<ULong, ULong, ULong, UByte, String, ULong,
        String, UByte, String, ULong, ULong, String, UByte, UByte, UByte> record, TaskTypeEnum taskType) {
        if (record == null) {
            return null;
        }
        TaskScriptStepDTO taskScriptStep = new TaskScriptStepDTO();
        taskScriptStep.setId(((ULong) record.get(0)).longValue());
        switch (taskType) {
            case TEMPLATE:
                taskScriptStep.setTemplateId(((ULong) record.get(1)).longValue());
                break;
            case PLAN:
                taskScriptStep.setPlanId(((ULong) record.get(1)).longValue());
                break;
            default:
                return null;
        }
        taskScriptStep.setStepId(((ULong) record.get(2)).longValue());
        taskScriptStep.setScriptSource(TaskScriptSourceEnum.valueOf(((UByte) record.get(3)).intValue()));
        taskScriptStep.setScriptId((String) record.get(4));
        if (record.get(5) != null) {
            taskScriptStep.setScriptVersionId(((ULong) record.get(5)).longValue());
        }
        taskScriptStep.setContent((String) record.get(6));
        taskScriptStep.setLanguage(ScriptTypeEnum.valueOf(((UByte) record.get(7)).intValue()));
        taskScriptStep.setScriptParam((String) record.get(8));
        taskScriptStep.setTimeout(((ULong) record.get(9)).longValue());
        taskScriptStep.setAccount(((ULong) record.get(10)).longValue());
        taskScriptStep.setExecuteTarget(TaskTargetDTO.fromString((String) record.get(11)));
        taskScriptStep.setSecureParam(((UByte) record.get(12)).intValue() == 1);
        taskScriptStep.setStatus(((UByte) record.get(13)).intValue());
        taskScriptStep.setIgnoreError(((UByte) record.get(14)).intValue() == 1);
        return taskScriptStep;
    }

    public static TaskFileStepDTO
    convertRecordToTaskFileStep(
        Record11<ULong, ULong, String, ULong, String, ULong, ULong, ULong, UByte, UByte, UByte> record) {
        if (record == null) {
            return null;
        }
        TaskFileStepDTO taskFileStep = new TaskFileStepDTO();
        taskFileStep.setId(((ULong) record.get(0)).longValue());
        taskFileStep.setStepId(((ULong) record.get(1)).longValue());
        taskFileStep.setOriginFileList(new ArrayList<>());
        taskFileStep.setDestinationFileLocation((String) record.get(2));
        taskFileStep.setExecuteAccount(((ULong) record.get(3)).longValue());
        taskFileStep.setDestinationHostList(TaskTargetDTO.fromString((String) record.get(4)));
        taskFileStep.setTimeout(((ULong) record.get(5)).longValue());
        taskFileStep.setOriginSpeedLimit(JooqDataTypeUtil.getLongFromULong((ULong) record.get(6)));
        taskFileStep.setTargetSpeedLimit(JooqDataTypeUtil.getLongFromULong((ULong) record.get(7)));
        taskFileStep.setIgnoreError(((UByte) record.get(8)).intValue() == 1);
        taskFileStep.setDuplicateHandler(DuplicateHandlerEnum.valueOf(((UByte) record.get(9)).intValue()));
        if (record.get(10) == null) {
            taskFileStep.setNotExistPathHandler(NotExistPathHandlerEnum.CREATE_DIR);
        } else {
            taskFileStep.setNotExistPathHandler(NotExistPathHandlerEnum.valueOf(((UByte) record.get(10)).intValue()));
        }
        return taskFileStep;
    }

    /**
     * 此处使用数字获取的原因：同时兼容作业模板与执行方案
     *
     * @param record
     * @return
     */
    public static TaskFileInfoDTO
    convertRecordToTaskFileInfo(Record9<ULong, ULong, UByte, String, ULong, String, String, ULong, Integer> record) {
        if (record == null) {
            return null;
        }
        TaskFileInfoDTO taskFileInfo = new TaskFileInfoDTO();
        taskFileInfo.setId(((ULong) record.get(0)).longValue());
        taskFileInfo.setStepId(((ULong) record.get(1)).longValue());
        taskFileInfo.setFileType(TaskFileTypeEnum.valueOf(((UByte) record.get(2)).intValue()));
        taskFileInfo.setFileLocation(JsonUtils.fromJson((String) record.get(3), new TypeReference<List<String>>() {
        }));
        if (record.get(4) != null) {
            taskFileInfo.setFileSize(((ULong) record.get(4)).longValue());
        } else {
            taskFileInfo.setFileSize(null);
        }
        taskFileInfo.setFileHash((String) record.get(5));
        taskFileInfo.setHost(TaskTargetDTO.fromString((String) record.get(6)));
        if (record.get(7) != null) {
            taskFileInfo.setHostAccount(((ULong) record.get(7)).longValue());
        } else {
            taskFileInfo.setHostAccount(null);
        }
        taskFileInfo.setFileSourceId((Integer) record.get(8));
        return taskFileInfo;
    }

    public static TaskTemplateInfoDTO convertRecordToTemplateInfo(Record13<ULong, ULong, String, String, String, UByte,
        ULong, String, ULong, ULong, ULong, String, UByte> record) {
        if (record == null) {
            return null;
        }

        TaskTemplate table = TaskTemplate.TASK_TEMPLATE;
        TaskTemplateInfoDTO taskTemplateInfo = new TaskTemplateInfoDTO();
        taskTemplateInfo.setId(record.get(table.ID).longValue());
        taskTemplateInfo.setAppId(record.get(table.APP_ID).longValue());
        taskTemplateInfo.setName(record.get(table.NAME));
        taskTemplateInfo.setDescription(record.get(table.DESCRIPTION));
        taskTemplateInfo.setCreator(record.get(table.CREATOR));
        taskTemplateInfo.setStatus(TaskTemplateStatusEnum.valueOf(record.get(table.STATUS).intValue()));
        taskTemplateInfo.setCreateTime(record.get(table.CREATE_TIME).longValue());
        taskTemplateInfo.setLastModifyUser(record.get(table.LAST_MODIFY_USER));
        taskTemplateInfo.setLastModifyTime(record.get(table.LAST_MODIFY_TIME).longValue());
        taskTemplateInfo.setFirstStepId(record.get(table.FIRST_STEP_ID).longValue());
        taskTemplateInfo.setLastStepId(record.get(table.LAST_STEP_ID).longValue());
        taskTemplateInfo.setVersion(record.get(table.VERSION));
        taskTemplateInfo.setScriptStatus(record.get(table.SCRIPT_STATUS).intValue());
        return taskTemplateInfo;
    }

    public static ApplicationHostInfoDTO convertRecordToApplicationHostInfo(
        Record12<ULong, ULong, String, String, String, String, ULong, String, String, String, String, UByte> record) {
        if (record == null) {
            return null;
        }

        Host table = Host.HOST;
        ApplicationHostInfoDTO applicationHostInfoDTO = new ApplicationHostInfoDTO();
        applicationHostInfoDTO.setAppId(record.get(table.APP_ID).longValue());
        applicationHostInfoDTO.setIp(record.get(table.IP));
        applicationHostInfoDTO.setIpDesc(record.get(table.IP_DESC));
        applicationHostInfoDTO.setGseAgentAlive(record.get(table.IS_AGENT_ALIVE).intValue() == 1);
        List<Long> setIdList = new ArrayList<>();
        String setIdsStr = record.get(table.SET_IDS);
        if (setIdsStr != null) {
            setIdList = Arrays.asList(setIdsStr.split(",")).stream().filter(id -> !id.trim().equals(""))
                .map(Long::parseLong).collect(Collectors.toList());
        }
        applicationHostInfoDTO.setSetId(setIdList);
        applicationHostInfoDTO.setModuleId(StringUtil.strToList(record.get(table.MODULE_IDS), Long.class, ","));
        applicationHostInfoDTO.setCloudAreaId(record.get(table.CLOUD_AREA_ID).longValue());
        applicationHostInfoDTO.setDisplayIp(record.get(table.DISPLAY_IP));
        applicationHostInfoDTO.setOs(record.get(table.OS));
        applicationHostInfoDTO.setOsType(record.get(table.OS_TYPE));
        applicationHostInfoDTO.setModuleType(StringUtil.strToList(record.get(table.MODULE_TYPE), Long.class, ","));
        applicationHostInfoDTO.setHostId(record.get(table.HOST_ID).longValue());
        return applicationHostInfoDTO;
    }

    public static ApplicationInfoDTO
    convertRecordToApplicationInfo(Record9<ULong, String, String, String, Byte, String, String, Long, String> record) {
        if (record == null) {
            return null;
        }
        Application table = Application.APPLICATION;
        ApplicationInfoDTO applicationInfoDTO = new ApplicationInfoDTO();
        applicationInfoDTO.setId(record.get(table.APP_ID).longValue());
        applicationInfoDTO.setName(record.get(table.APP_NAME));
        applicationInfoDTO.setMaintainers(record.get(table.MAINTAINERS));
        applicationInfoDTO.setBkSupplierAccount(record.get(table.BK_SUPPLIER_ACCOUNT));
        applicationInfoDTO.setAppType(AppTypeEnum.valueOf(record.get(table.APP_TYPE)));
        applicationInfoDTO.setSubAppIds(splitSubAppIds(record.get(table.SUB_APP_IDS)));
        applicationInfoDTO.setTimeZone(record.get(table.TIMEZONE));
        applicationInfoDTO.setOperateDeptId(record.get(table.BK_OPERATE_DEPT_ID));
        applicationInfoDTO.setLanguage(record.get(table.LANGUAGE));
        return applicationInfoDTO;
    }

    private static List<Long> splitSubAppIds(String appIds) {
        List<Long> appIdList = new LinkedList<>();
        if (StringUtils.isNotBlank(appIds)) {
            for (String appIdStr : appIds.split("[,;]")) {
                if (StringUtils.isNotBlank(appIdStr)) {
                    appIdList.add(Long.valueOf(appIdStr));
                }
            }

        }
        return appIdList;
    }

    public static ULong getJooqLongValue(Long longValue) {
        if (longValue == null) {
            return null;
        } else {
            return ULong.valueOf(longValue);
        }
    }

    public static Long getLongValue(ULong uLongValue) {
        if (uLongValue == null) {
            return null;
        } else {
            return uLongValue.longValue();
        }
    }

    public static TaskPlanInfoDTO convertRecordToPlanInfo(Record13<ULong, ULong, ULong, UByte, String, String, ULong,
        String, ULong, ULong, ULong, String, UByte> record) {
        if (record == null) {
            return null;
        }
        TaskPlanInfoDTO taskPlanInfo = new TaskPlanInfoDTO();
        TaskPlan table = TaskPlan.TASK_PLAN;
        taskPlanInfo.setId(record.get(table.ID).longValue());
        taskPlanInfo.setAppId(record.get(table.APP_ID).longValue());
        taskPlanInfo.setTemplateId(record.get(table.TEMPLATE_ID).longValue());
        taskPlanInfo.setDebug(record.get(table.TYPE).intValue() == 1);
        taskPlanInfo.setName(record.get(table.NAME));
        taskPlanInfo.setCreator(record.get(table.CREATOR));
        taskPlanInfo.setCreateTime(record.get(table.CREATE_TIME).longValue());
        taskPlanInfo.setLastModifyUser(record.get(table.LAST_MODIFY_USER));
        taskPlanInfo.setLastModifyTime(record.get(table.LAST_MODIFY_TIME).longValue());
        taskPlanInfo.setFirstStepId(record.get(table.FIRST_STEP_ID).longValue());
        taskPlanInfo.setLastStepId(record.get(table.LAST_STEP_ID).longValue());
        taskPlanInfo.setVersion(record.get(table.VERSION));
        taskPlanInfo.setNeedUpdate(record.get(table.IS_LATEST_VERSION).intValue() == 0);
        return taskPlanInfo;
    }
}
