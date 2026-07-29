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

package com.tencent.bk.job.logsvr.service.impl;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.common.util.date.DateUtils;
import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;
import com.tencent.bk.job.logsvr.consts.LogTypeEnum;
import com.tencent.bk.job.logsvr.model.FileLogQuery;
import com.tencent.bk.job.logsvr.model.FileTaskLogDoc;
import com.tencent.bk.job.logsvr.model.FileTaskLogDocField;
import com.tencent.bk.job.logsvr.model.ScriptLogQuery;
import com.tencent.bk.job.logsvr.model.ScriptTaskLogDoc;
import com.tencent.bk.job.logsvr.model.ScriptTaskLogDocField;
import com.tencent.bk.job.logsvr.model.TaskExecuteObjectLog;
import com.tencent.bk.job.logsvr.model.service.FileTaskTimeAndRawLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
import com.tencent.bk.job.logsvr.mongo.LogCollectionFactory;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

/**
 * LogServiceImpl 单元测试：用内存假存储模拟 Mongo 读写，避免启动嵌入式 MongoDB。
 */
@ExtendWith(MockitoExtension.class)
public class LogServiceImplTest {

    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private LogCollectionFactory logCollectionFactory;
    @Mock
    private MongoCollection<Document> mongoCollection;

    private final List<Document> scriptLogStore = new ArrayList<>();
    private final Map<String, FileTaskLogDoc> fileLogStore = new HashMap<>();

    private LogServiceImpl logService;

    @BeforeEach
    void setUp() {
        scriptLogStore.clear();
        fileLogStore.clear();
        logService = new LogServiceImpl(mongoTemplate, logCollectionFactory);

        lenient().when(logCollectionFactory.getCollection(anyString())).thenReturn(mongoCollection);

        lenient().when(mongoCollection.insertOne(any(Document.class))).thenAnswer(invocation -> {
            scriptLogStore.add(new Document(invocation.<Document>getArgument(0)));
            return null;
        });

        lenient().when(mongoCollection.updateOne(any(Bson.class), any(Bson.class), any(UpdateOptions.class)))
            .thenAnswer(invocation -> {
                applyFileUpsert(toDocument(invocation.getArgument(0)), toDocument(invocation.getArgument(1)));
                return UpdateResult.acknowledged(1, 1L, null);
            });

        lenient().when(mongoTemplate.find(any(Query.class), eq(ScriptTaskLogDoc.class), anyString()))
            .thenAnswer(invocation -> findScriptLogs(invocation.getArgument(0)));

        lenient().when(mongoTemplate.find(any(Query.class), eq(FileTaskLogDoc.class), anyString()))
            .thenAnswer(invocation -> findFileLogs(invocation.getArgument(0)));
    }

    @Nested
    @DisplayName("测试查询脚本日志")
    class GetScriptLogTest {
        @Test
        @DisplayName("测试通过关键字查询IP - 关键字包含特殊字符")
        void testGetIpsByKeyword() {
            String jobCreateDate = "2023_03_21";
            long stepInstanceId = 1L;
            int executeCount = 1;
            int batch = 1;

            saveScriptLog(jobCreateDate, stepInstanceId, executeCount, batch, 1L, "0:127.0.0.1", "0:::1",
                "str1 ~`!@#$%^&*()_+-=;,./?\\|':><{}", 10);
            saveScriptLog(jobCreateDate, stepInstanceId, executeCount, batch, 2L, "0:127.0.0.2", "0:::2",
                "str2 ?|':><{}", 10);

            assertHostIds(jobCreateDate, stepInstanceId, executeCount, batch, "str", 1L, 2L);
            assertHostIds(jobCreateDate, stepInstanceId, executeCount, batch, "job");
            assertHostIds(jobCreateDate, stepInstanceId, executeCount, batch, "~", 1L);
            assertHostIds(jobCreateDate, stepInstanceId, executeCount, batch, "`", 1L);
            assertHostIds(jobCreateDate, stepInstanceId, executeCount, batch, "!", 1L);
            assertHostIds(jobCreateDate, stepInstanceId, executeCount, batch, "@", 1L);
            assertHostIds(jobCreateDate, stepInstanceId, executeCount, batch, "#", 1L);
            assertHostIds(jobCreateDate, stepInstanceId, executeCount, batch, "$", 1L);
            assertHostIds(jobCreateDate, stepInstanceId, executeCount, batch, "%", 1L);
            assertHostIds(jobCreateDate, stepInstanceId, executeCount, batch, "^", 1L);
            assertHostIds(jobCreateDate, stepInstanceId, executeCount, batch, "&", 1L);
            assertHostIds(jobCreateDate, stepInstanceId, executeCount, batch, "*", 1L);
            assertHostIds(jobCreateDate, stepInstanceId, executeCount, batch, "(", 1L);
            assertHostIds(jobCreateDate, stepInstanceId, executeCount, batch, ")", 1L);
            assertHostIds(jobCreateDate, stepInstanceId, executeCount, batch, "_", 1L);
            assertHostIds(jobCreateDate, stepInstanceId, executeCount, batch, "+", 1L);
            assertHostIds(jobCreateDate, stepInstanceId, executeCount, batch, "=", 1L);
            assertHostIds(jobCreateDate, stepInstanceId, executeCount, batch, ";", 1L);
            assertHostIds(jobCreateDate, stepInstanceId, executeCount, batch, ",", 1L);
            assertHostIds(jobCreateDate, stepInstanceId, executeCount, batch, ".", 1L);
            assertHostIds(jobCreateDate, stepInstanceId, executeCount, batch, "/", 1L);
            assertHostIds(jobCreateDate, stepInstanceId, executeCount, batch, "?", 1L, 2L);
            assertHostIds(jobCreateDate, stepInstanceId, executeCount, batch, "\\", 1L);
            assertHostIds(jobCreateDate, stepInstanceId, executeCount, batch, ":", 1L, 2L);
            assertHostIds(jobCreateDate, stepInstanceId, executeCount, batch, "<", 1L, 2L);
            assertHostIds(jobCreateDate, stepInstanceId, executeCount, batch, ">", 1L, 2L);
            assertHostIds(jobCreateDate, stepInstanceId, executeCount, batch, "{", 1L, 2L);
            assertHostIds(jobCreateDate, stepInstanceId, executeCount, batch, "}", 1L, 2L);
        }

        private void assertHostIds(String jobCreateDate, long stepInstanceId, int executeCount, Integer batch,
                                   String keyword, Long... expectedHostIds) {
            List<HostDTO> hostDTOS = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount,
                batch, keyword);
            assertThat(hostDTOS).hasSize(expectedHostIds.length);
            if (expectedHostIds.length > 0) {
                assertThat(hostDTOS).extracting("hostId").containsOnly((Object[]) expectedHostIds);
            }
        }
    }

    @Nested
    @DisplayName("测试保存脚本日志")
    class SaveScriptLogTest {
        @Test
        @DisplayName("测试保存脚本日志")
        void testSaveScriptLogV2() {
            saveScriptLog("2020_07_29", 1L, 0, 1, 101L, "0:127.0.0.1", "0:::1", "hello", 10);
            saveScriptLog("2020_07_29", 1L, 0, 1, 101L, "0:127.0.0.1", "0:::1", "world", 20);

            ScriptLogQuery searchRequest = new ScriptLogQuery();
            searchRequest.setStepInstanceId(1L);
            searchRequest.setExecuteCount(0);
            searchRequest.setBatch(1);
            searchRequest.setJobCreateDate("2020_07_29");
            searchRequest.setHostIds(Collections.singletonList(101L));
            List<TaskExecuteObjectLog> result = logService.listScriptLogs(searchRequest);
            assertThat(result).hasSize(1);
            TaskExecuteObjectLog hostLog = result.get(0);
            assertThat(hostLog.getStepInstanceId()).isEqualTo(1L);
            assertThat(hostLog.getExecuteCount()).isEqualTo(0);
            assertThat(hostLog.getBatch()).isEqualTo(1);
            assertThat(hostLog.getHostId()).isEqualTo(101L);
            assertThat(hostLog.getIp()).isEqualTo("0:127.0.0.1");
            assertThat(hostLog.getIpv6()).isEqualTo("0:::1");
            assertThat(hostLog.getScriptContent()).isEqualTo("helloworld");

            saveScriptLog("2020_07_29", 2L, 0, null, 101L, "0:127.0.0.1", "0:::1", "abc", 20);

            searchRequest = new ScriptLogQuery();
            searchRequest.setStepInstanceId(2L);
            searchRequest.setExecuteCount(0);
            searchRequest.setBatch(null);
            searchRequest.setJobCreateDate("2020_07_29");
            searchRequest.setHostIds(Collections.singletonList(101L));
            result = logService.listScriptLogs(searchRequest);
            assertThat(result).hasSize(1);
            hostLog = result.get(0);
            assertThat(hostLog.getStepInstanceId()).isEqualTo(2L);
            assertThat(hostLog.getExecuteCount()).isEqualTo(0);
            assertThat(hostLog.getBatch()).isNull();
            assertThat(hostLog.getHostId()).isEqualTo(101L);
            assertThat(hostLog.getIp()).isEqualTo("0:127.0.0.1");
            assertThat(hostLog.getIpv6()).isEqualTo("0:::1");
            assertThat(hostLog.getScriptContent()).isEqualTo("abc");
        }
    }

    @Nested
    @DisplayName("测试保存文件分发日志")
    @SuppressWarnings("deprecation")
    class SaveFileLogTest {

        @Test
        @DisplayName("测试保存文件分发日志")
        void testSaveFileLog() {
            Long stepInstanceId = 1L;

            FileTaskLogDoc fileTaskLog1 = buildFileTaskDetailLog(
                FileTaskModeEnum.DOWNLOAD.getValue(),
                102L, 101L, "0:127.0.0.1", "0:::1", 1, "/tmp/1.log", "/tmp/1.log",
                102L, "0:127.0.0.2", "0:::2", "/tmp/2.log",
                3, "Downloading", "100KB/S", "100MB", "50%"
            );
            List<FileTaskLogDoc.LogEntry> writeContentList1 = new ArrayList<>();
            writeContentList1.add(new FileTaskLogDoc.LogEntry(1596078000000L, "Downloading...\n"));
            fileTaskLog1.setWriteContentList(writeContentList1);

            logService.saveLog(buildFileTaskHostLog(stepInstanceId, 0, "2020_07_29", 102L,
                "0:127.0.0.2", "0:::2", Collections.singletonList(fileTaskLog1)));

            FileLogQuery searchRequest = FileLogQuery.builder()
                .stepInstanceId(stepInstanceId)
                .executeCount(0)
                .jobCreateDate("2020_07_29")
                .hostIds(Collections.singletonList(102L))
                .build();
            List<FileTaskLogDoc> fileLogDocs = logService.listFileLogs(searchRequest);
            assertThat(fileLogDocs).hasSize(1);
            FileTaskLogDoc resultFileTaskLog1 = fileLogDocs.get(0);
            assertFileTaskBaseFields(resultFileTaskLog1, 3, "Downloading", "100KB/S", "100MB", "50%");
            assertThat(resultFileTaskLog1.getTimeLogList()).hasSize(1);
            Map<String, Object> logEntry1 = (Map<String, Object>) resultFileTaskLog1.getTimeLogList().get(0);
            assertThat(logEntry1.get("logTime")).isEqualTo(1596078000000L);
            assertThat(logEntry1.get("content")).isEqualTo("Downloading...\n");

            fileTaskLog1 = buildFileTaskDetailLog(
                FileTaskModeEnum.DOWNLOAD.getValue(),
                102L, 101L, "0:127.0.0.1", "0:::1", 1, "/tmp/1.log", "/tmp/1.log",
                102L, "0:127.0.0.2", "0:::2", "/tmp/2.log",
                4, "Finished", "0KB/S", "100MB", "100%"
            );
            List<FileTaskLogDoc.LogEntry> writeContentList2 = new ArrayList<>();
            writeContentList2.add(new FileTaskLogDoc.LogEntry(1596079800000L, "Download success\n"));
            fileTaskLog1.setWriteContentList(writeContentList2);

            logService.saveLog(buildFileTaskHostLog(stepInstanceId, 0, "2020_07_29", 102L,
                "0:127.0.0.2", "0:::2", Collections.singletonList(fileTaskLog1)));

            fileLogDocs = logService.listFileLogs(searchRequest);
            assertThat(fileLogDocs).hasSize(1);
            resultFileTaskLog1 = fileLogDocs.get(0);
            assertFileTaskBaseFields(resultFileTaskLog1, 4, "Finished", "0KB/S", "100MB", "100%");
            assertThat(resultFileTaskLog1.getTimeLogList()).hasSize(2);
            Map<String, Object> logEntry1Final = (Map<String, Object>) resultFileTaskLog1.getTimeLogList().get(0);
            assertThat(logEntry1Final.get("logTime")).isEqualTo(1596078000000L);
            assertThat(logEntry1Final.get("content")).isEqualTo("Downloading...\n");
            Map<String, Object> logEntry2 = (Map<String, Object>) resultFileTaskLog1.getTimeLogList().get(1);
            assertThat(logEntry2.get("logTime")).isEqualTo(1596079800000L);
            assertThat(logEntry2.get("content")).isEqualTo("Download success\n");
        }

        @Test
        @DisplayName("3.12.1时区改造版本添加，测试兼容保存文件分发日志，模拟发布时，新老logsvr实例共存且往同一个任务追加日志的情况")
        void testCompatiblySaveFileLog() {
            long stepInstanceId = 2L;

            FileTaskLogDoc fileTaskLog1 = buildFileTaskDetailLog(
                FileTaskModeEnum.DOWNLOAD.getValue(),
                102L, 101L, "0:127.0.0.1", "0:::1", 1, "/tmp/1.log", "/tmp/1.log",
                102L, "0:127.0.0.2", "0:::2", "/tmp/2.log",
                3, "Downloading", "100KB/S", "100MB", "50%");
            fileTaskLog1.setContent("[" + formatTime(1596078000000L) + "] Downloading1...\n");

            logService.saveLog(buildFileTaskHostLog(stepInstanceId, 0, "2020_07_29", 102L,
                "0:127.0.0.2", "0:::2", Collections.singletonList(fileTaskLog1)));

            FileLogQuery searchRequest = FileLogQuery.builder()
                .stepInstanceId(stepInstanceId)
                .executeCount(0)
                .jobCreateDate("2020_07_29")
                .hostIds(Collections.singletonList(102L))
                .build();
            List<FileTaskLogDoc> fileLogDocs = logService.listFileLogs(searchRequest);
            assertThat(fileLogDocs).hasSize(1);
            FileTaskLogDoc resultFileTaskLog1 = fileLogDocs.get(0);
            assertFileTaskBaseFields(resultFileTaskLog1, 3, "Downloading", "100KB/S", "100MB", "50%");
            assertThat(resultFileTaskLog1.getContentList()).hasSize(1);
            assertThat(resultFileTaskLog1.getContentList().get(0))
                .isEqualTo("[" + formatTime(1596078000000L) + "] Downloading1...\n");
            assertThat(resultFileTaskLog1.getTimeLogList()).isNull();
            ServiceFileTaskLogDTO serviceDTO1 = resultFileTaskLog1.toServiceFileTaskLogDTO();
            assertThat(serviceDTO1.getContentList()).hasSize(1);
            assertThat(serviceDTO1.getContentList().get(0).getTime()).isNull();
            assertThat(serviceDTO1.getContentList().get(0).getRawLog())
                .isEqualTo("[" + formatTime(1596078000000L) + "] Downloading1...\n");

            fileTaskLog1 = buildFileTaskDetailLog(
                FileTaskModeEnum.DOWNLOAD.getValue(),
                102L, 101L, "0:127.0.0.1", "0:::1", 1, "/tmp/1.log", "/tmp/1.log",
                102L, "0:127.0.0.2", "0:::2", "/tmp/2.log",
                3, "Downloading", "100KB/S", "100MB", "60%");
            List<FileTaskLogDoc.LogEntry> writeContentList = new ArrayList<>();
            writeContentList.add(new FileTaskLogDoc.LogEntry(1596078180000L, "Downloading2...\n"));
            fileTaskLog1.setWriteContentList(writeContentList);

            logService.saveLog(buildFileTaskHostLog(stepInstanceId, 0, "2020_07_29", 102L,
                "0:127.0.0.2", "0:::2", Collections.singletonList(fileTaskLog1)));

            fileLogDocs = logService.listFileLogs(searchRequest);
            assertThat(fileLogDocs).hasSize(1);
            resultFileTaskLog1 = fileLogDocs.get(0);
            assertThat(resultFileTaskLog1.getContentList()).hasSize(2);
            assertThat(resultFileTaskLog1.getContentList().get(0))
                .isEqualTo("[" + formatTime(1596078000000L) + "] Downloading1...\n");
            assertThat(resultFileTaskLog1.getContentList().get(1))
                .isEqualTo("[" + formatTime(1596078180000L) + "] Downloading2...\n");
            assertThat(resultFileTaskLog1.getTimeLogList()).hasSize(1);
            Map<String, Object> logEntry = (Map<String, Object>) resultFileTaskLog1.getTimeLogList().get(0);
            assertThat(logEntry.get("logTime")).isEqualTo(1596078180000L);
            assertThat(logEntry.get("content")).isEqualTo("Downloading2...\n");
            ServiceFileTaskLogDTO serviceDTO2 = resultFileTaskLog1.toServiceFileTaskLogDTO();
            assertThat(serviceDTO2.getContentList()).hasSize(2);
            assertThat(serviceDTO2.getContentList().get(0).getTime()).isNull();
            assertThat(serviceDTO2.getContentList().get(0).getRawLog())
                .isEqualTo("[" + formatTime(1596078000000L) + "] Downloading1...\n");
            assertThat(serviceDTO2.getContentList().get(1).getTime()).isNull();
            assertThat(serviceDTO2.getContentList().get(1).getRawLog())
                .isEqualTo("[" + formatTime(1596078180000L) + "] Downloading2...\n");

            fileTaskLog1 = buildFileTaskDetailLog(
                FileTaskModeEnum.DOWNLOAD.getValue(),
                102L, 101L, "0:127.0.0.1", "0:::1", 1, "/tmp/1.log", "/tmp/1.log",
                102L, "0:127.0.0.2", "0:::2", "/tmp/2.log",
                4, "Finished", "0KB/S", "100MB", "100%");
            fileTaskLog1.setContent("[" + formatTime(1596079800000L) + "] Downloading3...\n");

            logService.saveLog(buildFileTaskHostLog(stepInstanceId, 0, "2020_07_29", 102L,
                "0:127.0.0.2", "0:::2", Collections.singletonList(fileTaskLog1)));

            fileLogDocs = logService.listFileLogs(searchRequest);
            assertThat(fileLogDocs).hasSize(1);
            resultFileTaskLog1 = fileLogDocs.get(0);
            assertFileTaskBaseFields(resultFileTaskLog1, 4, "Finished", "0KB/S", "100MB", "100%");
            assertThat(resultFileTaskLog1.getContentList()).hasSize(3);
            assertThat(resultFileTaskLog1.getContentList().get(0))
                .isEqualTo("[" + formatTime(1596078000000L) + "] Downloading1...\n");
            assertThat(resultFileTaskLog1.getContentList().get(1))
                .isEqualTo("[" + formatTime(1596078180000L) + "] Downloading2...\n");
            assertThat(resultFileTaskLog1.getContentList().get(2))
                .isEqualTo("[" + formatTime(1596079800000L) + "] Downloading3...\n");
            assertThat(resultFileTaskLog1.getTimeLogList()).hasSize(1);
            Map<String, Object> logEntry2 = (Map<String, Object>) resultFileTaskLog1.getTimeLogList().get(0);
            assertThat(logEntry2.get("logTime")).isEqualTo(1596078180000L);
            assertThat(logEntry2.get("content")).isEqualTo("Downloading2...\n");

            ServiceFileTaskLogDTO serviceDTO3 = resultFileTaskLog1.toServiceFileTaskLogDTO();
            assertThat(serviceDTO3.getContentList()).hasSize(3);
            for (FileTaskTimeAndRawLogDTO item : serviceDTO3.getContentList()) {
                assertThat(item.getTime()).isNull();
            }
            assertThat(serviceDTO3.getContentList().get(0).getRawLog())
                .isEqualTo("[" + formatTime(1596078000000L) + "] Downloading1...\n");
            assertThat(serviceDTO3.getContentList().get(1).getRawLog())
                .isEqualTo("[" + formatTime(1596078180000L) + "] Downloading2...\n");
            assertThat(serviceDTO3.getContentList().get(2).getRawLog())
                .isEqualTo("[" + formatTime(1596079800000L) + "] Downloading3...\n");
        }

        private void assertFileTaskBaseFields(FileTaskLogDoc doc,
                                              Integer status,
                                              String statusDesc,
                                              String speed,
                                              String size,
                                              String process) {
            assertThat(doc.getMode()).isEqualTo(FileTaskModeEnum.DOWNLOAD.getValue());
            assertThat(doc.getHostId()).isEqualTo(102L);
            assertThat(doc.getSrcHostId()).isEqualTo(101L);
            assertThat(doc.getSrcIp()).isEqualTo("0:127.0.0.1");
            assertThat(doc.getSrcIpv6()).isEqualTo("0:::1");
            assertThat(doc.getSrcFileType()).isEqualTo(1);
            assertThat(doc.getSrcFile()).isEqualTo("/tmp/1.log");
            assertThat(doc.getDestHostId()).isEqualTo(102L);
            assertThat(doc.getDestIp()).isEqualTo("0:127.0.0.2");
            assertThat(doc.getDestIpv6()).isEqualTo("0:::2");
            assertThat(doc.getDestFile()).isEqualTo("/tmp/2.log");
            assertThat(doc.getStatus()).isEqualTo(status);
            assertThat(doc.getStatusDesc()).isEqualTo(statusDesc);
            assertThat(doc.getSize()).isEqualTo(size);
            assertThat(doc.getSpeed()).isEqualTo(speed);
            assertThat(doc.getProcess()).isEqualTo(process);
        }
    }

    private void saveScriptLog(String jobCreateDate,
                               long stepInstanceId,
                               int executeCount,
                               Integer batch,
                               long hostId,
                               String ip,
                               String ipv6,
                               String content,
                               int offset) {
        TaskExecuteObjectLog taskExecuteObjectLog = new TaskExecuteObjectLog();
        taskExecuteObjectLog.setStepInstanceId(stepInstanceId);
        taskExecuteObjectLog.setExecuteCount(executeCount);
        taskExecuteObjectLog.setBatch(batch);
        taskExecuteObjectLog.setJobCreateDate(jobCreateDate);
        taskExecuteObjectLog.setScriptTaskLog(new ScriptTaskLogDoc(stepInstanceId, executeCount, batch, hostId, ip,
            ipv6, content, offset));
        taskExecuteObjectLog.setLogType(LogTypeEnum.SCRIPT.getValue());
        logService.saveLog(taskExecuteObjectLog);
    }

    private TaskExecuteObjectLog buildFileTaskHostLog(long stepInstanceId,
                                                      int executeCount,
                                                      String jobCreateDate,
                                                      Long hostId,
                                                      String ip,
                                                      String ipv6,
                                                      List<FileTaskLogDoc> fileTaskLogs) {
        TaskExecuteObjectLog taskExecuteObjectLog = new TaskExecuteObjectLog();
        taskExecuteObjectLog.setStepInstanceId(stepInstanceId);
        taskExecuteObjectLog.setExecuteCount(executeCount);
        taskExecuteObjectLog.setJobCreateDate(jobCreateDate);
        taskExecuteObjectLog.setHostId(hostId);
        taskExecuteObjectLog.setIp(ip);
        taskExecuteObjectLog.setIpv6(ipv6);
        taskExecuteObjectLog.setFileTaskLogs(fileTaskLogs);
        taskExecuteObjectLog.setLogType(LogTypeEnum.FILE.getValue());
        return taskExecuteObjectLog;
    }

    private String formatTime(long timestampMillis) {
        return DateUtils.formatUnixTimestamp(
            timestampMillis,
            ChronoUnit.MILLIS,
            DateUtils.FILE_TASK_LOG_FORMAT,
            ZoneId.systemDefault()
        );
    }

    private FileTaskLogDoc buildFileTaskDetailLog(Integer mode,
                                                  Long hostId,
                                                  Long srcHostId,
                                                  String srcIp,
                                                  String srcIpv6,
                                                  Integer srcFileType,
                                                  String srcFileName,
                                                  String displaySrcFile,
                                                  Long destHostId,
                                                  String destIp,
                                                  String destIpv6,
                                                  String destFileName,
                                                  Integer status,
                                                  String statusDesc,
                                                  String speed,
                                                  String size,
                                                  String process) {
        FileTaskLogDoc fileTaskLogDoc = new FileTaskLogDoc();
        fileTaskLogDoc.setMode(mode);
        fileTaskLogDoc.setHostId(hostId);
        fileTaskLogDoc.setSrcHostId(srcHostId);
        fileTaskLogDoc.setSrcIp(srcIp);
        fileTaskLogDoc.setSrcIpv6(srcIpv6);
        fileTaskLogDoc.setSrcFileType(srcFileType);
        fileTaskLogDoc.setSrcFile(srcFileName);
        fileTaskLogDoc.setDisplaySrcFile(displaySrcFile);
        fileTaskLogDoc.setDestFile(destFileName);
        fileTaskLogDoc.setDestHostId(destHostId);
        fileTaskLogDoc.setDestIp(destIp);
        fileTaskLogDoc.setDestIpv6(destIpv6);
        fileTaskLogDoc.setStatus(status);
        fileTaskLogDoc.setStatusDesc(statusDesc);
        fileTaskLogDoc.setSpeed(speed);
        fileTaskLogDoc.setSize(size);
        fileTaskLogDoc.setProcess(process);
        return fileTaskLogDoc;
    }

    @SuppressWarnings("unchecked")
    private Document toDocument(Bson bson) {
        if (bson instanceof Document) {
            return (Document) bson;
        }
        if (bson instanceof Map) {
            return new Document((Map<String, Object>) bson);
        }
        return Document.parse(bson.toBsonDocument().toJson());
    }

    @SuppressWarnings("unchecked")
    private Document asDocument(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Document) {
            return (Document) value;
        }
        if (value instanceof Map) {
            return new Document((Map<String, Object>) value);
        }
        throw new IllegalArgumentException("Unsupported document type: " + value.getClass());
    }

    private void applyFileUpsert(Document filter, Document update) {
        String taskId = filter.getString(FileTaskLogDocField.TASK_ID);
        FileTaskLogDoc existing = fileLogStore.computeIfAbsent(taskId, id -> new FileTaskLogDoc());
        Document setDoc = asDocument(update.get("$set"));
        if (setDoc != null) {
            if (setDoc.containsKey(FileTaskLogDocField.STEP_ID)) {
                existing.setStepId(asLong(setDoc.get(FileTaskLogDocField.STEP_ID)));
            }
            if (setDoc.containsKey(FileTaskLogDocField.EXECUTE_COUNT)) {
                existing.setExecuteCount(asInt(setDoc.get(FileTaskLogDocField.EXECUTE_COUNT)));
            }
            if (setDoc.containsKey(FileTaskLogDocField.BATCH)) {
                existing.setBatch(asInt(setDoc.get(FileTaskLogDocField.BATCH)));
            }
            if (setDoc.containsKey(FileTaskLogDocField.MODE)) {
                existing.setMode(asInt(setDoc.get(FileTaskLogDocField.MODE)));
            }
            if (setDoc.containsKey(FileTaskLogDocField.TASK_ID)) {
                existing.setTaskId(setDoc.getString(FileTaskLogDocField.TASK_ID));
            }
            if (setDoc.containsKey(FileTaskLogDocField.HOST_ID)) {
                existing.setHostId(asLong(setDoc.get(FileTaskLogDocField.HOST_ID)));
            }
            if (setDoc.containsKey(FileTaskLogDocField.SRC_HOST_ID)) {
                existing.setSrcHostId(asLong(setDoc.get(FileTaskLogDocField.SRC_HOST_ID)));
            }
            if (setDoc.containsKey(FileTaskLogDocField.SRC_IP)) {
                existing.setSrcIp(setDoc.getString(FileTaskLogDocField.SRC_IP));
            }
            if (setDoc.containsKey(FileTaskLogDocField.SRC_IPV6)) {
                existing.setSrcIpv6(setDoc.getString(FileTaskLogDocField.SRC_IPV6));
            }
            if (setDoc.containsKey(FileTaskLogDocField.SRC_FILE_TYPE)) {
                existing.setSrcFileType(asInt(setDoc.get(FileTaskLogDocField.SRC_FILE_TYPE)));
            }
            if (setDoc.containsKey(FileTaskLogDocField.SRC_FILE)) {
                existing.setSrcFile(setDoc.getString(FileTaskLogDocField.SRC_FILE));
            }
            if (setDoc.containsKey(FileTaskLogDocField.DISPLAY_SRC_FILE)) {
                existing.setDisplaySrcFile(setDoc.getString(FileTaskLogDocField.DISPLAY_SRC_FILE));
            }
            if (setDoc.containsKey(FileTaskLogDocField.DEST_HOST_ID)) {
                existing.setDestHostId(asLong(setDoc.get(FileTaskLogDocField.DEST_HOST_ID)));
            }
            if (setDoc.containsKey(FileTaskLogDocField.DEST_IP)) {
                existing.setDestIp(setDoc.getString(FileTaskLogDocField.DEST_IP));
            }
            if (setDoc.containsKey(FileTaskLogDocField.DEST_IPV6)) {
                existing.setDestIpv6(setDoc.getString(FileTaskLogDocField.DEST_IPV6));
            }
            if (setDoc.containsKey(FileTaskLogDocField.DEST_FILE)) {
                existing.setDestFile(setDoc.getString(FileTaskLogDocField.DEST_FILE));
            }
            if (setDoc.containsKey(FileTaskLogDocField.STATUS)) {
                existing.setStatus(asInt(setDoc.get(FileTaskLogDocField.STATUS)));
            }
            if (setDoc.containsKey(FileTaskLogDocField.STATUS_DESC)) {
                existing.setStatusDesc(setDoc.getString(FileTaskLogDocField.STATUS_DESC));
            }
            if (setDoc.containsKey(FileTaskLogDocField.SPEED)) {
                existing.setSpeed(setDoc.getString(FileTaskLogDocField.SPEED));
            }
            if (setDoc.containsKey(FileTaskLogDocField.SIZE)) {
                existing.setSize(setDoc.getString(FileTaskLogDocField.SIZE));
            }
            if (setDoc.containsKey(FileTaskLogDocField.PROCESS)) {
                existing.setProcess(setDoc.getString(FileTaskLogDocField.PROCESS));
            }
        }

        Document pushDoc = asDocument(update.get("$push"));
        if (pushDoc != null) {
            if (pushDoc.containsKey(FileTaskLogDocField.CONTENT_LIST)) {
                if (existing.getContentList() == null) {
                    existing.setContentList(new ArrayList<>());
                }
                existing.getContentList().add(String.valueOf(pushDoc.get(FileTaskLogDocField.CONTENT_LIST)));
            }
            if (pushDoc.containsKey(FileTaskLogDocField.TIME_LOG_LIST)) {
                if (existing.getTimeLogList() == null) {
                    existing.setTimeLogList(new ArrayList<>());
                }
                Object timeLog = pushDoc.get(FileTaskLogDocField.TIME_LOG_LIST);
                if (timeLog instanceof Map) {
                    existing.getTimeLogList().add(new HashMap<>((Map<?, ?>) timeLog));
                } else {
                    existing.getTimeLogList().add(timeLog);
                }
            }
        }
    }

    private List<ScriptTaskLogDoc> findScriptLogs(Query query) {
        Document queryObj = query.getQueryObject();
        return scriptLogStore.stream()
            .filter(doc -> matchScriptQuery(doc, queryObj))
            .map(this::toScriptTaskLogDoc)
            .collect(Collectors.toList());
    }

    private List<FileTaskLogDoc> findFileLogs(Query query) {
        Document queryObj = query.getQueryObject();
        return fileLogStore.values().stream()
            .filter(doc -> matchFileQuery(doc, queryObj))
            .collect(Collectors.toList());
    }

    private boolean matchScriptQuery(Document doc, Document queryObj) {
        if (!matchEqualsOrCompare(doc.get(ScriptTaskLogDocField.STEP_ID),
            queryObj.get(ScriptTaskLogDocField.STEP_ID))) {
            return false;
        }
        if (!matchEqualsOrCompare(doc.get(ScriptTaskLogDocField.EXECUTE_COUNT),
            queryObj.get(ScriptTaskLogDocField.EXECUTE_COUNT))) {
            return false;
        }
        if (queryObj.containsKey(ScriptTaskLogDocField.BATCH)
            && !matchEqualsOrCompare(doc.get(ScriptTaskLogDocField.BATCH),
            queryObj.get(ScriptTaskLogDocField.BATCH))) {
            return false;
        }
        if (queryObj.containsKey(ScriptTaskLogDocField.HOST_ID)
            && !matchEqualsOrCompare(doc.get(ScriptTaskLogDocField.HOST_ID),
            queryObj.get(ScriptTaskLogDocField.HOST_ID))) {
            return false;
        }
        if (queryObj.containsKey(ScriptTaskLogDocField.CONTENT)) {
            Pattern pattern = extractPattern(queryObj.get(ScriptTaskLogDocField.CONTENT));
            String content = doc.getString(ScriptTaskLogDocField.CONTENT);
            if (pattern == null || content == null || !pattern.matcher(content).find()) {
                return false;
            }
        }
        return true;
    }

    private boolean matchFileQuery(FileTaskLogDoc doc, Document queryObj) {
        if (!matchEqualsOrCompare(doc.getStepId(), queryObj.get(FileTaskLogDocField.STEP_ID))) {
            return false;
        }
        if (!matchEqualsOrCompare(doc.getExecuteCount(), queryObj.get(FileTaskLogDocField.EXECUTE_COUNT))) {
            return false;
        }
        if (queryObj.containsKey(FileTaskLogDocField.HOST_ID)
            && !matchEqualsOrCompare(doc.getHostId(), queryObj.get(FileTaskLogDocField.HOST_ID))) {
            return false;
        }
        if (queryObj.containsKey(FileTaskLogDocField.BATCH)
            && !matchEqualsOrCompare(doc.getBatch(), queryObj.get(FileTaskLogDocField.BATCH))) {
            return false;
        }
        return true;
    }

    private boolean matchEqualsOrCompare(Object actual, Object expectedCriteria) {
        if (expectedCriteria == null) {
            return true;
        }
        if (expectedCriteria instanceof Document) {
            Document criteria = (Document) expectedCriteria;
            if (criteria.containsKey("$lte")) {
                return asLong(actual) <= asLong(criteria.get("$lte"));
            }
            if (criteria.containsKey("$in")) {
                List<?> values = (List<?>) criteria.get("$in");
                return values.stream().anyMatch(v -> Objects.equals(asLong(actual), asLong(v))
                    || Objects.equals(actual, v));
            }
            return false;
        }
        if (actual instanceof Number && expectedCriteria instanceof Number) {
            return asLong(actual).equals(asLong(expectedCriteria));
        }
        return Objects.equals(actual, expectedCriteria);
    }

    private Pattern extractPattern(Object contentCriteria) {
        if (contentCriteria instanceof Pattern) {
            return (Pattern) contentCriteria;
        }
        if (contentCriteria instanceof Document) {
            Document doc = (Document) contentCriteria;
            String regex = doc.getString("$regex");
            String options = doc.getString("$options");
            int flags = 0;
            if (options != null && options.contains("i")) {
                flags |= Pattern.CASE_INSENSITIVE;
            }
            return Pattern.compile(regex, flags);
        }
        return null;
    }

    private ScriptTaskLogDoc toScriptTaskLogDoc(Document doc) {
        ScriptTaskLogDoc scriptTaskLogDoc = new ScriptTaskLogDoc();
        scriptTaskLogDoc.setStepInstanceId(asLong(doc.get(ScriptTaskLogDocField.STEP_ID)));
        scriptTaskLogDoc.setExecuteCount(asInt(doc.get(ScriptTaskLogDocField.EXECUTE_COUNT)));
        if (doc.containsKey(ScriptTaskLogDocField.BATCH)) {
            scriptTaskLogDoc.setBatch(asInt(doc.get(ScriptTaskLogDocField.BATCH)));
        }
        if (doc.containsKey(ScriptTaskLogDocField.HOST_ID)) {
            scriptTaskLogDoc.setHostId(asLong(doc.get(ScriptTaskLogDocField.HOST_ID)));
        }
        scriptTaskLogDoc.setIp(doc.getString(ScriptTaskLogDocField.IP));
        scriptTaskLogDoc.setIpv6(doc.getString(ScriptTaskLogDocField.IPV6));
        scriptTaskLogDoc.setContent(doc.getString(ScriptTaskLogDocField.CONTENT));
        if (doc.containsKey(ScriptTaskLogDocField.OFFSET)) {
            scriptTaskLogDoc.setOffset(asInt(doc.get(ScriptTaskLogDocField.OFFSET)));
        }
        return scriptTaskLogDoc;
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        return ((Number) value).longValue();
    }

    private Integer asInt(Object value) {
        if (value == null) {
            return null;
        }
        return ((Number) value).intValue();
    }
}
