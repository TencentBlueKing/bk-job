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
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.redis.util.LockUtils;
import com.tencent.bk.job.common.util.BatchUtil;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.common.util.file.ZipUtil;
import com.tencent.bk.job.common.util.json.JsonUtils;
import com.tencent.bk.job.execute.common.trace.executors.TraceableExecutorService;
import com.tencent.bk.job.execute.config.ArtifactoryConfig;
import com.tencent.bk.job.execute.config.LogExportConfig;
import com.tencent.bk.job.execute.constants.LogExportStatusEnum;
import com.tencent.bk.job.execute.model.AgentTaskDTO;
import com.tencent.bk.job.execute.model.LogExportJobInfoDTO;
import com.tencent.bk.job.execute.model.ScriptHostLogContent;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.service.LogExportService;
import com.tencent.bk.job.execute.service.LogService;
import com.tencent.bk.job.execute.service.ScriptAgentTaskService;
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
    private final LogService logService;
    private final TraceableExecutorService logExportExecutor;
    private final StringRedisTemplate redisTemplate;
    private final TaskInstanceService taskInstanceService;
    private final ArtifactoryClient artifactoryClient;
    private final ArtifactoryConfig artifactoryConfig;
    private final LogExportConfig logExportConfig;
    private final ScriptAgentTaskService scriptAgentTaskService;

    @Autowired
    public LogExportServiceImpl(LogService logService,
                                Tracing tracing,
                                StringRedisTemplate redisTemplate,
                                TaskInstanceService taskInstanceService,
                                ArtifactoryClient artifactoryClient,
                                ArtifactoryConfig artifactoryConfig,
                                LogExportConfig logExportConfig,
                                ScriptAgentTaskService scriptAgentTaskService) {
        this.logService = logService;
        this.redisTemplate = redisTemplate;
        this.taskInstanceService = taskInstanceService;
        this.artifactoryClient = artifactoryClient;
        this.artifactoryConfig = artifactoryConfig;
        this.logExportConfig = logExportConfig;
        this.scriptAgentTaskService = scriptAgentTaskService;
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("log-export-thread-%d").build();
        this.logExportExecutor = new TraceableExecutorService(new ThreadPoolExecutor(10,
            100, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(), threadFactory), tracing);
    }

    @Override
    public LogExportJobInfoDTO packageLogFile(String username, Long appId, Long stepInstanceId, Long hostId,
                                              String ip, int executeCount,
                                              String logFileDir, String logFileName, Boolean repackage) {
        log.debug("Package log file for {}|{}|{}|{}|{}|{}|{}|{}", username, appId, stepInstanceId, hostId, executeCount,
            logFileDir, logFileName, repackage);
        LogExportJobInfoDTO exportJobInfo = new LogExportJobInfoDTO();
        exportJobInfo.setJobKey(getExportJobKey(appId, stepInstanceId, hostId, ip));
        exportJobInfo.setStatus(LogExportStatusEnum.INIT);

        if (repackage) {
            log.debug("Force release lock because of repackage flag");
            LockUtils.forceReleaseDistributedLock(exportJobInfo.getJobKey());
            deleteExportInfo(exportJobInfo.getJobKey());
        }
        saveExportInfo(exportJobInfo);

        boolean isGetByHost = hostId != null;

        if (isGetByHost) {
            doPackage(exportJobInfo, stepInstanceId, hostId, ip, executeCount, logFileDir, logFileName);
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

                        doPackage(exportJobInfo, stepInstanceId, hostId, ip, executeCount, logFileDir, logFileName);
                    } else {
                        log.error("Job already running!|{}|{}|{}|{}", requestId, appId, stepInstanceId, hostId);
                    }
                } catch (Exception e) {
                    log.error("Error while package log file!|{}|{}|{}|{}", requestId, stepInstanceId, hostId,
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
    public LogExportJobInfoDTO getExportInfo(Long appId, Long stepInstanceId, Long hostId, String ip) {
        return JsonUtils.fromJson(redisTemplate.opsForValue().get(getExportJobKey(appId, stepInstanceId, hostId, ip)),
            LogExportJobInfoDTO.class);
    }

    private void saveExportInfo(LogExportJobInfoDTO exportJobInfo) {
        redisTemplate.opsForValue().set(exportJobInfo.getJobKey(), JsonUtils.toJson(exportJobInfo));
    }

    private void deleteExportInfo(String jobKey) {
        redisTemplate.delete(jobKey);
    }

    private String getExportJobKey(Long appId, Long stepInstanceId, Long hostId, String ip) {
        String key = EXPORT_KEY_PREFIX + appId + ":" + stepInstanceId + ":";
        if (hostId != null) {
            key = key + hostId;
        } else {
            key = key + ip;
        }
        return key;
    }

    private void doPackage(LogExportJobInfoDTO exportJobInfo, long stepInstanceId, Long hostId,
                           String ip, int executeCount, String logFileDir, String logFileName) {
        StepInstanceBaseDTO stepInstance = taskInstanceService.getBaseStepInstance(stepInstanceId);
        boolean isGetByHost = hostId != null;
        File logFile = new File(logFileDir + logFileName);

        StopWatch watch = new StopWatch("exportJobLog");
        watch.start("listJobIps");
        List<AgentTaskDTO> gseAgentTasks = new ArrayList<>();
        if (isGetByHost) {
            HostDTO host = new HostDTO();
            host.setHostId(hostId);
            host.setIp(ip);
            AgentTaskDTO agentTask = scriptAgentTaskService.getAgentTaskByHost(stepInstance, executeCount, null,
                host);
            if (agentTask != null) {
                gseAgentTasks.add(agentTask);
            }
        } else {
            gseAgentTasks = scriptAgentTaskService.listAgentTasks(stepInstanceId, executeCount, null);
        }
        watch.stop();

        if (gseAgentTasks == null || gseAgentTasks.isEmpty()) {
            log.warn("Gse task ips are empty! stepInstanceId={}", stepInstanceId);
            markJobFailed(exportJobInfo);
            return;
        }

        Collection<LogBatchQuery> querys = buildLogBatchQuery(stepInstanceId, gseAgentTasks);

        watch.start("getLogContent");
        String jobCreateDate = DateUtils.formatUnixTimestamp(stepInstance.getCreateTime(), ChronoUnit.MILLIS,
            "yyyy_MM_dd", ZoneId.of("UTC"));
        try (PrintWriter out = new PrintWriter(logFile, "UTF-8")) {
            for (LogBatchQuery query : querys) {
                for (List<HostDTO> hosts : query.getHostBatches()) {
                    List<ScriptHostLogContent> scriptHostLogContentList =
                        logService.batchGetScriptHostLogContent(jobCreateDate, stepInstanceId, query.getExecuteCount(),
                            null, hosts);
                    for (ScriptHostLogContent scriptHostLogContent : scriptHostLogContentList) {
                        if (scriptHostLogContent != null && StringUtils.isNotEmpty(scriptHostLogContent.getContent())) {
                            String[] logList = scriptHostLogContent.getContent().split("\n");
                            for (String log : logList) {
                                if (isGetByHost) {
                                    out.println(log);
                                } else {
                                    out.println(scriptHostLogContent.getIp() + " | " + log);
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
            exportJobInfo.setFileSize(zipFile.length());
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

    private Collection<LogBatchQuery> buildLogBatchQuery(long stepInstanceId, List<AgentTaskDTO> agentTasks) {
        Map<Integer, LogBatchQuery> batchQueryGroups = new HashMap<>();
        agentTasks.forEach(agentTask -> {
            LogBatchQuery query = batchQueryGroups.computeIfAbsent(agentTask.getExecuteCount(),
                (executeCount) -> new LogBatchQuery(stepInstanceId, executeCount));
            HostDTO queryHost = null;
            if (agentTask.getHostId() != null) {
                queryHost = HostDTO.fromHostId(agentTask.getHostId());
            } else if (StringUtils.isNotEmpty(agentTask.getCloudIp())) {
                queryHost = HostDTO.fromCloudIp(agentTask.getCloudIp());
            }
            query.addHost(queryHost);
        });
        batchQueryGroups.values().forEach(LogBatchQuery::batchHosts);
        return batchQueryGroups.values();
    }

    @Data
    private static class LogBatchQuery {
        private static final int MAX_BATCH_IPS = 1000;
        private long stepInstanceId;
        private int executeCount;
        private List<HostDTO> hosts = new ArrayList<>();
        private List<List<HostDTO>> hostBatches;

        LogBatchQuery(long stepInstanceId, int executeCount) {
            this.stepInstanceId = stepInstanceId;
            this.executeCount = executeCount;
        }

        void addHost(HostDTO host) {
            if (hosts == null) {
                hosts = new ArrayList<>();
            }
            hosts.add(host);
        }

        void batchHosts() {
            hostBatches = BatchUtil.buildBatchList(hosts, MAX_BATCH_IPS);
        }
    }
}
