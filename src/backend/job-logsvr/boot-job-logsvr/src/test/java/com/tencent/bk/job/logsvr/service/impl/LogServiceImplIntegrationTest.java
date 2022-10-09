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

package com.tencent.bk.job.logsvr.service.impl;

import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;
import com.tencent.bk.job.logsvr.consts.LogTypeEnum;
import com.tencent.bk.job.logsvr.model.FileLogQuery;
import com.tencent.bk.job.logsvr.model.FileTaskLogDoc;
import com.tencent.bk.job.logsvr.model.ScriptLogQuery;
import com.tencent.bk.job.logsvr.model.ScriptTaskLogDoc;
import com.tencent.bk.job.logsvr.model.TaskHostLog;
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
    @DisplayName("测试保存脚本日志")
    class SaveScriptLogTest {
        @Test
        @DisplayName("测试保存脚本日志")
        void testSaveScriptLogV2() {
            TaskHostLog taskHostLog1 = new TaskHostLog();
            taskHostLog1.setStepInstanceId(1L);
            taskHostLog1.setExecuteCount(0);
            taskHostLog1.setBatch(1);
            taskHostLog1.setJobCreateDate("2020_07_29");
            ScriptTaskLogDoc scriptTaskLog1 = new ScriptTaskLogDoc(1L, 0, 1, 101L, "0:127.0.0.1", "0:::1", "hello", 10);
            taskHostLog1.setScriptTaskLog(scriptTaskLog1);
            taskHostLog1.setLogType(LogTypeEnum.SCRIPT.getValue());
            logService.saveLog(taskHostLog1);

            TaskHostLog taskHostLog2 = new TaskHostLog();
            taskHostLog2.setStepInstanceId(1L);
            taskHostLog2.setExecuteCount(0);
            taskHostLog2.setBatch(1);
            taskHostLog2.setJobCreateDate("2020_07_29");
            ScriptTaskLogDoc scriptTaskLog2 = new ScriptTaskLogDoc(1L, 0, 1, 101L, "0:127.0.0.1", "0:::1", "world", 20);
            taskHostLog2.setScriptTaskLog(scriptTaskLog2);
            taskHostLog2.setLogType(LogTypeEnum.SCRIPT.getValue());
            logService.saveLog(taskHostLog2);

            ScriptLogQuery searchRequest = new ScriptLogQuery();
            searchRequest.setStepInstanceId(1L);
            searchRequest.setExecuteCount(0);
            searchRequest.setBatch(1);
            searchRequest.setJobCreateDate("2020_07_29");
            searchRequest.setHostIds(Collections.singletonList(101L));
            List<TaskHostLog> result = logService.listScriptLogs(searchRequest);
            assertThat(result).hasSize(1);
            TaskHostLog hostLog = result.get(0);
            assertThat(hostLog.getStepInstanceId()).isEqualTo(1L);
            assertThat(hostLog.getExecuteCount()).isEqualTo(0);
            assertThat(hostLog.getBatch()).isEqualTo(1);
            assertThat(hostLog.getHostId()).isEqualTo(101L);
            assertThat(hostLog.getIp()).isEqualTo("0:127.0.0.1");
            assertThat(hostLog.getIpv6()).isEqualTo("0:::1");
            assertThat(hostLog.getScriptContent()).isEqualTo("helloworld");


            TaskHostLog taskHostLog3 = new TaskHostLog();
            taskHostLog3.setStepInstanceId(2L);
            taskHostLog3.setExecuteCount(0);
            taskHostLog3.setBatch(null);
            taskHostLog3.setJobCreateDate("2020_07_29");
            ScriptTaskLogDoc scriptTaskLog3 = new ScriptTaskLogDoc(2L, 0, null, 101L, "0:127.0.0.1", "0:::1", "abc",
                20);
            taskHostLog3.setScriptTaskLog(scriptTaskLog3);
            taskHostLog3.setLogType(LogTypeEnum.SCRIPT.getValue());
            logService.saveLog(taskHostLog3);

            searchRequest = new ScriptLogQuery();
            searchRequest.setStepInstanceId(2L);
            searchRequest.setExecuteCount(0);
            searchRequest.setBatch(null);
            searchRequest.setJobCreateDate("2020_07_29");
            searchRequest.setIps(Collections.singletonList(null));
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
    class SaveFileLogTest {

        @Test
        @DisplayName("测试保存文件分发日志")
        void testSaveFileLog() {
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
                "50%",
                "[2020-07-30 11:00:00] Downloading...\n");

            List<FileTaskLogDoc> fileTaskLogList = new ArrayList<>();
            fileTaskLogList.add(fileTaskLog1);
            TaskHostLog taskHostLog = buildFileTaskHostLog(1L, 0, "2020_07_29", 102L,
                "0:127.0.0.2", "0:::2", fileTaskLogList);
            logService.saveLog(taskHostLog);

            FileLogQuery searchRequest = FileLogQuery.builder()
                .stepInstanceId(1L)
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
            assertThat(resultFileTaskLog1.getContent()).isEqualTo("[2020-07-30 11:00:00] Downloading...\n");

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
                "100%",
                "[2020-07-30 11:00:00] Download success\n");
            fileTaskLogList.clear();
            fileTaskLogList.add(fileTaskLog1);
            taskHostLog = buildFileTaskHostLog(1L, 0, "2020_07_29", 102L, "0:127.0.0.2", "0:::2",
                fileTaskLogList);
            logService.saveLog(taskHostLog);

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
            assertThat(resultFileTaskLog1.getContent()).isEqualTo("[2020-07-30 11:00:00] Downloading...\n[2020-07-30 " +
                "11:00:00] Download success\n");
        }
    }


    private TaskHostLog buildFileTaskHostLog(long stepInstanceId, int executeCount, String jobCreateDate, Long hostId,
                                             String ip, String ipv6, List<FileTaskLogDoc> fileTaskLogs) {
        TaskHostLog taskHostLog = new TaskHostLog();
        taskHostLog.setStepInstanceId(stepInstanceId);
        taskHostLog.setExecuteCount(executeCount);
        taskHostLog.setJobCreateDate(jobCreateDate);
        taskHostLog.setHostId(hostId);
        taskHostLog.setIp(ip);
        taskHostLog.setIpv6(ipv6);
        taskHostLog.setFileTaskLogs(fileTaskLogs);
        taskHostLog.setLogType(LogTypeEnum.FILE.getValue());
        return taskHostLog;
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
                                          String process,
                                          String content) {
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
        fileTaskLogDoc.setContent(content);
        return fileTaskLogDoc;
    }
}
