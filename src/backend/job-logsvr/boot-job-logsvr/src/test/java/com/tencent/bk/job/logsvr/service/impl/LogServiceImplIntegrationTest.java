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

import com.tencent.bk.job.logsvr.consts.LogTypeEnum;
import com.tencent.bk.job.logsvr.model.ScriptLogQuery;
import com.tencent.bk.job.logsvr.model.ScriptTaskLog;
import com.tencent.bk.job.logsvr.model.TaskIpLog;
import com.tencent.bk.job.logsvr.mongo.FileLogsCollectionLoader;
import com.tencent.bk.job.logsvr.mongo.LogCollectionFactory;
import com.tencent.bk.job.logsvr.mongo.LogCollectionLoaderFactory;
import com.tencent.bk.job.logsvr.mongo.ScriptLogsCollectionLoader;
import com.tencent.bk.job.logsvr.service.LogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataMongoTest
@TestPropertySource(locations = "classpath:test.properties")
@Import({LogServiceImpl.class, LogCollectionFactory.class, ScriptLogsCollectionLoader.class,
    LogCollectionLoaderFactory.class, FileLogsCollectionLoader.class})
public class LogServiceImplIntegrationTest {
    @Autowired
    private LogService logService;

    @Test
    void testSaveScriptLog() {
        TaskIpLog taskIpLog1 = new TaskIpLog();
        taskIpLog1.setStepInstanceId(1L);
        taskIpLog1.setExecuteCount(0);
        taskIpLog1.setBatch(1);
        taskIpLog1.setJobCreateDate("2020_07_29");
        taskIpLog1.setIp("0:127.0.0.1");
        ScriptTaskLog scriptTaskLog1 = new ScriptTaskLog(1L, 0, 1, "0:127.0.0.1", "hello", 10);
        taskIpLog1.setScriptTaskLog(scriptTaskLog1);
        taskIpLog1.setLogType(LogTypeEnum.SCRIPT.getValue());
        logService.saveLog(taskIpLog1);

        TaskIpLog taskIpLog2 = new TaskIpLog();
        taskIpLog2.setStepInstanceId(1L);
        taskIpLog2.setExecuteCount(0);
        taskIpLog2.setBatch(1);
        taskIpLog2.setJobCreateDate("2020_07_29");
        ScriptTaskLog scriptTaskLog2 = new ScriptTaskLog(1L, 0, 1, "0:127.0.0.1", "world", 20);
        taskIpLog2.setScriptTaskLog(scriptTaskLog2);
        taskIpLog2.setLogType(LogTypeEnum.SCRIPT.getValue());
        logService.saveLog(taskIpLog2);

        ScriptLogQuery searchRequest = new ScriptLogQuery();
        searchRequest.setStepInstanceId(1L);
        searchRequest.setExecuteCount(0);
        searchRequest.setBatch(1);
        searchRequest.setJobCreateDate("2020_07_29");
        searchRequest.setIps(Collections.singletonList("0:127.0.0.1"));
        TaskIpLog result = logService.getScriptLogByIp(searchRequest);
        assertThat(result.getStepInstanceId()).isEqualTo(1L);
        assertThat(result.getExecuteCount()).isEqualTo(0);
        assertThat(result.getBatch()).isEqualTo(1);
        assertThat(result.getIp()).isEqualTo("0:127.0.0.1");
        assertThat(result.getScriptContent()).isEqualTo("helloworld");



        TaskIpLog taskIpLog3 = new TaskIpLog();
        taskIpLog3.setStepInstanceId(2L);
        taskIpLog3.setExecuteCount(0);
        taskIpLog3.setBatch(null);
        taskIpLog3.setJobCreateDate("2020_07_29");
        ScriptTaskLog scriptTaskLog3 = new ScriptTaskLog(2L, 0, null, "0:127.0.0.1", "abc", 20);
        taskIpLog3.setScriptTaskLog(scriptTaskLog3);
        taskIpLog3.setLogType(LogTypeEnum.SCRIPT.getValue());
        logService.saveLog(taskIpLog3);

        searchRequest = new ScriptLogQuery();
        searchRequest.setStepInstanceId(2L);
        searchRequest.setExecuteCount(0);
        searchRequest.setBatch(null);
        searchRequest.setJobCreateDate("2020_07_29");
        searchRequest.setIps(Collections.singletonList("0:127.0.0.1"));
        result = logService.getScriptLogByIp(searchRequest);
        assertThat(result.getStepInstanceId()).isEqualTo(2L);
        assertThat(result.getExecuteCount()).isEqualTo(0);
        assertThat(result.getBatch()).isEqualTo(null);
        assertThat(result.getIp()).isEqualTo("0:127.0.0.1");
        assertThat(result.getScriptContent()).isEqualTo("abc");
    }

//    @Test
//    void testSaveFileLog1() {
//        FileTaskLog fileTaskLog1 = buildFileTaskDetailLog(3, "Downloading", "100KB/S", "0:1.1.1.1", "100MB", "50%",
//                "/tmp/1.log", 1, "[2020-07-30 11:00:00] Downloading...\n");
//        FileTaskLog fileTaskLog2 = buildFileTaskDetailLog(3, "Downloading", "100KB/S", "0:1.1.1.1", "100MB", "50%",
//                "/tmp/2.log", 1, "[2020-07-30 11:00:00] Downloading...\n");
//        List<FileTaskLog> fileTaskLogList = new ArrayList<>();
//        fileTaskLogList.add(fileTaskLog1);
//        fileTaskLogList.add(fileTaskLog2);
//        TaskLog executionLog1 = buildFileTaskLog(1L, 0, "2020_07_29", "0:127.0.0.1",
//                fileTaskLogList);
//        logService.saveLog(executionLog1);
//
//        TaskLog searchRequest = new TaskLog();
//        searchRequest.setStepInstanceId(1L);
//        searchRequest.setExecuteCount(0);
//        searchRequest.setJobCreateDate("2020_07_29");
//        searchRequest.setIp("0:127.0.0.1");
//        searchRequest.setLogType(LogTypeEnum.FILE.getValue());
//        TaskLog result = logService.getLogByIp(searchRequest);
//        assertThat(result.getStepInstanceId()).isEqualTo(1L);
//        assertThat(result.getExecuteCount()).isEqualTo(0);
//        assertThat(result.getIp()).isEqualTo("0:127.0.0.1");
//        FileTaskLog resultFileTaskLog1 = result.getFileTaskLogs().get(0);
//        assertThat(resultFileTaskLog1.getStatus()).isEqualTo(3);
//        assertThat(resultFileTaskLog1.getStatusDesc()).isEqualTo("Downloading");
//        assertThat(resultFileTaskLog1.getMode()).isEqualTo(1);
//        assertThat(resultFileTaskLog1.getFile()).isEqualTo("/tmp/1.log");
//        assertThat(resultFileTaskLog1.getSourceIp()).isEqualTo("0:1.1.1.1");
//        assertThat(resultFileTaskLog1.getSize()).isEqualTo("100MB");
//        assertThat(resultFileTaskLog1.getSpeed()).isEqualTo("100KB/S");
//        assertThat(resultFileTaskLog1.getProcess()).isEqualTo("50%");
//        assertThat(resultFileTaskLog1.getContent()).isEqualTo("[2020-07-30 11:00:00] Downloading...\n");
//        FileTaskLog resultFileTaskLog2 = result.getFileTaskLogs().get(1);
//        assertThat(resultFileTaskLog2.getStatus()).isEqualTo(3);
//        assertThat(resultFileTaskLog2.getStatusDesc()).isEqualTo("Downloading");
//        assertThat(resultFileTaskLog2.getMode()).isEqualTo(1);
//        assertThat(resultFileTaskLog2.getFile()).isEqualTo("/tmp/2.log");
//        assertThat(resultFileTaskLog2.getSourceIp()).isEqualTo("0:1.1.1.1");
//        assertThat(resultFileTaskLog2.getSize()).isEqualTo("100MB");
//        assertThat(resultFileTaskLog2.getSpeed()).isEqualTo("100KB/S");
//        assertThat(resultFileTaskLog2.getProcess()).isEqualTo("50%");
//        assertThat(resultFileTaskLog2.getContent()).isEqualTo("[2020-07-30 11:00:00] Downloading...\n");
//
//        // 再次插入日志，验证日志更新场景
//        fileTaskLog1 = buildFileTaskDetailLog(4, "Finished", "0KB/S", "0:1.1.1.1", "100MB", "100%",
//                "/tmp/1.log", 1, "[2020-07-30 11:00:00] Download success\n");
//        fileTaskLog2 = buildFileTaskDetailLog(4, "Finished", "0KB/S", "0:1.1.1.1", "100MB", "100%",
//                "/tmp/2.log", 1, "[2020-07-30 11:00:00] Download success\n");
//        fileTaskLogList = new ArrayList<>();
//        fileTaskLogList.add(fileTaskLog1);
//        fileTaskLogList.add(fileTaskLog2);
//        executionLog1 = buildFileTaskLog(1L, 0, "2020_07_29", "0:127.0.0.1",
//                fileTaskLogList);
//        logService.saveLog(executionLog1);
//
//        result = logService.getLogByIp(searchRequest);
//        assertThat(result.getStepInstanceId()).isEqualTo(1L);
//        assertThat(result.getExecuteCount()).isEqualTo(0);
//        assertThat(result.getIp()).isEqualTo("0:127.0.0.1");
//        fileTaskLog1 = result.getFileTaskLogs().get(0);
//        assertThat(fileTaskLog1.getStatus()).isEqualTo(4);
//        assertThat(fileTaskLog1.getStatusDesc()).isEqualTo("Finished");
//        assertThat(fileTaskLog1.getMode()).isEqualTo(1);
//        assertThat(fileTaskLog1.getFile()).isEqualTo("/tmp/1.log");
//        assertThat(fileTaskLog1.getSourceIp()).isEqualTo("0:1.1.1.1");
//        assertThat(fileTaskLog1.getSize()).isEqualTo("100MB");
//        assertThat(fileTaskLog1.getSpeed()).isEqualTo("0KB/S");
//        assertThat(fileTaskLog1.getProcess()).isEqualTo("100%");
//        assertThat(fileTaskLog1.getContent()).isEqualTo("[2020-07-30 11:00:00] Downloading...\n[2020-07-30 
//       11:00:00] Download success\n");
//        fileTaskLog2 = result.getFileTaskLogs().get(1);
//        assertThat(fileTaskLog2.getStatus()).isEqualTo(4);
//        assertThat(fileTaskLog2.getStatusDesc()).isEqualTo("Finished");
//        assertThat(fileTaskLog2.getMode()).isEqualTo(1);
//        assertThat(fileTaskLog2.getFile()).isEqualTo("/tmp/2.log");
//        assertThat(fileTaskLog2.getSourceIp()).isEqualTo("0:1.1.1.1");
//        assertThat(fileTaskLog2.getSize()).isEqualTo("100MB");
//        assertThat(fileTaskLog2.getSpeed()).isEqualTo("0KB/S");
//        assertThat(fileTaskLog2.getProcess()).isEqualTo("100%");
//        assertThat(fileTaskLog2.getContent()).isEqualTo("[2020-07-30 11:00:00] Downloading...\n[2020-07-30 
//       11:00:00] Download success\n");
//    }
//
//    private TaskIpLog buildFileTaskLog(long stepInstanceId, int executeCount, String jobCreateDate, String ip, 
//   List<FileTaskLog> fileTaskLogs) {
//        TaskIpLog executionLog = new TaskIpLog();
//        executionLog.setStepInstanceId(stepInstanceId);
//        executionLog.setExecuteCount(executeCount);
//        executionLog.setJobCreateDate(jobCreateDate);
//        executionLog.setIp(ip);
//        executionLog.setFileTaskLogs(fileTaskLogs);
//        executionLog.setLogType(LogTypeEnum.FILE.getValue());
//        return executionLog;
//    }
//
//    private FileTaskLog buildFileTaskDetailLog(Integer status, String statusDesc, String speed, String sourceIp,
//                                               String size, String process, String name,
//                                               Integer mode, String content) {
//        FileTaskLog fileTaskLog1 = new FileTaskLog();
//        fileTaskLog1.setStatus(status);
//        fileTaskLog1.setStatusDesc(statusDesc);
//        fileTaskLog1.setSpeed(speed);
//        fileTaskLog1.setSourceIp(sourceIp);
//        fileTaskLog1.setSize(size);
//        fileTaskLog1.setProcess(process);
//        fileTaskLog1.setDisplayFile(name);
//        fileTaskLog1.setMode(mode);
//        fileTaskLog1.setContent(content);
//        return fileTaskLog1;
//    }
}
