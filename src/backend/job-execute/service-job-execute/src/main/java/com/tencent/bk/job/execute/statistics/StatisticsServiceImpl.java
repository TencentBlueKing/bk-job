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

package com.tencent.bk.job.execute.statistics;

import com.tencent.bk.job.common.constant.NotExistPathHandlerEnum;
import com.tencent.bk.job.common.statistics.consts.StatisticsConstants;
import com.tencent.bk.job.common.statistics.model.dto.StatisticsDTO;
import com.tencent.bk.job.common.util.TimeUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.config.StatisticConfig;
import com.tencent.bk.job.execute.dao.StatisticsDAO;
import com.tencent.bk.job.execute.dao.StepInstanceDAO;
import com.tencent.bk.job.execute.model.FileStepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.ApplicationService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.manage.common.consts.script.ScriptTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
public class StatisticsServiceImpl implements StatisticsService {

    private final Object writeLock = new Object();
    private final TaskInstanceService taskInstanceService;
    private final ApplicationService applicationService;
    private final StepInstanceDAO stepInstanceDAO;
    private final DSLContext dslContext;
    private final StatisticsDAO statisticsDAO;
    private final StatisticConfig statisticConfig;
    private volatile Map<String, Map<StatisticsKey, AtomicInteger>> incrementMap = new ConcurrentHashMap<>();
    private volatile LinkedBlockingQueue<Map<String, Map<StatisticsKey, AtomicInteger>>> flushQueue =
        new LinkedBlockingQueue<>(600);

    @Autowired
    public StatisticsServiceImpl(
        TaskInstanceService taskInstanceService,
        ApplicationService applicationService,
        StepInstanceDAO stepInstanceDAO,
        DSLContext dslContext,
        StatisticsDAO statisticsDAO,
        StatisticConfig statisticConfig
    ) {
        this.taskInstanceService = taskInstanceService;
        this.applicationService = applicationService;
        this.stepInstanceDAO = stepInstanceDAO;
        this.dslContext = dslContext;
        this.statisticsDAO = statisticsDAO;
        this.statisticConfig = statisticConfig;
    }

    @PostConstruct
    public void init() {
        StatisticsFlushThread flushThread = new StatisticsFlushThread(dslContext, flushQueue);
        flushThread.setName("flushThread");
        flushThread.start();
    }

    public void updateExecutedTaskCount(
        TaskInstanceDTO taskInstanceDTO,
        Map<StatisticsKey, AtomicInteger> metricsMap
    ) {
        // 累计任务执行次数统计
        StatisticsKey keyExecutedTaskCount = new StatisticsKey(taskInstanceDTO.getAppId(),
            StatisticsConstants.RESOURCE_EXECUTED_TASK, StatisticsConstants.DIMENSION_TIME_UNIT,
            StatisticsConstants.DIMENSION_VALUE_TIME_UNIT_DAY);
        AtomicInteger executedTaskCount = metricsMap.computeIfAbsent(keyExecutedTaskCount,
            statisticsKey -> new AtomicInteger(0));
        int executedTaskCountValue = executedTaskCount.incrementAndGet();
        log.debug("executedTaskCount={}", executedTaskCountValue);
    }

    public void updateExecutedTaskByStartupMode(TaskInstanceDTO taskInstanceDTO,
                                                Map<StatisticsKey, AtomicInteger> metricsMap) {
        // 按渠道统计
        Integer startupMode = taskInstanceDTO.getStartupMode();
        if (startupMode == null) {
            log.warn("startupMode is null, ignore");
            return;
        }
        if (startupMode == TaskStartupModeEnum.NORMAL.getValue()) {
            StatisticsKey keyStartupNormalTaskCount = new StatisticsKey(taskInstanceDTO.getAppId(),
                StatisticsConstants.RESOURCE_EXECUTED_TASK, StatisticsConstants.DIMENSION_TASK_STARTUP_MODE,
                StatisticsConstants.DIMENSION_VALUE_TASK_STARTUP_MODE_NORMAL);
            AtomicInteger startupNormalTaskCount = metricsMap.computeIfAbsent(keyStartupNormalTaskCount,
                statisticsKey -> new AtomicInteger(0));
            int startupNormalTaskCountValue = startupNormalTaskCount.incrementAndGet();
            log.debug("startupNormalTaskCount={}", startupNormalTaskCountValue);
        } else if (startupMode == TaskStartupModeEnum.API.getValue()) {
            StatisticsKey keyStartupApiTaskCount = new StatisticsKey(taskInstanceDTO.getAppId(),
                StatisticsConstants.RESOURCE_EXECUTED_TASK, StatisticsConstants.DIMENSION_TASK_STARTUP_MODE,
                StatisticsConstants.DIMENSION_VALUE_TASK_STARTUP_MODE_API);
            AtomicInteger startupApiTaskCount = metricsMap.computeIfAbsent(keyStartupApiTaskCount,
                statisticsKey -> new AtomicInteger(0));
            int startupApiTaskCountValue = startupApiTaskCount.incrementAndGet();
            log.debug("startupApiTaskCount={}", startupApiTaskCountValue);
        } else if (startupMode == TaskStartupModeEnum.CRON.getValue()) {
            StatisticsKey keyStartupCronTaskCount = new StatisticsKey(taskInstanceDTO.getAppId(),
                StatisticsConstants.RESOURCE_EXECUTED_TASK, StatisticsConstants.DIMENSION_TASK_STARTUP_MODE,
                StatisticsConstants.DIMENSION_VALUE_TASK_STARTUP_MODE_CRON);
            AtomicInteger startupCronTaskCount = metricsMap.computeIfAbsent(keyStartupCronTaskCount,
                statisticsKey -> new AtomicInteger(0));
            int startupCronTaskCountValue = startupCronTaskCount.incrementAndGet();
            log.debug("startupCronTaskCount={}", startupCronTaskCountValue);
        } else {
            log.warn("do not support startupMode {}, ignore", startupMode);
        }
    }

    public void updateExecutedTaskByType(TaskInstanceDTO taskInstanceDTO,
                                         Map<StatisticsKey, AtomicInteger> metricsMap) {
        // 按类型统计
        Integer type = taskInstanceDTO.getType();
        if (type == null) {
            log.warn("type is null, ignore");
            return;
        }
        if (type.equals(TaskTypeEnum.NORMAL.getValue())) {
            StatisticsKey keyTaskTypeNormalTaskCount = new StatisticsKey(taskInstanceDTO.getAppId(),
                StatisticsConstants.RESOURCE_EXECUTED_TASK, StatisticsConstants.DIMENSION_TASK_TYPE,
                StatisticsConstants.DIMENSION_VALUE_TASK_TYPE_EXECUTE_TASK);
            AtomicInteger taskTypeNormalTaskCount = metricsMap.computeIfAbsent(keyTaskTypeNormalTaskCount,
                statisticsKey -> new AtomicInteger(0));
            int taskTypeNormalTaskCountValue = taskTypeNormalTaskCount.incrementAndGet();
            log.debug("taskTypeNormalTaskCount={}", taskTypeNormalTaskCountValue);
        } else if (type.equals(TaskTypeEnum.SCRIPT.getValue())) {
            StatisticsKey keyTaskTypeScriptTaskCount = new StatisticsKey(taskInstanceDTO.getAppId(),
                StatisticsConstants.RESOURCE_EXECUTED_TASK, StatisticsConstants.DIMENSION_TASK_TYPE,
                StatisticsConstants.DIMENSION_VALUE_TASK_TYPE_FAST_EXECUTE_SCRIPT);
            AtomicInteger taskTypeScriptTaskCount = metricsMap.computeIfAbsent(keyTaskTypeScriptTaskCount,
                statisticsKey -> new AtomicInteger(0));
            int taskTypeScriptTaskCountValue = taskTypeScriptTaskCount.incrementAndGet();
            log.debug("taskTypeScriptTaskCount={}", taskTypeScriptTaskCountValue);
        } else if (type.equals(TaskTypeEnum.FILE.getValue())) {
            StatisticsKey keyTaskTypeFileTaskCount = new StatisticsKey(taskInstanceDTO.getAppId(),
                StatisticsConstants.RESOURCE_EXECUTED_TASK, StatisticsConstants.DIMENSION_TASK_TYPE,
                StatisticsConstants.DIMENSION_VALUE_TASK_TYPE_FAST_PUSH_FILE);
            AtomicInteger taskTypeFileTaskCount = metricsMap.computeIfAbsent(keyTaskTypeFileTaskCount,
                statisticsKey -> new AtomicInteger(0));
            int taskTypeFileTaskCountValue = taskTypeFileTaskCount.incrementAndGet();
            log.debug("taskTypeFileTaskCount={}", taskTypeFileTaskCountValue);
        } else {
            log.warn("do not support type {}, ignore", type);
        }
    }

    public void updateExecutedFastScriptStatistics(TaskInstanceDTO taskInstanceDTO,
                                                   Map<StatisticsKey, AtomicInteger> metricsMap) {
        Integer type = taskInstanceDTO.getType();
        if (type != null && type.equals(TaskTypeEnum.SCRIPT.getValue())) {
            // 查StepInstance
            Long stepInstanceId = stepInstanceDAO.getStepInstanceId(taskInstanceDTO.getId());
            if (stepInstanceId == null) {
                log.warn("Cannot find stepInstanceId by taskInstanceId {}", taskInstanceDTO.getId());
                return;
            }
            // 查StepInstanceScript
            Byte scriptType = stepInstanceDAO.getScriptTypeByStepInstanceId(stepInstanceId);
            // 更新统计数据
            // 快速执行脚本：按脚本类型统计
            String scriptTypeName = ScriptTypeEnum.getName(scriptType.intValue());
            StatisticsKey keyScriptTypeFastScriptCount = new StatisticsKey(taskInstanceDTO.getAppId(),
                StatisticsConstants.RESOURCE_EXECUTED_FAST_SCRIPT, StatisticsConstants.DIMENSION_SCRIPT_TYPE,
                StatisticsConstants.DIMENSION_VALUE_SCRIPT_TYPE_PREFIX + scriptTypeName);
            AtomicInteger scriptTypeFastScriptCount = metricsMap.computeIfAbsent(keyScriptTypeFastScriptCount,
                statisticsKey -> new AtomicInteger(0));
            int scriptTypeFastScriptCountValue = scriptTypeFastScriptCount.incrementAndGet();
            log.debug("scriptTypeFastScriptCount={}", scriptTypeFastScriptCountValue);
        }
    }

    public void updateExecutedFastFileStatistics(TaskInstanceDTO taskInstanceDTO,
                                                 Map<StatisticsKey, AtomicInteger> metricsMap) {
        Integer type = taskInstanceDTO.getType();
        if (type != null && type.equals(TaskTypeEnum.FILE.getValue())) {
            // 查StepInstance
            Long stepInstanceId = stepInstanceDAO.getStepInstanceId(taskInstanceDTO.getId());
            if (stepInstanceId == null) {
                log.warn("Cannot find stepInstanceId by taskInstanceId {}", taskInstanceDTO.getId());
                return;
            }
            // 查StepInstanceFile
            FileStepInstanceDTO fileStepInstanceDTO = stepInstanceDAO.getFileStepInstance(stepInstanceId);
            // 更新统计数据
            // 快速分发文件：按传输模式统计
            Integer notExistPathHandler = fileStepInstanceDTO.getNotExistPathHandler();
            if (notExistPathHandler == null
                || notExistPathHandler.equals(NotExistPathHandlerEnum.CREATE_DIR.getValue())) {
                // 强制模式
                StatisticsKey keyForceFileCount = new StatisticsKey(taskInstanceDTO.getAppId(),
                    StatisticsConstants.RESOURCE_EXECUTED_FAST_FILE, StatisticsConstants.DIMENSION_FILE_TRANSFER_MODE
                    , StatisticsConstants.DIMENSION_VALUE_FILE_TRANSFER_MODE_FORCE);
                AtomicInteger forceFileCount = metricsMap.computeIfAbsent(keyForceFileCount,
                    statisticsKey -> new AtomicInteger(0));
                int forceFileCountValue = forceFileCount.incrementAndGet();
                log.debug("forceFileCount={}", forceFileCountValue);
            } else {
                // 严谨模式
                StatisticsKey keyStrictFileCount = new StatisticsKey(taskInstanceDTO.getAppId(),
                    StatisticsConstants.RESOURCE_EXECUTED_FAST_FILE, StatisticsConstants.DIMENSION_FILE_TRANSFER_MODE
                    , StatisticsConstants.DIMENSION_VALUE_FILE_TRANSFER_MODE_STRICT);
                AtomicInteger strictFileCount = metricsMap.computeIfAbsent(keyStrictFileCount,
                    statisticsKey -> new AtomicInteger(0));
                int strictFileCountValue = strictFileCount.incrementAndGet();
                log.debug("strictFileCount={}", strictFileCountValue);
            }
        }
    }

    public void updateFailedTaskCount(TaskInstanceDTO taskInstanceDTO, Map<StatisticsKey, AtomicInteger> metricsMap) {
        // 累计执行失败次数统计
        if (RunStatusEnum.FAIL.getValue().equals(taskInstanceDTO.getStatus())) {
            StatisticsKey keyFailedTaskCount = new StatisticsKey(taskInstanceDTO.getAppId(),
                StatisticsConstants.RESOURCE_FAILED_TASK, StatisticsConstants.DIMENSION_TIME_UNIT,
                StatisticsConstants.DIMENSION_VALUE_TIME_UNIT_DAY);
            AtomicInteger failedTaskCount = metricsMap.computeIfAbsent(keyFailedTaskCount,
                statisticsKey -> new AtomicInteger(0));
            int failedTaskCountValue = failedTaskCount.incrementAndGet();
            log.debug("failedTaskCount={}", failedTaskCountValue);
        }
    }

    public void updateFastScriptCountByStatus(TaskInstanceDTO taskInstanceDTO,
                                              Map<StatisticsKey, AtomicInteger> metricsMap) {
        // 快速脚本按状态统计
        Integer type = taskInstanceDTO.getType();
        if (type != null && type.equals(TaskTypeEnum.SCRIPT.getValue())) {
            if (RunStatusEnum.SUCCESS.getValue().equals(taskInstanceDTO.getStatus())) {
                // 执行成功的快速脚本统计
                StatisticsKey keySuccessFastScriptCount = new StatisticsKey(taskInstanceDTO.getAppId(),
                    StatisticsConstants.RESOURCE_EXECUTED_FAST_SCRIPT, StatisticsConstants.DIMENSION_STEP_RUN_STATUS,
                    StatisticsConstants.DIMENSION_VALUE_STEP_RUN_STATUS_SUCCESS);
                AtomicInteger successFastScriptCount = metricsMap.computeIfAbsent(keySuccessFastScriptCount,
                    statisticsKey -> new AtomicInteger(0));
                int successFastScriptCountValue = successFastScriptCount.incrementAndGet();
                log.debug("successFastScriptCount={}", successFastScriptCountValue);
            } else if (RunStatusEnum.FAIL.getValue().equals(taskInstanceDTO.getStatus())) {
                // 执行失败的快速脚本统计
                StatisticsKey keyFailedFastScriptCount = new StatisticsKey(taskInstanceDTO.getAppId(),
                    StatisticsConstants.RESOURCE_EXECUTED_FAST_SCRIPT, StatisticsConstants.DIMENSION_STEP_RUN_STATUS,
                    StatisticsConstants.DIMENSION_VALUE_STEP_RUN_STATUS_FAIL);
                AtomicInteger failedFastScriptCount = metricsMap.computeIfAbsent(keyFailedFastScriptCount,
                    statisticsKey -> new AtomicInteger(0));
                int failedFastScriptCountValue = failedFastScriptCount.incrementAndGet();
                log.debug("failedFastScriptCount={}", failedFastScriptCountValue);
            } else if (RunStatusEnum.ABNORMAL_STATE.getValue().equals(taskInstanceDTO.getStatus())) {
                // 状态异常的快速脚本统计
                StatisticsKey keyExceptionFastScriptCount = new StatisticsKey(taskInstanceDTO.getAppId(),
                    StatisticsConstants.RESOURCE_EXECUTED_FAST_SCRIPT, StatisticsConstants.DIMENSION_STEP_RUN_STATUS,
                    StatisticsConstants.DIMENSION_VALUE_STEP_RUN_STATUS_EXCEPTION);
                AtomicInteger exceptionFastScriptCount = metricsMap.computeIfAbsent(keyExceptionFastScriptCount,
                    statisticsKey -> new AtomicInteger(0));
                int exceptionFastScriptCountValue = exceptionFastScriptCount.incrementAndGet();
                log.debug("exceptionFastScriptCount={}", exceptionFastScriptCountValue);
            }
        }
    }

    public void updateFastFileCountByStatus(TaskInstanceDTO taskInstanceDTO,
                                            Map<StatisticsKey, AtomicInteger> metricsMap) {
        // 快速文件按状态统计
        Integer type = taskInstanceDTO.getType();
        if (type != null && type.equals(TaskTypeEnum.FILE.getValue())) {
            if (RunStatusEnum.SUCCESS.getValue().equals(taskInstanceDTO.getStatus())) {
                // 执行成功的快速文件统计
                StatisticsKey keySuccessFastFileCount = new StatisticsKey(taskInstanceDTO.getAppId(),
                    StatisticsConstants.RESOURCE_EXECUTED_FAST_FILE, StatisticsConstants.DIMENSION_STEP_RUN_STATUS,
                    StatisticsConstants.DIMENSION_VALUE_STEP_RUN_STATUS_SUCCESS);
                AtomicInteger successFastFileCount = metricsMap.computeIfAbsent(keySuccessFastFileCount,
                    statisticsKey -> new AtomicInteger(0));
                int successFastFileCountValue = successFastFileCount.incrementAndGet();
                log.debug("successFastFileCount={}", successFastFileCountValue);
            } else if (RunStatusEnum.FAIL.getValue().equals(taskInstanceDTO.getStatus())) {
                // 执行失败的快速文件统计
                StatisticsKey keyFailedFastFileCount = new StatisticsKey(taskInstanceDTO.getAppId(),
                    StatisticsConstants.RESOURCE_EXECUTED_FAST_FILE, StatisticsConstants.DIMENSION_STEP_RUN_STATUS,
                    StatisticsConstants.DIMENSION_VALUE_STEP_RUN_STATUS_FAIL);
                AtomicInteger failedFastFileCount = metricsMap.computeIfAbsent(keyFailedFastFileCount,
                    statisticsKey -> new AtomicInteger(0));
                int failedFastFileCountValue = failedFastFileCount.incrementAndGet();
                log.debug("failedFastFileCount={}", failedFastFileCountValue);
            } else if (RunStatusEnum.ABNORMAL_STATE.getValue().equals(taskInstanceDTO.getStatus())) {
                // 执行失败的快速文件统计
                StatisticsKey keyExceptionFastFileCount = new StatisticsKey(taskInstanceDTO.getAppId(),
                    StatisticsConstants.RESOURCE_EXECUTED_FAST_FILE, StatisticsConstants.DIMENSION_STEP_RUN_STATUS,
                    StatisticsConstants.DIMENSION_VALUE_STEP_RUN_STATUS_EXCEPTION);
                AtomicInteger exceptionFastFileCount = metricsMap.computeIfAbsent(keyExceptionFastFileCount,
                    statisticsKey -> new AtomicInteger(0));
                int exceptionFastFileCountValue = exceptionFastFileCount.incrementAndGet();
                log.debug("exceptionFastFileCount={}", exceptionFastFileCountValue);
            }
        }
    }

    public void updateExecutedTaskByTimeConsuming(TaskInstanceDTO taskInstanceDTO,
                                                  Map<StatisticsKey, AtomicInteger> metricsMap) {
        // 按执行耗时统计
        Long totalTime = taskInstanceDTO.getTotalTime();
        if (totalTime == null) {
            log.warn("Unexpected:totalTime is null, taskInstanceId={}", taskInstanceDTO.getId());
            return;
        }
        if (totalTime < 60 * 1000) {
            // <1min
            StatisticsKey keyLessThanOneMinTaskCount = new StatisticsKey(taskInstanceDTO.getAppId(),
                StatisticsConstants.RESOURCE_EXECUTED_TASK, StatisticsConstants.DIMENSION_TASK_TIME_CONSUMING,
                StatisticsConstants.DIMENSION_VALUE_TASK_TIME_CONSUMING_LESS_THAN_ONE_MIN);
            AtomicInteger lessThanOneMinTaskCount = metricsMap.computeIfAbsent(keyLessThanOneMinTaskCount,
                statisticsKey -> new AtomicInteger(0));
            int lessThanOneMinTaskCountValue = lessThanOneMinTaskCount.incrementAndGet();
            log.debug("lessThanOneMinTaskCount={}", lessThanOneMinTaskCountValue);
        } else if (totalTime < 10 * 60 * 1000) {
            // 1min-10min
            StatisticsKey keyOneMinToTenMinTaskCount = new StatisticsKey(taskInstanceDTO.getAppId(),
                StatisticsConstants.RESOURCE_EXECUTED_TASK, StatisticsConstants.DIMENSION_TASK_TIME_CONSUMING,
                StatisticsConstants.DIMENSION_VALUE_TASK_TIME_CONSUMING_ONE_MIN_TO_TEN_MIN);
            AtomicInteger oneMinToTenMinTaskCount = metricsMap.computeIfAbsent(keyOneMinToTenMinTaskCount,
                statisticsKey -> new AtomicInteger(0));
            int oneMinToTenMinTaskCountValue = oneMinToTenMinTaskCount.incrementAndGet();
            log.debug("oneMinToTenMinTaskCount={}", oneMinToTenMinTaskCountValue);
        } else {
            // >=10min
            StatisticsKey keyOverTenMinTaskCount = new StatisticsKey(taskInstanceDTO.getAppId(),
                StatisticsConstants.RESOURCE_EXECUTED_TASK, StatisticsConstants.DIMENSION_TASK_TIME_CONSUMING,
                StatisticsConstants.DIMENSION_VALUE_TASK_TIME_CONSUMING_OVER_TEN_MIN);
            AtomicInteger overTenMinTaskCount = metricsMap.computeIfAbsent(keyOverTenMinTaskCount,
                statisticsKey -> new AtomicInteger(0));
            int overTenMinTaskCountValue = overTenMinTaskCount.incrementAndGet();
            log.debug("overTenMinTaskCount={}", overTenMinTaskCountValue);
        }
    }

    @Override
    public void updateStartJobStatistics(TaskInstanceDTO taskInstanceDTO) {
        String createDateStr = DateUtils.getDateStrFromUnixTimeMills(taskInstanceDTO.getCreateTime());
        synchronized (writeLock) {
            Map<StatisticsKey, AtomicInteger> metricsMap = incrementMap.computeIfAbsent(createDateStr,
                dateStr -> new ConcurrentHashMap<>());
            // 触发时间当天的数据统计
            // 累计任务执行次数统计
            updateExecutedTaskCount(taskInstanceDTO, metricsMap);
            // 按渠道统计
            updateExecutedTaskByStartupMode(taskInstanceDTO, metricsMap);
            // 按类型统计
            updateExecutedTaskByType(taskInstanceDTO, metricsMap);
            // 快速执行脚本：按脚本类型统计
            updateExecutedFastScriptStatistics(taskInstanceDTO, metricsMap);
            // 快速分发文件：按传输模式统计
            updateExecutedFastFileStatistics(taskInstanceDTO, metricsMap);
        }
    }

    @Override
    public void updateEndJobStatistics(TaskInstanceDTO taskInstanceDTO) {
        synchronized (writeLock) {
            String createDateStr = DateUtils.getDateStrFromUnixTimeMills(taskInstanceDTO.getCreateTime());
            Map<StatisticsKey, AtomicInteger> metricsMap = incrementMap.computeIfAbsent(createDateStr,
                dateStr -> new ConcurrentHashMap<>());
            // 触发时间当天的数据统计
            // 累计执行失败次数统计
            updateFailedTaskCount(taskInstanceDTO, metricsMap);
            // 快速执行脚本成功/失败/异常次数统计
            updateFastScriptCountByStatus(taskInstanceDTO, metricsMap);
            // 快速分发文件成功/失败/异常次数统计
            updateFastFileCountByStatus(taskInstanceDTO, metricsMap);
            // 按执行耗时统计
            updateExecutedTaskByTimeConsuming(taskInstanceDTO, metricsMap);
        }
    }

    @Override
    public StatisticsDTO getStatistics(Long appId, String resource, String dimension, String dimensionValue,
                                       String dateStr) {
        return statisticsDAO.getStatistics(appId, resource, dimension, dimensionValue, dateStr);
    }

    @Override
    public List<StatisticsDTO> listStatistics(Long appId, String resource, String dimension, String dimensionValue,
                                              String dateStr) {
        return statisticsDAO.getStatisticsList
            (
                Collections.singletonList(appId),
                null,
                resource,
                dimension,
                dimensionValue,
                dateStr
            );
    }

    @Override
    public Boolean triggerStatistics(List<String> dateList) {
        List<Long> appIds = applicationService.listAllAppIds();
        try {
            for (String dateStr : dateList) {
                LocalDateTime dayStartTime = TimeUtil.getDayStartTime(dateStr);
                LocalDateTime dayEndTime = dayStartTime.plusDays(1);
                for (Long appId : appIds) {
                    // 删除旧的统计数据
                    int affectedRows = statisticsDAO.deleteOneDayStatistics(appId, dateStr);
                    log.debug("{} old rows deleted", affectedRows);
                    int offset = 0;
                    int limit = 10000;
                    List<Long> taskInstanceIds;
                    do {
                        log.debug("trigger statistics of date {} appId {} offset {} limit {}", dateStr, appId, offset
                            , limit);
                        taskInstanceIds = taskInstanceService.listTaskInstanceId(appId,
                            TimeUtil.localDateTime2Long(dayStartTime), TimeUtil.localDateTime2Long(dayEndTime),
                            offset, limit);
                        // 触发统计
                        for (Long taskInstanceId : taskInstanceIds) {
                            TaskInstanceDTO taskInstance = taskInstanceService.getTaskInstance(taskInstanceId);
                            updateStartJobStatistics(taskInstance);
                            updateStartJobStatistics(taskInstance);
                        }
                        offset += limit;
                        Thread.sleep(2000);
                    } while (taskInstanceIds.size() == limit);
                }
            }
            return true;
        } catch (Throwable t) {
            log.warn("Fail to trigger statistics of {}", dateList);
            return false;
        }
    }

    @Scheduled(cron = "0 0 1 * * *")
    public void clearStatistics() {
        log.info("clearStatistics task triggered");
        new ClearExpiredStatisticsTask(statisticsDAO, statisticConfig).start();
    }

    @Scheduled(initialDelay = 3 * 1000, fixedRate = 3 * 1000)
    public void transfer() {
        try {
            synchronized (writeLock) {
                flushQueue.add(incrementMap);
                incrementMap = new ConcurrentHashMap<>();
            }
        } catch (Throwable t) {
            log.error("Fail to transfer incrementMap into flushQueue, flushQueue.size={}", flushQueue.size(), t);
        }
    }
}
