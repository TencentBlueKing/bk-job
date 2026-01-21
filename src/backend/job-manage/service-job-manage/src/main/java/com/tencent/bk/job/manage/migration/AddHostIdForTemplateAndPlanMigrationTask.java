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

package com.tencent.bk.job.manage.migration;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.constant.TaskVariableTypeEnum;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.model.dto.ApplicationHostDTO;
import com.tencent.bk.job.common.mysql.JobTransactional;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.manage.model.dto.task.TaskHostNodeDTO;
import com.tencent.bk.job.manage.model.dto.task.TaskTargetDTO;
import com.tencent.bk.job.manage.model.migration.AddHostIdResult;
import com.tencent.bk.job.manage.model.tables.TaskPlan;
import com.tencent.bk.job.manage.model.tables.TaskPlanStep;
import com.tencent.bk.job.manage.model.tables.TaskPlanStepFile;
import com.tencent.bk.job.manage.model.tables.TaskPlanStepFileList;
import com.tencent.bk.job.manage.model.tables.TaskPlanStepScript;
import com.tencent.bk.job.manage.model.tables.TaskPlanVariable;
import com.tencent.bk.job.manage.model.tables.TaskTemplate;
import com.tencent.bk.job.manage.model.tables.TaskTemplateStep;
import com.tencent.bk.job.manage.model.tables.TaskTemplateStepFile;
import com.tencent.bk.job.manage.model.tables.TaskTemplateStepFileList;
import com.tencent.bk.job.manage.model.tables.TaskTemplateStepScript;
import com.tencent.bk.job.manage.model.tables.TaskTemplateVariable;
import com.tencent.bk.job.manage.service.host.TenantHostService;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Result;
import org.jooq.types.UByte;
import org.jooq.types.ULong;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 作业模板、执行方案等包含的主机数据，在原来的云区域+ip的基础上，填充hostID属性
 */
@Service
@Slf4j
public class AddHostIdForTemplateAndPlanMigrationTask {
    private final DSLContext CTX;
    private final TenantHostService tenantHostService;

    private static final TaskTemplate TASK_TEMPLATE = TaskTemplate.TASK_TEMPLATE;
    private static final TaskPlan TASK_PLAN = TaskPlan.TASK_PLAN;
    private static final TaskTemplateStep TASK_TEMPLATE_STEP = TaskTemplateStep.TASK_TEMPLATE_STEP;
    private static final TaskPlanStep TASK_PLAN_STEP = TaskPlanStep.TASK_PLAN_STEP;

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
    public AddHostIdForTemplateAndPlanMigrationTask(@Qualifier("job-manage-dsl-context") DSLContext ctx,
                                                    TenantHostService tenantHostService) {
        CTX = ctx;
        this.tenantHostService = tenantHostService;
    }

    public List<AddHostIdResult> execute(List<Long> appIdList, boolean isDryRun) {
        List<AddHostIdResult> results = new ArrayList<>();
        try {
            results.add(new TaskTemplateStepScriptTargetMigration(appIdList, isDryRun).migrate());
            results.add(new TaskTemplateStepFileTargetMigration(appIdList, isDryRun).migrate());
            results.add(new TaskTemplateStepFileListTargetMigration(appIdList, isDryRun).migrate());
            results.add(new TaskTemplateVariableTargetMigration(appIdList, isDryRun).migrate());
            results.add(new TaskPlanStepScriptTargetMigration(appIdList, isDryRun).migrate());
            results.add(new TaskPlanStepFileTargetMigration(appIdList, isDryRun).migrate());
            results.add(new TaskPlanStepFileListTargetMigration(appIdList, isDryRun).migrate());
            results.add(new TaskPlanVariableTargetMigration(appIdList, isDryRun).migrate());
            return results;
        } finally {
            log.info("AddHostIdMigrationTask done, result: {}", JsonUtils.toJson(results));
            // 清理缓存，避免占用内存
            this.ipAndHostIdMapping.clear();
        }
    }

    public void addIpAndHostIdMappings(Collection<TaskTargetDTO> targets) {
        Set<String> notCachedCloudIps = new HashSet<>();
        for (TaskTargetDTO target : targets) {
            TaskHostNodeDTO hostNodeList = target.getHostNodeList();
            if (hostNodeList == null) {
                continue;
            }
            List<ApplicationHostDTO> hostList = hostNodeList.getHostList();
            if (CollectionUtils.isEmpty(hostList)) {
                continue;
            }
            hostList.forEach(host -> {
                String cloudIp = host.getCloudIp();
                if (ipAndHostIdMapping.get(cloudIp) == null) {
                    notCachedCloudIps.add(cloudIp);
                }
            });
        }

        if (CollectionUtils.isNotEmpty(notCachedCloudIps)) {
            try {
                // 多租户版本下该迁移任务默认不支持，需要支持时再开发这里
                Map<String, ApplicationHostDTO> hosts = tenantHostService.listHostsByIps(null,notCachedCloudIps);
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

    public boolean fillHostId(TaskTargetDTO target) {
        boolean success = true;
        TaskHostNodeDTO hostNodeList = target.getHostNodeList();
        if (hostNodeList == null) {
            return false;
        }
        List<ApplicationHostDTO> hostList = hostNodeList.getHostList();
        if (CollectionUtils.isEmpty(hostList)) {
            return false;
        }
        for (ApplicationHostDTO host : hostList) {
            String cloudIp = host.getCloudIp();
            Long hostId = ipAndHostIdMapping.get(cloudIp);
            if (hostId != null) {
                host.setHostId(hostId);
            } else {
                success = false;
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

    @SuppressWarnings("SameParameterValue")
    private abstract class AbstractTaskTargetMigration {
        private final List<Long> appIdList;
        private final boolean dryRun;

        AbstractTaskTargetMigration(List<Long> appIdList, boolean dryRun) {
            this.appIdList = appIdList;
            this.dryRun = dryRun;
        }

        public AddHostIdResult migrate() {
            String taskName = "migrate_" + getTableName();
            AddHostIdResult result = new AddHostIdResult(taskName);
            StopWatch watch = new StopWatch(taskName);
            TaskTargetRecordPageResult pageResult = null;
            int seq = 0;
            int totalCount = 0;
            int successCount = 0;
            try {
                log.info("[{}] Migration start ...", getTableName());
                do {
                    seq++;
                    watch.start("list_targets_" + seq);
                    pageResult = listPageTaskTargets(appIdList, pageResult == null ? null :
                        pageResult.getNextRecordId(), 500);
                    if (CollectionUtils.isEmpty(pageResult.getRecords())) {
                        watch.stop();
                        continue;
                    }

                    List<TaskTargetRecord> records = pageResult.getRecords();
                    records = records.stream().filter(record -> isTargetMissingHostId(record.getTarget()))
                        .collect(Collectors.toList());
                    if (records.isEmpty()) {
                        watch.stop();
                        continue;
                    }
                    totalCount += records.size();
                    log.info("[{}-{}] {} targets need migration", getTableName(), seq, records.size());
                    watch.stop();


                    watch.start("add_ip_host_id_mappings_" + seq);
                    addIpAndHostIdMappings(
                        records.stream().map(TaskTargetRecord::getTarget).collect(Collectors.toList()));
                    log.info("[{}-{}] ipAndHostIdMappings: {}", getTableName(), seq,
                        JsonUtils.toJson(ipAndHostIdMapping));
                    watch.stop();

                    successCount += fillHostIdAndUpdateTaskTargets(records, watch, seq);
                } while (pageResult.hasNext());

                result.setSuccess(true);
            } catch (Throwable e) {
                // catch all exception
                log.error("Migration caught exception", e);
                result.setSuccess(false);
                return result;
            } finally {
                result.setTotalRecords(totalCount);
                result.setSuccessRecords(successCount);
                if (watch.isRunning()) {
                    watch.stop();
                }
                log.info("[{}] Migration done. cost: {}", getTableName(), watch.prettyPrint());
            }
            return result;
        }

        private int fillHostIdAndUpdateTaskTargets(List<TaskTargetRecord> records, StopWatch watch, int seq) {
            watch.start("fill_host_id_" + seq);
            List<Long> invalidIds = new ArrayList<>();
            List<TaskTargetRecord> updateRecords = new ArrayList<>();
            for (TaskTargetRecord record : records) {
                boolean success = fillHostId(record.getTarget());
                if (!success) {
                    invalidIds.add(record.getId());
                } else {
                    updateRecords.add(record);
                }
            }
            if (CollectionUtils.isNotEmpty(invalidIds)) {
                log.error("[{}-{}] {} targets fill host id fail, invalidIdList: {}", getTableName(), seq,
                    invalidIds.size(), invalidIds);
            }
            watch.stop();

            if (CollectionUtils.isNotEmpty(updateRecords)) {
                watch.start("update_targets_" + seq);
                if (dryRun) {
                    // 只输出要更新的数据用于验证，不进行DB数据的变更
                    log.info("Update targets with dryRun mode, tableName: {},  records: {}", getTableName(),
                        JsonUtils.toJson(updateRecords));
                } else {
                    updateTaskTargets(updateRecords);
                }
                watch.stop();
                return updateRecords.size();
            }
            return 0;
        }

        /**
         * 查询所有需要处理的TaskTarget
         *
         * @param appIdList     限定的Job业务ID列表;传入null表示所有业务
         * @param nextRecordId  下一个记录ID;传入null表示第一页
         * @param maxRecordSize 分页-最大返回记录数量
         * @return 分页结果
         */
        private TaskTargetRecordPageResult listPageTaskTargets(List<Long> appIdList,
                                                               Long nextRecordId,
                                                               int maxRecordSize) {
            long fromId = nextRecordId == null ? 0L : nextRecordId;
            // 比单页最大数量多获取一条数据，用于判断是否还有下一页
            int limit = maxRecordSize + 1;
            List<TaskTargetRecord> records = listTaskTargets(appIdList, fromId, limit);

            if (records.size() == limit) {
                // 下一页仍然有内容
                return new TaskTargetRecordPageResult(records.subList(0, records.size() - 1),
                    records.get(records.size() - 1).getId());
            } else {
                // <= maxRecordSize, 表示当前已经是最后一页
                return new TaskTargetRecordPageResult(records, null);
            }
        }

        /**
         * 获取表名称
         *
         * @return 表名称
         */
        abstract String getTableName();

        /**
         * 从DB查询 TaskTarget
         *
         * @param appIdList     Job业务ID列表
         * @param fromRecordId  起始记录ID
         * @param maxRecordSize 最大返回记录数量
         * @return TaskTarget 列表
         */
        abstract List<TaskTargetRecord> listTaskTargets(List<Long> appIdList, Long fromRecordId, int maxRecordSize);

        /**
         * 批量更新Target
         *
         * @param records 更新目标主机
         */
        abstract void updateTaskTargets(List<TaskTargetRecord> records);
    }

    @Data
    @NoArgsConstructor
    private static class TaskTargetRecordPageResult {

        public TaskTargetRecordPageResult(List<TaskTargetRecord> records, Long nextRecordId) {
            this.records = records;
            this.nextRecordId = nextRecordId;
        }

        private List<TaskTargetRecord> records;
        private Long nextRecordId;

        public boolean hasNext() {
            return nextRecordId != null;
        }
    }

    @Data
    @NoArgsConstructor
    private static class TaskTargetRecord {

        public TaskTargetRecord(Long id, TaskTargetDTO target) {
            this.id = id;
            this.target = target;
        }

        /**
         * 包含目标主机的记录ID
         */
        private Long id;
        /**
         * 目标主机
         */
        private TaskTargetDTO target;
    }

    private class TaskTemplateStepScriptTargetMigration extends AbstractTaskTargetMigration {

        TaskTemplateStepScriptTargetMigration(List<Long> appIdList, boolean dryRun) {
            super(appIdList, dryRun);
        }

        @Override
        public String getTableName() {
            return "task_template_step_script";
        }

        @Override
        public List<TaskTargetRecord> listTaskTargets(List<Long> appIdList, Long fromRecordId, int maxRecordSize) {
            List<Condition> conditions = new ArrayList<>();
            conditions.add(TASK_TEMPLATE_STEP_SCRIPT.ID.ge(ULong.valueOf(fromRecordId)));
            if (appIdList != null) {
                conditions.add(TASK_TEMPLATE.APP_ID.in(appIdList));
            }
            Result<?> result = CTX.select(
                TASK_TEMPLATE_STEP_SCRIPT.ID,
                TASK_TEMPLATE_STEP_SCRIPT.DESTINATION_HOST_LIST)
                .from(TASK_TEMPLATE_STEP_SCRIPT)
                .join(TASK_TEMPLATE_STEP)
                .on(TASK_TEMPLATE_STEP_SCRIPT.STEP_ID.eq(TASK_TEMPLATE_STEP.ID))
                .join(TASK_TEMPLATE)
                .on(TASK_TEMPLATE_STEP.TEMPLATE_ID.eq(TASK_TEMPLATE.ID))
                .where(conditions)
                .orderBy(TASK_TEMPLATE_STEP_SCRIPT.ID.asc())
                .limit(maxRecordSize)
                .fetch();
            return result.map(record -> {
                long id = record.get(TASK_TEMPLATE_STEP_SCRIPT.ID).longValue();
                String targetJsonStr = record.get(TASK_TEMPLATE_STEP_SCRIPT.DESTINATION_HOST_LIST);
                TaskTargetDTO target = JsonUtils.fromJson(targetJsonStr, TaskTargetDTO.class);
                return new TaskTargetRecord(id, target);
            });
        }

        @Override
        @JobTransactional(transactionManager = "jobManageTransactionManager")
        public void updateTaskTargets(List<TaskTargetRecord> records) {
            if (CollectionUtils.isEmpty(records)) {
                return;
            }
            records.forEach(record ->
                CTX.update(TASK_TEMPLATE_STEP_SCRIPT)
                    .set(TASK_TEMPLATE_STEP_SCRIPT.DESTINATION_HOST_LIST, convertTargetToJson(record.getTarget()))
                    .where(TASK_TEMPLATE_STEP_SCRIPT.ID.eq(ULong.valueOf(record.getId())))
                    .execute());
        }
    }

    private class TaskTemplateStepFileTargetMigration extends AbstractTaskTargetMigration {

        TaskTemplateStepFileTargetMigration(List<Long> appIdList, boolean dryRun) {
            super(appIdList, dryRun);
        }

        @Override
        public String getTableName() {
            return "task_template_step_file";
        }

        @Override
        public List<TaskTargetRecord> listTaskTargets(List<Long> appIdList, Long fromRecordId, int maxRecordSize) {
            List<Condition> conditions = new ArrayList<>();
            conditions.add(TASK_TEMPLATE_STEP_FILE.ID.ge(ULong.valueOf(fromRecordId)));
            if (appIdList != null) {
                conditions.add(TASK_TEMPLATE.APP_ID.in(appIdList));
            }
            Result<?> result = CTX.select(
                TASK_TEMPLATE_STEP_FILE.ID,
                TASK_TEMPLATE_STEP_FILE.DESTINATION_HOST_LIST)
                .from(TASK_TEMPLATE_STEP_FILE)
                .join(TASK_TEMPLATE_STEP)
                .on(TASK_TEMPLATE_STEP_FILE.STEP_ID.eq(TASK_TEMPLATE_STEP.ID))
                .join(TASK_TEMPLATE)
                .on(TASK_TEMPLATE_STEP.TEMPLATE_ID.eq(TASK_TEMPLATE.ID))
                .where(conditions)
                .orderBy(TASK_TEMPLATE_STEP_FILE.ID.asc())
                .limit(maxRecordSize)
                .fetch();
            return result.map(record -> {
                long id = record.get(TASK_TEMPLATE_STEP_FILE.ID).longValue();
                String targetJsonStr = record.get(TASK_TEMPLATE_STEP_FILE.DESTINATION_HOST_LIST);
                TaskTargetDTO target = JsonUtils.fromJson(targetJsonStr, TaskTargetDTO.class);
                return new TaskTargetRecord(id, target);
            });
        }

        @Override
        public void updateTaskTargets(List<TaskTargetRecord> records) {
            if (CollectionUtils.isEmpty(records)) {
                return;
            }
            records.forEach(record ->
                CTX.update(TASK_TEMPLATE_STEP_FILE)
                    .set(TASK_TEMPLATE_STEP_FILE.DESTINATION_HOST_LIST, convertTargetToJson(record.getTarget()))
                    .where(TASK_TEMPLATE_STEP_FILE.ID.eq(ULong.valueOf(record.getId())))
                    .execute());
        }
    }

    private class TaskTemplateStepFileListTargetMigration extends AbstractTaskTargetMigration {

        TaskTemplateStepFileListTargetMigration(List<Long> appIdList, boolean dryRun) {
            super(appIdList, dryRun);
        }

        @Override
        public String getTableName() {
            return "task_template_step_file_list";
        }

        @Override
        public List<TaskTargetRecord> listTaskTargets(List<Long> appIdList, Long fromRecordId, int maxRecordSize) {
            List<Condition> conditions = new ArrayList<>();
            conditions.add(TASK_TEMPLATE_STEP_FILE_LIST.ID.ge(ULong.valueOf(fromRecordId)));
            if (appIdList != null) {
                conditions.add(TASK_TEMPLATE.APP_ID.in(appIdList));
            }
            Result<?> result = CTX.select(
                TASK_TEMPLATE_STEP_FILE_LIST.ID,
                TASK_TEMPLATE_STEP_FILE_LIST.HOST)
                .from(TASK_TEMPLATE_STEP_FILE_LIST)
                .join(TASK_TEMPLATE_STEP)
                .on(TASK_TEMPLATE_STEP_FILE_LIST.STEP_ID.eq(TASK_TEMPLATE_STEP.ID))
                .join(TASK_TEMPLATE)
                .on(TASK_TEMPLATE_STEP.TEMPLATE_ID.eq(TASK_TEMPLATE.ID))
                .where(conditions)
                .orderBy(TASK_TEMPLATE_STEP_FILE_LIST.ID.asc())
                .limit(maxRecordSize)
                .fetch();
            return result.map(record -> {
                long id = record.get(TASK_TEMPLATE_STEP_FILE_LIST.ID).longValue();
                String targetJsonStr = record.get(TASK_TEMPLATE_STEP_FILE_LIST.HOST);
                TaskTargetDTO target = JsonUtils.fromJson(targetJsonStr, TaskTargetDTO.class);
                return new TaskTargetRecord(id, target);
            });
        }

        @Override
        public void updateTaskTargets(List<TaskTargetRecord> records) {
            if (CollectionUtils.isEmpty(records)) {
                return;
            }
            records.forEach(record ->
                CTX.update(TASK_TEMPLATE_STEP_FILE_LIST)
                    .set(TASK_TEMPLATE_STEP_FILE_LIST.HOST, convertTargetToJson(record.getTarget()))
                    .where(TASK_TEMPLATE_STEP_FILE_LIST.ID.eq(ULong.valueOf(record.getId())))
                    .execute());
        }
    }

    private class TaskTemplateVariableTargetMigration extends AbstractTaskTargetMigration {

        TaskTemplateVariableTargetMigration(List<Long> appIdList, boolean dryRun) {
            super(appIdList, dryRun);
        }

        @Override
        public String getTableName() {
            return "task_template_variable";
        }

        @Override
        public List<TaskTargetRecord> listTaskTargets(List<Long> appIdList, Long fromRecordId, int maxRecordSize) {
            List<Condition> conditions = new ArrayList<>();
            conditions.add(TASK_TEMPLATE_VARIABLE.TYPE.eq(UByte.valueOf(TaskVariableTypeEnum.HOST_LIST.getType())));
            if (appIdList != null) {
                conditions.add(TASK_TEMPLATE.APP_ID.in(appIdList));
            }
            Result<?> result = CTX.select(
                TASK_TEMPLATE_VARIABLE.ID,
                TASK_TEMPLATE_VARIABLE.DEFAULT_VALUE)
                .from(TASK_TEMPLATE_VARIABLE)
                .join(TASK_TEMPLATE)
                .on(TASK_TEMPLATE_VARIABLE.TEMPLATE_ID.eq(TASK_TEMPLATE.ID))
                .where(conditions)
                .and(TASK_TEMPLATE_VARIABLE.ID.ge(ULong.valueOf(fromRecordId)))
                .orderBy(TASK_TEMPLATE_VARIABLE.ID.asc())
                .limit(maxRecordSize)
                .fetch();
            return result.map(record -> {
                long id = record.get(TASK_TEMPLATE_VARIABLE.ID).longValue();
                String targetJsonStr = record.get(TASK_TEMPLATE_VARIABLE.DEFAULT_VALUE);
                TaskTargetDTO target = JsonUtils.fromJson(targetJsonStr, TaskTargetDTO.class);
                return new TaskTargetRecord(id, target);
            });
        }

        @Override
        public void updateTaskTargets(List<TaskTargetRecord> records) {
            if (CollectionUtils.isEmpty(records)) {
                return;
            }
            records.forEach(record ->
                CTX.update(TASK_TEMPLATE_VARIABLE)
                    .set(TASK_TEMPLATE_VARIABLE.DEFAULT_VALUE, convertTargetToJson(record.getTarget()))
                    .where(TASK_TEMPLATE_VARIABLE.ID.eq(ULong.valueOf(record.getId())))
                    .and(TASK_TEMPLATE_VARIABLE.TYPE.eq(UByte.valueOf(TaskVariableTypeEnum.HOST_LIST.getType())))
                    .execute());
        }
    }


    private class TaskPlanStepScriptTargetMigration extends AbstractTaskTargetMigration {

        public TaskPlanStepScriptTargetMigration(List<Long> appIdList, boolean dryRun) {
            super(appIdList, dryRun);
        }

        @Override
        public String getTableName() {
            return "TaskPlanStepScript";
        }

        @Override
        public List<TaskTargetRecord> listTaskTargets(List<Long> appIdList, Long fromRecordId, int maxRecordSize) {
            List<Condition> conditions = new ArrayList<>();
            conditions.add(TASK_PLAN_STEP_SCRIPT.ID.ge(ULong.valueOf(fromRecordId)));
            if (appIdList != null) {
                conditions.add(TASK_PLAN.APP_ID.in(appIdList));
            }
            Result<?> result = CTX.select(
                TASK_PLAN_STEP_SCRIPT.ID,
                TASK_PLAN_STEP_SCRIPT.DESTINATION_HOST_LIST)
                .from(TASK_PLAN_STEP_SCRIPT)
                .join(TASK_PLAN_STEP)
                .on(TASK_PLAN_STEP_SCRIPT.STEP_ID.eq(TASK_PLAN_STEP.ID))
                .join(TASK_PLAN)
                .on(TASK_PLAN_STEP.PLAN_ID.eq(TASK_PLAN.ID))
                .where(conditions)
                .orderBy(TASK_PLAN_STEP_SCRIPT.ID.asc())
                .limit(maxRecordSize)
                .fetch();
            return result.map(record -> {
                long id = record.get(TASK_PLAN_STEP_SCRIPT.ID).longValue();
                String targetJsonStr = record.get(TASK_PLAN_STEP_SCRIPT.DESTINATION_HOST_LIST);
                TaskTargetDTO target = JsonUtils.fromJson(targetJsonStr, TaskTargetDTO.class);
                return new TaskTargetRecord(id, target);
            });
        }

        @Override
        @JobTransactional(transactionManager = "jobManageTransactionManager")
        public void updateTaskTargets(List<TaskTargetRecord> records) {
            if (CollectionUtils.isEmpty(records)) {
                return;
            }
            records.forEach(record ->
                CTX.update(TASK_PLAN_STEP_SCRIPT)
                    .set(TASK_PLAN_STEP_SCRIPT.DESTINATION_HOST_LIST, convertTargetToJson(record.getTarget()))
                    .where(TASK_PLAN_STEP_SCRIPT.ID.eq(ULong.valueOf(record.getId())))
                    .execute());
        }
    }

    private class TaskPlanStepFileTargetMigration extends AbstractTaskTargetMigration {

        TaskPlanStepFileTargetMigration(List<Long> appIdList, boolean dryRun) {
            super(appIdList, dryRun);
        }

        @Override
        public String getTableName() {
            return "task_plan_step_file";
        }

        @Override
        public List<TaskTargetRecord> listTaskTargets(List<Long> appIdList, Long fromRecordId, int maxRecordSize) {
            List<Condition> conditions = new ArrayList<>();
            conditions.add(TASK_PLAN_STEP_FILE.ID.ge(ULong.valueOf(fromRecordId)));
            if (appIdList != null) {
                conditions.add(TASK_PLAN.APP_ID.in(appIdList));
            }
            Result<?> result = CTX.select(
                TASK_PLAN_STEP_FILE.ID,
                TASK_PLAN_STEP_FILE.DESTINATION_HOST_LIST)
                .from(TASK_PLAN_STEP_FILE)
                .join(TASK_PLAN_STEP)
                .on(TASK_PLAN_STEP_FILE.STEP_ID.eq(TASK_PLAN_STEP.ID))
                .join(TASK_PLAN)
                .on(TASK_PLAN_STEP.PLAN_ID.eq(TASK_PLAN.ID))
                .where(conditions)
                .orderBy(TASK_PLAN_STEP_FILE.ID.asc())
                .limit(maxRecordSize)
                .fetch();
            return result.map(record -> {
                long id = record.get(TASK_PLAN_STEP_FILE.ID).longValue();
                String targetJsonStr = record.get(TASK_PLAN_STEP_FILE.DESTINATION_HOST_LIST);
                TaskTargetDTO target = JsonUtils.fromJson(targetJsonStr, TaskTargetDTO.class);
                return new TaskTargetRecord(id, target);
            });
        }

        @Override
        public void updateTaskTargets(List<TaskTargetRecord> records) {
            if (CollectionUtils.isEmpty(records)) {
                return;
            }
            records.forEach(record ->
                CTX.update(TASK_PLAN_STEP_FILE)
                    .set(TASK_PLAN_STEP_FILE.DESTINATION_HOST_LIST, convertTargetToJson(record.getTarget()))
                    .where(TASK_PLAN_STEP_FILE.ID.eq(ULong.valueOf(record.getId())))
                    .execute());
        }
    }

    private class TaskPlanStepFileListTargetMigration extends AbstractTaskTargetMigration {

        TaskPlanStepFileListTargetMigration(List<Long> appIdList, boolean dryRun) {
            super(appIdList, dryRun);
        }

        @Override
        public String getTableName() {
            return "task_plan_step_file_list";
        }

        @Override
        public List<TaskTargetRecord> listTaskTargets(List<Long> appIdList, Long fromRecordId, int maxRecordSize) {
            List<Condition> conditions = new ArrayList<>();
            conditions.add(TASK_PLAN_STEP_FILE_LIST.ID.ge(ULong.valueOf(fromRecordId)));
            if (appIdList != null) {
                conditions.add(TASK_PLAN.APP_ID.in(appIdList));
            }
            Result<?> result = CTX.select(
                TASK_PLAN_STEP_FILE_LIST.ID,
                TASK_PLAN_STEP_FILE_LIST.HOST)
                .from(TASK_PLAN_STEP_FILE_LIST)
                .join(TASK_PLAN_STEP)
                .on(TASK_PLAN_STEP_FILE_LIST.STEP_ID.eq(TASK_PLAN_STEP.ID))
                .join(TASK_PLAN)
                .on(TASK_PLAN_STEP.PLAN_ID.eq(TASK_PLAN.ID))
                .where(conditions)
                .orderBy(TASK_PLAN_STEP_FILE_LIST.ID.asc())
                .limit(maxRecordSize)
                .fetch();
            return result.map(record -> {
                long id = record.get(TASK_PLAN_STEP_FILE_LIST.ID).longValue();
                String targetJsonStr = record.get(TASK_PLAN_STEP_FILE_LIST.HOST);
                TaskTargetDTO target = JsonUtils.fromJson(targetJsonStr, TaskTargetDTO.class);
                return new TaskTargetRecord(id, target);
            });
        }

        @Override
        public void updateTaskTargets(List<TaskTargetRecord> records) {
            if (CollectionUtils.isEmpty(records)) {
                return;
            }
            records.forEach(record ->
                CTX.update(TASK_PLAN_STEP_FILE_LIST)
                    .set(TASK_PLAN_STEP_FILE_LIST.HOST, convertTargetToJson(record.getTarget()))
                    .where(TASK_PLAN_STEP_FILE_LIST.ID.eq(ULong.valueOf(record.getId())))
                    .execute());
        }
    }

    private class TaskPlanVariableTargetMigration extends AbstractTaskTargetMigration {

        TaskPlanVariableTargetMigration(List<Long> appIdList, boolean dryRun) {
            super(appIdList, dryRun);
        }

        @Override
        public String getTableName() {
            return "task_plan_variable";
        }

        @Override
        public List<TaskTargetRecord> listTaskTargets(List<Long> appIdList, Long fromRecordId, int maxRecordSize) {
            List<Condition> conditions = new ArrayList<>();
            conditions.add(TASK_PLAN_VARIABLE.TYPE.eq(UByte.valueOf(TaskVariableTypeEnum.HOST_LIST.getType())));
            if (appIdList != null) {
                conditions.add(TASK_PLAN.APP_ID.in(appIdList));
            }
            Result<?> result = CTX.select(
                TASK_PLAN_VARIABLE.ID,
                TASK_PLAN_VARIABLE.DEFAULT_VALUE)
                .from(TASK_PLAN_VARIABLE)
                .join(TASK_PLAN)
                .on(TASK_PLAN_VARIABLE.PLAN_ID.eq(TASK_PLAN.ID))
                .where(conditions)
                .and(TASK_PLAN_VARIABLE.ID.ge(ULong.valueOf(fromRecordId)))
                .orderBy(TASK_PLAN_VARIABLE.ID.asc())
                .limit(maxRecordSize)
                .fetch();
            return result.map(record -> {
                long id = record.get(TASK_PLAN_VARIABLE.ID).longValue();
                String targetJsonStr = record.get(TASK_PLAN_VARIABLE.DEFAULT_VALUE);
                TaskTargetDTO target = JsonUtils.fromJson(targetJsonStr, TaskTargetDTO.class);
                return new TaskTargetRecord(id, target);
            });
        }

        @Override
        public void updateTaskTargets(List<TaskTargetRecord> records) {
            if (CollectionUtils.isEmpty(records)) {
                return;
            }
            records.forEach(record ->
                CTX.update(TASK_PLAN_VARIABLE)
                    .set(TASK_PLAN_VARIABLE.DEFAULT_VALUE, convertTargetToJson(record.getTarget()))
                    .where(TASK_PLAN_VARIABLE.ID.eq(ULong.valueOf(record.getId())))
                    .and(TASK_PLAN_VARIABLE.TYPE.eq(UByte.valueOf(TaskVariableTypeEnum.HOST_LIST.getType())))
                    .execute());
        }
    }

}
