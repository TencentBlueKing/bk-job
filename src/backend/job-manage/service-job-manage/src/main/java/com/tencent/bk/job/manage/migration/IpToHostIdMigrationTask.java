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

package com.tencent.bk.job.manage.migration;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.manage.dao.TaskFileInfoDAO;
import com.tencent.bk.job.manage.dao.TaskFileStepDAO;
import com.tencent.bk.job.manage.dao.TaskScriptStepDAO;
import com.tencent.bk.job.manage.dao.TaskVariableDAO;
import com.tencent.bk.job.manage.model.dto.ResourceTagDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskVariableDTO;
import com.tencent.bk.job.manage.service.host.HostService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 主机数据迁移，把作业模板/执行方案/ip白名单等使用主机的数据，从原来的存储云区域+ip，转换为存储hostId
 */
@Service
@Slf4j
public class IpToHostIdMigrationTask {
    private final TaskVariableDAO taskTemplateVariableDAO;
    private final TaskVariableDAO taskPlanVariableDAO;
    private final TaskScriptStepDAO taskTemplateScriptStepDAO;
    private final TaskScriptStepDAO taskPlanScriptStepDAO;
    private final TaskFileStepDAO taskTemplateFileStepDAO;
    private final TaskFileStepDAO taskPlanFileStepDAO;
    private final TaskFileInfoDAO taskTemplateFileInfoDAO;
    private final TaskFileInfoDAO taskPlanFileInfoDAO;
    private final HostService hostService;
    private final Map<String, Long> ipAndHostIpMap = new HashMap<>();

    public IpToHostIdMigrationTask(@Qualifier("TaskTemplateVariableDAOImpl") TaskVariableDAO taskTemplateVariableDAO,
                                   @Qualifier("TaskPlanVariableDAOImpl") TaskVariableDAO taskPlanVariableDAO,
                                   @Qualifier("TaskTemplateScriptStepDAOImpl") TaskScriptStepDAO taskTemplateScriptStepDAO,
                                   @Qualifier("TaskPlanScriptStepDAOImpl") TaskScriptStepDAO taskPlanScriptStepDAO,
                                   @Qualifier("TaskTemplateFileStepDAOImpl") TaskFileStepDAO taskTemplateFileStepDAO,
                                   @Qualifier("TaskPlanFileStepDAOImpl") TaskFileStepDAO taskPlanFileStepDAO,
                                   @Qualifier("TaskTemplateFileInfoDAOImpl") TaskFileInfoDAO taskTemplateFileInfoDAO,
                                   @Qualifier("TaskPlanFileInfoDAOImpl") TaskFileInfoDAO taskPlanFileInfoDAO,
                                   @Autowired HostService hostService) {
        this.taskTemplateVariableDAO = taskTemplateVariableDAO;
        this.taskPlanVariableDAO = taskPlanVariableDAO;
        this.taskTemplateScriptStepDAO = taskTemplateScriptStepDAO;
        this.taskPlanScriptStepDAO = taskPlanScriptStepDAO;
        this.taskTemplateFileStepDAO = taskTemplateFileStepDAO;
        this.taskPlanFileStepDAO = taskPlanFileStepDAO;
        this.taskTemplateFileInfoDAO = taskTemplateFileInfoDAO;
        this.taskPlanFileInfoDAO = taskPlanFileInfoDAO;
        this.hostService = hostService;
    }

    public Response<List<ResourceTagDTO>> execute() {
        try {
            log.info("IpToHostIdMigrationTask start...");
            migrateTemplateHostVariable();
            migratePlanHostVariable();
            migrateTemplateScriptStepHost();
            migratePlanScriptStepHost();
            migrateTemplateFileStepHost();
            migratePlanFileStepHost();
            migrateTemplateFileStepSourceHost();
            migratePlanFileStepSourceHost();
            log.info("IpToHostIdMigrationTask end...");
            return Response.buildSuccessResp(null);
        } catch (Throwable e) {
            log.error("IpToHostIdMigrationTask caught exception", e);
            return Response.buildCommonFailResp(ErrorCode.INTERNAL_ERROR);
        }

    }

    private void migrateTemplateHostVariable() {
        log.info("Begin to migrate template host variable ...");

        StopWatch watch = new StopWatch("migrateTemplateHostVariable");
        watch.start("listHostVariables");
        List<TaskVariableDTO> taskVariables = taskTemplateVariableDAO.listHostVariables();
        if (CollectionUtils.isEmpty(taskVariables)) {
            log.info("Template host variable is empty! skip");
            return;
        }
        watch.stop();

        watch.start("fillHostVariableHostId");
        List<TaskVariableDTO> needMigrateVariables = fillHostVariableHostId(taskVariables);
        watch.stop();

        watch.start("saveVariableValues");
        log.info("Save template variable values, ids: {}",
            needMigrateVariables.stream().map(TaskVariableDTO::getId).collect(Collectors.toList()));
        saveTaskTemplateVariableValues(needMigrateVariables);
        watch.stop();

        log.info("Migrate template host variable successfully! cost: {}", watch.prettyPrint());
    }


    private List<TaskVariableDTO> fillHostVariableHostId(List<TaskVariableDTO> taskVariables) {
        List<TaskVariableDTO> needMigrateVariables = new ArrayList<>();
        taskVariables.forEach(variable -> {
            TaskTargetDTO target = TaskTargetDTO.fromJsonString(variable.getDefaultValue());
            if (target == null) {
                return;
            }
            boolean changed = fillHostId(target);
            if (changed) {
                variable.setDefaultValue(target.toJsonString());
                needMigrateVariables.add(variable);
            }
        });

        return needMigrateVariables;
    }

    /**
     * 设置hostId
     *
     * @param target 执行目标主机
     * @return 传入的target是否发生变更
     */
    private boolean fillHostId(TaskTargetDTO target) {
        if (target == null || target.getHostNodeList() == null
            || CollectionUtils.isEmpty(target.getHostNodeList().getHostList())) {
            return false;
        }

        List<ApplicationHostDTO> hosts = target.getHostNodeList().getHostList();
        boolean changed = false;
        for (ApplicationHostDTO oriHost : hosts) {
            // 如果hostId不存在或者不合法(<1）
            if (oriHost.getHostId() == null || oriHost.getHostId() <= 1) {
                changed = true;
                String cloudIp = oriHost.getCloudIp();
                Long hostId = ipAndHostIpMap.get(cloudIp);
                if (hostId == null) {
                    ApplicationHostDTO host = hostService.getHostByIp(cloudIp);
                    if (host != null) {
                        hostId = host.getHostId();
                    } else {
                        log.info("Get host by ip: {}, host not exist!", cloudIp);
                        hostId = -1L;
                    }
                    ipAndHostIpMap.put(cloudIp, hostId);
                }
                oriHost.setHostId(hostId);
            }
        }
        return changed;
    }

    @Transactional(rollbackFor = {Throwable.class})
    public void saveTaskTemplateVariableValues(List<TaskVariableDTO> needMigrateVariables) {
        needMigrateVariables.forEach(variable ->
            taskTemplateVariableDAO.updateVariableValue(variable.getId(), variable.getDefaultValue()));
    }

    private void migratePlanHostVariable() {
        log.info("Begin to migrate plan host variable ...");
        StopWatch watch = new StopWatch("migratePlanHostVariable");
        watch.start("listHostVariables");
        List<TaskVariableDTO> taskVariables = taskPlanVariableDAO.listHostVariables();
        if (CollectionUtils.isEmpty(taskVariables)) {
            log.info("Plan host variable is empty! skip");
            return;
        }
        watch.stop();

        watch.start("fillHostVariableHostId");
        List<TaskVariableDTO> needMigrateVariables = fillHostVariableHostId(taskVariables);
        watch.stop();

        watch.start("saveVariableValues");
        log.info("Save plan variable values, ids: {}",
            needMigrateVariables.stream().map(TaskVariableDTO::getId).collect(Collectors.toList()));
        saveTaskPlanVariableValues(needMigrateVariables);
        watch.stop();

        log.info("Migrate plan host variable successfully! cost: {}", watch.prettyPrint());
    }

    @Transactional(rollbackFor = {Throwable.class})
    public void saveTaskPlanVariableValues(List<TaskVariableDTO> needMigrateVariables) {
        needMigrateVariables.forEach(variable ->
            taskPlanVariableDAO.updateVariableValue(variable.getId(), variable.getDefaultValue()));
    }


    private void migrateTemplateScriptStepHost() {
        log.info("Begin to migrate template script step host ...");
        StopWatch watch = new StopWatch("migrateTemplateScriptStepHost");
        watch.start("listStepTargets");
        // Map<recordId, target>
        Map<Long, TaskTargetDTO> stepTargets = taskTemplateScriptStepDAO.listStepTargets();
        if (stepTargets == null || stepTargets.isEmpty()) {
            return;
        }
        watch.stop();

        watch.start("fillHostId");
        Map<Long, TaskTargetDTO> migratedStepTargets = new HashMap<>();
        stepTargets.forEach((recordId, target) -> {
            boolean changed = fillHostId(target);
            if (changed) {
                migratedStepTargets.put(recordId, target);
            }
        });
        watch.stop();

        watch.start("saveStepTargets");
        if (!migratedStepTargets.isEmpty()) {
            saveTaskTemplateScriptStepTargets(migratedStepTargets);
        }
        watch.stop();

        log.info("Migrate template script step host successfully! cost: {}", watch.prettyPrint());
    }

    @Transactional(rollbackFor = {Throwable.class})
    public void saveTaskTemplateScriptStepTargets(Map<Long, TaskTargetDTO> migratedStepTargets) {
        migratedStepTargets.forEach((recordId, target) ->
            taskTemplateScriptStepDAO.updateStepTargets(recordId, target.toJsonString()));
    }

    private void migratePlanScriptStepHost() {
        log.info("Begin to migrate plan script step host ...");
        StopWatch watch = new StopWatch("migratePlanScriptStepHost");
        watch.start("listStepTargets");
        // Map<recordId, target>
        Map<Long, TaskTargetDTO> stepTargets = taskPlanScriptStepDAO.listStepTargets();
        if (stepTargets == null || stepTargets.isEmpty()) {
            return;
        }
        watch.stop();

        watch.start("fillHostId");
        Map<Long, TaskTargetDTO> migratedStepTargets = new HashMap<>();
        stepTargets.forEach((recordId, target) -> {
            boolean changed = fillHostId(target);
            if (changed) {
                migratedStepTargets.put(recordId, target);
            }
        });
        watch.stop();

        watch.start("saveStepTargets");
        if (!migratedStepTargets.isEmpty()) {
            saveTaskPlanScriptStepTargets(migratedStepTargets);
        }
        watch.stop();

        log.info("Migrate plan script step host successfully! cost: {}", watch.prettyPrint());
    }

    @Transactional(rollbackFor = {Throwable.class})
    public void saveTaskPlanScriptStepTargets(Map<Long, TaskTargetDTO> migratedStepTargets) {
        migratedStepTargets.forEach((recordId, target) ->
            taskPlanScriptStepDAO.updateStepTargets(recordId, target.toJsonString()));
    }

    private void migrateTemplateFileStepHost() {
        log.info("Begin to migrate template file step host ...");
        StopWatch watch = new StopWatch("migrateTemplateFileStepHost");
        watch.start("listStepTargets");
        // Map<recordId, target>
        Map<Long, TaskTargetDTO> stepTargets = taskTemplateFileStepDAO.listStepTargets();
        if (stepTargets == null || stepTargets.isEmpty()) {
            return;
        }
        watch.stop();

        watch.start("fillHostId");
        Map<Long, TaskTargetDTO> migratedStepTargets = new HashMap<>();
        stepTargets.forEach((recordId, target) -> {
            boolean changed = fillHostId(target);
            if (changed) {
                migratedStepTargets.put(recordId, target);
            }
        });
        watch.stop();

        watch.start("saveStepTargets");
        if (!migratedStepTargets.isEmpty()) {
            saveTemplateFileStepTargets(migratedStepTargets);
        }
        watch.stop();

        log.info("Migrate template file step host successfully! cost: {}", watch.prettyPrint());
    }

    @Transactional(rollbackFor = {Throwable.class})
    public void saveTemplateFileStepTargets(Map<Long, TaskTargetDTO> migratedStepTargets) {
        migratedStepTargets.forEach((recordId, target) ->
            taskTemplateFileStepDAO.updateStepTargets(recordId, target.toJsonString()));
    }

    private void migratePlanFileStepHost() {
        log.info("Begin to migrate plan file step host ...");
        StopWatch watch = new StopWatch("migratePlanFileStepHost");
        watch.start("listStepTargets");
        // Map<recordId, target>
        Map<Long, TaskTargetDTO> stepTargets = taskPlanFileStepDAO.listStepTargets();
        if (stepTargets == null || stepTargets.isEmpty()) {
            return;
        }
        watch.stop();

        watch.start("fillHostId");
        Map<Long, TaskTargetDTO> migratedStepTargets = new HashMap<>();
        stepTargets.forEach((recordId, target) -> {
            boolean changed = fillHostId(target);
            if (changed) {
                migratedStepTargets.put(recordId, target);
            }
        });
        watch.stop();

        watch.start("saveStepTargets");
        if (!migratedStepTargets.isEmpty()) {
            savePlanFileStepTargets(migratedStepTargets);
        }
        watch.stop();

        log.info("Migrate plan file step host successfully! cost: {}", watch.prettyPrint());
    }

    @Transactional(rollbackFor = {Throwable.class})
    public void savePlanFileStepTargets(Map<Long, TaskTargetDTO> migratedStepTargets) {
        migratedStepTargets.forEach((recordId, target) ->
            taskTemplateFileStepDAO.updateStepTargets(recordId, target.toJsonString()));
    }

    private void migrateTemplateFileStepSourceHost() {
        log.info("Begin to migrate template file step source host ...");
        StopWatch watch = new StopWatch("migratePlanFileStepSourceHost");
        watch.start("listStepSourceHosts");
        // Map<recordId, target>
        Map<Long, TaskTargetDTO> stepTargets = taskTemplateFileInfoDAO.listStepFileHosts();
        if (stepTargets == null || stepTargets.isEmpty()) {
            return;
        }
        watch.stop();

        watch.start("fillHostId");
        Map<Long, TaskTargetDTO> migratedStepTargets = new HashMap<>();
        stepTargets.forEach((recordId, target) -> {
            boolean changed = fillHostId(target);
            if (changed) {
                migratedStepTargets.put(recordId, target);
            }
        });
        watch.stop();

        watch.start("saveStepSourceHosts");
        if (!migratedStepTargets.isEmpty()) {
            saveTemplateFileStepSourceHosts(migratedStepTargets);
        }
        watch.stop();

        log.info("Migrate plan file step host successfully! cost: {}", watch.prettyPrint());
    }

    @Transactional(rollbackFor = {Throwable.class})
    public void saveTemplateFileStepSourceHosts(Map<Long, TaskTargetDTO> migratedStepTargets) {
        migratedStepTargets.forEach((recordId, target) ->
            taskTemplateFileInfoDAO.updateStepFileHosts(recordId, target.toJsonString()));
    }

    private void migratePlanFileStepSourceHost() {
        log.info("Begin to migrate plan file step source host ...");
        StopWatch watch = new StopWatch("migratePlanFileStepSourceHost");
        watch.start("listStepSourceHosts");
        // Map<recordId, target>
        Map<Long, TaskTargetDTO> stepTargets = taskPlanFileInfoDAO.listStepFileHosts();
        if (stepTargets == null || stepTargets.isEmpty()) {
            return;
        }
        watch.stop();

        watch.start("fillHostId");
        Map<Long, TaskTargetDTO> migratedStepTargets = new HashMap<>();
        stepTargets.forEach((recordId, target) -> {
            boolean changed = fillHostId(target);
            if (changed) {
                migratedStepTargets.put(recordId, target);
            }
        });
        watch.stop();

        watch.start("saveStepSourceHosts");
        if (!migratedStepTargets.isEmpty()) {
            savePlanFileStepSourceHosts(migratedStepTargets);
        }
        watch.stop();

        log.info("Migrate plan file step host successfully! cost: {}", watch.prettyPrint());
    }

    @Transactional(rollbackFor = {Throwable.class})
    public void savePlanFileStepSourceHosts(Map<Long, TaskTargetDTO> migratedStepTargets) {
        migratedStepTargets.forEach((recordId, target) ->
            taskPlanFileInfoDAO.updateStepFileHosts(recordId, target.toJsonString()));
    }

}
