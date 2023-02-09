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

import brave.Tracing;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.tencent.bk.job.common.artifactory.sdk.ArtifactoryClient;
import com.tencent.bk.job.common.constant.JobConstants;
import com.tencent.bk.job.common.model.dto.IpDTO;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.trace.executors.TraceableExecutorService;
import com.tencent.bk.job.common.util.BatchUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.util.file.ZipUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.config.ArtifactoryConfig;
import com.tencent.bk.job.execute.config.LogExportConfig;
import com.tencent.bk.job.execute.constants.LogExportStatusEnum;
import com.tencent.bk.job.execute.model.GseTaskIpLogDTO;
import com.tencent.bk.job.execute.model.LogExportJobInfoDTO;
import com.tencent.bk.job.execute.model.ScriptIpLogContent;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.service.GseTaskLogService;
import com.tencent.bk.job.execute.service.LogExportService;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;

import java.io.File;
import java.io.PrintWriter;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @since 19/1/2021 12:01
 */
@Slf4j
@Service
public class LogExportServiceImpl implements LogExportService {
    private static final String EXPORT_KEY_PREFIX = "execute:log:export:";
    private final GseTaskLogService gseTaskLogService;
    private final LogService logService;
    private final TraceableExecutorService logExportExecutor;
    private final StringRedisTemplate redisTemplate;
    private final TaskInstanceService taskInstanceService;
    private final ArtifactoryClient artifactoryClient;
    private final ArtifactoryConfig artifactoryConfig;
    private final LogExportConfig logExportConfig;

    @Autowired
    public LogExportServiceImpl(GseTaskLogService gseTaskLogService, LogService logService, Tracing tracing,
                                StringRedisTemplate redisTemplate,
                                TaskInstanceService taskInstanceService,
                                ArtifactoryClient artifactoryClient,
                                ArtifactoryConfig artifactoryConfig,
                                LogExportConfig logExportConfig) {
        this.gseTaskLogService = gseTaskLogService;
        this.logService = logService;
        this.redisTemplate = redisTemplate;
        this.taskInstanceService = taskInstanceService;
        this.artifactoryClient = artifactoryClient;
        this.artifactoryConfig = artifactoryConfig;
        this.logExportConfig = logExportConfig;
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("log-export-thread-%d").build();
        this.logExportExecutor = new TraceableExecutorService(new ThreadPoolExecutor(10,
            100, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), threadFactory), tracing);
    }

    @Override
    public LogExportJobInfoDTO packageLogFile(String username, Long appId, Long stepInstanceId, String ip,
                                              int executeCount,
                                              String logFileDir, String logFileName, Boolean repackage) {
        log.debug("Package log file for {}|{}|{}|{}|{}|{}|{}|{}", username, appId, stepInstanceId, ip, executeCount,
            logFileDir, logFileName, repackage);
        LogExportJobInfoDTO exportJobInfo = new LogExportJobInfoDTO();
        exportJobInfo.setJobKey(getExportJobKey(appId, stepInstanceId, ip));
        exportJobInfo.setStatus(LogExportStatusEnum.INIT);

        if (repackage) {
            log.debug("Force release lock because of repackage flag");
            LockUtils.forceReleaseDistributedLock(exportJobInfo.getJobKey());
            deleteExportInfo(exportJobInfo.getJobKey());
        }
        saveExportInfo(exportJobInfo);

        boolean isGetByIp = StringUtils.isNotBlank(ip);

        if (isGetByIp) {
            doPackage(exportJobInfo, stepInstanceId, ip, executeCount, logFileDir, logFileName);
        } else {
            logExportExecutor.execute(() -> {
                String requestId = UUID.randomUUID().toString();
                log.debug("Begin log package process|{}", requestId);
                try {
                    boolean lockResult = LockUtils.tryGetDistributedLock(exportJobInfo.getJobKey(), requestId,
                        3600_000L);
                    if (lockResult) {
                        log.debug("Acquire lock success! Begin process!|{}", requestId);
                        exportJobInfo.setStatus(LogExportStatusEnum.PROCESSING);
                        saveExportInfo(exportJobInfo);

                        doPackage(exportJobInfo, stepInstanceId, ip, executeCount, logFileDir, logFileName);
                    } else {
                        log.error("Job already running!|{}|{}|{}|{}", requestId, appId, stepInstanceId, ip);
                    }
                } catch (Exception e) {
                    log.error("Error while package log file!|{}|{}|{}|{}", requestId, stepInstanceId, ip,
                        executeCount, e);
                    markJobFailed(exportJobInfo);
                } finally {
                    LockUtils.releaseDistributedLock(exportJobInfo.getJobKey(), requestId);
                    log.debug("Process finished!|{}", requestId);
                }
            });
        }
        return exportJobInfo;
    }

    private void markJobFailed(LogExportJobInfoDTO exportJobInfo) {
        exportJobInfo.setStatus(LogExportStatusEnum.FAILED);
        saveExportInfo(exportJobInfo);
    }

    @Override
    public LogExportJobInfoDTO getExportInfo(Long appId, Long stepInstanceId, String ip) {
        return JsonUtils.fromJson(redisTemplate.opsForValue().get(getExportJobKey(appId, stepInstanceId, ip)),
            LogExportJobInfoDTO.class);
    }

    private void saveExportInfo(LogExportJobInfoDTO exportJobInfo) {
        redisTemplate.opsForValue().set(exportJobInfo.getJobKey(), JsonUtils.toJson(exportJobInfo));
    }

    private void deleteExportInfo(String jobKey) {
        redisTemplate.delete(jobKey);
    }

    private String getExportJobKey(Long appId, Long stepInstanceId, String ip) {
        return EXPORT_KEY_PREFIX + appId + ":" + stepInstanceId + ":" + ip;
    }

    private void doPackage(LogExportJobInfoDTO exportJobInfo, Long stepInstanceId, String ip, int executeCount,
                           String logFileDir, String logFileName) {
        boolean isGetByIp = StringUtils.isNotBlank(ip);
        File logFile = new File(logFileDir + logFileName);

        StopWatch watch = new StopWatch("exportJobLog");
        watch.start("listJobIps");
        List<GseTaskIpLogDTO> gseTaskIpLogs = new ArrayList<>();
        if (isGetByIp) {
            GseTaskIpLogDTO gseTaskIpLog = gseTaskLogService.getIpLog(stepInstanceId, executeCount, ip);
            if (gseTaskIpLog != null) {
                gseTaskIpLogs.add(gseTaskIpLog);
            }
        } else {
            gseTaskIpLogs = gseTaskLogService.getIpLog(stepInstanceId, executeCount, true);
        }
        watch.stop();

        if (gseTaskIpLogs == null || gseTaskIpLogs.isEmpty()) {
            log.warn("Gse task ips are empty! stepInstanceId={}", stepInstanceId);
            markJobFailed(exportJobInfo);
            return;
        }

        Collection<LogBatchQuery> querys = buildLogBatchQuery(stepInstanceId, gseTaskIpLogs);

        watch.start("getLogContent");
        StepInstanceBaseDTO stepInstance = taskInstanceService.getBaseStepInstance(stepInstanceId);
        String jobCreateDate = DateUtils.formatUnixTimestamp(stepInstance.getCreateTime(), ChronoUnit.MILLIS,
            "yyyy_MM_dd", ZoneId.of("UTC"));
        try (PrintWriter out = new PrintWriter(logFile, "UTF-8")) {
            for (LogBatchQuery query : querys) {
                for (List<IpDTO> ips : query.getIpBatches()) {
                    List<ScriptIpLogContent> scriptIpLogContentList =
                        logService.batchGetScriptIpLogContent(jobCreateDate, stepInstanceId, query.getExecuteCount(),
                            ips);
                    for (ScriptIpLogContent scriptIpLogContent : scriptIpLogContentList) {
                        if (scriptIpLogContent != null && StringUtils.isNotEmpty(scriptIpLogContent.getContent())) {
                            String[] logList = scriptIpLogContent.getContent().split("\n");
                            for (String log : logList) {
                                if (isGetByIp) {
                                    out.println(log);
                                } else {
                                    out.println(scriptIpLogContent.getIp() + " | " + log);
                                }
                            }
                        }

                    }
                }
            }
            out.flush();
        } catch (Exception e) {
            log.warn("Export execution log fail", e);
            FileUtils.deleteQuietly(logFile);
            markJobFailed(exportJobInfo);
            return;
        }
        watch.stop();

        watch.start("zipLogFile");
        try {
            File zipFile = ZipUtil.zip(logFile.getAbsolutePath());
            if (zipFile == null) {
                log.warn("Zip log file fail, fileName={}", logFile.getName());
                markJobFailed(exportJobInfo);
                return;
            } else {
                FileUtils.deleteQuietly(logFile);
            }
            long zipFileLength = zipFile.length();
            // 将zip文件上传至制品库
            if (JobConstants.FILE_STORAGE_BACKEND_ARTIFACTORY.equals(logExportConfig.getStorageBackend())) {
                try {
                    artifactoryClient.uploadGenericFile(
                        artifactoryConfig.getArtifactoryJobProject(),
                        logExportConfig.getLogExportRepo(),
                        zipFile.getName(),
                        zipFile
                    );
                    FileUtils.deleteQuietly(zipFile);
                } catch (Exception e) {
                    String msg = MessageFormatter.format(
                        "Fail to upload {} to artifactory",
                        zipFile.getAbsolutePath()
                    ).getMessage();
                    log.error(msg, e);
                    markJobFailed(exportJobInfo);
                    return;
                }
            }
            exportJobInfo.setStatus(LogExportStatusEnum.SUCCESS);
            exportJobInfo.setZipFileName(logFileName + ".zip");
            exportJobInfo.setFileSize(zipFileLength);
            saveExportInfo(exportJobInfo);
        } catch (Exception e) {
            log.warn("Zip log file fail, fileName={}", logFile.getName());
            markJobFailed(exportJobInfo);
            return;
        }
        watch.stop();

        if (watch.getTotalTimeMillis() > 10000L) {
            log.info("Export job execution log is slow, cost: {}", watch.prettyPrint());
        }
    }

    private Collection<LogBatchQuery> buildLogBatchQuery(long stepInstanceId, List<GseTaskIpLogDTO> gseTaskIpLogs) {
        Map<Integer, LogBatchQuery> batchQueryGroups = new HashMap<>();
        gseTaskIpLogs.forEach(gseTaskIpLog -> {
            LogBatchQuery query = batchQueryGroups.computeIfAbsent(gseTaskIpLog.getExecuteCount(),
                (executeCount) -> new LogBatchQuery(stepInstanceId, executeCount));
            query.addIp(new IpDTO(gseTaskIpLog.getCloudAreaId(), gseTaskIpLog.getIp()));
        });
        batchQueryGroups.values().forEach(LogBatchQuery::batchIps);
        return batchQueryGroups.values();
    }

    @Data
    private static class LogBatchQuery {
        private static final int MAX_BATCH_IPS = 1000;
        private long stepInstanceId;
        private int executeCount;
        private List<IpDTO> ips = new ArrayList<>();
        private List<List<IpDTO>> ipBatches;

        LogBatchQuery(long stepInstanceId, int executeCount) {
            this.stepInstanceId = stepInstanceId;
            this.executeCount = executeCount;
        }

        void addIp(IpDTO ip) {
            if (ips == null) {
                ips = new ArrayList<>();
            }
            ips.add(ip);
        }

        void batchIps() {
            ipBatches = BatchUtil.buildBatchList(ips, MAX_BATCH_IPS);
        }
    }
}
