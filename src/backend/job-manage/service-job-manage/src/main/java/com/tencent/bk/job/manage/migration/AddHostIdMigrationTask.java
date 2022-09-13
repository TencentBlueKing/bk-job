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
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetDTO;
import com.tencent.bk.job.manage.service.host.HostService;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.generated.tables.TaskPlanStepFile;
import org.jooq.generated.tables.TaskPlanStepFileList;
import org.jooq.generated.tables.TaskPlanStepScript;
import org.jooq.generated.tables.TaskPlanVariable;
import org.jooq.generated.tables.TaskTemplateStepFile;
import org.jooq.generated.tables.TaskTemplateStepFileList;
import org.jooq.generated.tables.TaskTemplateStepScript;
import org.jooq.generated.tables.TaskTemplateVariable;
import org.jooq.types.UByte;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 作业模板、执行方案等包含的主机数据，在原来的云区域+ip的基础上，填充hostID属性
 */
@Service
@Slf4j
public class AddHostIdMigrationTask {
    private final DSLContext CTX;
    private final HostService hostService;

    private static final TaskTemplateStepScript TASK_TEMPLATE_STEP_SCRIPT =
        TaskTemplateStepScript.TASK_TEMPLATE_STEP_SCRIPT;
    private static final TaskTemplateStepFile TASK_TEMPLATE_STEP_FILE =
        TaskTemplateStepFile.TASK_TEMPLATE_STEP_FILE;
    private static final TaskTemplateStepFileList TASK_TEMPLATE_STEP_FILE_LIST =
        TaskTemplateStepFileList.TASK_TEMPLATE_STEP_FILE_LIST;
    private static final TaskTemplateVariable TASK_TEMPLATE_VARIABLE =
        TaskTemplateVariable.TASK_TEMPLATE_VARIABLE;
    private static final TaskPlanStepScript TASK_PLAN_STEP_SCRIPT =
        TaskPlanStepScript.TASK_PLAN_STEP_SCRIPT;
    private static final TaskPlanStepFile TASK_PLAN_STEP_FILE =
        TaskPlanStepFile.TASK_PLAN_STEP_FILE;
    private static final TaskPlanStepFileList TASK_PLAN_STEP_FILE_LIST =
        TaskPlanStepFileList.TASK_PLAN_STEP_FILE_LIST;
    private static final TaskPlanVariable TASK_PLAN_VARIABLE =
        TaskPlanVariable.TASK_PLAN_VARIABLE;

    /**
     * 不存在的主机，hostId使用-1表示
     */
    private static final long NOT_EXIST_HOST_ID = -1L;

    private final Map<String, Long> ipAndHostIdMapping = new HashMap<>();

    @Autowired
    public AddHostIdMigrationTask(@Qualifier("job-manage-dsl-context") DSLContext ctx,
                                  HostService hostService) {
        CTX = ctx;
        this.hostService = hostService;
    }

    public List<AddHostIdResult> execute(boolean isDryRun) {
        List<AddHostIdResult> results = new ArrayList<>();
        try {
            results.add(migrateTaskTargets(new TaskTemplateStepScriptTargetMigration(), isDryRun));
            results.add(migrateTaskTargets(new TaskTemplateStepFileTargetMigration(), isDryRun));
            results.add(migrateTaskTargets(new TaskTemplateStepFileListTargetMigration(), isDryRun));
            results.add(migrateTaskTargets(new TaskTemplateVariableTargetMigration(), isDryRun));
            results.add(migrateTaskTargets(new TaskPlanStepScriptTargetMigration(), isDryRun));
            results.add(migrateTaskTargets(new TaskPlanStepFileTargetMigration(), isDryRun));
            results.add(migrateTaskTargets(new TaskPlanStepFileListTargetMigration(), isDryRun));
            results.add(migrateTaskTargets(new TaskPlanVariableTargetMigration(), isDryRun));
            return results;
        } finally {
            log.info("AddHostIdMigrationTask done, result: {}", JsonUtils.toJson(results));
            // 清理缓存，避免占用内存
            this.ipAndHostIdMapping.clear();
        }
    }

    private AddHostIdResult migrateTaskTargets(TaskTargetMigration migration, boolean isDryRun) {
        String taskName = "migrate_" + migration.getTableName();
        AddHostIdResult result = new AddHostIdResult(taskName);
        StopWatch watch = new StopWatch(taskName);
        try {
            log.info("[{}] Migration start ...", migration.getTableName());

            watch.start("list_targets");
            Map<Long, TaskTargetDTO> targets = migration.listTaskTargets();
            log.info("[{}] {} targets need migration", migration.getTableName(), targets.size());
            result.setTotalRecords(targets.size());
            watch.stop();

            if (targets.isEmpty()) {
                result.setSuccess(true);
                return result;
            }

            watch.start("add_ip_host_id_mappings");
            addIpAndHostIdMappings(targets.values());
            log.info("[{}] ipAndHostIdMappings: {}", migration.getTableName(), JsonUtils.toJson(ipAndHostIdMapping));
            watch.stop();

            watch.start("fill_host_id");
            List<Long> invalidIds = new ArrayList<>();
            targets.forEach((id, target) -> {
                boolean success = fillHostId(target);
                if (!success) {
                    invalidIds.add(id);
                }
            });
            if (CollectionUtils.isNotEmpty(invalidIds)) {
                log.error("[{}] {} targets fill host id fail, invalidIdList: {}", migration.getTableName(),
                    invalidIds.size(), invalidIds);
                // 删除掉没有设置hostId的
                invalidIds.forEach(targets::remove);
            }
            watch.stop();

            watch.start("update_targets");
            if (isDryRun) {
                // 只输出要更新的数据用于验证，不进行DB数据的变更
                log.info("Update targets, tableName: {},  records: {}", migration.getTableName(),
                    JsonUtils.toJson(targets));
            } else {
                migration.updateTaskTargets(targets);
            }
            result.setSuccessRecords(targets.size());
            watch.stop();
        } catch (Throwable e) {
            // catch all exception
            log.error("Migration caught exception", e);
            result.setSuccess(false);
            // 由于回滚机制，所以successRecords=0
            result.setSuccessRecords(0);
            return result;
        } finally {
            if (watch.isRunning()) {
                watch.stop();
            }
            log.info("[{}] Migration done. cost: {}", migration.getTableName(), watch.prettyPrint());
        }
        result.setSuccess(true);
        return result;
    }

    private void addIpAndHostIdMappings(Collection<TaskTargetDTO> targets) {
        Set<String> notCachedCloudIps = new HashSet<>();
        targets.forEach(target ->
            target.getHostNodeList().getHostList().forEach(host -> {
                String cloudIp = host.getCloudIp();
                if (ipAndHostIdMapping.get(cloudIp) == null) {
                    notCachedCloudIps.add(cloudIp);
                }
            }));

        if (CollectionUtils.isNotEmpty(notCachedCloudIps)) {
            try {
                Map<String, ApplicationHostDTO> hosts = hostService.listHostsByIps(notCachedCloudIps);
                notCachedCloudIps.forEach(notCacheCloudIp -> {
                    ApplicationHostDTO host = hosts.get(notCacheCloudIp);
                    if (host == null) {
                        log.warn("Host with ip {} is not exist in cmdb, set hostId -1", notCacheCloudIp);
                        ipAndHostIdMapping.put(notCacheCloudIp, NOT_EXIST_HOST_ID);
                    } else {
                        ipAndHostIdMapping.put(notCacheCloudIp, host.getHostId());
                    }
                });
            } catch (Throwable e) {
                // 由于从cmdb查询hostId可能会返回异常（比如接口超时）等,为了保证下次迁移不需要全部从头开始，所以这里捕获住异常，控制影响范围
                log.error("Get host by ips fail", e);
            }
        }
    }

    private boolean isTargetMissingHostId(TaskTargetDTO target) {
        return target != null && target.getHostNodeList() != null
            && CollectionUtils.isNotEmpty(target.getHostNodeList().getHostList())
            && !hasHostId(target.getHostNodeList().getHostList().get(0).getHostId());
    }

    private boolean hasHostId(Long hostId) {
        // host = -1,表示已经处理过的数据，并且这个主机被判定为不存在
        return hostId != null && (hostId > 0 || hostId == NOT_EXIST_HOST_ID);
    }

    private boolean fillHostId(TaskTargetDTO target) {
        boolean success = true;
        for (ApplicationHostDTO host : target.getHostNodeList().getHostList()) {
            String cloudIp = host.getCloudIp();
            Long hostId = ipAndHostIdMapping.get(cloudIp);
            if (hostId != null) {
                host.setHostId(hostId);
            } else {
                success = false;
                break;
            }
        }
        return success;
    }

    private String convertTargetToJson(TaskTargetDTO target) {
        String json = JsonUtils.toJson(target);
        if (StringUtils.isBlank(json)) {
            // 可能是解析出现异常，需要抛出异常终止
            throw new InternalException(ErrorCode.INTERNAL_ERROR);
        }
        return json;
    }

    private interface TaskTargetMigration {
        /**
         * 获取表名称
         *
         * @return 表名称
         */
        String getTableName();

        /**
         * 查询所有需要处理的TaskTarget
         *
         * @return Map<表ID, Target>
         */
        Map<Long, TaskTargetDTO> listTaskTargets();

        /**
         * 批量根据表ID更新Target
         *
         * @param targets Map<表ID, Target>
         */
        void updateTaskTargets(Map<Long, TaskTargetDTO> targets);
    }

    private class TaskTemplateStepScriptTargetMigration implements TaskTargetMigration {
        @Override
        public String getTableName() {
            return "task_template_step_script";
        }

        @Override
        public Map<Long, TaskTargetDTO> listTaskTargets() {
            Result<?> result = CTX.select(
                TASK_TEMPLATE_STEP_SCRIPT.ID,
                TASK_TEMPLATE_STEP_SCRIPT.DESTINATION_HOST_LIST)
                .from(TASK_TEMPLATE_STEP_SCRIPT)
                .fetch();
            Map<Long, TaskTargetDTO> targets = new HashMap<>();
            if (result.size() > 0) {
                result.forEach(record -> {
                    long id = record.get(TASK_TEMPLATE_STEP_SCRIPT.ID).longValue();
                    String targetJsonStr = record.get(TASK_TEMPLATE_STEP_SCRIPT.DESTINATION_HOST_LIST);
                    if (StringUtils.isBlank(targetJsonStr)) {
                        return;
                    }
                    TaskTargetDTO target = JsonUtils.fromJson(targetJsonStr, TaskTargetDTO.class);
                    if (isTargetMissingHostId(target)) {
                        targets.put(id, target);
                    }
                });
            }
            return targets;
        }

        @Override
        @Transactional(rollbackFor = {Throwable.class})
        public void updateTaskTargets(Map<Long, TaskTargetDTO> targets) {
            if (targets == null || targets.isEmpty()) {
                return;
            }
            targets.forEach((id, target) ->
                CTX.update(TASK_TEMPLATE_STEP_SCRIPT)
                    .set(TASK_TEMPLATE_STEP_SCRIPT.DESTINATION_HOST_LIST, convertTargetToJson(target))
                    .where(TASK_TEMPLATE_STEP_SCRIPT.ID.eq(ULong.valueOf(id)))
                    .execute());
        }
    }

    private class TaskTemplateStepFileTargetMigration implements TaskTargetMigration {
        @Override
        public String getTableName() {
            return "task_template_step_file";
        }

        @Override
        public Map<Long, TaskTargetDTO> listTaskTargets() {
            Result<?> result = CTX.select(
                TASK_TEMPLATE_STEP_FILE.ID,
                TASK_TEMPLATE_STEP_FILE.DESTINATION_HOST_LIST)
                .from(TASK_TEMPLATE_STEP_FILE)
                .fetch();
            Map<Long, TaskTargetDTO> targets = new HashMap<>();
            if (result.size() > 0) {
                result.forEach(record -> {
                    long id = record.get(TASK_TEMPLATE_STEP_FILE.ID).longValue();
                    String targetJsonStr = record.get(TASK_TEMPLATE_STEP_FILE.DESTINATION_HOST_LIST);
                    if (StringUtils.isBlank(targetJsonStr)) {
                        return;
                    }
                    TaskTargetDTO target = JsonUtils.fromJson(targetJsonStr, TaskTargetDTO.class);
                    if (isTargetMissingHostId(target)) {
                        targets.put(id, target);
                    }
                });
            }
            return targets;
        }

        @Override
        public void updateTaskTargets(Map<Long, TaskTargetDTO> targets) {
            if (targets == null || targets.isEmpty()) {
                return;
            }
            targets.forEach((id, target) ->
                CTX.update(TASK_TEMPLATE_STEP_FILE)
                    .set(TASK_TEMPLATE_STEP_FILE.DESTINATION_HOST_LIST, convertTargetToJson(target))
                    .where(TASK_TEMPLATE_STEP_FILE.ID.eq(ULong.valueOf(id)))
                    .execute());
        }
    }

    private class TaskTemplateStepFileListTargetMigration implements TaskTargetMigration {
        @Override
        public String getTableName() {
            return "task_template_step_file_list";
        }

        @Override
        public Map<Long, TaskTargetDTO> listTaskTargets() {
            Result<?> result = CTX.select(
                TASK_TEMPLATE_STEP_FILE_LIST.ID,
                TASK_TEMPLATE_STEP_FILE_LIST.HOST)
                .from(TASK_TEMPLATE_STEP_FILE_LIST)
                .fetch();
            Map<Long, TaskTargetDTO> targets = new HashMap<>();
            if (result.size() > 0) {
                result.forEach(record -> {
                    long id = record.get(TASK_TEMPLATE_STEP_FILE_LIST.ID).longValue();
                    String targetJsonStr = record.get(TASK_TEMPLATE_STEP_FILE_LIST.HOST);
                    if (StringUtils.isBlank(targetJsonStr)) {
                        return;
                    }
                    TaskTargetDTO target = JsonUtils.fromJson(targetJsonStr, TaskTargetDTO.class);
                    if (isTargetMissingHostId(target)) {
                        targets.put(id, target);
                    }
                });
            }
            return targets;
        }

        @Override
        public void updateTaskTargets(Map<Long, TaskTargetDTO> targets) {
            if (targets == null || targets.isEmpty()) {
                return;
            }
            targets.forEach((id, target) ->
                CTX.update(TASK_TEMPLATE_STEP_FILE_LIST)
                    .set(TASK_TEMPLATE_STEP_FILE_LIST.HOST, convertTargetToJson(target))
                    .where(TASK_TEMPLATE_STEP_FILE_LIST.ID.eq(ULong.valueOf(id)))
                    .execute());
        }
    }

    private class TaskTemplateVariableTargetMigration implements TaskTargetMigration {
        @Override
        public String getTableName() {
            return "task_template_variable";
        }

        @Override
        public Map<Long, TaskTargetDTO> listTaskTargets() {
            Result<?> result = CTX.select(
                TASK_TEMPLATE_VARIABLE.ID,
                TASK_TEMPLATE_VARIABLE.DEFAULT_VALUE)
                .from(TASK_TEMPLATE_VARIABLE)
                .where(TASK_TEMPLATE_VARIABLE.TYPE.eq(UByte.valueOf(TaskVariableTypeEnum.HOST_LIST.getType())))
                .fetch();
            Map<Long, TaskTargetDTO> targets = new HashMap<>();
            if (result.size() > 0) {
                result.forEach(record -> {
                    long id = record.get(TASK_TEMPLATE_VARIABLE.ID).longValue();
                    String targetJsonStr = record.get(TASK_TEMPLATE_VARIABLE.DEFAULT_VALUE);
                    if (StringUtils.isBlank(targetJsonStr)) {
                        return;
                    }
                    TaskTargetDTO target = JsonUtils.fromJson(targetJsonStr, TaskTargetDTO.class);
                    if (isTargetMissingHostId(target)) {
                        targets.put(id, target);
                    }
                });
            }
            return targets;
        }

        @Override
        public void updateTaskTargets(Map<Long, TaskTargetDTO> targets) {
            if (targets == null || targets.isEmpty()) {
                return;
            }
            targets.forEach((id, target) ->
                CTX.update(TASK_TEMPLATE_VARIABLE)
                    .set(TASK_TEMPLATE_VARIABLE.DEFAULT_VALUE, convertTargetToJson(target))
                    .where(TASK_TEMPLATE_VARIABLE.ID.eq(ULong.valueOf(id)))
                    .and(TASK_TEMPLATE_VARIABLE.TYPE.eq(UByte.valueOf(TaskVariableTypeEnum.HOST_LIST.getType())))
                    .execute());
        }
    }


    private class TaskPlanStepScriptTargetMigration implements TaskTargetMigration {
        @Override
        public String getTableName() {
            return "TaskPlanStepScript";
        }

        @Override
        public Map<Long, TaskTargetDTO> listTaskTargets() {
            Result<?> result = CTX.select(
                TASK_PLAN_STEP_SCRIPT.ID,
                TASK_PLAN_STEP_SCRIPT.DESTINATION_HOST_LIST)
                .from(TASK_PLAN_STEP_SCRIPT)
                .fetch();
            Map<Long, TaskTargetDTO> targets = new HashMap<>();
            if (result.size() > 0) {
                result.forEach(record -> {
                    long id = record.get(TASK_PLAN_STEP_SCRIPT.ID).longValue();
                    String targetJsonStr = record.get(TASK_PLAN_STEP_SCRIPT.DESTINATION_HOST_LIST);
                    if (StringUtils.isBlank(targetJsonStr)) {
                        return;
                    }
                    TaskTargetDTO target = JsonUtils.fromJson(targetJsonStr, TaskTargetDTO.class);
                    if (isTargetMissingHostId(target)) {
                        targets.put(id, target);
                    }
                });
            }
            return targets;
        }

        @Override
        @Transactional(rollbackFor = {Throwable.class})
        public void updateTaskTargets(Map<Long, TaskTargetDTO> targets) {
            if (targets == null || targets.isEmpty()) {
                return;
            }
            targets.forEach((id, target) ->
                CTX.update(TASK_PLAN_STEP_SCRIPT)
                    .set(TASK_PLAN_STEP_SCRIPT.DESTINATION_HOST_LIST, convertTargetToJson(target))
                    .where(TASK_PLAN_STEP_SCRIPT.ID.eq(ULong.valueOf(id)))
                    .execute());
        }
    }

    private class TaskPlanStepFileTargetMigration implements TaskTargetMigration {
        @Override
        public String getTableName() {
            return "task_plan_step_file";
        }

        @Override
        public Map<Long, TaskTargetDTO> listTaskTargets() {
            Result<?> result = CTX.select(
                TASK_PLAN_STEP_FILE.ID,
                TASK_PLAN_STEP_FILE.DESTINATION_HOST_LIST)
                .from(TASK_PLAN_STEP_FILE)
                .fetch();
            Map<Long, TaskTargetDTO> targets = new HashMap<>();
            if (result.size() > 0) {
                result.forEach(record -> {
                    long id = record.get(TASK_PLAN_STEP_FILE.ID).longValue();
                    String targetJsonStr = record.get(TASK_PLAN_STEP_FILE.DESTINATION_HOST_LIST);
                    if (StringUtils.isBlank(targetJsonStr)) {
                        return;
                    }
                    TaskTargetDTO target = JsonUtils.fromJson(targetJsonStr, TaskTargetDTO.class);
                    if (isTargetMissingHostId(target)) {
                        targets.put(id, target);
                    }
                });
            }
            return targets;
        }

        @Override
        public void updateTaskTargets(Map<Long, TaskTargetDTO> targets) {
            if (targets == null || targets.isEmpty()) {
                return;
            }
            targets.forEach((id, target) ->
                CTX.update(TASK_PLAN_STEP_FILE)
                    .set(TASK_PLAN_STEP_FILE.DESTINATION_HOST_LIST, convertTargetToJson(target))
                    .where(TASK_PLAN_STEP_FILE.ID.eq(ULong.valueOf(id)))
                    .execute());
        }
    }

    private class TaskPlanStepFileListTargetMigration implements TaskTargetMigration {
        @Override
        public String getTableName() {
            return "task_plan_step_file_list";
        }

        @Override
        public Map<Long, TaskTargetDTO> listTaskTargets() {
            Result<?> result = CTX.select(
                TASK_PLAN_STEP_FILE_LIST.ID,
                TASK_PLAN_STEP_FILE_LIST.HOST)
                .from(TASK_PLAN_STEP_FILE_LIST)
                .fetch();
            Map<Long, TaskTargetDTO> targets = new HashMap<>();
            if (result.size() > 0) {
                result.forEach(record -> {
                    long id = record.get(TASK_PLAN_STEP_FILE_LIST.ID).longValue();
                    String targetJsonStr = record.get(TASK_PLAN_STEP_FILE_LIST.HOST);
                    if (StringUtils.isBlank(targetJsonStr)) {
                        return;
                    }
                    TaskTargetDTO target = JsonUtils.fromJson(targetJsonStr, TaskTargetDTO.class);
                    if (isTargetMissingHostId(target)) {
                        targets.put(id, target);
                    }
                });
            }
            return targets;
        }

        @Override
        public void updateTaskTargets(Map<Long, TaskTargetDTO> targets) {
            if (targets == null || targets.isEmpty()) {
                return;
            }
            targets.forEach((id, target) ->
                CTX.update(TASK_PLAN_STEP_FILE_LIST)
                    .set(TASK_PLAN_STEP_FILE_LIST.HOST, convertTargetToJson(target))
                    .where(TASK_PLAN_STEP_FILE_LIST.ID.eq(ULong.valueOf(id)))
                    .execute());
        }
    }

    private class TaskPlanVariableTargetMigration implements TaskTargetMigration {
        @Override
        public String getTableName() {
            return "task_plan_variable";
        }

        @Override
        public Map<Long, TaskTargetDTO> listTaskTargets() {
            Result<?> result = CTX.select(
                TASK_PLAN_VARIABLE.ID,
                TASK_PLAN_VARIABLE.DEFAULT_VALUE)
                .from(TASK_PLAN_VARIABLE)
                .where(TASK_PLAN_VARIABLE.TYPE.eq(UByte.valueOf(TaskVariableTypeEnum.HOST_LIST.getType())))
                .fetch();
            Map<Long, TaskTargetDTO> targets = new HashMap<>();
            if (result.size() > 0) {
                result.forEach(record -> {
                    long id = record.get(TASK_PLAN_VARIABLE.ID).longValue();
                    String targetJsonStr = record.get(TASK_PLAN_VARIABLE.DEFAULT_VALUE);
                    if (StringUtils.isBlank(targetJsonStr)) {
                        return;
                    }
                    TaskTargetDTO target = JsonUtils.fromJson(targetJsonStr, TaskTargetDTO.class);
                    if (isTargetMissingHostId(target)) {
                        targets.put(id, target);
                    }
                });
            }
            return targets;
        }

        @Override
        public void updateTaskTargets(Map<Long, TaskTargetDTO> targets) {
            if (targets == null || targets.isEmpty()) {
                return;
            }
            targets.forEach((id, target) ->
                CTX.update(TASK_PLAN_VARIABLE)
                    .set(TASK_PLAN_VARIABLE.DEFAULT_VALUE, convertTargetToJson(target))
                    .where(TASK_PLAN_VARIABLE.ID.eq(ULong.valueOf(id)))
                    .and(TASK_PLAN_VARIABLE.TYPE.eq(UByte.valueOf(TaskVariableTypeEnum.HOST_LIST.getType())))
                    .execute());
        }
    }

    @Data
    @NoArgsConstructor
    public static class AddHostIdResult {
        private String task;
        private int totalRecords;
        private int successRecords;
        private boolean success;

        public AddHostIdResult(String task) {
            this.task = task;
        }
    }

}
