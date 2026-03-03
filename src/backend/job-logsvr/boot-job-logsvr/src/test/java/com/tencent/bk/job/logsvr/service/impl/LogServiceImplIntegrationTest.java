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

import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;
import com.tencent.bk.job.logsvr.consts.LogTypeEnum;
import com.tencent.bk.job.logsvr.model.FileLogQuery;
import com.tencent.bk.job.logsvr.model.FileTaskLogDoc;
import com.tencent.bk.job.logsvr.model.ScriptLogQuery;
import com.tencent.bk.job.logsvr.model.ScriptTaskLogDoc;
import com.tencent.bk.job.logsvr.model.TaskExecuteObjectLog;
import com.tencent.bk.job.logsvr.model.service.FileTaskTimeAndRawLogDTO;
import com.tencent.bk.job.logsvr.model.service.ServiceFileTaskLogDTO;
import com.tencent.bk.job.logsvr.mongo.FileLogsCollectionLoader;
import com.tencent.bk.job.logsvr.mongo.LogCollectionFactory;
import com.tencent.bk.job.logsvr.mongo.LogCollectionLoaderFactory;
import com.tencent.bk.job.logsvr.mongo.ScriptLogsCollectionLoader;
import com.tencent.bk.job.logsvr.service.LogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataMongoTest
@TestPropertySource(locations = "classpath:test.properties")
@Import({LogServiceImpl.class, LogCollectionFactory.class, ScriptLogsCollectionLoader.class,
    LogCollectionLoaderFactory.class, FileLogsCollectionLoader.class})
public class LogServiceImplIntegrationTest {
    @Autowired
    private LogService logService;

    @Nested
    @DisplayName("测试查询脚本日志")
    class GetScriptLogTest {
        @Test
        @DisplayName("测试通过关键字查询IP - 关键字包含特殊字符")
        void testGetIpsBykeyword() {
            String jobCreateDate = "2023_03_21";
            long stepInstanceId = 1L;
            int executeCount = 1;
            int batch = 1;

            String content = "str1 ~`!@#$%^&*()_+-=;,./?\\|':><{}";
            String ip = "0:127.0.0.1";
            String ipv6 = "0:::1";
            long hostId = 1L;
            TaskExecuteObjectLog taskExecuteObjectLog1 = new TaskExecuteObjectLog();
            taskExecuteObjectLog1.setStepInstanceId(stepInstanceId);
            taskExecuteObjectLog1.setExecuteCount(executeCount);
            taskExecuteObjectLog1.setBatch(batch);
            taskExecuteObjectLog1.setJobCreateDate(jobCreateDate);
            ScriptTaskLogDoc scriptTaskLog1 = new ScriptTaskLogDoc(stepInstanceId, executeCount, batch, hostId, ip,
                ipv6, content, 10);
            taskExecuteObjectLog1.setScriptTaskLog(scriptTaskLog1);
            taskExecuteObjectLog1.setLogType(LogTypeEnum.SCRIPT.getValue());
            logService.saveLog(taskExecuteObjectLog1);
            content = "str2 ?|':><{}";
            ip = "0:127.0.0.2";
            ipv6 = "0:::2";
            hostId = 2L;
            TaskExecuteObjectLog taskExecuteObjectLog2 = new TaskExecuteObjectLog();
            taskExecuteObjectLog2.setStepInstanceId(stepInstanceId);
            taskExecuteObjectLog2.setExecuteCount(executeCount);
            taskExecuteObjectLog2.setBatch(batch);
            taskExecuteObjectLog2.setJobCreateDate(jobCreateDate);
            ScriptTaskLogDoc scriptTaskLog2 = new ScriptTaskLogDoc(stepInstanceId, executeCount, batch, hostId, ip,
                ipv6, content, 10);
            taskExecuteObjectLog2.setScriptTaskLog(scriptTaskLog2);
            taskExecuteObjectLog2.setLogType(LogTypeEnum.SCRIPT.getValue());
            logService.saveLog(taskExecuteObjectLog2);

            String keyword = "str";
            List<HostDTO> hostDTOS = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount,
                batch, keyword);
            assertThat(hostDTOS).hasSize(2);
            assertThat(hostDTOS).extracting("hostId").containsOnly(1L, 2L);
            keyword = "job";
            hostDTOS = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount,batch, keyword);
            assertThat(hostDTOS).hasSize(0);
            keyword = "~";
            hostDTOS = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount,batch, keyword);
            assertThat(hostDTOS).hasSize(1);
            assertThat(hostDTOS).extracting("hostId").containsOnly(1L);
            keyword = "`";
            hostDTOS = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount, batch, keyword);
            assertThat(hostDTOS).hasSize(1);
            assertThat(hostDTOS).extracting("hostId").containsOnly(1L);
            keyword = "!";
            hostDTOS = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount, batch, keyword);
            assertThat(hostDTOS).hasSize(1);
            assertThat(hostDTOS).extracting("hostId").containsOnly(1L);
            keyword = "@";
            hostDTOS = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount, batch, keyword);
            assertThat(hostDTOS).hasSize(1);
            assertThat(hostDTOS).extracting("hostId").containsOnly(1L);
            keyword = "#";
            hostDTOS = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount, batch, keyword);
            assertThat(hostDTOS).hasSize(1);
            assertThat(hostDTOS).extracting("hostId").containsOnly(1L);
            keyword = "$";
            hostDTOS = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount, batch, keyword);
            assertThat(hostDTOS).hasSize(1);
            assertThat(hostDTOS).extracting("hostId").containsOnly(1L);
            keyword = "%";
            hostDTOS = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount, batch, keyword);
            assertThat(hostDTOS).hasSize(1);
            assertThat(hostDTOS).extracting("hostId").containsOnly(1L);
            keyword = "^";
            hostDTOS = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount, batch, keyword);
            assertThat(hostDTOS).hasSize(1);
            assertThat(hostDTOS).extracting("hostId").containsOnly(1L);
            keyword = "&";
            hostDTOS = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount, batch, keyword);
            assertThat(hostDTOS).hasSize(1);
            assertThat(hostDTOS).extracting("hostId").containsOnly(1L);
            keyword = "*";
            hostDTOS = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount, batch, keyword);
            assertThat(hostDTOS).hasSize(1);
            assertThat(hostDTOS).extracting("hostId").containsOnly(1L);
            keyword = "(";
            hostDTOS = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount, batch, keyword);
            assertThat(hostDTOS).hasSize(1);
            assertThat(hostDTOS).extracting("hostId").containsOnly(1L);
            keyword = ")";
            hostDTOS = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount, batch, keyword);
            assertThat(hostDTOS).hasSize(1);
            assertThat(hostDTOS).extracting("hostId").containsOnly(1L);
            keyword = "_";
            hostDTOS = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount, batch, keyword);
            assertThat(hostDTOS).hasSize(1);
            assertThat(hostDTOS).extracting("hostId").containsOnly(1L);
            keyword = "+";
            hostDTOS = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount, batch, keyword);
            assertThat(hostDTOS).hasSize(1);
            assertThat(hostDTOS).extracting("hostId").containsOnly(1L);
            keyword = "=";
            hostDTOS = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount, batch, keyword);
            assertThat(hostDTOS).hasSize(1);
            assertThat(hostDTOS).extracting("hostId").containsOnly(1L);
            keyword = ";";
            hostDTOS = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount, batch, keyword);
            assertThat(hostDTOS).hasSize(1);
            assertThat(hostDTOS).extracting("hostId").containsOnly(1L);
            keyword = ",";
            hostDTOS = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount, batch, keyword);
            assertThat(hostDTOS).hasSize(1);
            assertThat(hostDTOS).extracting("hostId").containsOnly(1L);
            keyword = ".";
            hostDTOS = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount, batch, keyword);
            assertThat(hostDTOS).hasSize(1);
            assertThat(hostDTOS).extracting("hostId").containsOnly(1L);
            keyword = "/";
            hostDTOS = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount, batch, keyword);
            assertThat(hostDTOS).hasSize(1);
            assertThat(hostDTOS).extracting("hostId").containsOnly(1L);
            keyword = "?";
            hostDTOS = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount, batch, keyword);
            assertThat(hostDTOS).hasSize(2);
            assertThat(hostDTOS).extracting("hostId").containsOnly(1L, 2L);
            keyword = "\\";
            hostDTOS = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount, batch, keyword);
            assertThat(hostDTOS).hasSize(1);
            assertThat(hostDTOS).extracting("hostId").containsOnly(1L);
            keyword = ":";
            hostDTOS = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount, batch, keyword);
            assertThat(hostDTOS).hasSize(2);
            assertThat(hostDTOS).extracting("hostId").containsOnly(1L, 2L);
            keyword = "<";
            hostDTOS = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount, batch, keyword);
            assertThat(hostDTOS).hasSize(2);
            assertThat(hostDTOS).extracting("hostId").containsOnly(1L, 2L);
            keyword = ">";
            hostDTOS = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount, batch, keyword);
            assertThat(hostDTOS).hasSize(2);
            assertThat(hostDTOS).extracting("hostId").containsOnly(1L, 2L);
            keyword = "{";
            hostDTOS = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount, batch, keyword);
            assertThat(hostDTOS).hasSize(2);
            assertThat(hostDTOS).extracting("hostId").containsOnly(1L, 2L);
            keyword = "}";
            hostDTOS = logService.getHostsByKeyword(jobCreateDate, stepInstanceId, executeCount, batch, keyword);
            assertThat(hostDTOS).hasSize(2);
            assertThat(hostDTOS).extracting("hostId").containsOnly(1L, 2L);
        }
    }


    @Nested
    @DisplayName("测试保存脚本日志")
    class SaveScriptLogTest {
        @Test
        @DisplayName("测试保存脚本日志")
        void testSaveScriptLogV2() {
            TaskExecuteObjectLog taskExecuteObjectLog1 = new TaskExecuteObjectLog();
            taskExecuteObjectLog1.setStepInstanceId(1L);
            taskExecuteObjectLog1.setExecuteCount(0);
            taskExecuteObjectLog1.setBatch(1);
            taskExecuteObjectLog1.setJobCreateDate("2020_07_29");
            ScriptTaskLogDoc scriptTaskLog1 = new ScriptTaskLogDoc(1L, 0, 1, 101L, "0:127.0.0.1", "0:::1", "hello", 10);
            taskExecuteObjectLog1.setScriptTaskLog(scriptTaskLog1);
            taskExecuteObjectLog1.setLogType(LogTypeEnum.SCRIPT.getValue());
            logService.saveLog(taskExecuteObjectLog1);

            TaskExecuteObjectLog taskExecuteObjectLog2 = new TaskExecuteObjectLog();
            taskExecuteObjectLog2.setStepInstanceId(1L);
            taskExecuteObjectLog2.setExecuteCount(0);
            taskExecuteObjectLog2.setBatch(1);
            taskExecuteObjectLog2.setJobCreateDate("2020_07_29");
            ScriptTaskLogDoc scriptTaskLog2 = new ScriptTaskLogDoc(1L, 0, 1, 101L, "0:127.0.0.1", "0:::1", "world", 20);
            taskExecuteObjectLog2.setScriptTaskLog(scriptTaskLog2);
            taskExecuteObjectLog2.setLogType(LogTypeEnum.SCRIPT.getValue());
            logService.saveLog(taskExecuteObjectLog2);

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


            TaskExecuteObjectLog taskExecuteObjectLog3 = new TaskExecuteObjectLog();
            taskExecuteObjectLog3.setStepInstanceId(2L);
            taskExecuteObjectLog3.setExecuteCount(0);
            taskExecuteObjectLog3.setBatch(null);
            taskExecuteObjectLog3.setJobCreateDate("2020_07_29");
            ScriptTaskLogDoc scriptTaskLog3 = new ScriptTaskLogDoc(2L, 0, null, 101L, "0:127.0.0.1", "0:::1", "abc",
                20);
            taskExecuteObjectLog3.setScriptTaskLog(scriptTaskLog3);
            taskExecuteObjectLog3.setLogType(LogTypeEnum.SCRIPT.getValue());
            logService.saveLog(taskExecuteObjectLog3);

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
            assertThat(hostLog.getBatch()).isEqualTo(null);
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

            // 第一次写入：写入第一条日志
            FileTaskLogDoc fileTaskLog1 = buildFileTaskDetailLog(
                FileTaskModeEnum.DOWNLOAD.getValue(),
                102L,
                101L,
                "0:127.0.0.1",
                "0:::1",
                1,
                "/tmp/1.log",
                "/tmp/1.log",
                102L,
                "0:127.0.0.2",
                "0:::2",
                "/tmp/2.log",
                3,
                "Downloading",
                "100KB/S",
                "100MB",
                "50%"
            );
            // 设置writeContentList
            List<FileTaskLogDoc.LogEntry> writeContentList1 = new ArrayList<>();
            writeContentList1.add(new FileTaskLogDoc.LogEntry(1596078000000L, "Downloading...\n"));
            fillWriteList(fileTaskLog1, writeContentList1);

            List<FileTaskLogDoc> fileTaskLogList = new ArrayList<>();
            fileTaskLogList.add(fileTaskLog1);
            TaskExecuteObjectLog taskExecuteObjectLog = buildFileTaskHostLog(stepInstanceId, 0, "2020_07_29", 102L,
                "0:127.0.0.2", "0:::2", fileTaskLogList);
            logService.saveLog(taskExecuteObjectLog);

            FileLogQuery searchRequest = FileLogQuery.builder()
                .stepInstanceId(stepInstanceId)
                .executeCount(0)
                .jobCreateDate("2020_07_29")
                .hostIds(Collections.singletonList(102L))
                .build();
            List<FileTaskLogDoc> fileLogDocs = logService.listFileLogs(searchRequest);
            assertThat(fileLogDocs).hasSize(1);
            FileTaskLogDoc resultFileTaskLog1 = fileLogDocs.get(0);
            assertThat(resultFileTaskLog1.getMode()).isEqualTo(FileTaskModeEnum.DOWNLOAD.getValue());
            assertThat(resultFileTaskLog1.getHostId()).isEqualTo(102L);
            assertThat(resultFileTaskLog1.getSrcHostId()).isEqualTo(101L);
            assertThat(resultFileTaskLog1.getSrcIp()).isEqualTo("0:127.0.0.1");
            assertThat(resultFileTaskLog1.getSrcIpv6()).isEqualTo("0:::1");
            assertThat(resultFileTaskLog1.getSrcFileType()).isEqualTo(1);
            assertThat(resultFileTaskLog1.getSrcFile()).isEqualTo("/tmp/1.log");
            assertThat(resultFileTaskLog1.getDestHostId()).isEqualTo(102L);
            assertThat(resultFileTaskLog1.getDestIp()).isEqualTo("0:127.0.0.2");
            assertThat(resultFileTaskLog1.getDestIpv6()).isEqualTo("0:::2");
            assertThat(resultFileTaskLog1.getDestFile()).isEqualTo("/tmp/2.log");
            assertThat(resultFileTaskLog1.getStatus()).isEqualTo(3);
            assertThat(resultFileTaskLog1.getStatusDesc()).isEqualTo("Downloading");
            assertThat(resultFileTaskLog1.getSize()).isEqualTo("100MB");
            assertThat(resultFileTaskLog1.getSpeed()).isEqualTo("100KB/S");
            assertThat(resultFileTaskLog1.getProcess()).isEqualTo("50%");
            // 验证timeLogList包含1条LogEntry（Map对象）
            assertThat(resultFileTaskLog1.getTimeLogList()).hasSize(1);
            assertThat(resultFileTaskLog1.getTimeLogList().get(0)).isInstanceOf(Map.class);
            Map<String, Object> logEntry1 = (Map<String, Object>) resultFileTaskLog1.getTimeLogList().get(0);
            assertThat(logEntry1.get("logTime")).isEqualTo(1596078000000L);
            assertThat(logEntry1.get("content")).isEqualTo("Downloading...\n");

            // 再次插入日志，验证日志更新场景
            fileTaskLog1 = buildFileTaskDetailLog(
                FileTaskModeEnum.DOWNLOAD.getValue(),
                102L,
                101L,
                "0:127.0.0.1",
                "0:::1",
                1,
                "/tmp/1.log",
                "/tmp/1.log",
                102L,
                "0:127.0.0.2",
                "0:::2",
                "/tmp/2.log",
                4,
                "Finished",
                "0KB/S",
                "100MB",
                "100%"
            );
            // 设置writeContentList
            List<FileTaskLogDoc.LogEntry> writeContentList2 = new ArrayList<>();
            writeContentList2.add(new FileTaskLogDoc.LogEntry(1596079800000L, "Download success\n"));
            fillWriteList(fileTaskLog1, writeContentList2);
            
            fileTaskLogList.clear();
            fileTaskLogList.add(fileTaskLog1);
            taskExecuteObjectLog = buildFileTaskHostLog(stepInstanceId, 0, "2020_07_29", 102L, "0:127.0.0.2", "0:::2",
                fileTaskLogList);
            logService.saveLog(taskExecuteObjectLog);

            // 验证第二次写入后的结果
            fileLogDocs = logService.listFileLogs(searchRequest);
            assertThat(fileLogDocs).hasSize(1);
            resultFileTaskLog1 = fileLogDocs.get(0);
            assertThat(resultFileTaskLog1.getMode()).isEqualTo(FileTaskModeEnum.DOWNLOAD.getValue());
            assertThat(resultFileTaskLog1.getHostId()).isEqualTo(102L);
            assertThat(resultFileTaskLog1.getSrcHostId()).isEqualTo(101L);
            assertThat(resultFileTaskLog1.getSrcIp()).isEqualTo("0:127.0.0.1");
            assertThat(resultFileTaskLog1.getSrcIpv6()).isEqualTo("0:::1");
            assertThat(resultFileTaskLog1.getSrcFileType()).isEqualTo(1);
            assertThat(resultFileTaskLog1.getSrcFile()).isEqualTo("/tmp/1.log");
            assertThat(resultFileTaskLog1.getDestHostId()).isEqualTo(102L);
            assertThat(resultFileTaskLog1.getDestIp()).isEqualTo("0:127.0.0.2");
            assertThat(resultFileTaskLog1.getDestIpv6()).isEqualTo("0:::2");
            assertThat(resultFileTaskLog1.getDestFile()).isEqualTo("/tmp/2.log");
            assertThat(resultFileTaskLog1.getStatus()).isEqualTo(4);
            assertThat(resultFileTaskLog1.getStatusDesc()).isEqualTo("Finished");
            assertThat(resultFileTaskLog1.getSize()).isEqualTo("100MB");
            assertThat(resultFileTaskLog1.getSpeed()).isEqualTo("0KB/S");
            assertThat(resultFileTaskLog1.getProcess()).isEqualTo("100%");
            // 验证timeLogList包含2条LogEntry（Map对象）
            assertThat(resultFileTaskLog1.getTimeLogList()).hasSize(2);
            // 第1条
            Map<String, Object> logEntry1Final = (Map<String, Object>) resultFileTaskLog1.getTimeLogList().get(0);
            assertThat(logEntry1Final.get("logTime")).isEqualTo(1596078000000L);
            assertThat(logEntry1Final.get("content")).isEqualTo("Downloading...\n");
            // 第2条
            Map<String, Object> logEntry2 = (Map<String, Object>) resultFileTaskLog1.getTimeLogList().get(1);
            assertThat(logEntry2.get("logTime")).isEqualTo(1596079800000L);
            assertThat(logEntry2.get("content")).isEqualTo("Download success\n");
        }

        @Test
        @DisplayName("3.12.1时区改造版本添加，测试兼容保存文件分发日志，模拟发布时，新老logsvr实例共存且往同一个任务追加日志的情况")
        void testCompatiblySaveFileLog() {
            long stepInstanceId = 2L;

            // === 第一次写入：使用老版本方式（只有content字段） ===
            // 老 logsvr 只写 contentList（纯String），不写 timeLogList
            FileTaskLogDoc fileTaskLog1 = buildFileTaskDetailLog(
                FileTaskModeEnum.DOWNLOAD.getValue(),
                102L,
                101L,
                "0:127.0.0.1",
                "0:::1",
                1,
                "/tmp/1.log",
                "/tmp/1.log",
                102L,
                "0:127.0.0.2",
                "0:::2",
                "/tmp/2.log",
                3,
                "Downloading",
                "100KB/S",
                "100MB",
                "50%");
            oldFileTaskLogAddContent(fileTaskLog1, "[2020-07-30 11:00:00] Downloading1...\n");

            List<FileTaskLogDoc> fileTaskLogList = new ArrayList<>();
            fileTaskLogList.add(fileTaskLog1);
            TaskExecuteObjectLog taskExecuteObjectLog = buildFileTaskHostLog(stepInstanceId, 0, "2020_07_29", 102L,
                "0:127.0.0.2", "0:::2", fileTaskLogList);
            logService.saveLog(taskExecuteObjectLog);

            FileLogQuery searchRequest = FileLogQuery.builder()
                .stepInstanceId(stepInstanceId)
                .executeCount(0)
                .jobCreateDate("2020_07_29")
                .hostIds(Collections.singletonList(102L))
                .build();
            List<FileTaskLogDoc> fileLogDocs = logService.listFileLogs(searchRequest);
            assertThat(fileLogDocs).hasSize(1);
            FileTaskLogDoc resultFileTaskLog1 = fileLogDocs.get(0);
            assertThat(resultFileTaskLog1.getMode()).isEqualTo(FileTaskModeEnum.DOWNLOAD.getValue());
            assertThat(resultFileTaskLog1.getHostId()).isEqualTo(102L);
            assertThat(resultFileTaskLog1.getSrcHostId()).isEqualTo(101L);
            assertThat(resultFileTaskLog1.getSrcIp()).isEqualTo("0:127.0.0.1");
            assertThat(resultFileTaskLog1.getSrcIpv6()).isEqualTo("0:::1");
            assertThat(resultFileTaskLog1.getSrcFileType()).isEqualTo(1);
            assertThat(resultFileTaskLog1.getSrcFile()).isEqualTo("/tmp/1.log");
            assertThat(resultFileTaskLog1.getDestHostId()).isEqualTo(102L);
            assertThat(resultFileTaskLog1.getDestIp()).isEqualTo("0:127.0.0.2");
            assertThat(resultFileTaskLog1.getDestIpv6()).isEqualTo("0:::2");
            assertThat(resultFileTaskLog1.getDestFile()).isEqualTo("/tmp/2.log");
            assertThat(resultFileTaskLog1.getStatus()).isEqualTo(3);
            assertThat(resultFileTaskLog1.getStatusDesc()).isEqualTo("Downloading");
            assertThat(resultFileTaskLog1.getSize()).isEqualTo("100MB");
            assertThat(resultFileTaskLog1.getSpeed()).isEqualTo("100KB/S");
            assertThat(resultFileTaskLog1.getProcess()).isEqualTo("50%");
            // 验证contentList包含1条纯String（老版写入）
            assertThat(resultFileTaskLog1.getContentList()).hasSize(1);
            assertThat(resultFileTaskLog1.getContentList().get(0))
                .isEqualTo("[2020-07-30 11:00:00] Downloading1...\n");
            // 验证timeLogList为null（老版不写timeLogList）
            assertThat(resultFileTaskLog1.getTimeLogList()).isNull();
            // 验证 toServiceFileTaskLogDTO 兼容读取（timeLogList为空 → 降级使用contentList，time=null）
            ServiceFileTaskLogDTO serviceDTO1 = resultFileTaskLog1.toServiceFileTaskLogDTO();
            assertThat(serviceDTO1.getContentList()).hasSize(1);
            assertThat(serviceDTO1.getContentList().get(0).getTime()).isNull();
            assertThat(serviceDTO1.getContentList().get(0).getRawLog())
                .isEqualTo("[2020-07-30 11:00:00] Downloading1...\n");

            // === 第二次写入：使用新版本方式（writeContentList字段，包含LogEntry对象） ===
            // 新 logsvr 同时写 timeLogList（LogEntry对象）和 contentList（拼接的纯String）
            fileTaskLog1 = buildFileTaskDetailLog(
                FileTaskModeEnum.DOWNLOAD.getValue(),
                102L,
                101L,
                "0:127.0.0.1",
                "0:::1",
                1,
                "/tmp/1.log",
                "/tmp/1.log",
                102L,
                "0:127.0.0.2",
                "0:::2",
                "/tmp/2.log",
                3,
                "Downloading",
                "100KB/S",
                "100MB",
                "60%");
            // 设置writeContentList（新版本格式），timestamp=1596078180000L，对应东八区 2020-07-30 11:03:00
            List<FileTaskLogDoc.LogEntry> writeContentList = new ArrayList<>();
            writeContentList.add(new FileTaskLogDoc.LogEntry(1596078180000L, "Downloading2...\n"));
            fillWriteList(fileTaskLog1, writeContentList);

            fileTaskLogList.clear();
            fileTaskLogList.add(fileTaskLog1);
            taskExecuteObjectLog = buildFileTaskHostLog(stepInstanceId, 0, "2020_07_29", 102L, "0:127.0.0.2", "0:::2",
                fileTaskLogList);
            logService.saveLog(taskExecuteObjectLog);

            fileLogDocs = logService.listFileLogs(searchRequest);
            assertThat(fileLogDocs).hasSize(1);
            resultFileTaskLog1 = fileLogDocs.get(0);
            // 验证contentList包含2条纯String：1条老版 + 1条新版拼接的
            assertThat(resultFileTaskLog1.getContentList()).hasSize(2);
            // 第1条：老版写入的纯String，原样保留
            assertThat(resultFileTaskLog1.getContentList().get(0))
                .isEqualTo("[2020-07-30 11:00:00] Downloading1...\n");
            // 第2条：新版写入时拼接的 "[timestamp转化后的时间] log\n" 格式
            assertThat(resultFileTaskLog1.getContentList().get(1))
                .isEqualTo("[2020-07-30 11:03:00] Downloading2...\n");
            // 验证timeLogList包含1条LogEntry（只有新版写入的那条）
            assertThat(resultFileTaskLog1.getTimeLogList()).hasSize(1);
            Map<String, Object> logEntry = (Map<String, Object>) resultFileTaskLog1.getTimeLogList().get(0);
            assertThat(logEntry.get("logTime")).isEqualTo(1596078180000L);
            assertThat(logEntry.get("content")).isEqualTo("Downloading2...\n");
            // 验证 toServiceFileTaskLogDTO 兼容读取
            // timeLogList.size(1) != contentList.size(2) → 降级使用contentList，所有time=null
            ServiceFileTaskLogDTO serviceDTO2 = resultFileTaskLog1.toServiceFileTaskLogDTO();
            assertThat(serviceDTO2.getContentList()).hasSize(2);
            assertThat(serviceDTO2.getContentList().get(0).getTime()).isNull();
            assertThat(serviceDTO2.getContentList().get(0).getRawLog())
                .isEqualTo("[2020-07-30 11:00:00] Downloading1...\n");
            assertThat(serviceDTO2.getContentList().get(1).getTime()).isNull();
            assertThat(serviceDTO2.getContentList().get(1).getRawLog())
                .isEqualTo("[2020-07-30 11:03:00] Downloading2...\n");

            // === 第三次写入：再次使用老版本方式（只有content字段） ===
            fileTaskLog1 = buildFileTaskDetailLog(
                FileTaskModeEnum.DOWNLOAD.getValue(),
                102L,
                101L,
                "0:127.0.0.1",
                "0:::1",
                1,
                "/tmp/1.log",
                "/tmp/1.log",
                102L,
                "0:127.0.0.2",
                "0:::2",
                "/tmp/2.log",
                4,
                "Finished",
                "0KB/S",
                "100MB",
                "100%");
            oldFileTaskLogAddContent(fileTaskLog1, "[2020-07-30 11:30:00] Downloading3...\n");

            fileTaskLogList.clear();
            fileTaskLogList.add(fileTaskLog1);
            taskExecuteObjectLog = buildFileTaskHostLog(stepInstanceId, 0, "2020_07_29", 102L, "0:127.0.0.2", "0:::2",
                fileTaskLogList);
            logService.saveLog(taskExecuteObjectLog);

            fileLogDocs = logService.listFileLogs(searchRequest);
            assertThat(fileLogDocs).hasSize(1);
            resultFileTaskLog1 = fileLogDocs.get(0);
            assertThat(resultFileTaskLog1.getMode()).isEqualTo(FileTaskModeEnum.DOWNLOAD.getValue());
            assertThat(resultFileTaskLog1.getHostId()).isEqualTo(102L);
            assertThat(resultFileTaskLog1.getSrcHostId()).isEqualTo(101L);
            assertThat(resultFileTaskLog1.getSrcIp()).isEqualTo("0:127.0.0.1");
            assertThat(resultFileTaskLog1.getSrcIpv6()).isEqualTo("0:::1");
            assertThat(resultFileTaskLog1.getSrcFileType()).isEqualTo(1);
            assertThat(resultFileTaskLog1.getSrcFile()).isEqualTo("/tmp/1.log");
            assertThat(resultFileTaskLog1.getDestHostId()).isEqualTo(102L);
            assertThat(resultFileTaskLog1.getDestIp()).isEqualTo("0:127.0.0.2");
            assertThat(resultFileTaskLog1.getDestIpv6()).isEqualTo("0:::2");
            assertThat(resultFileTaskLog1.getDestFile()).isEqualTo("/tmp/2.log");
            assertThat(resultFileTaskLog1.getStatus()).isEqualTo(4);
            assertThat(resultFileTaskLog1.getStatusDesc()).isEqualTo("Finished");
            assertThat(resultFileTaskLog1.getSize()).isEqualTo("100MB");
            assertThat(resultFileTaskLog1.getSpeed()).isEqualTo("0KB/S");
            assertThat(resultFileTaskLog1.getProcess()).isEqualTo("100%");

            // 验证contentList包含3条纯String：老格式 + 新格式拼接的 + 老格式
            assertThat(resultFileTaskLog1.getContentList()).hasSize(3);
            // 第1条：老版写入的 "[时间] 日志" 格式
            assertThat(resultFileTaskLog1.getContentList().get(0))
                .isEqualTo("[2020-07-30 11:00:00] Downloading1...\n");
            // 第2条：新版写入时 timestamp 转化后拼接的 "[时间] 日志\n" 格式
            assertThat(resultFileTaskLog1.getContentList().get(1))
                .isEqualTo("[2020-07-30 11:03:00] Downloading2...\n");
            // 第3条：老版写入的 "[时间] 日志" 格式
            assertThat(resultFileTaskLog1.getContentList().get(2))
                .isEqualTo("[2020-07-30 11:30:00] Downloading3...\n");

            // 验证timeLogList只包含1条（只有第二次新版写入时才push到timeLogList）
            assertThat(resultFileTaskLog1.getTimeLogList()).hasSize(1);
            @SuppressWarnings("unchecked")
            Map<String, Object> logEntry2 = (Map<String, Object>) resultFileTaskLog1.getTimeLogList().get(0);
            assertThat(logEntry2.get("logTime")).isEqualTo(1596078180000L);
            assertThat(logEntry2.get("content")).isEqualTo("Downloading2...\n");

            // 验证 toServiceFileTaskLogDTO 兼容读取
            // timeLogList.size(1) != contentList.size(3) → 降级使用contentList，所有time=null
            ServiceFileTaskLogDTO serviceDTO3 = resultFileTaskLog1.toServiceFileTaskLogDTO();
            assertThat(serviceDTO3.getContentList()).hasSize(3);
            for (FileTaskTimeAndRawLogDTO item : serviceDTO3.getContentList()) {
                assertThat(item.getTime()).isNull();
            }
            assertThat(serviceDTO3.getContentList().get(0).getRawLog())
                .isEqualTo("[2020-07-30 11:00:00] Downloading1...\n");
            assertThat(serviceDTO3.getContentList().get(1).getRawLog())
                .isEqualTo("[2020-07-30 11:03:00] Downloading2...\n");
            assertThat(serviceDTO3.getContentList().get(2).getRawLog())
                .isEqualTo("[2020-07-30 11:30:00] Downloading3...\n");
        }
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

    /**
     * 老版本添加content
     * @param doc
     * @param content
     */
    void oldFileTaskLogAddContent(FileTaskLogDoc doc, String content) {
        doc.setContent(content);
    }

    void fillWriteList(FileTaskLogDoc doc, List<FileTaskLogDoc.LogEntry> writeContentList) {
        doc.setWriteContentList(writeContentList);
    }

    FileTaskLogDoc buildFileTaskDetailLog(Integer mode,
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
}
