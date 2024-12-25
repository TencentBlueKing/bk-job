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

package com.tencent.bk.job.execute.dao.impl;

import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.dao.StepInstanceDAO;
import com.tencent.bk.job.execute.model.ConfirmStepInstanceDTO;
import com.tencent.bk.job.execute.model.ExecuteTargetDTO;
import com.tencent.bk.job.execute.model.FileDetailDTO;
import com.tencent.bk.job.execute.model.FileSourceDTO;
import com.tencent.bk.job.execute.model.FileStepInstanceDTO;
import com.tencent.bk.job.execute.model.ScriptStepInstanceDTO;
import com.tencent.bk.job.execute.model.StepInstanceBaseDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.manage.api.common.constants.script.ScriptTypeEnum;
import com.tencent.bk.job.manage.api.common.constants.task.TaskFileTypeEnum;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@ExtendWith(SpringExtension.class)
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:test.properties")
@SqlConfig(encoding = "utf-8")
@Sql({"/init_step_instance_data.sql"})
public class StepInstanceDAOImplIntegrationTest {
    @Autowired
    private StepInstanceDAO stepInstanceDAO;

    @Test
    public void testGetStepInstanceBase() {
        long taskInstanceId = 1L;
        long stepInstanceId = 1L;
        StepInstanceBaseDTO stepInstance = stepInstanceDAO.getStepInstanceBase(taskInstanceId, stepInstanceId);

        assertThat(stepInstance.getId()).isEqualTo(stepInstanceId);
        assertThat(stepInstance.getAppId()).isEqualTo(2L);
        assertThat(stepInstance.getTaskInstanceId()).isEqualTo(1L);
        assertThat(stepInstance.getStepId()).isEqualTo(1L);
        assertThat(stepInstance.getStepNum()).isEqualTo(2);
        assertThat(stepInstance.getStepOrder()).isEqualTo(1);
        assertThat(stepInstance.getName()).isEqualTo("task1-step1");
        assertThat(stepInstance.getExecuteType()).isEqualTo(StepExecuteTypeEnum.EXECUTE_SCRIPT);
        assertThat(stepInstance.getOperator()).isEqualTo("admin");
        assertThat(stepInstance.getStatus()).isEqualTo(RunStatusEnum.SUCCESS);
        assertThat(stepInstance.getExecuteCount()).isEqualTo(0);
        assertThat(stepInstance.getStartTime()).isEqualTo(1572868800000L);
        assertThat(stepInstance.getEndTime()).isEqualTo(1572868801000L);
        assertThat(stepInstance.getTotalTime()).isEqualTo(1111L);
        assertThat(stepInstance.getCreateTime()).isEqualTo(1572868800000L);
        assertThat(stepInstance.getTargetExecuteObjects()).isNotNull();
        List<HostDTO> expectedServer = new ArrayList<>();
        expectedServer.add(new HostDTO(0L, "127.0.0.1"));
        assertThat(stepInstance.getTargetExecuteObjects().getIpList()).containsAll(expectedServer);
        assertThat(stepInstance.getBatch()).isEqualTo(0);
    }

    @Test
    public void testAddStepInstanceBase() {
        StepInstanceBaseDTO stepInstanceDTO = new StepInstanceBaseDTO();
        stepInstanceDTO.setAppId(2L);
        stepInstanceDTO.setName("task1-step1");
        stepInstanceDTO.setTaskInstanceId(1L);
        stepInstanceDTO.setStepId(1L);
        stepInstanceDTO.setExecuteType(StepExecuteTypeEnum.EXECUTE_SCRIPT);
        ExecuteTargetDTO executeTarget = new ExecuteTargetDTO();
        List<HostDTO> ipList = new ArrayList<>();
        ipList.add(new HostDTO(0L, "127.0.0.1"));
        executeTarget.setIpList(ipList);
        stepInstanceDTO.setTargetExecuteObjects(executeTarget);
        stepInstanceDTO.setOperator("admin");
        stepInstanceDTO.setStatus(RunStatusEnum.SUCCESS);
        stepInstanceDTO.setExecuteCount(0);
        stepInstanceDTO.setStartTime(1572868800000L);
        stepInstanceDTO.setEndTime(1572868801000L);
        stepInstanceDTO.setTotalTime(1000L);
        stepInstanceDTO.setCreateTime(1572868800000L);
        stepInstanceDTO.setStepNum(3);
        stepInstanceDTO.setStepOrder(1);

        long stepInstanceId = stepInstanceDAO.addStepInstanceBase(stepInstanceDTO);

        StepInstanceBaseDTO returnStepInstance = stepInstanceDAO.getStepInstanceBase(stepInstanceId);

        assertThat(returnStepInstance.getId()).isEqualTo(stepInstanceId);
        assertThat(returnStepInstance.getAppId()).isEqualTo(2L);
        assertThat(returnStepInstance.getTaskInstanceId()).isEqualTo(1L);
        assertThat(returnStepInstance.getStepId()).isEqualTo(1L);
        assertThat(returnStepInstance.getName()).isEqualTo("task1-step1");
        assertThat(returnStepInstance.getExecuteType()).isEqualTo(StepExecuteTypeEnum.EXECUTE_SCRIPT);
        assertThat(returnStepInstance.getTargetExecuteObjects().getIpList()).hasSize(1);
        List<HostDTO> expectedServer = new ArrayList<>();
        expectedServer.add(new HostDTO(0L, "127.0.0.1"));
        assertThat(returnStepInstance.getTargetExecuteObjects().getIpList()).containsAll(expectedServer);
        assertThat(returnStepInstance.getOperator()).isEqualTo("admin");
        assertThat(returnStepInstance.getStatus()).isEqualTo(RunStatusEnum.SUCCESS);
        assertThat(returnStepInstance.getExecuteCount()).isEqualTo(0);
        assertThat(returnStepInstance.getStartTime()).isEqualTo(1572868800000L);
        assertThat(returnStepInstance.getEndTime()).isEqualTo(1572868801000L);
        assertThat(returnStepInstance.getTotalTime()).isEqualTo(1000L);
        assertThat(returnStepInstance.getCreateTime()).isEqualTo(1572868800000L);
        assertThat(returnStepInstance.getStepNum()).isEqualTo(3);
        assertThat(returnStepInstance.getStepOrder()).isEqualTo(1);
        assertThat(returnStepInstance.getBatch()).isEqualTo(0);
    }

    @Test
    public void testListStepInstanceBaseByTaskInstanceId() {
        long taskInstanceId = 1L;

        List<StepInstanceBaseDTO> stepInstanceList =
            stepInstanceDAO.listStepInstanceBaseByTaskInstanceId(taskInstanceId);

        assertThat(stepInstanceList).hasSize(2);
        assertThat(stepInstanceList).extracting("id").containsSequence(1L, 2L);
    }

    @Test
    public void testResetStepStatus() {
        long taskInstanceId = 1L;
        long stepInstanceId = 1L;
        stepInstanceDAO.resetStepStatus(taskInstanceId, stepInstanceId);

        StepInstanceBaseDTO returnStepInstance = stepInstanceDAO.getStepInstanceBase(taskInstanceId, stepInstanceId);

        assertThat(returnStepInstance.getId()).isEqualTo(stepInstanceId);
        assertThat(returnStepInstance.getTaskInstanceId()).isEqualTo(taskInstanceId);
        assertThat(returnStepInstance.getStartTime()).isNull();
        assertThat(returnStepInstance.getEndTime()).isNull();
        assertThat(returnStepInstance.getTotalTime()).isNull();
    }

    @Test
    public void testAddStepRetryCount() {
        long taskInstanceId = 1L;
        long stepInstanceId = 1L;

        stepInstanceDAO.addStepExecuteCount(taskInstanceId, stepInstanceId);

        StepInstanceBaseDTO returnStepInstance = stepInstanceDAO.getStepInstanceBase(taskInstanceId, stepInstanceId);

        assertThat(returnStepInstance.getTaskInstanceId()).isEqualTo(taskInstanceId);
        assertThat(returnStepInstance.getId()).isEqualTo(stepInstanceId);
        assertThat(returnStepInstance.getExecuteCount()).isEqualTo(1);
    }

    @Test
    public void testUpdateStepStatus() {
        long taskInstanceId = 1L;
        long stepInstanceId = 1L;
        stepInstanceDAO.updateStepStatus(taskInstanceId, stepInstanceId, RunStatusEnum.RUNNING.getValue());

        StepInstanceBaseDTO returnStepInstance = stepInstanceDAO.getStepInstanceBase(stepInstanceId);

        assertThat(returnStepInstance.getId()).isEqualTo(stepInstanceId);
        assertThat(returnStepInstance.getId()).isEqualTo(stepInstanceId);
        assertThat(returnStepInstance.getStatus()).isEqualTo(RunStatusEnum.RUNNING);
    }

    @Test
    public void testUpdateStepStartTime() {
        long taskInstanceId = 3L;
        long stepInstanceId = 4L;
        long startTime = 1573041600000L;
        stepInstanceDAO.updateStepStartTimeIfNull(taskInstanceId, stepInstanceId, startTime);
        StepInstanceBaseDTO returnStepInstance = stepInstanceDAO.getStepInstanceBase(taskInstanceId, stepInstanceId);
        assertThat(returnStepInstance.getTaskInstanceId()).isEqualTo(taskInstanceId);
        assertThat(returnStepInstance.getId()).isEqualTo(stepInstanceId);
        assertThat(returnStepInstance.getStartTime()).isEqualTo(startTime);

        stepInstanceId = 1L;
        taskInstanceId = 1L;
        startTime = 1573041700000L;
        stepInstanceDAO.updateStepStartTimeIfNull(taskInstanceId, stepInstanceId, startTime);
        returnStepInstance = stepInstanceDAO.getStepInstanceBase(taskInstanceId, stepInstanceId);
        assertThat(returnStepInstance.getTaskInstanceId()).isEqualTo(taskInstanceId);
        assertThat(returnStepInstance.getId()).isEqualTo(stepInstanceId);
        assertThat(returnStepInstance.getStartTime()).isEqualTo(1572868800000L);
    }

    @Test
    void testUpdateStepStartTimeIfNull() {
        long taskInstanceId = 1L;
        long stepInstanceId = 1L;
        long startTime = 1573041600000L;

        stepInstanceDAO.updateStepStartTime(taskInstanceId, stepInstanceId, startTime);

        StepInstanceBaseDTO returnStepInstance = stepInstanceDAO.getStepInstanceBase(taskInstanceId, stepInstanceId);

        assertThat(returnStepInstance.getTaskInstanceId()).isEqualTo(taskInstanceId);
        assertThat(returnStepInstance.getId()).isEqualTo(stepInstanceId);
        assertThat(returnStepInstance.getStartTime()).isEqualTo(startTime);
    }

    @Test
    public void testUpdateStepEndTime() {
        long taskInstanceId = 1L;
        long stepInstanceId = 1L;
        long endTime = 1573041600000L;

        stepInstanceDAO.updateStepEndTime(taskInstanceId, stepInstanceId, endTime);

        StepInstanceBaseDTO returnStepInstance = stepInstanceDAO.getStepInstanceBase(taskInstanceId, stepInstanceId);

        assertThat(returnStepInstance.getTaskInstanceId()).isEqualTo(taskInstanceId);
        assertThat(returnStepInstance.getId()).isEqualTo(stepInstanceId);
        assertThat(returnStepInstance.getEndTime()).isEqualTo(endTime);
    }

    @Test
    public void testUpdateStepTotalTime() {
        long taskInstanceId = 1L;
        long stepInstanceId = 1L;
        long totalTime = 1234L;

        stepInstanceDAO.updateStepTotalTime(taskInstanceId, stepInstanceId, totalTime);

        StepInstanceBaseDTO returnStepInstance = stepInstanceDAO.getStepInstanceBase(taskInstanceId, stepInstanceId);

        assertThat(returnStepInstance.getTaskInstanceId()).isEqualTo(taskInstanceId);
        assertThat(returnStepInstance.getId()).isEqualTo(stepInstanceId);
        assertThat(returnStepInstance.getTotalTime()).isEqualTo(totalTime);
    }

    @Test
    public void testResetStepExecuteInfoForRetry() {
        long taskInstanceId = 1L;
        long stepInstanceId = 1L;
        stepInstanceDAO.resetStepExecuteInfoForRetry(taskInstanceId, stepInstanceId);
        StepInstanceBaseDTO returnStepInstance = stepInstanceDAO.getStepInstanceBase(taskInstanceId, stepInstanceId);

        assertThat(returnStepInstance).isNotNull();
        assertThat(returnStepInstance.getTaskInstanceId()).isEqualTo(taskInstanceId);
        assertThat(returnStepInstance.getId()).isEqualTo(stepInstanceId);
        assertThat(returnStepInstance.getStartTime()).isNotNull();
        assertThat(returnStepInstance.getEndTime()).isNull();
        assertThat(returnStepInstance.getStatus()).isEqualTo(RunStatusEnum.RUNNING);
        assertThat(returnStepInstance.getTotalTime()).isNull();
    }

    @Test
    public void testGetScriptStepInstance() {
        long taskInstanceId = 1L;
        long stepInstanceId = 1L;
        ScriptStepInstanceDTO returnStepInstance = stepInstanceDAO.getScriptStepInstance(taskInstanceId,
            stepInstanceId);
        assertThat(returnStepInstance.getTaskInstanceId()).isEqualTo(taskInstanceId);
        assertThat(returnStepInstance.getStepInstanceId()).isEqualTo(stepInstanceId);
        assertThat(returnStepInstance.getAccount()).isEqualTo("root");
        assertThat(returnStepInstance.getAccountId()).isEqualTo(1L);
        assertThat(returnStepInstance.getDbPass()).isEqualTo("db_password");
        assertThat(returnStepInstance.getDbPort()).isEqualTo(3306);
        assertThat(returnStepInstance.getDbAccount()).isEqualTo("root");
        assertThat(returnStepInstance.getDbAccountId()).isEqualTo(11L);
        assertThat(returnStepInstance.getDbType()).isEqualTo(1);
        assertThat(returnStepInstance.getScriptContent()).isEqualTo("script_content");
        assertThat(returnStepInstance.getScriptParam()).isEqualTo("${var1}");
        assertThat(returnStepInstance.getResolvedScriptParam()).isEqualTo("var1");
        assertThat(returnStepInstance.getScriptType()).isEqualTo(ScriptTypeEnum.SHELL);
        assertThat(returnStepInstance.getTimeout()).isEqualTo(1000);
        assertThat(returnStepInstance.isSecureParam()).isEqualTo(true);
    }

    @Test
    public void testAddScriptStepInstance() {
        StepInstanceDTO scriptStepInstance = new StepInstanceDTO();
        scriptStepInstance.setId(100L);
        scriptStepInstance.setTaskInstanceId(100L);
        scriptStepInstance.setAccount("root");
        scriptStepInstance.setAccountId(100L);
        scriptStepInstance.setAccountAlias("root");
        scriptStepInstance.setDbPass("password");
        scriptStepInstance.setDbPort(3306);
        scriptStepInstance.setDbAccount("root");
        scriptStepInstance.setDbAccountId(1001L);
        scriptStepInstance.setDbType(1);
        scriptStepInstance.setScriptContent("script_content");
        scriptStepInstance.setScriptParam("${var1} ${var2}");
        scriptStepInstance.setResolvedScriptParam("var1 var2");
        scriptStepInstance.setScriptType(ScriptTypeEnum.SHELL);
        scriptStepInstance.setTimeout(1000);

        stepInstanceDAO.addScriptStepInstance(scriptStepInstance);

        ScriptStepInstanceDTO savedStepInstance = stepInstanceDAO.getScriptStepInstance(100L, 100L);
        assertThat(savedStepInstance.getTaskInstanceId()).isEqualTo(100L);
        assertThat(savedStepInstance.getStepInstanceId()).isEqualTo(100L);
        assertThat(savedStepInstance.getAccount()).isEqualTo("root");
        assertThat(savedStepInstance.getAccountId()).isEqualTo(100L);
        assertThat(savedStepInstance.getDbPass()).isEqualTo("password");
        assertThat(savedStepInstance.getDbPort()).isEqualTo(3306);
        assertThat(savedStepInstance.getDbAccount()).isEqualTo("root");
        assertThat(savedStepInstance.getDbAccountId()).isEqualTo(1001L);
        assertThat(savedStepInstance.getDbType()).isEqualTo(1);
        assertThat(savedStepInstance.getScriptContent()).isEqualTo("script_content");
        assertThat(savedStepInstance.getScriptParam()).isEqualTo("${var1} ${var2}");
        assertThat(savedStepInstance.getResolvedScriptParam()).isEqualTo("var1 var2");
        assertThat(savedStepInstance.getScriptType()).isEqualTo(ScriptTypeEnum.SHELL);
        assertThat(savedStepInstance.getTimeout()).isEqualTo(1000);
    }

    @Test
    public void testAddFileStepInstance() {
        StepInstanceDTO fileStepInstance = new StepInstanceDTO();
        fileStepInstance.setId(101L);
        fileStepInstance.setTaskInstanceId(101L);
        fileStepInstance.setAccount("root");
        fileStepInstance.setAccountId(1L);
        fileStepInstance.setAccountAlias("root");
        fileStepInstance.setFileDownloadSpeedLimit(100);
        fileStepInstance.setFileUploadSpeedLimit(200);
        fileStepInstance.setFileDuplicateHandle(1);
        fileStepInstance.setNotExistPathHandler(1);
        fileStepInstance.setFileTargetPath("/tmp/");
        fileStepInstance.setFileTargetName("test.log");
        fileStepInstance.setTimeout(1000);

        List<FileSourceDTO> fileSources = new ArrayList<>();
        FileSourceDTO fileSource = new FileSourceDTO();
        fileSource.setAccount("root");
        fileSource.setAccountId(1L);
        fileSource.setLocalUpload(false);
        fileSource.setFileType(TaskFileTypeEnum.SERVER.getType());
        ExecuteTargetDTO fileSourceExecuteTarget = new ExecuteTargetDTO();
        fileSourceExecuteTarget.setIpList(Lists.newArrayList(new HostDTO(1L, "10.10.10.10")));
        fileSource.setServers(fileSourceExecuteTarget);
        FileDetailDTO fileDetail = new FileDetailDTO();
        fileDetail.setFilePath("/tmp/1.log");
        fileDetail.setFileHash("hash");
        fileSource.setFiles(Lists.newArrayList(fileDetail));
        fileSources.add(fileSource);

        fileStepInstance.setFileSourceList(fileSources);

        stepInstanceDAO.addFileStepInstance(fileStepInstance);

        FileStepInstanceDTO savedStepInstance = stepInstanceDAO.getFileStepInstance(101L, 101L);
        assertThat(savedStepInstance.getTaskInstanceId()).isEqualTo(101L);
        assertThat(savedStepInstance.getStepInstanceId()).isEqualTo(101L);
        assertThat(savedStepInstance.getAccount()).isEqualTo("root");
        assertThat(savedStepInstance.getAccountId()).isEqualTo(1L);
        assertThat(savedStepInstance.getFileUploadSpeedLimit()).isEqualTo(200);
        assertThat(savedStepInstance.getFileDownloadSpeedLimit()).isEqualTo(100);
        assertThat(savedStepInstance.getFileDuplicateHandle()).isEqualTo(1);
        assertThat(savedStepInstance.getNotExistPathHandler()).isEqualTo(1);
        assertThat(savedStepInstance.getFileTargetPath()).isEqualTo("/tmp/");
        assertThat(savedStepInstance.getFileTargetName()).isEqualTo("test.log");
        assertThat(savedStepInstance.getTimeout()).isEqualTo(1000);
        assertThat(savedStepInstance.getFileSourceList()).isNotEmpty();
        assertThat(savedStepInstance.getFileSourceList().get(0)).isNotNull();
        assertThat(savedStepInstance.getFileSourceList().get(0).getAccountId()).isEqualTo(1L);
        assertThat(savedStepInstance.getFileSourceList().get(0).getAccount()).isEqualTo("root");
        assertThat(savedStepInstance.getFileSourceList().get(0).getServers()).isNotNull();
        assertThat(savedStepInstance.getFileSourceList().get(0).getServers().getIpList()).isNotEmpty();
        assertThat(savedStepInstance.getFileSourceList().get(0).getServers().getIpList())
            .containsOnly(new HostDTO(1L,
                "10.10.10.10"));
        assertThat(savedStepInstance.getFileSourceList().get(0).getFiles()).isNotEmpty();
        assertThat(savedStepInstance.getFileSourceList().get(0).getFiles().get(0).getFilePath())
            .isEqualTo("/tmp/1.log");
    }

    @Test
    public void testGetFileStepInstance() {
        FileStepInstanceDTO returnStepInstance = stepInstanceDAO.getFileStepInstance(1L, 2L);
        assertThat(returnStepInstance).isNotNull();
        assertThat(returnStepInstance.getTaskInstanceId()).isEqualTo(1L);
        assertThat(returnStepInstance.getStepInstanceId()).isEqualTo(2L);
        assertThat(returnStepInstance.getFileSourceList()).isNotEmpty();
        assertThat(returnStepInstance.getFileSourceList().get(0).getFiles().get(0).getFilePath()).isEqualTo("/$" +
            "{log_dir}/1.log");
        assertThat(returnStepInstance.getFileTargetPath()).isEqualTo("/${log_dir}/");
        assertThat(returnStepInstance.getFileTargetName()).isEqualTo("2.log");
        assertThat(returnStepInstance.getResolvedFileTargetPath()).isEqualTo("/tmp/");
        assertThat(returnStepInstance.getFileUploadSpeedLimit()).isEqualTo(100);
        assertThat(returnStepInstance.getFileDownloadSpeedLimit()).isEqualTo(100);
        assertThat(returnStepInstance.getFileDuplicateHandle()).isEqualTo(1);
        assertThat(returnStepInstance.getNotExistPathHandler()).isEqualTo(1);
        assertThat(returnStepInstance.getTimeout()).isEqualTo(1000);
        assertThat(returnStepInstance.getAccountId()).isEqualTo(1L);
        assertThat(returnStepInstance.getAccount()).isEqualTo("root");
    }


    @Test
    public void testGetConfirmStepInstance() {
        ConfirmStepInstanceDTO returnStepInstance = stepInstanceDAO.getConfirmStepInstance(13L, 15L);
        assertThat(returnStepInstance).isNotNull();
        assertThat(returnStepInstance.getTaskInstanceId()).isEqualTo(13L);
        assertThat(returnStepInstance.getStepInstanceId()).isEqualTo(15L);
        assertThat(returnStepInstance.getConfirmMessage()).isEqualTo("confirm_message");
        assertThat(returnStepInstance.getConfirmReason()).isEqualTo("confirm_reason");
        assertThat(returnStepInstance.getConfirmRoles()).containsOnly("JOB_RESOURCE_TRIGGER_USER");
        assertThat(returnStepInstance.getConfirmUsers()).containsOnly("admin", "test");
        assertThat(returnStepInstance.getNotifyChannels()).containsOnly("weixin");
    }

    @Test
    public void testAddConfirmStepInstance() {
        StepInstanceDTO confirmStepInstance = new StepInstanceDTO();
        confirmStepInstance.setId(102L);
        confirmStepInstance.setTaskInstanceId(102L);
        confirmStepInstance.setConfirmMessage("confirm_message");
        confirmStepInstance.setConfirmUsers(Lists.newArrayList("admin"));
        confirmStepInstance.setConfirmRoles(Lists.newArrayList("JOB_RESOURCE_TRIGGER_USER"));
        confirmStepInstance.setNotifyChannels(Lists.newArrayList("weixin"));

        stepInstanceDAO.addConfirmStepInstance(confirmStepInstance);

        ConfirmStepInstanceDTO returnStepInstance = stepInstanceDAO.getConfirmStepInstance(102L, 102L);
        assertThat(returnStepInstance).isNotNull();
        assertThat(returnStepInstance.getTaskInstanceId()).isEqualTo(102L);
        assertThat(returnStepInstance.getStepInstanceId()).isEqualTo(102L);
        assertThat(returnStepInstance.getConfirmMessage()).isEqualTo("confirm_message");
        assertThat(returnStepInstance.getConfirmRoles()).containsOnly("JOB_RESOURCE_TRIGGER_USER");
        assertThat(returnStepInstance.getConfirmUsers()).containsOnly("admin");
        assertThat(returnStepInstance.getNotifyChannels()).containsOnly("weixin");

    }

    @Test
    void testUpdateResolvedScriptParam() {
        stepInstanceDAO.updateResolvedScriptParam(1L, 1L, true, "resolved_var");
        ScriptStepInstanceDTO updatedStepInstance = stepInstanceDAO.getScriptStepInstance(1L, 1L);

        assertThat(updatedStepInstance).isNotNull();
        assertThat(updatedStepInstance.getResolvedScriptParam()).isEqualTo("resolved_var");

    }

    @Test
    void testUpdateResolvedFileSource() {
        List<FileSourceDTO> fileSources = new ArrayList<>();
        List<FileDetailDTO> fileList = new ArrayList<>();
        FileDetailDTO fileDetail = new FileDetailDTO();
        fileDetail.setFilePath("/data/${log_dir}/1.log");
        fileDetail.setResolvedFilePath("/data/logs/1.log");
        fileDetail.setFileName("1.log");
        fileList.add(fileDetail);
        FileSourceDTO fileSourceDTO = new FileSourceDTO();
        fileSourceDTO.setFiles(fileList);
        fileSourceDTO.setLocalUpload(false);
        fileSourceDTO.setFileType(TaskFileTypeEnum.SERVER.getType());
        fileSourceDTO.setAccountId(1L);
        fileSourceDTO.setAccount("root");
        ExecuteTargetDTO executeTarget = new ExecuteTargetDTO();
        List<HostDTO> ips = new ArrayList<>();
        ips.add(new HostDTO(1L, "10.10.10.10"));
        executeTarget.setIpList(ips);
        fileSourceDTO.setServers(executeTarget);
        fileSources.add(fileSourceDTO);


        stepInstanceDAO.updateResolvedSourceFile(1L, 2L, fileSources);
        FileStepInstanceDTO updatedStepInstance = stepInstanceDAO.getFileStepInstance(1L, 2L);

        assertThat(updatedStepInstance).isNotNull();
        assertThat(updatedStepInstance.getFileSourceList()).hasSize(1);
        assertThat(updatedStepInstance.getFileSourceList().get(0).getFiles().get(0).getFilePath()).isEqualTo("/data/$" +
            "{log_dir}/1.log");
        assertThat(updatedStepInstance.getFileSourceList().get(0).getFiles().get(0).getResolvedFilePath()).isEqualTo(
            "/data/logs/1.log");
        assertThat(updatedStepInstance.getFileSourceList().get(0).getFiles().get(0).getResolvedFilePath()).isEqualTo(
            "/data/logs/1.log");
        assertThat(updatedStepInstance.getFileSourceList().get(0).getServers().getIpList()).contains(new HostDTO(1L,
            "10.10.10.10"));
    }

    @Test
    void testUpdateResolvedFileTargetPath() {
        stepInstanceDAO.updateResolvedTargetPath(1L, 2L, "/data/bkee/");
        FileStepInstanceDTO updatedStepInstance = stepInstanceDAO.getFileStepInstance(1L, 2L);

        assertThat(updatedStepInstance).isNotNull();
        assertThat(updatedStepInstance.getResolvedFileTargetPath()).isEqualTo("/data/bkee/");
    }

    @Test
    void testUpdateConfirmReason() {
        stepInstanceDAO.updateConfirmReason(13L, 15L, "ok");
        ConfirmStepInstanceDTO updatedStepInstance = stepInstanceDAO.getConfirmStepInstance(13L, 15L);

        assertThat(updatedStepInstance).isNotNull();
        assertThat(updatedStepInstance.getConfirmReason()).isEqualTo("ok");
    }

    @Test
    void testUpdateStepOperator() {
        stepInstanceDAO.updateStepOperator(3L, 4L, "test");
        StepInstanceBaseDTO updatedStepInstance = stepInstanceDAO.getStepInstanceBase(3L, 4L);

        assertThat(updatedStepInstance).isNotNull();
        assertThat(updatedStepInstance.getOperator()).isEqualTo("test");
    }

    @Test
    void testGetPreExecutableStepInstance() {
        StepInstanceBaseDTO preStepInstance = stepInstanceDAO.getPreExecutableStepInstance(1L, 2);
        assertThat(preStepInstance).isNotNull();
        assertThat(preStepInstance.getTaskInstanceId()).isEqualTo(1L);
        assertThat(preStepInstance.getId()).isEqualTo(1L);
    }

    @Test
    void updateStepCurrentBatch() {
        stepInstanceDAO.updateStepCurrentBatch(1L, 1L, 1);

        StepInstanceBaseDTO stepInstance = stepInstanceDAO.getStepInstanceBase(1L, 1L);
        assertThat(stepInstance.getBatch()).isEqualTo(1);
    }

    @Test
    void updateStepRollingConfigId() {
        stepInstanceDAO.updateStepRollingConfigId(1L, 1L, 1000L);

        StepInstanceBaseDTO stepInstance = stepInstanceDAO.getStepInstanceBase(1L, 1L);
        assertThat(stepInstance.getRollingConfigId()).isEqualTo(1000L);
    }


}
